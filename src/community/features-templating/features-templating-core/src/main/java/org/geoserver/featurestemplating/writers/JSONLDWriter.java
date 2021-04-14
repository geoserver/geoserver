/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.writers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geotools.filter.function.FilterFunction_toWKT;

/** Implements its superclass methods to write a valid json-ld output */
public class JSONLDWriter extends CommonJSONWriter {

    public JSONLDWriter(JsonGenerator generator) {
        super(generator);
    }

    private JsonNode contextHeader;

    @Override
    public void writeValue(Object value) throws IOException {
        writeString(String.valueOf(value));
    }

    @Override
    public void writeGeometry(Object value) throws IOException {
        FilterFunction_toWKT toWKT = new FilterFunction_toWKT();
        String wkt = (String) toWKT.evaluate(value);
        writeString(wkt);
    }

    @Override
    public void startTemplateOutput() throws IOException {
        writeStartObject();
        String contextName = "@context";
        if (contextHeader.isArray()) writeArrayNode(contextName, contextHeader);
        else if (contextHeader.isObject()) writeObjectNode(contextName, contextHeader);
        else writeValueNode(contextName, contextHeader);
        writeFieldName("type");
        writeString("FeatureCollection");
        writeFieldName("features");
        writeStartArray();
    }

    public void setContextHeader(JsonNode contextHeader) {
        this.contextHeader = contextHeader;
    }
}
