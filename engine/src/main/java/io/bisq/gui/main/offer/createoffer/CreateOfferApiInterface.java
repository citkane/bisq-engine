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
import io.bisq.core.payment.PaymentAccount;
import io.bisq.engine.app.api.ApiData.Message;
import static io.bisq.engine.app.api.ApiData.*;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

public interface CreateOfferApiInterface {

    static Message getOffer(
            String paymentAccountId,
            String direction,
            BigDecimal Amount,
            BigDecimal MinAmount,
            String priceModel,
            BigDecimal price,
            boolean commit
    ) throws InterruptedException {
        Message message = new Message();

        CreateOfferDataModel createOffer = injector.getInstance(CreateOfferDataModel.class);



        PaymentAccount paymentAccount = user.getPaymentAccount(paymentAccountId);
        if(paymentAccount == null){
            message.success = false;
            message.message = "Payment account was not found";
            //setMessage(message);
            return message;
        }

        long amount = Amount.multiply(new BigDecimal(100000000)).longValue();
        Long minAmount = (MinAmount != null)?MinAmount.multiply(new BigDecimal(100000000)).longValue():null;
        OfferPayload.Direction dir = direction.equals("BUY")?OfferPayload.Direction.BUY:OfferPayload.Direction.SELL;

        if(paymentAccount.getTradeCurrencies().isEmpty()){
            message.success = false;
            message.message = "Could not find a currency for the account";
            //setMessage(message);
            return message;
        }else if(paymentAccount.getSelectedTradeCurrency() == null){
            paymentAccount.setSelectedTradeCurrency(paymentAccount.getTradeCurrencies().get(0));
        }


        TradeCurrency tradeCurrency = paymentAccount.getSelectedTradeCurrency();
        String priceCode = paymentAccount.getSelectedTradeCurrency().getCode();

        createOffer.setUseMarketBasedPrice(priceModel.equals("PERCENTAGE"));
        createOffer.setAmount(Coin.valueOf(amount));

        if(minAmount == null || minAmount == 0) minAmount = amount;
        createOffer.setMinAmount(Coin.valueOf(minAmount));

        if(!createOffer.isMinAmountLessOrEqualAmount()){
            message.success = false;
            message.message = "Minimum amount must be less than or equal to amount";
            //setMessage(message);
            return message;
        }

        if(priceModel.equals("PERCENTAGE")){

            if(priceFeedService.getMarketPrice(priceCode) == null || !priceFeedService.getMarketPrice(priceCode).isPriceAvailable()){
                message.success = false;
                message.message = "Could not get a price feed";
                //setMessage(message);
                return message;
            }

            double max = preferences.getMaxPriceDistanceInPercent();
            double margin = price.divide(new BigDecimal(100),5, RoundingMode.CEILING).doubleValue();
            if (Math.abs(margin) > max) {
                message.success = false;
                message.message = "Price margin is greater than set in the user preferences";
                //setMessage(message);
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
            //setMessage(message);
            return message;
        }

        Coin fee = offer.getMakerFee();
        if(dir.toString().equals("SELL")) fee = fee.add(offer.getAmount());
        if (rootView.AvailableBalance.isLessThan(fee)){
            message.success = false;
            message.message = "Insufficient funds for offer in wallet";
            //setMessage(message);
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

            /* TODO implement Futures instead of sleep */
            TimeUnit.SECONDS.sleep(2);
        }

        return message;
    }
}
