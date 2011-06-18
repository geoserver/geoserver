package org.geoserver.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.vfny.geoserver.util.PartialBufferedOutputStream2;

import com.mockrunner.mock.web.MockHttpServletResponse;

import junit.framework.TestCase;

public class PartialBufferOutputStream2Test extends TestCase {

    public void testFlushOnClose() throws IOException  {
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        PartialBufferedOutputStream2 pbos = new PartialBufferedOutputStream2(mockResponse);
        PrintStream ps = new PrintStream(pbos);
        ps.print("Hello world!");
        ps.close();
        
        // check the in memory buffer has been flushed to the target output stream
        // close
        assertEquals("Hello world!", mockResponse.getOutputStreamContent());
    }
}
