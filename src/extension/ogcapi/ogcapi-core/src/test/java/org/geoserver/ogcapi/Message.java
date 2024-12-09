/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */
package org.geoserver.ogcapi;

public class Message {
    public String message;

    public Message() {}

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            Message other = (Message) obj;

            if (message == null) {
                return other.message == null;
            }

            return message.equals(other.message);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (message != null) ? message.hashCode() : 0;
    }
}
