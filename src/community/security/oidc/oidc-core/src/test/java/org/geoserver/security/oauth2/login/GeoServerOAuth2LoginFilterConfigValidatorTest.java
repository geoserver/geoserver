/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2.login;

import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.MSGRAPH_COMBINATION_INVALID;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.OAUTH2_CLIENT_SECRET_REQUIRED;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.OAUTH2_CLIENT_USER_NAME_REQUIRED;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException.OAUTH2_USERAUTHURI_NOT_HTTPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link GeoServerOAuth2LoginFilterConfigValidator} */
public class GeoServerOAuth2LoginFilterConfigValidatorTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    private GeoServerOAuth2LoginFilterConfigValidator validator;

    @Before
    public void setUp() {
        validator = new GeoServerOAuth2LoginFilterConfigValidator(getSecurityManager());
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
    }

    @Test
    public void testOAuth2FilterConfigValidation() throws Exception {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);
        config.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        config.setName("testOAuth2");

        check(config);
        validator.validateOAuth2FilterConfig(config);
    }

    private void check(GeoServerOAuth2LoginFilterConfig config) throws Exception {
        Callable<Void> lValidate = () -> {
            validator.validateOAuth2FilterConfig(config);
            fail("FilterConfigException expected.");
            return null;
        };

        config.setOidcUserNameAttribute(null);
        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            assertExceptionCodeWithArgCount(ex, OAUTH2_CLIENT_USER_NAME_REQUIRED, 1);
        }
        config.setOidcUserNameAttribute("email");

        // when: null uri
        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            // then: null not accepted
            assertExceptionCodeWithArgCount(ex, OAUTH2_USERAUTHURI_MALFORMED, 0);
        }

        // when: invalid uri
        config.setOidcAuthorizationUri("lalala");
        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            // then: invalid not accepted
            assertExceptionCodeWithArgCount(ex, OAUTH2_USERAUTHURI_MALFORMED, 0);
        }

        // when: http
        config.setOidcAuthorizationUri("http://lalala");
        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            // then: by default not accepted
            assertExceptionCodeWithArgCount(ex, OAUTH2_USERAUTHURI_NOT_HTTPS, 0);
        }

        // when: http allowed and http used
        config.setOidcForceAuthorizationUriHttps(false);
        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            // then: actual validation ok, next validation fails
            assertExceptionCodeWithArgCount(ex, OAUTH2_ACCESSTOKENURI_MALFORMED, 0);
        }

        // when: http not allowed and https used
        config.setOidcForceAuthorizationUriHttps(false);
        config.setOidcAuthorizationUri("https://lalala");
        config.setOidcForceAuthorizationUriHttps(false);
        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            // then: actual validation ok, next validation fails
            assertExceptionCodeWithArgCount(ex, OAUTH2_ACCESSTOKENURI_MALFORMED, 0);
        }

        // when: access token URI Ok
        config.setOidcTokenUri("https://tokenuri");

        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            // then: actual validation ok, next validation fails
            assertExceptionCodeWithArgCount(ex, GeoServerOAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, 1);
        }
        config.setOidcClientId("myClientId");

        try {
            lValidate.call();
        } catch (FilterConfigException ex) {
            assertExceptionCodeWithArgCount(ex, OAUTH2_CLIENT_SECRET_REQUIRED, 1);
        }
        config.setOidcClientSecret("myClientSecret");

        config.setOidcLogoutUri("blbla");
        try {
            lValidate.call();
        } catch (GeoServerOAuth2FilterConfigException ex) {
            assertExceptionCodeWithArgCount(ex, OAUTH2_URL_IN_LOGOUT_URI_MALFORMED, 0);
        }
        config.setOidcLogoutUri("http://localhost/gesoerver");

        config.setOidcClientId("oauth2clientid");
        config.setOidcClientSecret("oauth2clientsecret");

        // when: scope openid removed
        config.setOidcScopes("email,profile");
        try {
            lValidate.call();
        } catch (GeoServerOAuth2FilterConfigException ex) {
            assertExceptionCodeWithArgCount(
                    ex, GeoServerOAuth2FilterConfigException.OAUTH2_USER_INFO_URI_REQUIRED_NO_OIDC, 0);
        }

        config.setOidcScopes("openid,email,profile");

        try {
            lValidate.call();
        } catch (GeoServerOAuth2FilterConfigException ex) {
            assertExceptionCodeWithArgCount(ex, GeoServerOAuth2FilterConfigException.OAUTH2_JWK_SET_URI_REQUIRED, 0);
        }

        config.setOidcJwkSetUri("lalala");
        try {
            lValidate.call();
        } catch (GeoServerOAuth2FilterConfigException ex) {
            assertExceptionCodeWithArgCount(ex, GeoServerOAuth2FilterConfigException.OAUTH2_WKTS_URL_MALFORMED, 0);
        }

        config.setOidcJwkSetUri("https://jwkset");

        validator.validateOAuth2FilterConfig(config);

        config.setOidcUsePKCE(true);
        config.setOidcClientSecret(null);
        validator.validateOAuth2FilterConfig(config);

        config.setOidcUsePKCE(false);
        config.setOidcClientSecret("oauth2clientsecret");

        config.setGoogleEnabled(true);
        config.setGoogleClientId("googleclientId");
        config.setGoogleClientSecret("googleClientSecret");

        config.setGitHubEnabled(true);
        config.setGitHubClientId("gitHubClientId");
        config.setGitHubClientSecret("gitHubClientSecret");

        config.setMsEnabled(true);
        config.setMsClientId("msClientId");
        config.setMsClientSecret("msClientSecret");
    }

    @Test
    public void testRoleSourceIdToken() throws Exception {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        config.setName("testOAuth2");

        // given: roleSource ID Token
        config.setRoleSource(OpenIdRoleSource.IdToken);

        // when: Google enabled
        config.setGoogleEnabled(true);
        config.setGoogleClientId("gid");
        config.setGoogleClientSecret("gs");

        // then: OK
        validator.validateOAuth2FilterConfig(config);

        // when: MS enabled
        config.setMsEnabled(true);
        config.setMsClientId("mid");
        config.setMsClientSecret("ms");

        // then: OK
        validator.validateOAuth2FilterConfig(config);

        // when: OIDC enabled
        enableOidcValid(config);

        // then: OK
        validator.validateOAuth2FilterConfig(config);

        // when: github enabled
        config.setGitHubEnabled(true);
        config.setGitHubClientId("ghid");
        config.setGitHubClientSecret("ghs");

        // then: fail, not supported
        try {
            validator.validateOAuth2FilterConfig(config);
            fail("Expected FilterConfigException");
        } catch (FilterConfigException ex) {
            assertExceptionCodeWithArgCount(
                    ex, GeoServerOAuth2FilterConfigException.ROLE_SOURCE_ID_TOKEN_INVALID_FOR_GITHUB, 0);
        }
    }

    @Test
    public void testRoleSourceUserInfo() throws Exception {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        config.setName("testOAuth2");

        // given: roleSource user Info
        config.setRoleSource(OpenIdRoleSource.UserInfo);

        // when: OIDC enabled
        enableOidcValid(config);

        // then: fail, not supported
        try {
            validator.validateOAuth2FilterConfig(config);
            fail("Expected FilterConfigException");
        } catch (FilterConfigException ex) {
            assertExceptionCodeWithArgCount(
                    ex, GeoServerOAuth2FilterConfigException.ROLE_SOURCE_USER_INFO_URI_REQUIRED, 0);
        }

        config.setOidcUserInfoUri("https://userinfo");
        validator.validateOAuth2FilterConfig(config);
    }

    private void enableOidcValid(GeoServerOAuth2LoginFilterConfig config) {
        config.setOidcEnabled(true);
        config.setOidcClientId("oid");
        config.setOidcClientSecret("os");
        config.setOidcAuthorizationUri("https://a");
        config.setOidcTokenUri("https://t");
        config.setOidcJwkSetUri("https://j");
    }

    @Test
    public void testOAuth2FilterConfigValidationForMsGraph() throws Exception {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        config.setName("testOAuth2");

        // given: role source MSGraph
        config.setRoleSource(OpenIdRoleSource.MSGraphAPI);

        // Google IDP
        config.setGoogleEnabled(true);
        config.setGoogleClientId("ci");
        config.setGoogleClientSecret("cs");

        try {
            // when: validate
            validator.validateOAuth2FilterConfig(config);
            fail("Expected FilterConfigException");
        } catch (FilterConfigException ex) {
            assertExceptionCodeWithArgCount(ex, MSGRAPH_COMBINATION_INVALID, 0);
        }

        // given: additionally MS
        config.setMsEnabled(true);
        config.setMsClientId("ci");
        config.setMsClientSecret("cs");

        try {
            // when: validate
            validator.validateOAuth2FilterConfig(config);
            fail("Expected FilterConfigException");
        } catch (FilterConfigException ex) {
            // then: still failed - combine not allowed
            assertExceptionCodeWithArgCount(ex, MSGRAPH_COMBINATION_INVALID, 0);
        }

        // given: only MS
        config.setGoogleEnabled(false);

        // when: validate, then: OK
        validator.validateOAuth2FilterConfig(config);
    }

    private void assertExceptionCodeWithArgCount(FilterConfigException pException, String pCode, int pExArgCount) {
        assertEquals(pCode, pException.getId());
        assertEquals(pExArgCount, pException.getArgs().length);
        LOGGER.info(pException.getMessage());
    }
}
