package io.bisq.engine.app.api;

import io.bisq.core.btc.AddressEntry;
import io.bisq.engine.EngineBoot;
import io.bisq.engine.app.util.Args;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.bitcoinj.core.Address;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.util.MimeTypeUtils.*;




@RestController
@RequestMapping("/api/wallet")
@Api(tags = {"Wallet"})
public class WalletApi extends Data{

    public static class BalanceJson{
        public ItemJson available = new ItemJson();
        public ItemJson reservedForOffers = new ItemJson();
        public ItemJson lockedInTrades = new ItemJson();
        private class ItemJson{
            public String longval;
            public String shortval;
        }
    }
    @RequestMapping(value = "/btcBalance", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the BTC wallet balance")
    public BalanceJson getBalance(){



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

    @RequestMapping(value = "/paymentAddress", method= RequestMethod.GET, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Get an address to fund the BTC wallet")
    public String getBtcAddress(){
        return btcWalletService.getOrCreateAddressEntry(AddressEntry.Context.AVAILABLE).getAddressString();
    }
}
