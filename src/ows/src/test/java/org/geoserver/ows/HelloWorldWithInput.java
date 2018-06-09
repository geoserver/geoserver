/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.InputStream;

public class HelloWorldWithInput extends HelloWorld {
    public Message hello(InputStream input) throws IOException {
        byte[] buf = new byte[255];
        int n = input.read(buf);

        return new Message(new String(buf, 0, n));
    }
}
