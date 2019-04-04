/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.ConstantFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;

/**
 * Security provider for CAS
 *
 * @author mcr
 */
public class GeoServerCasAuthenticationProvider extends AbstractFilterProvider {

    static String PROXYRECEPTORCHAIN = "casproxy";
    static Logger LOGGER = Logging.getLogger("org.geoserver.security.cas");

    protected ProxyGrantingTicketCallbackFilter pgtCallback;
    protected ProxyGrantingTicketStorage pgtStorage;

    public GeoServerCasAuthenticationProvider(
            ProxyGrantingTicketCallbackFilter pgtCallback, ProxyGrantingTicketStorage pgtStorage) {
        this.pgtCallback = pgtCallback;
        this.pgtStorage = pgtStorage;
    }

    public GeoServerCasAuthenticationProvider(ProxyGrantingTicketStorage pgtStorage) {
        this.pgtStorage = pgtStorage;
    }

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("casAuthentication", CasAuthenticationFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerCasAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerCasAuthenticationFilter(pgtStorage);
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new CasFilterConfigValidator(securityManager);
    }

    @Override
    public void configureFilterChain(GeoServerSecurityFilterChain filterChain) {

        if (filterChain.getRequestChainByName(PROXYRECEPTORCHAIN) != null) return;

        RequestFilterChain casChain =
                new ConstantFilterChain(
                        GeoServerCasConstants.CAS_PROXY_RECEPTOR_PATTERN,
                        GeoServerCasConstants.CAS_PROXY_RECEPTOR_PATTERN + "/");
        casChain.setFilterNames(pgtCallback.getName());
        casChain.setName(PROXYRECEPTORCHAIN);
        filterChain.getRequestChains().add(0, casChain);
    }
}
