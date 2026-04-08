/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.json.GeoJSONBuilder;
import org.geoserver.json.JSONType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.filter.IsBetweenImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.OrImpl;
import org.locationtech.jts.geom.Geometry;

/**
 * A DescribeFeatureType output format that generates a JSON schema instead of an XML one
 *
 * @author Andrea Aime - GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 */
public class JSONDescribeFeatureTypeResponse extends WFSDescribeFeatureTypeOutputFormat {

    public JSONDescribeFeatureTypeResponse(GeoServer gs, final String mime) {
        super(gs, mime);
    }

    @Override
    protected void write(FeatureTypeInfo[] featureTypeInfos, OutputStream output, Operation describeFeatureType)
            throws IOException {

        if (featureTypeInfos.length == 0) {
            throw new IOException("Unable to write an empty feature info array.");
        }
        // prepare to write out
        try (OutputStreamWriter osw =
                        new OutputStreamWriter(output, gs.getSettings().getCharset());
                Writer outWriter = new BufferedWriter(osw)) {
            // jsonp?
            final boolean jsonp = JSONType.useJsonp(getMimeType(featureTypeInfos, describeFeatureType));
            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }

            // starting with JSON
            GeoJSONBuilder jw = new GeoJSONBuilder(outWriter);
            jw.object();
            jw.key("elementFormDefault");
            jw.value("qualified");
            jw.key("targetNamespace");
            NamespaceInfo nsInfo = featureTypeInfos[0].getNamespace();
            jw.value(nsInfo.getURI());
            jw.key("targetPrefix");
            jw.value(nsInfo.getName());
            jw.key("featureTypes");
            // in general one can describe more than one feature type
            jw.array();
            for (FeatureTypeInfo ft : featureTypeInfos) {
                jw.object();
                jw.key("typeName").value(ft.getName());
                SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
                jw.key("properties");
                jw.array();
                for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                    if (ad == schema.getGeometryDescriptor()) {
                        // this one we already described
                        describeProperty(ad.getLocalName(), ad, jw, true);
                    } else {
                        describeProperty(ad.getLocalName(), ad, jw, false);
                    }
                }
                jw.endArray();
                jw.endObject(); // end of the feature type schema
            }
            jw.endArray();
            jw.endObject();

            // jsonp?
            if (jsonp) {
                outWriter.write(")");
            }
            outWriter.flush();
        }
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }

    private static void describeProperty(String name, AttributeDescriptor ad, GeoJSONBuilder jw, boolean isGeometry) {
        jw.object();
        jw.key("name").value(name);
        jw.key("maxOccurs").value(ad.getMaxOccurs());
        jw.key("minOccurs").value(ad.getMinOccurs());
        jw.key("nillable").value((ad.getMinOccurs() > 0) ? false : true);
        Class<?> binding = ad.getType().getBinding();
        if (isGeometry) {
            jw.key("type").value("gml:" + mapToJsonType(binding));
        } else {
            jw.key("type").value("xsd:" + mapToJsonType(binding));
        }
        jw.key("localType").value(mapToJsonType(binding));

        if (ad.getType().getRestrictions() != null
                && !ad.getType().getRestrictions().isEmpty()) {
            describePropertyRestrictions(ad, jw);
        }

        jw.endObject(); // end of attribute description
    }

    private static String mapToJsonType(Class<?> binding) {
        if (Long.class.isAssignableFrom(binding)
                || Integer.class.isAssignableFrom(binding)
                || Short.class.isAssignableFrom(binding)
                || Byte.class.isAssignableFrom(binding)) {
            return "int";
        } else if (Number.class.isAssignableFrom(binding)) {
            return "number";
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return "boolean";
        } else if (Geometry.class.isAssignableFrom(binding)) {
            return binding.getSimpleName();
        } else if (java.sql.Date.class.isAssignableFrom(binding)) {
            return "date";
        } else if (java.sql.Time.class.isAssignableFrom(binding)) {
            return "time";
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            return "date-time";
        } else {
            return "string";
        }
    }

    private static void describePropertyRestrictions(AttributeDescriptor ad, GeoJSONBuilder jw) {
        jw.key("restriction");
        jw.object();

        for (Filter f : ad.getType().getRestrictions()) {
            Class<? extends Filter> filterClass = f.getClass();
            boolean isExpressionNumeric =
                    Number.class.isAssignableFrom(ad.getType().getBinding());

            if (filterClass == IsBetweenImpl.class) {
                String lowerBoundary = ((IsBetweenImpl) f).getLowerBoundary().toString();
                String upperBoundary = ((IsBetweenImpl) f).getUpperBoundary().toString();
                jw.key("minInclusive").value(renderExpression(isExpressionNumeric, lowerBoundary));
                jw.key("maxInclusive").value(renderExpression(isExpressionNumeric, upperBoundary));
            } else if (filterClass == OrImpl.class) {
                jw.key("enumeration");
                jw.array();
                for (Filter eq : ((OrImpl) f).getChildren()) {
                    String expression = ((IsEqualsToImpl) eq).getExpression2().toString();
                    jw.value(renderExpression(isExpressionNumeric, expression));
                }
                jw.endArray();
            }
        }
        jw.endObject(); // end restriction object
    }

    private static Object renderExpression(boolean isExpressionNumeric, String expression) {
        if (!isExpressionNumeric || expression.contains("Infinity") || expression.equals("NaN")) {
            return expression;
        }
        return Double.parseDouble(expression);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormats().isEmpty()
                ? null
                : getOutputFormats().iterator().next();
    }
}
