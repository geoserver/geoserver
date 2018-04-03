/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

/**
 * Base class for WFS 3.0 requests
 */
public abstract class BaseRequest {

    public static final String JSON_MIME = "application/json";
    public static final String YAML_MIME = "application/x-yaml";
    public static final String HTML_MIME = "text/html";
    
    String format;
    String baseUrl;

    /**
     * The requested format
     * @return The format name, or null if not set
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the format name
     * @param format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * The request base url
     * @return The url, or null if not set
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets the base url
     * @param baseUrl
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
