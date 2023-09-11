/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.builders;

import java.util.Collection;
import java.util.List;
import org.geoserver.schemalessfeatures.type.DynamicComplexTypeImpl;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.util.InternationalString;
import org.geotools.feature.type.FeatureTypeFactoryImpl;

/** A FeatureTypeFactory able to produce Dynamic Type */
public class DynamicComplexTypeFactory extends FeatureTypeFactoryImpl {

    @Override
    public FeatureType createFeatureType(
            Name name,
            Collection<PropertyDescriptor> schema,
            GeometryDescriptor defaultGeometry,
            boolean isAbstract,
            List<Filter> restrictions,
            AttributeType superType,
            InternationalString description) {
        return new DynamicFeatureType(
                name, schema, defaultGeometry, isAbstract, restrictions, superType, description);
    }

    @Override
    public ComplexType createComplexType(
            Name name,
            Collection<PropertyDescriptor> schema,
            boolean isIdentifiable,
            boolean isAbstract,
            List<Filter> restrictions,
            AttributeType superType,
            InternationalString description) {
        return new DynamicComplexTypeImpl(
                name, schema, isIdentifiable, isAbstract, restrictions, superType, description);
    }
}
