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
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.GeoServerApplication;
import org.opengis.filter.Filter;

/** Simple detachable model listing all the layers in a specific store. */
public class BackupRestoreLayersIndexModel extends LoadableDetachableModel<List<LayerInfo>> {

    private static final long serialVersionUID = -2216296970407724704L;

    private StoreModel<StoreInfo> storeInfo;

    private ResourceFilePanel resourceFilePanel;

    public BackupRestoreLayersIndexModel(
            StoreModel<StoreInfo> storeModel, ResourceFilePanel resourceFilePanel) {
        super();
        this.storeInfo = storeModel;
        this.resourceFilePanel = resourceFilePanel;
    }

    @Override
    protected List<LayerInfo> load() {
        StoreInfo si = (StoreInfo) storeInfo.getObject();

        if (si != null) {
            if (resourceFilePanel.getLayers() != null
                    && !resourceFilePanel.getLayers().isEmpty()
                    && resourceFilePanel.getLayers().containsKey(si.getName())) {
                return resourceFilePanel.getLayers().get(si.getName());
            }

            Catalog catalog = GeoServerApplication.get().getCatalog();
            Filter filter;
            try {
                filter = equal("resource.store.name", si.getName());
            } catch (Exception e) {
                filter = Filter.EXCLUDE;
            }
            CloseableIterator<LayerInfo> iterator = catalog.list(LayerInfo.class, filter);
            List<LayerInfo> layers = new ArrayList<LayerInfo>();
            try {
                while (iterator.hasNext()) {
                    layers.add(iterator.next());
                }
            } finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
            Collections.sort(layers, new LayerComparator());
            return layers;
        }

        return new ArrayList<LayerInfo>();
    }

    /* (non-Javadoc)
     * @see org.apache.wicket.model.LoadableDetachableModel#detach()
     */
    @Override
    public void detach() {
        super.detach();
        if (storeInfo != null) {
            storeInfo.detach();
        }
    }

    protected static class LayerComparator implements Comparator<LayerInfo> {

        public LayerComparator() {
            //
        }

        public int compare(LayerInfo l1, LayerInfo l2) {
            return l1.getName().compareToIgnoreCase(l2.getName());
        }
    }
}
