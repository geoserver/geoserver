/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.onelogin.test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.LogoutFilterChain;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.onelogin.OneloginAuthenticationFilter;
import org.geoserver.security.onelogin.OneloginAuthenticationFilterConfig;
import org.geotools.data.Base64;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.common.SAMLObject;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class OneloginAuthenticationTest extends AbstractAuthenticationProviderTest {

    private static final String METADATA_URL = "/saml/metadata";

    private static final String REDIRECT_URL = "/trust/saml2/http-redirect/sso";

    private static final Integer IDP_PORT = 8443;

    private static final String IDP_LOGIN_URL = "http://localhost:" + IDP_PORT + "/login";

    private static OneloginAuthenticationFilterConfig config;

    private static WireMockServer idpSamlService;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        idpSamlService.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(METADATA_URL))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                                        .withBodyFile("metadata.xml")));

        idpSamlService.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(REDIRECT_URL))
                        .willReturn(
                                aResponse().withStatus(302).withHeader("Location", IDP_LOGIN_URL)));
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        SSLUtilities.registerKeyStore("keystore");
        idpSamlService = new WireMockServer(wireMockConfig().httpsPort(IDP_PORT));
        idpSamlService.start();
    }

    @Before
    public void before() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        idpSamlService.shutdown();
    }

    @Test
    public void metadataDiscovery() throws Exception {
        confgiureFilter(PreAuthenticatedUserNameRoleSource.UserGroupService);
        verify(getRequestedFor(urlEqualTo(METADATA_URL)).withUrl(METADATA_URL));
    }

    @Test
    public void notAuthenticatedRedirect() throws Exception {
        confgiureFilter(PreAuthenticatedUserNameRoleSource.UserGroupService);
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);

        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String redirectURL = response.getHeader("Location");

        assertThat(redirectURL, CoreMatchers.containsString(REDIRECT_URL));

        URIBuilder uriBuilder = new URIBuilder(redirectURL);
        List<NameValuePair> urlParameters = uriBuilder.getQueryParams();
        String samlRequest = null;
        for (NameValuePair par : urlParameters) {
            if (par.getName().equals("SAMLRequest")) {
                samlRequest = par.getValue();
                break;
            }
        }
        assertNotNull(samlRequest);
        StringSamlDecoder decoder = new StringSamlDecoder();
        SAMLObject samlRequestObject = decoder.decode(samlRequest);
        assertNotNull(samlRequestObject);
    }

    @Test
    public void autorizationWithGroup() throws Exception {
        confgiureFilter(PreAuthenticatedUserNameRoleSource.UserGroupService);
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);

        /*
         * Build POST request form IDP to GeoServer
         */
        String encodedResponseMessage = buildSAMLRespons("abc@xyz.com");
        request = createRequest(SAMLProcessingFilter.FILTER_URL);
        request.setMethod("POST");
        request.addParameter("SAMLResponse", encodedResponseMessage);
        chain = new MockFilterChain();
        response = new MockHttpServletResponse();
        getProxy().doFilter(request, response, chain);

        /*
         * Check user
         */
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(false)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals("abc@xyz.com", auth.getPrincipal());
    }

    @Test
    public void authenticationWithRoles() throws Exception {
        confgiureFilter(PreAuthenticatedUserNameRoleSource.RoleService);
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);

        /*
         * Build POST request form IDP to GeoServer
         */
        String encodedResponseMessage = buildSAMLRespons(testUserName);
        request = createRequest(SAMLProcessingFilter.FILTER_URL);
        request.setMethod("POST");
        request.addParameter("SAMLResponse", encodedResponseMessage);
        chain = new MockFilterChain();
        response = new MockHttpServletResponse();
        getProxy().doFilter(request, response, chain);

        /*
         * Check user
         */
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(false)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        boolean hasRootRole = false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().equals(rootRole)) {
                hasRootRole = true;
                break;
            }
        }
        assertTrue(hasRootRole);
        assertEquals(testUserName, auth.getPrincipal());
    }

    @Test
    public void logoutTest() throws Exception {
        LogoutFilterChain logoutchain =
                (LogoutFilterChain)
                        getSecurityManager()
                                .getSecurityConfig()
                                .getFilterChain()
                                .getRequestChainByName("webLogout");

        confgiureFilter(PreAuthenticatedUserNameRoleSource.RoleService);
        MockHttpServletRequest request = createRequest("/foo/bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);

        /*
         * Build POST request form IDP to GeoServer
         */
        String encodedResponseMessage = buildSAMLRespons(testUserName);
        request = createRequest(SAMLProcessingFilter.FILTER_URL);
        request.setMethod("POST");
        request.addParameter("SAMLResponse", encodedResponseMessage);
        chain = new MockFilterChain();
        response = new MockHttpServletResponse();
        getProxy().doFilter(request, response, chain);

        /*
         * Check user
         */
        SecurityContext ctx =
                (SecurityContext)
                        request.getSession(false)
                                .getAttribute(
                                        HttpSessionSecurityContextRepository
                                                .SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertEquals(testUserName, auth.getPrincipal());

        /*
         * Logout
         */
        SecurityContextHolder.setContext(ctx);
        request = createRequest(logoutchain.getPatterns().get(0));
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        GeoServerLogoutFilter logoutFilter =
                (GeoServerLogoutFilter)
                        getSecurityManager()
                                .loadFilter(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        logoutFilter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == MockHttpServletResponse.SC_MOVED_TEMPORARILY);
        String redirectURL = response.getHeader("Location");

        /*
         * Check if SAML logut URL will be called
         */
        assertThat(redirectURL, CoreMatchers.containsString(SAMLLogoutFilter.FILTER_URL));
    }

    private String buildSAMLRespons(String username) throws Exception {
        /*
         * Buld valid SAML response from template
         */
        DateTime now = new DateTime();
        String xml =
                IOUtils.toString(
                        this.getClass().getResourceAsStream("/__files/response.xml"), "UTF-8");
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        Document doc =
                domFactory
                        .newDocumentBuilder()
                        .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xpath.evaluate("//@IssueInstant", doc, XPathConstants.NODESET);
        for (int idx = 0; idx < nodes.getLength(); idx++) {
            Node value = nodes.item(idx);
            value.setNodeValue(now.toString("yyyy-MM-dd'T'HH:mm:ssZ"));
        }
        nodes = (NodeList) xpath.evaluate("//@NotOnOrAfter", doc, XPathConstants.NODESET);
        for (int idx = 0; idx < nodes.getLength(); idx++) {
            Node value = nodes.item(idx);
            value.setNodeValue(now.toString("yyyy-MM-dd'T'HH:mm:ssZ"));
        }
        nodes = (NodeList) xpath.evaluate("//@NotBefore", doc, XPathConstants.NODESET);
        for (int idx = 0; idx < nodes.getLength(); idx++) {
            Node value = nodes.item(idx);
            value.setNodeValue(now.toString("yyyy-MM-dd'T'HH:mm:ssZ"));
        }
        nodes = (NodeList) xpath.evaluate("//@AuthnInstant", doc, XPathConstants.NODESET);
        for (int idx = 0; idx < nodes.getLength(); idx++) {
            Node value = nodes.item(idx);
            value.setNodeValue(now.toString("yyyy-MM-dd'T'HH:mm:ssZ"));
        }
        nodes = (NodeList) xpath.evaluate("//@SessionNotOnOrAfter", doc, XPathConstants.NODESET);
        for (int idx = 0; idx < nodes.getLength(); idx++) {
            Node value = nodes.item(idx);
            value.setNodeValue(now.plusDays(1).toString("yyyy-MM-dd'T'HH:mm:ssZ"));
        }
        Node node =
                (Node)
                        xpath.evaluate(
                                "//*[local-name() = 'NameID']/text()", doc, XPathConstants.NODE);
        node.setNodeValue(username);
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        xformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString();
        String encodedResponseMessage =
                Base64.encodeBytes(output.getBytes("UTF-8"), Base64.DONT_BREAK_LINES).trim();
        return encodedResponseMessage;
    }

    private void confgiureFilter(PreAuthenticatedUserNameRoleSource serviceType) {
        try {
            String oneloginFilterName = "testOneloginFilter";
            if (config == null) {
                config = new OneloginAuthenticationFilterConfig();
                config.setWantAssertionSigned(false);
                config.setClassName(OneloginAuthenticationFilter.class.getName());
                config.setName(oneloginFilterName);
                config.setEntityId("geoserver");
                config.setMetadataURL(
                        "https://localhost:" + idpSamlService.httpsPort() + METADATA_URL);
            }
            config.setUserGroupServiceName(
                    serviceType == PreAuthenticatedUserNameRoleSource.RoleService ? "rs1" : "ug1");
            config.setRoleSource(serviceType);
            getSecurityManager().saveFilter(config);
            prepareFilterChain(pattern, oneloginFilterName);
            modifyChain(pattern, false, true, null);
        } catch (Exception e) {
        }
    }
}
