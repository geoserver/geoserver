/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.geoserver.schemalessfeatures.type.DynamicComplexType;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.ComplexTypeImpl;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;

/**
 * Factory for a SchemalessPropertyAccessor. The PropertyAccessor produced can handle only
 * SchemalessFeatureType. When evaluating against a FeatureType returns a ComplexType "anyType",
 * while when evaluating against a descriptor returns a descriptor of type "anyType". The
 * PropertyAccessor is able also to evaluate against a Feature of type SchemalessFeatureType a
 * property path that references nested attributes as property and not as property/object
 */
public class SchemalessPropertyAccessorFactory implements PropertyAccessorFactory {

    public static final String NESTED_FEATURE_SUFFIX = "Feature";
    public static final ComplexType ANYTYPE_TYPE =
            new ComplexTypeImpl(
                    new NameImpl("http://www.w3.org/2001/XMLSchema", "anyType"),
                    null,
                    false,
                    true,
                    Collections.emptyList(),
                    null,
                    null);

    @Override
    public PropertyAccessor createPropertyAccessor(
            Class type, String propertyPath, Class target, Hints hints) {

        if (propertyPath == null) return null;

        if (!ComplexAttribute.class.isAssignableFrom(type)
                && !DynamicComplexType.class.isAssignableFrom(type)
                && !AttributeDescriptor.class.isAssignableFrom(type)) return null;

        return new SchemalessFeaturePropertyAccessor();
    }

    static class SchemalessFeaturePropertyAccessor implements PropertyAccessor {
        @Override
        public boolean canHandle(Object object, String xpath, Class target) {
            AttributeType type = null;
            if (object instanceof Attribute) {
                type = ((Attribute) object).getType();
            } else if (object instanceof AttributeType) {
                type = (AttributeType) object;
            } else if (object instanceof AttributeDescriptor)
                type = ((AttributeDescriptor) object).getType();
            return type != null && type instanceof DynamicComplexType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object object, String xpath, Class<T> target)
                throws IllegalArgumentException {
            if (object instanceof ComplexAttribute) {
                String[] pathParts;
                if (xpath.indexOf('/') != -1) pathParts = xpath.split("/");
                else pathParts = xpath.split(":");
                return (T) walkComplexAttribute((ComplexAttribute) object, pathParts);
            } else if (object instanceof DynamicComplexType) {
                return (T) ANYTYPE_TYPE;
            } else if (object instanceof AttributeDescriptor) {
                return (T)
                        new AttributeDescriptorImpl(
                                ANYTYPE_TYPE,
                                new NameImpl(null, "anyType"),
                                0,
                                Integer.MAX_VALUE,
                                true,
                                null);
            } else throw new IllegalArgumentException("Cannot handle the object");
        }

        private Object walkComplexAttribute(ComplexAttribute complexAttribute, String[] path) {
            Object result = null;
            for (int i = 0; i < path.length; i++) {
                String pathPart = path[i];
                result = walkComplexAttribute(complexAttribute, pathPart);
                if (result instanceof ComplexAttribute)
                    complexAttribute = (ComplexAttribute) result;
                else if (result instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> attributes = List.class.cast(result);
                    List<Object> results = walkList(attributes, path, i);
                    if (results.size() == 1) return results.get(0);
                    else return results;
                }
            }
            return result;
        }

        private Object walkComplexAttribute(ComplexAttribute complexAttribute, String pathPart) {
            if (complexAttribute == null) return null;
            Collection<Property> properties = complexAttribute.getProperties(pathPart);
            if (properties == null || properties.isEmpty()) return null;

            Object value;
            if (properties.size() == 1)
                value = extractValue(properties.iterator().next(), pathPart);
            else value = extractValues(properties, pathPart);
            return value;
        }

        private List<Object> walkList(List<Object> attributes, String[] path, int currentIndex) {
            List<Object> results = new ArrayList<>();
            for (Object value : attributes) {
                if (value == null) continue;
                if (!(value instanceof ComplexAttribute)) {
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> values = List.class.cast(value);
                        results.addAll(values);
                    } else results.add(value);
                } else {
                    value =
                            walkComplexAttribute(
                                    (ComplexAttribute) value,
                                    Arrays.copyOfRange(path, currentIndex + 1, path.length));
                    if (value != null) {
                        if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> values = List.class.cast(value);
                            results.addAll(values);
                        } else results.add(value);
                    }
                }
            }
            return results;
        }

        private Object extractValue(Property property, String pathPart) {
            Object value;
            if (property instanceof ComplexAttribute) {
                ComplexAttribute complexProp = (ComplexAttribute) property;
                String featurePath =
                        pathPart.substring(0, 1).toUpperCase()
                                + pathPart.substring(1)
                                + NESTED_FEATURE_SUFFIX;
                value = complexProp.getProperty(featurePath);
            } else {
                value = property.getValue();
            }
            return value;
        }

        private List<Object> extractValues(Collection<Property> properties, String pathPart) {
            List<Object> values = new ArrayList<>();
            for (Property property : properties) {
                values.add(extractValue(property, pathPart));
            }
            return values;
        }

        @Override
        public void set(Object object, String xpath, Object value, Class target)
                throws IllegalAttributeException {
            throw new UnsupportedOperationException("Set is not supported");
        }
    }
}
