/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
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
