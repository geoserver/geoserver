/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintStream;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.util.PartialBufferedOutputStream2;

public class PartialBufferOutputStream2Test {

    @Test
    public void testFlushOnClose() throws IOException {
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        PartialBufferedOutputStream2 pbos = new PartialBufferedOutputStream2(mockResponse);
        PrintStream ps = new PrintStream(pbos);
        ps.print("Hello world!");
        ps.close();

        // check the in memory buffer has been flushed to the target output stream
        // close
        assertEquals("Hello world!", mockResponse.getContentAsString());
    }
}
