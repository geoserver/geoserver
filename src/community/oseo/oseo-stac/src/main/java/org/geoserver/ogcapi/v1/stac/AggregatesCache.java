/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geotools.api.data.FeatureSource;

/**
 * Keeps a set of aggregate values keyed by aggregate,OSEO collection identifier, and property,
 * caches them with a time to live, reacts to GeoService lifecycle events to reset the cache
 */
public class AggregatesCache implements GeoServerLifecycleHandler {
    private final GeoServer geoServer;
    private final OSEOInfo service;
    private final OpenSearchAccessProvider accessProvider;

    private LoadingCache<AggregateCacheKey, Object> aggregates;

    public AggregatesCache(GeoServer geoServer, OpenSearchAccessProvider accessProvider) {

        this.geoServer = geoServer;
        service = geoServer.getService(OSEOInfo.class);
        this.accessProvider = accessProvider;
        initCache();
    }

    private void initCache() {
        long duration =
                service.getAggregatesCacheTTL() == null ? 0 : service.getAggregatesCacheTTL();
        TimeUnit unit =
                service.getAggregatesCacheTTLUnit() == null
                        ? TimeUnit.HOURS
                        : TimeUnit.valueOf(service.getAggregatesCacheTTLUnit().toUpperCase());

        aggregates =
                CacheBuilder.newBuilder()
                        .expireAfterWrite(duration, unit)
                        .build(
                                new CacheLoader<AggregateCacheKey, Object>() {
                                    @Override
                                    public Object load(AggregateCacheKey key) throws Exception {
                                        String property = key.getProperty();
                                        String aggregate = key.getAggregate();
                                        String collectionIdentifier = key.getCollectionIdentifier();
                                        OpenSearchAccess openSearchAccess =
                                                accessProvider.getOpenSearchAccess();
                                        FeatureSource productSource =
                                                openSearchAccess.getProductSource();
                                        AggregateStats aggregateStats =
                                                AggregateFactory.getAggregateStats(
                                                        AggregateFactory.AggregateType.fromString(
                                                                aggregate));
                                        return aggregateStats.getStat(
                                                productSource, collectionIdentifier, property);
                                    }
                                });
    }

    /**
     * Loads an aggregate value into the cache
     *
     * @param key the key to use
     * @param aggregate the aggregate value to cache
     */
    public void loadAggregate(AggregateCacheKey key, Object aggregate) {
        aggregates.put(key, aggregate);
    }

    /**
     * Gets an aggregate value from the cache
     *
     * @param key the key to use
     * @return the aggregate value, if present
     */
    public Object getWrappedAggregate(AggregateCacheKey key) throws IOException {
        try {
            if (key == null) {
                return null;
            }
            return aggregates.get(key);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException(e);
        }
    }

    @Override
    public void onReset() {}

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {
        aggregates.cleanUp();
    }

    @Override
    public void onReload() {
        aggregates.cleanUp();
        initCache();
    }

    public static class AggregateCacheKey {
        private String aggregate;
        private String collectionIdentifier;
        private String property;

        public AggregateCacheKey(String aggregate, String collectionIdentifier, String property) {
            this.aggregate = aggregate;
            this.collectionIdentifier = collectionIdentifier;
            this.property = property;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AggregateCacheKey that = (AggregateCacheKey) o;
            return Objects.equals(aggregate, that.aggregate)
                    && Objects.equals(collectionIdentifier, that.collectionIdentifier)
                    && Objects.equals(property, that.property);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aggregate, collectionIdentifier, property);
        }

        public String getAggregate() {
            return aggregate;
        }

        public void setAggregate(String aggregate) {
            this.aggregate = aggregate;
        }

        public String getCollectionIdentifier() {
            return collectionIdentifier;
        }

        public void setCollectionIdentifier(String collectionIdentifier) {
            this.collectionIdentifier = collectionIdentifier;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }
}
