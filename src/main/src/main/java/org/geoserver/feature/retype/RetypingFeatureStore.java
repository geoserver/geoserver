/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.feature.RetypingFeatureCollection.RetypingFeatureReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

/**
 * Renaming wrapper for a {@link FeatureStore} instance, to be used along with {@link
 * RetypingDataStore}
 */
public class RetypingFeatureStore extends RetypingFeatureSource implements SimpleFeatureStore {

    RetypingFeatureStore(RetypingDataStore ds, SimpleFeatureStore wrapped, FeatureTypeMap typeMap) {
        super(ds, wrapped, typeMap);
    }

    RetypingFeatureStore(SimpleFeatureStore wrapped, FeatureTypeMap typeMap) throws IOException {
        super(wrapped, typeMap);
    }

    protected SimpleFeatureStore featureStore() {
        return (SimpleFeatureStore) wrapped;
    }

    public Transaction getTransaction() {
        return featureStore().getTransaction();
    }

    public void setTransaction(Transaction transaction) {
        featureStore().setTransaction(transaction);
    }

    public void removeFeatures(Filter filter) throws IOException {
        featureStore().removeFeatures(store.retypeFilter(filter, typeMap));
    }

    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader)
            throws IOException {
        RetypingFeatureReader retypingFeatureReader;
        retypingFeatureReader = new RetypingFeatureReader(reader, typeMap.getOriginalFeatureType());
        featureStore().setFeatures(retypingFeatureReader);
    }

    public List<FeatureId> addFeatures(
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection) throws IOException {
        List<FeatureId> ids =
                featureStore()
                        .addFeatures(
                                new RetypingFeatureCollection(
                                        DataUtilities.simple(collection),
                                        typeMap.getOriginalFeatureType()));
        List<FeatureId> retyped = new ArrayList<FeatureId>();
        for (FeatureId id : ids) {
            retyped.add(
                    RetypingFeatureCollection.reTypeId(
                            id, typeMap.getOriginalFeatureType(), typeMap.getFeatureType()));
        }
        return retyped;
    }

    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException {
        Name[] param = Arrays.stream(names).map(n -> new NameImpl(n)).toArray(n -> new Name[n]);
        modifyFeatures(param, attributeValues, filter);
    }

    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {
        modifyFeatures(new Name[] {new NameImpl(name)}, new Object[] {attributeValue}, filter);
    }

    public void modifyFeatures(Name[] names, Object[] values, Filter filter) throws IOException {
        SimpleFeatureType original = typeMap.getOriginalFeatureType();

        // map back attribute types and values to the original values
        Object[] originalValues = new Object[values.length];
        for (int i = 0; i < names.length; i++) {
            AttributeDescriptor ad = original.getDescriptor(names[i]);
            if (values[i] != null) {
                Class<?> target = ad.getType().getBinding();
                originalValues[i] = Converters.convert(values[i], target);
                if (originalValues[i] == null)
                    throw new IOException("Could not map back " + values[i] + " to type " + target);
            }
        }

        featureStore().modifyFeatures(names, originalValues, store.retypeFilter(filter, typeMap));
    }

    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {
        modifyFeatures(new Name[] {attributeName}, new Object[] {attributeValue}, filter);
    }
}
