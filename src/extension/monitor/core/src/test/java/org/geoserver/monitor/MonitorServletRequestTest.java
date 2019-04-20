/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletInputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.monitor.MonitorServletRequest.MonitorInputStream;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;

public class MonitorServletRequestTest {
    static final String THE_REQUEST = "TheRequest";

    static final class SingleInputCallRequest extends MockHttpServletRequest {
        static final byte[] BUFFER = THE_REQUEST.getBytes();

        AtomicBoolean called = new AtomicBoolean(false);

        public javax.servlet.ServletInputStream getInputStream() {
            checkCalled();
            final ByteArrayInputStream bis = new ByteArrayInputStream(BUFFER);
            return new ServletInputStream() {

                @Override
                public int read() throws IOException {
                    return bis.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            checkCalled();
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(BUFFER)));
        }

        private void checkCalled() {
            if (called.get()) {
                fail("Input got retrieved twice");
            }
            called.set(true);
        }
    }

    @Test
    public void testInputStreamMaxSizeZero() throws Exception {
        byte[] data = data();
        DelegatingServletInputStream mock =
                new DelegatingServletInputStream(new ByteArrayInputStream(data));

        MonitorInputStream in = new MonitorInputStream(mock, 0);
        byte[] read = read(in);

        assertEquals(data.length, read.length);

        byte[] buffer = in.getData();
        assertEquals(0, buffer.length);

        // ? why does this report 1 off ?
        assertEquals(data.length - 1, in.getBytesRead());
    }

    @Test
    public void testInputStream() throws Exception {
        byte[] data = data();
        DelegatingServletInputStream mock =
                new DelegatingServletInputStream(new ByteArrayInputStream(data));

        MonitorInputStream in = new MonitorInputStream(mock, 1024);
        byte[] read = read(in);

        assertEquals(data.length, read.length);

        byte[] buffer = in.getData();
        assertEquals(1024, buffer.length);

        for (int i = 0; i < buffer.length; i++) {
            assertEquals(data[i], buffer[i]);
        }

        // ? why does this report 1 off ?
        assertEquals(data.length - 1, in.getBytesRead());
    }

    static byte[] data() throws IOException {
        InputStream in = MonitorServletRequest.class.getResourceAsStream("wms.xml");
        return read(in);
    }

    static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }

        in.close();
        return bytes.toByteArray();
    }

    @Test
    public void testGetReader() throws IOException {
        MockHttpServletRequest mock = new SingleInputCallRequest();

        MonitorServletRequest request = new MonitorServletRequest(mock, 1024);
        try (BufferedReader reader = request.getReader()) {
            assertEquals(THE_REQUEST, reader.readLine());
        }
        ;
        assertArrayEquals(THE_REQUEST.getBytes(), request.getBodyContent());
    }

    @Test
    public void testGetInputStream() throws IOException {
        MockHttpServletRequest mock = new SingleInputCallRequest();

        MonitorServletRequest request = new MonitorServletRequest(mock, 1024);
        try (InputStream is = request.getInputStream()) {
            assertEquals(THE_REQUEST, IOUtils.toString(is, "UTF-8"));
        }
        ;
        assertArrayEquals(THE_REQUEST.getBytes(), request.getBodyContent());
    }
}
