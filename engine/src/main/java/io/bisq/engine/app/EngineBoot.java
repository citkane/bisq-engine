/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.engine.app;

import com.google.common.net.InetAddresses;
import com.google.inject.Inject;

import io.bisq.common.Clock;
import io.bisq.common.Timer;
import io.bisq.common.UserThread;
import io.bisq.common.app.DevEnv;
import io.bisq.common.crypto.CryptoException;
import io.bisq.common.crypto.KeyRing;
import io.bisq.common.crypto.SealedAndSigned;
import io.bisq.common.locale.Res;
import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.app.SetupUtils;
import io.bisq.core.arbitration.ArbitratorManager;
import io.bisq.core.arbitration.Dispute;
import io.bisq.core.arbitration.DisputeManager;
import io.bisq.core.btc.AddressEntry;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.btc.wallet.WalletsManager;
import io.bisq.core.btc.wallet.WalletsSetup;
import io.bisq.core.filter.FilterManager;
import io.bisq.core.offer.OpenOfferManager;
import io.bisq.core.payment.AccountAgeWitnessService;
import io.bisq.core.payment.payload.PaymentMethod;
import io.bisq.core.provider.fee.FeeService;
import io.bisq.core.provider.price.PriceFeedService;
import io.bisq.core.trade.TradeManager;
import io.bisq.core.user.Preferences;

