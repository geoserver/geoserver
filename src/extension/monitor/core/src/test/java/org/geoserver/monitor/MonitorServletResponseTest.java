/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.geoserver.monitor.MonitorServletRequestTest.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.geoserver.monitor.MonitorServletResponse.MonitorOutputStream;
import org.junit.Test;

import com.mockrunner.mock.web.MockServletOutputStream;

public class MonitorServletResponseTest {

    @Test
    public void testOutputStream() throws IOException {
        byte[] data = data();
        
        MockServletOutputStream mock = new MockServletOutputStream();
        MonitorOutputStream out = new MonitorOutputStream(mock);
        out.write(data);
        
        assertEquals(data.length, mock.getBinaryContent().length);
        assertEquals(data.length, out.getBytesWritten());
    }
}
