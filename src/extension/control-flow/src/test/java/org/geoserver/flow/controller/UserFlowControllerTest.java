/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.*;

import javax.servlet.http.Cookie;
import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;
import org.springframework.mock.web.MockHttpServletResponse;

public class UserFlowControllerTest extends AbstractFlowControllerTest {
    public void testConcurrentRequestsSingleUser() {
        // a cookie based flow controller that will allow just one request at a time
        UserConcurrentFlowController controller = new UserConcurrentFlowController(1);

        Request firstRequest = buildCookieRequest(null);
        FlowControllerTestingThread tSample =
                new FlowControllerTestingThread(firstRequest, 0, 0, controller);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        Cookie cookie =
                (Cookie) ((MockHttpServletResponse) firstRequest.getHttpResponse()).getCookies()[0];
        String cookieValue = cookie.getValue();

        // make three testing threads that will "process" forever, and will use the cookie to
        // identify themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildCookieRequest(cookieValue), 0, Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildCookieRequest(cookieValue), 0, Long.MAX_VALUE, controller);
        try {
            // start threads making sure every one of them managed to block somewhere before
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            waitBlocked(t2, MAX_WAIT);

            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.STARTED, t2.state);

            // let t1 go and wait until its termination. This should allow t2 to go
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.PROCESSING, t2.state);

            t2.interrupt();
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }
}
