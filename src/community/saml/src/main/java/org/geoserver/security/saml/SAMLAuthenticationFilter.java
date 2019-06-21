/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.opensaml.saml2.metadata.provider.DOMMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.xml.parse.ParserPool;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.key.EmptyKeyManager;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.w3c.dom.Document;

/**
 * SAML SSO Authentication filter that configures SP metadata discovery filter and delegates to
 * {@link #SAMLEntryPoint} the SAML authentication process
 *
 * @author Xandros
 */
public class SAMLAuthenticationFilter extends GeoServerPreAuthenticatedCompositeUserNameFilter
        implements LogoutHandler {

    static final Logger LOGGER = Logging.getLogger(SAMLAuthenticationFilter.class);

    protected SAMLEntryPoint samlEntryPoint;

    private static ApplicationContext context;

    public SAMLAuthenticationFilter(ApplicationContext ctx) {
        context = ctx;
        this.samlEntryPoint = context.getBean(SAMLEntryPoint.class);
    }

    private SAMLKeyManager getKeyManager() {
        return (SAMLKeyManager) GeoServerExtensions.bean("keyManager");
    }

    private GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    /**
     * Configures {@link MetadataGenerator} using EntityId and ID MetadataURL from filter
     * configuration<br>
     * Configures SAMLUserDetailsService to use {@link GeoServerRole} selected provider
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        SAMLAuthenticationFilterConfig authConfig = (SAMLAuthenticationFilterConfig) config;

        try {
            if (getNestedFilters().isEmpty()) {
                /*
                 * Create metadata filter
                 */

                KeyManager keyManager = buildKeyManager(authConfig);
                Boolean signing = authConfig.getSigning();
                MetadataGenerator generator = new MetadataGenerator();
                generator.setEntityId(authConfig.getEntityId());
                generator.setIncludeDiscoveryExtension(false);
                generator.setKeyManager(keyManager);
                generator.setRequestSigned(signing);
                generator.setWantAssertionSigned(authConfig.getWantAssertionSigned() || signing);
                ExtendedMetadata em = new ExtendedMetadata();
                em.setRequireLogoutRequestSigned(signing);
                generator.setExtendedMetadata(em);
                MetadataGeneratorFilter metadataGeneratorFilter =
                        new MetadataGeneratorFilter(generator);

                /*
                 * Create metadata provider
                 */

                ParserPool parserPool = context.getBean(ParserPool.class);
                MetadataProvider pro = null;
                if (hasLocalMetadata(authConfig)) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc =
                            db.parse(
                                    new ByteArrayInputStream(
                                            authConfig.getMetadata().getBytes("UTF-8")));

                    DOMMetadataProvider domProvider =
                            new DOMMetadataProvider(doc.getDocumentElement());
                    domProvider.setParserPool(parserPool);
                    domProvider.initialize();
                    pro = domProvider;
                } else {
                    HttpClientParams clientParams = new HttpClientParams();
                    clientParams.setSoTimeout(5000);
                    HttpClient httpClient = new HttpClient(clientParams);
                    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
                    HTTPMetadataProvider httpProvider =
                            new LoggingHTTPMetadataProvider(
                                    new Timer(true), httpClient, authConfig.getMetadataURL());
                    httpProvider.setParserPool(parserPool);
                    pro = httpProvider;
                }

                ExtendedMetadataDelegate emd = new ExtendedMetadataDelegate(pro, em);

                /*
                 * Set metadata provider and add filter to chain
                 */
                MetadataManager metadata = context.getBean(MetadataManager.class);
                metadata.setHostedSPName(authConfig.getEntityId());
                List<MetadataProvider> providers = new ArrayList<MetadataProvider>();
                providers.add(emd);
                metadata.setProviders(providers);
                metadata.setRefreshRequired(hasLocalMetadata(authConfig) ? false : true);
                metadata.refreshMetadata();
                metadataGeneratorFilter.setManager(metadata);
                getNestedFilters().add(metadataGeneratorFilter);
            } else {
                LOGGER.log(Level.FINE, "Metadata filter already added");
            }

            /*
             * Inject UserGroup and Role configuration into SAML user details service
             */
            SAMLUserDetailsServiceImpl usd = context.getBean(SAMLUserDetailsServiceImpl.class);
            usd.setConverter(this.getConverter());
            usd.setRoleServiceName(this.getRoleServiceName());
            usd.setRolesHeaderAttribute(this.getRolesHeaderAttribute());
            usd.setRoleSource(this.getRoleSource());
            usd.setSecurityManager(this.securityManager);
            usd.setUserGroupServiceName(this.getUserGroupServiceName());

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private KeyManager buildKeyManager(SAMLAuthenticationFilterConfig authConfig) {
        if (authConfig.getSigning()) {
            if (authConfig.getKeyStoreId() != null
                    && authConfig.getKeyStoreIdPassword() != null
                    && authConfig.getKeyStorePath() != null
                    && authConfig.getKeyStorePassword() != null) {
                Map<String, String> passwords = new HashMap<String, String>();
                passwords.put(authConfig.getKeyStoreId(), authConfig.getKeyStoreIdPassword());
                File samlKeyStore = new File(authConfig.getKeyStorePath());
                try {
                    if (!samlKeyStore.isAbsolute()) {
                        samlKeyStore = getDataDirectory().findFile(authConfig.getKeyStorePath());
                    }
                    if (samlKeyStore != null) {
                        KeyManager keyManager =
                                new JKSKeyManager(
                                        new FileSystemResource(samlKeyStore),
                                        authConfig.getKeyStorePassword(),
                                        passwords,
                                        authConfig.getKeyStoreId());
                        getKeyManager().setDelegate(keyManager);
                        return keyManager;
                    } else {
                        LOGGER.severe(
                                "Cannot find keystore file in " + authConfig.getKeyStorePath());
                    }
                } catch (Exception e) {
                    LOGGER.severe("Cannot read keystore file from " + authConfig.getKeyStorePath());
                }
            } else {
                LOGGER.severe("KeyStore parameters are not completely configured");
            }
        }
        return new EmptyKeyManager();
    }

    private boolean hasLocalMetadata(SAMLAuthenticationFilterConfig authConfig) {
        return authConfig.getMetadata() != null && !"".equals(authConfig.getMetadata());
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return this.samlEntryPoint;
    }

    /**
     * Injects current request into {@link SAMLUserDetailsServiceImpl} and sets {@link
     * SAMLEntryPoint} as filter entry point
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) req;
            // force session creation so that HttpSessionRequestCache can correcly save the request
            // on session
            httpReq.getSession(true);
        }
        req.setAttribute(
                GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, this.samlEntryPoint);
        /*
         * Inject current request into SAML user details service
         */
        SAMLUserDetailsServiceImpl usd = context.getBean(SAMLUserDetailsServiceImpl.class);
        usd.setRequest((HttpServletRequest) req);
        super.doFilter(req, res, chain);
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        request.setAttribute(
                GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR, SAMLLogoutFilter.FILTER_URL);
    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        // TODO Auto-generated method stub
        return null;
    }
}
