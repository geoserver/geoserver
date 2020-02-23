/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Renaming wrapper for a {@link FeatureSource} instance, to be used along with {@link
 * RetypingDataStore}
 */
public class RetypingFeatureSource implements SimpleFeatureSource {

    SimpleFeatureSource wrapped;

    FeatureTypeMap typeMap;

    RetypingDataStore store;

    Map listeners = new HashMap();

    /**
     * Builds a retyping wrapper
     *
     * @param targetSchema The target schema can have a different name and less attributes than the
     *     original one
     */
    public static SimpleFeatureSource getRetypingSource(
            SimpleFeatureSource wrapped, SimpleFeatureType targetSchema) throws IOException {
        FeatureTypeMap map = new FeatureTypeMap(wrapped.getSchema(), targetSchema);

        if (wrapped instanceof SimpleFeatureLocking) {
            return new RetypingFeatureLocking((SimpleFeatureLocking) wrapped, map);
        } else if (wrapped instanceof SimpleFeatureStore) {
            return new RetypingFeatureStore((SimpleFeatureStore) wrapped, map);
        } else {
            return new RetypingFeatureSource(wrapped, map);
        }
    }

    RetypingFeatureSource(
            RetypingDataStore ds, SimpleFeatureSource wrapped, FeatureTypeMap typeMap) {
        this.store = ds;
        this.wrapped = wrapped;
        this.typeMap = typeMap;
    }

    RetypingFeatureSource(SimpleFeatureSource wrapped, final FeatureTypeMap typeMap)
            throws IOException {
        this.wrapped = wrapped;
        this.typeMap = typeMap;
        this.store =
                new RetypingDataStore((DataStore) wrapped.getDataStore()) {
                    @Override
                    protected String transformFeatureTypeName(String originalName) {
                        if (typeMap.getOriginalName().equals(originalName)) {
                            // rename
                            return typeMap.getName();
                        } else if (typeMap.getName().equals(originalName)) {
                            // hide
                            return null;
                        } else {
                            return originalName;
                        }
                    }

                    @Override
                    protected SimpleFeatureType transformFeatureType(SimpleFeatureType original)
                            throws IOException {
                        if (typeMap.getOriginalName().equals(original.getTypeName())) {
                            return typeMap.featureType;
                        } else {
                            return super.transformFeatureType(original);
                        }
                    }

                    @Override
                    public String[] getTypeNames() throws IOException {
                        // Populate local hashmaps with new values.
                        Map<String, FeatureTypeMap> forwardMapLocal =
                                new ConcurrentHashMap<String, FeatureTypeMap>();
                        Map<String, FeatureTypeMap> backwardsMapLocal =
                                new ConcurrentHashMap<String, FeatureTypeMap>();

                        forwardMapLocal.put(typeMap.getOriginalName(), typeMap);
                        backwardsMapLocal.put(typeMap.getName(), typeMap);

                        // Replace the member variables.
                        forwardMap = forwardMapLocal;
                        backwardsMap = backwardsMapLocal;

                        return new String[] {typeMap.getName()};
                    }
                };
    }

    /**
     * Returns the same name than the feature type (ie, {@code getSchema().getName()} to honor the
     * simple feature land common practice of calling the same both the Features produces and their
     * types
     *
     * @since 1.7
     * @see FeatureSource#getName()
     */
    public Name getName() {
        return getSchema().getName();
    }

    public void addFeatureListener(FeatureListener listener) {
        FeatureListener wrapper = new WrappingFeatureListener(this, listener);
        listeners.put(listener, wrapper);
        wrapped.addFeatureListener(wrapper);
    }

    public void removeFeatureListener(FeatureListener listener) {
        FeatureListener wrapper = (FeatureListener) listeners.get(listener);
        if (wrapper != null) {
            wrapped.removeFeatureListener(wrapper);
            listeners.remove(listener);
        }
    }

    public ReferencedEnvelope getBounds() throws IOException {
        // not fully correct if we use this to shave attributes too, but this is
        // not in the scope now
        return wrapped.getBounds();
    }

    public ReferencedEnvelope getBounds(Query query) throws IOException {
        // not fully correct if we use this to shave attributes too, but this is
        // not in the scope now
        return wrapped.getBounds(store.retypeQuery(query, typeMap));
    }

    public int getCount(Query query) throws IOException {
        return wrapped.getCount(store.retypeQuery(query, typeMap));
    }

    public DataStore getDataStore() {
        return store;
    }

    public SimpleFeatureCollection getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        if (query.getTypeName() == null) {
            query = new Query(query);
            ((Query) query).setTypeName(typeMap.getName());
        } else if (!typeMap.getName().equals(query.getTypeName())) {
            throw new IOException(
                    "Cannot query this feature source with "
                            + query.getTypeName()
                            + " since it serves only "
                            + typeMap.getName());
        }

        // GEOS-3210, if the query specifies a subset of property names we need to take that into
        // account
        SimpleFeatureType target = typeMap.getFeatureType(query);
        return new RetypingFeatureCollection(
                wrapped.getFeatures(store.retypeQuery(query, typeMap)), target);
    }

    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(typeMap.getName(), filter));
    }

    public SimpleFeatureType getSchema() {
        return typeMap.getFeatureType();
    }

    public Set getSupportedHints() {
        return wrapped.getSupportedHints();
    }

    public ResourceInfo getInfo() {
        return wrapped.getInfo();
    }

    public QueryCapabilities getQueryCapabilities() {
        return wrapped.getQueryCapabilities();
    }
}
