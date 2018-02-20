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
import io.bisq.gui.util.BSFormatter;
import io.bisq.network.crypto.EncryptionService;
import io.bisq.network.p2p.P2PService;
import org.bitcoinj.core.Coin;

public class HeadlessConstructor extends EngineBoot{

    private final WalletsManager walletsManager;
    private final WalletsSetup walletsSetup;
    private final BtcWalletService btcWalletService;
    private final ArbitratorManager arbitratorManager;
    private final P2PService p2PService;
    private final TradeManager tradeManager;
    private final OpenOfferManager openOfferManager;
    private final DisputeManager disputeManager;
    private final Preferences preferences;
    private final AlertManager alertManager;
    private final PrivateNotificationManager privateNotificationManager;
    private final FilterManager filterManager;
    private final TradeStatisticsManager tradeStatisticsManager;
    private final NotificationCenter notificationCenter;
    private final Clock clock;
    private final FeeService feeService;
    private final DaoManager daoManager;
    private final EncryptionService encryptionService;
    private final KeyRing keyRing;
    private final BisqEnvironment bisqEnvironment;
    private final FailedTradesManager failedTradesManager;
    private final ClosedTradableManager closedTradableManager;
    private final AccountAgeWitnessService accountAgeWitnessService;
    private final BSFormatter formatter;

    final PriceFeedService priceFeedService;
    private final User user;

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
        this.walletsManager = walletsManager;
        this.walletsSetup = walletsSetup;
        this.btcWalletService = btcWalletService;
        this.priceFeedService = priceFeedService;
        this.user = user;
        this.arbitratorManager = arbitratorManager;
        this.p2PService = p2PService;
        this.tradeManager = tradeManager;
        this.openOfferManager = openOfferManager;
        this.disputeManager = disputeManager;
        this.preferences = preferences;
        this.alertManager = alertManager;
        this.privateNotificationManager = privateNotificationManager;
        this.filterManager = filterManager; // Reference so it's initialized and eventListener gets registered
        this.tradeStatisticsManager = tradeStatisticsManager;
        this.notificationCenter = notificationCenter;
        this.clock = clock;
        this.feeService = feeService;
        this.daoManager = daoManager;
        this.encryptionService = encryptionService;
        this.keyRing = keyRing;
        this.bisqEnvironment = bisqEnvironment;
        this.failedTradesManager = failedTradesManager;
        this.closedTradableManager = closedTradableManager;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.formatter = formatter;

        EngineBoot.walletsManager = this.walletsManager;
        EngineBoot.walletsSetup = this.walletsSetup;
        EngineBoot.btcWalletService = this.btcWalletService;
        EngineBoot.priceFeedService = this.priceFeedService;
        EngineBoot.user = this.user;
        EngineBoot.arbitratorManager = this.arbitratorManager;
        EngineBoot.p2PService = this.p2PService;
        EngineBoot.tradeManager = this.tradeManager;
        EngineBoot.openOfferManager = this.openOfferManager;
        EngineBoot.disputeManager = this.disputeManager;
        EngineBoot.preferences = this.preferences;
        EngineBoot.alertManager = this.alertManager;
        EngineBoot.privateNotificationManager = this.privateNotificationManager;
        EngineBoot.filterManager = this.filterManager; // Reference so it's initialized and eventListener gets registered
        EngineBoot.tradeStatisticsManager = this.tradeStatisticsManager;
        EngineBoot.notificationCenter = this.notificationCenter;
        EngineBoot.clock = this.clock;
        EngineBoot.feeService = this.feeService;
        EngineBoot.daoManager = this.daoManager;
        EngineBoot.encryptionService = this.encryptionService;
        EngineBoot.keyRing = this.keyRing;
        EngineBoot.bisqEnvironment = this.bisqEnvironment;
        EngineBoot.failedTradesManager = this.failedTradesManager;
        EngineBoot.closedTradableManager = this.closedTradableManager;
        EngineBoot.accountAgeWitnessService = this.accountAgeWitnessService;
        EngineBoot.formatter = this.formatter;

        TxIdTextField.setPreferences(preferences);

    }
}
