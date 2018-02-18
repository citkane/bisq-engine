/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Args {
        private static final List<String> coreArgs = new ArrayList<String>();
        private static final List<String> envArgs = new ArrayList<String>();
        private static final List<String> appArgs = new ArrayList<String>();
        private static final List<String> springArgs = new ArrayList<String>();
        private String port = "8080"; //The default port for Spring-core Tomcat server
        
        public String[] coreargs;
        public String[] envargs;
        public String[] springargs;
        public static Boolean http = false;
        public static Boolean gui = true;

    public Args(String[] args) {
        appArgs.add("--headless");
        appArgs.add("--http");
        
        for( int i=0; i < args.length; i++){
            if(args[i].indexOf("--") == 0 && !appArgs.contains(args[i].split("=")[0].trim())){
                coreArgs.add(args[i]);
                if(i+1 < args.length && args[i+1].indexOf("--") != 0) coreArgs.add(args[i+1]);
            }
            if(args[i].indexOf("--headless") == 0){               
                if(i+1 < args.length && args[i+1].indexOf("--") == 0){                   
                    if(args[i].split("=").length == 1){
                        gui = false;
                    }else if(args[i].split("=")[1].trim().equals("true")){
                        gui = false;
                    }
                }else if(i+1 < args.length){
                    if(args[i+1].equals("true")){
                        gui = false;
                    } else {
                    }
                    args[i+1] = "deleteMe";
                }else{
                    if(args[i].split("=").length == 1){
                        gui = false;
                    }else if(args[i].split("=")[1].trim().equals("true")){
                        gui = false;
                    }
                }
                args[i] = "deleteMe";
            }
            if(args[i].indexOf("--http") == 0){ 
                http = true;
                if(i+1 < args.length && args[i+1].indexOf("--") == 0){                   
                    if(args[i].split("=").length > 1){
                        if(args[i].split("=")[1].equals("false")){
                            http =false;
                        }else if(!args[i].split("=")[1].equals("true")){
                            port = args[i].split("=")[1];
                        }                       
                    }
                }else if(i+1 < args.length){
                    port = args[i+1];
                    args[i+1] = "deleteMe";
                }else{
                    if(args[i].split("=").length > 1){
                        if(args[i].split("=")[1].equals("false")){
                            http =false;
                        }else if(!args[i].split("=")[1].equals("true")){
                            port = args[i].split("=")[1];
                        }                       
                    }
                }
                args[i] = "deleteMe";
            }
        }
        Arrays.asList(args).forEach((val)->{
            if(!val.equals("deleteMe") && val.indexOf("--headless") != 0  && val.indexOf("--http") != 0){
                envArgs.add(val);
            }
        });
        
        /*
        //TODO We cant get these to BisqEnvironment without adjusting code in io.bisq.core.app.BisqExecutable so we leave them out for now.
        
        envArgs.add("--gui="+gui);
        envArgs.add("--http="+http);
        */
        
        if(http){
            springArgs.add("--server.port="+port);
            springArgs.add("--server.address=localhost");
        }
        coreargs = coreArgs.toArray(new String[coreArgs.size()]);
        envargs = envArgs.toArray(new String[envArgs.size()]);
        springargs = springArgs.toArray(new String[springArgs.size()]);
    }
}
