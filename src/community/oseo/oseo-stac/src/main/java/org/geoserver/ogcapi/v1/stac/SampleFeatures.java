/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.IllegalAttributeException;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.data.DataUtilities;
import org.springframework.stereotype.Component;

/**
 * Keeps a set of sample features keyed by collection, caches them, reacts to reload/reset events to
 * clear the cache
 */
@Component
public class SampleFeatures implements GeoServerLifecycleHandler {

    private static final Object NO_COLLECTION_KEY = new Object();

    private static final Feature NO_SAMPLE =
            new Feature() {
                @Override
                public FeatureType getType() {
                    return null;
                }

                @Override
                public FeatureId getIdentifier() {
                    return null;
                }

                @Override
                public BoundingBox getBounds() {
                    return null;
                }

                @Override
                public GeometryAttribute getDefaultGeometryProperty() {
                    return null;
                }

                @Override
                public void setDefaultGeometryProperty(GeometryAttribute geometryAttribute) {}

                @Override
                public void setValue(Collection<Property> values) {}

                @Override
                public Collection<? extends Property> getValue() {
                    return null;
                }

                @Override
                public Collection<Property> getProperties(Name name) {
                    return null;
                }

                @Override
                public Property getProperty(Name name) {
                    return null;
                }

                @Override
                public Collection<Property> getProperties(String name) {
                    return null;
                }

                @Override
                public Collection<Property> getProperties() {
                    return null;
                }

                @Override
                public Property getProperty(String name) {
                    return null;
                }

                @Override
                public void validate() throws IllegalAttributeException {}

                @Override
                public AttributeDescriptor getDescriptor() {
                    return null;
                }

                @Override
                public void setValue(Object newValue) {}

                @Override
                public Name getName() {
                    return null;
                }

                @Override
                public boolean isNillable() {
                    return false;
                }

                @Override
                public Map<Object, Object> getUserData() {
                    return null;
                }
            };

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
                                                STACService.getProductInCollectionFilter(
                                                        Arrays.asList((String) o));
                                    }
                                    Query q = new Query();
                                    q.setMaxFeatures(1);
                                    q.setFilter(filter);
                                    return Optional.ofNullable(
                                                    DataUtilities.first(ps.getFeatures(q)))
                                            .orElse(NO_SAMPLE);
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
            Feature feature = sampleFeatures.get(key);
            if (feature == NO_SAMPLE) return null;
            return feature;
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
