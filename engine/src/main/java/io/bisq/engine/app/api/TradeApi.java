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


import io.bisq.business.formatters.Message;
import io.bisq.business.formatters.TradeData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import static io.bisq.business.Handlers.checkErrors;
import static io.bisq.business.actions.TradeActions.*;
import static org.springframework.util.MimeTypeUtils.*;

@RestController
@RequestMapping("/api/trade")
@Api(tags = {"Trade"})
public class TradeApi{

    @RequestMapping(value = "/list", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of all trades")
    public List<TradeData> ListTrades(

        @ApiParam(value = "Optionally filter by a trade ID")
        @RequestParam(value = "tradeId", required=false)
        String tradeId

    ) throws Exception {
        checkErrors();
        return listTrades(tradeId);
    }

    @RequestMapping(value = "/contract", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the contract for a trade")
    public Message GetContract(

        @ApiParam(value = "The trade offer id", required=true)
        @RequestParam(value = "tradeId")
        String tradeId

    ) throws Exception {
        checkErrors();
        return getContract(tradeId);
    }



    @RequestMapping(value = "/payment/started", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Indicate that payment has started on a trade")
    public Message PaymentStarted(

        @ApiParam(value = "The trade offer id", required=true)
        @RequestParam(value = "tradeId")
        String tradeId

    ) throws Exception {
        checkErrors();
        return paymentStarted(tradeId);
    }

    @RequestMapping(value = "/payment/received", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Indicate that payment for trade was received")
    public Message PaymentReceived(

        @ApiParam(value = "The trade offer id", required=true)
        @RequestParam(value = "tradeId")
        String tradeId

    ) throws Exception {
        checkErrors();
        return paymentReceived(tradeId);
    }

    @RequestMapping(value = "/payment/movetobisq", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Move trade funds to the BISQ wallet")
    public Message MoveToBisqWallet(

        @ApiParam(value = "The trade offer id", required=true)
        @RequestParam(value = "tradeId")
        String tradeId

    ) throws Exception {
        checkErrors();
        return moveToBisqWallet(tradeId);
    }
}
