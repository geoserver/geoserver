package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * A wrapper making sure the servlet container will never see a flush call once the output stream
 * is closed
 *  
 * @author Andrea Aime - GeoSolutions
 */
class FlushSafeResponse extends HttpServletResponseWrapper implements HttpServletResponse {
    
    ServletOutputStream os = null;

    public FlushSafeResponse(HttpServletResponse response) {
        super(response);
    }
    
    @Override
    public synchronized ServletOutputStream getOutputStream() throws IOException {
        if(os == null) {
            os = new FlushSaveServletOutputStream(super.getOutputStream()); 
        }
        return os;
    }
    
    static class FlushSaveServletOutputStream extends ServletOutputStream {

        OutputStream delegate;
        boolean closed = false;

        public FlushSaveServletOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte b[]) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if(!closed) {
                delegate.flush();
            }
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }

}
