/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;

/**
 * {@link GeoServerX509CertificateAuthenticationFilter} configuration object.
 *
 * <p>
 *
 * @author christian
 */
public class X509CertificateAuthenticationFilterConfig extends J2eeAuthenticationBaseFilterConfig
        implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;
}
