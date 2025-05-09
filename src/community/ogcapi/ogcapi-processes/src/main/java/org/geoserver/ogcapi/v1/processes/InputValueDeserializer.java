/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.geoserver.ogcapi.v1.processes.InputValue.ArrayInputValue;
import static org.geoserver.ogcapi.v1.processes.InputValue.BoundingBoxInputValue;
import static org.geoserver.ogcapi.v1.processes.InputValue.ComplexJSONInputValue;
import static org.geoserver.ogcapi.v1.processes.InputValue.InlineFileInputValue;
import static org.geoserver.ogcapi.v1.processes.InputValue.LiteralInputValue;
import static org.geoserver.ogcapi.v1.processes.InputValue.ReferenceInputValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Arrays;

public class InputValueDeserializer extends JsonDeserializer<InputValue> {

    static final String CRS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    static final String CRS84H = "http://www.opengis.net/def/crs/OGC/0/CRS84h";

    @Override
    public InputValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        return getInputValue(node);
    }

    public InputValue getInputValue(JsonNode node) throws IOException {
        // handle case of array of inputs separately and then drill down
        if (node.isArray()) {
            ArrayInputValue array = new ArrayInputValue();
            for (int i = 0; i < node.size(); i++) {
                InputValue inputValue = getInputValueFlat(node.get(i));
                array.getValues().add(inputValue);
            }
            return array;
        }

        // non repeated inputs
        return getInputValueFlat(node);
    }

    private InputValue getInputValueFlat(JsonNode node) throws IOException {

        if (node.isObject()) {
            if (node.has("value")) {
                if (node.has("mediaType")) {
                    InlineFileInputValue file = new InlineFileInputValue();
                    file.value = node.get("value").asText();
                    file.mediaType = node.get("mediaType").asText();
                    return file;
                } else {
                    ComplexJSONInputValue complex = new ComplexJSONInputValue();
                    complex.value = node.get("value");
                    return complex;
                }
            }

            if (node.has("href")) {
                ReferenceInputValue ref = new ReferenceInputValue();
                ref.href = node.get("href").asText();
                ref.type = node.has("type") ? node.get("type").asText() : null;
                return ref;
            }

            if (node.has("bbox") && node.get("bbox").isArray()) {
                return parseBoundingBox(node);
            }

        } else {
            LiteralInputValue literal = new LiteralInputValue();
            literal.value = extractPrimitive(node);
            return literal;
        }

        throw new IOException("Unexpected input: " + node.toPrettyString());
    }

    private static BoundingBoxInputValue parseBoundingBox(JsonNode node) throws IOException {
        BoundingBoxInputValue bbox = new BoundingBoxInputValue();
        JsonNode array = node.get("bbox");
        double[] coords = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            coords[i] = array.get(i).asDouble();
        }
        String crsSpec = node.has("crs") ? node.get("crs").asText() : null;
        if (coords.length == 4) {
            bbox.lowerCorner = Arrays.asList(coords[0], coords[1]);
            bbox.upperCorner = Arrays.asList(coords[2], coords[3]);
            bbox.crs = crsSpec == null ? CRS84 : crsSpec;
        } else if (coords.length == 6) {
            bbox.lowerCorner = Arrays.asList(coords[0], coords[1], coords[2]);
            bbox.upperCorner = Arrays.asList(coords[3], coords[4], coords[5]);
            bbox.crs = crsSpec == null ? CRS84 : crsSpec;
        } else {
            throw new IOException("Invalid number of coordinates in bounding box (should be 4 or 6): " + node);
        }
        return bbox;
    }

    private Object extractPrimitive(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        } else if (node.isNumber()) {
            if (node.isInt()) return node.intValue();
            if (node.isLong()) return node.longValue();
            if (node.isFloat()) return node.floatValue();
            if (node.isDouble()) return node.doubleValue();
            return node.numberValue(); // fallback
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isNull()) {
            return null;
        }

        // Not a primitive
        throw new IllegalArgumentException("Expected a JSON primitive but got: " + node);
    }
}
