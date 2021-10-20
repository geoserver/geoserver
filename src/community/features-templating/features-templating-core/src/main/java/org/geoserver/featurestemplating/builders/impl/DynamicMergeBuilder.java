package org.geoserver.featurestemplating.builders.impl;

import static org.geoserver.featurestemplating.readers.JSONMerger.DYNAMIC_MERGE_KEY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.JSONFieldSupport;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.readers.JSONMerger;
import org.geoserver.featurestemplating.readers.JSONTemplateReaderUtil;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicMergeBuilder extends DynamicValueBuilder {

    private static final Logger LOGGER = Logging.getLogger(DynamicMergeBuilder.class);

    private JsonNode base;

    public DynamicMergeBuilder(
            String key, String expression, NamespaceSupport namespaces, JsonNode base) {
        super(key, expression, namespaces);
        this.base = base;
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Object evaluate = null;
        if (xpath != null) {
            evaluate = xpath.evaluate(context.getCurrentObj());
        } else if (cql != null) {
            evaluate = cql.evaluate(context.getCurrentObj());
        }

        Object result = null;
        try {
            int i = 0;
            while (i < contextPos) {
                context = context.getParent();
                i++;
            }
            Object contextObject = context.getCurrentObj();
            Expression expression = null;
            if (cql != null) {
                expression = cql;
            } else if (xpath != null) {
                expression = xpath;
            }
            result = expression.evaluate(contextObject);
            result = JSONFieldSupport.parseWhenJSON(expression, contextObject, result);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Unable to evaluate expression. Exception: {0}", e.getMessage());
        }

        if (!(result instanceof JsonNode)) {
            addChildrenEvaluationToEncodingHints(writer, context);
            writeValue(writer, result, context);
        } else {
            BaseJsonNode overlay = null;
            try {
                overlay = (BaseJsonNode) new ObjectMapper().readTree(String.valueOf(evaluate));
            } catch (Exception e) {
                LOGGER.info("Overlay could not be created :" + e.getLocalizedMessage());
            }

            JSONMerger jsonMerger = new JSONMerger();
            ObjectNode mergedNodes = jsonMerger.mergeTrees(base, overlay);

            if (mergedNodes.toString().contains("${")) {
                TemplateReaderConfiguration configuration =
                        new TemplateReaderConfiguration(getNamespaces());
                JSONTemplateReaderUtil jsonTemplateReaderUtil =
                        new JSONTemplateReaderUtil(mergedNodes, configuration);
                TemplateBuilder templateBuilder =
                        jsonTemplateReaderUtil.getBuilderFromJson(mergedNodes);
                templateBuilder.evaluate(writer, context);

            } else {
                context.setCurrentObj(null);
                StaticBuilder staticBuilder =
                        new StaticBuilder(DYNAMIC_MERGE_KEY, mergedNodes, getNamespaces());
                staticBuilder.evaluate(writer, context);
            }
        }
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        System.out.println();
        return super.accept(visitor, value);
    }
}
