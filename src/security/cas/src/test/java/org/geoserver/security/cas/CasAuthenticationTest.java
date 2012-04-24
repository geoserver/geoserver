package org.geoserver.security.cas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.auth.TestingAuthenticationCache;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.RoleSource;
import org.geoserver.security.filter.GeoServerExceptionTranslationFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.impl.AbstractSecurityServiceTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockHttpSession;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

/**
 * A running cas server is needed
 * 
 * To activate the test a file ".geoserver/cas.properties" in the
 * home directory is needed.
 * 
 * Content
 * # Fixture for cas
 * # 
 * casserverurlprefix=https://ux-server02.mc-home.local:8443/cas
 * service=https://ux-desktop03.mc-home.local:4711/geoserver/j_spring_cas_security_check
 * proxycallbackurlprefix=https://ux-desktop03.mc-home.local:4711/geoserver/
 * 
 *  Client ssl configuration:
 * Create a keystore keystore.jks in  home_dir/.geoserver with key store key password "changeit"
 * 
 * Create self signing certificate
 * keytool -genkey -alias mc-home.local -keystore rsa-keystore -keyalg RSA -sigalg MD5withRSA
 *         
 * Only the cn must be set to the full server name "ux-desktop03.mc-home.local"
 * 
 * Export the certificate
 * keytool -export -alias mc-home.local -keystore keystore.jks -file ux-desktop03.crt
 * 
 * For the cas server 
 * copy ux-desktop03.crt to the server
 * 
 * Find cacerts file for the virtual machine running cas
 * 
 * Import the certificate
 * 
 * keytool -import -trustcacerts -alias mc-home.local -file ux-desktop03.crt \
 *       -keystore /usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/security/cacerts
 * 
 * The keystore password for cacerts is "changeit"
 * 
 * Next, export the certificate of tomcat and import it into the cacerts
 * of your java sdk
 * 
 * @author christian
 *
 */
public class CasAuthenticationTest extends AbstractAuthenticationProviderTest {

    public final String casFilterName="testCasFilter";
    public final String casProxyFilterName1="testCasProxyFilter1";
    public final String CAS_EXCEPTION_TRANSLATION_FILTER="casExceptionTranslationFilter";
    
    URL casServerURLPrefix;
    URL serviceUrl;
    URL loginUrl;
    URL proxyCallbackUrlPrefix;
    HttpsServer httpsServer;
    

    public class HttpsProxyCallBackHandler  implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            URI uri = ex.getRequestURI();
            ex.getRequestBody().close();
            LOGGER.info("Cas proxy callback: "+uri.toString());
            String query = uri.getQuery();
                                    
            MockHttpServletRequest request= createRequest(GeoServerCasConstants.CAS_PROXY_RECEPTOR_PATTERN);
            MockHttpServletResponse response= new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            // CAS sends the callback twice, the first time without parameters
            if (query!=null) {
                request.setQueryString(query);
                String[] kvps = query.split("&");
                for (String kvp : kvps) {
                    String[] tmp = kvp.split("=");
                    request.setupAddParameter(tmp[0],tmp[1]);                
                }
            }
            
