/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.OWS20Exception;
import org.geotools.data.DataAccess;

/**
 * Helper object returning the configured {@link OpenSearchAccess}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSearchAccessProvider {

    private GeoServer geoServer;

    public OpenSearchAccessProvider(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }

    /**
     * Returns the OpenSearchAccess configured in {@link OSEOInfo}, or throws a service exception in
     * case of mis-configuration
     */
    public OpenSearchAccess getOpenSearchAccess() throws IOException {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        DataStoreInfo dataStore = getDataStoreInfo();

        DataAccess result = dataStore.getDataStore(null);
        if (result == null) {
            throw new OWS20Exception(
                    "Failed to locate OpenSearch data access with identifier "
                            + openSearchAccessStoreId
                            + ", please correct the configuration in the OpenSearch for EO panel");
        } else if (!(result instanceof OpenSearchAccess)) {
            throw new OWS20Exception(
                    "Data access with identifier "
                            + openSearchAccessStoreId
                            + " does not point to a valid OpenSearchDataAccess, "
                            + "please correct the configuration in the OpenSearch for EO panel, "
                            + "but got instead an istance of "
                            + result.getClass()
                            + "\n. ToString follows: "
                            + result);
        }

        return (OpenSearchAccess) result;
    }

    /** Returns the configuration of the store backing the OpenSearch subsystem */
    public DataStoreInfo getDataStoreInfo() {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        if (openSearchAccessStoreId == null) {
            throw new OWS20Exception(
                    "OpenSearchAccess is not configured in the"
                            + " OpenSearch for EO panel, please do so");
        }
        DataStoreInfo dataStore = this.geoServer.getCatalog().getDataStore(openSearchAccessStoreId);

        return dataStore;
    }

    /** List the configured product classes */
    public List<ProductClass> getProductClasses() {
        return ProductClass.getProductClasses(getService());
    }
}
