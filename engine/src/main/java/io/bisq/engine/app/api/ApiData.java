/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;

import io.bisq.common.Clock;
import io.bisq.common.crypto.KeyRing;
import io.bisq.core.alert.AlertManager;
import io.bisq.core.alert.PrivateNotificationManager;
import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.arbitration.ArbitratorManager;
import io.bisq.core.arbitration.DisputeManager;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.btc.wallet.WalletsManager;
import io.bisq.core.btc.wallet.WalletsSetup;
import io.bisq.core.dao.DaoManager;
import io.bisq.core.filter.FilterManager;
import io.bisq.core.offer.OpenOfferManager;
import io.bisq.core.payment.AccountAgeWitnessService;
import io.bisq.core.provider.fee.FeeService;
import io.bisq.core.provider.price.PriceFeedService;
import io.bisq.core.trade.TradeManager;
import io.bisq.core.trade.closed.ClosedTradableManager;
import io.bisq.core.trade.failed.FailedTradesManager;
import io.bisq.core.trade.statistics.TradeStatisticsManager;
import io.bisq.core.user.Preferences;
import io.bisq.core.user.User;
import io.bisq.gui.main.overlays.notifications.NotificationCenter;
import io.bisq.gui.util.BSFormatter;
import io.bisq.gui.util.validation.BICValidator;
import io.bisq.gui.util.validation.IBANValidator;
import io.bisq.gui.util.validation.InputValidator;
import io.bisq.network.crypto.EncryptionService;
import io.bisq.network.p2p.P2PService;
import io.bisq.core.offer.OfferBookService;
import io.bisq.engine.EngineBoot;
import io.bisq.gui.main.offer.offerbook.OfferBook;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.concurrent.ExecutorService;

import static io.bisq.engine.app.EngineAppMain.BisqEngine;


public class ApiData {

    public static WalletsManager walletsManager;
    public static WalletsSetup walletsSetup;
    public static BtcWalletService btcWalletService;
    public static PriceFeedService priceFeedService;
    public static ArbitratorManager arbitratorManager;
    public static P2PService p2PService;
    public static TradeManager tradeManager;
    public static OpenOfferManager openOfferManager;
    public static DisputeManager disputeManager;
    public static Preferences preferences;
    public static User user;
    public static AlertManager alertManager;
    public static PrivateNotificationManager privateNotificationManager;
    public static FilterManager filterManager;
    public static TradeStatisticsManager tradeStatisticsManager;
    public static NotificationCenter notificationCenter;
    public static Clock clock;
    public static FeeService feeService;
    public static DaoManager daoManager;
    public static EncryptionService encryptionService;
    public static KeyRing keyRing;
    public static BisqEnvironment bisqEnvironment;
    public static FailedTradesManager failedTradesManager;
    public static ClosedTradableManager closedTradableManager;
    public static AccountAgeWitnessService accountAgeWitnessService;
    public static BSFormatter formatter;
    public static OfferBookService offerBookService;
    public static OfferBook offerBook;
    public static EngineBoot rootView;
    public static ExecutorService exec = BisqEngine;


    //public static String threadName;

    public static Injector injector;

    public static class Message{
        public Boolean success;
        public String message;
        public Object data;
    };

    public static ObjectMapper Json = new ObjectMapper();

    public static void inject(Injector _injector){
        injector = _injector;
        walletsManager = injector.getInstance(WalletsManager.class);
        walletsSetup = injector.getInstance(WalletsSetup.class);
        btcWalletService = injector.getInstance(BtcWalletService.class);
        priceFeedService = injector.getInstance(PriceFeedService.class);
        arbitratorManager = injector.getInstance(ArbitratorManager.class);
        p2PService = injector.getInstance(P2PService.class);
        tradeManager = injector.getInstance(TradeManager.class);
        openOfferManager = injector.getInstance(OpenOfferManager.class);
        disputeManager = injector.getInstance(DisputeManager.class);
        preferences = injector.getInstance(Preferences.class);
        user = injector.getInstance(User.class);
        alertManager = injector.getInstance(AlertManager.class);
        privateNotificationManager = injector.getInstance(PrivateNotificationManager.class);
        filterManager = injector.getInstance(FilterManager.class);
        tradeStatisticsManager = injector.getInstance(TradeStatisticsManager.class);
        notificationCenter = injector.getInstance(NotificationCenter.class);
        clock = injector.getInstance(Clock.class);
        feeService = injector.getInstance(FeeService.class);
        daoManager = injector.getInstance(DaoManager.class);
        encryptionService = injector.getInstance(EncryptionService.class);
        keyRing = injector.getInstance(KeyRing.class);
        bisqEnvironment = injector.getInstance(BisqEnvironment.class);
        failedTradesManager = injector.getInstance(FailedTradesManager.class);
        closedTradableManager = injector.getInstance(ClosedTradableManager.class);
        accountAgeWitnessService = injector.getInstance(AccountAgeWitnessService.class);
        formatter = injector.getInstance(BSFormatter.class);
        offerBook = injector.getInstance(OfferBook.class);
        offerBookService = injector.getInstance(OfferBookService.class);

        rootView = injector.getInstance(EngineBoot.class);



    }

    public static class valid{
        public static IBANValidator ibanValidator = new IBANValidator();
        public static BICValidator bicValidator = new BICValidator();
        public static InputValidator inputValidator = new InputValidator();
    }

    /*Exception Handlers*/
    public static class error{
        @ResponseStatus(code = HttpStatus.NOT_FOUND)
        public static final class NotFound extends Exception {
            public NotFound() {}
        }

        @ResponseStatus(code = HttpStatus.BAD_REQUEST)
        public static final class BadRequest extends Exception {
            public BadRequest() {}
        }

        @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
        public static final class ServerError extends Exception {
            public ServerError() {}
        }
        @ResponseStatus(code = HttpStatus.PRECONDITION_REQUIRED, reason = "BISQ terms and conditions have not been accepted. hint: go to 'Preferences")
        public static final class TACerror extends Exception {
            public TACerror() {}
        }
    }

    public static void checkErrors() throws Exception {
        if(!preferences.isTacAccepted()) throw new error.TACerror();
        if(user.getPaymentAccounts() == null) throw new error.ServerError();
    }
}
