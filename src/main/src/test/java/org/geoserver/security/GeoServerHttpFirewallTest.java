/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.RequestRejectedException;

public class GeoServerHttpFirewallTest {

    private final GeoServerHttpFirewall firewall = new GeoServerHttpFirewall();

    @After
    public void resetProperty() {
        setProperty(null);
    }

    @Test
    public void testForwardSlashEncoded() {
        // decoded forward slashes are not blocked
        // this is blocked by both strict and default firewalls
        // some servers may block this even if this firewall wasn't blocking it
        doTestPath(null, "%2f", true);
        doTestPath(true, "%2f", true);
        doTestPath(false, "%2f", true);
        doTestPath(null, "%2F", true);
        doTestPath(true, "%2F", true);
        doTestPath(false, "%2F", true);
    }

    @Test
    public void testBackSlashDecoded() {
        // this is actually an invalid URL that servers will probably block
        doTestPath(null, "\\", true);
        doTestPath(true, "\\", true);
        doTestPath(false, "\\", false);
    }

    @Test
    public void testBackSlashEncoded() {
        // some servers may block this even when the strict firewall is disabled
        doTestPath(null, "%5c", true);
        doTestPath(true, "%5c", true);
        doTestPath(false, "%5c", false);
        doTestPath(null, "%5C", true);
        doTestPath(true, "%5C", true);
        doTestPath(false, "%5C", false);
    }

    @Test
    public void testPercentEncoded() {
        // decoded percents create invalid URLs that don't work
        doTestPath(null, "%25", true);
        doTestPath(true, "%25", true);
        doTestPath(false, "%25", false);
    }

    @Test
    public void testPeriodEncoded() {
        // decoded periods are not blocked
        doTestPath(null, "%2e", true);
        doTestPath(true, "%2e", true);
        doTestPath(false, "%2e", false);
        doTestPath(null, "%2E", true);
        doTestPath(true, "%2E", true);
        doTestPath(false, "%2E", false);
    }

    @Test
    public void testSemicolonDecoded() {
        doTestPath(null, ";", true);
        doTestPath(true, ";", true);
        doTestPath(false, ";", false);
    }

    @Test
    public void testSemicolonEncoded() {
        doTestPath(null, "%3b", true);
        doTestPath(true, "%3b", true);
        doTestPath(false, "%3b", false);
        doTestPath(null, "%3B", true);
        doTestPath(true, "%3B", true);
        doTestPath(false, "%3B", false);
    }

    @Test
    public void testNonNormalizedPath() {
        // GeoServer needs this to be allowed
        doTestPath(null, "//", false);
        doTestPath(true, "//", false);
        doTestPath(false, "//", false);
    }

    private void doTestPath(Boolean strict, String path, boolean exceptionExpected) {
        setProperty(strict);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (exceptionExpected) {
            assertThrows(RequestRejectedException.class, () -> this.firewall.getFirewalledRequest(request));
        } else {
            assertNotNull(this.firewall.getFirewalledRequest(request));
        }
    }

    private static void setProperty(Boolean strict) {
        if (strict == null) {
            System.clearProperty(GeoServerHttpFirewall.USE_STRICT_FIREWALL);
        } else {
            System.setProperty(GeoServerHttpFirewall.USE_STRICT_FIREWALL, strict.toString());
        }
    }
}
