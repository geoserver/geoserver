/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.io.IOException;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.Link;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;

/** Allows creation of a new user in users.properties */
public abstract class AbstractSecurityPage extends GeoServerSecuredPage {

    public static String ServiceNameKey = "serviceName";
    public static String TabbedPanelId = "tabbedPanel";
    /** Indicates if model data has changed */
    boolean dirty = false;

    protected GeoServerDialog dialog;

    public AbstractSecurityPage() {
        add(dialog = new GeoServerDialog("dialog"));
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    protected void setReturnPageDirtyAndReturn(boolean dirty) {
        if (returnPage instanceof AbstractSecurityPage) {
            ((AbstractSecurityPage) returnPage).setDirty(dirty);
        }
        doReturn();
    }

    public Link<Page> getCancelLink() {
        return new Link<Page>("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setReturnPageDirtyAndReturn(false);
            }
        };
    }

    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    public GeoServerUserGroupService getUserGroupService(String name) {
        try {
            return getSecurityManager().loadUserGroupService(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GeoServerRoleService getRoleService(String name) {
        try {
            return getSecurityManager().loadRoleService(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasRoleStore(String name) {
        return getRoleService(name).canCreateStore();
    }

    public GeoServerRoleStore getRoleStore(String name) throws IOException {
        return getRoleService(name).createStore();
    }

    public boolean hasUserGroupStore(String name) {
        return getUserGroupService(name).canCreateStore();
    }

    public GeoServerUserGroupStore getUserGroupStore(String name) throws IOException {
        return getUserGroupService(name).createStore();
    }
}
