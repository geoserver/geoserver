/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;

public class HelloWorldWithOutput extends HelloWorld {
    public void hello(Message message, OutputStream output) throws IOException {
        output.write(message.message.getBytes());
    }
}
