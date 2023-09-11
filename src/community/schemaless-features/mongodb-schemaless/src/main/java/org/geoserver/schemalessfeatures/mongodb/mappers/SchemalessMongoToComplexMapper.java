/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.mongodb.mappers;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.geoserver.schemalessfeatures.SchemalessFeatureMapper;
import org.geoserver.schemalessfeatures.builders.DynamicComplexTypeBuilder;
import org.geoserver.schemalessfeatures.builders.DynamicComplexTypeFactory;
import org.geoserver.schemalessfeatures.mongodb.MongoSchemalessUtils;
import org.geoserver.schemalessfeatures.type.DynamicComplexType;
import org.geoserver.schemalessfeatures.type.DynamicFeatureType;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.ComplexAttribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.mongodb.MongoGeometryBuilder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.ValidatingFeatureFactoryImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;

/**
 * A MongoDB implementation of SchemalessMapper. This class is responsible to build a Feature from a
 * DBObject source, and to create/update the AttrbiuteType along with the feature building process.
 */
public class SchemalessMongoToComplexMapper extends SchemalessFeatureMapper<DBObject> {

    private MongoGeometryBuilder geomBuilder;

    private DynamicFeatureType type;
    private final CoordinateReferenceSystem crs;
    private final boolean reproject;
    private MathTransform transform = null;

