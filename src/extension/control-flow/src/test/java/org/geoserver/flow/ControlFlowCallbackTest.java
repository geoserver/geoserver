/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
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
        callback.doFilter(null, null, new FilterChain() {

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
        BasicOWSController controller = new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
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
        final BasicOWSController controller = new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
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
        MockFilterChain filterChain = new MockFilterChain(
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
        final BasicOWSController controller = new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
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
        MockFilterChain filterChain = new MockFilterChain(
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

    /**
     * A peer thread holds the only BasicOWSController slot past the configured timeout; a second request then times out
     * on the queue. The stuck-slot watchdog must emit a WARN naming the peer thread, the peer's {@link Request} and a
     * {@code top frame:} string.
     */
    @Test
    public void testStuckSlotWatchdogLogsPeerOnQueueTimeout() throws Exception {
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = 200;
        BasicOWSController controller = new BasicOWSController("WMS", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        CapturingHandler handler = new CapturingHandler();
        Logger logger = ControlFlowCallback.LOGGER;
        Level previous = logger.getLevel();
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);

        CountDownLatch enteredRunning = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Request peerRequest = new Request();
        peerRequest.setService("WMS");
        Thread peer = new Thread(
                () -> {
                    callback.operationDispatched(peerRequest, null);
                    enteredRunning.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    callback.finished(peerRequest);
                },
                "stuck-slot-peer");
        peer.setDaemon(true);
        peer.start();

        try {
            assertTrue("peer thread failed to acquire slot", enteredRunning.await(5, TimeUnit.SECONDS));
            // give the peer a cushion so its held time is clearly past the timeout when the watchdog fires
            Thread.sleep(tc.timeout + 100);

            Request mainRequest = new Request();
            mainRequest.setService("WMS");
            try {
                callback.operationDispatched(mainRequest, null);
                fail("HTTP 503 expected");
            } catch (HttpErrorCodeException e) {
                assertEquals(503, e.getErrorCode());
            }

            List<LogRecord> warns = handler.recordsAt(Level.WARNING);
            assertFalse("expected a watchdog WARN record", warns.isEmpty());
            LogRecord warn = warns.get(0);
            String message = warn.getMessage();
            assertTrue(message, message.contains("peer thread stuck-slot-peer"));
            assertTrue(message, message.contains("holding a slot for"));
            assertTrue(message, message.contains("top frame:"));
        } finally {
            release.countDown();
            peer.join(5000);
            callback.finished(null);
            logger.removeHandler(handler);
            logger.setLevel(previous);
            ControlFlowCallback.ACTIVE_RUNNING.clear();
        }
    }

    /**
     * Single request holds its slot past the configured timeout, then releases normally. The release path must emit a
     * WARN naming the hold duration and referencing the configured timeout.
     */
    @Test
    public void testHeldSlotWarnsOnSlowRelease() throws Exception {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = 100;
        BasicOWSController controller = new BasicOWSController("WMS", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        CapturingHandler handler = new CapturingHandler();
        Logger logger = ControlFlowCallback.LOGGER;
        Level previous = logger.getLevel();
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);

        try {
            Request request = new Request();
            request.setService("WMS");
            callback.operationDispatched(request, null);
            Thread.sleep(tc.timeout + 100);
            callback.finished(request);

            boolean matched = handler.recordsAt(Level.WARNING).stream()
                    .map(LogRecord::getMessage)
                    .anyMatch(m -> m.contains("held flow-controller slot for") && m.contains("over the configured"));
            assertTrue("expected a release-path watchdog WARN", matched);
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previous);
            ControlFlowCallback.ACTIVE_RUNNING.clear();
        }
    }

    /** Minimal JUL {@link Handler} that records every published record for later inspection. */
    private static final class CapturingHandler extends Handler {
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}

        List<LogRecord> recordsAt(Level level) {
            return records.stream().filter(r -> r.getLevel() == level).toList();
        }
    }

    /** A wide open configurator to be used for testing */
    static class TestingConfigurator implements ControlFlowConfigurator {
        List<FlowController> controllers = new ArrayList<>();
        long timeout;
        boolean stale = true;

        @Override
        public Collection<FlowController> buildFlowControllers() throws Exception {
            stale = false;
            return controllers;
        }

        @Override
        public long getTimeout() {
            return timeout;
        }

        @Override
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

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public void requestComplete(Request request) {
            requestCompleteCalls++;
        }

        @Override
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
