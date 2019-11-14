/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import org.geoserver.jsonld.builders.*;
import org.geoserver.jsonld.builders.impl.*;

/** Produce the builder tree starting from the evaluation of json-ld template file * */
public class JsonLdTemplateReader {

    public static final String SOURCEKEY = "$source";

    public static final String CONTEXTKEY = "@context";

    public static final String EXPRSTART = "${";

    private JsonNode template;

    public JsonLdTemplateReader(JsonNode template) {
        this.template = template;
    }

    public RootBuilder getBuilderTree() {
        RootBuilder root = new RootBuilder();
        workJson(null, template, root);
        return root;
    }

    private void workJson(String nodeName, JsonNode node, JsonBuilder currentBuilder) {
        if (node.isObject()) {
            workObjectNode(node, currentBuilder);
        } else if (node.isArray()) {
            workArrayNode(nodeName, node, currentBuilder);
        } else {
            workValueNode(nodeName, node, currentBuilder);
        }
    }

    private void workObjectNode(JsonNode node, JsonBuilder currentBuilder) {
        if (node.has(SOURCEKEY) && node.size() == 1) {
            String source = node.get(SOURCEKEY).asText();
            ((SourceBuilder) currentBuilder).setSource(source);
        } else {
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> nodEntry = iterator.next();
                String entryName = nodEntry.getKey();
                JsonNode valueNode = nodEntry.getValue();
                // These fields have to be jumped cause they got writed
                // before feature evaluation starts
                boolean jumpField =
                        (entryName.equalsIgnoreCase("type")
                                || entryName.equalsIgnoreCase("features"));
                if (entryName.equals(SOURCEKEY)) {
                    String source = "/" + valueNode.asText();
                    if (currentBuilder instanceof SourceBuilder)
                        ((SourceBuilder) currentBuilder).setSource(source);
                } else if (entryName.equals(CONTEXTKEY)) {
                    RootBuilder rootBuilder = (RootBuilder) currentBuilder;
                    rootBuilder.setContextHeader(valueNode);
                    // Since a the moment namespaces can't be retrieved from feature collection
                    // it searches in the template file for a field named hints listing namespaces
                } else if (entryName.equals("@hints")) {
                    RootBuilder rootBuilder = (RootBuilder) currentBuilder;
                    rootBuilder.handleHints(valueNode);
                } else if (!valueNode.toString().contains(EXPRSTART) && !jumpField) {
                    StaticBuilder builder = new StaticBuilder(entryName, valueNode);
                    currentBuilder.addChild(builder);
                } else {
                    if (valueNode.isObject()) {
                        CompositeBuilder compositeBuilder = new CompositeBuilder(entryName);
                        currentBuilder.addChild(compositeBuilder);
                        workObjectNode(valueNode, compositeBuilder);
                        // else workValueNode(entryName, valueNode, currentBuilder);
                    } else if (valueNode.isArray()) {
                        workArrayNode(entryName, valueNode, currentBuilder);
                    } else {
                        if (!jumpField) workValueNode(entryName, valueNode, currentBuilder);
                    }
                }
            }
        }
    }

    private void workArrayNode(String nodeName, JsonNode node, JsonBuilder currentBuilder) {
        IteratingBuilder iteratingBuilder = new IteratingBuilder(nodeName);
        currentBuilder.addChild(iteratingBuilder);
        if (!node.toString().contains(EXPRSTART)) {
            StaticBuilder staticBuilder = new StaticBuilder(nodeName, node);
            currentBuilder.addChild(staticBuilder);
        } else {
            Iterator<JsonNode> arrayIterator = node.elements();
            while (arrayIterator.hasNext()) {
                JsonNode childNode = arrayIterator.next();
                if (childNode.isObject()) {
                    if (!childNode.has(SOURCEKEY) && childNode.toString().contains(EXPRSTART)) {
                        CompositeBuilder compositeBuilder = new CompositeBuilder(null);
                        iteratingBuilder.addChild(compositeBuilder);
                        workObjectNode(childNode, compositeBuilder);
                    } else {
                        workObjectNode(childNode, iteratingBuilder);
                    }
                } else if (childNode.isArray()) {
                    workArrayNode(nodeName, childNode, iteratingBuilder);
                } else {
                    workValueNode(nodeName, node, iteratingBuilder);
                }
            }
        }
    }

    private void workValueNode(String nodeName, JsonNode node, JsonBuilder currentBuilder) {
        if (node.toString().contains(EXPRSTART) && !node.asText().equals("FeatureCollection")) {
            DynamicValueBuilder dynamicBuilder = new DynamicValueBuilder(nodeName, node.asText());
            currentBuilder.addChild(dynamicBuilder);
        } else {
            StaticBuilder staticBuilder = new StaticBuilder(nodeName, node);
            currentBuilder.addChild(staticBuilder);
        }
    }
}
