/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.DispatcherOutputStream;
import org.geoserver.ows.ServiceStrategy;

/**
 * Fast and Dangeroud service strategy.
 *
 * <p>Will fail when a ServiceException is encountered on writeTo, and will not tell the user about
 * it!
 *
 * <p>This is the worst case scenario, you are trading speed for danger by using this
 * ServiceStrategy.
 *
 * @author jgarnett
 */
public class SpeedStrategy implements ServiceStrategy {
    public String getId() {
        return "SPEED";
    }

    private OutputStream out = null;

    /**
     * Works against the real output stream provided by the response.
     *
     * <p>This is dangerous of course, but fast and exciting.
     *
     * @param response Response provided by doService
     * @return An OutputStream that works against, the response output stream.
     * @throws IOException If response output stream could not be aquired
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        out = response.getOutputStream();

        return new DispatcherOutputStream(out);
    }

    /**
     * Completes writing to Response.getOutputStream.
     *
     * @throws IOException If Response.getOutputStream not available.
     */
    public void flush(HttpServletResponse response) throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /* (non-Javadoc)
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#abort()
     */
    public void abort() {
        // out.close();
    }

    public Object clone() throws CloneNotSupportedException {
        return new SpeedStrategy();
    }
}