            try {
                getProxy().doFilter(request, response, chain);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            
            
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();            
        }        
    }
    
    public class SingleSignOutHandler  implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {

            BufferedReader in = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
            String line = "";
            StringBuffer buff=new StringBuffer();
            while((line=in.readLine())!=null) {
                    buff.append(line);
            }
            in.close();
            
            MockHttpServletRequest  request= createRequest("/j_spring_cas_security_check");
            request.setMethod("POST");            
            MockHttpServletResponse response= new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            String paramValue = URLDecoder.decode(buff.toString(),"utf-8");
            request.setupAddParameter("logoutRequest",paramValue.substring(paramValue.indexOf("=")+1));
            try {
                getProxy().doFilter(request, response, chain);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }                
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());                                                            
            
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();            
        }        
    }


    
    @Override
    protected TestData buildTestData() throws Exception {
        return new LiveCasData(AbstractSecurityServiceTest.unpackTestDataDir());
    }
    
    
    
    @Override
    protected void setUpInternal() throws Exception {        
        if (getTestData().isTestDataAvailable()) {
            super.setUpInternal();
            LiveCasData td = (LiveCasData) getTestData();
            casServerURLPrefix=td.getServerURLPrefix();
            loginUrl=td.getLoginURL();            
            serviceUrl=td.getServiceURL();
            proxyCallbackUrlPrefix=td.getProxyCallbackURLPrefix();
            httpsServer=createAndStartHttpsServer();
        }                    
    }
    
    @Override
    protected void tearDownInternal() throws Exception {
        if (httpsServer!=null) 
            httpsServer.stop(0);
        super.tearDownInternal();
        
    }
    
    protected HttpsServer createAndStartHttpsServer() throws Exception{
        HttpsServer httpsServer = ((LiveCasData) getTestData()).createSSLServer();
        URL callbackUrl = new URL(GeoServerCasConstants.createProxyCallBackURl(proxyCallbackUrlPrefix.toString()));
        httpsServer.createContext(callbackUrl.getPath(), new HttpsProxyCallBackHandler());
        httpsServer.createContext(serviceUrl.getPath(), new SingleSignOutHandler());
        httpsServer.start();
        
        return httpsServer;
    }

    
    protected String getResponseHeaderValue(HttpURLConnection conn,String name) {
        for (int i=0; ; i++) {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);

            if (headerName == null && headerValue == null) {
                // No more headers
                break;
            }
            if (name.equalsIgnoreCase(headerName)) {
                return headerValue;
            }
        }
        return null;
    }
    
    public void testCASLogin() throws Exception {
        
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setService(serviceUrl.toString());
        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setName(casFilterName);        
        config.setUserGroupServiceName("ug1");
        getSecurityManager().saveFilter(config);
        
        ExceptionTranslationFilterConfig exConfig = new ExceptionTranslationFilterConfig();
        exConfig.setClassName(GeoServerExceptionTranslationFilter.class.getName());
        exConfig.setName(CAS_EXCEPTION_TRANSLATION_FILTER);
        exConfig.setAccessDeniedErrorPage("/denied.jsp");
        exConfig.setAuthenticationFilterName(casFilterName);
        getSecurityManager().saveFilter(exConfig);

                
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,
            CAS_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);

        
        prepareFilterChain("/j_spring_cas_security_check",
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,    
                casFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);
        
        // Test entry point
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        String redirectURL= response.getHeader("Location");
        assertTrue(redirectURL.contains("login"));
        assertTrue(redirectURL.endsWith("j_spring_cas_security_check"));

        // test success
        String username = "castest";
        String password = username;
        CasFormAuthenticationHelper helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();
        String ticket = helper.getServiceTicket(serviceUrl);
        
        request= createRequest("/j_spring_cas_security_check");
        request.setQueryString("ticket="+ticket);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();       
        request.setupAddParameter("ticket",ticket);
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        String redirectUrl = response.getHeader("Location");
        assertNotNull(redirectUrl);

                
        SecurityContext ctx = (SecurityContext)request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        assertNotNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        helper.ssoLogout();



        // check unknown user
        username = "unknown";
        password =  username;
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(serviceUrl);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        request.setQueryString("ticket="+ticket);
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNotNull(ctx);
        auth =  ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(ctx.getAuthentication());
        assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());
        assertEquals(1,auth.getAuthorities().size());
        assertNotNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        helper.ssoLogout();

        
        // test root user
        username = GeoServerUser.ROOT_USERNAME;
        password = username;
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();

        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(serviceUrl);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        request.setQueryString("ticket="+ticket);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        assertTrue(response.getHeader("Location").endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        //checkForAuthenticatedRole(auth);
        assertEquals(GeoServerUser.ROOT_USERNAME, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().size()==1);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));
        assertNotNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        helper.ssoLogout();




        // check disabled user
        username="castest";
        password=username;
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();
        updateUser("ug1", username, false);
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(serviceUrl);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        request.setQueryString("ticket="+ticket);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        redirectURL= response.getHeader("Location");
        assertTrue(redirectURL.contains("login"));
        assertTrue(redirectURL.endsWith("j_spring_cas_security_check"));        
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        updateUser("ug1", username, true);
        helper.ssoLogout();
        
        insertAnonymousFilter(CAS_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
        

        // test invalid ticket 

        username = "castest";
        password = username;
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();
        
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(serviceUrl);
        ticket+="A";
        request.setupAddParameter("ticket",ticket);
        request.setQueryString("ticket="+ticket);
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        redirectURL= response.getHeader("Location");
        assertTrue(redirectURL.contains("login"));
        assertTrue(redirectURL.endsWith("j_spring_cas_security_check"));        
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        helper.ssoLogout();
        
        // test success with proxy granting ticket
        config.setProxyCallbackUrlPrefix(proxyCallbackUrlPrefix.toString());
        getSecurityManager().saveFilter(config);
        
        username = "castest";
        password = username;
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();
        
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = helper.getServiceTicket(serviceUrl);
        request.setupAddParameter("ticket",ticket);
        request.setQueryString("ticket="+ticket);        
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        redirectUrl = response.getHeader("Location");
        assertNotNull(redirectUrl);

                
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNotNull(ctx);
        CasAuthenticationToken casAuth = (CasAuthenticationToken)ctx.getAuthentication();
        assertNotNull(casAuth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(casAuth);
        assertEquals(username, ((UserDetails) casAuth.getPrincipal()).getUsername());
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        String proxyTicket=casAuth.getAssertion().getPrincipal().getProxyTicketFor("http://localhost/blabla");
        assertNotNull(proxyTicket);
        assertNotNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        helper.ssoLogout();

        


    }
    public void testLogout() throws Exception {
        
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setService(serviceUrl.toString());
        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setName(casFilterName);        
        config.setUserGroupServiceName("ug1");
        config.setUrlInCasLogoutPage("http://localhost/afterlogout");
        getSecurityManager().saveFilter(config);
        
                        
        prepareFilterChain("/j_spring_cas_security_check",
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,    
                casFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);
        
        // logout triggered by geoserver
        MockHttpServletRequest request= createRequest("/j_spring_cas_security_check");
        request.setQueryString(GeoServerCasAuthenticationFilter.LOGOUT_PARAM+"=true");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();       
        request.setupAddParameter(GeoServerCasAuthenticationFilter.LOGOUT_PARAM,"true");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        String redirectUrl = response.getHeader("Location");
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("logout"));
        assertTrue(redirectUrl.contains("afterlogout"));

                
        // login
        String username = "castest";
        String password = username;
        CasFormAuthenticationHelper helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,password);
        helper.ssoLogin();
        String ticket = helper.getServiceTicket(serviceUrl);
        
        request= createRequest("/j_spring_cas_security_check");
        request.setQueryString("ticket="+ticket);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();       
        request.setupAddParameter("ticket",ticket);
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        redirectUrl = response.getHeader("Location");
        assertNotNull(redirectUrl);
               
        SecurityContext ctx = (SecurityContext)request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertNotNull(session);
        assertTrue(session.isValid());

        // logout triggered by cas server
        request= createRequest("/j_spring_cas_security_check");
        request.setMethod("POST");
        request.setSession(session);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();       
        request.setupAddParameter("logoutRequest",getBodyForLogoutRequest(ticket));
        getProxy().doFilter(request, response, chain);
