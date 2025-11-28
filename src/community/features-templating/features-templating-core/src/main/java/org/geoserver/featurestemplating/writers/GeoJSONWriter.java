/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.writers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;

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
    public void writeLink(String href, String rel, String mimeType, String title, String method) throws IOException {
        if (href != null) {
            writeStartObject();
            if (title != null) {
                generator.writeName("title");
                writeValue(title);
            }
            if (mimeType != null) {
                generator.writeName("type");
                writeValue(mimeType);
            }
            if (rel != null) {
                generator.writeName("rel");
                writeValue(rel);
            }
            if (method != null) {
                generator.writeName("method");
                writeValue(method);
            }
            generator.writeName("href");
            writeValue(href);
            writeEndObject();
        }
    }

    public void writePagingLinks(String mimeType, String previous, String next) throws IOException {

        generator.writeName("links");
        writeStartArray();
        writeLink(previous, "previous", mimeType, "previous page", null);
        writeLink(next, "next", mimeType, "next page", null);
        writeEndArray();
    }

    public void writeCollectionCounts(BigInteger featureCount) throws IOException {
        // counts
        if (featureCount != null && featureCount.longValue() > 0) {
            generator.writeName("totalFeatures");
            writeValue(featureCount);
            generator.writeName("numberMatched");
            writeValue(featureCount);
        } else {
            generator.writeName("totalFeatures");
            writeValue("unknown");
        }
        writeNumberReturned();
    }

    public void writeNumberReturned() throws IOException {
        generator.writeName("numberReturned");
        writeValue(numberReturned);
    }

    public void writeTimeStamp() throws IOException {
        generator.writeName("timeStamp");
        writeValue(new ISO8601Formatter().format(new Date()));
    }

    public void writeCrs() throws IOException {
        generator.writeName("crs");
        if (crs != null) {
            String identifier = getCRSIdentifier(crs);
            writeStartObject();
            generator.writeName("type");
            writeValue("name");
            generator.writeName("properties");
            writeStartObject();
            generator.writeName("name");
            writeValue(identifier);
            writeEndObject(); // end properties
            writeEndObject(); // end crs
        } else {
            generator.writeNull();
        }
    }

    public void writeCollectionBounds(ReferencedEnvelope env) throws IOException {
        generator.writeName("bbox");
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

    public void writeStaticContent(String key, Object staticContent, String separator) throws IOException {
        if (separator == null || staticContent instanceof String) super.writeStaticContent(key, staticContent, null);
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

    private void writeArrayNodeFlat(String nodeName, JsonNode arNode, String separator) throws IOException {
        int i = 1;
        for (JsonNode node : arNode.values()) {
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

    private void writeObjectNodeFlat(String superNodeName, JsonNode node, String separator) throws IOException {
        for (Map.Entry<String, JsonNode> nodeEntry : node.properties()) {
            String entryName = nodeEntry.getKey();
            String newEntryName;
            if (entryName != null) {
                newEntryName = superNodeName + separator + entryName;
            } else {
                newEntryName = null;
            }
            JsonNode childNode = nodeEntry.getValue();
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
