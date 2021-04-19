/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.impl.RootBuilder.VendorOption.FLAT_OUTPUT;
import static org.geoserver.featurestemplating.builders.impl.RootBuilder.VendorOption.SEPARATOR;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.BuilderFactory;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.jsonld.JSONLDRootBuilder;
import org.geotools.filter.text.cql2.CQLException;

/** Produce the builder tree starting from the evaluation of json-ld template file * */
public class JSONTemplateReader implements TemplateReader {

    public static final String SOURCEKEY = "$source";

    public static final String CONTEXTKEY = "@context";

    public static final String FILTERKEY = "$filter";

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
        RootBuilder root;
        boolean isJsonLd;
        if (template.has(CONTEXTKEY)) isJsonLd = true;
        else isJsonLd = false;
        BuilderFactory factory = configuration.getBuilderFactory(isJsonLd);
        root = factory.getRootBuilder();
        getBuilderFromJson(null, template, root, factory);
        return root;
    }

    private void getBuilderFromJson(
            String nodeName,
            JsonNode node,
            TemplateBuilder currentBuilder,
            BuilderFactory factory) {
        if (node.isObject()) {
            getBuilderFromJsonObject(node, currentBuilder, factory);
        } else if (node.isArray()) {
            getBuilderFromJsonArray(nodeName, node, currentBuilder, factory);
        } else {
            getBuilderFromJsonAttribute(nodeName, node, currentBuilder, factory);
        }
    }

    private void getBuilderFromJsonObject(
            JsonNode node, TemplateBuilder currentBuilder, BuilderFactory factory) {
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
                    JSONLDRootBuilder rootBuilder = (JSONLDRootBuilder) currentBuilder;
                    rootBuilder.setContextHeader(valueNode);
                } else if (entryName.equals(VENDOROPTION)) {
                    setVendorOptions(valueNode, (RootBuilder) currentBuilder, factory);
                } else if (!strValueNode.contains(EXPRSTART)
                        && !strValueNode.contains(FILTERKEY)
                        && !jumpField) {
                    TemplateBuilder builder =
                            factory.getStaticBuilder(
                                    entryName, valueNode, configuration.getNamespaces());
                    currentBuilder.addChild(builder);
                } else {
                    if (valueNode.isObject()) {
                        TemplateBuilder compositeBuilder =
                                factory.getCompositeBuilder(
                                        entryName, configuration.getNamespaces());
                        currentBuilder.addChild(compositeBuilder);
                        getBuilderFromJsonObject(valueNode, compositeBuilder, factory);
                    } else if (valueNode.isArray()) {
                        getBuilderFromJsonArray(entryName, valueNode, currentBuilder, factory);
                    } else {
                        if (!jumpField)
                            getBuilderFromJsonAttribute(
                                    entryName, valueNode, currentBuilder, factory);
                    }
                }
            }
        }
    }

    private void getBuilderFromJsonArray(
            String nodeName,
            JsonNode node,
            TemplateBuilder currentBuilder,
            BuilderFactory factory) {
        TemplateBuilder iteratingBuilder =
                factory.getIteratingBuilder(nodeName, configuration.getNamespaces());
        currentBuilder.addChild(iteratingBuilder);
        if (!node.toString().contains(EXPRSTART) && !node.toString().contains(FILTERKEY)) {
            TemplateBuilder staticBuilder =
                    factory.getStaticBuilder(nodeName, node, configuration.getNamespaces());
            currentBuilder.addChild(staticBuilder);
        } else {
            Iterator<JsonNode> arrayIterator = node.elements();
            while (arrayIterator.hasNext()) {
                JsonNode childNode = arrayIterator.next();
                if (childNode.isObject()) {
                    String childJSON = childNode.toString();
                    if (isArrayControlNode(childNode)) {
                        // special object controlling array contents
                        getBuilderFromJsonObject(childNode, iteratingBuilder, factory);
                    } else if (childJSON.contains(EXPRSTART) || childJSON.contains(FILTERKEY)) {
                        // regular dynamic object/filtered object
                        TemplateBuilder compositeBuilder =
                                factory.getCompositeBuilder(null, configuration.getNamespaces());
                        iteratingBuilder.addChild(compositeBuilder);
                        getBuilderFromJsonObject(childNode, compositeBuilder, factory);
                    } else {
                        // static node
                        TemplateBuilder staticBuilder =
                                factory.getStaticBuilder(
                                        null, childNode, configuration.getNamespaces());
                        iteratingBuilder.addChild(staticBuilder);
                    }
                } else if (childNode.isArray()) {
                    getBuilderFromJsonArray(null, childNode, iteratingBuilder, factory);
                } else {
                    getBuilderFromJsonAttribute(null, childNode, iteratingBuilder, factory);
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
            BuilderFactory factory) {
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
            TemplateBuilder dynamicBuilder =
                    factory.getDynamicBuilder(nodeName, strNode, configuration.getNamespaces());
            if (filter != null) {
                setFilterToBuilder(dynamicBuilder, filter);
            }
            currentBuilder.addChild(dynamicBuilder);
        } else {
            TemplateBuilder staticBuilder;
            if (filter != null) {
                staticBuilder =
                        factory.getStaticBuilder(nodeName, strNode, configuration.getNamespaces());
                setFilterToBuilder(staticBuilder, filter);
            } else {
                staticBuilder =
                        factory.getStaticBuilder(nodeName, node, configuration.getNamespaces());
            }
            currentBuilder.addChild(staticBuilder);
        }
    }

    private void setFilterToBuilder(TemplateBuilder builder, JsonNode node) {
        String filter = node.get(FILTERKEY).asText();
        try {
            ((AbstractTemplateBuilder) builder).setFilter(filter);
        } catch (CQLException e) {
            throw new RuntimeException("Invalid filter " + filter, e);
        }
    }

    private void setFilterToBuilder(TemplateBuilder builder, String filter) {
        try {
            ((AbstractTemplateBuilder) builder).setFilter(filter);
        } catch (CQLException e) {
            throw new RuntimeException("Invalid filter " + filter, e);
        }
    }

    private void setVendorOptions(JsonNode node, RootBuilder builder, BuilderFactory factory) {
        String vendorOption = node.asText();
        String[] options = vendorOption.split(";");
        for (String option : options) {
            String[] arrOp = option.split(":");
            builder.setVendorOptions(arrOp);
        }
        String strFlatOutput = builder.getVendorOption(FLAT_OUTPUT.getVendorOptionName());
        if (strFlatOutput != null) {
            boolean flatOutput = Boolean.valueOf(strFlatOutput).booleanValue();
            factory.setFlatOutput(flatOutput);
            if (flatOutput) {
                String delimiter = builder.getVendorOption(SEPARATOR.getVendorOptionName());
                if (delimiter != null) {
                    factory.setSeparator(delimiter);
                }
            }
        }
    }
}