//        helper.ssoLogout();
        assertNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(ticket));
        assertFalse(session.isValid());
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());

        
        

        
    }
    

    protected Assertion authenticateWithPGT(CasFormAuthenticationHelper helper) throws Exception {        
        helper.ssoLogin();


        String ticket = helper.getServiceTicket(serviceUrl);
  
        Cas20ProxyTicketValidator validator = new Cas20ProxyTicketValidator(casServerURLPrefix.toString());
        validator.setAcceptAnyProxy(true);
        validator.setProxyCallbackUrl(GeoServerCasConstants.createProxyCallBackURl(proxyCallbackUrlPrefix.toExternalForm()));
        validator.setProxyGrantingTicketStorage(GeoServerExtensions.bean(ProxyGrantingTicketStorage.class));

        Assertion result = validator.validate(ticket,serviceUrl.toExternalForm());

        assertNotNull(result);
        return result;
         
    }
    
    
    public void testProxyAuth () throws Exception{
        
        
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setService(serviceUrl.toString());
        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setName(casFilterName);        
        config.setUserGroupServiceName("ug1");
        config.setProxyCallbackUrlPrefix(proxyCallbackUrlPrefix.toString());
        getSecurityManager().saveFilter(config);
        
        ExceptionTranslationFilterConfig exConfig = new ExceptionTranslationFilterConfig();
        exConfig.setClassName(GeoServerExceptionTranslationFilter.class.getName());
        exConfig.setName(CAS_EXCEPTION_TRANSLATION_FILTER);
        exConfig.setAccessDeniedErrorPage("/denied.jsp");
        exConfig.setAuthenticationFilterName(casFilterName);
        getSecurityManager().saveFilter(exConfig);

        String targetService = "https://ux-desktop03.mc-home.local/geoserver/wms";
        CasProxiedAuthenticationFilterConfig pconfig1 = new CasProxiedAuthenticationFilterConfig();
        pconfig1.setClassName(GeoServerCasProxiedAuthenticationFilter.class.getName());
        pconfig1.setName(casProxyFilterName1);
        pconfig1.setCasServerUrlPrefix(casServerURLPrefix.toString());
        pconfig1.setService(targetService);
        pconfig1.setRoleSource(RoleSource.UserGroupService);
        pconfig1.setUserGroupServiceName("ug1");
        getSecurityManager().saveFilter(pconfig1);

                
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,
            casProxyFilterName1,
            GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);

