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
import io.bisq.business.formatters.OfferData;
import io.bisq.business.formatters.OfferData.*;
import static io.bisq.business.Handlers.checkErrors;
import static io.bisq.business.actions.OfferActions.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.math.BigDecimal;
import java.util.List;
import static org.springframework.util.MimeTypeUtils.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offers")
@Api(tags = {"Offers"})
public class OfferApi {

    @RequestMapping(value = "/list", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of all open offers")
    public List<OfferData> ListOpenOffers(

        @ApiParam(value = "Optionally filter by currency code")
        @RequestParam(value = "currency", required=false)
        String currency

    ) throws Exception {
        checkErrors();
        return listOpenOffers(currency);
    }

    @RequestMapping(value = "/detail", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get details of a single offer")
    public Message OfferById(
            @ApiParam(value = "The offer id", required=true)
            @RequestParam(value = "offerId")
            String offerId
    ) throws Exception {
        checkErrors();
        return offerById(offerId);

    }

    @RequestMapping(value = "/fees", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get fees for an offer")
    public FeesData GetFees(
            @ApiParam(value = "The offer id", required=true)
            @RequestParam(value = "offerId")
                    String offerId
    ) throws Exception {
        checkErrors();
        return getFees(offerId);
    }

    @RequestMapping(value = "/offerTake", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Accept an offer")
    public Message TakeOffer(
	    @ApiParam(value = "The offer id", required=true)
        @RequestParam(value = "offerId")
        String offerId,

	    @ApiParam(value = "The id of the payment account.", required=true)
        @RequestParam(value = "accountId")
        String accountId,

        @ApiParam(value = "The trade amount in base units (eg. <0.1> for 0.1 BTC). Will set to offer maximum if left blank")
        @RequestParam(value = "Amount", required = false)
        BigDecimal Amount

    ) throws Exception {
        checkErrors();
        return takeOffer(offerId, accountId, Amount);
    }


    @RequestMapping(value = "/offerMake", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new offer.")
    public Message CreateOffe(

        @ApiParam(value = "The id of the payment account.", required=true)
        @RequestParam(value = "paymentAccountId")
        String paymentAccountId,

        @ApiParam(value = "The offer direction", allowableValues = "BUY,SELL", required=true)
        @RequestParam(value = "direction")
        String direction,

        @ApiParam(value = "The trade amount in base units (eg. <0.1> for 0.1 BTC).", required=true)
        @RequestParam(value = "amount")
        BigDecimal Amount,

        @ApiParam(value = "The trade minimum amount base units (eg. <0.1> for 0.1 BTC).")
        @RequestParam(value = "minAmount", required = false)
        BigDecimal MinAmount,

        @ApiParam(value = "The price model to use", allowableValues = "FIXED,PERCENTAGE", required=true)
        @RequestParam(value = "priceModel")
        String priceModel,

        @ApiParam(value = "The offer price, depending on the model (eg. <9747.23> for €9747.23 or <1.2> for 1.2% from the market)", required=true)
        @RequestParam(value = "price")
        BigDecimal price,

        @ApiParam(value = "Should the new offer be committed?", required=true)
        @RequestParam(value = "commit", defaultValue = "false")
        boolean commit

    ) throws Exception {
        checkErrors();
        return createOffer(paymentAccountId,direction,Amount,MinAmount,priceModel,price,commit);
    }

    @RequestMapping(value = "/offerCancel", method= RequestMethod.DELETE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Cancel one of your own offers.")
    public Message offerCancel(
            @ApiParam(value = "The id of the offer", required=true)
            @RequestParam(value = "offerId")
            String offerId
    ) throws Exception {
        checkErrors();
        return removeOffer(offerId);
    }
}
