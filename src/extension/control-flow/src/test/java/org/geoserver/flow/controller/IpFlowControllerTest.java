/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.*;

import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;
import org.junit.Test;

public class IpFlowControllerTest extends AbstractFlowControllerTest {

    @Test
    public void testConcurrentRequestsSingleIPAddress() {
        // an ip based flow controller that will allow just one request at a time
        IpFlowController controller = new IpFlowController(1);
        String ipAddress = "127.0.0.1";
        Request firstRequest = buildIpRequest(ipAddress, "");
        FlowControllerTestingThread tSample =
                new FlowControllerTestingThread(firstRequest, 0, 0, controller);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        assertEquals(ThreadState.COMPLETE, tSample.state);

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        // make three testing threads that will "process" forever, and will use the ip to identify
        // themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, controller);

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
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            t2.interrupt();
        } catch (Exception e) {
            // System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }

    @Test
    public void testUserAndIPAddressFlowControl() {
        // an ip based flow controller that will allow just one request at a time
        IpFlowController ipController = new IpFlowController(1);
        UserConcurrentFlowController userController = new UserConcurrentFlowController(1);
        String ipAddress = "127.0.0.1";
        Request firstRequest = buildIpRequest(ipAddress, "");
        FlowControllerTestingThread tSample =
                new FlowControllerTestingThread(firstRequest, 0, 0, userController, ipController);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);

        assertEquals(ThreadState.COMPLETE, tSample.state);

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        // make three testing threads that will "process" forever, and will use the ip to identify
        // themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, ipController);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, ""), 0, Long.MAX_VALUE, ipController);

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
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            t2.interrupt();
            waitTerminated(t2, MAX_WAIT);
            assertEquals(ThreadState.COMPLETE, t2.state);
        } catch (Exception e) {
            // System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }

    // Test 2 remote addresses that are reported as the same, but have gone through a proxy. These
    // two should not queue up
    @Test
    public void testConcurrentProxiedIPAddresses() {
        IpFlowController controller = new IpFlowController(1);
        String ipAddress = "192.168.1.1";

        Request firstRequest = buildIpRequest(ipAddress, "");

        String ip = firstRequest.getHttpRequest().getRemoteAddr();

        FlowControllerTestingThread t1 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, "192.168.1.2"), 0, Long.MAX_VALUE, controller);
        FlowControllerTestingThread t2 =
                new FlowControllerTestingThread(
                        buildIpRequest(ip, "192.168.1.3"), 0, Long.MAX_VALUE, controller);

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
            waitState(ThreadState.PROCESSING, t2, MAX_WAIT);

            t2.interrupt();
        } catch (Exception e) {
            // System.out.println(e.getMessage());
        } finally {
            waitAndKill(t1, MAX_WAIT);
            waitAndKill(t2, MAX_WAIT);
        }
    }
}
