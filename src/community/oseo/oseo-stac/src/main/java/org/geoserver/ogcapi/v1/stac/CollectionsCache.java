/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.OseoEvent;
import org.geoserver.opensearch.eo.OseoEventListener;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.springframework.stereotype.Component;

/** Keeps a set of collections, caches them, reacts to reload/reset events to clear the cache */
@Component
public class CollectionsCache implements GeoServerLifecycleHandler, OseoEventListener {

    private final OpenSearchAccessProvider accessProvider;
    private final LoadingCache<Object, Feature> collections =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<Object, Feature>() {
                                @Override
                                public Feature load(Object o) throws Exception {
                                    FeatureSource<FeatureType, Feature> ps =
                                            accessProvider
                                                    .getOpenSearchAccess()
                                                    .getCollectionSource();
                                    Filter filter = Filter.INCLUDE;
                                    if (o instanceof String) {
                                        filter = STACService.getCollectionFilter((String) o);
                                    }
                                    Query q = new Query();
                                    q.setMaxFeatures(1);
                                    q.setFilter(filter);
                                    return DataUtilities.first(ps.getFeatures(q));
                                }
                            });

    /**
     * Creates a new cache
     *
     * @param gs The GeoServer instance
     * @param accessProvider The OpenSearch access provider
     */
    public CollectionsCache(GeoServer gs, OpenSearchAccessProvider accessProvider) {
        this.accessProvider = accessProvider;
    }

    /**
     * Returns the sample feature for the given collection
     *
     * @param collectionId A collection identifier
     * @return The first feature found for the collection
     * @throws IOException
     */
    public Feature getCollection(String collectionId) throws IOException {
        try {
            if (collectionId == null) {
                return null;
            }
            return collections.get(collectionId);
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
    public void onReset() {
        collections.cleanUp();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {
        collections.cleanUp();
    }

    @Override
    public void onReload() {
        collections.cleanUp();
    }

    @Override
    public void dataStoreChange(OseoEvent event) {
        String collection = event.getCollectionName();
        if (collection != null) collections.refresh(collection);
    }
}
