/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GZIPResponseStreamTest {

    @Test
    public void testStream() throws Exception {
        ByteStreamCapturingHttpServletResponse response =
                new ByteStreamCapturingHttpServletResponse(new MockHttpServletResponse());
        GZIPResponseStream stream = new GZIPResponseStream(response);
        stream.write("Hello world!".getBytes());
        stream.flush();
        stream.close();
        assertEquals("Hello world!", new String(unzip(response.toByteArray())));
    }

    private byte[] unzip(byte[] zipped) throws Exception {
        InputStream stream = new GZIPInputStream(new ByteArrayInputStream(zipped));
        int character;
        ArrayList<Byte> builder = new ArrayList<Byte>();
        while ((character = stream.read()) != -1) {
            builder.add((byte) character);
        }

        byte[] results = new byte[builder.size()];
        for (int i = 0; i < builder.size(); i++) results[i] = builder.get(i).byteValue();
        return results;
    }

    private static class CapturingByteOutputStream extends ServletOutputStream {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        public void write(int b) {
            bos.write(b);
        }

        public byte[] toByteArray() {
            return bos.toByteArray();
        }

        public boolean isReady() {
            return true;
        }

        public void setWriteListener(WriteListener writeListener) {}
    }

    private static class ByteStreamCapturingHttpServletResponse extends HttpServletResponseWrapper {
        CapturingByteOutputStream myOutputStream;

        public ByteStreamCapturingHttpServletResponse(HttpServletResponse r) {
            super(r);
        }

        public ServletOutputStream getOutputStream() throws IOException {
            if (myOutputStream == null) myOutputStream = new CapturingByteOutputStream();
            return myOutputStream;
        }

        public byte[] toByteArray() {
            return myOutputStream.toByteArray();
        }
    }
}
