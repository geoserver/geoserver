/* Copyright (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

/**
 * Pluggable Resource Watcher
 *
 * @author Niels Charlier
 */
public interface ResourceNotificationDispatcher {

    /**
     * Listen for changes to ResourceStore content.
     *
     * <p>Listeners can be configured to check for changes to individual files or directory
     * contents.
     *
     * <ul>
     *   <li>styles: listener receives events for any change to the contents of the styles directory
     *   <li>user_projections/epsg.properties: listener notified for any change to the
     *       epsg.properties resource
     * </ul>
     *
     * <p>Notification is course grained, often just based on change of last modified time stamp, as
     * such they are issued after the change has been performed.
     *
     * @param resource path to resource to listen to
     * @param listener Listener to receive change notification
     */
    public void addListener(String resource, ResourceListener listener);

    /**
     * Remove resource store content listener.
     *
     * @param resource path to resource to listen to
     * @param listener Listener to stop receiving change notification
     * @return true iff successful
     */
    public boolean removeListener(String resource, ResourceListener listener);

    /**
     * Send notification.
     *
     * <p>Events should be propagated to children and parents automatically where applicable,to
     * avoid unnecessary communication between GeoServer instances in a clustered environment.
     * (Delete notifications are propagated to their children. All operations are propagated to
     * their parents.) See {@link SimpleResourceNotificationDispatcher} for an example.
     *
     * @param notification notification of resource change (may be for a single resource or a
     *     directory)
     */
    public void changed(ResourceNotification notification);
}
