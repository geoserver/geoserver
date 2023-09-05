/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.builders;

import java.util.ArrayList;
import java.util.Collections;
import org.geoserver.schemalessfeatures.type.DynamicComplexType;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.gml3.v3_2.GMLSchema;

/** A builder able to build Dynamic types */
public class DynamicComplexTypeBuilder extends AttributeTypeBuilder {

    DynamicComplexTypeFactory factory;

    public DynamicComplexTypeBuilder(DynamicComplexTypeFactory factory) {
        super(factory);
        this.factory = factory;
    }

    public DynamicFeatureType buildNestedFeatureType() {
        DynamicFeatureType type =
                (DynamicFeatureType)
                        factory.createFeatureType(
                                name(),
                                new ArrayList<>(),
                                null,
                                isAbstract,
                                Collections.emptyList(),
                                GMLSchema.ABSTRACTGMLTYPE_TYPE,
                                null);
        resetTypeState();
        return type;
    }

    public DynamicComplexType buildComplexType() {
        ComplexType type =
                factory.createComplexType(
                        name(),
                        new ArrayList<>(),
                        isIdentifiable,
                        isAbstract,
                        Collections.emptyList(),
                        GMLSchema.ABSTRACTGMLTYPE_TYPE,
                        null);
        resetTypeState();
        return (DynamicComplexType) type;
    }

    public AttributeDescriptor buildDescriptor(String name, AttributeType type, boolean unbounded) {
        minOccurs(0);
        maxOccurs(unbounded ? Integer.MAX_VALUE : 1);
        return super.buildDescriptor(name, type);
    }

    private Name name() {
        return new NameImpl(namespaceURI, name);
    }
}
