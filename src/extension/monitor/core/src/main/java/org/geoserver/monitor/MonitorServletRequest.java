/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MonitorServletRequest extends HttpServletRequestWrapper {

    /** Don't restrict the maximum length of a request body. */
    public static final long BODY_SIZE_UNBOUNDED = -1;

    MonitorInputStream input;

    long maxSize;

    public MonitorServletRequest(HttpServletRequest request, long maxSize) {
        super(request);
        this.maxSize = maxSize;
    }

    public byte[] getBodyContent() throws IOException {
        MonitorInputStream stream = getInputStream();
        return stream.getData();
    }

    public long getBytesRead() {
        try {
            MonitorInputStream stream = getInputStream();
            return stream.getBytesRead();
        } catch (IOException ex) {
            return 0;
        }
    }

    @Override
    public MonitorInputStream getInputStream() throws IOException {
        if (input == null) {
            ServletInputStream delegateTo = super.getInputStream();
            input = new MonitorInputStream(delegateTo, maxSize);
        }
        return input;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String encoding = getCharacterEncoding();
        if (encoding == null) {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        } else {
            return new BufferedReader(new InputStreamReader(getInputStream(), encoding));
        }
    }

    static class MonitorInputStream extends ServletInputStream {

        ByteArrayOutputStream buffer;

        ServletInputStream delegate;

        long nbytes = 0;

        long maxSize;

        public MonitorInputStream(ServletInputStream delegate, long maxSize) {
            this.delegate = delegate;
            this.maxSize = maxSize;
            if (maxSize > 0) {
                buffer = new ByteArrayOutputStream();
            }
        }

        public int available() throws IOException {
            return delegate.available();
        }

        public void close() throws IOException {
            delegate.close();
        }

        public void mark(int readlimit) {
            delegate.mark(readlimit);
        }

        public boolean markSupported() {
            return delegate.markSupported();
        }

        public void reset() throws IOException {
            delegate.reset();
        }

        public long skip(long n) throws IOException {
            nbytes += n;
            return delegate.skip(n);
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (!bufferIsFull()) {
                buffer.write((byte) b);
            }

            if (b >= 0) nbytes += 1; // Increment byte count unless EoF marker
            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int n = delegate.read(b);
            fill(b, 0, n);

            nbytes += n;
            return n;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            fill(b, off, n);

            nbytes += n;
            return n;
        }

        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            int n = delegate.readLine(b, off, len);
            fill(b, off, n);

            nbytes += n;
            return n;
        }

        void fill(byte[] b, int off, int len) {
            if (len < 0) return;
            if (!bufferIsFull()) {
                if (maxSize > 0) {
                    long residual = maxSize - buffer.size();
                    len = len < residual ? len : (int) residual;
                }
                buffer.write(b, off, len);
            }
        }

        boolean bufferIsFull() {
            return maxSize == 0 || (buffer.size() >= maxSize && maxSize > 0);
        }

        public byte[] getData() {
            return buffer == null ? new byte[0] : buffer.toByteArray();
        }

        public long getBytesRead() {
            return nbytes;
        }

        public void dispose() {
            buffer = null;
            delegate = null;
        }
    }
}
