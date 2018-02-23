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

package io.bisq.engine;


import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.bisq.common.app.AppModule;
import io.bisq.common.locale.Res;
import io.bisq.core.app.AppOptionKeys;
import io.bisq.engine.app.util.Args;
import io.bisq.gui.Navigation;
import io.bisq.gui.common.fxml.FxmlViewLoader;
import io.bisq.gui.common.view.CachingViewLoader;
import io.bisq.gui.common.view.ViewFactory;
import io.bisq.gui.common.view.ViewLoader;
import io.bisq.gui.common.view.guice.InjectorViewFactory;
import io.bisq.gui.main.offer.offerbook.OfferBook;
import io.bisq.gui.main.overlays.windows.TorNetworkSettingsWindow;
import io.bisq.gui.util.BSFormatter;
import io.bisq.gui.util.BsqFormatter;
import io.bisq.gui.util.Transitions;
import javafx.stage.Stage;
import org.springframework.core.env.Environment;

import java.util.ResourceBundle;

public class EngineModule extends AppModule {

    private final Stage primaryStage;

    public EngineModule(Environment environment, Stage primaryStage) {
        super(environment);
        this.primaryStage = primaryStage;
    }

    @Override
    protected void configure() {
        bind(BSFormatter.class).in(Singleton.class);
        bind(InjectorViewFactory.class).in(Singleton.class);
        bind(ViewFactory.class).to(InjectorViewFactory.class);

        if(Args.gui) bind(ViewLoader.class).to(FxmlViewLoader.class).in(Singleton.class);
        if(Args.gui) bind(CachingViewLoader.class).in(Singleton.class);
        if(Args.gui)bind(Navigation.class).in(Singleton.class);

        bind(OfferBook.class).in(Singleton.class);
        bind(BsqFormatter.class).in(Singleton.class);

        if(Args.gui) {
            bind(TorNetworkSettingsWindow.class).in(Singleton.class);
            bind(Transitions.class).in(Singleton.class);
            bind(Stage.class).toInstance(primaryStage);
        }


        bind(ResourceBundle.class).toInstance(Res.getResourceBundle());
        bindConstant().annotatedWith(Names.named(AppOptionKeys.APP_NAME_KEY)).to(environment.getRequiredProperty(AppOptionKeys.APP_NAME_KEY));
    }
}
