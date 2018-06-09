/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.aggregate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geotools.data.DataStore;
import org.geotools.util.logging.Logging;

/**
 * Reports the list of feature types available in the specified store
 *
 * @author Andrea Aime - GeoSolutiosn
 */
class TypeListModel extends LoadableDetachableModel<List<String>> {
    private static final long serialVersionUID = 5420253236935587959L;

    static final Logger LOGGER = Logging.getLogger(TypeListModel.class);

    IModel<StoreInfo> storeModel;

    public TypeListModel(IModel<StoreInfo> storeModel) {
        this.storeModel = storeModel;
    }

    @Override
    protected List<String> load() {
        StoreInfo store = storeModel.getObject();
        if (store == null) {
            return Collections.emptyList();
        }
        try {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            DataStore ds =
                    (DataStore) catalog.getResourcePool().getDataStore((DataStoreInfo) store);
            String[] typeNames = ds.getTypeNames();
            Arrays.sort(typeNames);
            return Arrays.asList(typeNames);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load feature type list ", e);
            return Collections.emptyList();
        }
    }
}
