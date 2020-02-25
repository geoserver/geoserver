/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;

/**
 * Configuration panel for {@link GeoServerX509CertificateAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class X509AuthFilterPanel
        extends J2eeBaseAuthFilterPanel<X509CertificateAuthenticationFilterConfig> {

    public X509AuthFilterPanel(String id, IModel<X509CertificateAuthenticationFilterConfig> model) {
        super(id, model);
    }
}
