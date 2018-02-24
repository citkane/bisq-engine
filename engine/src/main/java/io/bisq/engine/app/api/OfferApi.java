/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.api;

import io.bisq.core.offer.Offer;
import io.bisq.core.offer.OfferPayload;
import io.bisq.core.offer.OpenOffer;
import io.bisq.gui.main.offer.createoffer.CreateOfferApiInterface;
import static io.bisq.gui.main.offer.createoffer.CreateOfferApiInterface.*;
import io.bisq.gui.main.offer.takeoffer.TakeOfferApiInterface;
import static io.bisq.gui.main.offer.takeoffer.TakeOfferApiInterface.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.MimeTypeUtils.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offers")
@Api(tags = {"Offers"})
public class OfferApi extends ApiData implements CreateOfferApiInterface, TakeOfferApiInterface {

    public static class OfferJson{
        public String id;
        public long date;
        public String paymentMethod;
        public String direction;
        public Currencies currencies = new Currencies();
        public Money money = new Money();
        public Boolean isMyOffer;
        public String buyerSecurityDeposit;
        public String makerFee;
        public String makerNodeAddress;

        public String error;

        public class Currencies{
            public String currency;
            public String counterCurrency;
            public String baseCurrency;
        };
        public class Money{
            public String amount;
            public String minAmount;
            public Boolean useMarketPrice;
            public String marketPriceMargin;
            public String price;
            public String volume;
        };
    }

    private OfferJson Map(Offer offer){
        OfferPayload op = offer.getOfferPayload();
        OfferJson offr = new OfferJson();

        offr.id = op.getId();
        offr.date = op.getDate();
        offr.currencies.currency = op.getCurrencyCode();
        offr.currencies.counterCurrency = op.getCounterCurrencyCode();
        offr.currencies.baseCurrency = op.getBaseCurrencyCode();
        offr.paymentMethod = op.getPaymentMethodId();
        offr.direction = offer.getDirection().toString();
        offr.money.amount = offer.getAmount().toPlainString();
        offr.money.minAmount = offer.getMinAmount().toPlainString();
        offr.money.useMarketPrice = op.isUseMarketBasedPrice();
        offr.money.marketPriceMargin = formatter.formatToPercent(offer.getMarketPriceMargin());
        offr.money.price = formatter.formatPrice(offer.getPrice());
        offr.money.volume = formatter.formatVolume(offer.getVolume());
        offr.isMyOffer = offer.isMyOffer(keyRing);
        offr.buyerSecurityDeposit = offer.getBuyerSecurityDeposit().toPlainString();
        offr.makerFee = offer.getMakerFee().toPlainString();
        offr.makerNodeAddress = offer.getMakerNodeAddress().getFullAddress();
        offr.error = offer.getErrorMessage();

        return offr;
    }

    @RequestMapping(value = "/list", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of all open offers")
    public List<OfferJson> listOffers(
        @RequestParam(value = "Optionally filter by currency code", required=false)
        String currency
    ) throws Exception {
        checkErrors();

        List<OfferJson> list = offerBookService.getOffers().stream().filter(
            offer->currency == null || offer.getCurrencyCode().equals(currency)
        ).map(this::Map).collect(toList());
        return list;
    }

    @RequestMapping(value = "/detail", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get details of a single offer")
    public OfferJson offerById(
            @RequestParam(value = "The offer id", required=true)
            String offerId
    ) throws Exception {
        checkErrors();

        List<OfferJson> list = offerBookService.getOffers().stream().filter(
                offer->offer.getId().equals(offerId)
        ).map(this::Map).collect(toList());

        if(list.isEmpty())
            throw new error.NotFound();

        return list.get(0);
    }

    @RequestMapping(value = "/offerTake", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Accept an offer")
    public Message offerTake(
            @RequestParam(value = "The offer id", required=true)
            String offerId,

            @RequestParam(value = "The payment account id", required=true)
            String accountId,

            @ApiParam(value = "The trade amount in base units (eg. <0.1> for 0.1 BTC). Will set to offer maximum if left blank")
            @RequestParam(value = "Amount", required = false)
            BigDecimal Amount

    ) throws Exception {
        checkErrors();

        return takeOffer(offerId,accountId,Amount);

    }


    @RequestMapping(value = "/offerMake", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new offer.")
    public Message makeOffer(

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


        Message message = getOffer(paymentAccountId,direction,Amount,MinAmount,priceModel,price,commit);

        if(message.data != null){
            message.data = Map((Offer) message.data);
        }

        return message;
    }


    @RequestMapping(value = "/offerCancel", method= RequestMethod.DELETE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Cancel one of your own offers.")
    public Message offerCancel(
            @ApiParam(value = "The id of the offer", required=true)
            @RequestParam(value = "offerId")
                    String offerId
    ) throws Exception {
        checkErrors();

        Message message = new Message();
        Optional<OpenOffer> toDelete = openOfferManager.getOpenOfferById(offerId);
        if(!toDelete.isPresent()){
            message.message = "Offer "+offerId+" is not available for deletion.";
            message.success = false;
            return message;
        }
        OpenOffer Delete = toDelete.get();
        openOfferManager.removeOpenOffer(Delete,()->{
            message.message = "Offer " + offerId + " was removed.";
        },(err)->{
            message.message = "Error: "+err;
            message.success = false;
        });
        return message;
    }
}