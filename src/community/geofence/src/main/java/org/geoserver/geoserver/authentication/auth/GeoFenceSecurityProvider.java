/*
 *  Copyright (C) 2007 - 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.geoserver.authentication.auth;

import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceSecurityProvider extends GeoServerSecurityProvider
        implements InitializingBean {

    private static final Logger LOGGER =
            Logging.getLogger(GeoFenceSecurityProvider.class.getName());

    private RuleReaderService ruleReaderService;

    private GeoServerSecurityManager securityManager;

    public GeoFenceSecurityProvider() {}

    @Override
    public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {
        return GeoFenceAuthenticationProvider.class;
    }

    @Override
    public GeoFenceAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        GeoFenceAuthenticationProvider authProv = new GeoFenceAuthenticationProvider();
        authProv.setRuleReaderService(ruleReaderService);
        return authProv;
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
    }

    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("geofence", GeoFenceAuthenticationProviderConfig.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ensureProviderConfigFile();
    }

    private void ensureProviderConfigFile() {
        if (securityManager == null) {
            LOGGER.severe("securityManager is null!");
            return;
        }

        try {
            SecurityAuthProviderConfig loadedConfig =
                    securityManager.loadAuthenticationProviderConfig("geofence");
            if (loadedConfig == null) {
                LOGGER.warning("Configuration file not found, creating default config");

                // config: create a default one
                GeoFenceAuthenticationProviderConfig defaultConfig =
                        new GeoFenceAuthenticationProviderConfig();
                defaultConfig.setName("geofence");
                defaultConfig.setClassName(GeoFenceAuthenticationProvider.class.getName());
                securityManager.saveAuthenticationProvider(defaultConfig);
            }

        } catch (Exception ex) {
            LOGGER.severe("Error in configuration: " + ex.getMessage());
        }
    }
}
