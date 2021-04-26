/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures;

import org.geoserver.schemalessfeatures.builders.DynamicComplexTypeBuilder;
import org.geoserver.schemalessfeatures.filter.SchemalessPropertyAccessorFactory;
import org.geoserver.schemalessfeatures.type.DynamicComplexType;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.gml3.v3_2.GMLSchema;
import org.geotools.util.Converters;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * Abstract class for a mapper able to produce Features from a source without relying on the
 * presence of a FeatureType upfront
 *
 * @param <T> the type of the source object used to build a Feature
 */
public abstract class SchemalessFeatureMapper<T> {

    public static final String TYPE_SUFFIX = "Type";

    public static final String PROPERTY_TYPE_SUFFIX = "PropertyType";

    protected AttributeBuilder attributeBuilder;

    protected DynamicComplexTypeBuilder typeBuilder;

    public SchemalessFeatureMapper(
            AttributeBuilder attributeBuilder, DynamicComplexTypeBuilder typeBuilder) {
        this.attributeBuilder = attributeBuilder;
        this.typeBuilder = typeBuilder;
    }

    /**
     * Build a feature from a source object
     *
     * @param sourceObj the source object to be mapped to a feature
     * @return a feature
     */
    public abstract Feature buildFeature(T sourceObj);

    /**
     * build a property descriptor for a nested object, compliant to the GML object-property model
     *
     * @param parentType the parent type to which the new descriptor will belong
     * @param namespaceURI the namespace URI
     * @param attrName the name of the descriptor will be used for both the property and the object
     *     descriptor
     * @param isCollection if the descriptor is unbounded
     * @return the propertyDescriptor with the Object Type and descriptor
     */
    protected PropertyDescriptor buildFullyObjectPropertyModelDescriptor(
            DynamicComplexType parentType,
            String namespaceURI,
            String attrName,
            boolean isCollection) {
        typeBuilder
                .nillable(true)
                .namespaceURI(namespaceURI)
                .name(capitalizeName(attrName) + PROPERTY_TYPE_SUFFIX)
                .superType(GMLSchema.ABSTRACTFEATURETYPE_TYPE);

        DynamicComplexType complexPropertyType = typeBuilder.buildComplexType();

        PropertyDescriptor descriptorProperty =
                typeBuilder.buildDescriptor(attrName, complexPropertyType, isCollection);
        parentType.addPropertyDescriptor(descriptorProperty);

        typeBuilder
                .nillable(true)
                .namespaceURI(namespaceURI)
                .name(capitalizeName(attrName) + TYPE_SUFFIX)
                .superType(GMLSchema.ABSTRACTGMLTYPE_TYPE);
        DynamicComplexType nestedFeatureType = typeBuilder.buildNestedFeatureType();

        PropertyDescriptor nestedFeatureDescriptor =
                typeBuilder.buildDescriptor(
                        capitalizeNameFeature(attrName), nestedFeatureType, isCollection);
        complexPropertyType.addPropertyDescriptor(nestedFeatureDescriptor);
        return descriptorProperty;
    }

    private String capitalizeName(String typeName) {
        return typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
    }

    private String capitalizeNameFeature(String typeName) {
        return capitalizeName(typeName) + SchemalessPropertyAccessorFactory.NESTED_FEATURE_SUFFIX;
    }

    /**
     * Build a simple attribute from a value and its descriptor. The descriptor will be added to the
     * parentType
     *
     * @param namespaceURI the namespaceURI
     * @param attrName the name of the attribute
     * @param value the value
     * @param parentType the parentType to which append the descriptor of the attribute
     * @param isCollection true if the attribute being encode can contain multiple values false
     *     otherwise
     * @return the simple Attribute
     */
    protected Attribute buildSimpleAttribute(
            String namespaceURI,
            String attrName,
            Object value,
            DynamicComplexType parentType,
            boolean isCollection) {
        Name name = new NameImpl(namespaceURI, attrName);
        PropertyDescriptor attrDescriptor = parentType.getDescriptor(name);
        boolean shouldRemove =
                attrDescriptor != null
                        && attrDescriptor.getType().getBinding().equals(Object.class);
        if (shouldRemove) parentType.removePropertyDescriptor(attrDescriptor);
        if (attrDescriptor == null || shouldRemove) {
            typeBuilder
                    .binding(value.getClass())
                    .name(attrName)
                    .namespaceURI(namespaceURI)
                    .maxOccurs(isCollection ? Integer.MAX_VALUE : 1)
                    .minOccurs(0);
            AttributeType attrType = typeBuilder.buildType();
            attrDescriptor = typeBuilder.buildDescriptor(attrType.getName(), attrType);
            if (parentType instanceof DynamicComplexType) {
                parentType.addPropertyDescriptor(attrDescriptor);
            }
        }
        Class<?> binding = attrDescriptor.getType().getBinding();
        if (!binding.equals(Object.class) && !value.getClass().equals(binding)) {
            value = Converters.convert(value, attrDescriptor.getType().getBinding());
        }
        attributeBuilder.setDescriptor((AttributeDescriptor) attrDescriptor);
        return attributeBuilder.buildSimple(null, value);
    }

    protected Attribute buildSimpleAttribute(
            String namespaceURI, String attrName, Object value, DynamicComplexType parentType) {
        return buildSimpleAttribute(namespaceURI, attrName, value, parentType, false);
    }

    /**
     * Builds a simple attribute based on a null value.
     *
     * @param namespaceURI the namespaceURI.
     * @param attrName the name of the attribute.
     * @param parentType the parent ComplexType.
     * @return an Attribute holding a null value.
     */
    protected Attribute buildNullAttribute(
            String namespaceURI, String attrName, DynamicComplexType parentType) {
        typeBuilder
                .binding(Object.class)
                .name(attrName)
                .namespaceURI(namespaceURI)
                .maxOccurs(1)
                .minOccurs(0)
                .nillable(true);
        AttributeType attrType = typeBuilder.buildType();
        PropertyDescriptor attrDescriptor =
                typeBuilder.buildDescriptor(attrType.getName(), attrType);
        if (parentType instanceof DynamicComplexType) {
            parentType.addPropertyDescriptor(attrDescriptor);
        }
        attributeBuilder.setDescriptor((AttributeDescriptor) attrDescriptor);
        return attributeBuilder.buildSimple(null, null);
    }
}
