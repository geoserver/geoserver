/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

/**
 * X509 Authentication Filter
 *
 * @author mcr
 */
public class GeoServerX509CertificateAuthenticationFilter
        extends GeoServerJ2eeBaseAuthenticationFilter {

    private X509PrincipalExtractor principalExtractor;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        //      not needed at the moment
        //        X509CertificateAuthenticationFilterConfig authConfig =
        //                (X509CertificateAuthenticationFilterConfig) config;
        setPrincipalExtractor(new SubjectDnX509PrincipalExtractor());
    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        X509Certificate[] certs =
                (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) return null;

        X509Certificate cert = certs[0];
        String principal = (String) principalExtractor.extractPrincipal(cert);

        if (principal != null && principal.trim().length() == 0) principal = null;

        return principal;
    }

    public X509PrincipalExtractor getPrincipalExtractor() {
        return principalExtractor;
    }

    public void setPrincipalExtractor(X509PrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }
}
