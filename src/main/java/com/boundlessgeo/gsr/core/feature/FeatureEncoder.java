/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import com.boundlessgeo.gsr.core.geometry.GeometryEncoder;
import com.boundlessgeo.gsr.core.geometry.SpatialReference;

public class FeatureEncoder {
    private FeatureEncoder() {
        throw new RuntimeException("Feature encoder has only static methods, no need to instantiate it.");
    }

    /**
     * Get an {@link AttributeList} from a {@link org.opengis.feature.Feature}
     *
     * @param feature
     * @param objectIdFieldName
     * @return the list of feature attributes
     */
    public static Map<String, Object> attributeList(org.opengis.feature.Feature feature, String objectIdFieldName) {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        Map<String, Object> attributes = new HashMap<>();
        for (Property prop : feature.getProperties()) {
            if (prop.getValue() != null && (geometryAttribute == null || !prop.getName()
                .equals(geometryAttribute.getName()))) {
                final Object value;
                if (prop.getValue() instanceof java.util.Date) {
                    value = ((java.util.Date) prop.getValue()).getTime();
                } else if (prop.getValue() instanceof java.lang.Boolean) {
                    value = ((Boolean) prop.getValue()) ? Integer.valueOf(1) : Integer.valueOf(0);
                } else {
                    value = prop.getValue().toString();
                }
                attributes.put(prop.getName().getLocalPart(), value);
            }
        }

        if (objectIdFieldName != null) {
            attributes.put(objectIdFieldName, adaptId(feature.getIdentifier().getID()));
        }

        return attributes;
    }

    public static Feature feature(org.opengis.feature.Feature feature, boolean returnGeometry,
        SpatialReference spatialReference) {
        return feature(feature, returnGeometry, spatialReference, null);
    }

    public static Feature feature(org.opengis.feature.Feature feature, boolean returnGeometry,
        SpatialReference spatialReference, String objectIdFieldName) {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        Map<String, Object> attributes = FeatureEncoder.attributeList(feature, objectIdFieldName);
        if (returnGeometry) {
            return new Feature(GeometryEncoder
                .toRepresentation((com.vividsolutions.jts.geom.Geometry) geometryAttribute.getValue(),
                    spatialReference), attributes);
        } else {
            return new Feature(null, attributes);
        }
    }

    public static Field field(PropertyDescriptor field) {
        // Similar to LayerListResource encodeSchemaProperties
        // Similar to FeatureEncoder descriptorToJson.

        FieldTypeEnum fieldType = FieldTypeEnum.forClass(field.getType().getBinding());
        Integer fieldLength = FeatureTypes.getFieldLength(field);
        Boolean editable;

        // String, Date, GlobalID, GUID and XML
        switch (fieldType) {
        case STRING:
        case DATE:
        case GUID:
        case GLOBAL_ID:
        case XML:
            fieldLength = fieldLength == -1 ? 4000 : fieldLength;
            editable = false;
            break;
        default:
            // length and editable are optional
            fieldLength = null;
            editable = null;
        }
        return new Field(field.getName().getLocalPart(), fieldType, field.getName().toString(), fieldLength, editable,
            field.isNillable());
    }

    public static <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureIdSet objectIds(
        FeatureCollection<T, F> features) {

        // TODO: Advertise "real" identifier property

        List<Long> objectIds = new ArrayList<>();
        try (FeatureIterator<F> iterator = features.features()) {
            while (iterator.hasNext()) {
                F feature = iterator.next();
                objectIds.add(adaptId(feature.getIdentifier().getID()));
            }
        }
        return new FeatureIdSet("objectId", objectIds.stream().mapToLong(i -> i).toArray());
    }

    private final static Pattern featureIDPattern = Pattern.compile("^(?:.*\\.)?(\\p{Digit}+)$");

    private static Long adaptId(String featureId) {
        Matcher matcher = featureIDPattern.matcher(featureId);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(1));
        } else {
            return (long) featureId.hashCode();
        }
    }

    /**
     * ESRI JS relies heavily on the object ID field, whereas in GeoServer this concept is a little vaguer.
     *
     * @param objectIdFieldName
     * @return
     */
    public static Field syntheticObjectIdField(String objectIdFieldName) {
        Field idField = new Field(objectIdFieldName, FieldTypeEnum.OID, objectIdFieldName);
        return idField;
    }
}
