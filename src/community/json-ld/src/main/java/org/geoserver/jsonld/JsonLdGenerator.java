/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.*;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;

/** Decorator for a JsonGenerator that add some functionality mainly to write JsonNode */
public class JsonLdGenerator extends com.fasterxml.jackson.core.JsonGenerator {

    private com.fasterxml.jackson.core.JsonGenerator delegate;

    public JsonLdGenerator(com.fasterxml.jackson.core.JsonGenerator generator) {
        this.delegate = generator;
    }

    /**
     * Write contents from a Json Object. Used with {@link
     * org.geoserver.jsonld.builders.impl.StaticBuilder} to write content as it is from the json-ld
     * template to the json-ld output
     */
    public void writeObjectNode(String nodeName, JsonNode node) throws IOException {
        if (nodeName != null && !nodeName.equals("")) delegate.writeFieldName(nodeName);
        writeStartObject();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> nodEntry = iterator.next();
            String entryName = nodEntry.getKey();
            JsonNode childNode = nodEntry.getValue();
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
     * Write contents from a Json Array. Used with {@link
     * org.geoserver.jsonld.builders.impl.StaticBuilder}ù to write content as it is from the json-ld
     * template to the json-ld output
     */
    public void writeArrayNode(String nodeName, JsonNode arNode) throws IOException {
        if (nodeName != null && !nodeName.equals("")) delegate.writeFieldName(nodeName);
        writeStartArray();
        Iterator<JsonNode> arrayIterator = arNode.elements();
        while (arrayIterator.hasNext()) {
            JsonNode node = arrayIterator.next();
            if (node.isValueNode()) {
                writeValueNode(null, node);
            } else if (node.isObject()) {
                writeStartObject();
                writeObjectNode(null, node);
                writeEndObject();
            } else if (node.isArray()) {
                writeArrayNode(null, node);
            }
        }
        writeEndArray();
    }

    /**
     * Write contents from a Json attribute's value. Used with {@link
     * org.geoserver.jsonld.builders.impl.StaticBuilder}ù to write content as it is from the json-ld
     * template to the json-ld output
     */
    public void writeValueNode(String entryName, JsonNode valueNode) throws IOException {
        if (entryName != null && !entryName.equals("")) delegate.writeFieldName(entryName);
        if (valueNode.isTextual()) {
            writeString(valueNode.asText());
        } else if (valueNode.isFloat() || valueNode.isDouble()) {
            writeNumber(valueNode.asDouble());
        } else if (valueNode.isLong()) {
            writeNumber(valueNode.asLong());
        } else if (valueNode.isNumber()) {
            writeNumber(valueNode.asInt());
        } else if (valueNode.isBoolean()) {
            writeBoolean(valueNode.asBoolean());
        }
    }

    /**
     * Write the result of an xpath or cql expression evaluation operated by the {@link
     * org.geoserver.jsonld.builders.impl.DynamicValueBuilder}
     */
    public void writeResult(Object result) throws IOException {
        if (result instanceof String || result instanceof Number || result instanceof Boolean) {
            writeString(String.valueOf(result));
        } else if (result instanceof Date) {
            Date timeStamp = (Date) result;
            DateFormat df = new SimpleDateFormat("yyyy/mm/dd hh:mm:ss");
            writeResult(df.format(timeStamp));
        } else if (result instanceof ComplexAttribute) {
            ComplexAttribute attr = (ComplexAttribute) result;
            writeResult(attr.getValue());
        } else if (result instanceof Attribute) {
            Attribute attr = (Attribute) result;
            writeResult(attr.getValue());
        } else if (result instanceof List) {
            List list = (List) result;
            if (list.size() == 1) {
                writeResult(list.get(0));
            } else {
                writeStartArray();
                for (int i = 0; i < list.size(); i++) {
                    writeResult(list.get(i));
                }
                writeEndArray();
            }
        } else if (result == null) {
            writeNull();
        } else {
            throw new RuntimeException(
                    "We can't handle the provided object o type '" + result.getClass() + "'.");
        }
    }

    public void writeString(String entryName, String value) throws IOException {
        if (entryName != null && !entryName.equals("")) delegate.writeFieldName(entryName);
        if (value != null && !value.equals("")) delegate.writeString(value);
    }

    public com.fasterxml.jackson.core.JsonGenerator getDelegate() {
        return delegate;
    }

    @Override
    public com.fasterxml.jackson.core.JsonGenerator setCodec(ObjectCodec objectCodec) {
        return delegate.setCodec(objectCodec);
    }

    @Override
    public ObjectCodec getCodec() {
        return delegate.getCodec();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public com.fasterxml.jackson.core.JsonGenerator enable(Feature feature) {
        return delegate.enable(feature);
    }

    @Override
    public com.fasterxml.jackson.core.JsonGenerator disable(Feature feature) {
        return delegate.disable(feature);
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return delegate.isEnabled(feature);
    }

    @Override
    public int getFeatureMask() {
        return delegate.getFeatureMask();
    }

    @Override
    @SuppressWarnings("deprecation")
    public com.fasterxml.jackson.core.JsonGenerator setFeatureMask(int i) {
        return delegate.setFeatureMask(i);
    }

    @Override
    public com.fasterxml.jackson.core.JsonGenerator useDefaultPrettyPrinter() {
        return delegate.useDefaultPrettyPrinter();
    }

    @Override
    public void writeStartArray() throws IOException {
        delegate.writeStartArray();
    }

    @Override
    public void writeEndArray() throws IOException {
        delegate.writeEndArray();
    }

    @Override
    public void writeStartObject() throws IOException {
        delegate.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException {
        delegate.writeEndObject();
    }

    @Override
    public void writeFieldName(String s) throws IOException {
        delegate.writeFieldName(s);
    }

    @Override
    public void writeFieldName(SerializableString serializableString) throws IOException {
        delegate.writeFieldName(serializableString);
    }

    @Override
    public void writeString(String s) throws IOException {
        delegate.writeString(s);
    }

    @Override
    public void writeString(char[] chars, int i, int i1) throws IOException {
        delegate.writeString(chars, i, i1);
    }

    @Override
    public void writeString(SerializableString serializableString) throws IOException {
        delegate.writeString(serializableString);
    }

    @Override
    public void writeRawUTF8String(byte[] bytes, int i, int i1) throws IOException {
        delegate.writeRawUTF8String(bytes, i, i1);
    }

    @Override
    public void writeUTF8String(byte[] bytes, int i, int i1) throws IOException {
        delegate.writeUTF8String(bytes, i, i1);
    }

    @Override
    public void writeRaw(String s) throws IOException {
        delegate.writeRaw(s);
    }

    @Override
    public void writeRaw(String s, int i, int i1) throws IOException {
        delegate.writeRaw(s, i, i1);
    }

    @Override
    public void writeRaw(char[] chars, int i, int i1) throws IOException {
        delegate.writeRaw(chars, i, i1);
    }

    @Override
    public void writeRaw(char c) throws IOException {
        delegate.writeRaw(c);
    }

    @Override
    public void writeRawValue(String s) throws IOException {
        delegate.writeRawValue(s);
    }

    @Override
    public void writeRawValue(String s, int i, int i1) throws IOException {
        delegate.writeRawValue(s, i, i1);
    }

    @Override
    public void writeRawValue(char[] chars, int i, int i1) throws IOException {
        delegate.writeRaw(chars, i, i1);
    }

    @Override
    public void writeBinary(Base64Variant base64Variant, byte[] bytes, int i, int i1)
            throws IOException {
        delegate.writeBinary(base64Variant, bytes, i, i1);
    }

    @Override
    public int writeBinary(Base64Variant base64Variant, InputStream inputStream, int i)
            throws IOException {
        return delegate.writeBinary(base64Variant, inputStream, i);
    }

    @Override
    public void writeNumber(int i) throws IOException {
        delegate.writeNumber(i);
    }

    @Override
    public void writeNumber(long l) throws IOException {
        delegate.writeNumber(l);
    }

    @Override
    public void writeNumber(BigInteger bigInteger) throws IOException {
        delegate.writeNumber(bigInteger);
    }

    @Override
    public void writeNumber(double v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(float v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigDecimal bigDecimal) throws IOException {
        delegate.writeNumber(bigDecimal);
    }

    @Override
    public void writeNumber(String s) throws IOException {
        delegate.writeNumber(s);
    }

    @Override
    public void writeBoolean(boolean b) throws IOException {
        delegate.writeBoolean(b);
    }

    @Override
    public void writeNull() throws IOException {
        delegate.writeNull();
    }

    @Override
    public void writeObject(Object o) throws IOException {
        delegate.writeObject(o);
    }

    @Override
    public void writeTree(TreeNode treeNode) throws IOException {
        delegate.writeTree(treeNode);
    }

    @Override
    public JsonStreamContext getOutputContext() {
        return delegate.getOutputContext();
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
