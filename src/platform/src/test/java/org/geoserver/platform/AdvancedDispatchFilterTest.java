package org.geoserver.platform;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import static org.junit.Assert.fail;
import org.junit.Test;

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
        final MockHttpServletRequest request = new MyMockRequest();
        request.setServerName("localhost");
        request.setRequestURL("/test?name=0");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final AdvancedDispatchFilter instance = new AdvancedDispatchFilter();
        try {
            instance.doFilter(null, response, filterChain);
        } catch (IOException ex) {
            fail("should work" + ex.getMessage());
        } catch (ServletException ex) {
            fail("should work" + ex.getMessage());
        }
    }

    /**
     * Necessary due to special filtering out delegates with
     * name MockHttpServletRequest.
     */
    class MyMockRequest extends MockHttpServletRequest {

    }

    /**
     * Test of doFilter method, of class AdvancedDispatchFilter.
     * @throws java.lang.Exception
     */
    @Test
    public void testDoFilter() throws Exception {
        System.out.println("doFilter");
        final AdvancedDispatchFilter instance = new AdvancedDispatchFilter();

        try {
            instance.doFilter(null, null, null);
        } catch (NullPointerException ex) {
            System.out.println("FIXME: The test case is a prototype.");
        }
    }
}
