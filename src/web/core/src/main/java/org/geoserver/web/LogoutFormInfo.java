/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/**
 * Information about a logout form that should be shown from the main page in the GeoServer UI. The
 * "order" field is based on the "name".
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@SuppressWarnings("serial")
public class LogoutFormInfo extends ComponentInfo<GeoServerBasePage>
        implements Comparable<LogoutFormInfo> {
    String name;
    String icon = "";
    private String logoutPath;

    /** Name of the logout extension; it will determine also the order displayed for the icons */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Path to the icon; the graphic file must be places under resources on the same package of the
     * "componentClass"
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Path to the icon; the graphic file must be places under resources on the same package of the
     * "componentClass"
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /** Name of the logout extension; it will determine also the order displayed for the icons */
    public String getName() {
        return name;
    }

    /**
     * Authentication Security Endpoint invoked by the pluggable form
     *
     * @return the logoutPath
     */
    public String getLogoutPath() {
        return logoutPath;
    }

    /**
     * Authentication Security Endpoint invoked by the pluggable form
     *
     * @param logoutPath the logoutPath to set
     */
    public void setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
    }

    /** Sorts by name the Login extensions */
    public int compareTo(LogoutFormInfo other) {
        return getName().compareTo(other.getName());
    }
}
