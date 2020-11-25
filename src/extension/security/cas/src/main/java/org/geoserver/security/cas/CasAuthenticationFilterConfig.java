/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;

/**
 * Configuration for cas authentication receiving proxy tickets
 *
 * @author mcr
 */
public class CasAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig {

    private static final long serialVersionUID = 1L;

    /**
     * RoleSource list specific to CAS. To be used in addition to {@link
     * org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource}
     *
     * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
     */
    public static enum CasSpecificRoleSource implements RoleSource {
        CustomAttribute;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    };

    /** if true, no single sign on possible */
    private boolean sendRenew;

    /**
     * The CAS server URL including context root
     *
     * <p>example "https://localhost:9443/cas"
     */
    private String casServerUrlPrefix;

    /**
     * The geoserver url for the proxy callback
     *
     * <p>example: http://localhost:8080/geoserver
     */
    private String proxyCallbackUrlPrefix;

    /**
     * Optional:
     *
     * <p>After a successful CAS logout triggered by geoserver, a cas response page is rendered.
     *
     * <p>This url should be rendered as a link in the CAS response page.
     *
     * <p>example: https://myhost:8443/geoserver
     */
    private String urlInCasLogoutPage;

    /**
     * Name of the custom attribute originating roles when using {@link
     * CasSpecificRoleSource#CustomAttribute}
     */
    private String customAttributeName;

    /** Participate in Single Sign Out. */
    private boolean singleSignOut;

    public boolean isSendRenew() {
        return sendRenew;
    }

    public void setSendRenew(boolean sendRenew) {
        this.sendRenew = sendRenew;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    public String getProxyCallbackUrlPrefix() {
        return proxyCallbackUrlPrefix;
    }

    public void setProxyCallbackUrlPrefix(String proxyCallbackUrlPrefix) {
        this.proxyCallbackUrlPrefix = proxyCallbackUrlPrefix;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    public String getUrlInCasLogoutPage() {
        return urlInCasLogoutPage;
    }

    public void setUrlInCasLogoutPage(String urlInCasLogoutPage) {
        this.urlInCasLogoutPage = urlInCasLogoutPage;
    }

    public boolean isSingleSignOut() {
        return singleSignOut;
    }

    public void setSingleSignOut(boolean singleSignOut) {
        this.singleSignOut = singleSignOut;
    }

    public String getCustomAttributeName() {
        return customAttributeName;
    }

    public void setCustomAttributeName(String customAttributeName) {
        this.customAttributeName = customAttributeName;
    }
}
