/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import static org.geoserver.catalog.Predicates.equal;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.GeoServerApplication;
import org.opengis.filter.Filter;

/** Simple detachable model listing all the layers in a specific store. */
@SuppressWarnings("serial")
public class LayersModel extends LoadableDetachableModel<List<LayerInfo>> {

    protected IModel storeInfo;

    public LayersModel(IModel storeModel) {
        this.storeInfo = storeModel;
    }

    @Override
    protected List<LayerInfo> load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        StoreInfo si = (StoreInfo) storeInfo.getObject();
        Filter filter;
        try {
            filter = equal("resource.store.id", si.getId());
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
        return layers;
    }

    @Override
    public void detach() {
        super.detach();
        if (storeInfo != null) {
            storeInfo.detach();
        }
    }
}
