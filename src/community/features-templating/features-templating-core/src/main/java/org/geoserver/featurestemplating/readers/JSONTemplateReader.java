/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;

/** Produce the builder tree starting from the evaluation of json-ld template file * */
public class JSONTemplateReader implements TemplateReader {

    public static final String SOURCEKEY = "$source";

    public static final String CONTEXTKEY = "@context";

    public static final String FILTERKEY = "$filter";

    public static final String INCLUDEKEY = "$include";

    public static final String EXPRSTART = "${";

    public static final String VENDOROPTION = "$VendorOptions";

    private JsonNode template;

    private TemplateReaderConfiguration configuration;

    public JSONTemplateReader(JsonNode template, TemplateReaderConfiguration configuration) {
        this.template = template;
        this.configuration = configuration;
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
        return root;
    }

    private void getBuilderFromJson(
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
                    if (currentBuilder instanceof SourceBuilder) {
                        ((SourceBuilder) currentBuilder).setSource(source);
                    }
                } else if (entryName.equals(FILTERKEY)) {
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
                    maker.name(entryName).jsonNode(valueNode);
                    currentBuilder.addChild(maker.build());
                } else {
                    if (valueNode.isObject()) {
                        maker.name(entryName);
                        TemplateBuilder compositeBuilder = maker.build();
                        currentBuilder.addChild(compositeBuilder);
                        getBuilderFromJsonObject(valueNode, compositeBuilder, maker);
                    } else if (valueNode.isArray()) {
                        getBuilderFromJsonArray(entryName, valueNode, currentBuilder, maker);
                    } else {
                        if (!jumpField)
                            getBuilderFromJsonAttribute(
                                    entryName, valueNode, currentBuilder, maker);
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
        TemplateBuilder iteratingBuilder = maker.name(nodeName).collection(true).build();
        currentBuilder.addChild(iteratingBuilder);
        if (!node.toString().contains(EXPRSTART) && !node.toString().contains(FILTERKEY)) {
            maker.name(nodeName).jsonNode(node);
            currentBuilder.addChild(maker.build());
        } else {
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
                        TemplateBuilder compositeBuilder = maker.build();
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
        String vendorOption = node.asText();
        String[] options = vendorOption.split(";");
        for (String option : options) {
            String[] arrOp = option.split(":");
            builder.setVendorOptions(arrOp);
        }
        boolean flatOutput =
                builder.getVendorOptions()
                        .get(VendorOptions.FLAT_OUTPUT, Boolean.class, false)
                        .booleanValue();
        String separator = builder.getVendorOptions().get(VendorOptions.SEPARATOR, String.class);
        maker.flatOutput(flatOutput).separator(separator);
    }
}
