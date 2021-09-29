/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.writers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

/** Implements its superclass methods to write a valid GeoJSON output */
public class GeoJSONWriter extends CommonJSONWriter {

    public GeoJSONWriter(JsonGenerator generator, TemplateIdentifier identifier) {
        super(generator, identifier);
    }

    public GeoJSONWriter(JsonGenerator generator) {
        super(generator, TemplateIdentifier.JSON);
    }

    @Override
    public void endTemplateOutput(EncodingHints encodingHints) throws IOException {
        endObject(null, encodingHints);
    }

    /**
     * Writes a OGC link object Writes a OGC link object
     *
     * @param href
     * @param rel
     * @param mimeType
     * @param title
     * @param method
     * @throws IOException
     */
    public void writeLink(String href, String rel, String mimeType, String title, String method)
            throws IOException {
        if (href != null) {
            writeStartObject();
            if (title != null) {
                generator.writeFieldName("title");
                writeValue(title);
            }
            if (mimeType != null) {
                generator.writeFieldName("type");
                writeValue(mimeType);
            }
            if (rel != null) {
                generator.writeFieldName("rel");
                writeValue(rel);
            }
            if (method != null) {
                generator.writeFieldName("method");
                writeValue(method);
            }
            generator.writeFieldName("href");
            writeValue(href);
            writeEndObject();
        }
    }

    public void writePagingLinks(String mimeType, String previous, String next) throws IOException {

        generator.writeFieldName("links");
        writeStartArray();
        writeLink(previous, "previous", mimeType, "previous page", null);
        writeLink(next, "next", mimeType, "next page", null);
        writeEndArray();
    }

    public void writeCollectionCounts(BigInteger featureCount) throws IOException {
        // counts
        if (featureCount != null && featureCount.longValue() > 0) {
            generator.writeFieldName("totalFeatures");
            writeValue(featureCount);
            generator.writeFieldName("numberMatched");
            writeValue(featureCount);
        } else {
            generator.writeFieldName("totalFeatures");
            writeValue("unknown");
        }
        writeNumberReturned();
    }

    public void writeNumberReturned() throws IOException {
        generator.writeFieldName("numberReturned");
        writeValue(numberReturned);
    }

    public void writeTimeStamp() throws IOException {
        generator.writeFieldName("timeStamp");
        writeValue(new ISO8601Formatter().format(new Date()));
    }

    public void writeCrs() throws IOException {
        generator.writeFieldName("crs");
        if (crs != null) {
            String identifier = getCRSIdentifier(crs);
            writeStartObject();
            generator.writeFieldName("type");
            writeValue("name");
            generator.writeFieldName("properties");
            writeStartObject();
            generator.writeFieldName("name");
            writeValue(identifier);
            writeEndObject(); // end properties
            writeEndObject(); // end crs
        } else {
            generator.writeNull();
        }
    }

    public void writeCollectionBounds(ReferencedEnvelope env) throws IOException {
        generator.writeFieldName("bbox");
        writeStartArray();
        if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
            writeValue(env.getMinY());
            writeValue(env.getMinX());
            writeValue(env.getMaxY());
            writeValue(env.getMaxX());
        } else {
            writeValue(env.getMinX());
            writeValue(env.getMinY());
            writeValue(env.getMaxX());
            writeValue(env.getMaxY());
        }
        writeEndArray();
    }

    public void writeStaticContent(String key, Object staticContent, String separator)
            throws IOException {
        if (separator == null || staticContent instanceof String)
            super.writeStaticContent(key, staticContent, null);
        else {
            JsonNode jsonNode = (JsonNode) staticContent;
            if (jsonNode.isArray()) {
                writeArrayNodeFlat(key, jsonNode, separator);
            } else if (jsonNode.isObject()) {
                writeObjectNodeFlat(key, jsonNode, separator);
            } else {
                writeValueNode(key, jsonNode);
            }
        }
    }

    private void writeArrayNodeFlat(String nodeName, JsonNode arNode, String separator)
            throws IOException {
        Iterator<JsonNode> arrayIterator = arNode.elements();
        int i = 1;
        while (arrayIterator.hasNext()) {
            JsonNode node = arrayIterator.next();
            String arrayNodeName = nodeName + "_" + i;
            if (node.isValueNode()) {
                writeValueNode(arrayNodeName, node);
            } else if (node.isObject()) {
                writeObjectNodeFlat(arrayNodeName, node, separator);
            } else if (node.isArray()) {
                writeArrayNodeFlat(arrayNodeName, node, separator);
            }
            i++;
        }
    }

    private void writeObjectNodeFlat(String superNodeName, JsonNode node, String separator)
            throws IOException {
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> nodEntry = iterator.next();
            String entryName = nodEntry.getKey();
            String newEntryName;
            if (entryName != null) {
                newEntryName = superNodeName + separator + entryName;
            } else {
                newEntryName = null;
            }
            JsonNode childNode = nodEntry.getValue();
            if (childNode.isObject()) {
                writeObjectNodeFlat(newEntryName, childNode, separator);
            } else if (childNode.isValueNode()) {
                writeValueNode(newEntryName, childNode);
            } else {
                writeArrayNodeFlat(newEntryName, childNode, separator);
            }
        }
    }
}
