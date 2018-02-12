/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine;

import com.google.inject.Inject;
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
import io.bisq.gui.components.BalanceWithConfirmationTextField;
import io.bisq.gui.components.TxIdTextField;
import io.bisq.gui.main.overlays.notifications.NotificationCenter;
import io.bisq.gui.main.overlays.windows.WalletPasswordWindow;
import io.bisq.gui.util.BSFormatter;
import io.bisq.network.crypto.EncryptionService;
import io.bisq.network.p2p.P2PService;

public class HeadlessConstructor extends EngineBoot{
    @SuppressWarnings("WeakerAccess")
    @Inject
    public HeadlessConstructor(
            WalletsManager walletsManager, 
            WalletsSetup walletsSetup,
            BtcWalletService btcWalletService, 
            PriceFeedService priceFeedService,
            ArbitratorManager arbitratorManager, 
            P2PService p2PService, 
            TradeManager tradeManager,
            OpenOfferManager openOfferManager, 
            DisputeManager disputeManager, 
            Preferences preferences,
            User user, 
            AlertManager alertManager, 
            PrivateNotificationManager privateNotificationManager,
            FilterManager filterManager, 
            WalletPasswordWindow walletPasswordWindow, 
            TradeStatisticsManager tradeStatisticsManager,
            NotificationCenter notificationCenter,  
            Clock clock, 
            FeeService feeService,
            DaoManager daoManager, 
            EncryptionService encryptionService,
            KeyRing keyRing, 
            BisqEnvironment bisqEnvironment, 
            FailedTradesManager failedTradesManager,
            ClosedTradableManager closedTradableManager, 
            AccountAgeWitnessService accountAgeWitnessService,
            BSFormatter formatter
    ) {
        EngineBoot.walletsManager = walletsManager;
        EngineBoot.walletsSetup = walletsSetup;
        EngineBoot.btcWalletService = btcWalletService;
        EngineBoot.priceFeedService = priceFeedService;
        EngineBoot.user = user;
        EngineBoot.arbitratorManager = arbitratorManager;
        EngineBoot.p2PService = p2PService;
        EngineBoot.tradeManager = tradeManager;
        EngineBoot.openOfferManager = openOfferManager;
        EngineBoot.disputeManager = disputeManager;
        EngineBoot.preferences = preferences;
        EngineBoot.alertManager = alertManager;
        EngineBoot.privateNotificationManager = privateNotificationManager;
        EngineBoot.filterManager = filterManager; // Reference so it's initialized and eventListener gets registered
        EngineBoot.walletPasswordWindow = walletPasswordWindow;
        EngineBoot.tradeStatisticsManager = tradeStatisticsManager;
        EngineBoot.notificationCenter = notificationCenter;
        EngineBoot.clock = clock;
        EngineBoot.feeService = feeService;
        EngineBoot.daoManager = daoManager;
        EngineBoot.encryptionService = encryptionService;
        EngineBoot.keyRing = keyRing;
        EngineBoot.bisqEnvironment = bisqEnvironment;
        EngineBoot.failedTradesManager = failedTradesManager;
        EngineBoot.closedTradableManager = closedTradableManager;
        EngineBoot.accountAgeWitnessService = accountAgeWitnessService;
        EngineBoot.formatter = formatter;

        TxIdTextField.setPreferences(preferences);

        // TODO
        TxIdTextField.setWalletService(btcWalletService);
        BalanceWithConfirmationTextField.setWalletService(btcWalletService);
    }   
}
