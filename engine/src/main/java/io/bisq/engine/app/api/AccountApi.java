package io.bisq.engine.app.api;

import io.bisq.common.locale.*;
import io.bisq.core.payment.*;
import io.bisq.core.payment.payload.PaymentAccountPayload;
import io.bisq.core.payment.payload.SepaAccountPayload;
import io.bisq.gui.util.validation.BICValidator;
import io.bisq.gui.util.validation.IBANValidator;
import io.bisq.gui.util.validation.InputValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.MimeTypeUtils.*;

@RestController
@RequestMapping("/api/account")
@Api(tags = {"Account"})
public class AccountApi extends ApiData {

    public static final class AccountJson {
        public String name;
        public String id;
        public String paymentmethod;
        public List<String> currencies;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of payment accounts")
    public static List<AccountJson> PaymentAccounts() throws Exception {
        checkErrors();

        if (user.getPaymentAccounts().isEmpty()) return new ArrayList<AccountJson>();
        return user.getPaymentAccounts().stream().map((PaymentAccount acc) -> {
            AccountJson account = new AccountJson();
            account.id = acc.getId();
            account.name = acc.getAccountName();
            account.paymentmethod = acc.getPaymentMethod().getId();
            account.currencies = acc.getTradeCurrencies().stream().map((a)->{
                return a.getCode();
            }).collect(toList());
            return account;
        }).collect(toList());
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get account detail")
    public Message accountDetail(

            @ApiParam(value = "The account id", required=true)
            @RequestParam(value = "id")
                    String id

    ) throws Exception {
        checkErrors();

        Message message = new Message();

        PaymentAccount account = user.getPaymentAccount(id);
        if(account != null){
            message.success = true;
            message.message = account.getAccountName();
            message.data = account.getPaymentAccountPayload();
        }else{
            message.success = false;
            message.message = "Account \""+id+"\" was not found.";
        }

        return message;
    }

    @RequestMapping(value = "/deleteAccount", method = RequestMethod.DELETE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Delete an existing account")
    public Message deleteAccount(

            @ApiParam(value = "The account id", required=true)
            @RequestParam(value = "id")
            String id

    ) throws Exception {
        checkErrors();

        Message message = new Message();

        PaymentAccount account = user.getPaymentAccount(id);
        if(account != null){
            user.removePaymentAccount(account);
            message.success = true;
            message.message = "Account \""+id+"\" was removed.";
        }else{
            message.success = false;
            message.message = "Account \""+id+"\" was not found.";
        }

        return message;
    }

    @RequestMapping(value = "/create/SEPA", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new SEPA payment account")
    public Message createSEPA(

            @ApiParam(value = "Account owner full name", required=true)
            @RequestParam(value = "holderName")
                    String holderName,

            @ApiParam(value = "IBAN code", required=true)
            @RequestParam(value = "iban")
                    String iban,

            @ApiParam(value = "BIC code", required=true)
            @RequestParam(value = "bic")
            String bic,

            @ApiParam(value = "Country code of bank (eg. \"IE\" for Ireland)", required=true)
            @RequestParam(value = "countryCode")
            String countryCode,

            @ApiParam(value = "Trading currency code (eg. \"EUR\" for Euro)", required=true)
            @RequestParam(value = "currencyCode")
            String currencyCode

    ) throws Exception {
        checkErrors();

        Message message = new Message();
        IBANValidator ibanValidator = new IBANValidator();
        BICValidator bicValidator = new BICValidator();
        InputValidator inputValidator = new InputValidator();

        SepaAccount account = new SepaAccount();
        account.init();

        FiatCurrency currency;
        if(CurrencyUtil.isFiatCurrency(currencyCode)){
            currency = new FiatCurrency(currencyCode);
        }else{
            message.success = false;
            message.message = "\""+currencyCode+"\" is not a valid currency code";
            return message;
        }

        Country country;
        List<Country> clist = CountryUtil.getAllSepaCountries().stream().filter((c) -> c.code.equals(countryCode)).collect(toList());
        if (!clist.isEmpty()) {
            country = clist.get(0);
        } else {
            message.success = false;
            message.message = "\""+countryCode+"\" is not a valid SEPA country code";
            return message;
        }
        if(!ibanValidator.validate(iban).isValid){
            message.success = false;
            message.message = "IBAN: "+ibanValidator.validate(iban).errorMessage;
            return message;
        };
        if(!bicValidator.validate(bic).isValid){
            message.success = false;
            message.message = "BIC: "+bicValidator.validate(bic).errorMessage;
            return message;
        };
        if(!inputValidator.validate(holderName).isValid){
            message.success = false;
            message.message = "HOLDERNAME: "+inputValidator.validate(holderName).errorMessage;
            return message;
        }

        account.setHolderName(holderName);
        account.setBic(bic);
        account.setIban(iban);
        account.setCountry(country);
        account.addCurrency(currency);
        account.setSelectedTradeCurrency(currency);
        String method = Res.get(account.getPaymentMethod().getId());
        account.setAccountName(method.concat(" (").concat(currency.getCode()).concat("/").concat(country.code).concat("): ").concat(iban));

        message.data = account.paymentAccountPayload;
        if(user.getPaymentAccounts().stream().filter((a)->a.getAccountName().equals(account.getAccountName())).collect(toList()).isEmpty()){

            user.addPaymentAccount(account);
            preferences.addFiatCurrency(currency);

            message.success = true;
            message.message = "New SEPA account \""+account.getAccountName()+"\" was created";
        }else{
            message.success = false;
            message.message = "Account with name \""+account.getAccountName()+"\" already exists";
        }

        return message;

    }
}
