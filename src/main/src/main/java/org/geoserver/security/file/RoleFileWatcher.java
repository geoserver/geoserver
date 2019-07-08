/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.file;

import java.io.IOException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;

/**
 * Watches a file storing role information and triggers a load on external file change
 *
 * @author christian
 */
public class RoleFileWatcher extends FileWatcher implements RoleLoadedListener {

    public RoleFileWatcher(Resource resource, GeoServerRoleService service) {
        super(resource);
        this.service = service;
        checkAndConfigure();
    }

    public RoleFileWatcher(Resource resource, GeoServerRoleService service, long lastModified) {
        super(resource);
        this.service = service;
        this.lastModified = lastModified;
        checkAndConfigure();
    }

    protected GeoServerRoleService service;

    public synchronized GeoServerRoleService getService() {
        return service;
    }

    public synchronized void setService(GeoServerRoleService service) {
        this.service = service;
    }

    /**
     * Triggers a load on {@link #service}
     *
     * <p>(non-Javadoc)
     *
     * @see org.geoserver.security.file.FileWatcher#doOnChange()
     */
    @Override
    protected void doOnChange() {
        GeoServerRoleService theService = getService();
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
     * @see GeoServerRoleService#registerRoleLoadedListener(RoleLoadedListener)
     */
    @Override
    public void rolesChanged(RoleLoadedEvent event) {
        // avoid reloads
        setLastModified(resource.lastmodified());
        LOGGER.info("Adjusted last modified for file: " + path);
    }
}
