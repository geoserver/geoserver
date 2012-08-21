/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;

import junit.framework.TestCase;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class MonitorFilterTest extends TestCase {
    
    DummyMonitorDAO dao;
    MonitorFilter filter;
    MockFilterChain chain;
    
    @Before
    public void setUp() throws Exception {
        dao = new DummyMonitorDAO();
        
        filter = new MonitorFilter(new Monitor(dao), new MonitorRequestFilter());
        
        chain = new MockFilterChain();
    }

    @After
    public void tearDown() throws Exception {
        
    }
    
    public void testSimple() throws Exception {
       
        HttpServletRequest req = request("GET", "/foo/bar", "12.34.56.78", null, null);
        filter.doFilter(req, response(), chain);
      
        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        assertNull(data.getReferer());
    }
    
    public void testSimple2() throws Exception {
        chain.setServlet(new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                req.getInputStream().read(new byte[10]);
                res.getOutputStream().write("hello".getBytes());
            }
        });
        
        
        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", "baz", null);
        filter.doFilter(req, response(), chain);
        
        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertNull(data.getReferer());
        
        assertEquals(new String(data.getBody()), "baz");
        assertEquals(5, data.getResponseLength());
      
    }
    
    public void testReferer() throws Exception {
        HttpServletRequest req = request("GET", "/foo/bar", "12.34.56.78", null, "http://testhost/testpath");
        filter.doFilter(req, response(), chain);
      
        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        assertEquals("http://testhost/testpath", data.getReferer());
      
    }
    public void testReferrer() throws Exception {
        // "Referrer" was misspelled in the HTTP spec, check if it works with the "correct" 
        // spelling. 
        MockHttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        ((MockHttpServletRequest)req).setHeader("Referrer", "http://testhost/testpath");
        filter.doFilter(req, response(), chain);
        
        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertEquals("http://testhost/testpath", data.getReferer());

      
    }
    
    MockHttpServletRequest request(String method, String path, String remoteAddr, String body, String referer) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setServerName("localhost");
        req.setServletPath(path.substring(0, path.indexOf('/', 1)));
        req.setPathInfo(path.substring(path.indexOf('/', 1)));
        req.setRemoteAddr(remoteAddr);
        req.setBodyContent(body);
        if(referer!=null)
            req.setHeader("Referer", referer);
        return req;
    }
    
    MockHttpServletResponse response() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        return response;
    }
}
