/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.api;

import io.bisq.core.payment.PaymentAccount;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Api(tags = {"User"})
public class UserApi extends Data{
    
    public final class UserJson{
        public String id;
        public List<AccountJson> paymentAccounts;  
    }
    public final class AccountJson{
        public String name;
        public String id;
        public String paymentmethod;  
    }
    
    @RequestMapping(value = "/", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get details of the user")
    public UserJson User(){
        UserJson thisUser = new UserJson();
        thisUser.id = user.getAccountId();
        thisUser.paymentAccounts = PaymentAccounts();
        return thisUser;
    }
    
    @RequestMapping(value = "/PaymentAccounts", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get details of the user's payment accounts")
    public List<AccountJson> PaymentAccounts(){
        return user.getPaymentAccounts().stream().map((PaymentAccount acc)->{
            AccountJson account = new AccountJson();
            account.id = acc.getId();
            account.name = acc.getAccountName();
            account.paymentmethod = acc.getPaymentMethod().getId();
            return account;
        }).collect(toList());
    }
}
