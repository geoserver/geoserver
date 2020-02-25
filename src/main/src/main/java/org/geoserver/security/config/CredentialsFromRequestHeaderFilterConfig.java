/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

/**
 * {@link GeoServerCredentialsFromRequestHeaderAuthenticationFilter} configuration object.
 *
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class CredentialsFromRequestHeaderFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;

    /** The header that contains the username */
    private String userNameHeaderName = "X-Credentials";

    /** The header that contains the password */
    private String passwordHeaderName = "X-Credentials";

    /** The regex to parse the username in the header */
    private String userNameRegex = "private-user=([^&]*)";

    /** The regex to parse the password in the header */
    private String passwordRegex = "private-pw=([^&]*)";

    /** Parse the username and password as URI Components, Converting "%40" to "@" and so on */
    private boolean parseAsUriComponents = true;

    public boolean isParseAsUriComponents() {
        return parseAsUriComponents;
    }

    public void setParseAsUriComponents(boolean parseAsUriComponents) {
        this.parseAsUriComponents = parseAsUriComponents;
    }

    public String getUserNameHeaderName() {
        return userNameHeaderName;
    }

    public void setUserNameHeaderName(String userNameHeaderName) {
        this.userNameHeaderName = userNameHeaderName;
    }

    public String getPasswordHeaderName() {
        return passwordHeaderName;
    }

    public void setPasswordHeaderName(String passwordHeaderName) {
        this.passwordHeaderName = passwordHeaderName;
    }

    public String getUserNameRegex() {
        return userNameRegex;
    }

    public void setUserNameRegex(String userNameRegex) {
        this.userNameRegex = userNameRegex;
    }

    public String getPasswordRegex() {
        return passwordRegex;
    }

    public void setPasswordRegex(String passwordRegex) {
        this.passwordRegex = passwordRegex;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }
}
