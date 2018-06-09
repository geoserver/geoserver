/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/**
 * Information about a login form that should be shown from the main page in the GeoServer UI. The
 * "order" field is based on the "name".
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@SuppressWarnings("serial")
public class LoginFormInfo extends ComponentInfo<GeoServerBasePage>
        implements Comparable<LoginFormInfo> {
    String name;
    String icon = "";
    private String include = "";
    private String loginPath;

    /**
     * Name of the login extension; it will determine also the order displayed for the icons
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Path to the icon; the graphic file must be places under resources on the same package of the
     * "componentClass"
     *
     * @return
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Path to the icon; the graphic file must be places under resources on the same package of the
     * "componentClass"
     *
     * @param icon
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * Static HTML Resource to include in the form (if needed).
     *
     * @return the include
     */
    public String getInclude() {
        return include;
    }

    /**
     * Static HTML Resource to include in the form (if needed).
     *
     * @param include the include to set
     */
    public void setInclude(String include) {
        this.include = include;
    }

    /**
     * Name of the login extension; it will determine also the order displayed for the icons
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Authentication Security Endpoint invoked by the pluggable form
     *
     * @return the loginPath
     */
    public String getLoginPath() {
        return loginPath;
    }

    /**
     * Authentication Security Endpoint invoked by the pluggable form
     *
     * @param loginPath the loginPath to set
     */
    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    /** Sorts by name the Login extensions */
    public int compareTo(LoginFormInfo other) {
        return getName().compareTo(other.getName());
    }
}
