/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.JSONFieldSupport;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A builder able to evaluate an $includeFlat directive. The json property will be merged with the
 * including node to handle eventually duplicate fields.
 */
public class DynamicIncludeFlatBuilder extends DynamicValueBuilder {

    private static Logger LOGGER = Logging.getLogger(DynamicIncludeFlatBuilder.class);

    private JsonNode includingNode;

    public DynamicIncludeFlatBuilder(
            String expression, NamespaceSupport namespaces, JsonNode includingNode) {
        // key is null since the $includeFlat key should not appear
        super(null, expression, namespaces);
        this.includingNode = includingNode;
    }

    public DynamicIncludeFlatBuilder(DynamicIncludeFlatBuilder original, boolean includeChildren) {
        super(original, includeChildren);
        this.includingNode = original.getIncludingNode();
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (canWrite(context)) {
            ObjectNode finalNode = getFinalJSON(context);

            if (finalNode != null) doIncludeFlat(finalNode, context, writer);
            else iterateAndEvaluateNestedTree(context, writer, (ObjectNode) includingNode);
        }
    }

    protected ObjectNode getFinalJSON(TemplateBuilderContext context) {
        Object evaluate = null;
        if (xpath != null) {
            evaluate = evaluateXPath(context);
        } else if (cql != null) {
            evaluate = evaluateExpressions(cql, context);
        }

        return mergeNodes(evaluate);
    }

    private ObjectNode mergeNodes(Object evaluate) {
        ObjectNode finalNode = null;
        if (!(evaluate instanceof ObjectNode) && evaluate != null) {
            String message = "Cannot include flat a value different from a JSON object";
            LOGGER.severe(() -> message);
            throw new UnsupportedOperationException(message);
        } else if (evaluate != null) {
            JSONMerger merger = new JSONMerger();
            finalNode = merger.mergeTrees(includingNode, (ObjectNode) evaluate);
        }
        return finalNode;
    }

    private void doIncludeFlat(
            ObjectNode finalNode, TemplateBuilderContext context, TemplateOutputWriter writer)
            throws IOException {
        if (hasDynamic(finalNode)) {
            LOGGER.fine(
                    () ->
                            "Included Json object has property interpolation or expression."
                                    + "Going to build a nested TemplateBuilder tree.");
            iterateAndEvaluateNestedTree(context, writer, finalNode);
        } else {
            LOGGER.fine(() -> "Writing the included flat Json Node.");
            // iterate here instead of directly in writer to avoid
            // a JSON object being started since we are writing attributes directly into the
            // parent one.
            iterateAndWrite(finalNode, writer, context);
        }
    }

    protected void iterateAndWrite(
            ObjectNode objectNode, TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Iterator<String> names = objectNode.fieldNames();
        while (names != null && names.hasNext()) {
            String name = names.next();
            writeValue(name, writer, objectNode.findValue(name), context);
        }
    }

    /**
     * Iterate an ObjectNode and create a Builder from every attribute. Then each builder is
     * evaluate and its result is written. Use this if the ObjectNode being passed as one or more
     * property interpolation or expression directive and it cannot be written as it is.
     *
     * @param context the current evaluation context.
     * @param writer the writer.
     * @param node the ObjectNode to iterate and from which build nested builders.
     * @throws IOException
     */
    private void iterateAndEvaluateNestedTree(
            TemplateBuilderContext context, TemplateOutputWriter writer, ObjectNode node)
            throws IOException {
        TemplateReaderConfiguration configuration =
                new TemplateReaderConfiguration(getNamespaces());
        JSONTemplateReader jsonTemplateReader =
                new JSONTemplateReader(node, configuration, new ArrayList<>());

        TemplateBuilderMaker maker = configuration.getBuilderMaker();
        maker.namespaces(configuration.getNamespaces());
        CompositeBuilder cb = new CompositeBuilder(null, getNamespaces(), false);
        jsonTemplateReader.getBuilderFromJson(null, node, cb, maker);
        List<TemplateBuilder> children = cb.getChildren();
        if (!children.isEmpty())
            for (TemplateBuilder child : children) child.evaluate(writer, context);
    }

    // returns a new compositeBuilder in case the node is an objectNode.
    private TemplateBuilder getCurrentBuilder(JsonNode childNode, String name) {
        TemplateBuilder result = this;
        if (childNode.isObject()) {
            result = new CompositeBuilder(name, getNamespaces(), false);
            this.addChild(result);
        }
        return result;
    }

    @Override
    protected boolean canWriteValue(Object value) {
        return includingNode != null;
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        return includingNode != null;
    }

    /**
     * Returns a TemplateBuilder representing the including node of this dynamic include flat
     * builder, or null, if the including node is not an object.
     *
     * @param key
     * @return
     */
    public TemplateBuilder getIncludingNodeBuilder(String key) {
        if (!includingNode.isObject()) return null;
        return getBuilderFromNode(key, includingNode);
    }

    /**
     * Returns the overlay builder created from the sample feature. Can be used to inspect the
     * structure of a template builder, assuming other features in the lot will have a similar
     * structure.
     */
    public TemplateBuilder getIncludeFlatBuilder(String key, Feature sample) {
        // perform dynamic expansion against the sample feature, if possible
        if (sample == null) return null;

        Object evaluate = null;
        if (xpath != null) {
            evaluate = xpath.evaluate(sample);
        } else if (cql != null) {
            evaluate = cql.evaluate(sample);
        }
        if (evaluate instanceof Attribute)
            evaluate = JSONFieldSupport.parseWhenJSON(null, null, evaluate);
        if (!(evaluate instanceof JsonNode)) return null;

        return getBuilderFromNode(key, (JsonNode) evaluate);
    }

    private TemplateBuilder getBuilderFromNode(String key, JsonNode node) {
        TemplateReaderConfiguration configuration =
                new TemplateReaderConfiguration(getNamespaces());
        TemplateBuilderMaker maker = configuration.getBuilderMaker();
        maker.namespaces(configuration.getNamespaces());
        JSONTemplateReader jsonTemplateReader =
                new JSONTemplateReader(node, configuration, new ArrayList<>());

        CompositeBuilder result = new CompositeBuilder(key, getNamespaces(), false);
        jsonTemplateReader.getBuilderFromJson(null, node, result, maker);
        return result;
    }

    public JsonNode getIncludingNode() {
        return includingNode;
    }

    @Override
    public DynamicIncludeFlatBuilder copy(boolean includeChildren) {
        return new DynamicIncludeFlatBuilder(this, includeChildren);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DynamicIncludeFlatBuilder that = (DynamicIncludeFlatBuilder) o;
        return Objects.equals(includingNode, that.includingNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), includingNode);
    }
}
