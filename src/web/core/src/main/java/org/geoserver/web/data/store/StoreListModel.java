/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Model providing the store list
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StoreListModel extends LoadableDetachableModel<List<StoreInfo>> {
    private static final long serialVersionUID = -7742496075623731474L;

    @Override
    protected List<StoreInfo> load() {
        List<StoreInfo> stores = GeoServerApplication.get().getCatalog().getStores(StoreInfo.class);
        stores = new ArrayList<StoreInfo>(stores);
        Collections.sort(
                stores,
                new Comparator<StoreInfo>() {
                    public int compare(StoreInfo o1, StoreInfo o2) {
                        if (o1.getWorkspace().equals(o2.getWorkspace())) {
                            return o1.getName().compareTo(o2.getName());
                        }
                        return o1.getWorkspace().getName().compareTo(o2.getWorkspace().getName());
                    }
                });
        return stores;
    }
}
