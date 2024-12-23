/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.springframework.stereotype.Component;

/** A response that writes a message to the output stream, used to test the bridge between OWS responses and OGC APIs */
@Component
public class MessageResponse extends Response {
    public MessageResponse() {
        super(Message.class, "text/plain");
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        return "text/plain";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        Message message = (Message) value;
        output.write(message.message.getBytes());
    }

    public void abort(Object value, OutputStream output, Operation operation) throws IOException {}
}
