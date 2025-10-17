/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.IllegalAttributeException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.data.DataUtilities;
import org.geotools.data.complex.feature.type.Types;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.Converters;

/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>Support FeatureSource decorator for FeatureTypeInfo that takes care of mapping the FeatureTypeInfo's FeatureSource
 * with the schema and definition query configured for it.
 *
 * <p>Because GeoServer requires that attributes always be returned in the same order we need a way to smoothly inforce
 * this. Could we use this class to do so? It would need to support writing and locking though.
 *
 * @author Gabriel Rold?n
 */
public class GeoServerFeatureStore extends GeoServerFeatureSource implements SimpleFeatureStore {
    /**
     * Creates a new DEFQueryFeatureLocking object.
     *
     * @param store GeoTools2 FeatureSource
     * @param settings Settings for this store
     */
    GeoServerFeatureStore(FeatureStore<SimpleFeatureType, SimpleFeature> store, Settings settings) {
        super(store, settings);
    }

    /** FeatureStore access (to save casting) */
    SimpleFeatureStore store() {
        return (SimpleFeatureStore) source;
    }

    /** see interface for details. */
    @Override
    public List<FeatureId> addFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> fc) throws IOException {

        enforceFeatureInsertValidation(fc);

        FeatureStore<SimpleFeatureType, SimpleFeature> store = store();

        // check if the feature collection needs to be retyped
        if (!store.getSchema().equals(fc.getSchema())) {
            fc = new RetypingFeatureCollection(DataUtilities.simple(fc), store.getSchema());
        }

        return store().addFeatures(fc);
    }

    private void enforceFeatureInsertValidation(FeatureCollection<SimpleFeatureType, SimpleFeature> fc) {
        try (FeatureIterator<SimpleFeature> features = fc.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                feature.getType()
                        .getAttributeDescriptors()
                        .forEach(attributeDescriptor -> validateFeatureAttributeValue(attributeDescriptor, feature));
            }
        }
    }

    private void validateFeatureAttributeValue(AttributeDescriptor attributeDescriptor, SimpleFeature feature) {
        try {
            Object value = feature.getAttribute(attributeDescriptor.getName());
            Types.validate(attributeDescriptor, value);
        } catch (IllegalAttributeException ex) {
            throw new IllegalArgumentException(
                    "Restriction evaluation failed for attribute '%s' of feature '%s' (%s)"
                            .formatted(attributeDescriptor.getName(), feature.getID(), ex.getMessage()),
                    ex);
        }
    }

    /** */
    @Override
    public void removeFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        store().removeFeatures(filter);
    }

    /** */
    @Override
    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
        FeatureStore<SimpleFeatureType, SimpleFeature> store = store();

        // check if the feature reader needs to be retyped
        if (!store.getSchema().equals(reader.getFeatureType())) {
            reader = new RetypingFeatureCollection.RetypingFeatureReader(reader, store.getSchema());
        }

        store().setFeatures(reader);
    }

    /** */
    @Override
    public void setTransaction(Transaction transaction) {
        store().setTransaction(transaction);
    }

    /** */
    @Override
    public Transaction getTransaction() {
        return store().getTransaction();
    }

    @Override
    public void modifyFeatures(String name, Object attributeValue, Filter filter) throws IOException {

        enforceFeatureUpdateValidation(name, attributeValue);

        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(name, attributeValue, filter);
    }

    @Override
    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter) throws IOException {

        enforceFeatureUpdateValidation(names, attributeValues);

        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(names, attributeValues, filter);
    }

    @Override
    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter) throws IOException {

        String[] names = Stream.of(attributeNames).map(Name::getLocalPart).toArray(String[]::new);
        enforceFeatureUpdateValidation(names, attributeValues);

        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(attributeNames, attributeValues, filter);
    }

    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter) throws IOException {

        enforceFeatureUpdateValidation(attributeName.getLocalPart(), attributeValue);
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(attributeName, attributeValue, filter);
    }

    private void enforceFeatureUpdateValidation(String name, Object attributeValue) {
        enforceFeatureUpdateValidation(new String[] {name}, new Object[] {attributeValue});
    }

    private void enforceFeatureUpdateValidation(String[] attributeNames, Object[] attributeValues) {
        for (int i = 0; i < attributeNames.length; i++) {
            String name = attributeNames[i];
            Object value = attributeValues[i];

            AttributeDescriptor attributeDescriptor = schema.getDescriptor(name);

            Class<?> binding = attributeDescriptor.getType().getBinding();
            Object convertedValue = Converters.convert(value, binding);

            validateAttributeValue(attributeDescriptor, convertedValue);
        }
    }

    private void validateAttributeValue(AttributeDescriptor attributeDescriptor, Object value) {
        try {
            Types.validate(attributeDescriptor, value);
        } catch (IllegalAttributeException ex) {
            throw new IllegalArgumentException(
                    "Restriction evaluation failed for the value of attribute '%s' (%s)"
                            .formatted(attributeDescriptor.getName(), ex.getMessage()),
                    ex);
        }
    }
}
