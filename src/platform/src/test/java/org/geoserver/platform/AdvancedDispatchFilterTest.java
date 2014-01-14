package org.geoserver.platform;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tw
 */
public class AdvancedDispatchFilterTest {
    
    public AdvancedDispatchFilterTest() {
    }
  
    /**
     * Test of destroy method, of class AdvancedDispatchFilter.
     */
    @Test
    public void testPathIsNullNPE() {
        MockHttpServletRequest request = new MyMockRequest();
        request.setServerName("localhost");
        request.setRequestURL("/test?name=0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        
        AdvancedDispatchFilter instance = new AdvancedDispatchFilter();
        try {
            instance.doFilter(request, response, filterChain);
        } catch (Exception ex) {
            fail("should work");
        } 
    }
    
    /**
     * Necessary due to special filtering out delegates with
     * name MockHttpServletRequest.
     */
    class MyMockRequest extends MockHttpServletRequest {
        
    }
}