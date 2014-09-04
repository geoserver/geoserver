/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.*;

import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;
import org.springframework.security.web.util.RequestMatcher;

import com.mockrunner.mock.web.MockHttpServletRequest;

/**
 * @author christian
 *
 */
public class GeoserverRequestMatcherTest extends GeoServerMockTestSupport {

    @Test
    public void testMacher() {
        GeoServerSecurityFilterChainProxy proxy = new GeoServerSecurityFilterChainProxy(getSecurityManager());
        
        // match all
        VariableFilterChain chain =  new ServiceLoginFilterChain("/**");         
        RequestMatcher matcher = proxy.matcherForChain(chain);        
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        
        // set methods, but match is inactvie
        chain =  new ServiceLoginFilterChain("/**");
        chain.getHttpMethods().add(HTTPMethod.GET);
        chain.getHttpMethods().add(HTTPMethod.POST);
        matcher = proxy.matcherForChain(chain);        
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wms")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.PUT, "/wms")));

        // active method matching
        chain.setMatchHTTPMethod(true);
        matcher = proxy.matcherForChain(chain);        
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wms")));
        assertFalse(matcher.matches(createRequest(HTTPMethod.PUT, "/wms")));
        
        chain =  new ServiceLoginFilterChain("/wfs/**,/web/**");         
        matcher = proxy.matcherForChain(chain);
        
        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms/abc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wfs/acc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/web/abc")));

        chain.getHttpMethods().add(HTTPMethod.GET);
        chain.getHttpMethods().add(HTTPMethod.POST);
        matcher = proxy.matcherForChain(chain);

        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms/abc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wfs/acc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.PUT, "/web/abc")));

        chain.setMatchHTTPMethod(true);
        matcher = proxy.matcherForChain(chain);        
        
        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms/abc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wfs/acc")));
        assertFalse(matcher.matches(createRequest(HTTPMethod.PUT, "/web/abc")));

    }
    
    
    MockHttpServletRequest createRequest(HTTPMethod method,String pathInfo) {
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setServletPath("");
        request.setPathInfo(pathInfo);
        request.setMethod(method.toString());
        return request;
    }
}
