/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

public class Message {
    public String message;

    public Message() {}

    public Message(String message) {
        this.message = message;
    }

    public Message getMessage() {
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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

    public int hashCode() {
        return (message != null) ? message.hashCode() : 0;
    }
}
