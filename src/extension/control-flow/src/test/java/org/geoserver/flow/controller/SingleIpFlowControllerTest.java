package org.geoserver.flow.controller;

import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;

public class SingleIpFlowControllerTest extends IpFlowControllerTest {

    private static final long MAX_WAIT = 10000;

    @Override
    public void testConcurrentRequestsSingleIPAddress() {
        // an ip based flow controller that will allow just one request at a time
        SingleIpFlowController controller = new SingleIpFlowController(1, "127.0.0.1");
        String ipAddress = "127.0.0.1";
        Request firstRequest = buildRequest(ipAddress, "");
        FlowControllerTestingThread tSample = new FlowControllerTestingThread(controller,
                firstRequest, 0, 0);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        assertEquals(ThreadState.COMPLETE, tSample.state);

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        // make three testing threads that will "process" forever, and will use the ip to identify themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(controller, buildRequest(
                ip, ""), 0, Long.MAX_VALUE);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(controller, buildRequest(
                ip, ""), 0, Long.MAX_VALUE);

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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }

    public void testConcurrentRequestsDifferentIPAddress() {
        SingleIpFlowController controller = new SingleIpFlowController(1, "192.168.1.8");
        String ipAddress = "127.0.0.1";
        Request firstRequest = buildRequest(ipAddress, "");
        FlowControllerTestingThread tSample = new FlowControllerTestingThread(controller,
                firstRequest, 0, 0);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        assertEquals(ThreadState.COMPLETE, tSample.state);

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        FlowControllerTestingThread t1 = new FlowControllerTestingThread(controller, buildRequest(
                ip, ""), 0, Long.MAX_VALUE);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(controller, buildRequest(
                ip, ""), 0, Long.MAX_VALUE);

        try {
            // start threads making sure every one of them managed to block somewhere before
            // starting the next one
            t1.start();
            waitBlocked(t1, MAX_WAIT);
            t2.start();
            waitBlocked(t2, MAX_WAIT);

            // Both threads are processing, there is no queuing in this case

            assertEquals(ThreadState.PROCESSING, t1.state);
            assertEquals(ThreadState.PROCESSING, t2.state);

            // let t1 go and wait until its termination. This should allow t2 to go
            t1.interrupt();
            waitTerminated(t1, MAX_WAIT);

            assertEquals(ThreadState.COMPLETE, t1.state);
            assertEquals(ThreadState.PROCESSING, t2.state);

            t2.interrupt();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }

    }

}
