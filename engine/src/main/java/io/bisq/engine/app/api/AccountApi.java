package io.bisq.engine.app.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bisq.common.UserThread;
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
import org.bitcoinj.core.Coin;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        public JSONObject limits;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of payment accounts")
    public static List<AccountJson> PaymentAccounts() throws Exception {
        checkErrors();

        class Limit{
            public double max;
            public double min;
        }

        if (user.getPaymentAccounts().isEmpty()) return new ArrayList<AccountJson>();
        return user.getPaymentAccounts().stream().map((PaymentAccount acc) -> {
            AccountJson account = new AccountJson();
            account.id = acc.getId();
            account.name = acc.getAccountName();
            account.paymentmethod = acc.getPaymentMethod().getId();
            account.currencies = acc.getTradeCurrencies().stream().map((a)->{
                return a.getCode();
            }).collect(toList());
            HashMap<String,Limit> limit = new HashMap<>();
            acc.getTradeCurrencies().stream().forEach((curr)->{
                Coin Max = Coin.valueOf(accountAgeWitnessService.getMyTradeLimit(acc,curr.getCode()));
                Limit l = new Limit();
                l.max= Double.parseDouble(Max.toPlainString());
                l.min = 0.001; // TODO how to get min trade amount from account?
                limit.put(curr.getCode(),l);
            });
            account.limits = new JSONObject(limit);
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

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()-> {
            PaymentAccount account = user.getPaymentAccount(id);
            if (account != null) {
                user.removePaymentAccount(account);
                message.success = true;
                message.message = "Account \"" + id + "\" was removed.";
            } else {
                message.success = false;
                message.message = "Account \"" + id + "\" was not found.";
            }
            promise.complete(message);
        });

        return promise.get();
    }


}
