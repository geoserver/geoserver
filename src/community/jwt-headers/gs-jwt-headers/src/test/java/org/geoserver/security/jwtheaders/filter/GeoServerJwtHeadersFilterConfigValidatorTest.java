/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.junit.Test;

/** Save-time rules on the JWT Headers specific settings. */
public class GeoServerJwtHeadersFilterConfigValidatorTest {

    private final GeoServerJwtHeadersFilterConfigValidator validator =
            new GeoServerJwtHeadersFilterConfigValidator(null);

    /** A new filter, completed the way the documentation describes for bearer tokens. */
    private static GeoServerJwtHeadersFilterConfig validatingConfig() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setUserNameHeaderAttributeName("Authorization");
        jwt.setUserNameJsonPath("preferred_username");
        jwt.setValidateTokenSignatureURL("https://idp.example.org/realms/r/protocol/openid-connect/certs");
        jwt.setValidateTokenIssuer("https://idp.example.org/realms/r");
        config.setRoleSource(JWTHeaderRoleSource.JWT);
        jwt.setRolesHeaderName("Authorization");
        jwt.setRolesJsonPath("resource_access.geoserver.roles");
        return config;
    }

    private void assertRejected(GeoServerJwtHeadersFilterConfig config, String errorId) {
        FilterConfigException e = assertThrows(
                FilterConfigException.class, () -> validator.validateGeoServerJwtHeadersFilterConfig(config));
        assertEquals(errorId, e.getId());
    }

    @Test
    public void testValidConfiguration() throws Exception {
        validator.validateGeoServerJwtHeadersFilterConfig(validatingConfig());
    }

    @Test
    public void testNewFilterNeedsKeySetUrl() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateTokenSignatureURL(null);
        assertRejected(config, JwtHeadersFilterConfigException.SIGNATURE_URL_INVALID);

        config.getJwtConfiguration().setValidateTokenSignatureURL("not a url");
        assertRejected(config, JwtHeadersFilterConfigException.SIGNATURE_URL_INVALID);
    }

    @Test
    public void testKeySetUrlMayBeLocalFile() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateTokenSignatureURL("file:///opt/geoserver/jwks.json");
        validator.validateGeoServerJwtHeadersFilterConfig(config);
    }

    @Test
    public void testEndpointMustBeHttp() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateTokenAgainstURL(true);
        config.getJwtConfiguration().setValidateTokenAgainstURLEndpoint("file:///opt/geoserver/userinfo.json");
        assertRejected(config, JwtHeadersFilterConfigException.ENDPOINT_URL_INVALID);
    }

    /** Only a JWT can be validated: a proxy-set JSON or plain header has to be trusted as such. */
    @Test
    public void testValidationNeedsJwtFormat() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JSON);
        assertRejected(config, JwtHeadersFilterConfigException.VALIDATION_NEEDS_JWT_FORMAT);

        config.getJwtConfiguration().setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.STRING);
        assertRejected(config, JwtHeadersFilterConfigException.VALIDATION_NEEDS_JWT_FORMAT);
    }

    @Test
    public void testSignatureValidationNeedsIssuer() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateTokenIssuer(" ");
        assertRejected(config, JwtHeadersFilterConfigException.ISSUER_NEEDED);
    }

    @Test
    public void testValidationNeedsSignatureOrEndpoint() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateTokenSignature(false);
        assertRejected(config, JwtHeadersFilterConfigException.VALIDATION_METHOD_NEEDED);

        config.getJwtConfiguration().setValidateTokenAgainstURL(true);
        assertRejected(config, JwtHeadersFilterConfigException.ENDPOINT_URL_INVALID);

        config.getJwtConfiguration().setValidateTokenAgainstURLEndpoint("https://idp.example.org/userinfo");
        validator.validateGeoServerJwtHeadersFilterConfig(config);
    }

    @Test
    public void testAudienceNeedsNameAndValue() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateTokenAudience(true);
        config.getJwtConfiguration().setValidateTokenAudienceClaimName("aud");
        assertRejected(config, JwtHeadersFilterConfigException.AUDIENCE_NEEDED);
    }

    @Test
    public void testRolesFromSeparateHeaderRejectedWhenValidating() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setRolesHeaderName("X-Roles");
        assertRejected(config, JwtHeadersFilterConfigException.ROLES_NOT_FROM_VALIDATED_TOKEN);

        config.setTrustUnvalidatedRolesHeader(true);
        validator.validateGeoServerJwtHeadersFilterConfig(config);
    }

    @Test
    public void testHeaderOrJsonRoleSourceRejectedWhenValidating() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.setRoleSource(JWTHeaderRoleSource.JSON);
        assertRejected(config, JwtHeadersFilterConfigException.ROLES_NOT_FROM_VALIDATED_TOKEN);

        config.setRoleSource(JWTHeaderRoleSource.Header);
        assertRejected(config, JwtHeadersFilterConfigException.ROLES_NOT_FROM_VALIDATED_TOKEN);
    }

    @Test
    public void testRolesFromIdentityHeaderOrServicesAccepted() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setRolesHeaderName("authorization");
        validator.validateGeoServerJwtHeadersFilterConfig(config);

        config.getJwtConfiguration().setRolesHeaderName(null);
        validator.validateGeoServerJwtHeadersFilterConfig(config);

        config.setRoleSource(JWTHeaderRoleSource.RoleService);
        validator.validateGeoServerJwtHeadersFilterConfig(config);
    }

    /** Proxy-trust mode: the header is set by an authenticating proxy and is not validated here. */
    @Test
    public void testSeparateRolesHeaderAllowedWithoutValidation() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setValidateToken(false);
        config.getJwtConfiguration().setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JSON);
        config.setRoleSource(JWTHeaderRoleSource.JSON);
        config.getJwtConfiguration().setRolesHeaderName("X-Roles");
        validator.validateGeoServerJwtHeadersFilterConfig(config);
    }

    @Test
    public void testPathsRequired() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig();
        config.getJwtConfiguration().setUserNameJsonPath(null);
        assertRejected(config, JwtHeadersFilterConfigException.USER_NAME_PATH_NEEDED);

        config = validatingConfig();
        config.getJwtConfiguration().setRolesJsonPath("");
        assertRejected(config, JwtHeadersFilterConfigException.ROLES_PATH_NEEDED);

        config = validatingConfig();
        config.getJwtConfiguration().setUserNameHeaderAttributeName(null);
        assertRejected(config, JwtHeadersFilterConfigException.USER_NAME_HEADER_NEEDED);
    }
}
