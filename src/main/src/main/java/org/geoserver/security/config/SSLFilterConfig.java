/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerSSLFilter;

/**
 * Configuration for {@link GeoServerSSLFilter}
 *
 * @author mcr
 */
public class SSLFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;

    /** The SSL port to use for a HTTP redirect Default is 443 */
    private Integer sslPort = 443;

    public Integer getSslPort() {
        return sslPort;
    }

    public void setSslPort(Integer sslPort) {
        this.sslPort = sslPort;
    }
}
