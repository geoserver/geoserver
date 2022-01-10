/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.geonode.kombu;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class KombuMessage implements Serializable {

    private String id;

    private String type;

    private String action;

    private String generator = "GeoServer";

    private Date timestamp;

    private String user;

    private String originator;

    private KombuSource source;

    private Map<String, Object> properties;

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setOriginator(String originator) {
        this.originator = originator;
    }

    public void setSource(KombuSource source) {
        this.source = source;
    }

    public void setProperties(Map<String, Object> map) {
        this.properties = map;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getOriginator() {
        return originator;
    }

    public KombuSource getSource() {
        return source;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
