/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerX509CertificateAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class X509AuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                X509CertificateAuthenticationFilterConfig, X509AuthFilterPanel> {

    public X509AuthFilterPanelInfo() {
        setComponentClass(X509AuthFilterPanel.class);
        setServiceClass(GeoServerX509CertificateAuthenticationFilter.class);
        setServiceConfigClass(X509CertificateAuthenticationFilterConfig.class);
    }
}
