/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import static org.geoserver.featurestemplating.builders.EncodingHints.SKIP_OBJECT_ENCODING;
import static org.geoserver.featurestemplating.builders.EncodingHints.isSingleFeatureRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.ComplexAttribute;
import org.geotools.util.Converters;
import org.locationtech.jts.geom.Geometry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.util.StdDateFormat;

/** Decorator for a JsonGenerator that add some functionality mainly to write JsonNode */
public abstract class CommonJSONWriter extends TemplateOutputWriter {

    protected tools.jackson.core.JsonGenerator generator;
    private boolean flatOutput;

    protected TemplateIdentifier identifier;

    public CommonJSONWriter(tools.jackson.core.JsonGenerator generator, TemplateIdentifier templateIdentifier) {
        this.generator = generator;
        this.identifier = templateIdentifier;
    }

    @Override
    public void writeStaticContent(String key, Object staticContent, EncodingHints encodingHints) throws IOException {
        if (staticContent instanceof String || staticContent instanceof Number || staticContent instanceof Date) {
            if (key == null) {
                writeValue(staticContent);
            } else {
                generator.writeStringProperty(key, (String) staticContent);
            }
        } else {
            JsonNode jsonNode = (JsonNode) staticContent;
            if (jsonNode.isArray()) writeArrayNode(key, jsonNode);
            else if (jsonNode.isObject()) writeObjectNode(key, jsonNode);
            else writeValueNode(key, jsonNode);
        }
    }
    /**
     * Write contents from a Json Object. Used with {@link StaticBuilder} to write content as it is from the json-ld
     * template to the json-ld output
     */
    public void writeObjectNode(String nodeName, JsonNode node) throws IOException {
        if (nodeName != null && !nodeName.equals("")) generator.writeName(nodeName);
        writeStartObject();
        Set<Map.Entry<String, JsonNode>> properties = node.properties();
        for (Map.Entry<String, JsonNode> nodeEntry : properties) {
            String entryName = nodeEntry.getKey();
            JsonNode childNode = nodeEntry.getValue();
            if (childNode.isObject()) {
                writeObjectNode(entryName, childNode);
            } else if (childNode.isValueNode()) {
                writeValueNode(entryName, childNode);
            } else {
                writeArrayNode(entryName, childNode);
            }
        }
        writeEndObject();
    }

    /**
     * Write contents from a Json Array. Used with {@link StaticBuilder}ù to write content as it is from the json-ld
     * template to the json-ld output
     */
    public void writeArrayNode(String nodeName, JsonNode arNode) throws IOException {
        if (nodeName != null && !nodeName.equals("")) generator.writeName(nodeName);
        writeStartArray();
        for (JsonNode node : arNode.values()) {
            if (node.isValueNode()) {
                writeValueNode(null, node);
            } else if (node.isObject()) {
                writeObjectNode(null, node);
            } else if (node.isArray()) {
                writeArrayNode(null, node);
            }
        }
        writeEndArray();
    }

    /**
     * Write contents from a Json attribute's value. Used with {@link StaticBuilder} to write content as it is from the
     * json-ld template to the json-ld output
     */
    public void writeValueNode(String entryName, JsonNode valueNode) throws IOException {
        if (entryName != null && !entryName.equals("")) generator.writeName(entryName);
        if (valueNode.isString()) {
            generator.writeString(valueNode.asString());
        } else if (valueNode.isFloat() || valueNode.isDouble()) {
            generator.writeNumber(valueNode.asDouble());
        } else if (valueNode.isLong()) {
            generator.writeNumber(valueNode.asLong());
        } else if (valueNode.isNumber()) {
            generator.writeNumber(valueNode.asInt());
        } else if (valueNode.isBoolean()) {
            generator.writeBoolean(valueNode.asBoolean());
        } else if (valueNode.isNull()) {
            generator.writeNull();
        }
    }

    public void writeValue(Object value) throws IOException {
        if (isNull(value)) {
            generator.writeNull();
        } else if (value instanceof String string) {
            generator.writeString(string);
        } else if (value instanceof Integer integer1) {
            generator.writeNumber(integer1);
        } else if (value instanceof Double double1) {
            generator.writeNumber(double1);
        } else if (value instanceof Float float1) {
            generator.writeNumber(float1);
        } else if (value instanceof Long long1) {
            generator.writeNumber(long1);
        } else if (value instanceof BigInteger integer) {
            generator.writeNumber(integer);
        } else if (value instanceof BigDecimal decimal) {
            generator.writeNumber(decimal);
        } else if (value instanceof Boolean boolean1) {
            generator.writeBoolean(boolean1);
        } else if (value instanceof Date) {
            generator.writeString(new ISO8601Formatter().format(value));
        } else if (value.getClass().isArray()) {
            List list = Converters.convert(value, List.class);
            generator.writeStartArray();
            for (Object o : list) {
                writeValue(o);
            }
            generator.writeEndArray();
        } else if (value instanceof Geometry) {
            writeGeometry(value);
        } else {
            generator.writeString(value.toString());
        }
    }

