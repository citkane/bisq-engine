/*
 * Copyright (c) 2018. Michael Jonker (http://openpoint.ie)
 *
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.engine.app.api;

import io.bisq.business.formatters.AccountData;
import io.bisq.business.formatters.Message;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import static io.bisq.business.Handlers.checkErrors;
import static io.bisq.business.actions.AccountActions.*;
import static org.springframework.util.MimeTypeUtils.*;

@RestController
@RequestMapping("/api/account")
@Api(tags = {"Account"})
public class AccountApi {

    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of payment accounts")
    public static List<AccountData> GetPaymentAccounts() throws Exception {
        checkErrors();
        return getPaymentAccounts();
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get account detail")
    public Message AccountDetail(

            @ApiParam(value = "The account id", required=true)
            @RequestParam(value = "id")
                    String id

    ) throws Exception {
        checkErrors();
        return accountDetail(id);
    }

    @RequestMapping(value = "/deleteAccount", method = RequestMethod.DELETE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Delete an existing account")
    public Message DeleteAccount(

            @ApiParam(value = "The account id", required=true)
            @RequestParam(value = "id")
            String id

    ) throws Exception {
        checkErrors();
        return deleteAccount(id);
    }
}
