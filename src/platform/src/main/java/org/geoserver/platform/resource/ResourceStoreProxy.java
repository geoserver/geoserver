/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

public class ResourceStoreProxy implements ResourceStore {
    
    private ResourceStore delegate;
    
    public ResourceStore getDelegate() {
        return delegate;
    }

    public void setDelegate(ResourceStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public Resource get(String path) {
        return delegate.get(path);
    }

    @Override
    public boolean remove(String path) {
        return delegate.remove(path);
    }

    @Override
    public boolean move(String path, String target) {
        return delegate.move(path, target);
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return delegate.getResourceNotificationDispatcher();
    }

}
