/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * Tests for {@link GeoServerKeycloakFilterConfigValidator}. Shows an invalid set of config failing
 * with the correct error type, and valid config passing (not throwing an exception).
 */
public class GeoServerKeycloakFilterConfigValidatorTest {

    // identifiers for the auth context
    public static final String REALM = "stuff";
    public static final String CLIENT_ID = "guys";

    // locations for useful resources
    public static final String AUTH_URL = "https://place:8000/auth";
    public static final String OPENID_URL = AUTH_URL + "/realms/" + REALM;

    @Test(expected = FilterConfigException.class)
    public void testBadConfig() throws Exception {
        // create some config, but don't set any of the necessary values
        GeoServerKeycloakFilterConfig kcConfig = new GeoServerKeycloakFilterConfig();
        kcConfig.writeAdapterConfig(new AdapterConfig());
        SecurityNamedServiceConfig config = kcConfig;
        // attempt to validate the config
        GeoServerKeycloakFilterConfigValidator validator =
                new GeoServerKeycloakFilterConfigValidator(null);
        validator.validateFilterConfig(config);
        // exception on previous line
    }

    @Test
    public void testGoodConfig() throws Exception {
        // create config and set the required values
        AdapterConfig aConfig = new AdapterConfig();
        aConfig.setRealm(REALM);
        aConfig.setResource(CLIENT_ID);
        aConfig.setAuthServerUrl(AUTH_URL);
        GeoServerKeycloakFilterConfig kcConfig = new GeoServerKeycloakFilterConfig();
        kcConfig.writeAdapterConfig(aConfig);
        SecurityNamedServiceConfig config = kcConfig;
        // attempt to validate the config
        GeoServerKeycloakFilterConfigValidator validator =
                new GeoServerKeycloakFilterConfigValidator(null);
        validator.validateFilterConfig(config);
    }
}
