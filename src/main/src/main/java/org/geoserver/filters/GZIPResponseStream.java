/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple streaming gzipping servlet output stream wrapper
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GZIPResponseStream extends ServletOutputStream {
    protected final ServletOutputStream delegateStream;
    protected GZIPOutputStream gzipstream = null;

    protected boolean closed = false;

    public GZIPResponseStream(HttpServletResponse response) throws IOException {
        super();
        closed = false;
        delegateStream = response.getOutputStream();
        gzipstream = new GZIPOutputStream(delegateStream, 4096, true);
    }

    public void close() throws IOException {
        if (closed) {
            throw new IOException("This output stream has already been closed");
        }
        gzipstream.finish();
        closed = true;
    }

    public void flush() throws IOException {
        if (!closed) {
            gzipstream.flush();
        }
    }

    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }
        gzipstream.write((byte) b);
    }

    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }
        gzipstream.write(b, off, len);
    }

    public boolean closed() {
        return (this.closed);
    }
}
