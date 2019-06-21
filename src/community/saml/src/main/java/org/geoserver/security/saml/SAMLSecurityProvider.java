/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml;

import java.util.List;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.ConstantFilterChain;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.SecurityManagerListener;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;

/**
 * Security provider for SAML SSO
 *
 * @author Xandros
 */
public class SAMLSecurityProvider extends AbstractFilterProvider
        implements SecurityManagerListener {

    private SAMLAuthenticationProvider samlAuthenticationProvider;

    private ApplicationContext context;

    public SAMLSecurityProvider(GeoServerSecurityManager securityManager) {
        context = securityManager.getApplicationContext();
        this.samlAuthenticationProvider = context.getBean(SAMLAuthenticationProvider.class);
        securityManager.addListener(this);
    }

    /** Adds {@link #SAMLAuthenticationProvider} as {@link #AuthenticationProvider} */
    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        List<GeoServerAuthenticationProvider> aps = securityManager.getAuthenticationProviders();
        if (aps != null && !aps.contains(this.samlAuthenticationProvider)) {
            securityManager.getProviders().add(this.samlAuthenticationProvider);
        }
    }

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("samlAuthentication", SAMLAuthenticationFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return SAMLAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new SAMLAuthenticationFilter(context);
    }

    /**
     * Configures filter chain for:
     *
     * <ul>
     *   <li>IDP login callback for URL: <code>/saml/SSO</code> to {@link #SAMLProcessingFilter}
     *   <li>IDP single logout callback for URL: <code>/saml/SingleLogout</code> to {@link
     *       #SAMLLogoutProcessingFilter}
     *   <li>IDP logout callback for URL: <code>/saml/logout</code> to {@link #SAMLLogoutFilter}
     * </ul>
     */
    @Override
    public void configureFilterChain(GeoServerSecurityFilterChain filterChain) {
        if (filterChain.getRequestChainByName("samlSSOChain") == null) {
            RequestFilterChain samlChain =
                    new ConstantFilterChain(SAMLProcessingFilter.FILTER_URL + "/**");
            samlChain.setFilterNames("samlWebSSOProcessingFilter");
            samlChain.setName("samlSSOChain");
            filterChain.getRequestChains().add(0, samlChain);
        }
        if (filterChain.getRequestChainByName("samlLogoutChain") == null) {
            RequestFilterChain samlChain =
                    new ConstantFilterChain(SAMLLogoutProcessingFilter.FILTER_URL + "/**");
            samlChain.setFilterNames("samlLogoutProcessingFilter");
            samlChain.setName("samlLogoutChain");
            filterChain.getRequestChains().add(0, samlChain);
        }
        if (filterChain.getRequestChainByName("samlLogout") == null) {
            RequestFilterChain samlChain =
                    new ConstantFilterChain(SAMLLogoutFilter.FILTER_URL + "/**");
            samlChain.setFilterNames("samlLogoutFilter");
            samlChain.setName("samlLogout");
            filterChain.getRequestChains().add(0, samlChain);
        }
    }
}
