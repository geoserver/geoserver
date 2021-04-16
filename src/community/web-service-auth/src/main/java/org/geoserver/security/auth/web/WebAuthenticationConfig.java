/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import java.util.Arrays;
import java.util.List;
import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;

/** @author Imran Rajjad - Geo Solutions */
public class WebAuthenticationConfig extends BaseSecurityNamedServiceConfig
        implements SecurityAuthProviderConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = -41214123098267966L;

    public static final String URL_PLACEHOLDER_USER = "{user}";
    public static final String URL_PLACEHOLDER_PASSWORD = "{password}";

    public static final int DEFAULT_TIME_OUT = 30;
    public static final int DEFAULT_READTIME_OUT = 30;

    public static final String AUTHORIZATION_RADIO_OPTION_WEB = "Web Response";
    public static final String AUTHORIZATION_RADIO_OPTION_SERVICE = "Existing Role Service";

    public static final List<String> AUTHORIZATION_RADIO_OPTIONS =
            Arrays.asList(
                    new String[] {
                        AUTHORIZATION_RADIO_OPTION_SERVICE, AUTHORIZATION_RADIO_OPTION_WEB
                    });

    private String connectionURL;
    private int readTimeoutOut = DEFAULT_READTIME_OUT;
    private int connectionTimeOut = DEFAULT_TIME_OUT;
    private String roleRegex;
    private String roleServiceName;
    private boolean useHeader;
    private String authorizationOption = AUTHORIZATION_RADIO_OPTION_WEB;

    private boolean allowHTTPConnection;

    public WebAuthenticationConfig() {
        super();
    }

    public WebAuthenticationConfig(WebAuthenticationConfig other) {
        super(other);
        this.connectionURL = other.getConnectionURL();
        this.roleRegex = other.getRoleRegex();
        this.roleServiceName = other.getRoleServiceName();
        this.readTimeoutOut = other.getReadTimeoutOut();
        this.connectionTimeOut = other.getConnectionTimeOut();
        this.useHeader = other.isUseHeader();
        this.authorizationOption = other.getAuthorizationOption();
        this.allowHTTPConnection = other.isAllowHTTPConnection();
    }

    /** @return the connectionURL */
    public String getConnectionURL() {
        return connectionURL;
    }

    /** @param connectionURL the connectionURL to set */
    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    /** @return the roleRegex */
    public String getRoleRegex() {
        return roleRegex;
    }

    /** @param roleRegex the roleRegex to set */
    public void setRoleRegex(String roleRegex) {
        this.roleRegex = roleRegex;
    }

    /** @return the roleServiceName */
    public String getRoleServiceName() {
        return roleServiceName;
    }

    /** @param roleServiceName the roleServiceName to set */
    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    /** @return the readTimeoutOut */
    public int getReadTimeoutOut() {
        return readTimeoutOut;
    }

    /** @param readTimeoutOut the readTimeoutOut to set */
    public void setReadTimeoutOut(int readTimeoutOut) {
        this.readTimeoutOut = readTimeoutOut;
    }

    /** @return the connectionTimeOut */
    public int getConnectionTimeOut() {
        return connectionTimeOut;
    }

    /** @param connectionTimeOut the connectionTimeOut to set */
    public void setConnectionTimeOut(int connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
    }

    /** @return the useHeader */
    public boolean isUseHeader() {
        return useHeader;
    }

    /** @param useHeader the useHeader to set */
    public void setUseHeader(boolean useHeader) {
        this.useHeader = useHeader;
    }

    /** @return the authorizationOption */
    public String getAuthorizationOption() {
        return authorizationOption;
    }

    /** @param authorizationOption the authorizationOption to set */
    public void setAuthorizationOption(String authorizationOption) {
        this.authorizationOption = authorizationOption;
    }

    @Override
    public String getUserGroupServiceName() {
        return null;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        // empty
    }

    public boolean isAllowHTTPConnection() {
        return allowHTTPConnection;
    }

    public void setAllowHTTPConnection(boolean allowHTTPConnection) {
        this.allowHTTPConnection = allowHTTPConnection;
    }

    @Override
    public String toString() {
        return "WebAuthenticationConfig [connectionURL="
                + connectionURL
                + ", readTimeoutOut="
                + readTimeoutOut
                + ", connectionTimeOut="
                + connectionTimeOut
                + ", roleRegex="
                + roleRegex
                + ", roleServiceName="
                + roleServiceName
                + ", useHeader="
                + useHeader
                + ", authorizationOption="
                + authorizationOption
                + ", allowHTTPConnection"
                + allowHTTPConnection
                + "]";
    }
}
