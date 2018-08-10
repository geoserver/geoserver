/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.translate.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.boundlessgeo.gsr.model.feature.*;
import com.boundlessgeo.gsr.model.geometry.*;
import com.boundlessgeo.gsr.translate.geometry.AbstractGeometryEncoder;
import com.boundlessgeo.gsr.translate.geometry.GeometryEncoder;
import net.sf.json.JSONObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

public class FeatureEncoder {

    public static final String OBJECTID_FIELD_NAME = "objectid";

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
            attributes.put(objectIdFieldName, toGSRObjectId(feature.getIdentifier().getID()));
        }

        return attributes;
    }

    public static Feature fromJson(JSONObject json) {
        Geometry geometry = GeometryEncoder.jsonToGeometry(json.getJSONObject("geometry"));
        Map<String, Object> attributes = new HashMap<>();
        JSONObject jsonAttributes = json.getJSONObject("attributes");

        for (Object key : jsonAttributes.keySet()) {
            attributes.put((String) key, jsonAttributes.get(key));
        }

        return new Feature(geometry, attributes);
    }

    public static Feature feature(org.opengis.feature.Feature feature, boolean returnGeometry,
                                  SpatialReference spatialReference) {
        return feature(feature, returnGeometry, spatialReference, FeatureEncoder.OBJECTID_FIELD_NAME);
    }

    public static Feature feature(org.opengis.feature.Feature feature, boolean returnGeometry,
                                  SpatialReference spatialReference, String objectIdFieldName) {
        return feature(feature, returnGeometry, spatialReference, objectIdFieldName, new GeometryEncoder());
    }
    public static Feature feature(org.opengis.feature.Feature feature, boolean returnGeometry,
                                  SpatialReference spatialReference, String objectIdFieldName, AbstractGeometryEncoder geometryEncoder) {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        Map<String, Object> attributes = FeatureEncoder.attributeList(feature, objectIdFieldName);
        if (returnGeometry) {
            return new Feature(geometryEncoder.toRepresentation(
                    (com.vividsolutions.jts.geom.Geometry) geometryAttribute.getValue(),spatialReference), attributes);
        } else {
            return new Feature(null, attributes);
        }
    }

    public static Field field(PropertyDescriptor field, Boolean featureIsEditable) {
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
            editable = featureIsEditable;
            break;
        case GEOMETRY:
            fieldLength = null;
            editable = featureIsEditable;
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
                objectIds.add(toGSRObjectId(feature.getIdentifier().getID()));
            }
        }
        return new FeatureIdSet(OBJECTID_FIELD_NAME, objectIds.stream().mapToLong(i -> i).toArray());
    }

    public final static Pattern FEATURE_ID_PATTERN = Pattern.compile("(^(?:.*\\.)?)(\\p{Digit}+)$");

    /**
     * Converts a GeoTools FeatureId of the form $NAME.$ID to a ESRI-compatible long value by removing the $NAME prefix
     * using {@link FeatureEncoder#FEATURE_ID_PATTERN}
     *
     * @param featureId
     * @return
     */
    public static Long toGSRObjectId(String featureId) {
        Matcher matcher = FEATURE_ID_PATTERN.matcher(featureId);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(2));
        } else {
            return (long) featureId.hashCode();
        }
    }

    /**
     * Converts a long id generated using {@link #toGSRObjectId(String)} back to a GeoTools FeatureId.
     *
     * @param objectId the generated id
     * @param idPrefix the prefix to prepend
     * @return
     */
    public static String toGeotoolsFeatureId(Long objectId, String idPrefix) {
        return idPrefix+objectId.toString();
    }

    /**
     * Converts a long id generated using {@link #toGSRObjectId(String)} back to a GeoTools FeatureId.
     *
     * @param objectId the generated id
     * @param targetFeature The target featuretype of the id, used to calculate the prefix
     * @return
     * @throws IOException
     */
    public static String toGeotoolsFeatureId(Long objectId, FeatureTypeInfo targetFeature) throws IOException {
        return toGeotoolsFeatureId(objectId, calculateFeatureIdPrefix(targetFeature));

    }

    /**
     * Calculates the geotools id prefix,
     * @param targetFeature
     * @return
     * @throws IOException
     */
    public static String calculateFeatureIdPrefix(FeatureTypeInfo targetFeature) throws IOException {
        org.opengis.feature.Feature sampleFeature = null;
        String featureIdPrefix = "";
        FeatureIterator i = targetFeature.getFeatureSource(null, null).getFeatures().features();
        if (i.hasNext()) {
            sampleFeature = i.next();
            String fid = sampleFeature.getIdentifier().getID();

            Matcher matcher = FeatureEncoder.FEATURE_ID_PATTERN.matcher(fid);
            if (matcher.matches()) {
                featureIdPrefix = matcher.group(1);
            }
        }
        i.close();
        return featureIdPrefix;
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
