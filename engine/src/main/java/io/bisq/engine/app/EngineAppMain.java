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

//import io.bisq.gui.app.GuiApp;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.bisq.common.app.DevEnv;
import io.bisq.common.util.Utilities;
import io.bisq.core.app.AppOptionKeys;
import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.app.BisqExecutable;
import io.bisq.engine.app.util.Args;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static io.bisq.core.app.BisqEnvironment.DEFAULT_APP_NAME;
import static io.bisq.core.app.BisqEnvironment.DEFAULT_USER_DATA_DIR;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class EngineAppMain extends BisqExecutable{


    public static ExecutorService BisqEngine;
    public static ApplicationContext SpringApp;
    public static Args args;

    static {
        // Need to set default locale initially otherwise we get problems at non-english OS
        Locale.setDefault(new Locale("en", Locale.getDefault().getCountry()));
        Utilities.removeCryptographyRestrictions();
    }

    public static void main(String[] arguments) throws Exception {

        for (String arg: arguments){
            if(arg.contains("REGTEST")) DevEnv.USE_DEV_PRIVILEGE_KEYS = true;
        }

        args = new Args(arguments);
        if(Args.http){
            SpringApplicationBuilder springApp = new SpringApplicationBuilder(EngineAppMain.class);
            if(Args.gui) springApp.headless(false);
            SpringApp = springApp.run(args.springargs);
        }


        // We don't want to do the full argument parsing here as that might easily change in update versions
        // So we only handle the absolute minimum which is APP_NAME, APP_DATA_DIR_KEY and USER_DATA_DIR
        OptionParser parser = new OptionParser();

        parser.allowsUnrecognizedOptions();

        parser.accepts(AppOptionKeys.USER_DATA_DIR_KEY, description("User data directory", DEFAULT_USER_DATA_DIR))
                .withRequiredArg();
        parser.accepts(AppOptionKeys.APP_NAME_KEY, description("Application name", DEFAULT_APP_NAME))
                .withRequiredArg();

        /*
        //We cant get these to BisqEnvironment without adjusting code in io.bisq.core.app.BisqExecutable.

        parser.accepts("http", description("Is the http server active", false))
                .withRequiredArg();
        parser.accepts("gui", description("Is the javaFX GUI active", true))
                .withRequiredArg();
        */

        OptionSet options;

        try {
            options = parser.parse(args.envargs);
        } catch (OptionException ex) {
            System.out.println("error: " + ex.getMessage());
            System.out.println();
            parser.printHelpOn(System.out);
            System.exit(EXIT_FAILURE);
            return;
        }
        BisqEnvironment environment = getBisqEnvironment(options);

        System.out.println();
        if(Args.gui){
            System.out.println("Starting in GUI mode");
        }else{
            System.out.println("Starting in HEADLESS mode");
        }
        System.out.println();


        // need to call that before bisqAppMain().execute(args)
        initAppDir(environment.getProperty(AppOptionKeys.APP_DATA_DIR_KEY));

        // For some reason the JavaFX launch process results in us losing the thread context class loader: reset it.
        // In order to work around a bug in JavaFX 8u25 and below, you must include the following code as the first line of your realMain method:
        Thread.currentThread().setContextClassLoader(EngineAppMain.class.getClassLoader());

        if(Args.http){
            SpringApp.getBean(EngineAppMain.class).execute(args.coreargs);
        }else{
            new EngineAppMain().execute(args.coreargs);
        }
    }

    @Override
    protected void doExecute(OptionSet options) {
        BisqEnvironment bisqEnvironment = getBisqEnvironment(options);
        CommonApp.setEnvironment(bisqEnvironment);

        if(Args.gui){
            javafx.application.Application.launch(CommonApp.class);
        }else{
            ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("Bisq Engine-API").build();
            BisqEngine = Executors.newSingleThreadExecutor(factory);
            BisqEngine.submit(new HeadlessApp());
        }
    }
}
