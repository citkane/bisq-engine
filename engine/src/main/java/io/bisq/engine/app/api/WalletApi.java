package io.bisq.engine.app.api;

import com.google.gson.JsonObject;
import io.bisq.core.btc.AddressEntry;
import io.bisq.core.offer.OpenOffer;
import io.bisq.core.trade.Tradable;
import io.bisq.core.trade.Trade;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.bitcoinj.core.Address;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.bisq.gui.main.funds.transactions.TransactionsListItemApiInterface.getTransaction;
import static org.springframework.util.MimeTypeUtils.*;
import io.bisq.gui.main.funds.transactions.TransactionsListItemApiInterface;



@RestController
@RequestMapping("/api/wallet")
@Api(tags = {"Wallet"})
public class WalletApi extends ApiData {

    public static class BalanceJson{
        public ItemJson available = new ItemJson();
        public ItemJson reservedForOffers = new ItemJson();
        public ItemJson lockedInTrades = new ItemJson();
        private class ItemJson{
            public String longval;
            public String shortval;
        }
    }

    public static class TransactionItem{
        public Date date;
        public String details;
        public Boolean received;
        public String direction;
        public String address;
        public String id;
        public String amount;
        public String confirmations;
    }

    @RequestMapping(value = "/btcBalance", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the BTC wallet balance")
    public BalanceJson getBalance() throws Exception {
        checkErrors();

        BalanceJson balance = new BalanceJson();
        rootView.updateBalance();

        balance.available.longval = rootView.AvailableBalance.toFriendlyString();
        balance.available.shortval = rootView.AvailableBalance.toPlainString();

        balance.reservedForOffers.longval = rootView.ReservedBalance.toFriendlyString();
        balance.reservedForOffers.shortval = rootView.ReservedBalance.toPlainString();

        balance.lockedInTrades.longval = rootView.LockedBalance.toFriendlyString();
        balance.lockedInTrades.shortval = rootView.LockedBalance.toPlainString();

        return balance;
    }

    @RequestMapping(value = "/paymentAddress", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get an address to fund the BTC wallet")
    public Message getBtcAddress() throws Exception {
        checkErrors();

        Message message = new Message();
        message.success = true;
        message.message = "Payment address to fund BISQ trading wallet";
        message.data = btcWalletService.getOrCreateAddressEntry(AddressEntry.Context.AVAILABLE).getAddressString();
        return message;
    }

    @RequestMapping(value = "/transactions/list", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a list of wallet transactions")
    public List<TransactionItem> getSavings() throws Exception {
        checkErrors();

        Stream<Tradable> concat1 = Stream.concat(openOfferManager.getObservableList().stream(), tradeManager.getTradableList().stream());
        Stream<Tradable> concat2 = Stream.concat(concat1, closedTradableManager.getClosedTradables().stream());
        Stream<Tradable> concat3 = Stream.concat(concat2, failedTradesManager.getFailedTrades().stream());
        Set<Tradable> all = concat3.collect(Collectors.toSet());

        return btcWalletService.getTransactions(false).stream().map(transaction->{
            Optional<Tradable> tradableOptional = all.stream().filter(tradable -> {
                String txId = transaction.getHashAsString();
                if (tradable instanceof OpenOffer)
                    return tradable.getOffer().getOfferFeePaymentTxId().equals(txId);
                else if (tradable instanceof Trade) {
                    Trade trade = (Trade) tradable;
                    boolean isTakeOfferFeeTx = txId.equals(trade.getTakerFeeTxId());
                    boolean isOfferFeeTx = trade.getOffer() != null &&
                            txId.equals(trade.getOffer().getOfferFeePaymentTxId());
                    boolean isDepositTx = trade.getDepositTx() != null &&
                            trade.getDepositTx().getHashAsString().equals(txId);
                    boolean isPayoutTx = trade.getPayoutTx() != null &&
                            trade.getPayoutTx().getHashAsString().equals(txId);

                    boolean isDisputedPayoutTx = disputeManager.getDisputesAsObservableList().stream().anyMatch(
                            dispute -> txId.equals(dispute.getDisputePayoutTxId()) && tradable.getId().equals(dispute.getTradeId())
                    );

                    return isTakeOfferFeeTx || isOfferFeeTx || isDepositTx || isPayoutTx || isDisputedPayoutTx;
                } else
                    return false;
            }).findAny();
            return getTransaction(transaction,tradableOptional);
        }).collect(Collectors.toList());

    }
}
