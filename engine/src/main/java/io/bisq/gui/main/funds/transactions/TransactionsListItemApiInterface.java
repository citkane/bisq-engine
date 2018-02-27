package io.bisq.gui.main.funds.transactions;

import com.google.gson.JsonObject;
import io.bisq.core.trade.Tradable;
import io.bisq.engine.app.api.WalletApi.TransactionItem;
import org.bitcoinj.core.Transaction;

import java.util.Optional;

import static io.bisq.engine.app.api.ApiData.*;

public interface TransactionsListItemApiInterface {
    static TransactionItem getTransaction(Transaction transaction, Optional<Tradable> tradableOptional){
        TransactionsListItem item = new TransactionsListItem(transaction,btcWalletService,bsqWalletService,tradableOptional,formatter);
        TransactionItem Item = new TransactionItem();
        Item.date = item.getDate();
        Item.details = item.getDetails();
        Item.received = item.getReceived();
        Item.direction = item.getDirection();
        Item.address = item.getAddressString();
        Item.id = item.getTxId();
        Item.amount = item.getAmount();
        Item.confirmations = item.getNumConfirmations();

        return Item;
    }
}
