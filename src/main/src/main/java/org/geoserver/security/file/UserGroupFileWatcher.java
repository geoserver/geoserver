/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.file;

import java.io.IOException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;

/**
 * Watches a file storing user/group information and triggers a load on an external file change.
 *
 * @author christian
 */
public class UserGroupFileWatcher extends FileWatcher implements UserGroupLoadedListener {

    public UserGroupFileWatcher(Resource resource, GeoServerUserGroupService service) {
        super(resource);
        this.service = service;
        checkAndConfigure();
    }

    public UserGroupFileWatcher(
            Resource resource, GeoServerUserGroupService service, long lastModified) {
        super(resource);
        this.service = service;
        this.lastModified = lastModified;
        checkAndConfigure();
    }

    protected GeoServerUserGroupService service;

    public synchronized GeoServerUserGroupService getService() {
        return service;
    }

    public synchronized void setService(GeoServerUserGroupService service) {
        this.service = service;
    }

    /** triggers a load on {@link #service} */
    @Override
    protected void doOnChange() {
        GeoServerUserGroupService theService = getService();
        try {
            if (theService != null) theService.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        String serviceName = service == null ? "UNKNOWN" : service.getName();

        buff.append("FileWatcher for ").append(serviceName);
        buff.append(", ").append(getFileInfo());
        return buff.toString();
    }

    /**
     * Another method to avoid reloads if this object is registered
     *
     * @see GeoServerUserGroupService#registerUserGroupLoadedListener(UserGroupLoadedListener)
     */
    @Override
    public void usersAndGroupsChanged(UserGroupLoadedEvent event) {
        // avoid unnecessary reloads
        setLastModified(resource.lastmodified());
        LOGGER.info("Adjusted last modified for file: " + path);
    }
}
