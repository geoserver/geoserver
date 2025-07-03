/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

public class OAuth2FilterConfigValidatorTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    OAuth2FilterConfigValidator validator;

    @Before
    public void setValidator() {
        validator = new OpenIdConnectFilterConfigValidator(getSecurityManager());
    }

    @Test
    public void testOAuth2FilterConfigValidation() throws Exception {
        OpenIdConnectFilterConfig config = new OpenIdConnectFilterConfig();
        config.setClassName(GeoServerOAuthAuthenticationFilter.class.getName());
        config.setName("testOAuth2");
        // the OpenConnectId config is empty as anyone can implement it, fill in some mandatory
        // values
        config.setAccessTokenUri("https://www.connectid/fake/test");
        config.setUserAuthorizationUri("https://www.connectid/fake/test");
        config.setCheckTokenEndpointUrl("https://www.connectid/fake/test");

        check(config);
        validator.validateOAuth2FilterConfig(config);
    }

    public void check(OpenIdConnectFilterConfig config) throws Exception {

        boolean failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            // assertEquals(FilterConfigException.ROLE_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            // assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setUserGroupServiceName("blabla");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (FilterConfigException ex) {
            // assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setRoleConverterName(null);

        config.setCheckTokenEndpointUrl(null);

        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OpenIdConnectFilterConfigException ex) {
            assertEquals(
                    OpenIdConnectFilterConfigException
                            .OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED,
                    ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setCheckTokenEndpointUrl("http://localhost/callback");

        config.setAccessTokenUri("blabal");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setAccessTokenUri("http://localhost/callback");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_NOT_HTTPS, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setAccessTokenUri("https://localhost/callback");

        config.setUserAuthorizationUri("blabal");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setUserAuthorizationUri("https://oauth2server/case");

        config.setLogoutUri("blbla");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(
                    OAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setLogoutUri("http://localhost/gesoerver");

        config.setCliendId("oauth2clientid");
        config.setClientSecret("oauth2clientsecret");
        config.setScopes("email,profile");

        validator.validateOAuth2FilterConfig(config);

        config.setUsePKCE(true);
        config.setClientSecret(null);
        validator.validateOAuth2FilterConfig(config);

        config.setUsePKCE(false);
        config.setClientSecret("oauth2clientsecret");
    }

    @Test
    public void testExtractFromJSON() {
        String json = "{\"a\":{\"b\":[\"d\",\"e\"]}}";

        // bad path
        Object o = OpenIdConnectAuthenticationFilter.extractFromJSON(json, "aaaaa");
        assertNull(o);

        // path to {"b":["d","e"]}
        o = OpenIdConnectAuthenticationFilter.extractFromJSON(json, "a");
        assertTrue(o instanceof Map);

        // path to ["d","e"]
        o = OpenIdConnectAuthenticationFilter.extractFromJSON(json, "a.b");
        assertTrue(o instanceof List);
        assertSame(2, ((List) o).size());
        assertEquals("d", ((List) o).get(0));
        assertEquals("e", ((List) o).get(1));
    }
}
