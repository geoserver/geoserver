/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.springframework.stereotype.Component;

/**
 * Keeps a set of sample features keyed by collection, caches them, reacts to reload/reset events to
 * clear the cache
 */
@Component
public class SampleFeatures implements GeoServerLifecycleHandler {

    private static final Object NO_COLLECTION_KEY = new Object();

    private final OpenSearchAccessProvider accessProvider;
    private final LoadingCache<Object, Feature> sampleFeatures =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<Object, Feature>() {
                                @Override
                                public Feature load(Object o) throws Exception {
                                    FeatureSource<FeatureType, Feature> ps =
                                            accessProvider.getOpenSearchAccess().getProductSource();
                                    Filter filter = Filter.INCLUDE;
                                    if (o instanceof String) {
                                        filter =
                                                STACService.getCollectionsFilter(
                                                        Arrays.asList((String) o));
                                    }
                                    Query q = new Query();
                                    q.setMaxFeatures(1);
                                    q.setFilter(filter);
                                    return DataUtilities.first(ps.getFeatures(q));
                                }
                            });

    public SampleFeatures(GeoServer gs, OpenSearchAccessProvider accessProvider) {
        this.accessProvider = accessProvider;
    }

    /**
     * Returns the sample feature for the given collection
     *
     * @param collectionId A collection identifier, or null if a random feature is desired
     * @return The first feature found for the collection, or null if no feature was found
     * @throws IOException
     */
    public Feature getSample(String collectionId) throws IOException {
        Object key = collectionId == null ? NO_COLLECTION_KEY : collectionId;
        try {
            return sampleFeatures.get(key);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException(e);
        }
    }

    /** Returns the schema for the sample features */
    public FeatureType getSchema() throws IOException {
        return accessProvider.getOpenSearchAccess().getProductSource().getSchema();
    }

    @Override
    public void onReset() {}

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {
        sampleFeatures.cleanUp();
    }

    @Override
    public void onReload() {
        sampleFeatures.cleanUp();
    }
}
