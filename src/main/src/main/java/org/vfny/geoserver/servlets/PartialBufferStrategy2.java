/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.DispatcherOutputStream;
import org.geoserver.ows.ServiceStrategy;
import org.vfny.geoserver.util.PartialBufferedOutputStream2;

/**
 * <b>PartialBufferStrategy</b><br>
 * Oct 19, 2005<br>
 * <b>Purpose:</b><br>
 * This strategy will buffer the response before it starts streaming it to the user. This will allow
 * for errors to be caught early so a proper error message can be sent to the user. Right now it
 * buffers the first 20KB, enough for a full getCapabilities document.
 *
 * @author Brent Owens (The Open Planning Project)
 * @version
 */
public class PartialBufferStrategy2 implements ServiceStrategy {
    /** Class logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    public static final int DEFAULT_BUFFER_SIZE = 50;
    private PartialBufferedOutputStream2 out = null;
    private int bufferSize;

    public String getId() {
        return "PARTIAL-BUFFER2";
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    private int bufferedSize() {
        if (bufferSize > 0) {
            return bufferSize;
        }

        return DEFAULT_BUFFER_SIZE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#getDestination(javax.servlet.http.HttpServletResponse)
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        out = new PartialBufferedOutputStream2(response, bufferedSize());

        return new DispatcherOutputStream(out);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#flush()
     */
    public void flush(HttpServletResponse response) throws IOException {
        if (out != null) {
            out.forceFlush();
            out = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#abort()
     */
    public void abort() {
        if (out != null) {
            try {
                if (out.abort()) {
                    LOGGER.info("OutputStream was successfully aborted.");
                } else {
                    LOGGER.warning(
                            "OutputStream could not be aborted in time. An error has occurred and could not be sent to the user.");
                }
            } catch (IOException e) {
                LOGGER.warning("Error aborting OutputStream");
                e.printStackTrace();
            }
        }
    }

    public Object clone() throws CloneNotSupportedException {
        PartialBufferStrategy2 clone = new PartialBufferStrategy2();
        clone.bufferSize = bufferSize;

        return clone;
    }
}
