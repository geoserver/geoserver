/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.ROOT_USERNAME;

import java.io.Serial;
import java.text.Normalizer;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.*;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geotools.util.logging.Logging;

/** configuration of the JWT Header Filter. */
public class GeoServerJwtHeadersFilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig, SecurityAuthProviderConfig, Cloneable {

    private static final Logger LOG = Logging.getLogger(GeoServerJwtHeadersFilterConfig.class);

    @Serial
    private static final long serialVersionUID = 1L;

    // generic required for saving config
    protected String id;
    protected String name;
    protected String className;

    // used by super-class
    protected String userGroupServiceName;

    protected JwtConfiguration jwtConfiguration = new JwtConfiguration();

    /**
     * Whether the built-in {@code admin} account may be asserted by the header. Null (configurations written before
     * this option existed) means allowed.
     */
    private Boolean allowAdminLogin;

    /**
     * Whether roles may be read from a header that is not the validated token while token validation is on. Null
     * (configurations written before this option existed) means not trusted.
     */
    private Boolean trustUnvalidatedRolesHeader;

    /** Whether token content is logged at FINE level, for troubleshooting a setup. Null means off. */
    private Boolean logSensitiveInformation;

    /**
     * Defaults for a newly created filter: validate the token and its signature and expiry, only accept explicitly
     * mapped roles, and do not let the header assert the built-in {@code admin} account.
     *
     * <p>Configurations read from the data directory or the REST API do not go through this constructor, so existing
     * filters keep their stored settings.
     */
    public GeoServerJwtHeadersFilterConfig() {
        jwtConfiguration = new JwtConfiguration();
        jwtConfiguration.setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JWT);
        jwtConfiguration.setValidateToken(true);
        jwtConfiguration.setValidateTokenSignature(true);
        jwtConfiguration.setValidateTokenExpiry(true);
        jwtConfiguration.setOnlyExternalListedRoles(true);
        allowAdminLogin = Boolean.FALSE;
    }

    public org.geoserver.security.jwtheaders.JwtConfiguration getJwtConfiguration() {
        return jwtConfiguration;
    }

    public void setJwtConfiguration(org.geoserver.security.jwtheaders.JwtConfiguration jwtConfiguration) {
        this.jwtConfiguration = jwtConfiguration;
    }

    /**
     * Whether the header may assert the built-in {@code admin} account. Defaults to {@code true} for configurations
     * written before this option existed, {@code false} for new filters.
     *
     * @return never null
     */
    public Boolean getAllowAdminLogin() {
        return allowAdminLogin == null ? Boolean.TRUE : allowAdminLogin;
    }

    public void setAllowAdminLogin(Boolean allowAdminLogin) {
        this.allowAdminLogin = allowAdminLogin;
    }

    /**
     * Whether roles may be read from a header that is not the validated token while token validation is on. Only safe
     * when a proxy in front of GeoServer always removes or overwrites that header.
     *
     * @return never null
     */
    public Boolean getTrustUnvalidatedRolesHeader() {
        return trustUnvalidatedRolesHeader == null ? Boolean.FALSE : trustUnvalidatedRolesHeader;
    }

    public void setTrustUnvalidatedRolesHeader(Boolean trustUnvalidatedRolesHeader) {
        this.trustUnvalidatedRolesHeader = trustUnvalidatedRolesHeader;
    }

    /**
     * Whether the decoded token content, the full reason a token is rejected and the claims the user name and roles are
     * taken from are logged at FINE level. Meant for troubleshooting only, the logs then contain personal data.
     *
     * @return never null
     */
    public Boolean getLogSensitiveInformation() {
        return logSensitiveInformation == null ? Boolean.FALSE : logSensitiveInformation;
    }

    public void setLogSensitiveInformation(Boolean logSensitiveInformation) {
        this.logSensitiveInformation = logSensitiveInformation;
    }

    /**
     * Tells whether a principal taken from the header must be refused because it is a built-in administrator account.
     *
     * <p>{@code root} is always refused: it is the emergency account backed by the master password and can never be
     * represented by an external identity. {@code admin} is refused when {@link #getAllowAdminLogin()} is false.
     *
     * @param principal the principal name from the header, may be null
     * @return true when the principal must not be authenticated by this filter
     */
    public boolean isPrincipalBlocked(String principal) {
        if (principal == null) {
            return false;
        }
        String name = canonicalName(principal);
        if (ROOT_USERNAME.equalsIgnoreCase(name)) {
            return true;
        }
        return ADMIN_USERNAME.equalsIgnoreCase(name) && !getAllowAdminLogin();
    }

    /** Compatibility-normalised name without whitespace, control or invisible formatting characters. */
    private static String canonicalName(String principal) {
        String normalized = Normalizer.normalize(principal, Normalizer.Form.NFKC);
        StringBuilder name = new StringBuilder(normalized.length());
        normalized
                .codePoints()
                .filter(cp -> !Character.isWhitespace(cp)
                        && !Character.isSpaceChar(cp)
                        && !Character.isISOControl(cp)
                        && Character.getType(cp) != Character.FORMAT)
                .forEach(name::appendCodePoint);
        return name.toString();
    }

    /** True when the roles header is the user name header (a blank roles header name means the user name header). */
    public boolean rolesHeaderIsUserNameHeader() {
        String rolesHeader = jwtConfiguration.getRolesHeaderName();
        return rolesHeader == null
                || rolesHeader.isBlank()
                || rolesHeader.trim().equalsIgnoreCase(jwtConfiguration.getUserNameHeaderAttributeName());
    }

    /**
     * True when the token is validated but the role source reads a request header other than the validated token, and
     * that header is not explicitly trusted. No roles are taken from such a header.
     */
    public boolean readsRolesFromUnvalidatedHeader() {
        if (!jwtConfiguration.isValidateToken() || getTrustUnvalidatedRolesHeader()) return false;

        RoleSource source = getRoleSource();
        if (JWTHeaderRoleSource.JWT.equals(source)) return !rolesHeaderIsUserNameHeader();
        return JWTHeaderRoleSource.JSON.equals(source) || JWTHeaderRoleSource.Header.equals(source);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    @Override
    public void initBeforeSave() {
        // no-op
    }

    // NOTE: This implementation does a soft-copy only, and is generally pretty garbage. It isn't
    // clear if a real deep-copy is needed, or what (if anything) relies on this cloning-capability,
    // so the (rather significant) effort of manually setting all the properties has been skipped.
    // Don't be surprised if the copies behave badly though.
    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {
        final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);
        GeoServerJwtHeadersFilterConfig target;
        try {
            target = (GeoServerJwtHeadersFilterConfig) this.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        }

        if (target != null
                && allowEnvParametrization
                && gsEnvironment != null
                && GeoServerEnvironment.allowEnvParametrization()) {
            target.setName((String) gsEnvironment.resolveValue(name));
        }

        return target;
    }

    /** what formats we support for roles in the header. */
    public enum JWTHeaderRoleSource implements RoleSource {
        JSON,
        JWT,

        // From: PreAuthenticatedUserNameFilterConfig
        Header,
        UserGroupService,
        RoleService;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    }

    // ===================================================================

    @Override
    public RoleSource getRoleSource() {
        var val = jwtConfiguration.getJwtHeaderRoleSource();
        if (val == null) return null;
        return JWTHeaderRoleSource.valueOf(val);
    }

    @Override
    public void setRoleSource(RoleSource roleSource) {
        super.setRoleSource(roleSource);
        var strVal = roleSource == null ? null : roleSource.toString();
        jwtConfiguration.setJwtHeaderRoleSource(strVal);
    }

    // ---------------------------------------------------------------------

    public JwtConfiguration.UserNameHeaderFormat getUserNameFormatChoice() {
        return jwtConfiguration.getUserNameFormatChoice();
    }

    public void setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat userNameFormatChoice) {
        jwtConfiguration.setUserNameFormatChoice(userNameFormatChoice);
    }

    public String getUserNameJsonPath() {
        return jwtConfiguration.getUserNameJsonPath();
    }

    public void setUserNameJsonPath(String userNameJsonPath) {
        jwtConfiguration.setUserNameJsonPath(userNameJsonPath);
    }

    public String getRolesHeaderName() {
        return jwtConfiguration.getRolesHeaderName();
    }

    public void setRolesHeaderName(String rolesHeaderName) {
        jwtConfiguration.setRolesHeaderName(rolesHeaderName);
    }

    public String getRolesJsonPath() {
        return jwtConfiguration.getRolesJsonPath();
    }

    public void setRolesJsonPath(String rolesJsonPath) {
        jwtConfiguration.setRolesJsonPath(rolesJsonPath);
    }

    public String getRoleConverterString() {
        return jwtConfiguration.getRoleConverterString();
    }

    public void setRoleConverterString(String roleConverterString) {
        jwtConfiguration.setRoleConverterString(roleConverterString);
    }

    public boolean isOnlyExternalListedRoles() {
        return jwtConfiguration.isOnlyExternalListedRoles();
    }

    public void setOnlyExternalListedRoles(boolean onlyExternalListedRoles) {
        jwtConfiguration.setOnlyExternalListedRoles(onlyExternalListedRoles);
    }

    public boolean isValidateToken() {
        return jwtConfiguration.isValidateToken();
    }

    public void setValidateToken(boolean validateToken) {
        jwtConfiguration.setValidateToken(validateToken);
    }

    public boolean isValidateTokenExpiry() {
        return jwtConfiguration.isValidateTokenExpiry();
    }

    public void setValidateTokenExpiry(boolean validateTokenExpiry) {
        jwtConfiguration.setValidateTokenExpiry(validateTokenExpiry);
    }

    public boolean isValidateTokenSignature() {
        return jwtConfiguration.isValidateTokenSignature();
    }

    public void setValidateTokenSignature(boolean validateTokenSignature) {
        jwtConfiguration.setValidateTokenSignature(validateTokenSignature);
    }

    public String getValidateTokenSignatureURL() {
        return jwtConfiguration.getValidateTokenSignatureURL();
    }

    public void setValidateTokenSignatureURL(String validateTokenSignatureURL) {
        jwtConfiguration.setValidateTokenSignatureURL(validateTokenSignatureURL);
    }

    public boolean isValidateTokenAgainstURL() {
        return jwtConfiguration.isValidateTokenAgainstURL();
    }

    public void setValidateTokenAgainstURL(boolean validateTokenAgainstURL) {
        jwtConfiguration.setValidateTokenAgainstURL(validateTokenAgainstURL);
    }

    public String getValidateTokenAgainstURLEndpoint() {
        return jwtConfiguration.getValidateTokenAgainstURLEndpoint();
    }

    public void setValidateTokenAgainstURLEndpoint(String validateTokenAgainstURLEndpoint) {
        jwtConfiguration.setValidateTokenAgainstURLEndpoint(validateTokenAgainstURLEndpoint);
    }

    public boolean isValidateSubjectWithEndpoint() {
        return jwtConfiguration.isValidateSubjectWithEndpoint();
    }

    public void setValidateSubjectWithEndpoint(boolean validateSubjectWithEndpoint) {
        jwtConfiguration.setValidateSubjectWithEndpoint(validateSubjectWithEndpoint);
    }

    public boolean isValidateTokenAudience() {
        return jwtConfiguration.isValidateTokenAudience();
    }

    public void setValidateTokenAudience(boolean validateTokenAudience) {
        jwtConfiguration.setValidateTokenAudience(validateTokenAudience);
    }

    public String getValidateTokenAudienceClaimName() {
        return jwtConfiguration.getValidateTokenAudienceClaimName();
    }

    public void setValidateTokenAudienceClaimName(String validateTokenAudienceClaimName) {
        jwtConfiguration.setValidateTokenAudienceClaimName(validateTokenAudienceClaimName);
    }

    public String getValidateTokenAudienceClaimValue() {
        return jwtConfiguration.getValidateTokenAudienceClaimValue();
    }

    public void setValidateTokenAudienceClaimValue(String validateTokenAudienceClaimValue) {
        jwtConfiguration.setValidateTokenAudienceClaimValue(validateTokenAudienceClaimValue);
    }

    public String getValidateTokenIssuer() {
        return jwtConfiguration.getValidateTokenIssuer();
    }

    public void setValidateTokenIssuer(String validateTokenIssuer) {
        jwtConfiguration.setValidateTokenIssuer(validateTokenIssuer);
    }

    public String getUserNameHeaderAttributeName() {
        return jwtConfiguration.getUserNameHeaderAttributeName();
    }

    public void setUserNameHeaderAttributeName(String userNameHeaderAttributeName) {
        jwtConfiguration.setUserNameHeaderAttributeName(userNameHeaderAttributeName);
    }
}
