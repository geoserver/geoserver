/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.dggs;

import static org.geotools.data.util.PropertiesTransformer.propertiesToMap;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.rest.catalog.VectorGranuleIngestionConfigurer;
import org.geoserver.rest.catalog.VectorGranuleIngestionMetadata;
import org.geoserver.rest.catalog.VectorGranuleReference;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.dggs.datastore.DGGSStoreFactory;
import org.geotools.util.logging.Logging;

/**
 * DGGS Specific {@link VectorGranuleIngestionConfigurer} that prepares wrapping params to ingest a vector granule as a
 * DGGS datastore.
 */
public class DGGSGranuleIngestionConfigurer implements VectorGranuleIngestionConfigurer {

    private static final Logger LOGGER = Logging.getLogger(DGGSGranuleIngestionConfigurer.class);

    private static final DGGSStoreFactory DGGS_STORE_FACTORY = new DGGSStoreFactory();
    private static final String URI_PARAM = DGGSStoreFactory.DELEGATE_PREFIX + "uri";
    private static final String URL_PARAM = DGGSStoreFactory.DELEGATE_PREFIX + "url";

    @Override
    public String getName() {
        return "dggs";
    }

    @Override
    public VectorGranuleIngestionMetadata configureMetadata(
            Object ingested, Properties granuleParams, Properties commonParams) throws IOException {
        VectorGranuleReference resource = new VectorGranuleReference(ingested);
        // Some factories need uri, some others need url.
        // Let's provide both and then we will remove the unneeded-one.
        URI uri = resource.getUri();
        if (uri != null) {
            granuleParams.put(URI_PARAM, uri.toString());
        }
        URL url = resource.getUrl();
        if (url != null) {
            granuleParams.put(URL_PARAM, url.toString());
        }

        VectorGranuleIngestionMetadata result = new VectorGranuleIngestionMetadata();
        DataStore datastore = null;
        SimpleFeatureSource source;
        try {
            cleanupGranuleParameters(granuleParams, commonParams);
            Properties datastoreParams = new Properties();
            datastoreParams.putAll(commonParams);
            datastoreParams.putAll(granuleParams);
            datastore = DGGS_STORE_FACTORY.createDataStore(propertiesToMap(datastoreParams));
            String[] typeNames = datastore.getTypeNames();
            if (typeNames != null && typeNames.length > 0) {
                source = datastore.getFeatureSource(typeNames[0]);
                // TODO: This can take some time for big collections
                result.setFootprint(getEnvelope(source));
            } else {
                LOGGER.warning("No typename found for the existing source: " + resource.getUri()
                        + " vector granule is potentially empty");
            }

        } finally {
            if (datastore != null) {
                datastore.dispose();
            }
        }
        result.setUri(resource.getUri());
        // We are not setting the commonProperties as part of the result
        result.setParams(granuleParams);
        return result;
    }

    /**
     * This method is looking for a Datastore factory that can process the provided params (including uri and url) and
     * finally remove the ones that are not used by the factory.
     *
     * @param granuleParams
     */
    private void cleanupGranuleParameters(Properties granuleParams, Properties commonParams) {
        Map<String, Object> datastoreConnectionParams = propertiesToMap(granuleParams);
        Map<String, Object> configParams = propertiesToMap(commonParams);
        Map<String, Object> delegateParams = DGGSStoreFactory.extractDelegateParams(datastoreConnectionParams);
        configParams.entrySet().stream()
                .filter(e -> e.getKey().startsWith(DGGSStoreFactory.DELEGATE_PREFIX))
                .forEach(e -> {
                    String bareKey = e.getKey().substring(DGGSStoreFactory.DELEGATE_PREFIX.length());
                    delegateParams.put(bareKey, e.getValue());
                });

        Iterator<DataStoreFactorySpi> availableDSSpis = DataStoreFinder.getAvailableDataStores();
        while (availableDSSpis.hasNext()) {
            DataStoreFactorySpi spi = availableDSSpis.next();
            if (spi.canProcess(delegateParams)) {
                // Clean up the uri/url param based on what is actually
                // used by the factory
                DataAccessFactory.Param[] supportedParams = spi.getParametersInfo();
                Set<String> supportedNames = Arrays.stream(supportedParams)
                        .map(p -> String.valueOf(p.key))
                        .collect(Collectors.toSet());

                boolean urlSupported = supportedNames.contains("url");
                boolean uriSupported = supportedNames.contains("uri");

                if (!urlSupported) {
                    granuleParams.remove(URL_PARAM);
                }
                if (!uriSupported) {
                    granuleParams.remove(URI_PARAM);
                }
                break;
            }
        }
    }
}
