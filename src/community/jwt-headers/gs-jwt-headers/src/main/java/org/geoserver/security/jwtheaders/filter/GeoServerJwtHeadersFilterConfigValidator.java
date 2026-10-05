/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.AUDIENCE_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.ENDPOINT_URL_INVALID;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.ISSUER_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.JWT_CONFIGURATION_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.ROLES_NOT_FROM_VALIDATED_TOKEN;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.ROLES_PATH_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.SIGNATURE_URL_INVALID;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.USER_NAME_HEADER_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.USER_NAME_PATH_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.VALIDATION_METHOD_NEEDED;
import static org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException.VALIDATION_NEEDS_JWT_FORMAT;

import java.net.URI;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geoserver.security.jwtheaders.token.TokenIssuerValidator;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.geotools.util.logging.Logging;

/** Validate the configuration. */
public class GeoServerJwtHeadersFilterConfigValidator extends FilterConfigValidator {

    private static final Logger LOG = Logging.getLogger(GeoServerJwtHeadersFilterConfigValidator.class);

    /**
     * Default constructor.
     *
     * @param securityManager the active security manager for the context
     */
    public GeoServerJwtHeadersFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    /** Validates the configuration type and content. */
    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config) throws FilterConfigException {
        if (config instanceof GeoServerJwtHeadersFilterConfig filterConfig) {
            validateGeoServerJwtHeadersFilterConfig(filterConfig);
            // role source and the services it refers to
            super.validateFilterConfig((PreAuthenticatedUserNameFilterConfig) filterConfig);
        } else {
            throw new FilterConfigException(
                    FilterConfigException.CLASS_WRONG_TYPE_$2,
                    "configuration type is not appropriate for the requested filter type",
                    config.getClass().getName(),
                    GeoServerJwtHeadersFilterConfig.class.getName());
        }
    }

    /** Validates the JWT Headers specific settings, from the UI or the REST API. */
    public void validateGeoServerJwtHeadersFilterConfig(GeoServerJwtHeadersFilterConfig config)
            throws FilterConfigException {
        JwtConfiguration jwt = config.getJwtConfiguration();
        if (jwt == null) throw new JwtHeadersFilterConfigException(JWT_CONFIGURATION_NEEDED);

        if (isBlank(jwt.getUserNameHeaderAttributeName()))
            throw new JwtHeadersFilterConfigException(USER_NAME_HEADER_NEEDED);
        if (jwt.getUserNameFormatChoice() != null
                && jwt.getUserNameFormatChoice() != JwtConfiguration.UserNameHeaderFormat.STRING
                && isBlank(jwt.getUserNameJsonPath())) throw new JwtHeadersFilterConfigException(USER_NAME_PATH_NEEDED);

        RoleSource roleSource = config.getRoleSource();
        boolean jsonOrJwtRoles =
                JWTHeaderRoleSource.JWT.equals(roleSource) || JWTHeaderRoleSource.JSON.equals(roleSource);
        if (jsonOrJwtRoles && isBlank(jwt.getRolesJsonPath()))
            throw new JwtHeadersFilterConfigException(ROLES_PATH_NEEDED);

        if (!jwt.isValidateToken()) return;

        // only a JWT can be validated
        if (jwt.getUserNameFormatChoice() != JwtConfiguration.UserNameHeaderFormat.JWT)
            throw new JwtHeadersFilterConfigException(VALIDATION_NEEDS_JWT_FORMAT);
        if (!jwt.isValidateTokenSignature() && !jwt.isValidateTokenAgainstURL())
            throw new JwtHeadersFilterConfigException(VALIDATION_METHOD_NEEDED);
        if (jwt.isValidateTokenSignature()) {
            if (!isAbsoluteUrl(jwt.getValidateTokenSignatureURL()))
                throw new JwtHeadersFilterConfigException(
                        SIGNATURE_URL_INVALID, valueOf(jwt.getValidateTokenSignatureURL()));
            if (TokenIssuerValidator.acceptedIssuers(jwt.getValidateTokenIssuer())
                    .isEmpty()) throw new JwtHeadersFilterConfigException(ISSUER_NEEDED);
        }
        if (jwt.isValidateTokenAgainstURL() && !isHttpUrl(jwt.getValidateTokenAgainstURLEndpoint()))
            throw new JwtHeadersFilterConfigException(
                    ENDPOINT_URL_INVALID, valueOf(jwt.getValidateTokenAgainstURLEndpoint()));
        if (jwt.isValidateTokenAudience()
                && (isBlank(jwt.getValidateTokenAudienceClaimName())
                        || isBlank(jwt.getValidateTokenAudienceClaimValue())))
            throw new JwtHeadersFilterConfigException(AUDIENCE_NEEDED);

        // roles must come from the token that was validated, unless the admin explicitly trusts the header
        if (config.readsRolesFromUnvalidatedHeader())
            throw new JwtHeadersFilterConfigException(ROLES_NOT_FROM_VALIDATED_TOKEN);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String valueOf(String value) {
        return value == null ? "" : value;
    }

    /** An absolute URL the key set can be read from (http, https, or a local file). */
    private static boolean isAbsoluteUrl(String value) {
        if (isBlank(value)) return false;
        try {
            URI uri = new URI(value.trim());
            return uri.isAbsolute() && uri.toURL() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** An http(s) URL, as the endpoint is called over HTTP. */
    private static boolean isHttpUrl(String value) {
        if (isBlank(value)) return false;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
