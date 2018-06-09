/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

/**
 * Model providing a list of DataStoreInfo backed by a {@link OpenSearchAccess}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSearchAccessListModel extends LoadableDetachableModel<List<DataStoreInfo>> {
    private static final long serialVersionUID = -7742496075623731474L;

    static final Logger LOGGER = Logging.getLogger(OpenSearchAccessListModel.class);

    @Override
    protected List<DataStoreInfo> load() {
        List<DataStoreInfo> stores =
                GeoServerApplication.get().getCatalog().getStores(DataStoreInfo.class);
        List<DataStoreInfo> openSearchAccesses = new ArrayList<>();
        for (DataStoreInfo info : stores) {
            try {
                if (info.getDataStore(null) instanceof OpenSearchAccess) {
                    openSearchAccesses.add(info);
                }
            } catch (Exception e) {
                LOGGER.fine(
                        "Skipping store "
                                + info.getWorkspace().getName()
                                + ":"
                                + info.getName()
                                + " as it cannot be connected to");
            }
        }
        return openSearchAccesses;
    }
}
