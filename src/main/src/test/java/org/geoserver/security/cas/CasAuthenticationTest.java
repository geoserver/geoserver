package org.geoserver.security.cas;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.data.test.TestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.cas.CasAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasAuthenticationFilter;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerExceptionTranslationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.impl.AbstractSecurityServiceTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
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
 * serverurl=http://ux-server02mc-home.local:8080/cas
 * service=http://ux-desktop03.mc-home.local:8080/geoserver/j_spring_cas_security_check
 * 
 *  Client ssl configuration:
 * Create a keystore cas.jks  home_dir/.geoserver with key store key password "cascas"
 * 
 * Create self signing certificate
 * keytool -genkey -alias mc-home.local -keystore rsa-keystore -keyalg RSA -sigalg MD5withRSA
 *         
 * The only the cn must be set to the full server name "ux-desktop03.mc-home.local"
 * 
 * Export the certificate
 * keytool -export -alias mc-home.local -keystore cas.jks -file ux-desktop03.crt
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
    public final String CAS_EXCEPTION_TRANSLATION_FILTER="casExceptionTranslationFilter";
    
    URL casServerURLPrefix;
    URL serviceUrl;
    URL loginUrl;
    URL proxyCallbackUrl;
    
    HttpsServer httpsServer;
    File keyStoreFile;
    
    
    @Override
    protected TestData buildTestData() throws Exception {
        return new LiveCasData(AbstractSecurityServiceTest.unpackTestDataDir());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        if (getTestData().isTestDataAvailable()) {
            super.setUpInternal();
            LiveCasData td = (LiveCasData) getTestData();
            casServerURLPrefix=td.getServerURL();
            loginUrl=td.getLoginURL();            
            serviceUrl=td.getServiceURL();
            proxyCallbackUrl=td.getProxyCallbackURL();
            File base = new File(System.getProperty("user.home"), ".geoserver");
            keyStoreFile = new File(base,"cas.jks");

        }                    
    }

    

    
    /**
     * @param siteUrl
     * @param data
     * @param cookie
     * @return null if not successful, redirect location on success
     * @throws Exception
     */
    protected String doLogin(URL siteUrl, Map<String, String> data, String cookie ) throws Exception {
        
        boolean follow = HttpURLConnection.getFollowRedirects();
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
        HttpURLConnection.setFollowRedirects(follow);
        
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Cookie", cookie);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        
        StringBuffer buff = new StringBuffer();
        for (Entry<String,String> entry : data.entrySet()) {
            if (buff.length()>0)
                buff.append("&");
            buff.append(entry.getKey()).append("=").
                append(URLEncoder.encode(entry.getValue(),"utf-8"));
        }
        //System.out.println(content);
        
        out.writeBytes(buff.toString());
        out.flush();
        out.close();
        if (HttpServletResponse.SC_MOVED_TEMPORARILY== conn.getResponseCode())
                return getResponseHeaderValue(conn, "Location");
        else
            return null; // not successful
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
    
    String extractValue(String searchString, String buff) {
        int index = buff.indexOf(searchString);
        index+=searchString.length();
        int index2 = buff.indexOf("\"", index);
        return  buff.substring(index,index2);
    }

    /**
     * @param username
     * @param password
     * @return null if login not successful, ticket on success
     * @throws Exception
     */
    String getCasTicket(String username, String password) throws Exception {
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();        
        
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        String casLogin = response.getHeader("Location");
        assertNotNull(casLogin);
        assertTrue(casLogin.startsWith(loginUrl.toString()));
        
        URL casLoginURL= new URL(casLogin);    
        assertTrue(casLoginURL.getQuery().startsWith("service="));        
        SecurityContext ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Execute redirect to cas login page
        HttpURLConnection conn = (HttpURLConnection) casLoginURL.openConnection();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        StringBuffer buff=new StringBuffer();
        while((line=in.readLine())!=null) {
                buff.append(line);
        }
        in.close();

        
        assertTrue(buff.toString().contains("username"));
        assertTrue(buff.toString().contains("password"));
        
        String actionValue=extractValue("action=\"", buff.toString());
        
        URL url = new URL(casLoginURL.getProtocol(),
                casLoginURL.getHost(),casLoginURL.getPort(),
                actionValue);        
        //System.out.println(buff.toString());
        String cookieValue = getResponseHeaderValue(conn, "Set-Cookie");
        assertNotNull(cookieValue);
        
        Map<String,String> paramMap = new HashMap<String,String>();
        paramMap.put("username",username);
        paramMap.put("password",password);
        paramMap.put("_eventId","submit");
        paramMap.put("submit","LOGIN");
        
        String lt =extractValue("name=\"lt\" value=\"", buff.toString());
        assertNotNull(lt);
        String execution=extractValue("name=\"execution\" value=\"", buff.toString());
        assertNotNull(execution);
        paramMap.put("lt", lt);
        paramMap.put("execution", execution);
        
        String redirectAfterLogin = doLogin(url, paramMap, cookieValue);
        if (redirectAfterLogin==null)
            return null;
        
        assertTrue(redirectAfterLogin.startsWith(serviceUrl.toString()));
        int index = redirectAfterLogin.indexOf("ticket=");
        String ticket=redirectAfterLogin.substring(index+"ticket=".length());
        return ticket;
    }
    
    public void testCASLogin() throws Exception {
        
        /*
                
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setService(serviceUrl.toString());
        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setTicketValidatorUrl(casServerURLPrefix.toString());
        config.setLoginUrl(loginUrl.toString());
        config.setName(casFilterName);        
        config.setUserGroupServiceName("ug1");
        getSecurityManager().saveFilter(config);
        
        ExceptionTranslationFilterConfig exConfig = new ExceptionTranslationFilterConfig();
        exConfig.setClassName(GeoServerExceptionTranslationFilter.class.getName());
        exConfig.setName(CAS_EXCEPTION_TRANSLATION_FILTER);
        exConfig.setAccessDeniedErrorPage("/denied.jsp");
        exConfig.setAuthenticationFilterName(casFilterName);
        getSecurityManager().saveFilter(exConfig);

                
        prepareFiterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,
            CAS_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);

        
        prepareFiterChain("/j_spring_cas_security_check",
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,    
                casFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);

        
        // test success
        String username = "castest";
        String password = username;
        MockHttpServletRequest request= createRequest("/j_spring_cas_security_check");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        String ticket = getCasTicket(username, password);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());pattern
        assertTrue(response.getHeader("Location").endsWith(GeoServerURL.get();UserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        SecurityContext ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        
        // test invalid password
        username = "castest";
        password = "xxxx";
        ticket = getCasTicket(username, password);
        assertNull(ticket);

        // check unknown user
        username = "unknown";
        password =  username;
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = getCasTicket(username, password);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        getProxy().doFilter(request, response, chain);
        // TODO, is this correct
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        // test root user
        username = GeoServerUser.ROOT_USERNAME;
        password = username;
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = getCasTicket(username, password);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
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


        // test root with invalid password
        username = GeoServerUser.ROOT_USERNAME;
        password = "xxxx";
        ticket = getCasTicket(username, password);
        assertNull(ticket);

        // check disabled user
        username="castest";
        password=username;
        updateUser("ug1", username, false);
        request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        ticket = getCasTicket(username, password);
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        getProxy().doFilter(request, response, chain);
        // TODO, is this correct
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNull(ctx);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        updateUser("ug1", username, true);
        
        insertAnonymousFilter(CAS_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
        
        
        ////////////////////////////////////////////////////////////////////////////////// TODO
        /*
        // Test logout                
                
        MockHttpServletRequest request= createRequest("/j_spring_cas_security_check");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        tmp = response.getHeader("Location");
        assertNotNull(tmp);
        assertTrue(tmp.endsWith(GeoServerLogoutFilter.URL_AFTER_LOGOUT));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
         */      

    }
    
    public void testProxyAuth () throws Exception{
        
        
        CasAuthenticationFilterConfig config = new CasAuthenticationFilterConfig();
        config.setClassName(GeoServerCasAuthenticationFilter.class.getName());
        config.setService(serviceUrl.toString());
        config.setCasServerUrlPrefix(casServerURLPrefix.toString());
        config.setTicketValidatorUrl(casServerURLPrefix.toString());
        config.setLoginUrl(loginUrl.toString());
        config.setName(casFilterName);        
        config.setUserGroupServiceName("ug1");
        config.setProxyCallbackUrl(proxyCallbackUrl.toString());
        getSecurityManager().saveFilter(config);
        
        ExceptionTranslationFilterConfig exConfig = new ExceptionTranslationFilterConfig();
        exConfig.setClassName(GeoServerExceptionTranslationFilter.class.getName());
        exConfig.setName(CAS_EXCEPTION_TRANSLATION_FILTER);
        exConfig.setAccessDeniedErrorPage("/denied.jsp");
        exConfig.setAuthenticationFilterName(casFilterName);
        getSecurityManager().saveFilter(exConfig);

                
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,
            casFilterName,
            CAS_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);

        prepareFilterChain(CasAuthenticationFilterConfig.CAS_PROXY_RECEPTOR_PATTERN,
                //GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
                casFilterName);
        
        prepareFilterChain("/j_spring_cas_security_check",
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER,    
                casFilterName);

        SecurityContextHolder.getContext().setAuthentication(null);
        
        String username = "castest";
        String password = username;
        String ticket = getCasTicket(username,password);

        
        MockHttpServletRequest request= createRequest("/j_spring_cas_security_check");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        //request.setMethod("POST");
        request.setupAddParameter("ticket",ticket);
        try {
            createSSLServer();
            httpsServer.start();
            checkSSLServer();
            
            getProxy().doFilter(request, response, chain);
        } finally {
            if (httpsServer!=null)
                httpsServer.stop(1);
        }        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        assertTrue(response.wasRedirectSent());
        assertTrue(response.getHeader("Location").endsWith(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS));
        SecurityContext ctx = (SecurityContext)request.getSession(true).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);        
        assertNotNull(ctx);
        Authentication auth = ctx.getAuthentication();
        assertNotNull(auth);
        assertTrue (auth instanceof CasAuthenticationToken);
        checkForAuthenticatedRole(auth);
        assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        final String proxyTicket = ((CasAuthenticationToken)auth).getAssertion().getPrincipal().getProxyTicketFor(
                config.getService());
        assertNotNull(proxyTicket);
        
        
        
        // Test entry point                
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
                
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());

        
        
        
//        assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());
//        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
//        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        
        
