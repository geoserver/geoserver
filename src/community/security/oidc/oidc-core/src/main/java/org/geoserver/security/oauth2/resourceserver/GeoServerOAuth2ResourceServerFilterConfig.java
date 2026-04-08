/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import java.io.Serial;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.SecurityAuthFilterConfig;

/**
 * Configuration for {@link GeoServerOAuth2ResourceServerAuthenticationFilter}.
 *
 * <p>Used for the "Resource Server" use case. Implementation is unfinished, because a different GS extension supports
 * this case already. Filter is not offered in UI. This code is never executed.
 *
 * @author awaterme
 */
public class GeoServerOAuth2ResourceServerFilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig {

    @Serial
    private static final long serialVersionUID = -8581346584859849111L;

    /**
     * Add extra logging. NOTE: this might spill confidential information to the log - do not turn on in normal
     * operation!
     */
    boolean allowUnSecureLogging = false;

    private String issuerUri;

    public GeoServerOAuth2ResourceServerFilterConfig() {
        super();
    }

    /** @return the issuerUri */
    public String getIssuerUri() {
        return issuerUri;
    }

    /** @param pIssuerUri the issuerUri to set */
    public void setIssuerUri(String pIssuerUri) {
        issuerUri = pIssuerUri;
    }
}
