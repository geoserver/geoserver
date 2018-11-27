/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/** @author aless */
public class BackupRestoreWorkspacesIndexModel
        extends LoadableDetachableModel<List<WorkspaceInfo>> {

    private static final long serialVersionUID = 4052600127151308001L;
    private ResourceFilePanel resourceFilePanel;

    public BackupRestoreWorkspacesIndexModel(ResourceFilePanel resourceFilePanel) {
        super();
        this.resourceFilePanel = resourceFilePanel;
    }

    @Override
    protected List<WorkspaceInfo> load() {
        if (resourceFilePanel.getWorkspaces() != null
                && !resourceFilePanel.getWorkspaces().isEmpty()) {
            return resourceFilePanel.getWorkspaces();
        }

        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<WorkspaceInfo> workspaces = new ArrayList<WorkspaceInfo>(catalog.getWorkspaces());
        Collections.sort(workspaces, new WorkspaceComparator());
        return workspaces;
    }

    protected static class WorkspaceComparator implements Comparator<WorkspaceInfo> {

        public WorkspaceComparator() {
            //
        }

        public int compare(WorkspaceInfo w1, WorkspaceInfo w2) {
            return w1.getName().compareToIgnoreCase(w2.getName());
        }
    }
}
