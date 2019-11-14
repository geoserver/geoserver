/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geoserver.jsonld.JsonLdGenerator;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * The root of the builders' tree. It manages the writing of the starting, the ending and
 * the @context json object of json-ld output, and trigger the evaluation
 */
public class RootBuilder implements JsonBuilder {

    public static NamespaceSupport namespaces;

    private List<JsonBuilder> children;

    private JsonNode contextHeader;

    public RootBuilder() {
        super();
        this.children = new ArrayList<JsonBuilder>(2);
    }

    public void addChild(JsonBuilder builder) {
        this.children.add(builder);
    }

    @Override
    public void evaluate(JsonLdGenerator writer, JsonBuilderContext context) throws IOException {
        for (JsonBuilder jb : children) {
            jb.evaluate(writer, context);
        }
    }

    public void startJsonLd(JsonLdGenerator generator) throws IOException {

        generator.writeStartObject();
        generator.writeFieldName("@context");
        generator.writeStartObject();
        Iterator<Map.Entry<String, JsonNode>> iterator = contextHeader.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> nodEntry = iterator.next();
            String entryName = nodEntry.getKey();
            JsonNode childNode = nodEntry.getValue();
            if (childNode.isObject()) {
                generator.writeObjectNode(entryName, childNode);
            } else if (childNode.isValueNode()) {
                generator.writeValueNode(entryName, childNode);
            } else {
                generator.writeArrayNode(entryName, childNode);
            }
        }
        generator.writeEndObject();
        generator.writeFieldName("type");
        generator.writeString("FeatureCollection");
        generator.writeFieldName("features");
        generator.writeStartArray();
    }

    public void endJsonLd(JsonLdGenerator generator) throws IOException {
        generator.writeEndArray();
        generator.writeEndObject();
    }

    @Override
    public List<JsonBuilder> getChildren() {
        return children;
    }

    public void setChildren(List<JsonBuilder> children) {
        this.children = children;
    }

    public JsonNode getContextHeader() {
        return contextHeader;
    }

    public void setContextHeader(JsonNode contextHeader) {
        this.contextHeader = contextHeader;
    }

    /**
     * Following two methods are a temporary workaround to pass namespaces to {@link
     * org.geoserver.jsonld.expressions.FilterFunction_xpath}*
     */
    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    public void handleHints(JsonNode hints) {
        if (hints != null) {
            namespaces = new NamespaceSupport();
            Iterator<Map.Entry<String, JsonNode>> iterator = hints.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> nodEntry = iterator.next();
                String entryName = nodEntry.getKey();
                JsonNode childNode = nodEntry.getValue();
                if (childNode.isValueNode()) {
                    try {
                        new URL(childNode.asText());
                        namespaces.declarePrefix(entryName, childNode.asText());
                    } catch (MalformedURLException ex) {
                        //
                    }
                }
            }
        }
    }
}
