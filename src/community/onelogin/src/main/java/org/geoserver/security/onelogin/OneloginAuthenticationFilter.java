/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.onelogin;

import java.io.IOException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.xml.parse.ParserPool;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.key.EmptyKeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * OneLogin Authentication filter that configures SP metadata discovery filter and delegates to {@link #SAMLEntryPoint} the SAML authentication
 * process
 * 
 * @author Xandros
 */

public class OneloginAuthenticationFilter extends GeoServerPreAuthenticatedCompositeUserNameFilter
        implements LogoutHandler {

    static final Logger LOGGER = Logging.getLogger(OneloginAuthenticationFilter.class);

    protected SAMLEntryPoint samlEntryPoint;

    private static ApplicationContext context;

    public OneloginAuthenticationFilter(ApplicationContext ctx) {
        context = ctx;
        this.samlEntryPoint = context.getBean(SAMLEntryPoint.class);
    }

    /**
     * Configures {@link MetadataGenerator} using EntityId and ID MetadataURL from filter configuration<br/>
     * Configures SAMLUserDetailsService to use {@link GeoServerRole} selected provider
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        OneloginAuthenticationFilterConfig authConfig = (OneloginAuthenticationFilterConfig) config;

        try {
            if (getNestedFilters().isEmpty()) {
                /*
                 * Create metadata filter
                 */
                MetadataGenerator generator = new MetadataGenerator();
                generator.setEntityId(authConfig.getEntityId());
                generator.setIncludeDiscoveryExtension(false);
                generator.setKeyManager(new EmptyKeyManager());
                generator.setRequestSigned(false);
                generator.setWantAssertionSigned(authConfig.getWantAssertionSigned());
                ExtendedMetadata em = new ExtendedMetadata();
                em.setRequireLogoutRequestSigned(false);
                generator.setExtendedMetadata(em);
                MetadataGeneratorFilter metadataGeneratorFilter = new MetadataGeneratorFilter(
                        generator);

                /*
                 * Create metadata provider
                 */

                ParserPool parserPool = context.getBean(ParserPool.class);

                HttpClientParams clientParams = new HttpClientParams();
                clientParams.setSoTimeout(5000);
                HttpClient httpClient = new HttpClient(clientParams);
                httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
                HTTPMetadataProvider pro = new HTTPMetadataProvider(new Timer(true), httpClient,
                        authConfig.getMetadataURL());
                pro.setParserPool(parserPool);

                /*
                 * Use this to pass metadata string String xml =
                 * "<?xml version=\"1.0\"?> <EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"https://app.onelogin.com/saml/metadata/575443\"> <IDPSSODescriptor xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"> <KeyDescriptor use=\"signing\"> <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"> <ds:X509Data> <ds:X509Certificate>MIIEHTCCAwWgAwIBAgIUIYlArFxWB1C7BX6q/mFytCIYeI8wDQYJKoZIhvcNAQEF BQAwWjELMAkGA1UEBhMCVVMxEzARBgNVBAoMCmNvZGVzdHVkaW8xFTATBgNVBAsM DE9uZUxvZ2luIElkUDEfMB0GA1UEAwwWT25lTG9naW4gQWNjb3VudCA4ODk4ODAe Fw0xNjA4MDMxMDUyNTNaFw0yMTA4MDQxMDUyNTNaMFoxCzAJBgNVBAYTAlVTMRMw EQYDVQQKDApjb2Rlc3R1ZGlvMRUwEwYDVQQLDAxPbmVMb2dpbiBJZFAxHzAdBgNV BAMMFk9uZUxvZ2luIEFjY291bnQgODg5ODgwggEiMA0GCSqGSIb3DQEBAQUAA4IB DwAwggEKAoIBAQDWKHu+QvLa9BvqL6OoKKMVMBY0deHU6xqPAoatxiGlNIazoK8T PY5srjRX18W4aOb9In3zEulipGfNaQ0Avj/Jhi1UbS9lJMVNNODZ0dzfkJhIlpkG z7+totPf5P1BdUBTNk7OpguDLsb5DXKm5ZhGSDzMgGGNDNdOEZpJJ1zVjskUkmR2 frea+ZcpMkNa9CB6Jf6d6oE2BNhW94d1F4N5KB0NbmFonzLa3N5vRHstM88DbFSO UM7N9SRf+8Jnviae7fcG12woE+25G4qB1wJ1rfIu9wL7JhiXLPA7FB4L4bRglXZs Vin9sY93QyUmrv8kxNrtwLXnQopu0myfG3CXAgMBAAGjgdowgdcwDAYDVR0TAQH/ BAIwADAdBgNVHQ4EFgQUvz2fdMwH1G1W8bbcAWN238szW34wgZcGA1UdIwSBjzCB jIAUvz2fdMwH1G1W8bbcAWN238szW36hXqRcMFoxCzAJBgNVBAYTAlVTMRMwEQYD VQQKDApjb2Rlc3R1ZGlvMRUwEwYDVQQLDAxPbmVMb2dpbiBJZFAxHzAdBgNVBAMM Fk9uZUxvZ2luIEFjY291bnQgODg5ODiCFCGJQKxcVgdQuwV+qv5hcrQiGHiPMA4G A1UdDwEB/wQEAwIHgDANBgkqhkiG9w0BAQUFAAOCAQEANkaO/xJag1n93l+6/sbl cG/1Oi3/hI19+lp0PU26kFtkrpzfjE4QugBgnGXkeJ/MU6abk650uh+7yLqkY15G m9Lsk0XiH1k7vWWnQl12Yj0uwCY47baBLw0lMCl5vNaJcULicAM715W3d2oHptnh ftePShyHHD69Z4e+UduuClbXjSxPNB9zTOsLYRVbrX+fIFm1AK8bWcmrH/jAuj7p WP7hRJdT3jG5N7LNb4th3Ojj47NksjaPo2nOuydZvyoL2CJ/E2qJeW78V6oqXCB3 D/XVIWWdUmYMNNmXksT9MJCtZWvnV3OmdYbyZjOK2bJK7fbVgm/gwpeBSY+pquMG AA==</ds:X509Certificate> </ds:X509Data> </ds:KeyInfo> </KeyDescriptor> <SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://codestudio-dev.onelogin.com/trust/saml2/http-redirect/slo/575443\"/> <NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</NameIDFormat> <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://codestudio-dev.onelogin.com/trust/saml2/http-redirect/sso/575443\"/> <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://codestudio-dev.onelogin.com/trust/saml2/http-post/sso/575443\"/> <SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\"https://codestudio-dev.onelogin.com/trust/saml2/soap/sso/575443\"/> <SingleLogoutService Location=\"https://codestudio-dev.onelogin.com/trust/saml2/http-redirect/slo/575443\" Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"/> </IDPSSODescriptor> <ContactPerson contactType=\"technical\"> <SurName>Support</SurName> <EmailAddress>support@onelogin.com</EmailAddress> </ContactPerson> </EntityDescriptor>"
                 * ; DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); dbf.setNamespaceAware(true); DocumentBuilder db =
                 * dbf.newDocumentBuilder(); Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
                 * 
                 * DOMMetadataProvider pro = new DOMMetadataProvider(doc.getDocumentElement()); pro.setParserPool(parserPool); pro.initialize();
                 */

                ExtendedMetadataDelegate emd = new ExtendedMetadataDelegate(pro, em);

                /*
                 * Set metadata provider and add filter to chain
                 */
                MetadataManager metadata = context.getBean(MetadataManager.class);
                metadata.addMetadataProvider(emd);
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

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return this.samlEntryPoint;
    }

    /**
     * Injects current request into {@link SAMLUserDetailsServiceImpl} and sets {@link SAMLEntryPoint} as filter entry point
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER,
                this.samlEntryPoint);
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
    public void logout(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        request.setAttribute(GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR,
                SAMLLogoutFilter.FILTER_URL);

    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

}
