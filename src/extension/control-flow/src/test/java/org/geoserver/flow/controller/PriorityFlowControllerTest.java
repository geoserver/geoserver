/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class PriorityFlowControllerTest extends AbstractFlowControllerTest {

    static final String PRIORITY_HEADER_NAME = "priorityHeader";

    @Test
    public void testSingleDelay() throws Exception {
        // create a single item flow controller 
        HttpHeaderPriorityProvider priorityProvider = new HttpHeaderPriorityProvider
                (PRIORITY_HEADER_NAME, 0);
        GlobalFlowController controller = new GlobalFlowController(1, new PriorityThreadBlocker
                (1, priorityProvider));

        // make three testing threads that will "process" forever, until we interrupt them
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(buildRequest(1), 0,
                Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(buildRequest(2), 0,
                Long.MAX_VALUE, controller);
        FlowControllerTestingThread t3 = new FlowControllerTestingThread(buildRequest(3), 0,
                Long.MAX_VALUE, controller);
        try {
            // start threads making sure every one of them managed to block somewhere before 
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            waitBlocked(t2, MAX_WAIT);
            t3.start();
            waitBlocked(t3, MAX_WAIT);

            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.STARTED, t2.state);
            assertEquals(ThreadState.STARTED, t3.state);

            // let t1 go and wait until its termination. This should allow t3 to go (has higher 
            // priority)
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            waitState(ThreadState.PROCESSING, t3, MAX_WAIT);
            assertEquals(ThreadState.STARTED, t2.state);

            // let t3 go and wait until its termination. This should allow t2 to go
            t3.interrupt();
            waitTerminated(t3, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.COMPLETE, t3.state);
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            // unlock t2 as well
            t2.interrupt();
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
            waitAndKill(t3, MAX_WAIT);
        }
    }

    @Test
    public void testFirstInFirstOut() throws Exception {
        // create a single item flow controller 
        HttpHeaderPriorityProvider priorityProvider = new HttpHeaderPriorityProvider
                (PRIORITY_HEADER_NAME, 0);
        GlobalFlowController controller = new GlobalFlowController(1, new PriorityThreadBlocker
                (1, priorityProvider));

        // make three testing threads that will "process" forever, until we interrupt them,
        // all having the same priority
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(buildRequest(1), 0,
                Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(buildRequest(1), 0,
                Long.MAX_VALUE, controller);
        FlowControllerTestingThread t3 = new FlowControllerTestingThread(buildRequest(1), 0,
                Long.MAX_VALUE, controller);
        try {
            // start threads making sure every one of them managed to block somewhere before 
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            Thread.sleep(10);
            t2.start();
            waitBlocked(t2, MAX_WAIT);
            Thread.sleep(10);
            t3.start();
            waitBlocked(t3, MAX_WAIT);

            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.STARTED, t2.state);
            assertEquals(ThreadState.STARTED, t3.state);

            // let t1 go and wait until its termination. This should allow t2 to go (has higher 
            // priority because it has been queued later)
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);
            assertEquals(ThreadState.STARTED, t3.state);

            // let t2 go and wait until its termination. This should allow t3 to go
            t2.interrupt();
            waitTerminated(t2, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.COMPLETE, t2.state);
            waitState(ThreadState.PROCESSING, t3, MAX_WAIT);

            // unlock t2 as well
            t2.interrupt();
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
            waitAndKill(t3, MAX_WAIT);
        }
    }

    @Test
    public void testTimeout() {
        // create a single item flow controller 
        HttpHeaderPriorityProvider priorityProvider = new HttpHeaderPriorityProvider
                (PRIORITY_HEADER_NAME, 0);
        GlobalFlowController controller = new GlobalFlowController(1, new PriorityThreadBlocker
                (1, priorityProvider));

        // make two testing threads that will "process" for 400ms, but with a timeout of 100 on the
        // flow controller
        // t2 may start "late" on a slow/noisy/otherwise loaded machine, make extra sture
        // t1 won't start counting until t2 has had an occasion to start
        CountDownLatch latch = new CountDownLatch(1);
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(buildRequest(1), 100,
                400, controller);
        t1.setWaitLatch(latch);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(buildRequest(2), 100,
                400, controller);

        // start t1 first, let go t2 after
        try {
            t1.start();
            waitBlocked(t1, MAX_WAIT); // wait until it blocks on latch
            t2.start();
            waitBlocked(t2, MAX_WAIT); // wait until it blocks on control-flow
            latch.countDown(); // release t1 and make it do it's 400ms wait

            // wait until both terminate
            waitTerminated(t1, MAX_WAIT);
            waitTerminated(t2, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.TIMED_OUT, t2.state);
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);

            System.out.println("Done -----------------------\n\n");
        }
    }

    private Request buildRequest(Integer priority) {
        Request request = new Request();
        MockHttpServletRequest hr = new MockHttpServletRequest();
        if (priority != null) {
            hr.addHeader(PRIORITY_HEADER_NAME, String.valueOf(priority));
        }
        request.setHttpRequest(hr);

        return request;
    }

}
