/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/** Simple detachable model listing all the available workspaces from the {@link Catalog#getWorkspaces()}. */
public class WorkspacesModel extends LoadableDetachableModel<List<WorkspaceInfo>> {
    @Serial
    private static final long serialVersionUID = -2014677058862746780L;

    @Override
    protected List<WorkspaceInfo> load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<WorkspaceInfo> workspaces = new ArrayList<>(catalog.getWorkspaces());
        Collections.sort(workspaces, Comparator.comparing(WorkspaceInfo::getName));
        return workspaces;
    }
}
