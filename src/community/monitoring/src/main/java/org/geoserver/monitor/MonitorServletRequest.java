package org.geoserver.monitor;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MonitorServletRequest extends HttpServletRequestWrapper {

    MonitorInputStream input;
    
    public MonitorServletRequest(HttpServletRequest request) {
        super(request);
    }
    
    public byte[] getBodyContent() {
        if (input == null) {
            return null;
        }
        return input.getData();
    }
    
    @Override
    public MonitorInputStream getInputStream() throws IOException {
        if (input == null) {
            input = new MonitorInputStream(super.getInputStream());
        }
        return input;
    }
    
    static class MonitorInputStream extends ServletInputStream {

        ByteBuffer buffer;
        ServletInputStream delegate;
        
        public MonitorInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
            buffer = ByteBuffer.allocate(1024);
            buffer.mark();
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
            return delegate.skip(n);
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (!bufferIsFull()) {
                buffer.put((byte)b);
            }
            return b;
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            int n = delegate.read(b);
            fill(b, 0, n);
            return n;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            fill(b, off, n);
            return n;
        }
        
        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            int n = delegate.readLine(b, off, len);
            fill(b, off, n);
            return n;
        }
        
        void fill(byte[] b, int off, int len) {
            if (len < 0) return;
            if (!bufferIsFull()) {
                int m = Math.min(len, buffer.capacity()-buffer.position());
                buffer.put(b, off, m);
            }
        }
        
        boolean bufferIsFull() {
            return buffer.position() == buffer.capacity();
        }
        
        public byte[] getData() {
            if (bufferIsFull()) {
                return buffer.array();
            }
            
            byte[] data = new byte[buffer.position()];
            ((ByteBuffer)buffer.duplicate().reset()).get(data);
            return data;
        }
        
        
        public void dispose() {
            buffer = null;
            delegate = null;
        }
    }

}
