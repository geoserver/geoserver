/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MonitorServletResponse extends HttpServletResponseWrapper {

    MonitorOutputStream output;
    
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
    

    static class MonitorOutputStream extends ServletOutputStream {

        long nbytes;
        OutputStream delegate;

        public MonitorOutputStream(OutputStream delegate) {
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
        public void write(byte b[]) throws IOException {
            delegate.write(b);
            nbytes += b.length;
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
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
    }
}
