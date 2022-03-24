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
public class DynamicIncludeFlatBuilder extends DynamicJsonBuilder {

    private static Logger LOGGER = Logging.getLogger(DynamicIncludeFlatBuilder.class);

    public DynamicIncludeFlatBuilder(
            String expression, NamespaceSupport namespaces, JsonNode node) {
        // key is null since the $includeFlat key should not appear
        super(null, expression, namespaces, node);
    }

    public DynamicIncludeFlatBuilder(DynamicIncludeFlatBuilder original, boolean includeChildren) {
        super(original, includeChildren);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (canWrite(context)) {
            ObjectNode finalNode = getFinalJSON(context);

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
    protected ObjectNode getFinalJSON(TemplateBuilderContext context) {
        JsonNode node = getJsonNodeAttributeValue(context);
        return mergeNodes(node);
    }

    private ObjectNode mergeNodes(JsonNode evaluate) {
        ObjectNode finalNode = null;
        if (!(evaluate instanceof ObjectNode) && evaluate != null) {
            String message = "Cannot include flat a value different from a JSON object";
            LOGGER.severe(() -> message);
            throw new UnsupportedOperationException(message);
        } else if (evaluate != null) {
            JSONMerger merger = new JSONMerger();
            finalNode = merger.mergeTrees(node, (ObjectNode) evaluate);
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

    /**
     * Iterate the final object node and write it to the output.
     *
     * @param objectNode the object node.
     * @param writer the template writer.
     * @param context the template context.
     * @throws IOException
     */
    protected void iterateAndWrite(
            ObjectNode objectNode, TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Iterator<String> names = objectNode.fieldNames();
        while (names != null && names.hasNext()) {
            String name = names.next();
            writeValue(name, writer, objectNode.findValue(name), context);
        }
    }

    @Override
    protected boolean canWriteValue(Object value) {
        return node != null;
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        return node != null;
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

    /**
     * Returns a TemplateBuilder representing the including node of this dynamic include flat
     * builder, or null, if the including node is not an object.
     *
     * @param key
     * @return
     */
    public TemplateBuilder getIncludingNodeBuilder(String key) {
        if (!node.isObject()) return null;
        return getBuilderFromNode(key, node);
    }

    @Override
    public TemplateBuilder getNestedTree(JsonNode node, TemplateBuilderContext context) {
        return getNestedTree(node, context, false);
    }

    @Override
    public DynamicIncludeFlatBuilder copy(boolean includeChildren) {
        return new DynamicIncludeFlatBuilder(this, includeChildren);
    }
}
