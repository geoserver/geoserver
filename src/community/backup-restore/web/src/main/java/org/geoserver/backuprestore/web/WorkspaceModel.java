/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/** Detachable model for a specific workspace. */
public class WorkspaceModel<T extends WorkspaceInfo> extends LoadableDetachableModel<T> {

    private static final long serialVersionUID = 8956830437048564765L;

    private ResourceFilePanel resourceFilePanel;
    String name;

    private WorkspaceModel(T workspace) {
        super(workspace);
        setObject(workspace);
    }

    public WorkspaceModel(ResourceFilePanel resourceFilePanel, T workspace) {
        this(workspace);
        this.resourceFilePanel = resourceFilePanel;
    }

    public void setObject(T object) {
        super.setObject(object);
        if (object != null) {
            name = object.getName();
        } else {
            name = null;
        }
    };

    @Override
    protected T load() {
        if (name == null) {
            return null;
        }

        if (resourceFilePanel != null
                && resourceFilePanel.getWorkspaces() != null
                && !resourceFilePanel.getWorkspaces().isEmpty()) {
            for (WorkspaceInfo ws : resourceFilePanel.getWorkspaces()) {
                if (ws.getName().equals(name)) {
                    return (T) ws;
                }
            }
        }

        WorkspaceInfo workspace =
                (T) GeoServerApplication.get().getCatalog().getWorkspaceByName(name);
        if (workspace != null) {
            return (T) workspace;
        }
        return getObject();
    }
}
