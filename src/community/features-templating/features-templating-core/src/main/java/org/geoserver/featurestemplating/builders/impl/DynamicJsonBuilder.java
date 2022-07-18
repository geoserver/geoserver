/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Abstraction for a DynamicBuilder which needs to perform dynamic operations over piece of JSON
 * templates and JSON values.
 */
public abstract class DynamicJsonBuilder extends DynamicValueBuilder {

    protected JsonNode node;

    protected DynamicJsonBuilder(
            String key, String expression, NamespaceSupport namespaces, JsonNode node) {
        super(key, expression, namespaces);
        this.node = node;
    }

    protected DynamicJsonBuilder(DynamicJsonBuilder original, boolean includeChildren) {
        super(original, includeChildren);
        if (original.node != null) {
            this.node = original.node.deepCopy();
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
    protected void iterateAndEvaluateNestedTree(
            TemplateBuilderContext context, TemplateOutputWriter writer, JsonNode node)
            throws IOException {
        TemplateBuilder builder = getNestedTree(node, context);
        List<TemplateBuilder> children = builder.getChildren();
        if (!children.isEmpty())
            for (TemplateBuilder child : children) child.evaluate(writer, context);
    }

    /**
     * Get the nested builders' tree resulting from the parsing of the final JsonNode.
     *
     * @param node the JsonNode node from which building the nested builders' tree.
     * @param context the builder context.
     * @return the nested builders' tree.
     */
    public abstract TemplateBuilder getNestedTree(JsonNode node, TemplateBuilderContext context);

    /**
     * Get the nested builders' tree resulting from the parsing of the final JsonNode.
     *
     * @param node the JsonNode node from which building the nested builders' tree.
     * @param context the builder context.
     * @param topLevelComplex a flag telling if the CompositeBuilder at the root of the tree should
     *     be considered as a topLevelComplex builder, hence without its own output or not.
     * @return the builder tree.
     */
    protected TemplateBuilder getNestedTree(
            JsonNode node, TemplateBuilderContext context, boolean topLevelComplex) {
        TemplateReaderConfiguration configuration =
                new TemplateReaderConfiguration(getNamespaces());
        JSONTemplateReader jsonTemplateReader =
                new JSONTemplateReader(node, configuration, new ArrayList<>());

        TemplateBuilderMaker maker = configuration.getBuilderMaker();
        maker.namespaces(configuration.getNamespaces());
        String key = getKey(context);
        CompositeBuilder cb = new CompositeBuilder(key, getNamespaces(), topLevelComplex);
        jsonTemplateReader.getBuilderFromJson(key, node, cb, maker);
        return cb;
    }

    /**
     * Get a JsonNode from the evaluation.
     *
     * @param context the builder context.
     * @return the JsonNode, result of the evaluation.
     */
    protected JsonNode getJsonNodeAttributeValue(TemplateBuilderContext context) {
        Object evaluate = evaluateDirective(context);
        JsonNode result = null;
        if (evaluate != null) {
            if (!(evaluate instanceof JsonNode))
                throw new UnsupportedOperationException("The selected attribute is not a JSON");
            result = (JsonNode) evaluate;
            validateJson(result);
        }

        return result;
    }

    /**
     * Validate the JsonNode obtained from the Feature to be sure doesn't hold any template
     * directive.
     *
     * @param node the JsonNode to validate.
     */
    protected void validateJson(JsonNode node) {
        if (node.toString().contains("${")) {
            throw new UnsupportedOperationException(
                    "A json attribute value cannot have a template directive among its fields.");
        }
    }

    /**
     * Get the base JsonNode.
     *
     * @return the node.
     */
    public JsonNode getNode() {
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DynamicJsonBuilder that = (DynamicJsonBuilder) o;
        return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), node);
    }
}
