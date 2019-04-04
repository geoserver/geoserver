/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Copy of the BufferStrategy in main, for testing purposes
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TestBufferStrategy implements ServiceStrategy, OutputStrategyFactory {
    public String getId() {
        return "BUFFER";
    }

    ByteArrayOutputStream buffer = null;

    /**
     * Provides a ByteArrayOutputStream for writeTo.
     *
     * @param response Response being processed.
     * @return A ByteArrayOutputStream for writeTo opperation.
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        buffer = new ByteArrayOutputStream(1024 * 1024);

        return new DispatcherOutputStream(buffer);
    }

    /**
     * Copies Buffer to Response output output stream.
     *
     * @throws IOException If the response outputt stream is unavailable.
     */
    public void flush(HttpServletResponse response) throws IOException {
        if ((buffer == null) || (response == null)) {
            return; // should we throw an Exception here
        }

        OutputStream out = response.getOutputStream();
        buffer.writeTo(out);

        buffer = null;
    }

    /**
     * Clears the buffer with out writing anything out to response.
     *
     * @see org.geoserver.ows.ServiceStrategy#abort()
     */
    public void abort() {
        buffer = null;
    }

    public Object clone() throws CloneNotSupportedException {
        return new TestBufferStrategy();
    }

    @Override
    public ServiceStrategy createOutputStrategy(HttpServletResponse response) {
        return this;
    }
}
