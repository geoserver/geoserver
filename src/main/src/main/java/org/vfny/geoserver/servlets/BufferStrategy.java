/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.DispatcherOutputStream;
import org.geoserver.ows.ServiceStrategy;

/**
 * A safe Service strategy that buffers output until writeTo completes.
 *
 * <p>This strategy wastes memory, for saftey. It represents a middle ground between SpeedStrategy
 * and FileStrategy
 *
 * @author jgarnett
 */
public class BufferStrategy implements ServiceStrategy {
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
        return new BufferStrategy();
    }
}
