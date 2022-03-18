/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/** Responsible for merging 2 nodes if one of the evaluates to either ${ or ${{ */
public class DynamicMergeBuilder extends DynamicJsonBuilder {

    private static final Logger LOGGER = Logging.getLogger(DynamicMergeBuilder.class);

    private boolean overlayExpression;

    public DynamicMergeBuilder(
            String key,
            String expression,
            NamespaceSupport namespaces,
            JsonNode node,
            boolean overlayExpression) {
        super(key, expression, namespaces, node);
        this.overlayExpression = overlayExpression;
    }

    public DynamicMergeBuilder(DynamicMergeBuilder original, boolean includeChildren) {
        super(original, includeChildren);
        this.overlayExpression = original.overlayExpression;
    }

    @Override
    protected boolean canWriteValue(Object value) {
        if (overlayExpression) return super.canWriteValue(value);
        else return node != null;
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        if (overlayExpression) return super.canWrite(context);
        else return node != null;
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Object evaluate = evaluateDirective(context);

        if (!(evaluate instanceof JsonNode)) {
            if (overlayExpression) {
                addChildrenEvaluationToEncodingHints(writer, context);
                writeValue(writer, evaluate, context);
            } else if (hasDynamic(node)) writeFromNestedTree(context, writer, node);
            else writeValue(writer, node, context);
        } else {
            JSONMerger jsonMerger = new JSONMerger();
            JsonNode attrNode = (JsonNode) evaluate;
            validateJson(attrNode);
            ObjectNode mergedNodes;
            if (overlayExpression) mergedNodes = jsonMerger.mergeTrees(node, attrNode);
            else mergedNodes = jsonMerger.mergeTrees(attrNode, node);

            // if the expression contains dynamic content
            if (hasDynamic(mergedNodes)) writeFromNestedTree(context, writer, mergedNodes);
            else // contains static content
            writeValue(writer, mergedNodes, context);
        }
    }

    protected void writeFromNestedTree(
            TemplateBuilderContext context, TemplateOutputWriter writer, JsonNode node)
            throws IOException {
        writer.startObject(getKey(context), getEncodingHints());
        super.iterateAndEvaluateNestedTree(context, writer, node);
        writer.endObject(getKey(context), encodingHints);
    }

    @Override
    public TemplateBuilder getNestedTree(JsonNode node, TemplateBuilderContext context) {
        return getNestedTree(node, context, true);
    }

    @Override
    public DynamicMergeBuilder copy(boolean includeChildren) {
        return new DynamicMergeBuilder(this, includeChildren);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DynamicMergeBuilder that = (DynamicMergeBuilder) o;
        return overlayExpression == that.overlayExpression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), overlayExpression);
    }
}
