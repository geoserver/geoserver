/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;


public class HelloWorldWithOutput extends HelloWorld {
    public void hello(Message message, OutputStream output)
        throws IOException {
        output.write(message.message.getBytes());
    }
}
