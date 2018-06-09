/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

public class RequestWrapperTestSupport {

    protected final String[] testStrings =
            new String[] {
                "Hello, this is a test",
                "LongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLong",
                "",
                "test\ncontaining\nnewlines"
            };

    protected MockHttpServletRequest makeRequest(String body, String queryString)
            throws UnsupportedEncodingException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver");
        request.setQueryString(queryString != null ? queryString : "");
        request.setRemoteAddr("127.0.0.1");
        request.setServletPath("/geoserver");
        request.setContentType("application/x-www-form-urlencoded");

        request.setMethod("POST");
        request.setContent(body.getBytes("UTF-8"));

        MockHttpSession session = new MockHttpSession(new MockServletContext());
        request.setSession(session);

        request.setUserPrincipal(null);

        return request;
    }

    public static void compare(HttpServletRequest reqA, HttpServletRequest reqB) {
        Method[] methods = HttpServletRequest.class.getMethods();

        for (int i = 0; i < methods.length; i++) {
            try {
                if (methods[i].getParameterTypes().length == 0) {
                    Object resultA = methods[i].invoke(reqA);
                    Object resultB = methods[i].invoke(reqB);
                    assertEquals(resultA, resultB);
                }
            } catch (Exception e) {
                // don't do anything, it's fine
            }
        }
    }
}
