/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.main.api.util;

import io.bisq.engine.app.EngineBoot;
import javafx.beans.property.BooleanProperty;
import org.fxmisc.easybind.monadic.MonadicBinding;

public class bootInfo {
    public Boolean p2pNetWorkReady = EngineBoot.p2pNetWorkReady.get();
    public Boolean allServicesDone = EngineBoot.allServicesDone.get();
    public Boolean walletInitialized = EngineBoot.walletInitialized.get();
    public Boolean bootstrapComplete = EngineBoot.bootstrapComplete.get();
    public Boolean allBasicServicesInitialized = EngineBoot.allBasicServicesInitialized;
}
