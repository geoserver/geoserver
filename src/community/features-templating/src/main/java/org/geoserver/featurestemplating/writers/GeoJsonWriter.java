/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.writers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.Link;
import org.geoserver.api.features.CollectionDocument;
import org.geoserver.api.features.FeaturesResponse;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.MediaType;

/** Implements its superclass methods to write a valid GeoJSON output */
public class GeoJsonWriter extends CommonJsonWriter {

    private long numberReturned;

    private CoordinateReferenceSystem crs;

    private CRS.AxisOrder axisOrder = CRS.AxisOrder.EAST_NORTH;

    public GeoJsonWriter(JsonGenerator generator) {
        super(generator);
    }

    @Override
    protected void writeValue(Object value) throws IOException {
        if (value instanceof String) {
            writeString((String) value);
        } else if (value instanceof Integer) {
            writeNumber((Integer) value);
        } else if (value instanceof Double) {
            writeNumber((Double) value);
        } else if (value instanceof Float) {
            writeNumber((Float) value);
        } else if (value instanceof Long) {
            writeNumber((Long) value);
        } else if (value instanceof BigInteger) {
            writeNumber((BigInteger) value);
        } else if (value instanceof BigDecimal) {
            writeNumber((BigDecimal) value);
        } else if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
        }
    }

    @Override
    protected void writeGeometry(Object value) throws IOException {
        GeometryJSON geomJson = new GeometryJSON();
        String strGeom = geomJson.toString((Geometry) value);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(strGeom);
        writeObjectNode(null, actualObj);
    }

    @Override
    public void endTemplateOutput() throws IOException {
        endObject();
    }

    private void writeLink(String title, String mimeType, String rel, String href)
            throws IOException {

        if (href != null) {
            startObject();
            if (title != null) {
                writeFieldName("title");
                writeValue(title);
            }
            if (mimeType != null) {
                writeFieldName("type");
                writeValue(mimeType);
            }
            if (rel != null) {
                writeFieldName("rel");
                writeValue(rel);
            }
            writeFieldName("href");
            writeValue(href);
            endObject();
        }
    }

    public void writePagingLinks(String mimeType, String previous, String next) throws IOException {

        writeFieldName("links");
        startArray();
        writeLink("previous page", mimeType, "previous", previous);
        writeLink("next page", mimeType, "next", next);
        endArray();
    }

    public void writeCollectionCounts(BigInteger featureCount) throws IOException {
        // counts
        if (featureCount != null && featureCount.longValue() > 0) {
            writeFieldName("totalFeatures");
            writeValue(featureCount);
            writeFieldName("numberMatched");
            writeValue(featureCount);
        } else {
            writeFieldName("totalFeatures");
            writeValue("unknown");
        }
        writeNumberReturned();
    }

    public void writeNumberReturned() throws IOException {
        writeFieldName("numberReturned");
        writeValue(numberReturned);
    }

    public void writeTimeStamp() throws IOException {
        writeFieldName("timeStamp");
        writeValue(new ISO8601Formatter().format(new Date()));
    }

    public void writeLinks(
            String previous, String next, String prefixedName, String featureId, String mimeType)
            throws IOException {
        APIRequestInfo requestInfo = APIRequestInfo.get();
        writeElementName("links");
        startArray();
        // paging links
        if (previous != null) {
            writeLink("Previous page", mimeType, "prev", previous);
        }
        if (next != null) {
            writeLink("Next page", mimeType, "next", next);
        }
        // alternate/self links
        String basePath = "ogc/features/collections/" + ResponseUtils.urlEncode(prefixedName);
        Collection<MediaType> formats =
                requestInfo.getProducibleMediaTypes(FeaturesResponse.class, true);
        for (MediaType format : formats) {
            String path = basePath + "/items";
            if (featureId != null) {
                path += "/" + ResponseUtils.urlEncode(featureId);
            }
            String href =
                    ResponseUtils.buildURL(
                            requestInfo.getBaseURL(),
                            path,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_ALTERNATE;
            String linkTitle = "This document as " + format;
            if (format.toString().equals(mimeType)) {
                linkType = Link.REL_SELF;
                linkTitle = "This document";
            }
            writeLink(linkTitle, format.toString(), linkType, href);
        }
        // backpointer to the collection
        for (MediaType format :
                requestInfo.getProducibleMediaTypes(CollectionDocument.class, true)) {
            String href =
                    ResponseUtils.buildURL(
                            requestInfo.getBaseURL(),
                            basePath,
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            String linkType = Link.REL_COLLECTION;
            String linkTitle = "The collection description as " + format;
            writeLink(linkTitle, format.toString(), linkType, href);
        }
        endArray();
    }

    public void writeCrs() throws FactoryException, IOException {
        writeFieldName("crs");
        if (crs != null) {
            String identifier = null;
            Integer code = CRS.lookupEpsgCode(crs, true);
            if (code != null) {
                if (code != null) {
                    identifier = SrsSyntax.OGC_URN.getPrefix() + code;
                }
            } else {
                identifier = CRS.lookupIdentifier(crs, true);
            }
            startObject();
            writeFieldName("type");
            writeValue("name");
            writeFieldName("properties");
            startObject();
            writeFieldName("name");
            writeValue(identifier);
            endObject(); // end properties
            endObject(); // end crs
        } else {
            writeNull();
        }
    }

    public void writeBoundingBox(Envelope env) throws IOException {
        writeFieldName("bbox");
        startArray();
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
        endArray();
    }

    public void writeStaticContent(String key, Object staticContent, String separator)
            throws IOException {
        if (separator == null || staticContent instanceof String)
            super.writeStaticContent(key, staticContent);
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

    public void incrementNumberReturned() {
        numberReturned++;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public void setAxisOrder(CRS.AxisOrder axisOrder) {
        this.axisOrder = axisOrder;
    }
}
