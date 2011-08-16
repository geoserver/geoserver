/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MonitorServletRequest extends HttpServletRequestWrapper {

    MonitorInputStream input;

    long maxSize;

    public MonitorServletRequest(HttpServletRequest request, long maxSize) {
        super(request);
        this.maxSize = maxSize;
    }

    public byte[] getBodyContent() {
        if (input == null) {
            return null;
        }
        return input.getData();
    }

    public long getBytesRead() {
        if (input == null) {
            return -1;
        }

        return input.getBytesRead();
    }

    @Override
    public MonitorInputStream getInputStream() throws IOException {
        if (input == null) {
            input = new MonitorInputStream(super.getInputStream(), maxSize);
        }
        return input;
    }

    static class MonitorInputStream extends ServletInputStream {

        ByteArrayOutputStream buffer;

        ServletInputStream delegate;

        long nbytes = 0;

        long maxSize;

        public MonitorInputStream(ServletInputStream delegate, long maxSize) {
            this.delegate = delegate;
            this.maxSize = maxSize;
            buffer = new ByteArrayOutputStream();
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

            nbytes += 1;
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
            if (len < 0)
                return;
            if (!bufferIsFull()) {
                if (maxSize > 0) {
                    long residual = maxSize - buffer.size();
                    len = len < residual ? len : (int) residual;
                }
                buffer.write(b, off, len);
            }
        }

        boolean bufferIsFull() {
            return buffer.size() >= maxSize && maxSize > 0;
        }

        public byte[] getData() {
            return buffer.toByteArray();
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
