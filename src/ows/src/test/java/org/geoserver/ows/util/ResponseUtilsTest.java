/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class ResponseUtilsTest {

    @Test
    public void testBaseURL() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerPort(80);
        assertEquals("http://localhost/", ResponseUtils.baseURL(req));
        req.setServerPort(1234);
        assertEquals("http://localhost:1234/", ResponseUtils.baseURL(req));
        req.setScheme("https");
        assertEquals("https://localhost:1234/", ResponseUtils.baseURL(req));
        req.setServerPort(443);
        assertEquals("https://localhost/", ResponseUtils.baseURL(req));
    }
}
