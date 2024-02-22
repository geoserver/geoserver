/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.*;
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

    // --- user name extraction
    // how is the username stored in the header
    // i.e. text, JSON string, or JWT?
    protected UserNameHeaderFormat userNameFormatChoice;
    // If the username is stored as json (JSON or JWT), then where
    // in the JSON is the username?
    protected String userNameJsonPath;
    // name of the HTTP header that the roles information is stored in
    protected String rolesHeaderName;

    // ---roles
    // The roles will be either in a JWT or JSON object - where in that
    // object is the list of roles
    protected String rolesJsonPath;
    // string in the format of
    // "externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2"
    protected String roleConverterString;
    // if true, then any external role must be in the roleConverterString map.
    //      If its not in the map, then it will be ignored.
    // if false, then external roles not listed in the roleConverterString will be passed
    // on to GeoServer
    protected boolean onlyExternalListedRoles;
    // show a token be validated?
    protected boolean validateToken;

    protected boolean validateTokenExpiry;

    protected boolean validateTokenSignature;
    protected String validateTokenSignatureURL;

    protected boolean validateTokenAgainstURL;
    protected String validateTokenAgainstURLEndpoint;

    protected boolean validateSubjectWithEndpoint;

    protected boolean validateTokenAudience;
    protected String validateTokenAudienceClaimName;
    protected String validateTokenAudienceClaimValue;

    // --token validation
    // what HTTP header is the user's name stored in
    private String userNameHeaderAttributeName;

    // ------------------ getters/setters ----------------------------

    public boolean isValidateSubjectWithEndpoint() {
        return validateSubjectWithEndpoint;
    }

    public void setValidateSubjectWithEndpoint(boolean validateSubjectWithEndpoint) {
        this.validateSubjectWithEndpoint = validateSubjectWithEndpoint;
    }

    public boolean isValidateTokenExpiry() {
        return validateTokenExpiry;
    }

    public void setValidateTokenExpiry(boolean validateTokenExpiry) {
        this.validateTokenExpiry = validateTokenExpiry;
    }

    public boolean isValidateTokenAudience() {
        return validateTokenAudience;
    }

    public void setValidateTokenAudience(boolean validateTokenAudience) {
        this.validateTokenAudience = validateTokenAudience;
    }

    public String getValidateTokenAudienceClaimName() {
        return validateTokenAudienceClaimName;
    }

    public void setValidateTokenAudienceClaimName(String validateTokenAudienceClaimName) {
        this.validateTokenAudienceClaimName = validateTokenAudienceClaimName;
    }

    public String getValidateTokenAudienceClaimValue() {
        return validateTokenAudienceClaimValue;
    }

    public void setValidateTokenAudienceClaimValue(String validateTokenAudienceClaimValue) {
        this.validateTokenAudienceClaimValue = validateTokenAudienceClaimValue;
    }

    public boolean isValidateTokenAgainstURL() {
        return validateTokenAgainstURL;
    }

    public void setValidateTokenAgainstURL(boolean validateTokenAgainstURL) {
        this.validateTokenAgainstURL = validateTokenAgainstURL;
    }

    public String getValidateTokenAgainstURLEndpoint() {
        return validateTokenAgainstURLEndpoint;
    }

    public void setValidateTokenAgainstURLEndpoint(String validateTokenAgainstURLEndpoint) {
        this.validateTokenAgainstURLEndpoint = validateTokenAgainstURLEndpoint;
    }

    public String getValidateTokenSignatureURL() {
        return validateTokenSignatureURL;
    }

    public void setValidateTokenSignatureURL(String validateTokenSignatureURL) {
        this.validateTokenSignatureURL = validateTokenSignatureURL;
    }

    public boolean isValidateTokenSignature() {
        return validateTokenSignature;
    }

    public void setValidateTokenSignature(boolean validateTokenSignature) {
        this.validateTokenSignature = validateTokenSignature;
    }

    public boolean isValidateToken() {
        return validateToken;
    }

    public void setValidateToken(boolean validateToken) {
        this.validateToken = validateToken;
    }

    public boolean isOnlyExternalListedRoles() {
        return onlyExternalListedRoles;
    }

    public void setOnlyExternalListedRoles(boolean onlyExternalListedRoles) {
        this.onlyExternalListedRoles = onlyExternalListedRoles;
    }

    public String getRoleConverterString() {
        return roleConverterString;
    }

    public void setRoleConverterString(String roleConverterString) {
        this.roleConverterString = roleConverterString;
    }

    public String getRolesHeaderName() {
        return rolesHeaderName;
    }

    public void setRolesHeaderName(String rolesHeaderName) {
        this.rolesHeaderName = rolesHeaderName;
    }

    public UserNameHeaderFormat getUserNameFormatChoice() {
        return userNameFormatChoice;
    }

    public void setUserNameFormatChoice(UserNameHeaderFormat userNameFormatChoice) {
        this.userNameFormatChoice = userNameFormatChoice;
    }

    public String getUserNameJsonPath() {
        return userNameJsonPath;
    }

    public void setUserNameJsonPath(String userNameJsonPath) {
        this.userNameJsonPath = userNameJsonPath;
    }

    public String getRolesJsonPath() {
        return rolesJsonPath;
    }

    public void setRolesJsonPath(String rolesJsonPath) {
        this.rolesJsonPath = rolesJsonPath;
    }

    public String getUserNameHeaderAttributeName() {
        return userNameHeaderAttributeName;
    }

    public void setUserNameHeaderAttributeName(String userNameHeaderAttributeName) {
        this.userNameHeaderAttributeName = userNameHeaderAttributeName;
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
        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);
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

    // convert string of the form:
    // "externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2"
    // To a Map<String,String>
    public Map<String, String> getRoleConverterAsMap() {
        Map<String, String> result = new HashMap<>();

        if (roleConverterString == null || roleConverterString.isBlank()) return result; // empty

        String[] parts = roleConverterString.split(";");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length != 2) {
                continue; // invalid
            }
            String key = goodCharacters(keyValue[0]);
            String val = goodCharacters(keyValue[1]);
            if (key.isBlank() || val.isBlank()) continue;
            result.put(key, val);
        }
        return result;
    }

    public String goodCharacters(String str) {
        return str.replaceAll("[^a-zA-Z0-9_\\-\\.]", "");
    }

    /** what formats we support of the header the username is contained in. */
    public enum UserNameHeaderFormat {
        STRING,
        JSON,
        JWT
    }

    /** what formats we support for roles in the header. */
    public enum JWTHeaderRoleSource implements RoleSource {
        JSON,
        JWT;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    }
}
