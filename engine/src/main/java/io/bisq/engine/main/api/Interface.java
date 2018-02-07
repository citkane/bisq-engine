/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.main.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.bisq.engine.main.api.util.bootInfo;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonRpcService("/api")
public interface Interface {
    public Boolean test();

    public bootInfo boot();
    
    public Object user();
    
}
