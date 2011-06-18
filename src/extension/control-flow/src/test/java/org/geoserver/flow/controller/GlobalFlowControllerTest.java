package org.geoserver.flow.controller;

import org.geoserver.flow.controller.GlobalFlowController;
import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;

public class GlobalFlowControllerTest extends AbstractFlowControllerTest {
    private static final long MAX_WAIT = 1000;
    
    public void testPriority() {
        GlobalFlowController controller = new GlobalFlowController(1);
        // priority == queue size
        assertEquals(1, controller.getPriority());
    }

    public void testSingleDelay() throws Exception {
        // create a single item flow controller 
        GlobalFlowController controller = new GlobalFlowController(1);

        // make three testing threads that will "process" forever, until we interrupt them
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(controller, new Request(),
                0, Long.MAX_VALUE);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(controller, new Request(),
                0, Long.MAX_VALUE);
        FlowControllerTestingThread t3 = new FlowControllerTestingThread(controller, new Request(),
                0, Long.MAX_VALUE);
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

            // let t1 go and wait until its termination. This should allow t2 to go
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.PROCESSING, t2.state);
            assertEquals(ThreadState.STARTED, t3.state);

            // let t2 go and wait until its termination. This should allow t3 to go
            t2.interrupt();
            waitTerminated(t2, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.COMPLETE, t2.state);
            assertEquals(ThreadState.PROCESSING, t3.state);

            // unlock t3 as well
            t3.interrupt();
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
            waitAndKill(t3, MAX_WAIT);
        }
    }
    
    public void testTimeout() {
        // create a single item flow controller 
        GlobalFlowController controller = new GlobalFlowController(1);

        // make two testing threads that will "process" for 400ms, but with a timeout of 200 on the
        // flow controller
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(controller, new Request(),
                100, 400);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(controller, new Request(),
                100, 400);
        
        // start t1 first, let go t2 after
        try {
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            
            // wait until both terminate
            waitTerminated(t1, MAX_WAIT);
            waitTerminated(t2, MAX_WAIT);
            
            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.TIMED_OUT, t2.state);
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }

    }

    

}
