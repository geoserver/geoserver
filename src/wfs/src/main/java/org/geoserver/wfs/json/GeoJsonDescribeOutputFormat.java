/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat;
import org.geoserver.wfs.response.GeoJSONBuilder;
import org.geotools.feature.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A DescribeFeatureType output format that generates a JSON schema instead of a XML one
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class GeoJsonDescribeOutputFormat extends WFSDescribeFeatureTypeOutputFormat {

    static final Logger LOGGER = Logging.getLogger(GeoJsonDescribeOutputFormat.class);

    public GeoJsonDescribeOutputFormat(GeoServer gs, final String mime) {
        super(gs, mime);
    }

    @Override
    protected void write(FeatureTypeInfo[] featureTypeInfos, OutputStream output,
            Operation describeFeatureType) throws IOException {

        final boolean jsonp = JSONType.isJsonpMimeType(getMimeType(featureTypeInfos, describeFeatureType));

        // prepare to write out
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(output, gs.getSettings()
                .getCharset()));

        // jsonp?
        if (jsonp) {
            outWriter.write(getCallbackFunction() + "(");
        }

        // starting with JSon
        GeoJSONBuilder jw = new GeoJSONBuilder(outWriter);

        // in general one can describe more than one feature type
        jw.array();
        for (FeatureTypeInfo ft : featureTypeInfos) {
            jw.object();
            jw.key("name").value(ft.getNamespace().getPrefix() + ":" + ft.getName());
            jw.key("description").value(ft.getTitle());
            jw.key("type").value("object");
            jw.key("extends").value("Feature");
            SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
            jw.key("properties").object();
            if (schema.getGeometryDescriptor() != null) {
                describeProperty("geometry", schema.getGeometryDescriptor(), jw);
            }

            if (schema.getAttributeDescriptors().size() > 0) {
                jw.key("properties").object();
                jw.key("type").value("object");
                jw.key("properties").object();
                for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                    if (ad == schema.getGeometryDescriptor()) {
                        // this one we already described
                        continue;
                    }

                    describeProperty(ad.getLocalName(), ad, jw);
                }
                jw.endObject();
                jw.endObject(); // end of the "properties" sub-object description
            }
            jw.endObject(); // end of the main properties
            jw.endObject(); // end of the feature type schema
        }
        jw.endArray();

        // jsonp?
        if (jsonp) {
            outWriter.write(")");
        }

        outWriter.flush();
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if(request == null) {
            return JSONType.CALLBACK_FUNCTION;
        } else if(!(request.getKvp().get("FORMAT_OPTIONS") instanceof Map)) {
        	return JSONType.CALLBACK_FUNCTION;
        }
        
        return JSONType.getCallbackFunction(request.getKvp());
        
    }

    private static void describeProperty(String name, AttributeDescriptor ad, GeoJSONBuilder jw) {
        jw.key(name);
        jw.object();
        Class<?> binding = ad.getType().getBinding();
        jw.key("type").value(mapToJsonType(binding));
        String format = mapToJsonFormat(binding);
        if (format != null) {
            jw.key("format").value(format);
        }
        if (ad.getMinOccurs() != 1) {
            jw.key("minimum").value(ad.getMinOccurs());
        }
        if (ad.getMaxOccurs() != 1) {
            jw.key("maximum").value(ad.getMinOccurs());
        }
        if (ad.getMinOccurs() > 0) {
            jw.key("required").value(true);
        }
        int fieldLen = FeatureTypes.getFieldLength(ad);
        if (fieldLen != FeatureTypes.ANY_LENGTH) {
            jw.key("maxLength").value(fieldLen);
        }
        jw.endObject(); // end of attribute description
    }

    private static String mapToJsonFormat(Class<?> binding) {
        if (java.sql.Date.class.isAssignableFrom(binding)) {
            return "date";
        } else if (java.sql.Time.class.isAssignableFrom(binding)) {
            return "time";
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            return "date-time";
        } else {
            return null;
        }
    }

    private static String mapToJsonType(Class<?> binding) {
        if (Long.class.isAssignableFrom(binding) || Integer.class.isAssignableFrom(binding)
                || Short.class.isAssignableFrom(binding) || Byte.class.isAssignableFrom(binding)) {
            return "integer";
        } else if (Number.class.isAssignableFrom(binding)) {
            return "number";
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return "boolean";
        } else if (Geometry.class.isAssignableFrom(binding)) {
            return binding.getSimpleName();
        } else {
            return "string";
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormats().iterator().next();
    }

}
