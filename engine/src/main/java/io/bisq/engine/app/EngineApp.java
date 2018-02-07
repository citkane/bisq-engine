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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.bisq.common.CommonOptionKeys;
import io.bisq.common.UserThread;
import io.bisq.common.app.Capabilities;
import io.bisq.common.app.Log;
import io.bisq.common.app.Version;
import io.bisq.common.crypto.LimitedKeyStrengthException;
import io.bisq.common.handlers.ResultHandler;
import io.bisq.common.locale.CurrencyUtil;
import io.bisq.common.locale.Res;
import io.bisq.common.proto.persistable.PersistedDataHost;
import io.bisq.common.storage.Storage;
import io.bisq.common.util.Profiler;
import io.bisq.common.util.Utilities;
import io.bisq.core.app.AppOptionKeys;
import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.arbitration.ArbitratorManager;
import io.bisq.core.arbitration.DisputeManager;
import io.bisq.core.btc.AddressEntryList;
import io.bisq.core.btc.BaseCurrencyNetwork;
import io.bisq.core.btc.wallet.*;
import io.bisq.core.dao.blockchain.json.JsonBlockChainExporter;
import io.bisq.core.dao.compensation.CompensationRequestManager;
import io.bisq.core.dao.vote.VotingManager;
import io.bisq.core.offer.OpenOfferManager;
import io.bisq.core.trade.TradeManager;
import io.bisq.core.trade.closed.ClosedTradableManager;
import io.bisq.core.trade.failed.FailedTradesManager;
import io.bisq.core.user.Preferences;
import io.bisq.core.user.User;
import io.bisq.network.p2p.P2PService;
import java.applet.Applet;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bitcoinj.store.BlockStoreException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.bisq.engine.main.api.Model;

