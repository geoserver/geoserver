/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;
import static org.easymock.EasyMock.*;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class MonitorFilterTest {
    
    DummyMonitorDAO dao;
    MonitorFilter filter;
    MockFilterChain chain;
    
    static final int MAX_BODY_SIZE = 10;
    static final int LONG_BODY_SIZE = 3*MAX_BODY_SIZE;
    
    @Before
    public void setUp() throws Exception {
        dao = new DummyMonitorDAO();
        
        filter = new MonitorFilter(new Monitor(dao), new MonitorRequestFilter());
        
        chain = new MockFilterChain(new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                res.getOutputStream().write(new byte[0]);
            }
        });
        
        filter.monitor.config.props.put("maxBodySize", Integer.toString(MAX_BODY_SIZE)); // Ensure the configured property is correct for the tests
    }

    @After
    public void tearDown() throws Exception {
        
    }
    
    @Test
    public void testSimple() throws Exception {
       
        HttpServletRequest req = request("GET", "/foo/bar", "12.34.56.78", null, null);
        filter.doFilter(req, response(), chain);
      
        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        assertNull(data.getHttpReferer());
    }
    
    @Test    
    public void testWithBody() throws Exception {
        chain = new MockFilterChain(new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                res.getOutputStream().write("hello".getBytes());
            }
        });
        
        
        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", "baz", null);
        filter.doFilter(req, response(), chain);
        
        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertNull(data.getHttpReferer());
        
        assertEquals(new String(data.getBody()), "baz");
        assertEquals(3, data.getBodyContentLength());
        assertEquals(5, data.getResponseLength());
      
    }
    
    @Test
    public void testWithLongBody() throws Exception {
        chain = new MockFilterChain(new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                res.getOutputStream().write("hello".getBytes());
            }
        });
        
        StringBuilder b = new StringBuilder();
        
        for (int i=0; i<MAX_BODY_SIZE; i++) {
            b.append('b');
        }
        String wanted_body = b.toString();
        for (int i=MAX_BODY_SIZE; i<LONG_BODY_SIZE; i++) {
            b.append('b');
        }
        String given_body = b.toString();
        
        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", given_body, null);
        filter.doFilter(req, response(), chain);
        
        RequestData data = dao.getLast();
        
        assertEquals(wanted_body, new String(data.getBody())); // Should be trimmed to the maximum length
        assertEquals(LONG_BODY_SIZE, data.getBodyContentLength()); // Should be the full length, not the trimmed one
      
    }
    
    @Test
    public void testWithUnboundedBody() throws Exception {
        final int UNBOUNDED_BODY_SIZE= 10000; // Something really big
        
        filter.monitor.config.props.put("maxBodySize", Integer.toString(UNBOUNDED_BODY_SIZE)); // Ensure the configured property is correct for the tests
        
        chain = new MockFilterChain(new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                while(req.getInputStream().read()!=-1); // "read" the stream until the end.
                req.getInputStream().read();
                res.getOutputStream().write("hello".getBytes());
            }
        });
        
        StringBuilder b = new StringBuilder();
        
        for (int i=0; i<UNBOUNDED_BODY_SIZE; i++) {
            b.append(i%10);
        }
        
        String wanted_body = b.toString();
        String given_body = b.toString();
        
        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", given_body, null);
        filter.doFilter(req, response(), chain);
        
        RequestData data = dao.getLast();
        
        assertEquals(wanted_body, new String(data.getBody())); // Should be trimmed to the maximum length
        assertEquals(UNBOUNDED_BODY_SIZE, data.getBodyContentLength()); // Should be the full length, not the trimmed one
      
    }
   
    @Test
    public void testReferer() throws Exception {
        HttpServletRequest req = request("GET", "/foo/bar", "12.34.56.78", null, "http://testhost/testpath");
        filter.doFilter(req, response(), chain);
      
        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        assertEquals("http://testhost/testpath", data.getHttpReferer());
      
    }
    
    @Test
    public void testReferrer() throws Exception {
        // "Referrer" was misspelled in the HTTP spec, check if it works with the "correct" 
        // spelling. 
        MockHttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        ((MockHttpServletRequest)req).addHeader("Referrer", "http://testhost/testpath");
        filter.doFilter(req, response(), chain);
        
        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertEquals("http://testhost/testpath", data.getHttpReferer());
        assertNull(data.getCacheResult());
        assertNull(data.getMissReason());
    }

    @Test
    public void testGWCHeaders() throws Exception {
        MockHttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        MockHttpServletResponse response = response();
        String cacheResult = "Miss";
        response.addHeader(MonitorFilter.GEOWEBCACHE_CACHE_RESULT, cacheResult);
        String missReason = "Wrong planet alignment";
        response.addHeader(MonitorFilter.GEOWEBCACHE_MISS_REASON, missReason);
        filter.doFilter(req, response, chain);

        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertEquals(cacheResult, data.getCacheResult());
        assertEquals(missReason, data.getMissReason());
    }

    @Test
    public void testUserRemoteUser() throws Exception {
        Object principal = new User("username", "", Collections.<GrantedAuthority>emptyList());

        testRemoteUser(principal);
    }

    @Test
    public void testUserDetailsRemoteUser() throws Exception {
        UserDetails principal = createMock(UserDetails.class);

        expect(principal.getUsername()).andReturn("username");
        replay(principal);

        testRemoteUser(principal);
    }

    private void testRemoteUser(Object principal) throws Exception {
        Authentication authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();
        assertEquals("username", data.getRemoteUser());

        SecurityContextHolder.getContext().setAuthentication(null);
    }
    
    MockHttpServletRequest request(String method, String path, String remoteAddr, String body, String referer) throws UnsupportedEncodingException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setServerName("localhost");
        req.setServletPath(path.substring(0, path.indexOf('/', 1)));
        req.setPathInfo(path.substring(path.indexOf('/', 1)));
        req.setRemoteAddr(remoteAddr);
        if(body==null) body=""; // MockHttpServletRequest#getInputStream doesn't like null bodies 
                                // and throws NullPointerException. It should probably do something useful like return an empty stream or throw
                                // IOException.
        req.setContent(body.getBytes("UTF-8"));
        if(referer!=null)
            req.addHeader("Referer", referer);
        return req;
    }
    
    MockHttpServletResponse response() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        return response;
    }
}