//        prepareFilterChain(GeoServerCasConstants.CAS_PROXY_RECEPTOR_PATTERN,
//                casFilterName);
        
        prepareFilterChain("/j_spring_cas_security_check",
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,    
                casFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);
        
        // test entry point
        MockHttpServletRequest request= createRequest(pattern);
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();        
        request.setupAddParameter("ticket","blabla");
        request.setQueryString("ticket=blabla");
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());


        // test successful 
        getCache().removeAll();
        String username = "castest";
        CasFormAuthenticationHelper helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,username);
        Assertion ass =authenticateWithPGT(helper);           
        String proxyTicket = ass.getPrincipal().getProxyTicketFor(
                targetService);
        assertNotNull(proxyTicket);
        for (int i = 0; i < 2 ; i++) {
            request= createRequest(pattern);
            response= new MockHttpServletResponse();
            chain = new MockFilterChain();        
            request.setupAddParameter("ticket",proxyTicket);
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            TestingAuthenticationCache cache = getCache();
            CasAuthenticationToken casAuth = (CasAuthenticationToken) 
                    cache.get(casProxyFilterName1, proxyTicket);
            assertNotNull(casAuth);
            checkForAuthenticatedRole(casAuth);
            assertEquals(username, ((UserDetails) casAuth.getPrincipal()).getUsername());
            assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));            
        }
        assertNull(GeoServerCasAuthenticationFilter.getHandler().getSessionMappingStorage().removeSessionByMappingId(proxyTicket));
        helper.ssoLogout();
        
     
        // check unknown user

        username = "unknown";
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,username);
        ass =authenticateWithPGT(helper);           
        proxyTicket = ass.getPrincipal().getProxyTicketFor(
                targetService);
        assertNotNull(proxyTicket);
        for (int i = 0; i < 2 ; i++) {
            request= createRequest(pattern);
            response= new MockHttpServletResponse();
            chain = new MockFilterChain();        
            request.setupAddParameter("ticket",proxyTicket);
            request.setQueryString("ticket="+proxyTicket);
            getProxy().doFilter(request, response, chain);
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            TestingAuthenticationCache cache = getCache();
            CasAuthenticationToken casAuth = (CasAuthenticationToken) 
                    cache.get(casProxyFilterName1, proxyTicket);
            assertNotNull(casAuth);
            checkForAuthenticatedRole(casAuth);
            assertEquals(username, ((UserDetails) casAuth.getPrincipal()).getUsername());
            assertEquals(1,casAuth.getAuthorities().size());
        }
        helper.ssoLogout();

        // check for disabled user
        getCache().removeAll();
        updateUser("ug1", "castest", false);
        
        username="castest";
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,username);
        ass =authenticateWithPGT(helper);           
        proxyTicket = ass.getPrincipal().getProxyTicketFor(
                targetService);
        assertNotNull(proxyTicket);
        
        request= createRequest(pattern);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
        request.setupAddParameter("ticket",proxyTicket);
        request.setQueryString("ticket="+proxyTicket);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        TestingAuthenticationCache cache = getCache();
        CasAuthenticationToken casAuth = (CasAuthenticationToken) 
                cache.get(casProxyFilterName1, proxyTicket);
        assertNull(casAuth);
            
        updateUser("ug1", "castest", true);
        helper.ssoLogout();
            
        // Test anonymous
        insertAnonymousFilter(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();


        // test proxy granting ticket in proxied auth filter
        
        pconfig1.setProxyCallbackUrlPrefix(proxyCallbackUrlPrefix.toString());
        getSecurityManager().saveFilter(pconfig1);

        getCache().removeAll();
        username = "castest";
        helper= new CasFormAuthenticationHelper(casServerURLPrefix,username,username);
        ass =authenticateWithPGT(helper);           
        proxyTicket = ass.getPrincipal().getProxyTicketFor(
                targetService);
        assertNotNull(proxyTicket);        
        request= createRequest(pattern);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
        request.setupAddParameter("ticket",proxyTicket);
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        cache = getCache();
        casAuth = (CasAuthenticationToken) 
                cache.get(casProxyFilterName1, proxyTicket);
        assertNotNull(casAuth);
        checkForAuthenticatedRole(casAuth);
        assertEquals(username, ((UserDetails) casAuth.getPrincipal()).getUsername());
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(casAuth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        proxyTicket = casAuth.getAssertion().getPrincipal().getProxyTicketFor(
              "http://localhost/blabla");
                
        assertNotNull(proxyTicket);
        helper.ssoLogout();
    }
    
    

    
    protected MockHttpServletRequest createRequest(String url) {
        MockHttpServletRequest request = super.createRequest(url);
        request.setProtocol(serviceUrl.getProtocol());
        request.setScheme(serviceUrl.getProtocol());
        request.setServerName(serviceUrl.getHost());
        request.setServerPort(serviceUrl.getPort());
        return request;        
    }
    
    public void testCasAuthenticationHelper() throws Exception {
        
        CasFormAuthenticationHelper helper= new CasFormAuthenticationHelper(casServerURLPrefix, "fail", "abc");
        assertFalse(helper.ssoLogin());
        
        helper= new CasFormAuthenticationHelper(casServerURLPrefix, "success", "success");
        assertTrue(helper.ssoLogin());
        assertNotNull(helper.getTicketGrantingCookie());
        LOGGER.info("TGC after login : "+helper.getTicketGrantingCookie());
        
        assertTrue(helper.ssoLogout());
        assertNotNull(helper.getTicketGrantingCookie());
        LOGGER.info("TGC after logout : "+helper.getTicketGrantingCookie());

        
        assertTrue(helper.ssoLogin());
        assertNotNull(helper.getTicketGrantingCookie());
        String ticket = helper.getServiceTicket(serviceUrl);
        assertNotNull(ticket);
        assertTrue(ticket.startsWith("ST-"));
        LOGGER.info("ST : "+ticket);
        helper.ssoLogout();
        

        
    }
    
    protected String getBodyForLogoutRequest(String ticket) {        
        String template=
                "<LogoutRequest ID=\"[RANDOM ID]\" Version=\"2.0\" IssueInstant=\"[CURRENT DATE/TIME]\">"+
                     "<NameID>@NOT_USED@</NameID>"+
                     "<SessionIndex>[SESSION IDENTIFIER]</SessionIndex>"+
                     "</LogoutRequest>"; 
        
        return template.replace("[SESSION IDENTIFIER]", ticket);
    }

}