    public SchemalessMongoToComplexMapper(DynamicFeatureType type, CoordinateReferenceSystem crs) {
        super(
                new AttributeBuilder(new ValidatingFeatureFactoryImpl()),
                new DynamicComplexTypeBuilder(new DynamicComplexTypeFactory()));
        this.geomBuilder = new MongoGeometryBuilder();
        this.type = type;
        this.crs = crs;
        this.reproject = crs != null && !CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84);
        if (reproject) {
            try {
                this.transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Feature buildFeature(DBObject rootDBO) {
        attributeBuilder.init();
        List<Property> attributes = getNestedAttributes(rootDBO, type);
        ComplexFeatureBuilder featureBuilder = new ComplexFeatureBuilder(type);
        GeometryAttribute geometryAttribute = null;
        for (Property p : attributes) {
            if (p instanceof GeometryAttribute) {
                GeometryAttribute geom = (GeometryAttribute) p;
                handleGeometryReprojection(geom);
                if (p.getName().equals(type.getGeometryDescriptor().getName())) {
                    geometryAttribute = geom;
                    featureBuilder.append(p.getName(), geometryAttribute);
                }
            } else {
                featureBuilder.append(p.getName(), p);
            }
        }
        Feature f = featureBuilder.buildFeature(rootDBO.get("_id").toString());
        f.setDefaultGeometryProperty(geometryAttribute);
        return f;
    }

    private void handleGeometryReprojection(GeometryAttribute geom) {
        if (crs == null) return;
        Geometry geomValue = (Geometry) geom.getValue();
        // if crs is not wgs84, we need to reproject the geometry
        if (reproject) {
            try {
                geomValue = JTS.transform(geomValue, transform);
                JTS.setCRS(geomValue, crs);
                geom.setValue(geomValue);
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Property> getNestedAttributes(DBObject rootDBO, DynamicComplexType parentType) {
        Set<String> keys = rootDBO.keySet();
        String namespaceURI = type.getName().getNamespaceURI();
        List<Property> attributes = new ArrayList<>();
        for (String key : keys) {
            Object value = rootDBO.get(key);
            if (value == null) {
                attributes.add(buildNullAttribute(namespaceURI, key, parentType));
            } else if (value instanceof BasicDBList) {
                BasicDBList list = (BasicDBList) value;
                attributes.addAll(
                        buildComplexAttributesUnbounded(namespaceURI, key, list, parentType));
            } else if (value instanceof DBObject) {
                DBObject dbObj = (DBObject) value;
                if (MongoSchemalessUtils.isGeometry(dbObj)) {
                    Geometry geom = geomBuilder.toGeometry(dbObj);
                    GeometryAttribute geometryAttribute =
                            buildGeometryAttribute(geom, namespaceURI, key, parentType);
                    attributes.add(geometryAttribute);
                    continue;
                }
                attributes.add(
                        buildComplexAttribute(
                                namespaceURI, key, (DBObject) value, parentType, false));
            } else {
                attributes.add(buildSimpleAttribute(namespaceURI, key, value, parentType));
            }
        }
        return attributes;
    }

    private Attribute buildComplexAttribute(
            String namespaceURI,
            String attrName,
            DBObject dbobject,
            DynamicComplexType parentType,
            boolean isCollection) {
        PropertyDescriptor descriptorProperty =
                parentType.getDescriptor(new NameImpl(namespaceURI, attrName));

        // if the value being mapped was null for a previous feature
        // we might have it as a simple attribute type holding a null value
        // in that case the descriptor is rebuilt.
        boolean notAComplexType =
                descriptorProperty != null
                        && !(descriptorProperty.getType() instanceof DynamicComplexType);
        if (descriptorProperty == null || notAComplexType)
            descriptorProperty =
                    buildFullyObjectPropertyModelDescriptor(
                            parentType, namespaceURI, attrName, isCollection);
        ComplexType complexTypeProperty = (ComplexType) descriptorProperty.getType();
        PropertyDescriptor nestedFeatureDescriptor = extractFeatureDescriptor(descriptorProperty);
        DynamicComplexType nestedFeatureType =
                (DynamicComplexType) nestedFeatureDescriptor.getType();
        ComplexFeatureBuilder featureBuilder =
                new ComplexFeatureBuilder((AttributeDescriptor) nestedFeatureDescriptor);
        List<Property> attributes = getNestedAttributes(dbobject, nestedFeatureType);
        for (Property p : attributes) featureBuilder.append(p.getName(), p);
        Feature f = featureBuilder.buildFeature(null);
        ComplexAttribute propertyAttribute =
                attributeBuilder.createComplexAttribute(
                        Arrays.asList(f),
                        complexTypeProperty,
                        (AttributeDescriptor) descriptorProperty,
                        null);
        return propertyAttribute;
    }

    private List<Property> buildComplexAttributesUnbounded(
            String namespaceURI,
            String attrName,
            BasicDBList value,
            DynamicComplexType parentType) {
        List<Property> attributes = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            Object obj = value.get(i);
            if (obj instanceof DBObject) {
                Attribute attribute =
                        buildComplexAttribute(
                                namespaceURI, attrName, (DBObject) obj, parentType, true);
                attributes.add(attribute);
            } else if (obj != null) {
                Attribute attribute =
                        buildSimpleAttribute(namespaceURI, attrName, obj, parentType, true);
                attributes.add(attribute);
            } else if (obj == null) {
                Attribute attribute = buildNullAttribute(namespaceURI, attrName, parentType);
                attributes.add(attribute);
            }
        }
        return attributes;
    }

    private GeometryAttribute buildGeometryAttribute(
            Geometry geom, String namespaceURI, String name, DynamicComplexType parentType) {
        AttributeDescriptor descriptor =
                (AttributeDescriptor) parentType.getDescriptor(new NameImpl(namespaceURI, name));
        if (descriptor == null) {
            typeBuilder
                    .binding(geom.getClass())
                    .name(name)
                    .namespaceURI(namespaceURI)
                    .maxOccurs(1)
                    .minOccurs(0);
            GeometryType attrType = typeBuilder.buildGeometryType();
            descriptor = typeBuilder.buildDescriptor(attrType.getName(), attrType);
            GeometryDescriptor geometryDescriptor = type.getGeometryDescriptor();
            if (geometryDescriptor == null) {
                type.setGeometryDescriptor((GeometryDescriptor) descriptor);
                type.addPropertyDescriptor(descriptor);
            } else {
                parentType.addPropertyDescriptor(descriptor);
            }
        }
        attributeBuilder.setDescriptor(descriptor);
        return (GeometryAttribute) attributeBuilder.buildSimple(null, geom);
    }

    // Get the nested feature descriptor from its property descriptor container
    private PropertyDescriptor extractFeatureDescriptor(PropertyDescriptor descriptorProperty) {
        if (descriptorProperty == null) return null;
        ComplexType typeFeature = (ComplexType) descriptorProperty.getType();
        return typeFeature.getDescriptors().iterator().next();
    }
}
