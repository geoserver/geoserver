/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;

/**
 * {@link GeoServerRequestHeaderAuthenticationFilter} configuration object.
 *
 * <p>{@link #getPrincipalHeaderAttribute()} is the name of the header containing the principal
 * name.
 *
 * @author christian
 */
public class RequestHeaderAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig {

    private String principalHeaderAttribute;

    private static final long serialVersionUID = 1L;

    public String getPrincipalHeaderAttribute() {
        return principalHeaderAttribute;
    }

    public void setPrincipalHeaderAttribute(String principalHeaderAttribute) {
        this.principalHeaderAttribute = principalHeaderAttribute;
    }
}
