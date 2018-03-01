package io.bisq.gui.main.offer.takeoffer;

import static io.bisq.engine.app.api.ApiData.*;
import static java.util.stream.Collectors.toList;

import io.bisq.common.UserThread;
import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.offer.Offer;
import io.bisq.core.payment.PaymentAccount;
import io.bisq.core.payment.PaymentAccountUtil;
import io.bisq.core.trade.protocol.ProcessModel;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public interface TakeOfferApiInterface {

    static Coin getTxFee(Offer offer) throws ExecutionException, InterruptedException {
        TakeOfferDataModel take = injector.getInstance(TakeOfferDataModel.class);

        CompletableFuture<Coin> promise = new CompletableFuture<>();
        feeService.requestFees(()->{
            take.txFeePerByteFromFeeService = feeService.getTxFeePerByte();
            take.txFeeFromFeeService = take.getTxFeeBySize(take.feeTxSize);
            promise.complete(take.getTotalTxFee());
        },null);

        return promise.get();
    }

    static Message takeOffer(String offerId, String accountId, BigDecimal Amount) throws InterruptedException, ExecutionException {


        TakeOfferDataModel take = injector.getInstance(TakeOfferDataModel.class);

        Message message = new Message();
        Offer offer;
        PaymentAccount account;

        List<Offer> oList = offerBookService.getOffers().stream().filter(
                o->o.getId().equals(offerId)
        ).collect(toList());

        if(oList.isEmpty()){
            message.success = false;
            message.message = "Offer with id "+offerId+" was not found";
            return message;
        }else{
            offer = oList.get(0);
        }

        if(offer.isMyOffer(keyRing)){
            message.success = false;
            message.message = "Offer is your own";
            return message;
        }
        if(user.getPaymentAccounts().isEmpty()){
            message.success = false;
            message.message = "No available payment accounts were found";
            return message;
        }
        List<PaymentAccount> aList = user.getPaymentAccounts().stream().filter(
                a->a.getId().equals(accountId)
        ).collect(toList());

        if(aList.isEmpty()){
            message.success = false;
            message.message = "Account with id "+accountId+" was not found";
            return message;
        }else{
            account = aList.get(0);
        }
        if(!PaymentAccountUtil.isPaymentAccountValidForOffer(offer, account)){
            message.success = false;
            message.message = "Payment account is not valid for the offer";
            return message;
        }

        if (Amount == null) Amount = new BigDecimal(offer.getAmount().longValue());

        Long am = Amount.multiply(new BigDecimal(100000000)).longValue();
        Coin amount = Coin.valueOf(am);

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()-> {
            take.initWithData(offer);
            take.onPaymentAccountSelected(account);
            take.applyAmount(amount);
            take.fundFromSavingsWallet();


            message.success = false;

            if(!take.hasAcceptedArbitrators()){
                message.message = "No accepted arbitrators found";
                promise.complete(message);
                return;
            }
            if(!take.isMinAmountLessOrEqualAmount()){
                message.message = "Amount is less than the acceptable minimum";
                promise.complete(message);
                return;
            }
            if(take.isAmountLargerThanOfferAmount()){
                message.message = "Amount is larger than the offer amount";
                promise.complete(message);
                return;
            }
            if(take.wouldCreateDustForMaker()){
                message.message = "Trade would create dust for the maker";
                promise.complete(message);
                return;
            }
            if(!take.isTakerFeeValid()){
                message.message = "Taker fee is not valid";
                promise.complete(message);
                return;
            }
            take.onTakeOffer((trade) -> {
                if(take.message.success == null){
                    take.message.success = true;
                    take.message.message = "You successfully accepted the offer";
                }
                promise.complete(take.message);
            });
            if(take.message.success == false){
                promise.complete(take.message);
            }
        });

        return promise.get();
    }
}
