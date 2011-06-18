package org.geoserver.flow.controller;

import javax.servlet.http.Cookie;

import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class UserFlowControllerTest extends AbstractFlowControllerTest {
    private static final long MAX_WAIT = 1000;
    
    public void testConcurrentRequestsSingleUser() {
        // a cookie based flow controller that will allow just one request at a time
        UserFlowController controller = new UserFlowController(1);
        
        Request firstRequest = buildRequest(null);
        FlowControllerTestingThread tSample = new FlowControllerTestingThread(controller, firstRequest,
                0, 0);
        tSample.start();
        waitTerminated(tSample, MAX_WAIT);
        
        Cookie cookie = (Cookie) ((MockHttpServletResponse) firstRequest.getHttpResponse()).getCookies().get(0);
        String cookieValue = cookie.getValue();
        
        // make three testing threads that will "process" forever, and will use the cookie to identify themselves
        // as the same client, until we interrupt them
        FlowControllerTestingThread t1 = new FlowControllerTestingThread(controller, buildRequest(cookieValue),
                0, Long.MAX_VALUE);
        FlowControllerTestingThread t2 = new FlowControllerTestingThread(controller, buildRequest(cookieValue),
                0, Long.MAX_VALUE);
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
    
    Request buildRequest(String gsCookieValue) {
        Request request = new Request();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(new MockHttpServletResponse());
        
        if(gsCookieValue != null) {
            httpRequest.addCookie(new Cookie(UserFlowController.COOKIE_NAME, gsCookieValue));
        }
        return request;
    }
}
