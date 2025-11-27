/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vectormosaic.rest;

import static org.geotools.data.util.PropertiesTransformer.propertiesToMap;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.geoserver.rest.catalog.VectorGranuleIngestionConfigurer;
import org.geoserver.rest.catalog.VectorGranuleIngestionMetadata;
import org.geoserver.rest.catalog.VectorGranuleReference;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;

/**
 * Default {@link VectorGranuleIngestionConfigurer} that prepares metadata to ingest a vector granule, including the
 * granule URL and the vector granule footprint.
 */
public class DefaultVectorGranuleIngestionConfigurer implements VectorGranuleIngestionConfigurer {

    private static final String URL_PARAM = "url";

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public VectorGranuleIngestionMetadata configureMetadata(
            Object harvested, Properties vectorGranuleParameters, Properties commonParameters) throws IOException {
        VectorGranuleReference resource = new VectorGranuleReference(harvested);
        URL url = resource.getUrl();
        if (url != null) {
            vectorGranuleParameters.put(URL_PARAM, url.toString());
        }
        VectorGranuleIngestionMetadata result = new VectorGranuleIngestionMetadata();
        DataStore datastore = null;
        SimpleFeatureSource source;
        try {
            Properties dataStoreProperties = new Properties();
            dataStoreProperties.putAll(commonParameters);
            dataStoreProperties.putAll(vectorGranuleParameters);
            datastore = DataStoreFinder.getDataStore(propertiesToMap(dataStoreProperties));
            source = datastore.getFeatureSource(datastore.getTypeNames()[0]);
            result.setFootprint(getEnvelope(source));
        } finally {
            if (datastore != null) {
                datastore.dispose();
            }
        }
        result.setUri(resource.getUri());
        // We are not setting the commonParameters as part of the result
        result.setParams(vectorGranuleParameters);
        return result;
    }
}
