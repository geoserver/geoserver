/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class CORSConfiguration implements Serializable {
    @Serial
    private static final long serialVersionUID = 975607561778404268L;

    public static final String CORS_CONFIGURATION_METADATA_KEY = "corsConfigurationSettings";

    /** Whether CORS is enabled */
    private Boolean enabled = false;

    private String allowedOriginPatterns = "*";
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private Integer maxAge = 3600;

    public Boolean getSupportsCredentials() {
        return supportsCredentials;
    }

    public void setSupportsCredentials(Boolean supportsCredentials) {
        this.supportsCredentials = supportsCredentials;
    }

    private Boolean supportsCredentials = false;

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(String allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }
}
