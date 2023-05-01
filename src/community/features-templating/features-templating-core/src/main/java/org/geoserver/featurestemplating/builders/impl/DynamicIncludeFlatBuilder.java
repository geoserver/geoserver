package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.readers.JSONTemplateReader;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.logging.Logging;
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

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Object evaluate = null;
        if (xpath != null) {
            evaluate = evaluateXPath(context);
        } else if (cql != null) {
            evaluate = evaluateExpressions(cql, context);
        }

        ObjectNode finalNode = null;
        if (!(evaluate instanceof ObjectNode) && evaluate != null) {
            String message = "Cannot include flat a value different from a JSON object";
            LOGGER.severe(() -> message);
            throw new UnsupportedOperationException(message);
        } else if (evaluate != null) {
            JSONMerger merger = new JSONMerger();
            finalNode = merger.mergeTrees(includingNode, (ObjectNode) evaluate);
        }

        if (finalNode != null) doIncludeFlat(finalNode, context, writer);
        else
            // write the node as it is
            iterateAndWrite((ObjectNode) includingNode, writer, context);
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

    private void iterateAndWrite(
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
        Iterator<String> names = node.fieldNames();
        while (names != null && names.hasNext()) {
            // create a builder tree from each first level attribute
            String n = names.next();
            JsonNode childNode = node.get(n);
            // make sure we have a CompositeBuilder in case of ObjectNode
            // the reader will not create it for us when passing to it directly a JSON Object
            TemplateBuilder current = getCurrentBuilder(childNode, n);
            jsonTemplateReader.getBuilderFromJson(n, node.get(n), current, maker);
        }
        List<TemplateBuilder> children = getChildren();
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
    public Object accept(TemplateVisitor visitor, Object value) {
        return super.accept(visitor, value);
    }

    @Override
    protected boolean canWriteValue(Object value) {
        return includingNode != null;
    }

    @Override
    public boolean checkNotNullValue(TemplateBuilderContext context) {
        return includingNode != null;
    }
}
