/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import org.geoserver.jsonld.builders.*;
import org.geoserver.jsonld.builders.impl.*;
import org.geotools.filter.text.cql2.CQLException;
import org.xml.sax.helpers.NamespaceSupport;

/** Produce the builder tree starting from the evaluation of json-ld template file * */
public class JsonLdTemplateReader {

    public static final String SOURCEKEY = "$source";

    public static final String CONTEXTKEY = "@context";

    public static final String FILTERKEY = "$filter";

    public static final String EXPRSTART = "${";

    private JsonNode template;

    private NamespaceSupport namespaces;

    public JsonLdTemplateReader(JsonNode template, NamespaceSupport namespaces) {
        this.template = template;
        this.namespaces = namespaces;
    }

    public RootBuilder getRootBuilder() {
        RootBuilder root = new RootBuilder();
        getBuilderFromJson(null, template, root);
        return root;
    }

    private void getBuilderFromJson(String nodeName, JsonNode node, JsonBuilder currentBuilder) {
        if (node.isObject()) {
            getBuilderFromJsonObject(node, currentBuilder);
        } else if (node.isArray()) {
            getBuilderFromJsonArray(nodeName, node, currentBuilder);
        } else {
            getBuilderFromJsonAttribute(nodeName, node, currentBuilder);
        }
    }

    private void getBuilderFromJsonObject(JsonNode node, JsonBuilder currentBuilder) {
        if (node.has(SOURCEKEY) && node.size() == 1) {
            String source = node.get(SOURCEKEY).asText();
            ((SourceBuilder) currentBuilder).setSource(source);
            if (node.has(FILTERKEY)) {
                setFilterToBuilder(currentBuilder, node);
            }
        } else {
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> nodEntry = iterator.next();
                String entryName = nodEntry.getKey();
                JsonNode valueNode = nodEntry.getValue();
                String strValueNode = valueNode.toString();
                // These fields have to be jumped cause they got writed
                // before feature evaluation starts
                boolean jumpField =
                        (entryName.equalsIgnoreCase("type")
                                        && valueNode.asText().equals("FeatureCollection"))
                                || entryName.equalsIgnoreCase("features");
                if (entryName.equals(SOURCEKEY)) {
                    String source = valueNode.asText();
                    if (currentBuilder instanceof SourceBuilder) {
                        ((SourceBuilder) currentBuilder).setSource(source);
                    }
                } else if (entryName.equals(FILTERKEY)) {
                    setFilterToBuilder(currentBuilder, node);
                } else if (entryName.equals(CONTEXTKEY)) {
                    RootBuilder rootBuilder = (RootBuilder) currentBuilder;
                    rootBuilder.setContextHeader(valueNode);
                } else if (!strValueNode.contains(EXPRSTART)
                        && !strValueNode.contains(FILTERKEY)
                        && !jumpField) {
                    // to do static builder filter management
                    StaticBuilder builder = new StaticBuilder(entryName, valueNode, namespaces);
                    currentBuilder.addChild(builder);
                } else {
                    if (valueNode.isObject()) {
                        CompositeBuilder compositeBuilder =
                                new CompositeBuilder(entryName, namespaces);
                        currentBuilder.addChild(compositeBuilder);
                        getBuilderFromJsonObject(valueNode, compositeBuilder);
                    } else if (valueNode.isArray()) {
                        getBuilderFromJsonArray(entryName, valueNode, currentBuilder);
                    } else {
                        if (!jumpField)
                            getBuilderFromJsonAttribute(entryName, valueNode, currentBuilder);
                    }
                }
            }
        }
    }

    private void getBuilderFromJsonArray(
            String nodeName, JsonNode node, JsonBuilder currentBuilder) {
        IteratingBuilder iteratingBuilder = new IteratingBuilder(nodeName, namespaces);
        currentBuilder.addChild(iteratingBuilder);
        if (!node.toString().contains(EXPRSTART)) {
            StaticBuilder staticBuilder = new StaticBuilder(nodeName, node, namespaces);
            currentBuilder.addChild(staticBuilder);
        } else {
            Iterator<JsonNode> arrayIterator = node.elements();
            while (arrayIterator.hasNext()) {
                JsonNode childNode = arrayIterator.next();
                if (childNode.isObject()) {
                    if (!childNode.has(SOURCEKEY) && childNode.toString().contains(EXPRSTART)) {
                        // CompositeBuilder child of Iterating has no key
                        CompositeBuilder compositeBuilder = new CompositeBuilder(null, namespaces);
                        iteratingBuilder.addChild(compositeBuilder);
                        getBuilderFromJsonObject(childNode, compositeBuilder);
                    } else {
                        getBuilderFromJsonObject(childNode, iteratingBuilder);
                    }
                } else if (childNode.isArray()) {
                    getBuilderFromJsonArray(nodeName, childNode, iteratingBuilder);
                } else {
                    getBuilderFromJsonAttribute(nodeName, node, iteratingBuilder);
                }
            }
        }
    }

    private void getBuilderFromJsonAttribute(
            String nodeName, JsonNode node, JsonBuilder currentBuilder) {
        String strNode = node.asText();
        String filter = null;
        if (strNode.contains(FILTERKEY)) {
            strNode = strNode.replace(FILTERKEY + "{", "");
            int sepIndex = strNode.indexOf('}') + 1;
            String sep = String.valueOf(strNode.charAt(sepIndex));
            String[] arrNode = strNode.split(sep);
            strNode = arrNode[1];
            filter = arrNode[0];
            filter = filter.substring(0, filter.length() - 1);
        }
        if (node.toString().contains(EXPRSTART) && !node.asText().equals("FeatureCollection")) {
            DynamicValueBuilder dynamicBuilder =
                    new DynamicValueBuilder(nodeName, strNode, namespaces);
            if (filter != null) {
                setFilterToBuilder(dynamicBuilder, filter);
            }
            currentBuilder.addChild(dynamicBuilder);
        } else {
            StaticBuilder staticBuilder;
            if (filter != null) {
                staticBuilder = new StaticBuilder(nodeName, strNode, namespaces);
                setFilterToBuilder(staticBuilder, filter);
            } else {
                staticBuilder = new StaticBuilder(nodeName, node, namespaces);
            }
            currentBuilder.addChild(staticBuilder);
        }
    }

    private void setFilterToBuilder(JsonBuilder builder, JsonNode node) {
        String filter = node.get(FILTERKEY).asText();
        try {
            ((AbstractJsonBuilder) builder).setFilter(filter);
        } catch (CQLException e) {
            throw new RuntimeException("Invalid filter " + filter, e);
        }
    }

    private void setFilterToBuilder(JsonBuilder builder, String filter) {
        try {
            ((AbstractJsonBuilder) builder).setFilter(filter);
        } catch (CQLException e) {
            throw new RuntimeException("Invalid filter " + filter, e);
        }
    }
}
