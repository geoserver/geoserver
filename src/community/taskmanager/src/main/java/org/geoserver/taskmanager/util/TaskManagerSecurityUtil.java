/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 
 * @author Niels Charlier
 *
 */
@Service
public class TaskManagerSecurityUtil {
        
    @Autowired
    @Qualifier("rawCatalog")
    private Catalog catalog;
   
    @Autowired
    private SecureCatalogImpl secureCatalog;

    private WorkspaceInfo getWorkspace(String workspaceName) {
        if (workspaceName == null) {
            return catalog.getDefaultWorkspace();
        } else {
            return catalog.getWorkspaceByName(workspaceName);
        }
    }
        
    public boolean isReadable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) {
            return config.getWorkspace() == null;
        } else {
            WorkspaceAccessLimits limits = secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isReadable();
        }
    }

    public boolean isReadable(Authentication user, Batch batch) {
        WorkspaceInfo wi = getWorkspace(batch.getWorkspace());
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) {
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits = secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isReadable();
        } else {
            check1 = batch.getWorkspace() == null;
        }
        if (wif != null) {
            WorkspaceAccessLimits limits = secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isReadable();
        } else {
            check2 = batch.getConfiguration() == null || batch.getConfiguration().getWorkspace() == null;
        }
        return check1 && check2;
    }

    public boolean isWritable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) {
            return config.getWorkspace() == null;
        } else {
            WorkspaceAccessLimits limits = secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isWritable();
        }
    }

    public boolean isWritable(Authentication user, Batch batch) {
        WorkspaceInfo wi = getWorkspace(batch.getWorkspace());
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) {
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits = secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isWritable();
        } else {
            check1 = batch.getWorkspace() == null;
        }
        if (wif != null) {
            WorkspaceAccessLimits limits = secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isWritable();
        } else {
            check2 = batch.getConfiguration() == null || batch.getConfiguration().getWorkspace() == null;
        }
        return check1 && check2;
    }

}
