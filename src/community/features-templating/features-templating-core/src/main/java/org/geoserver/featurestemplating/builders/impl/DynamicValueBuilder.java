/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.feature.ComplexAttributeImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/** Evaluates xpath and cql functions, writing their results to the output. */
public class DynamicValueBuilder extends AbstractTemplateBuilder {

    protected Expression cql;

    protected AttributeExpressionImpl xpath;

    protected int contextPos = 0;

    protected NamespaceSupport namespaces;

    private static final Logger LOGGER = Logging.getLogger(DynamicValueBuilder.class);

    public DynamicValueBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.namespaces = namespaces;
        TemplateCQLManager cqlManager = new TemplateCQLManager(expression, namespaces);
        if (expression.startsWith("$${")) {
            this.cql = cqlManager.getExpressionFromString();
        } else if (expression.startsWith("${")) {
            this.xpath = cqlManager.getAttributeExpressionFromString();
        } else {
            throw new IllegalArgumentException("Invalid value: " + expression);
        }
        this.contextPos = cqlManager.getContextPos();
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Object o = null;
        if (evaluateFilter(context)) {
            if (xpath != null) {
                o = evaluateXPath(context);
            } else if (cql != null) {
                o = evaluateExpressions(context);
            }
            writeValue(writer, o);
        }
    }

    /**
     * Write the value obtained by the evaluation to the output
     *
     * @param writer the template writer
     * @param value the value to write
     * @throws IOException
     */
    protected void writeValue(TemplateOutputWriter writer, Object value) throws IOException {
        if (canWriteValue(value)) {
            writeKey(writer);
            writer.writeElementValue(value);
        }
    }

    public Expression getCql() {
        return cql;
    }

    public AttributeExpressionImpl getXpath() {
        return xpath;
    }

    /**
     * Evaluate the xpath against the provided context
     *
     * @param context the context against which evaluate the xpath
     * @return the evaluation result
     */
    protected Object evaluateXPath(TemplateBuilderContext context) {
        int i = 0;
        while (i < contextPos) {
            context = context.getParent();
            i++;
        }
        Object result = null;
        try {
            result = xpath.evaluate(context.getCurrentObj());
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Unable to evaluate xpath " + xpath + ". Exception: {0}",
                    e.getMessage());
        }
        return result;
    }

    /**
     * Evaluate the Expression against the provided context
     *
     * @param context the context against which evaluate the xpath
     * @return the evaluation result
     */
    protected Object evaluateExpressions(TemplateBuilderContext context) {
        Object result = null;
        try {
            int i = 0;
            while (i < contextPos) {
                context = context.getParent();
                i++;
            }
            result = cql.evaluate(context.getCurrentObj());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Unable to evaluate expression. Exception: {0}", e.getMessage());
        }
        return result;
    }

    /**
     * Check if can write the value to the output
     *
     * @param value the value to write
     * @return true if can write the value else false
     */
    protected boolean canWriteValue(Object value) {
        if (value instanceof ComplexAttributeImpl) {
            return canWriteValue(((ComplexAttribute) value).getValue());
        } else if (value instanceof Attribute) {
            return canWriteValue(((Attribute) value).getValue());
        } else if (value instanceof List && ((List) value).size() == 0) {
            if (((List) value).size() == 0) return false;
            else return true;
        } else if (value == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    public int getContextPos() {
        return contextPos;
    }

    public boolean checkNotNullValue(TemplateBuilderContext context) {
        Object o = null;
        if (xpath != null) {

            o = evaluateXPath(context);

        } else if (cql != null) {
            o = evaluateExpressions(context);
        }
        if (o == null) return false;
        return true;
    }
}
