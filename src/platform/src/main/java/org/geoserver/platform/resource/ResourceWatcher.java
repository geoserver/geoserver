/* Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

/**
 * 
 * Pluggable Resource Watcher
 * 
 * @author Niels Charlier
 *
 */
public interface ResourceWatcher {
    
    /**
     * Add resource listener to this watcher.
     * 
     * @param resource the resource to listen to
     * @param listener the resource listener
     * @return true iff successful 
     */
    public void addListener(Resource resource, ResourceListener listener);
    
    /**
     * Remove resource listener from this watcher.
     * 
     * @param resource the resource to listen to
     * @param listener the resource listener
     * @return true iff successful 
     */
    public boolean removeListener(Resource resource, ResourceListener listener);
    
    /**
     * Send notification.
     * 
     * Events should be propagated to children and parents automatically where applicable,to avoid unnecessary
     * communication between GeoServer instances in a clustered environment.
     * (Delete notifications are propagated to their children. All operations are propagated to their parents.)
     * See {@link SimpleResourceWatcher} for an example.
     * 
     * @param notification
     */
    public void changed(ResourceNotification notification);

}
