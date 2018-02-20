/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;

import static org.springframework.util.MimeTypeUtils.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Api(tags = {"User"})
public class UserApi extends ApiData {

    public final class UserJson{
        public String id;
        public List<AccountApi.AccountJson> paymentAccounts;
    }


    @RequestMapping(value = "/", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get details of the user")
    public UserJson User() throws Exception {
        checkErrors();

        UserJson thisUser = new UserJson();
        thisUser.id = user.getAccountId();
        thisUser.paymentAccounts = AccountApi.PaymentAccounts();
        return thisUser;
    }

}
