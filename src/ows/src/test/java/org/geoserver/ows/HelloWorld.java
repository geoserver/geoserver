/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletResponse;

public class HelloWorld {
    Message message;

    public Message hello(Message message) {
        return message;
    }
    
    public void httpErrorCodeException() {
        throw new HttpErrorCodeException( HttpServletResponse.SC_NO_CONTENT );
    }
}
