/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.main.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.inject.Injector;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import org.springframework.stereotype.Service;

import io.bisq.core.user.User;
import io.bisq.engine.main.api.util.bootInfo;

@Service
@AutoJsonRpcServiceImpl
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Model implements Interface{
    
    private static Injector injector;
    
    
    public static void inject(Injector i){
        injector = i;
    }
    
    @Override
    public Boolean test(){
        return true;
    }
    
    @Override
    public bootInfo boot(){
        return new bootInfo();
    }
    
    @Override
    public Object user(){
        return injector.getInstance(User.class).getClass();
    }
}