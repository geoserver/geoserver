/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.FeatureFactory;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;

/**
 * A {@link org.geotools.feature.ComplexFeatureBuilder} that allows to append attributes to a
 * feature using their simple name and their value.
 */
class ComplexFeatureBuilder extends org.geotools.feature.ComplexFeatureBuilder {

    private final AttributeBuilder ab;

    public ComplexFeatureBuilder(FeatureType featureType) {
        this(featureType, CommonFactoryFinder.getFeatureFactory(null));
    }

    public ComplexFeatureBuilder(FeatureType featureType, FeatureFactory factory) {
        super(featureType, factory);
        this.ab = new AttributeBuilder(factory);
    }

    /** Appends a new attribute to the feature given its simple name and its value */
    public void append(String name, Object value) {
        AttributeDescriptor ad = (AttributeDescriptor) featureType.getDescriptor(name);
        ab.setDescriptor(ad);
        Attribute attribute = ab.buildSimple(null, value);
        append(ad.getName(), attribute);
    }
}
