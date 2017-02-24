/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataAccess;
import org.geotools.data.Parameter;
import org.geotools.feature.FeatureCollection;

/**
 * Default implementation of {@link OpenSearchEoService}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultOpenSearchEoService implements OpenSearchEoService {

    GeoServer geoServer;

    public DefaultOpenSearchEoService(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public OSEODescription description(OSEODescriptionRequest request) throws IOException {
        OpenSearchAccess openSearchAccess = getOpenSearchAccess();
        
        final OSEOInfo service = getService();
        
        List<Parameter> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());
        return new OSEODescription(request, service, geoServer.getGlobal(), searchParameters);
    }

    @Override
    public FeatureCollection search(SearchRequest request) {
        throw new UnsupportedOperationException();
    }

    OpenSearchAccess getOpenSearchAccess() throws IOException {
        OSEOInfo service = getService();
        String openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
        if (openSearchAccessStoreId == null) {
            throw new NullPointerException("OpenSearchAccess is not configured in the"
                    + " OpenSearch for EO panel, please do so");
        }
        DataStoreInfo dataStore = this.geoServer.getCatalog().getDataStore(openSearchAccessStoreId);
        if (dataStore == null) {
            throw new ServiceException("Could not locate OpenSearch data access with identifier "
                    + openSearchAccessStoreId
                    + ", please correct the configuration in the OpenSearch for EO panel");
        }

        DataAccess result = dataStore.getDataStore(null);
        if (result == null) {
            throw new ServiceException("Failed to locate OpenSearch data access with identifier "
                    + openSearchAccessStoreId
                    + ", please correct the configuration in the OpenSearch for EO panel");
        } else if (!(result instanceof OpenSearchAccess)) {
            throw new ServiceException("Data access with identifier " + openSearchAccessStoreId
                    + " does not point to a valid OpenSearchDataAccess, "
                    + "please correct the configuration in the OpenSearch for EO panel");
        }

        return (OpenSearchAccess) result;
    }

    private OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }
}
