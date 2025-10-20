/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class MonitorServletResponse extends HttpServletResponseWrapper {

    MonitorOutputStream output;
    int status = 200;

    public MonitorServletResponse(HttpServletResponse response) {
        super(response);
    }

    public long getContentLength() {
        if (output == null) {
            return 0;
        }

        return output.getBytesWritten();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (output == null) {
            output = new MonitorOutputStream(super.getOutputStream());
        }
        return output;
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
        super.setStatus(sc);
    }

    @Override
    public int getStatus() {
        return status;
    }

    static class MonitorOutputStream extends ServletOutputStream {

        long nbytes;
        ServletOutputStream delegate;

        public MonitorOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        public long getBytesWritten() {
            return nbytes;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            ++nbytes;
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.write(b);
            nbytes += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            nbytes += len;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }
    }
}
