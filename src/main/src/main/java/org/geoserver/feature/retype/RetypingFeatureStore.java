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
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.data.DataUtilities;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.Converters;

/** Renaming wrapper for a {@link FeatureStore} instance, to be used along with {@link RetypingDataStore} */
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

    @Override
    public Transaction getTransaction() {
        return featureStore().getTransaction();
    }

    @Override
    public void setTransaction(Transaction transaction) {
        featureStore().setTransaction(transaction);
    }

    @Override
    public void removeFeatures(Filter filter) throws IOException {
        featureStore().removeFeatures(store.retypeFilter(filter, typeMap));
    }

    @Override
    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
        RetypingFeatureReader retypingFeatureReader =
                new RetypingFeatureReader(reader, typeMap.getOriginalFeatureType());
        featureStore().setFeatures(retypingFeatureReader);
    }

    @Override
    public List<FeatureId> addFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> collection)
            throws IOException {
        List<FeatureId> ids = featureStore()
                .addFeatures(new RetypingFeatureCollection(
                        DataUtilities.simple(collection), typeMap.getOriginalFeatureType()));
        List<FeatureId> retyped = new ArrayList<>();
        for (FeatureId id : ids) {
            retyped.add(
                    RetypingFeatureCollection.reTypeId(id, typeMap.getOriginalFeatureType(), typeMap.getFeatureType()));
        }
        return retyped;
    }

    @Override
    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter) throws IOException {
        Name[] param = Arrays.stream(names).map(n -> new NameImpl(n)).toArray(n -> new Name[n]);
        modifyFeatures(param, attributeValues, filter);
    }

    @Override
    public void modifyFeatures(String name, Object attributeValue, Filter filter) throws IOException {
        modifyFeatures(new Name[] {new NameImpl(name)}, new Object[] {attributeValue}, filter);
    }

    @Override
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

    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter) throws IOException {
        modifyFeatures(new Name[] {attributeName}, new Object[] {attributeValue}, filter);
    }
}
