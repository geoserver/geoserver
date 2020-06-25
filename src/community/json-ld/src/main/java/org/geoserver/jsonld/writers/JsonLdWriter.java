/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jsonld.writers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.geotools.filter.function.FilterFunction_toWKT;

/** Implements its superclass methods to write a valid json-ld output */
public class JsonLdWriter extends CommonJsonWriter {

    public JsonLdWriter(JsonGenerator generator, boolean flattenedList) {
        super(generator, flattenedList);
    }

    private JsonNode contextHeader;

    @Override
    protected void writeValue(Object value) throws IOException {
        writeString(String.valueOf(value));
    }

    @Override
    protected void writeGeometry(Object value) throws IOException {
        FilterFunction_toWKT toWKT = new FilterFunction_toWKT();
        String wkt = (String) toWKT.evaluate(value);
        writeString(wkt);
    }

    @Override
    public void startJson() throws IOException {
        writeStartObject();
        writeFieldName("@context");
        writeStartObject();
        Iterator<Map.Entry<String, JsonNode>> iterator = contextHeader.fields();
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
        writeFieldName("type");
        writeString("FeatureCollection");
        writeFieldName("features");
        writeStartArray();
    }

    public void setContextHeader(JsonNode contextHeader) {
        this.contextHeader = contextHeader;
    }
}
