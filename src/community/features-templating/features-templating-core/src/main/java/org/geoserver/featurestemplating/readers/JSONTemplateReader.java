/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.VendorOptions.COLLECTION_NAME;
import static org.geoserver.featurestemplating.builders.VendorOptions.FLAT_OUTPUT;
import static org.geoserver.featurestemplating.builders.VendorOptions.JSONLD_TYPE;
import static org.geoserver.featurestemplating.builders.VendorOptions.JSON_LD_STRING_ENCODE;
import static org.geoserver.featurestemplating.builders.VendorOptions.SEPARATOR;
import static org.geoserver.featurestemplating.readers.JSONMerger.*;
import static org.geoserver.featurestemplating.readers.RecursiveJSONParser.INCLUDE_FLAT_EXPR;
import static org.geoserver.featurestemplating.readers.RecursiveJSONParser.INCLUDE_FLAT_KEY;
import static org.geoserver.featurestemplating.readers.RecursiveJSONParser.INCLUDING_NODE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geoserver.platform.FileWatcher;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.LiteralExpressionImpl;

/** Produce the builder tree starting from the evaluation of json-ld template file * */
public class JSONTemplateReader implements TemplateReader {

    public static final String SOURCEKEY = "$source";

    public static final String CONTEXTKEY = "@context";

    public static final String FILTERKEY = "$filter";

    public static final String EXPRSTART = "${";

    public static final String VENDOROPTION = "$options";

    private JsonNode template;

    private TemplateReaderConfiguration configuration;

    private List<FileWatcher<Object>> watchers;

    public JSONTemplateReader(
            JsonNode template,
            TemplateReaderConfiguration configuration,
            List<FileWatcher<Object>> watchers) {
        this.template = template;
        this.configuration = configuration;
        this.watchers = watchers;
    }

    /**
     * Get a builder tree as a ${@link RootBuilder} mapping it from a Json template
     *
     * @return
     */
    @Override
    public RootBuilder getRootBuilder() {
        TemplateBuilderMaker builderMaker = configuration.getBuilderMaker();
        if (template.has(CONTEXTKEY))
            builderMaker.encodingOption(CONTEXTKEY, template.get(CONTEXTKEY));
        builderMaker.rootBuilder(true);
        RootBuilder root = (RootBuilder) builderMaker.build();
        builderMaker.namespaces(configuration.getNamespaces());
        getBuilderFromJson(null, template, root, builderMaker);
        root.setWatchers(watchers);
        return root;
    }

    public void getBuilderFromJson(
            String nodeName,
            JsonNode node,
            TemplateBuilder currentBuilder,
            TemplateBuilderMaker maker) {
        if (node.isObject()) {
            getBuilderFromJsonObject(node, currentBuilder, maker);
        } else if (node.isArray()) {
            getBuilderFromJsonArray(nodeName, node, currentBuilder, maker);
        } else {
            getBuilderFromJsonAttribute(nodeName, node, currentBuilder, maker);
        }
    }

