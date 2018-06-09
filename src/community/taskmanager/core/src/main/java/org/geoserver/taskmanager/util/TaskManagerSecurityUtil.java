/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** @author Niels Charlier */
@Service
public class TaskManagerSecurityUtil {

    @Autowired
    @Qualifier("rawCatalog")
    private Catalog catalog;

    @Autowired private SecureCatalogImpl secureCatalog;

    @Autowired GeoServerSecurityManager secManager;

    private WorkspaceInfo getWorkspace(String workspaceName) {
        if (workspaceName == null) {
            return catalog.getDefaultWorkspace();
        } else {
            return catalog.getWorkspaceByName(workspaceName);
        }
    }

    public boolean isReadable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) { // lack of default workspace (allow) versus incorrect workspace (deny
            // unless admin)
            return config.getWorkspace() == null
                    || secManager.checkAuthenticationForAdminRole(user);
        } else {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isReadable();
        }
    }

    public boolean isReadable(Authentication user, Batch batch) {
        WorkspaceInfo wi = null;
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) {
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
            if (batch.getWorkspace() != null) { // otherwise ignore this, don't use default ws
                wi = getWorkspace(batch.getWorkspace());
            }
        } else {
            wi = getWorkspace(batch.getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isReadable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check1 =
                    batch.getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        if (wif != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isReadable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check2 =
                    batch.getConfiguration() == null
                            || batch.getConfiguration().getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
            ;
        }
        return check1 && check2;
    }

    public boolean isWriteable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) { // lack of default workspace (allow) versus incorrect workspace (deny
            // unless admin)
            return config.getWorkspace() == null
                    || secManager.checkAuthenticationForAdminRole(user);
        } else {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isWritable();
        }
    }

    public boolean isWritable(Authentication user, Batch batch) {
        WorkspaceInfo wi = null;
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) {
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
            if (batch.getWorkspace() != null) { // otherwise ignore this, don't use default ws
                wi = getWorkspace(batch.getWorkspace());
            }
        } else {
            wi = getWorkspace(batch.getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isWritable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check1 =
                    batch.getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        if (wif != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isWritable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check2 =
                    batch.getConfiguration() == null
                            || batch.getConfiguration().getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        return check1 && check2;
    }

    public boolean isAdminable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) { // lack of default workspace (allow) versus incorrect workspace (deny
            // unless admin)
            return config.getWorkspace() == null
                    || secManager.checkAuthenticationForAdminRole(user);
        } else {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isAdminable();
        }
    }

    public boolean isAdminable(Authentication user, Batch batch) {
        WorkspaceInfo wi = null;
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) { // otherwise ignore this, don't use default ws
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
            if (batch.getWorkspace() != null) {
                wi = getWorkspace(batch.getWorkspace());
            }
        } else {
            wi = getWorkspace(batch.getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isAdminable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check1 =
                    batch.getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        if (wif != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isAdminable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check2 =
                    batch.getConfiguration() == null
                            || batch.getConfiguration().getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        return check1 && check2;
    }

    public boolean isAdminable(Authentication user, WorkspaceInfo ws) {
        WorkspaceAccessLimits limits =
                secureCatalog.getResourceAccessManager().getAccessLimits(user, ws);
        return limits == null || limits.isAdminable();
    }
}
