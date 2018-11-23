/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import static org.geoserver.catalog.Predicates.equal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.GeoServerApplication;
import org.opengis.filter.Filter;

/** @author Alessio Fabiani, Geo-Solutions S.A.S. */
public class BackupRestoreStoresIndexModel extends LoadableDetachableModel<List<StoreInfo>> {

    private static final long serialVersionUID = -998149757898741087L;

    private WorkspaceModel<WorkspaceInfo> workspace;

    private ResourceFilePanel resourceFilePanel;

    public BackupRestoreStoresIndexModel(
            WorkspaceModel<WorkspaceInfo> workspace, ResourceFilePanel resourceFilePanel) {
        super();
        this.workspace = workspace;
        this.resourceFilePanel = resourceFilePanel;
    }

    @Override
    protected List<StoreInfo> load() {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();

        if (ws != null) {
            if (resourceFilePanel.getStores() != null
                    && !resourceFilePanel.getStores().isEmpty()
                    && resourceFilePanel.getStores().containsKey(ws.getName())) {
                return resourceFilePanel.getStores().get(ws.getName());
            }

            Catalog catalog = GeoServerApplication.get().getCatalog();
            Filter filter;
            try {
                filter = equal("workspace.name", ws.getName());
            } catch (Exception e) {
                filter = Filter.EXCLUDE;
            }
            CloseableIterator<StoreInfo> iterator = catalog.list(StoreInfo.class, filter);
            List<StoreInfo> stores = new ArrayList<StoreInfo>();
            try {
                while (iterator.hasNext()) {
                    stores.add(iterator.next());
                }
            } finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
            Collections.sort(stores, new StoreComparator());
            return stores;
        }

        return new ArrayList<StoreInfo>();
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.model.LoadableDetachableModel#detach()
     */
    @Override
    public void detach() {
        super.detach();
        if (workspace != null) {
            workspace.detach();
        }
    }

    protected static class StoreComparator implements Comparator<StoreInfo> {

        public StoreComparator() {
            //
        }

        public int compare(StoreInfo s1, StoreInfo s2) {
            return s1.getName().compareToIgnoreCase(s2.getName());
        }
    }
}
