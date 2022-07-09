/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/** A builder able to execute an $includeFlat{} directive in a json array. */
public class ArrayIncludeFlatBuilder extends DynamicJsonBuilder {

    private static Logger LOGGER = Logging.getLogger(DynamicIncludeFlatBuilder.class);

    public ArrayIncludeFlatBuilder(
            String key, String expression, NamespaceSupport namespaces, JsonNode node) {
        super(key, expression, namespaces, node);
    }

    public ArrayIncludeFlatBuilder(DynamicJsonBuilder original, boolean includeChildren) {
        super(original, includeChildren);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (canWrite(context)) {
            ArrayNode finalNode = getFinalJSON(context);

            if (finalNode != null) doIncludeFlat(finalNode, context, writer);
            else iterateAndEvaluateNestedTree(context, writer, node);
        }
    }

    /**
     * Get the final JSON after the inclusion is performed
     *
     * @param context the template context.
     * @return the JsonNode.
     */
    protected ArrayNode getFinalJSON(TemplateBuilderContext context) {
        return mergeNodes(context);
    }

    private ArrayNode mergeNodes(TemplateBuilderContext context) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ArrayNode finalNode = factory.arrayNode();
        for (TemplateBuilder builder : getChildren()) {
            // each child is always a DynamicValueBuilder
            DynamicValueBuilder dynamicBuilder = (DynamicValueBuilder) builder;
            Object evaluate = dynamicBuilder.evaluateDirective(context);
            if (!(evaluate instanceof ArrayNode) && evaluate != null) {
                String message =
                        "Cannot include flat a value different from a JSON Array into the containing array";
                LOGGER.severe(() -> message);
                throw new UnsupportedOperationException(message);
            } else if (evaluate != null) {
                ArrayNode arrayNode = (ArrayNode) evaluate;
                finalNode.addAll(arrayNode);
            } else if (evaluate == null) {
                if (dynamicBuilder.isEncodeNull()) finalNode.add(factory.nullNode());
            }
        }
        finalNode.addAll((ArrayNode) node);

        return finalNode;
    }

    private void doIncludeFlat(
            ArrayNode finalNode, TemplateBuilderContext context, TemplateOutputWriter writer)
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

    /**
     * Iterate the final array node and write it to the output.
     *
     * @param arrayResult the array node.
     * @param writer the template writer.
     * @param context the template context.
     * @throws IOException
     */
    protected void iterateAndWrite(
            ArrayNode arrayResult, TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        String key = getKey(context);
        writer.writeElementNameAndValue(key, arrayResult, getEncodingHints());
    }

    @Override
    protected boolean canWriteValue(Object value) {
        return node != null;
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        return node != null;
    }

    @Override
    public TemplateBuilder getNestedTree(JsonNode node, TemplateBuilderContext context) {

        return getNestedTree(node, context, false);
    }

    @Override
    public ArrayIncludeFlatBuilder copy(boolean includeChildren) {
        return new ArrayIncludeFlatBuilder(this, includeChildren);
    }
}
