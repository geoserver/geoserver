/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *     <p>Validates {@link OAuth2FilterConfig} objects.
 */
public class OAuth2FilterConfigValidatorTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    OAuth2FilterConfigValidator validator;

    @Before
    public void setValidator() {
        validator = new OAuth2FilterConfigValidator(getSecurityManager());
    }

    @Test
    public void testOAuth2FilterConfigValidation() throws Exception {
        GoogleOAuth2FilterConfig config = new GoogleOAuth2FilterConfig();
        config.setClassName(GeoServerOAuthAuthenticationFilter.class.getName());
        config.setName("testOAuth2");

        check(config);
        validator.validateOAuth2FilterConfig(config);
    }

    public void check(GoogleOAuth2FilterConfig config) throws Exception {

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
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(
                    OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED, ex.getId());
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
    }
}
