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


import com.google.inject.name.Names;

import io.bisq.common.app.AppModule;
import io.bisq.common.locale.Res;
import io.bisq.core.app.AppOptionKeys;

import org.springframework.core.env.Environment;
import java.util.ResourceBundle;

public class EngineModule extends AppModule {

    public EngineModule(Environment environment) {
        super(environment);
    }

    @Override
    protected void configure() {
        bind(ResourceBundle.class).toInstance(Res.getResourceBundle());
        bindConstant().annotatedWith(Names.named(AppOptionKeys.APP_NAME_KEY)).to(environment.getRequiredProperty(AppOptionKeys.APP_NAME_KEY));
    }
}
