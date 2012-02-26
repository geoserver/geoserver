package org.geoserver.flow.controller;

import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class IpFlowControllerTest extends AbstractFlowControllerTest {

    private static final long MAX_WAIT = 10000;

    public void testConcurrentRequestsSingleIPAddress() {
        // an ip based flow controller that will allow just one request at a time
        IpFlowController controller = new IpFlowController(1);
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

    // Test 2 remote addresses that are reported as the same, but have gone through a proxy. These two should not queue up
    public void testConcurrentProxiedIPAddresses() {
        IpFlowController controller = new IpFlowController(1);
        String ipAddress = "192.168.1.1";

        Request firstRequest = buildRequest(ipAddress, "");

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        FlowControllerTestingThread t1 = new FlowControllerTestingThread(controller, buildRequest(
                ip, "192.168.1.2"), 0, Long.MAX_VALUE);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(controller, buildRequest(
                ip, "192.168.1.3"), 0, Long.MAX_VALUE);

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

    Request buildRequest(String ipAddress, String proxyIp) {
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
            httpRequest.setHeader("x-forwarded-for", proxyIp + ", " + ipAddress);
        }
        return request;
    }

}
