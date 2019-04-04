/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** @author christian */
public class GeoserverRequestMatcherTest extends GeoServerMockTestSupport {

    GeoServerSecurityFilterChainProxy proxy;

    @Before
    public void setUp() {
        proxy = new GeoServerSecurityFilterChainProxy(getSecurityManager());
    }

    @Test
    public void testMacher() {
        // match all
        VariableFilterChain chain = new ServiceLoginFilterChain("/**");
        RequestMatcher matcher = proxy.matcherForChain(chain);
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));

        // set methods, but match is inactvie
        chain = new ServiceLoginFilterChain("/**");
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

        chain = new ServiceLoginFilterChain("/wfs/**,/web/**");
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

    @Test
    public void testMacherWithQueryString() {
        VariableFilterChain chain =
                new ServiceLoginFilterChain("/wms/**|.*request=getcapabilities.*");
        RequestMatcher matcher = proxy.matcherForChain(chain);

        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&request=GetCapabilities")));
        assertFalse(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET, "/wms?service=WMS&version=1.1.1&request=GetMap")));

        // regex for parameters in any order
        chain = new ServiceLoginFilterChain("/wms/**|(?=.*request=getmap)(?=.*format=image/png).*");
        matcher = proxy.matcherForChain(chain);

        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&request=GetMap&format=image/png")));
        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/png&request=GetMap")));
        assertFalse(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/jpg&request=GetMap")));

        // regex for parameters not contained
        chain = new ServiceLoginFilterChain("/wms/**|(?=.*request=getmap)(?!.*format=image/png).*");
        matcher = proxy.matcherForChain(chain);
        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/jpg&request=GetMap")));
        assertFalse(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/png&request=GetMap")));
    }

    MockHttpServletRequest createRequest(HTTPMethod method, String pathInfo) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("");
        String queryString = null;
        if (pathInfo.indexOf("?") != -1) {
            queryString = pathInfo.substring(pathInfo.indexOf("?") + 1);
            pathInfo = pathInfo.substring(0, pathInfo.indexOf("?"));
        }
        request.setPathInfo(pathInfo);
        if (queryString != null) {
            request.setQueryString(queryString);
        }
        request.setMethod(method.toString());
        return request;
    }
}
