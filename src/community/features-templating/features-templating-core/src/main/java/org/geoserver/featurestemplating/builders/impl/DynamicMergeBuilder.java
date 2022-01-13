package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/** Responsible for merging 2 nodes if one of the evaluates to either ${ or ${{ */
public class DynamicMergeBuilder extends DynamicValueBuilder {

    private static final Logger LOGGER = Logging.getLogger(DynamicMergeBuilder.class);

    private JsonNode node;

    private boolean overlayExpression;

    public DynamicMergeBuilder(
            String key,
            String expression,
            NamespaceSupport namespaces,
            JsonNode node,
            boolean overlayExpression) {
        super(key, expression, namespaces);
        this.node = node;
        this.overlayExpression = overlayExpression;
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

        if (!(evaluate instanceof JsonNode)) {
            if (overlayExpression) {
                addChildrenEvaluationToEncodingHints(writer, context);
                writeValue(writer, evaluate, context);
            } else if (hasDynamic(node)) writeFromNestedTree(context, writer, node);
            else writeValue(writer, node, context);
        } else {
            JSONMerger jsonMerger = new JSONMerger();
            ObjectNode mergedNodes = jsonMerger.mergeTrees(node, (JsonNode) evaluate);

            // if the expression contains dynamic content
            if (hasDynamic(mergedNodes)) writeFromNestedTree(context, writer, mergedNodes);
            else // contains static content
            writeValue(writer, mergedNodes, context);
        }
    }

    private boolean hasDynamic(JsonNode node) {
        return node.toString().contains("${");
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return super.accept(visitor, value);
    }
}
