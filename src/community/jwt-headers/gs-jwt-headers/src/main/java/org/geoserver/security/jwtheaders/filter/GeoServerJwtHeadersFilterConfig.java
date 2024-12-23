/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

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

    private static final long serialVersionUID = 1L;

    // generic required for saving config
    protected String id;
    protected String name;
    protected String className;

    // used by super-class
    protected String userGroupServiceName;

    protected JwtConfiguration jwtConfiguration = new JwtConfiguration();

    public GeoServerJwtHeadersFilterConfig() {
        jwtConfiguration = new JwtConfiguration();
    }

    public org.geoserver.security.jwtheaders.JwtConfiguration getJwtConfiguration() {
        return jwtConfiguration;
    }

    public void setJwtConfiguration(org.geoserver.security.jwtheaders.JwtConfiguration jwtConfiguration) {
        jwtConfiguration = jwtConfiguration;
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

    public String getUserNameHeaderAttributeName() {
        return jwtConfiguration.getUserNameHeaderAttributeName();
    }

    public void setUserNameHeaderAttributeName(String userNameHeaderAttributeName) {
        jwtConfiguration.setUserNameHeaderAttributeName(userNameHeaderAttributeName);
    }
}
