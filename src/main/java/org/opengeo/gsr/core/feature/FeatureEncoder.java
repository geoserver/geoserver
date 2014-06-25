/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package org.opengeo.gsr.core.feature;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.util.JSONBuilder;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.opengeo.gsr.core.geometry.GeometryEncoder;
import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
import org.opengeo.gsr.core.geometry.SpatialReference;
import org.opengeo.gsr.core.geometry.SpatialReferenceEncoder;
import org.opengeo.gsr.core.geometry.SpatialReferences;
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
    
    public static
    <T extends FeatureType, F extends org.opengis.feature.Feature>
    void featuresToJson(FeatureCollection<T, F> collection, JSONBuilder json, boolean returnGeometry)
    throws IOException
    {
        FeatureIterator<F> iterator = collection.features();
        
        T schema = collection.getSchema();
        json.object()
          .key("objectIdFieldName").value("objectid")
          .key("globalIdFieldName").value("");
        
        if (returnGeometry) {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            if (geometryDescriptor == null) throw new RuntimeException("No geometry descriptor for type " + schema + "; " + schema.getDescriptors());
            GeometryType geometryType = geometryDescriptor.getType();
            if (geometryType == null) throw new RuntimeException("No geometry type for type " + schema);
            Class<?> binding = geometryType.getBinding();
            if (binding == null) throw new RuntimeException("No binding for geometry type " + schema);
            GeometryTypeEnum geometryTypeEnum = GeometryTypeEnum.forJTSClass(binding);
            json.key("geometryType").value(geometryTypeEnum.getGeometryType());
        }
        
        if (schema.getCoordinateReferenceSystem() != null) {
            try {
                SpatialReference sr = SpatialReferences.fromCRS(schema.getCoordinateReferenceSystem());
                json.key("spatialReference");
                SpatialReferenceEncoder.toJson(sr, json);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }
        
        json.key("fields").array();
        for (PropertyDescriptor desc : schema.getDescriptors()) {
            if (schema.getGeometryDescriptor() != null && !desc.getName().equals(schema.getGeometryDescriptor().getName())) {
                descriptorToJson(desc, json);
            }
        }
        json.endArray();
        
        try {
            json.key("features");
            json.array();
            while (iterator.hasNext()) {
                F feature = iterator.next();
                featureToJson(feature, json, returnGeometry);
            }
            json.endArray();
        } finally {
            iterator.close();
        }
        json.endObject();
    }

    public static void featureToJson(org.opengis.feature.Feature feature, JSONBuilder json, boolean returnGeometry) {
        GeometryAttribute geometry = feature.getDefaultGeometryProperty();
        json.object();
        if (returnGeometry) {
            json.key("geometry");
            GeometryEncoder.toJson((com.vividsolutions.jts.geom.Geometry)geometry.getValue(), json);
        }
        json.key("attributes");
        json.object();
        
        json.key("objectid").value(adaptId(feature.getIdentifier().getID()));

        for (Property prop : feature.getProperties()) {
            if (geometry == null || !prop.getName().equals(geometry.getName())) {
                final Object value;
                if (prop.getValue() instanceof java.util.Date) {
                    value = ((java.util.Date) prop.getValue()).getTime();
                } else if (prop.getValue() instanceof java.lang.Boolean) {
                    value = ((Boolean) prop.getValue()) ? Integer.valueOf(1) : Integer.valueOf(0);
                } else {
                    value = prop.getValue();
                }
                json.key(prop.getName().getLocalPart()).value(value);
            }
        }
        
        json.endObject();
        json.endObject();
    }
    
    public static void descriptorToJson(PropertyDescriptor field, JSONBuilder json) {
        // Similar to LayerListResource encodeencodeSchemaProperties
        // Similar to FeatureEncoder descriptorToJson. 
        json.object();			
        json.key("name").value(field.getName().getLocalPart());

        FieldTypeEnum type = FieldTypeEnum.forClass(field.getType().getBinding());						
        json.key("type").value(type.getFieldType());
        json.key("alias").value(field.getName().toString());

        int length = FeatureTypes.getFieldLength(field);

        // String, Date, GlobalID, GUID and XML
        switch (type) {
            case STRING:
            case DATE:
            case GUID:
            case GLOBAL_ID:
            case XML:
                json.key("length").value(length == -1 ? 4000 : length);
                json.key("editable").value("false");
                break;				
            default:
                // length and editable are optional
        }
        json.key("nullable").value(field.isNillable() ? "true" : "false");
        json.key("domain").value(null);		

        json.endObject();
    }

    public static <T extends FeatureType, F extends Feature>
    void featureIdSetToJson(FeatureCollection<T, F> features, JSONBuilder json)
    {
        json.object();
        json.key("objectIdFieldName");
        json.value("objectid"); // TODO: Advertise "real" identifier property

        FeatureIterator<F> iterator = features.features();
        try {
            json.key("objectIds");
            json.array();
            while (iterator.hasNext()) {
                F feature = iterator.next();
                json.value(adaptId(feature.getIdentifier().getID()));
            }
            json.endArray();
        } finally {
            iterator.close();
        }

        json.endObject();
    }
    
    private final static Pattern featureIDPattern = Pattern.compile("^(?:.*\\.)?(\\p{Digit}+)$");
    private static Object adaptId(String featureId) {
        Matcher matcher = featureIDPattern.matcher(featureId);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return Long.valueOf(featureId.hashCode());
        }
    }
}
