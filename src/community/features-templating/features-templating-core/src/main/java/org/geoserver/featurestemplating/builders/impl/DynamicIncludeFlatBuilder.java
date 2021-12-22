package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicIncludeFlatBuilder extends DynamicValueBuilder {
    public DynamicIncludeFlatBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key, expression, namespaces);
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

        // updating the key and the value since we do not want to display includeFlat keyword in the
        // result
        if (evaluate instanceof ObjectNode) {
            if (((ObjectNode) evaluate).fields().hasNext()) {
                Map.Entry<String, JsonNode> includeFlatField =
                        ((ObjectNode) evaluate).fields().next();
                setKey(includeFlatField.getKey());
                evaluate = includeFlatField.getValue();
            }
        }

        if (evaluate instanceof JsonNode && hasDynamic((JsonNode) evaluate))
            writeFromNestedTree(context, writer, (JsonNode) evaluate);
        else writeValue(writer, evaluate, context);
    }

    private boolean hasDynamic(JsonNode node) {
        return node.toString().contains("${");
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return super.accept(visitor, value);
    }
}