    private void getBuilderFromJsonObject(
            JsonNode node, TemplateBuilder currentBuilder, TemplateBuilderMaker maker) {
        // check special node at beginning of arrays, controlling the array contents
        if (isArrayControlNode(node)) {
            if (node.has(SOURCEKEY)) {
                String source = node.get(SOURCEKEY).asText();
                ((SourceBuilder) currentBuilder).setSource(source);
            }
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
                    currentBuilder = createCompositeIfNeeded(currentBuilder, maker);
                    if (currentBuilder instanceof SourceBuilder) {
                        ((SourceBuilder) currentBuilder).setSource(source);
                    }
                } else if (entryName.equals(FILTERKEY)) {
                    currentBuilder = createCompositeIfNeeded(currentBuilder, maker);
                    setFilterToBuilder(currentBuilder, node);
                } else if (entryName.equals(CONTEXTKEY)) {
                    RootBuilder rootBuilder = (RootBuilder) currentBuilder;
                    if (rootBuilder.getEncodingHints().get(CONTEXTKEY) == null) {
                        rootBuilder.getEncodingHints().put(CONTEXTKEY, valueNode);
                    }
                } else if (entryName.equals(VENDOROPTION)) {
                    setVendorOptions(valueNode, (RootBuilder) currentBuilder, maker);
                } else if (!strValueNode.contains(EXPRSTART)
                        && !strValueNode.contains(FILTERKEY)
                        && !jumpField) {
                    currentBuilder = createCompositeIfNeeded(currentBuilder, maker);
                    maker.name(entryName).jsonNode(valueNode);
                    currentBuilder.addChild(maker.build());
                } else if (entryName.startsWith(INCLUDE_FLAT_KEY)) {
                    currentBuilder = createCompositeIfNeeded(currentBuilder, maker);
                    ObjectNode objectNode = (ObjectNode) node;
                    ObjectNode container = (ObjectNode) objectNode.remove(INCLUDE_FLAT_KEY);
                    JsonNode includingNode = container.remove(INCLUDING_NODE);
                    JsonNode exprNode = container.remove(INCLUDE_FLAT_EXPR);
                    currentBuilder.addChild(
                            maker.jsonNode(valueNode)
                                    .dynamicIncludeFlatBuilder(true)
                                    .baseNode(includingNode)
                                    .textContent(exprNode.asText())
                                    .build());
                } else {
                    if (valueNode.isObject()) {
                        if (entryName.startsWith(DYNAMIC_MERGE_KEY)) {
                            if (valueNode.fields().hasNext()) {
                                currentBuilder = createCompositeIfNeeded(currentBuilder, maker);
                                Map.Entry<String, JsonNode> fieldNode = valueNode.fields().next();
                                String key = fieldNode.getKey();
                                JsonNode innerNode = fieldNode.getValue();
                                JsonNode overlay = innerNode.get(DYNAMIC_MERGE_OVERLAY);
                                JsonNode baseMergeNode = innerNode.get(DYNAMIC_MERGE_BASE);

                                currentBuilder.addChild(
                                        maker.overlayNode(overlay)
                                                .name(key)
                                                .baseNode(baseMergeNode)
                                                .build());
                            }
                        } else {
                            maker.name(entryName);
                            // if the parent of the template builder being created
                            // is a root one, in case of simplified template support,
                            // or hasNotOwnOutput is flagged the CompositeBuilder being produced
                            // maps the topLevelFeature source.
                            maker.topLevelFeature(isRootOrHasNotOwnOutput(currentBuilder));
                            TemplateBuilder compositeBuilder = maker.build();
                            currentBuilder.addChild(compositeBuilder);
                            getBuilderFromJsonObject(valueNode, compositeBuilder, maker);
                        }
                    } else if (valueNode.isArray()) {
                        getBuilderFromJsonArray(entryName, valueNode, currentBuilder, maker);
                    } else {
                        if (!jumpField) {
                            currentBuilder = createCompositeIfNeeded(currentBuilder, maker);
                            getBuilderFromJsonAttribute(
                                    entryName, valueNode, currentBuilder, maker);
                        }
                    }
                }
            }
        }
    }

    private void getBuilderFromJsonArray(
            String nodeName,
            JsonNode node,
            TemplateBuilder currentBuilder,
            TemplateBuilderMaker maker) {
        if (!node.toString().contains(EXPRSTART) && !node.toString().contains(FILTERKEY)) {
            maker.name(nodeName).jsonNode(node);
            currentBuilder.addChild(maker.build());
        } else {
            List<JsonNode> expressionNodes = new ArrayList<>();
            ArrayNode arrayNode = cleanFromExpressionNodes((ArrayNode) node, expressionNodes);
            if (!expressionNodes.isEmpty()) {
                addArrayIncludeFlat(currentBuilder, maker, arrayNode, nodeName, expressionNodes);
            } else {
                buildIteratingBuilder(nodeName, currentBuilder, (ArrayNode) node, maker);
            }
        }
    }

    private void buildIteratingBuilder(
            String nodeName,
            TemplateBuilder currentBuilder,
            ArrayNode node,
            TemplateBuilderMaker maker) {
        TemplateBuilder iteratingBuilder =
                maker.name(nodeName)
                        .collection(true)
                        .topLevelFeature(isRootOrHasNotOwnOutput(currentBuilder))
                        .build();
        currentBuilder.addChild(iteratingBuilder);
        Iterator<JsonNode> arrayIterator = node.elements();
        while (arrayIterator.hasNext()) {
            JsonNode childNode = arrayIterator.next();
            if (childNode.isObject()) {
                String childJSON = childNode.toString();
                if (isArrayControlNode(childNode)) {
                    // special object controlling array contents
                    getBuilderFromJsonObject(childNode, iteratingBuilder, maker);
                } else if (childJSON.contains(EXPRSTART) || childJSON.contains(FILTERKEY)) {
                    // regular dynamic object/filtered object
                    TemplateBuilder compositeBuilder =
                            maker.topLevelFeature(isRootOrHasNotOwnOutput(currentBuilder)).build();
                    iteratingBuilder.addChild(compositeBuilder);
                    getBuilderFromJsonObject(childNode, compositeBuilder, maker);
                } else {
                    // static node
                    maker.jsonNode(childNode);
                    iteratingBuilder.addChild(maker.build());
                }
            } else if (childNode.isArray()) {
                getBuilderFromJsonArray(null, childNode, iteratingBuilder, maker);
            } else {
                getBuilderFromJsonAttribute(null, childNode, iteratingBuilder, maker);
            }
        }
    }

    private void addArrayIncludeFlat(
            TemplateBuilder currentBuilder,
            TemplateBuilderMaker maker,
            ArrayNode node,
            String nodeName,
            List<JsonNode> expressionNodes) {
        // the array include flat is a container
        // since might have multiple includeFlat directive.
        TemplateBuilder arrayIncludeFlat =
                maker.dynamicIncludeFlatBuilder(true)
                        .baseNode(node)
                        .name(nodeName)
                        .textContent("${.}")
                        .collection(true)
                        .build();

        currentBuilder.addChild(arrayIncludeFlat);
        // here we add the actual expression of the includeFlat directive as children.
        for (JsonNode jsonNode : expressionNodes) {
            if (jsonNode.isValueNode()) {
                String text = jsonNode.asText();
                if (text.startsWith(INCLUDE_FLAT_KEY + "{") && text.endsWith("}")) {
                    text = text.substring(INCLUDE_FLAT_KEY.length() + 1, text.length() - 1);
                    arrayIncludeFlat.addChild(maker.textContent(text).build());
                }
            }
        }
    }

    private ArrayNode cleanFromExpressionNodes(ArrayNode arrNode, List<JsonNode> nodes) {
        Iterator<JsonNode> elements = arrNode.elements();
        ArrayNode arrayNode = null;
        while (elements.hasNext()) {
            JsonNode next = elements.next();
            if (next.isValueNode() && next.asText().startsWith(INCLUDE_FLAT_KEY)) {
                nodes.add(next);
            } else {
                if (arrayNode == null) arrayNode = JsonNodeFactory.instance.arrayNode();
                arrayNode.add(next);
            }
        }
        return arrayNode;
    }

    private boolean isArrayControlNode(JsonNode node) {
        return (node.size() == 1 && (node.has(SOURCEKEY) || node.has(FILTERKEY)))
                || (node.size() == 2 && node.has(SOURCEKEY) && node.has(FILTERKEY));
    }

    private void getBuilderFromJsonAttribute(
            String nodeName,
            JsonNode node,
            TemplateBuilder currentBuilder,
            TemplateBuilderMaker maker) {
        String strNode = node.asText();
        if (!node.asText().contains("FeatureCollection")) {
            maker.name(nodeName).contentAndFilter(strNode);
            TemplateBuilder builder = maker.build();
            currentBuilder.addChild(builder);
        }
    }

    private void setFilterToBuilder(TemplateBuilder builder, JsonNode node) {
        String filter = node.get(FILTERKEY).asText();
        ((AbstractTemplateBuilder) builder).setFilter(filter);
    }

    private void setVendorOptions(JsonNode node, RootBuilder builder, TemplateBuilderMaker maker) {
        if (!node.isObject()) {
            throw new RuntimeException("VendorOptions should be defined as a JSON object");
        }
        addOptionIfPresent(builder, FLAT_OUTPUT, node);
        addOptionIfPresent(builder, SEPARATOR, node);
        addOptionIfPresent(builder, JSON_LD_STRING_ENCODE, node);
        addOptionIfPresent(builder, COLLECTION_NAME, node);
        addOptionIfPresent(builder, JSONLD_TYPE, node);
        if (node.has(CONTEXTKEY)) {
            builder.addVendorOption(CONTEXTKEY, node.get(CONTEXTKEY));
        }
        Expression flatOutput =
                builder.getVendorOptions()
                        .get(FLAT_OUTPUT, Expression.class, new LiteralExpressionImpl(false));
        boolean bFlatOutput = flatOutput.evaluate(null, Boolean.class).booleanValue();
        Expression expression =
                builder.getVendorOptions()
                        .get(
                                VendorOptions.SEPARATOR,
                                Expression.class,
                                new LiteralExpressionImpl("_"));
        maker.flatOutput(bFlatOutput).separator(expression.evaluate(null, String.class));
    }

    private void addOptionIfPresent(RootBuilder builder, String optionName, JsonNode node) {
        if (node.has(optionName)) {
            TemplateCQLManager cqlManager =
                    new TemplateCQLManager(node.get(optionName).asText(), null);
            builder.addVendorOption(optionName, cqlManager.getExpressionFromString());
        }
    }

    // create a composite as direct child of a Root builder
    // needed for the case where we have a template not defining the features array but only
    // the feature attributes template
    private TemplateBuilder createCompositeIfNeeded(
            TemplateBuilder currentParent, TemplateBuilderMaker maker) {
        TemplateBuilder builder;
        if (currentParent instanceof RootBuilder) {
            maker.topLevelFeature(true);
            builder = maker.build();
            currentParent.addChild(builder);
        } else builder = currentParent;
        return builder;
    }

    private boolean isRootOrHasNotOwnOutput(TemplateBuilder parent) {
        return parent instanceof RootBuilder
                || (parent instanceof SourceBuilder && !((SourceBuilder) parent).hasOwnOutput());
    }
}
