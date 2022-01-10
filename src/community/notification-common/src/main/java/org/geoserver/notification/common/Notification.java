/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import java.util.Map;

/**
 * Stores notification event informations
 *
 * @author Xandros
 */
public interface Notification {

    /** The type of event */
    public enum Type {
        Catalog,
        Data, /* Request, Service */
    };

    /** The event action, if applicable */
    public enum Action {
        Add,
        Remove,
        Update,
        None
    };

    /**
     * An event handle, identifying the event (can be coming from an external system to avoid
     * re-processing notifications for action the external system has undertaken)
     */
    public String getHandle();

    /** The event type */
    public Type getType();

    /** The event action */
    public Action getAction();

    /**
     * The "object" of the event, could be what has been created/inserted/modified, the container of
     * it, the request, and so on. Typically a catalog object, a service object, or a Request
     */
    public Object getObject();

    /**
     * A set of "properties" attached to the event, could be properties being changed, the bounds
     * being affected, and so on
     */
    public Map<String, Object> getProperties();

    /** The user triggering the change, if any */
    public String getUser();
}
