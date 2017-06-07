/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package com.boundlessgeo.gsr.core.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.boundlessgeo.gsr.core.GSRModel;
import net.sf.json.util.JSONBuilder;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import com.boundlessgeo.gsr.core.geometry.GeometryEncoder;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeEnum;
import com.boundlessgeo.gsr.core.geometry.SpatialReference;
import com.boundlessgeo.gsr.core.geometry.SpatialReferenceEncoder;
import com.boundlessgeo.gsr.core.geometry.SpatialReferences;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;

public class FeatureEncoder {
    private FeatureEncoder() {
        throw new RuntimeException("Feature encoder has only static methods, no need to instantiate it.");
    }

    public static class Features implements GSRModel {

        public final String objectIdFieldName = "objectid";
        public final String globalIdFieldName = "";
        public final String geometryType;
        public final SpatialReference spatialReference;

        public final ArrayList<Descriptor> fields = new ArrayList<>();
        public final ArrayList<Feature> features = new ArrayList<>();

        public <T extends FeatureType, F extends org.opengis.feature.Feature> Features(FeatureCollection<T, F> collection, boolean returnGeometry) throws IOException {
            FeatureIterator<F> iterator = collection.features();

            T schema = collection.getSchema();

            if (returnGeometry) {
                GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
                if (geometryDescriptor == null) {
                    throw new RuntimeException("No geometry descriptor for type " + schema + "; " + schema.getDescriptors());
                }
                GeometryType geometryType = geometryDescriptor.getType();
                if (geometryType == null) {
                    throw new RuntimeException("No geometry type for type " + schema);
                }
                Class<?> binding = geometryType.getBinding();
                if (binding == null) {
                    throw new RuntimeException("No binding for geometry type " + schema);
                }
                GeometryTypeEnum geometryTypeEnum = GeometryTypeEnum.forJTSClass(binding);
                this.geometryType = geometryTypeEnum.getGeometryType();
            } else {
                this.geometryType = null;
            }

            if (schema.getCoordinateReferenceSystem() != null) {
                try {
                    spatialReference = SpatialReferences.fromCRS(schema.getCoordinateReferenceSystem());
                    //SpatialReferenceEncoder.toJson(sr, json);
                } catch (FactoryException e) {
                    throw new RuntimeException(e);
                }
            } else {
                spatialReference = null;
            }

            for (PropertyDescriptor desc : schema.getDescriptors()) {
                if (schema.getGeometryDescriptor() != null && !desc.getName().equals(schema.getGeometryDescriptor().getName())) {
                    fields.add(new Descriptor(desc));
                }
            }

            try {
                while (iterator.hasNext()) {
                    features.add(new Feature(iterator.next(), returnGeometry));
                }
            } finally {
                iterator.close();
            }
        }
    }

    //TODO - Consolidate these static inner classes with the existing outer classes Feature etc.
    public static class Feature implements GSRModel {
        public final Map<String, Object> attributes = new HashMap<>();
        public final Object geometry;

        public Feature(org.opengis.feature.Feature feature, boolean returnGeometry) {

            GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
            if(returnGeometry) {
                geometry = GeometryEncoder.toRepresentation((com.vividsolutions.jts.geom.Geometry) geometryAttribute.getValue());
            } else {
                geometry = null;
            }
            for(Property prop :feature.getProperties()) {
                if (geometryAttribute == null || !prop.getName().equals(geometryAttribute.getName())) {
                    final Object value;
                    if (prop.getValue() instanceof java.util.Date) {
                        value = ((java.util.Date) prop.getValue()).getTime();
                    } else if (prop.getValue() instanceof java.lang.Boolean) {
                        value = ((Boolean) prop.getValue()) ? Integer.valueOf(1) : Integer.valueOf(0);
                    } else {
                        value = prop.getValue();
                    }
                    attributes.put(prop.getName().getLocalPart(), value);
                }
            }
            attributes.put("objectid", adaptId(feature.getIdentifier().getID()));
        }
    }

    public static class Descriptor implements GSRModel {

        public final String name;
        public final String type;
        public final String alias;
        public final Integer length;
        public final Boolean editable;
        public final Boolean nullable;
        public final String domain = null;

        public Descriptor(String name, String type, String alias, Integer length, Boolean editable, Boolean nullable) {
            this.name = name;
            this.type = type;
            this.alias = alias;
            this.length = length;
            this.editable = editable;
            this.nullable = nullable;
        }

        public Descriptor(PropertyDescriptor field) {
            // Similar to LayerListResource encodeencodeSchemaProperties
            // Similar to FeatureEncoder descriptorToJson.
            name = field.getName().getLocalPart();

            FieldTypeEnum fieldType = FieldTypeEnum.forClass(field.getType().getBinding());
            type = fieldType.getFieldType();
            alias = field.getName().toString();

            int fieldLength = FeatureTypes.getFieldLength(field);

            // String, Date, GlobalID, GUID and XML
            switch (fieldType) {
                case STRING:
                case DATE:
                case GUID:
                case GLOBAL_ID:
                case XML:
                    length = fieldLength == -1 ? 4000 : fieldLength;
                    editable = false;
                    break;
                default:
                    // length and editable are optional
                    length = null;
                    editable = null;
            }
            nullable = field.isNillable();
        }
    }

    public static class FeatureIdSet implements GSRModel {

        // TODO: Advertise "real" identifier property
        public final String objectIdFieldName = "objectid";
        public final ArrayList<Object> objectIds = new ArrayList<>();

        public <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureIdSet(FeatureCollection<T, F> features) {

            FeatureIterator<F> iterator = features.features();
            try {
                while (iterator.hasNext()) {
                    F feature = iterator.next();
                    objectIds.add(adaptId(feature.getIdentifier().getID()));
                }
            } finally {
                iterator.close();
            }
        }
    }

    private final static Pattern featureIDPattern = Pattern.compile("^(?:.*\\.)?(\\p{Digit}+)$");

    private static Object adaptId(String featureId) {
        Matcher matcher = featureIDPattern.matcher(featureId);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return featureId.hashCode();
        }
    }
}