    public void writeGeometry(Object value) throws IOException {
        JsonNode node = toJsonNode((Geometry) value);
        writeObjectNode(null, node);
    }

    protected JsonNode toJsonNode(Geometry geometry) throws JacksonException {
        String jsonGeom = org.geotools.data.geojson.GeoJSONWriter.toGeoJSON(geometry);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonGeom);
    }

    @Override
    public void writeElementName(Object elementName, EncodingHints encodingHints) throws IOException {
        if (elementName != null) generator.writeName(elementName.toString());
    }

    /** Write the result of an xpath or cql expression evaluation operated by the {@link DynamicValueBuilder} */
    @Override
    public void writeElementValue(Object result, EncodingHints encodingHints) throws IOException {
        writeElementNameAndValue(null, result, encodingHints);
    }

    /**
     * Write the key and the result of an xpath or cql expression evaluation operated by the {@link DynamicValueBuilder}
     */
    @Override
    public void writeElementNameAndValue(String key, Object result, EncodingHints encodingHints) throws IOException {
        if (result instanceof String || result instanceof Number || result instanceof Boolean) {
            writeElementName(key, null);
            writeValue(result);
        } else if (result instanceof Date timeStamp) {
            String formatted = new StdDateFormat().withColonInTimeZone(true).format(timeStamp);
            writeElementNameAndValue(key, formatted, encodingHints);
        } else if (result instanceof Geometry) {
            writeElementName(key, encodingHints);
            writeGeometry(result);
        } else if (result instanceof ComplexAttribute attr) {
            writeElementNameAndValue(key, attr.getValue(), encodingHints);
        } else if (result instanceof Attribute attr) {
            writeElementNameAndValue(key, attr.getValue(), encodingHints);
        } else if (result instanceof JsonNode) {
            writeStaticContent(key, result, encodingHints);
        } else if (result instanceof List list1) {
            writeList(key, list1, encodingHints, false);
        } else if (result == null) {
            writeElementName(key, encodingHints);
            generator.writeNull();
        } else if (result.getClass().isArray()) {
            List list = Converters.convert(result, List.class);
            writeList(key, list, encodingHints, true);
        } else {
            writeElementName(key, encodingHints);
            writeValue(result.toString());
        }
    }

    private void writeList(String key, List result, EncodingHints encodingHints, boolean forceArray)
            throws IOException {
        List list = result;
        if (list.size() == 1 && !forceArray) {
            writeElementNameAndValue(key, list.get(0), encodingHints);
        } else {
            if (!flatOutput) {
                generator.writeName(key);
                writeList(encodingHints, list);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    String itKey = null;
                    itKey = key + "_" + (i + 1);
                    writeElementNameAndValue(itKey, list.get(i), encodingHints);
                }
            }
        }
    }

    private void writeList(EncodingHints encodingHints, List list) throws IOException {
        writeStartArray();
        for (int i = 0; i < list.size(); i++) {
            writeElementValue(list.get(i), encodingHints);
        }
        writeEndArray();
    }

    @Override
    public void startTemplateOutput(EncodingHints encodingHints) throws IOException {

        writeStartObject();
        generator.writeName("type");
        generator.writeString("FeatureCollection");
        generator.writeName("features");
        writeStartArray();
    }

    @Override
    public void endTemplateOutput(EncodingHints encodingHints) throws IOException {
        if (!isSingleFeatureRequest()) {
            writeEndArray();
        }
        writeEndObject();
    }

    public void setFlatOutput(boolean flatOutput) {
        this.flatOutput = flatOutput;
    }

    @Override
    public void startObject(String name, EncodingHints encodingHints) throws IOException {
        if (name != null) writeElementName(name, encodingHints);
        writeStartObject();
    }

    @Override
    public void endObject(String name, EncodingHints encodingHints) throws IOException {
        writeEndObject();
    }

    @Override
    public void startArray(String name, EncodingHints encodingHints) throws IOException {
        writeElementName(name, encodingHints);
        writeStartArray();
    }

    @Override
    public void endArray(String name, EncodingHints encodingHints) throws IOException {
        writeEndArray();
    }

    public void writeStartArray() throws IOException {
        generator.writeStartArray();
    }

    public void writeEndArray() throws IOException {
        generator.writeEndArray();
    }

    public void writeStartObject() throws IOException {
        generator.writeStartObject();
    }

    public void writeEndObject() throws IOException {
        generator.writeEndObject();
    }

    @Override
    public void close() throws IOException {
        generator.close();
    }

    protected boolean skipObjectWriting(EncodingHints encodingHints) {
        Boolean skipIfSingleFeature = getEncodingHintIfPresent(encodingHints, SKIP_OBJECT_ENCODING, Boolean.class);
        return skipIfSingleFeature != null
                && skipIfSingleFeature.booleanValue()
                && isSingleFeatureRequest()
                && (identifier.equals(TemplateIdentifier.GEOJSON) || identifier.equals(TemplateIdentifier.JSONLD));
    }
}
