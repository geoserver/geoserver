/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.eo.store.SecuredOpenSearchAccess;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.api.data.DataAccess;

/**
 * Helper object returning the configured {@link OpenSearchAccess}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OpenSearchAccessProvider {

    private final GeoServerSecurityManager securityManager;
    private GeoServer geoServer;
    private Catalog rawCatalog;

    public OpenSearchAccessProvider(GeoServer geoServer, GeoServerSecurityManager securityManager, Catalog rawCatalog) {
        this.geoServer = geoServer;
        this.rawCatalog = rawCatalog;
        this.securityManager = securityManager;
    }

    public OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }

    /**
     * Returns the OpenSearchAccess configured in {@link OSEOInfo}, or throws a service exception in case of
     * mis-configuration
     */
    public OpenSearchAccess getOpenSearchAccess() throws IOException {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        DataStoreInfo dataStore = getDataStoreInfo();

        DataAccess result = dataStore.getDataStore(null);
        if (result == null) {
            throw new OWS20Exception("Failed to locate OpenSearch data access with identifier "
                    + openSearchAccessStoreId
                    + ", please correct the configuration in the OpenSearch for EO panel");
        } else if (!(result instanceof OpenSearchAccess)) {
            throw new OWS20Exception("Data access with identifier "
                    + openSearchAccessStoreId
                    + " does not point to a valid OpenSearchDataAccess, "
                    + "please correct the configuration in the OpenSearch for EO panel, "
                    + "but got instead an istance of "
                    + result.getClass()
                    + "\n. ToString follows: "
                    + result);
        }

        OpenSearchAccess openSearchAccess = (OpenSearchAccess) result;

        if (securityManager.checkAuthenticationForAdminRole()) {
            // admin user, no need to secure anything
            return openSearchAccess;
        } else {
            // non admin user, wrap with security
            return new SecuredOpenSearchAccess(openSearchAccess, geoServer);
        }
    }

    /** Returns the configuration of the store backing the OpenSearch subsystem */
    public DataStoreInfo getDataStoreInfo() {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        if (openSearchAccessStoreId == null) {
            throw new OWS20Exception(
                    "OpenSearchAccess is not configured in the" + " OpenSearch for EO panel, please do so");
        }
        // using rawCatalog to avoid issues with mismatch between workspace and OSEO delegate store
        DataStoreInfo dataStore = this.rawCatalog.getDataStore(openSearchAccessStoreId);

        return dataStore;
    }

    /** List the configured product classes */
    public List<ProductClass> getProductClasses() {
        return ProductClass.getProductClasses(getService());
    }
}
