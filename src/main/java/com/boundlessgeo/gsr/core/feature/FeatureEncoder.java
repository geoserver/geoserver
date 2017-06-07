/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package com.boundlessgeo.gsr.core.feature;

import com.boundlessgeo.gsr.core.geometry.GeometryEncoder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureEncoder {
    private FeatureEncoder() {
        throw new RuntimeException("Feature encoder has only static methods, no need to instantiate it.");
    }

    /**
     * Get an {@link AttributeList} from a {@link org.opengis.feature.Feature}
     *
     * @param feature
     * @return
     */
    public static AttributeList attributeList(org.opengis.feature.Feature feature) {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        AttributeList attributes = new AttributeList(new ArrayList<Attribute>());
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
                attributes.add(new Attribute(prop.getName().getLocalPart(), value));
            }
        }
        attributes.add(new Attribute("objectid", adaptId(feature.getIdentifier().getID())));

        return attributes;
    }

    public static Feature feature(org.opengis.feature.Feature feature, boolean returnGeometry) {
        GeometryAttribute geometryAttribute = feature.getDefaultGeometryProperty();
        if (returnGeometry) {
            return new Feature(GeometryEncoder.toRepresentation((com.vividsolutions.jts.geom.Geometry) geometryAttribute.getValue()), FeatureEncoder.attributeList(feature));
        } else {
            return new Feature(null, FeatureEncoder.attributeList(feature));
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
        return new Field(field.getName().getLocalPart(), fieldType, field.getName().toString(), fieldLength, editable, field.isNillable());
    }

    public static <T extends FeatureType, F extends org.opengis.feature.Feature> FeatureIdSet objectIds(FeatureCollection<T, F> features) {

        // TODO: Advertise "real" identifier property

        FeatureIterator<F> iterator = features.features();
        List<Long> objectIds = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                F feature = iterator.next();
                objectIds.add(adaptId(feature.getIdentifier().getID()));
            }
        } finally {
            iterator.close();
        }
        return new FeatureIdSet("objectId", objectIds.stream().mapToLong(i->i).toArray());
    }

    private final static Pattern featureIDPattern = Pattern.compile("^(?:.*\\.)?(\\p{Digit}+)$");

    private static Long adaptId(String featureId) {
        Matcher matcher = featureIDPattern.matcher(featureId);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(1));
        } else {
            return Long.valueOf(featureId.hashCode());
        }
    }
}
