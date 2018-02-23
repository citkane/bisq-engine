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


}
