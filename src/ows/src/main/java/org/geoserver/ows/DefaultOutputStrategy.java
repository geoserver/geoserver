/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * A default output strategy which simple writes all output to the output stream of the response.
 *
 * <p>This is the output strategy used by {@link Dispatcher} when no other strategy can be found.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class DefaultOutputStrategy implements ServiceStrategy {
    /** @return The string "default". */
    public String getId() {
        return "default";
    }

    /** @return response.getOutputStream(); */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        return new DispatcherOutputStream(outputStream);
    }

    /** Calls response.getOutputStream().flush() */
    public void flush(HttpServletResponse response) throws IOException {
        response.getOutputStream().flush();
    }

    /** Does nothing. */
    public void abort() {
        // do nothing
    }

    public Object clone() throws CloneNotSupportedException {
        return new DefaultOutputStrategy();
    }
}