import io.bisq.network.crypto.DecryptedDataTuple;
import io.bisq.network.crypto.EncryptionService;
import io.bisq.network.p2p.P2PService;
import io.bisq.network.p2p.P2PServiceListener;
import io.bisq.network.p2p.network.CloseConnectionReason;
import io.bisq.network.p2p.network.Connection;
import io.bisq.network.p2p.network.ConnectionListener;
import io.bisq.network.p2p.peers.keepalive.messages.Ping;

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class EngineBoot{
    
    private static final Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EngineBoot.class);
    private static final long STARTUP_TIMEOUT_MINUTES = 4;
    
    public static BooleanProperty p2pNetWorkReady;
    public static MonadicBinding<Boolean> allServicesDone;
    public static final BooleanProperty walletInitialized = new SimpleBooleanProperty();
    public static final BooleanProperty bootstrapComplete = new SimpleBooleanProperty();
    public static boolean allBasicServicesInitialized;
    
    private final Clock clock;
    private final FilterManager filterManager;
    private final KeyRing keyRing;
    private final EncryptionService encryptionService;
       
    private final BtcWalletService btcWalletService;
    private final AccountAgeWitnessService accountAgeWitnessService;
    private final ArbitratorManager arbitratorManager;
    private final FeeService feeService;
    private final OpenOfferManager openOfferManager;
    private final TradeManager tradeManager;
    private final DisputeManager disputeManager;
    private final WalletsManager walletsManager;
    private final PriceFeedService priceFeedService;
    private final P2PService p2PService;
    private final WalletsSetup walletsSetup;
    private final Preferences preferences;
    private final BisqEnvironment bisqEnvironment;
    
    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess")
    @Inject
    public EngineBoot(            
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
            FilterManager filterManager,
            Clock clock, 
            FeeService feeService,
            EncryptionService encryptionService,
            KeyRing keyRing, 
            BisqEnvironment bisqEnvironment,  
            AccountAgeWitnessService accountAgeWitnessService           
    ){   
        this.walletsManager = walletsManager;
        this.walletsSetup = walletsSetup;
        this.btcWalletService = btcWalletService;
        this.priceFeedService = priceFeedService;
        this.arbitratorManager = arbitratorManager;
        this.p2PService = p2PService;
        this.tradeManager = tradeManager;
        this.openOfferManager = openOfferManager;
        this.disputeManager = disputeManager;
        this.preferences = preferences;
        this.filterManager = filterManager; // Reference so it's initialized and eventListener gets registered
        this.clock = clock;
        this.feeService = feeService;
        this.encryptionService = encryptionService;
        this.keyRing = keyRing;
        this.bisqEnvironment = bisqEnvironment;
        this.accountAgeWitnessService = accountAgeWitnessService;       
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void start() {
        
        bisqEnvironment.saveBaseCryptoNetwork(BisqEnvironment.getBaseCurrencyNetwork());

        // We do the delete of the spv file at startup before BitcoinJ is initialized to avoid issues with locked files under Windows.
        if (preferences.isResyncSpvRequested()) {
            try {
                walletsSetup.reSyncSPVChain();
            } catch (IOException e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
        
        checkIfLocalHostNodeIsRunning();
    }

    private void readMapsFromResources() {
        SetupUtils.readFromResources(p2PService.getP2PDataStorage()).addListener((observable, oldValue, newValue) -> {
            if (newValue)
                startBasicServices();
        });

        // TODO can be removed in jdk 9
        checkCryptoSetup();
    }

    private void startBasicServices() {
        log.info("startBasicServices");

        Timer startupTimeout = UserThread.runAfter(() -> {
            log.warn("startupTimeout called");
        }, STARTUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        p2pNetWorkReady = initP2PNetwork();

        // We only init wallet service here if not using Tor for bitcoinj.
        // When using Tor, wallet init must be deferred until Tor is ready.
        if (!preferences.getUseTorForBitcoinJ() || bisqEnvironment.isBitcoinLocalhostNodeRunning())
            initWalletService();

        // need to store it to not get garbage collected
        allServicesDone = EasyBind.combine(walletInitialized, p2pNetWorkReady,
                (a, b) -> {
                    log.debug("\nwalletInitialized={}\n" +
                                    "p2pNetWorkReady={}",
                            a, b);
                    return a && b;
                });
        allServicesDone.subscribe((observable, oldValue, newValue) -> {
            if (newValue) {
                startupTimeout.stop();
                onBasicServicesInitialized();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Initialisation
    ///////////////////////////////////////////////////////////////////////////////////////////

    private BooleanProperty initP2PNetwork() {
        log.info("initP2PNetwork");

        BooleanProperty hiddenServicePublished = new SimpleBooleanProperty();
        BooleanProperty initialP2PNetworkDataReceived = new SimpleBooleanProperty();

        p2PService.getNetworkNode().addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnection(Connection connection) {
            }

            @Override
            public void onDisconnect(CloseConnectionReason closeConnectionReason, Connection connection) {
                // We only check at seed nodes as they are running the latest version
                // Other disconnects might be caused by peers running an older version
                if (connection.getPeerType() == Connection.PeerType.SEED_NODE &&
                        closeConnectionReason == CloseConnectionReason.RULE_VIOLATION) {
                    log.warn("RULE_VIOLATION onDisconnect closeConnectionReason=" + closeConnectionReason);
                    log.warn("RULE_VIOLATION onDisconnect connection=" + connection);
                }
            }

            @Override
            public void onError(Throwable throwable) {
            }
        });

        final BooleanProperty p2pNetworkInitialized = new SimpleBooleanProperty();
        p2PService.start(new P2PServiceListener() {
            @Override
            public void onTorNodeReady() {
                log.debug("onTorNodeReady");
                if (preferences.getUseTorForBitcoinJ())
                    initWalletService();

                // We want to get early connected to the price relay so we call it already now
                priceFeedService.setCurrencyCodeOnInit();
                priceFeedService.initialRequestPriceFeed();
            }

            @Override
            public void onHiddenServicePublished() {
                log.debug("onHiddenServicePublished");
                hiddenServicePublished.set(true);
            }

            @Override
            public void onDataReceived() {
                log.debug("onRequestingDataCompleted");
                initialP2PNetworkDataReceived.set(true);
                p2pNetworkInitialized.set(true);
            }

            @Override
            public void onNoSeedNodeAvailable() {
                log.warn("onNoSeedNodeAvailable");
            }

            @Override
            public void onNoPeersAvailable() {
                log.warn("onNoPeersAvailable");
                p2pNetworkInitialized.set(true);
            }

            @Override
            public void onUpdatedDataReceived() {
                log.debug("onBootstrapComplete");
                bootstrapComplete.set(true);
            }

            @Override
            public void onSetupFailed(Throwable throwable) {
                log.warn("onSetupFailed");
            }

            @Override
            public void onRequestCustomBridges() {

            }
        });

        return p2pNetworkInitialized;
    }

    private void initWalletService() {
        log.info("initWalletService");
        log.debug("walletsSetup.onInitialized");

         // We only check one as we apply encryption to all or none
         if (walletsManager.areWalletsEncrypted()) {
             if (p2pNetWorkReady.get()){

             }
         } else {
             if (preferences.isResyncSpvRequested()) {

             } else {
                 walletInitialized.set(true);
             }
         }

         walletInitialized.set(true);
    }

    private void onBasicServicesInitialized() {
        log.info("onBasicServicesInitialized");

        clock.start();

        PaymentMethod.onAllServicesInitialized();

        // disputeManager
        disputeManager.onAllServicesInitialized();
        disputeManager.getDisputesAsObservableList().addListener((ListChangeListener<Dispute>) change -> {
            change.next();
        });

        // tradeManager
        tradeManager.onAllServicesInitialized();
        openOfferManager.onAllServicesInitialized();
        arbitratorManager.onAllServicesInitialized();
        p2PService.onAllServicesInitialized();
        feeService.onAllServicesInitialized();
        accountAgeWitnessService.onAllServicesInitialized();
        priceFeedService.setCurrencyCodeOnInit();
        filterManager.onAllServicesInitialized();
      
        if (DevEnv.DEV_MODE) {
            preferences.setShowOwnOffersInOfferBook(true);
        }
        swapPendingOfferFundingEntries();

        allBasicServicesInitialized = true;
        System.out.println("______________allBasicServicesInitialized____________________________________");
    }

    private void checkIfLocalHostNodeIsRunning() {
        Thread checkIfLocalHostNodeIsRunningThread = new Thread() {
            @Override
            public void run() {
                Thread.currentThread().setName("checkIfLocalHostNodeIsRunningThread");
                Socket socket = null;
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddresses.forString("127.0.0.1"),
                            BisqEnvironment.getBaseCurrencyNetwork().getParameters().getPort()), 5000);
                    log.info("Localhost peer detected.");
                    UserThread.execute(() -> {
                        bisqEnvironment.setBitcoinLocalhostNodeRunning(true);
                        readMapsFromResources();
                    });
                } catch (Throwable e) {
                    log.info("Localhost peer not detected.");
                    UserThread.execute(() -> {
                        readMapsFromResources();
                    });
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }
        };
        checkIfLocalHostNodeIsRunningThread.start();
    }

    private void checkCryptoSetup() {
        BooleanProperty result = new SimpleBooleanProperty();
        // We want to test if the client is compiled with the correct crypto provider (BountyCastle)
        // and if the unlimited Strength for cryptographic keys is set.
        // If users compile themselves they might miss that step and then would get an exception in the trade.
        // To avoid that we add here at startup a sample encryption and signing to see if it don't causes an exception.
        // See: https://github.com/bisq-network/exchange/blob/master/doc/build.md#7-enable-unlimited-strength-for-cryptographic-keys
        Thread checkCryptoThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setName("checkCryptoThread");
                    log.trace("Run crypto test");
                    // just use any simple dummy msg
                    Ping payload = new Ping(1, 1);
                    SealedAndSigned sealedAndSigned = EncryptionService.encryptHybridWithSignature(payload,
                            keyRing.getSignatureKeyPair(), keyRing.getPubKeyRing().getEncryptionPubKey());
                    DecryptedDataTuple tuple = encryptionService.decryptHybridWithSignature(sealedAndSigned, keyRing.getEncryptionKeyPair().getPrivate());
                    if (tuple.getNetworkEnvelope() instanceof Ping &&
                            ((Ping) tuple.getNetworkEnvelope()).getNonce() == payload.getNonce() &&
                            ((Ping) tuple.getNetworkEnvelope()).getLastRoundTripTime() == payload.getLastRoundTripTime()) {
                        log.debug("Crypto test succeeded");

                        if (Security.getProvider("BC") != null) {
                            UserThread.execute(() -> result.set(true));
                        } else {
                            throw new CryptoException("Security provider BountyCastle is not available.");
                        }
                    } else {
                        throw new CryptoException("Payload not correct after decryption");
                    }
                } catch (CryptoException e) {
                    e.printStackTrace();
                    String msg = Res.get("popup.warning.cryptoTestFailed", e.getMessage());
                    log.error(msg);
                }
            }
        };
        checkCryptoThread.start();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////


    private void swapPendingOfferFundingEntries() {
        tradeManager.getAddressEntriesForAvailableBalanceStream()
                .filter(addressEntry -> addressEntry.getOfferId() != null)
                .forEach(addressEntry -> {
                    log.debug("swapPendingOfferFundingEntries, offerId={}, OFFER_FUNDING", addressEntry.getOfferId());
                    btcWalletService.swapTradeEntryToAvailableEntry(addressEntry.getOfferId(), AddressEntry.Context.OFFER_FUNDING);
                });
    }
}
