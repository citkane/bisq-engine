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

import io.bisq.business.formatters.BalanceData;
import io.bisq.business.formatters.Message;
import io.bisq.business.formatters.TransactionData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import static io.bisq.business.Handlers.checkErrors;
import static io.bisq.business.actions.WalletActions.*;
import static org.springframework.util.MimeTypeUtils.*;

@RestController
@RequestMapping("/api/wallet")
@Api(tags = {"Wallet"})
public class WalletApi {

    @RequestMapping(value = "/btcBalance", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the BTC wallet balance")
    public BalanceData GetBalance() throws Exception {
        checkErrors();
        return getBalance();
    }

    @RequestMapping(value = "/paymentAddress", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get an address to fund the BTC wallet")
    public Message GetFundingAddress() throws Exception {
        checkErrors();
        return getFundingAddress();
    }

    @RequestMapping(value = "/transactions/list", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of wallet transactions")
    public List<TransactionData> GetTransactions() throws Exception {
        checkErrors();
        return getTransactions();

    }
}
