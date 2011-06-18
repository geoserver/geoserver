/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

public class MessageKvpRequestReader extends KvpRequestReader {
    public MessageKvpRequestReader() {
        super(Message.class);
    }

    public Object createRequest() {
        return new Message();
    }
}
