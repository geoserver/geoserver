/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Simple detachable model listing all the available workspaces
 */
@SuppressWarnings("serial")
public class WorkspacesModel extends LoadableDetachableModel {
    @Override
    protected Object load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<WorkspaceInfo> workspaces = new ArrayList<WorkspaceInfo>(catalog.getWorkspaces());
        Collections.sort(workspaces, new WorkspaceComparator());
        return workspaces;
    }
    
    protected static class WorkspaceComparator implements Comparator<WorkspaceInfo> {

        public WorkspaceComparator(){
            //
        }
        public int compare(WorkspaceInfo w1, WorkspaceInfo w2) {
            return w1.getName().compareToIgnoreCase(w2.getName());
        }
        
    }
}
