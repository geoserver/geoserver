/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
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
