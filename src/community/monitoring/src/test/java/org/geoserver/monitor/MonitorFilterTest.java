package org.geoserver.monitor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class MonitorFilterTest extends TestCase {

    public void testSimple() throws Exception {
        DummyMonitorDAO dao = new DummyMonitorDAO();
        
        MonitorFilter filter = new MonitorFilter(new Monitor(dao), new MonitorRequestFilter());
        
        MockFilterChain chain = new MockFilterChain();
       
        HttpServletRequest req = request("GET", "/foo/bar", "12.34.56.78", null);
        filter.doFilter(req, response(), chain);
      
        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        
        chain.setServlet(new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                req.getInputStream().read(new byte[10]);
                res.getOutputStream().write("hello".getBytes());
            }
        });
        
        
        req = request("POST", "/bar/foo", "78.56.34.12", "baz");
        filter.doFilter(req, response(), chain);
        
        data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        
        assertEquals(new String(data.getBody()), "baz");
        assertEquals(5, data.getResponseLength());
      
    }
    
    MockHttpServletRequest request(String method, String path, String remoteAddr, String body ) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setServerName("localhost");
        req.setServletPath(path.substring(0, path.indexOf('/', 1)));
        req.setPathInfo(path.substring(path.indexOf('/', 1)));
        req.setRemoteAddr(remoteAddr);
        req.setBodyContent(body);
        
        return req;
    }
    
    MockHttpServletResponse response() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        return response;
    }
}
