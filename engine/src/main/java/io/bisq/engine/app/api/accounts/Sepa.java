package io.bisq.engine.app.api.accounts;

import io.bisq.common.locale.*;
import io.bisq.core.payment.SepaAccount;
import io.bisq.engine.app.api.ApiData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/account/create")
@Api(tags = {"Account"})
public class Sepa extends ApiData {

    @RequestMapping(value = "/SEPA", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
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
        if(!valid.ibanValidator.validate(iban).isValid){
            message.success = false;
            message.message = "IBAN: "+valid.ibanValidator.validate(iban).errorMessage;
            return message;
        };
        if(!valid.bicValidator.validate(bic).isValid){
            message.success = false;
            message.message = "BIC: "+valid.bicValidator.validate(bic).errorMessage;
            return message;
        };
        if(!valid.inputValidator.validate(holderName).isValid){
            message.success = false;
            message.message = "HOLDERNAME: "+valid.inputValidator.validate(holderName).errorMessage;
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
        if(
                user.getPaymentAccounts().isEmpty() ||
                user.getPaymentAccounts().stream().filter((a)->a.getAccountName().equals(account.getAccountName())).collect(toList()).isEmpty()
        ){
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
