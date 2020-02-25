/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class RequestUtilsTest {

    @Test
    public void testGetRemoteAddrNotForwarded() {
        HttpServletRequest req = request("192.168.1.1", null);
        assertEquals("192.168.1.1", RequestUtils.getRemoteAddr(req));
    }

    @Test
    public void testGetRemoteAddrSingleForwardedIP() {
        HttpServletRequest req = request("192.168.1.2", "192.168.1.1");
        assertEquals("192.168.1.1", RequestUtils.getRemoteAddr(req));
    }

    @Test
    public void testGetRemoteAddrMultipleForwardedIP() {
        HttpServletRequest req = request("192.168.1.4", "192.168.1.1, 192.168.1.2, 192.168.1.3");
        assertEquals("192.168.1.1", RequestUtils.getRemoteAddr(req));
    }

    private static HttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            req.addHeader("X-Forwarded-For", forwardedFor);
        }
        return req;
    }
}
