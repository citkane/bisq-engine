/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.gui.main.offer.createoffer;

import static com.google.common.base.Preconditions.checkNotNull;

import io.bisq.common.UserThread;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface CreateOfferApiInterface {

    static Message getOffer(
            String paymentAccountId,
            String direction,
            BigDecimal Amount,
            BigDecimal MinAmount,
            String priceModel,
            BigDecimal tPrice,
            boolean commit
    ) throws InterruptedException, ExecutionException {
        final Message message = new Message();

        CreateOfferDataModel createOffer = injector.getInstance(CreateOfferDataModel.class);

        PaymentAccount paymentAccount = user.getPaymentAccount(paymentAccountId);
        if(paymentAccount == null){
            message.success = false;
            message.message = "Payment account was not found";
            return message;
        }

        if(MinAmount == null) MinAmount = BigDecimal.ZERO;
        final long amount = Amount.multiply(new BigDecimal(100000000)).longValue();
        long minAmountTemp = MinAmount.multiply(new BigDecimal(100000000)).longValue();
        if(minAmountTemp  == 0) minAmountTemp  = amount;
        final long minAmount = minAmountTemp;

        System.out.println("--------------------------"+minAmount+"-----------------------------------------");

        OfferPayload.Direction dir = direction.equals("BUY")?OfferPayload.Direction.BUY:OfferPayload.Direction.SELL;

        if(paymentAccount.getTradeCurrencies().isEmpty()){
            message.success = false;
            message.message = "Could not find a currency for the account";
            return message;
        };
        if(!createOffer.isMinAmountLessOrEqualAmount()){
            message.success = false;
            message.message = "Minimum amount must be less than or equal to amount";
            return message;
        }

        boolean fiat = !paymentAccount.getPaymentAccountPayload().getPaymentMethodId().matches("BLOCK_CHAINS");
        if(!fiat && !priceModel.equals("PERCENTAGE")){
            String foo = reciprocal(String.valueOf(tPrice));
            tPrice = new BigDecimal(foo);
        }
        BigDecimal price = tPrice;

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()->{

            TradeCurrency tradeCurrency = paymentAccount.getSelectedTradeCurrency();
            String priceCode = paymentAccount.getSelectedTradeCurrency().getCode();

            createOffer.setUseMarketBasedPrice(priceModel.equals("PERCENTAGE"));
            createOffer.setAmount(Coin.valueOf(amount));
            createOffer.setMinAmount(Coin.valueOf(minAmount));

            if(priceModel.equals("PERCENTAGE")){

                if(priceFeedService.getMarketPrice(priceCode) == null || !priceFeedService.getMarketPrice(priceCode).isPriceAvailable()){
                    message.success = false;
                    message.message = "Could not get a price feed";
                    promise.complete(message);
                    return;
                }

                double max = preferences.getMaxPriceDistanceInPercent();
                double margin = price.divide(new BigDecimal(100),8, RoundingMode.CEILING).doubleValue();
                if (Math.abs(margin) > max) {
                    message.success = false;
                    message.message = "Price margin is greater than set in the user preferences";
                    promise.complete(message);
                    return;
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



            if(offer.getAmount().compareTo(offer.getPaymentMethod().getMaxTradeLimitAsCoin(offer.getCurrencyCode())) > 0){
                message.success = false;
                message.message = "Amount is larger than " + offer.getPaymentMethod().getMaxTradeLimitAsCoin(offer.getCurrencyCode()).toFriendlyString();
                promise.complete(message);
                return;
            }

            Coin fee = offer.getMakerFee();
            if(dir.toString().equals("SELL")) fee = fee.add(offer.getAmount());
            if (rootView.AvailableBalance.isLessThan(fee)){
                message.success = false;
                message.message = "Insufficient funds for offer in wallet";
                promise.complete(message);
                return;
            }
            message.data = offer;
            if(commit){
                checkNotNull(createOffer.getMakerFee(), "makerFee must not be null");
                createOffer.estimateTxSize();
                Coin reservedFundsForOffer = createOffer.getSecurityDeposit();
                if (!createOffer.isBuyOffer())
                    reservedFundsForOffer = reservedFundsForOffer.add(createOffer.getAmount().get());

                Coin finalReservedFundsForOffer = reservedFundsForOffer;

                openOfferManager.placeOffer(offer, finalReservedFundsForOffer,true,(mess)->{
                    message.success = true;
                    message.message = "Offer was successfully placed";
                    promise.complete(message);
                },(err)->{
                    message.success = false;
                    message.message = err;
                    promise.complete(message);
                });

            }else{
                message.success = true;
                message.message = "Offer is valid but NOT committed";
                promise.complete(message);
            }
        });

        return promise.get();
    }
}
