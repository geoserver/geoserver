/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.springframework.stereotype.Component;

/** Helps testing the bridge between OWS responses and OGC APIs */
@Component
public class HelloResponseMessageConverter extends MessageConverterResponseAdapter<Message> {

    public HelloResponseMessageConverter() {
        super(Message.class, Message.class);
    }
}
