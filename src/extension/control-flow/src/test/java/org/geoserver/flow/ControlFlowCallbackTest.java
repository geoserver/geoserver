/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.SimpleThreadBlocker;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HttpServletBean;

public class ControlFlowCallbackTest {

    @Test
    public void testBasicFunctionality() throws IOException, ServletException {
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        final CountingController controller = new CountingController(1, 0);
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);
        callback.doFilter(
                null,
                null,
                new FilterChain() {

                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        callback.operationDispatched(null, null);
                        assertEquals(1, controller.requestIncomingCalls);
                        assertEquals(0, controller.requestCompleteCalls);
                    }
                });

        assertEquals(1, controller.requestIncomingCalls);
        assertEquals(1, controller.requestCompleteCalls);
    }

    @Test
    public void testTimeout() {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = 300;
        CountingController c1 = new CountingController(2, 200);
        CountingController c2 = new CountingController(1, 200);
        tc.controllers.add(c1);
        tc.controllers.add(c2);
        callback.provider = new DefaultFlowControllerProvider(tc);

        try {
            callback.operationDispatched(null, null);
            fail("A HTTP 503 should have been raised!");
        } catch (HttpErrorCodeException e) {
            assertEquals(503, e.getErrorCode());
        }
        assertEquals(1, c1.requestIncomingCalls);
        assertEquals(0, c1.requestCompleteCalls);
        assertEquals(1, c2.requestIncomingCalls);
        assertEquals(0, c1.requestCompleteCalls);
        callback.finished(null);
    }

    @Test
    public void testDelayHeader() {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = Integer.MAX_VALUE;
        CountingController cc = new CountingController(2, 50);
        tc.controllers.add(cc);
        callback.provider = new DefaultFlowControllerProvider(tc);

        Request request = new Request();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        request.setHttpResponse(httpResponse);

        callback.operationDispatched(request, null);
        callback.finished(null);

        String delayHeader = httpResponse.getHeader(ControlFlowCallback.X_RATELIMIT_DELAY);
        assertNotNull(delayHeader);
        long delay = Long.parseLong(delayHeader);
        assertTrue("Delay should be greater than 50 " + delay, delay >= 50);
    }

    @Test
    public void testFailBeforeOperationDispatch() {
        ControlFlowCallback callback = new ControlFlowCallback();
        callback.init((Request) null);
        callback.finished(null);
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
    }

    @Test
    public void testRequestReplaced() {
        // setup a controller hitting on GWC
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        BasicOWSController controller =
                new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        Request r1 = new Request();
        r1.setService("GWC");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        r1.setHttpResponse(httpResponse);

        // setup external request
        callback.operationDispatched(r1, null);
        assertEquals(1, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        // fake a nested WMS request with some code that
        Request r2 = new Request(r1);
        r2.setService("WMS");
        callback.operationDispatched(r2, null);
        // no locking happened on the nested one
        assertEquals(1, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        // finish nested
        callback.finished(r2);
        assertEquals(1, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        // finish outer, but simulate code that does set back the outer request (so it's again
        // called with r2)
        callback.finished(r2);
        // the callback machinery is not fooled and clear stuff anyways
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
    }

    @Test
    public void testFinishedNotCalled() throws IOException, ServletException {
        // setup a controller hitting on GWC
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        final BasicOWSController controller =
                new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        // outer request
        final Request r1 = new Request();
        r1.setService("GWC");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setMethod("GET");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        r1.setHttpRequest(httpRequest);
        r1.setHttpResponse(httpResponse);
        final AtomicBoolean servletCalled = new AtomicBoolean(false);
        MockFilterChain filterChain =
                new MockFilterChain(
                        new HttpServletBean() {
                            @Override
                            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                                    throws ServletException, IOException {
                                servletCalled.set(true);

                                // setup external request
                                callback.operationDispatched(r1, null);
                                assertEquals(1, callback.getRunningRequests());
                                assertEquals(0, callback.getBlockedRequests());
                                assertEquals(1, controller.getRequestsInQueue());

                                // fail to call finished
                            }
                        },
                        callback);
        filterChain.doFilter(httpRequest, httpResponse);
        // check the servlet doing the test has been called
        assertTrue(servletCalled.get());
        // the callback machinery is not fooled and clears stuff anyways
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        assertEquals(0, controller.getRequestsInQueue());
    }

    @Test
    public void testFailNestedRequestParse() throws IOException, ServletException {
        // setup a controller hitting on GWC
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        final BasicOWSController controller =
                new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        // outer request
        final Request r1 = new Request();
        r1.setService("GWC");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setMethod("GET");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        r1.setHttpRequest(httpRequest);
        r1.setHttpResponse(httpResponse);

        final AtomicBoolean servletCalled = new AtomicBoolean(false);
        MockFilterChain filterChain =
                new MockFilterChain(
                        new HttpServletBean() {
                            @Override
                            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                                    throws ServletException, IOException {
                                servletCalled.set(true);

                                // setup external request
                                callback.operationDispatched(r1, null);
                                assertEquals(1, callback.getRunningRequests());
                                assertEquals(0, callback.getBlockedRequests());

                                // call the nested one
                                Request r2 = new Request(r1);
                                callback.operationDispatched(r2, null);
                                assertEquals(1, callback.getRunningRequests());
                                assertEquals(0, callback.getBlockedRequests());
                                assertEquals(1, controller.getRequestsInQueue());

                                // fail to call finished on either
                            }
                        },
                        callback);
        filterChain.doFilter(httpRequest, httpResponse);
        // check the servlet doing the test has been called
        assertTrue(servletCalled.get());
        // the callback machinery is not fooled and clears stuff anyways
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        assertEquals(0, controller.getRequestsInQueue());
    }

    /** A wide open configurator to be used for testing */
    static class TestingConfigurator implements ControlFlowConfigurator {
        List<FlowController> controllers = new ArrayList<FlowController>();
        long timeout;
        boolean stale = true;

        public Collection<FlowController> buildFlowControllers() throws Exception {
            stale = false;
            return controllers;
        }

        public long getTimeout() {
            return timeout;
        }

        public boolean isStale() {
            return stale;
        }
    }

    /** A controller counting requests, can also be used to check for timeouts */
    static class CountingController implements FlowController {

        int priority;
        long delay;
        int requestCompleteCalls;
        int requestIncomingCalls;

        public CountingController(int priority, long delay) {
            this.priority = priority;
            this.delay = delay;
        }

        public int getPriority() {
            return priority;
        }

        public void requestComplete(Request request) {
            requestCompleteCalls++;
        }

        public boolean requestIncoming(Request request, long timeout) {
            requestIncomingCalls++;
            if (delay > 0)
                if (timeout > delay) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("This is unexpected");
                    }
                } else {
                    return false;
                }
            return true;
        }
    }
}