public class EngineApp {
    private static final Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EngineApp.class);
    
    private static final long LOG_MEMORY_PERIOD_MIN = 10;
    
    private static BisqEnvironment bisqEnvironment;
    public static Runnable shutDownHandler;
    
    protected static void setEnvironment(BisqEnvironment bisqEnvironment) {
        EngineApp.bisqEnvironment = bisqEnvironment;
    }
    
    private EngineAppModule bisqAppModule;
    
    protected static Injector injector;
    
    private final List<String> corruptedDatabaseFiles = new ArrayList<>();
    private boolean shutDownRequested;
    
    public EngineApp(){
        try {
            init();
            start();   
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public final void init() throws Exception {
        String logPath = Paths.get(bisqEnvironment.getProperty(AppOptionKeys.APP_DATA_DIR_KEY), "bisq").toString();
        Log.setup(logPath);
        log.info("Log files under: " + logPath);
        Utilities.printSysInfo();
        Log.setLevel(Level.toLevel(bisqEnvironment.getRequiredProperty(CommonOptionKeys.LOG_LEVEL_KEY)));

        shutDownHandler = this::stop;

        // setup UncaughtExceptionHandler
        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            // Might come from another thread
            if (throwable.getCause() != null && throwable.getCause().getCause() != null &&
                    throwable.getCause().getCause() instanceof BlockStoreException) {
                log.error(throwable.getMessage());
            } else if (throwable instanceof ClassCastException &&
                    "sun.awt.image.BufImgSurfaceData cannot be cast to sun.java2d.xr.XRSurfaceData".equals(throwable.getMessage())) {
                log.warn(throwable.getMessage());
            } else {
                log.error("Uncaught Exception from thread " + Thread.currentThread().getName());
                log.error("throwableMessage= " + throwable.getMessage());
                log.error("throwableClass= " + throwable.getClass());
                log.error("Stack trace:\n" + ExceptionUtils.getStackTrace(throwable));
                throwable.printStackTrace();
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);

        try {
            Utilities.checkCryptoPolicySetup();
        } catch (NoSuchAlgorithmException | LimitedKeyStrengthException e) {
            e.printStackTrace();
        }

        Security.addProvider(new BouncyCastleProvider());

        final BaseCurrencyNetwork baseCurrencyNetwork = BisqEnvironment.getBaseCurrencyNetwork();
        final String currencyCode = baseCurrencyNetwork.getCurrencyCode();
        Res.setBaseCurrencyCode(currencyCode);
        Res.setBaseCurrencyName(baseCurrencyNetwork.getCurrencyName());
        CurrencyUtil.setBaseCurrencyCode(currencyCode);

        Capabilities.setSupportedCapabilities(new ArrayList<>(Arrays.asList(
                Capabilities.Capability.TRADE_STATISTICS.ordinal(),
                Capabilities.Capability.TRADE_STATISTICS_2.ordinal(),
                Capabilities.Capability.ACCOUNT_AGE_WITNESS.ordinal(),
                Capabilities.Capability.COMP_REQUEST.ordinal()
        )));
    }
    
    public final void start() throws IOException {
        //BisqApp.primaryStage = stage;

        try {
            // Guice
            bisqAppModule = new EngineAppModule(bisqEnvironment);
            injector = Guice.createInjector(bisqAppModule); 
            Model.inject(injector);
            
            // All classes which are persisting objects need to be added here
            // Maintain order!
            ArrayList<PersistedDataHost> persistedDataHosts = new ArrayList<>();
            final Preferences preferences = injector.getInstance(Preferences.class);
            persistedDataHosts.add(preferences);
            persistedDataHosts.add(injector.getInstance(User.class));
            persistedDataHosts.add(injector.getInstance(AddressEntryList.class));
            persistedDataHosts.add(injector.getInstance(OpenOfferManager.class));
            persistedDataHosts.add(injector.getInstance(TradeManager.class));
            persistedDataHosts.add(injector.getInstance(ClosedTradableManager.class));
            persistedDataHosts.add(injector.getInstance(FailedTradesManager.class));
            persistedDataHosts.add(injector.getInstance(DisputeManager.class));
            persistedDataHosts.add(injector.getInstance(P2PService.class));
            persistedDataHosts.add(injector.getInstance(VotingManager.class));
            persistedDataHosts.add(injector.getInstance(CompensationRequestManager.class));

            // we apply at startup the reading of persisted data but don't want to get it triggered in the constructor
            persistedDataHosts.stream().forEach(e -> {
                try {
                    log.debug("call readPersisted at " + e.getClass().getSimpleName());
                    e.readPersisted();
                } catch (Throwable e1) {
                    log.error("readPersisted error", e1);
                }
            });
            Version.setBaseCryptoNetworkId(BisqEnvironment.getBaseCurrencyNetwork().ordinal());
            Version.printVersion();

            if (Utilities.isLinux())
                System.setProperty("prism.lcdtext", "false");

            Storage.setDatabaseCorruptionHandler((String fileName) -> {
                corruptedDatabaseFiles.add(fileName);
            });
            UserThread.runPeriodically(() -> Profiler.printSystemLoad(log), LOG_MEMORY_PERIOD_MIN, TimeUnit.MINUTES);
            
            EngineBoot root = injector.getInstance(EngineBoot.class);
            UserThread.execute(root::start);
            
        } catch (Throwable throwable) {
            log.error("Error during app init", throwable);
        }
    }

    @SuppressWarnings("CodeBlock2Expr")

    public void stop() {
        if (!shutDownRequested) {
            UserThread.runAfter(() -> {
                gracefulShutDown(() -> {
                    log.debug("App shutdown complete");
                    System.exit(0);
                });
            }, 200, TimeUnit.MILLISECONDS);
            shutDownRequested = true;
        }
    }

    public void gracefulShutDown(ResultHandler resultHandler) {
        try {
            if (injector != null) {
                injector.getInstance(ArbitratorManager.class).shutDown();
                injector.getInstance(TradeManager.class).shutDown();
                injector.getInstance(JsonBlockChainExporter.class).shutDown();

                injector.getInstance(OpenOfferManager.class).shutDown(() -> {
                    injector.getInstance(P2PService.class).shutDown(() -> {
                        injector.getInstance(WalletsSetup.class).shutDownComplete.addListener((ov, o, n) -> {
                            bisqAppModule.close(injector);
                            log.debug("Graceful shutdown completed");
                            resultHandler.handleResult();
                        });
                        injector.getInstance(WalletsSetup.class).shutDown();
                        injector.getInstance(BtcWalletService.class).shutDown();
                        injector.getInstance(BsqWalletService.class).shutDown();
                    });
                });
                // we wait max 20 sec.
                UserThread.runAfter(() -> {
                    log.warn("Timeout triggered resultHandler");
                    resultHandler.handleResult();
                }, 20);
            } else {
                log.warn("injector == null triggered resultHandler");
                UserThread.runAfter(resultHandler::handleResult, 1);
            }
        } catch (Throwable t) {
            log.error("App shutdown failed with exception");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
