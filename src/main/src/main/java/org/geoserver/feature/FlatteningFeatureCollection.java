/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentDataStore;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * A DecoratingSimpleFeatureCollection that is used for "flattening" SimpleFeatures that may contain
 * other SimpleFeatures as attributes (used in WFS 2.0 joins to store the tuple as a simple
 * feature). This allows to generate outputs for formats do not support nested structures (e.g,
 * CSV).-
 */
public class FlatteningFeatureCollection extends DecoratingSimpleFeatureCollection {

    private SimpleFeatureType flattenedType;

    private FlatteningFeatureCollection(
            SimpleFeatureCollection delegate, SimpleFeatureType flattenedType) {
        super(delegate);
        this.flattenedType = flattenedType;
    }

    /**
     * Flattens a SimpleFeatureCollection that may contain SimpleFeatures as attributes of other
     * features.
     *
     * @param collection The input SimpleFeatureCollection
     * @return A SimpleFeatureCollection whose features have no SimpleFeature attributes, or the
     *     original one, if no SimpleFeature attributes were found
     */
    public static SimpleFeatureCollection flatten(SimpleFeatureCollection collection) {
        SimpleFeatureType schema = collection.getSchema();

        // collect the attributes
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
        scanAttributeDescriptors(attributeDescriptors, schema, null);

        // build the flattened feature type
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(schema.getName());
        for (AttributeDescriptor desc : attributeDescriptors) builder.add(desc);
        SimpleFeatureType flattenedType = builder.buildFeatureType();

        // if the number of attributes is the same, we did not encounter a new attribute
        if (collection.getSchema().getAttributeCount() == flattenedType.getAttributeCount()) {
            return collection;
        }

        return new FlatteningFeatureCollection(collection, flattenedType);
    }

    /**
     * Recursively scans a SimpleFeature for SimpleFeature attributes in order to build a
     * "flattened" list of attributes
     *
     * @param attributeDescriptors A List of attribute descriptors, populated recursively
     * @param featureType The feature type to scan
     * @param attrAlias An alias for adding as a prefix to the simple attribute names
     */
    private static void scanAttributeDescriptors(
            List<AttributeDescriptor> attributeDescriptors,
            SimpleFeatureType featureType,
            String attrAlias) {
        List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            AttributeDescriptor ad = descriptors.get(i);
            SimpleFeatureType joinedSchema =
                    (SimpleFeatureType) ad.getUserData().get(ContentDataStore.JOINED_FEATURE_TYPE);
            String name = (attrAlias != null ? attrAlias + "." : "") + ad.getLocalName();
            if (joinedSchema != null) {
                // go forth and harvest feature attribute types
                scanAttributeDescriptors(attributeDescriptors, joinedSchema, name);
            } else {
                // this is a common (non-feature) attribute type
                AttributeTypeBuilder build = new AttributeTypeBuilder();
                build.init(ad);
                AttributeDescriptor descriptor = build.buildDescriptor(name);
                attributeDescriptors.add(descriptor);
            }
        }
    }

    @Override
    public SimpleFeatureType getSchema() {
        return flattenedType;
    }

    public SimpleFeatureIterator features() {
        return new FlatteningFeatureIterator(delegate.features(), flattenedType);
    }

    /** Flattens the features in a streaming fashion */
    class FlatteningFeatureIterator implements SimpleFeatureIterator {

        private SimpleFeatureIterator delegate;

        private SimpleFeatureBuilder builder;

        public FlatteningFeatureIterator(
                SimpleFeatureIterator delegate, SimpleFeatureType flattenedType) {
            this.delegate = delegate;
            this.builder = new SimpleFeatureBuilder(flattenedType);
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature next = delegate.next();
            accumulateAttributes(next);

            return builder.buildFeature(next.getID());
        }

        /**
         * Recursively breaks down SimpleFeatures that may contain other features as attributes to
         * accumulate simple attribute values to a List
         *
         * @param feature A SimpleFeature to harvest attributes
         */
        private void accumulateAttributes(SimpleFeature feature) {
            for (int i = 0; i < feature.getAttributes().size(); i++) {
                Object attr = feature.getAttribute(i);
                if (attr instanceof SimpleFeature) {
                    // go forth and harvest attrubutes
                    accumulateAttributes((SimpleFeature) attr);
                } else {
                    builder.add(attr);
                }
            }
        }
    }
}
