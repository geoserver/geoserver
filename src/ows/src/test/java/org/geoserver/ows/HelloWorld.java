/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.ServiceException;

public class HelloWorld {
    Message message;

    public Message hello(Message message) {
        return message;
    }

    public void httpErrorCodeException() {
        throw new HttpErrorCodeException(HttpServletResponse.SC_NO_CONTENT);
    }

    public void wrappedHttpErrorCodeException() {
        try {
            throw new HttpErrorCodeException(HttpServletResponse.SC_NO_CONTENT);
        } catch (Exception e) {
            throw new ServiceException("Wrapping code error", e);
        }
    }

    public void badRequestHttpErrorCodeException() {
        throw new HttpErrorCodeException(HttpServletResponse.SC_BAD_REQUEST);
    }

    public void httpErrorCodeExceptionWithContentType() {
        throw new HttpErrorCodeException(HttpServletResponse.SC_OK, "{\"hello\":\"world\"}")
                .setContentType("application/json");
    }
}
