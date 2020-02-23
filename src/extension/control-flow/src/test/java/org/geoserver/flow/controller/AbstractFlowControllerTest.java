/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.fail;

import java.lang.Thread.State;
import javax.servlet.http.Cookie;
import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Base class providing utilities to test flow controllers
 *
 * @author Andrea Aime - OpenGeo
 */
public abstract class AbstractFlowControllerTest {

    protected static final long MAX_WAIT = 5000;

    /**
     * Waits until the thread enters in WAITING or TIMED_WAITING state
     *
     * @param t the thread
     * @param maxWait max amount of time we'll wait
     */
    void waitBlocked(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.getState() != State.WAITING && t.getState() != State.TIMED_WAITING) {
                if (System.currentTimeMillis() > (start + maxWait))
                    fail("Waited for the thread to be blocked more than maxWait: " + maxWait);
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            fail("Sometime interrupeted our wait: " + e);
        }
    }

    /**
     * Waits until the thread is terminated
     *
     * @param t the thread
     * @param maxWait max amount of time we'll wait
     */
    void waitTerminated(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.getState() != State.TERMINATED) {
                if (System.currentTimeMillis() > (start + maxWait))
                    fail("Waited for the thread to be terminated more than maxWait: " + maxWait);
                Thread.sleep(20);
            }
        } catch (Exception e) {
            // System.out.println("Could not terminate thread " + t);
        }
    }

    /** Waits maxWait for the thread to finish by itself, then forcefully kills it */
    void waitAndKill(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.isAlive()) {
                if (System.currentTimeMillis() > (start + maxWait)) {
                    // forcefully destroy the thread
                    t.interrupt();
                }

                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            fail("Sometime interrupeted our wait: " + e);
        }
    }

    protected Request buildCookieRequest(String gsCookieValue) {
        Request request = new Request();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(new MockHttpServletResponse());

        if (gsCookieValue != null) {
            httpRequest.setCookies(new Cookie(CookieKeyGenerator.COOKIE_NAME, gsCookieValue));
        }
        return request;
    }

    Request buildIpRequest(String ipAddress, String proxyIp) {
        Request request = new Request();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(new MockHttpServletResponse());

        if (ipAddress != null && !ipAddress.equals("")) {
            httpRequest.setRemoteAddr(ipAddress);
        } else {
            httpRequest.setRemoteAddr("127.0.0.1");
        }
        if (!proxyIp.equals("")) {
            httpRequest.addHeader("x-forwarded-for", proxyIp + ", " + ipAddress);
        }
        return request;
    }

    /**
     * Waits for he flow controller testing thread to get into a specified state for a max given
     * amount of time, fail otherwise
     */
    protected void waitState(ThreadState state, FlowControllerTestingThread tt, long maxWait)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!state.equals(tt.state) && System.currentTimeMillis() - start < maxWait) {
            Thread.sleep(20);
        }

        ThreadState finalState = tt.state;
        if (!state.equals(finalState)) {
            fail(
                    "Waited "
                            + maxWait
                            + "ms for FlowControllerTestingThread to get into "
                            + state
                            + ", but it is still in state "
                            + finalState);
        }
    }
}
