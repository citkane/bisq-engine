package io.bisq.gui.main.offer.takeoffer;

import static io.bisq.engine.app.api.ApiData.*;
import static java.util.stream.Collectors.toList;

import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.offer.Offer;
import io.bisq.core.payment.PaymentAccount;
import io.bisq.core.trade.protocol.ProcessModel;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface TakeOfferApiInterface {

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
        Long am = (Amount != null)?Amount.multiply(new BigDecimal(100000000)).longValue():offer.getAmount().longValue();
        Coin amount = Coin.valueOf(am);

        take.initWithData(offer);
        take.onPaymentAccountSelected(account);
        take.applyAmount(amount);
        take.fundFromSavingsWallet();

        if(!take.hasAcceptedArbitrators()){
            message.message = "No accepted arbitrators found";
            return message;
        }
        if(!take.isMinAmountLessOrEqualAmount()){
            message.message = "Amount is less than the acceptable minimum";
            return message;
        }
        if(take.isAmountLargerThanOfferAmount()){
            message.message = "Amount is larger than the offer amount";
            return message;
        }
        if(take.wouldCreateDustForMaker()){
            message.message = "Trade would create dust for the maker";
            return message;
        }

        if(!take.isTakerFeeValid()){
            message.message = "Taker fee is not valid";
            return message;
        }

        take.onTakeOffer((trade) -> {
            take.message.success = true;
            take.message.message = "You successfully accepted the offer";

        });

        /* TODO implement Futures instead of sleep */
        TimeUnit.SECONDS.sleep(2);
        return take.message;
    }
}