//        MockHttpServletRequest request= createRequest("/foo/bar");
//        MockHttpServletResponse response= new MockHttpServletResponse();
//        MockFilterChain chain = new MockFilterChain();        
//        
//        
//        getProxy().doFilter(request, response, chain);


    }
    
    protected void checkSSLServer() throws Exception {
        String trustStore=System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        System.setProperty("javax.net.ssl.trustStore",keyStoreFile.getCanonicalPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "cascas");
        URL testSSLURL = new URL(proxyCallbackUrl.getProtocol(),
                proxyCallbackUrl.getHost(),proxyCallbackUrl.getPort(),"/test");
        HttpURLConnection con = (HttpURLConnection) testSSLURL.openConnection();
        con.getInputStream().close();
        if (trustStore==null)
            System.clearProperty("javax.net.ssl.trustStore");
        else    
            System.setProperty("javax.net.ssl.trustStore",trustStore);
        if (trustStorePassword==null)
            System.clearProperty("javax.net.ssl.trustStorePassword"); 
         else               
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);                    
    }
    
    protected void createSSLServer() throws Exception{
        
//        keytool -genkey -alias alias -keypass simulator \
//        -keystore lig.keystore -storepass simulator


        InetSocketAddress address = new InetSocketAddress ( proxyCallbackUrl.getPort() );

        // initialise the HTTPS server
        httpsServer = HttpsServer.create ( address, 0 );
        SSLContext sslContext = SSLContext.getInstance ( "TLS" );

        // initialise the keystore
        char[] password = "cascas".toCharArray ();
        KeyStore ks = KeyStore.getInstance ( "JKS" );
        File base = new File(System.getProperty("user.home"), ".geoserver");
        File keystore = new File(base,"cas.jks");
        FileInputStream fis = new FileInputStream ( keystore );
        ks.load ( fis, password );

        // setup the key manager factory
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance ( KeyManagerFactory.getDefaultAlgorithm() );
        kmf.init ( ks, password );

        X509TrustManager trustManager = new  X509TrustManager() {
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
            
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
        }; 

        // setup the trust manager factory
        //TrustManagerFactory tmf = TrustManagerFactory.getInstance ( TrustManagerFactory.getDefaultAlgorithm() );
        //tmf.init ( ks );

        // setup the HTTPS context and parameters
        sslContext.init ( kmf.getKeyManagers (), new TrustManager[]{trustManager}, null );
        httpsServer.setHttpsConfigurator(  new HttpsConfigurator( sslContext )
        {
            public void configure ( HttpsParameters params )
            {
                try
                {
                    // initialise the SSL context
                    SSLContext c = SSLContext.getDefault ();
                    SSLEngine engine = c.createSSLEngine ();
                    params.setNeedClientAuth ( false );
                    params.setCipherSuites ( engine.getEnabledCipherSuites () );
                    params.setProtocols ( engine.getEnabledProtocols () );

                    // get the default parameters
                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters ();
                    params.setSSLParameters ( defaultSSLParameters );
                }
                catch ( Exception ex )
                {
                    throw new RuntimeException(ex);
                }
            }
        } );                
        
        
        httpsServer.createContext(proxyCallbackUrl.getPath(), new HttpsProxyCallBackHandler());
        httpsServer.createContext("/test", new HttpHandler() {            
            @Override
            public void handle(HttpExchange t) throws IOException {
                LOGGER.info("https server working");
                t.getRequestBody().close();                                                                
                t.sendResponseHeaders(200, 0);
                t.getResponseBody().close();
            }
        });
        
        httpsServer.setExecutor(null); // creates a default executor
    }

    class HttpsProxyCallBackHandler  implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            URI uri = ex.getRequestURI();
            ex.getRequestBody().close();
            LOGGER.info("Cas proxy callback: "+uri.toString());
            String query = uri.getQuery();
                                    
            MockHttpServletRequest request= createRequest(CasAuthenticationFilterConfig.CAS_PROXY_RECEPTOR_PATTERN);
            MockHttpServletResponse response= new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            // CAS sends the callback twice, the first time without paramaters
            if (query!=null) {
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

    
    protected MockHttpServletRequest createRequest(String url) {
        MockHttpServletRequest request = super.createRequest(url);
        request.setServerName(serviceUrl.getHost());
        request.setServerPort(serviceUrl.getPort());
        return request;        
    }
    
    public void testCasAuthenticationHelper() throws Exception {
        CasFormAuthenticationHelper helper= new CasFormAuthenticationHelper(casServerURLPrefix, "success", "success");
        assertTrue(helper.ssoLogin());
        helper.ssoLogout();
        helper.ssoLogin(serviceUrl);
    }

}
