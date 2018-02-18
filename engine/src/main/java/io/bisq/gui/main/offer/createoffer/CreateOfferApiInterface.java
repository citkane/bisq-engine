/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.gui.main.offer.createoffer;

import static com.google.common.base.Preconditions.checkNotNull;
import io.bisq.common.locale.TradeCurrency;
import io.bisq.common.monetary.Price;
import io.bisq.core.offer.Offer;
import io.bisq.core.offer.OfferPayload;
import io.bisq.core.offer.OpenOfferManager;
import io.bisq.core.payment.AccountAgeWitnessService;
import io.bisq.core.payment.PaymentAccount;
import io.bisq.core.provider.price.PriceFeedService;
import io.bisq.core.trade.handlers.TransactionResultHandler;
import io.bisq.core.user.Preferences;
import io.bisq.engine.app.api.Data.Message;
import static io.bisq.engine.app.api.Data.injector;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;

public interface CreateOfferApiInterface {

    static Message getOffer(
            PaymentAccount paymentAccount,
            long amount,
            Long minAmount,
            String priceModel,
            BigDecimal price,
            OfferPayload.Direction dir,
            boolean commit
    ){
        Message message = new Message();

        CreateOfferDataModel createOffer = injector.getInstance(CreateOfferDataModel.class);
        Preferences preferences = injector.getInstance(Preferences.class);
        PriceFeedService priceFeedService = injector.getInstance(PriceFeedService.class);
        OpenOfferManager openOfferManager = injector.getInstance(OpenOfferManager.class);
        AccountAgeWitnessService accountAgeWitnessService = injector.getInstance(AccountAgeWitnessService.class);
        TradeCurrency tradeCurrency = paymentAccount.getSelectedTradeCurrency();
        String priceCode = paymentAccount.getSelectedTradeCurrency().getCode();

        createOffer.setUseMarketBasedPrice(priceModel.equals("PERCENTAGE")?true:false);
        createOffer.setAmount(Coin.valueOf(amount));

        if(minAmount == null || minAmount == 0) minAmount = amount;
        createOffer.setMinAmount(Coin.valueOf(minAmount));

        if(!createOffer.isMinAmountLessOrEqualAmount()){
            message.success = false;
            message.message = "Minimum amount must be less than or equal to amount";
            return message;
        }

        if(priceModel.equals("PERCENTAGE")){

            if(!priceFeedService.getMarketPrice(priceCode).isPriceAvailable()){
                message.success = false;
                message.message = "Could not get a price feed";
                return message;
            }

            double max = preferences.getMaxPriceDistanceInPercent();
            double margin = price.divide(new BigDecimal(100)).doubleValue();
            if (Math.abs(margin) > max) {
                message.success = false;
                message.message = "Price margin is greater than set in the user preferences";
                return message;
            }
            createOffer.setMarketPriceAvailable(true);
            createOffer.setMarketPriceMargin(margin);

            double P = priceFeedService.getMarketPrice(priceCode).getPrice();
            createOffer.setPrice(Price.parse(priceCode,String.valueOf(P)));
        }else{
            createOffer.setMarketPriceMargin(0);
            createOffer.setPrice(Price.parse(priceCode,price.toString()));
        }

        preferences.setSelectedPaymentAccountForCreateOffer(paymentAccount);
        createOffer.initWithData(dir, tradeCurrency);
        Offer offer = createOffer.createAndGetOffer();

        message.data = offer;

        if(offer.getAmount().compareTo(offer.getPaymentMethod().getMaxTradeLimitAsCoin(offer.getCurrencyCode())) > 0){
            message.success = false;
            message.message = "Amount is larger than " + offer.getPaymentMethod().getMaxTradeLimitAsCoin(offer.getCurrencyCode()).toFriendlyString();
            return message;
        }

        if(commit){
            checkNotNull(createOffer.getMakerFee(), "makerFee must not be null");
            createOffer.estimateTxSize();
            Coin reservedFundsForOffer = createOffer.getSecurityDeposit();
            if (!createOffer.isBuyOffer())
                reservedFundsForOffer = reservedFundsForOffer.add(createOffer.getAmount().get());

            openOfferManager.placeOffer(offer,reservedFundsForOffer,true,(mess)->{
                message.success = true;
                message.message = "Offer was successfully placed";
            },(err)->{
                message.success = false;
                message.message = err;
            });
        }
        return message;
    }
}
