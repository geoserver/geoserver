/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.*;

import javax.servlet.http.Cookie;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class RateFlowControllerTest extends AbstractFlowControllerTest {

    @Test
    public void testCookieRateControl() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 1000, new CookieKeyGenerator());

        // run the first request
        Request firstRequest = buildCookieRequest(null);
        assertTrue(controller.requestIncoming(firstRequest, Integer.MAX_VALUE));
        checkHeaders(firstRequest, "Any OGC request", 2, 1);

        // grab the cookie
        Cookie cookie =
                (Cookie) ((MockHttpServletResponse) firstRequest.getHttpResponse()).getCookies()[0];
        String cookieValue = cookie.getValue();

        // second request
        Request request = buildCookieRequest(cookieValue);
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        checkHeaders(request, "Any OGC request", 2, 0);

        // third one, this one will have to wait
        long start = System.currentTimeMillis();
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        long end = System.currentTimeMillis();
        long delay = end - start;
        assertTrue("Request was not delayed enough: " + delay, delay >= 1000);
        checkHeaders(request, "Any OGC request", 2, 0);

        // fourth one, this one will bail out immediately because we give it not enough wait
        assertFalse(controller.requestIncoming(request, 500));
        checkHeaders(request, "Any OGC request", 2, 0);
    }

    private void checkHeaders(Request request, String context, int limit, int remaining) {
        MockHttpServletResponse response = (MockHttpServletResponse) request.getHttpResponse();
        assertEquals(context, response.getHeader(RateFlowController.X_RATE_LIMIT_CONTEXT));
        assertEquals(
                String.valueOf(limit), response.getHeader(RateFlowController.X_RATE_LIMIT_LIMIT));
        assertEquals(
                String.valueOf(remaining),
                response.getHeader(RateFlowController.X_RATE_LIMIT_REMAINING));
    }

    @Test
    public void testCookie429() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 0, new CookieKeyGenerator());

        // run the first request
        Request firstRequest = buildCookieRequest(null);
        assertTrue(controller.requestIncoming(firstRequest, Integer.MAX_VALUE));

        // grab the cookie
        Cookie cookie =
                (Cookie) ((MockHttpServletResponse) firstRequest.getHttpResponse()).getCookies()[0];
        String cookieValue = cookie.getValue();

        // second request
        Request request = buildCookieRequest(cookieValue);
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));

        // this one should fail with a 429
        try {
            assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        } catch (HttpErrorCodeException e) {
            assertEquals(429, e.getErrorCode());
        }
    }

    @Test
    public void testIpRateControl() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 1000, new IpKeyGenerator());

        // run two requests
        Request request = buildIpRequest("127.0.0.1", "");
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));

        // third one, this one will have to wait
        long start = System.currentTimeMillis();
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        long end = System.currentTimeMillis();
        long delay = end - start;
        assertTrue("Request was not delayed enough: " + delay, delay >= 1000);

        // fourth one, this one will bail out immediately because we give it not enough wait
        assertFalse(controller.requestIncoming(request, 500));
    }

    @Test
    public void testIp429() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 0, new IpKeyGenerator());

        // run two requests
        Request request = buildIpRequest("127.0.0.1", "");
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));

        // this one should fail with a 429
        try {
            assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        } catch (HttpErrorCodeException e) {
            assertEquals(429, e.getErrorCode());
        }
    }
}
