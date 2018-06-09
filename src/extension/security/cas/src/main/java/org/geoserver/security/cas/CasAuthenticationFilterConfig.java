/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;

/**
 * Configuration for cas authentication receiving proxy tickets
 *
 * @author mcr
 */
public class CasAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig {

    private static final long serialVersionUID = 1L;

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
}
