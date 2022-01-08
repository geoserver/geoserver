/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import java.util.Map;
import org.geoserver.notification.common.Notification;

public class NotificationImpl implements Notification {

    private Type type;

    private String handle;

    private Action action;

    private Map<String, Object> properties;

    private String user;

    private Object object;

    public NotificationImpl(
            Type type,
            String handle,
            Action action,
            Object object,
            Map<String, Object> properties,
            String user) {
        this.type = type;
        this.handle = handle;
        this.action = action;
        this.properties = properties;
        this.user = user;
        this.object = object;
    }

    @Override
    public String getHandle() {
        return handle;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String getUser() {
        return user;
    }
}
