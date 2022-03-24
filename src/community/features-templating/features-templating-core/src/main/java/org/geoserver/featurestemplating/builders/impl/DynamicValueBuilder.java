/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.*;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
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

    private boolean encodeNull = false;

    private static final Logger LOGGER = Logging.getLogger(DynamicValueBuilder.class);

    public DynamicValueBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key, namespaces);

        if (expression.endsWith("!")) {
            this.encodeNull = true;
            expression = expression.substring(0, expression.length() - 1);
        }

        TemplateCQLManager cqlManager = new TemplateCQLManager(expression, namespaces);
        if (expression.startsWith("$${")) {
            this.cql = cqlManager.getExpressionFromString();
        } else if (expression.equals("${.}")) {
            this.cql = cqlManager.getThis();
        } else if (expression.startsWith("${")) {
            this.xpath = cqlManager.getAttributeExpressionFromString();
        } else {
            throw new IllegalArgumentException("Invalid value: " + expression);
        }
        this.contextPos = cqlManager.getContextPos();
    }

    public DynamicValueBuilder(DynamicValueBuilder dynamicBuilder, boolean includeChildren) {
        super(dynamicBuilder, includeChildren);
        this.cql = dynamicBuilder.getCql();
        this.xpath = dynamicBuilder.getXpath();
        this.encodeNull = dynamicBuilder.isEncodeNull();
        this.contextPos = dynamicBuilder.getContextPos();
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (evaluateFilter(context)) {
            Object o = null;
            if (xpath != null) {
                o = evaluateXPath(context);
            } else if (cql != null) {
                o = evaluateExpressions(cql, context);
            }
            addChildrenEvaluationToEncodingHints(writer, context);
            writeValue(writer, o, context);
        }
    }

    /**
     * Write the value obtained by the evaluation to the output
     *
     * @param writer the template writer
     * @param value the value to write
     * @param context
     * @throws IOException
     */
    protected void writeValue(
            TemplateOutputWriter writer, Object value, TemplateBuilderContext context)
            throws IOException {
        writeValue(null, writer, value, context);
    }

    protected void writeValue(
            String name, TemplateOutputWriter writer, Object value, TemplateBuilderContext context)
            throws IOException {
        if (encodeNull || canWriteValue(value)) {
            EncodingHints encodingHints = getEncodingHints();
            if (name == null) name = getKey(context);
            writer.writeElementNameAndValue(name, value, encodingHints);
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
    // TODO: thi and evaluateExpression are almost identical. Can they be merged?
    protected Object evaluateXPath(TemplateBuilderContext context) {
        int i = 0;
        while (i < contextPos) {
            context = context.getParent();
            i++;
        }
        Object result = null;
        try {
            Object contextObject = getContextObject(context);
            result = xpath.evaluate(contextObject);
            result = JSONFieldSupport.parseWhenJSON(xpath, contextObject, result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to evaluate xpath " + xpath + ". Exception: {0}", e);
        }
        return result;
    }

    /**
     * Evaluate the Expression against the provided context
     *
     * @param expression
     * @param context the context against which evaluate the xpath
     * @return the evaluation result
     */
    protected Object evaluateExpressions(Expression expression, TemplateBuilderContext context) {
        Object result = null;
        try {
            int i = 0;
            while (i < contextPos) {
                context = context.getParent();
                i++;
            }
            Object contextObject = context.getCurrentObj();
            result = expression.evaluate(contextObject);
            result = JSONFieldSupport.parseWhenJSON(cql, contextObject, result);
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
        } else if (value == null || value.equals("null") || "".equals(value)) {
            return false;
        } else {
            return true;
        }
    }

    public int getContextPos() {
        return contextPos;
    }

    public boolean canWrite(TemplateBuilderContext context) {
        if (encodeNull) return true;
        Object o = null;
        if (xpath != null) {
            o = evaluateXPath(context);
        } else if (cql != null) {
            o = evaluateExpressions(cql, context);
        }
        if (o == null) return false;
        return true;
    }

    public void setCql(Expression cql) {
        this.cql = cql;
    }

    public void setXpath(AttributeExpressionImpl xpath) {
        this.xpath = xpath;
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return visitor.visit(this, value);
    }

    protected Object getContextObject(TemplateBuilderContext context) {
        Object contextObject = context.getCurrentObj();
        if (contextObject != null && contextObject instanceof List) {
            List<Object> multipleValue = (List<Object>) contextObject;
            if (!multipleValue.isEmpty()) {
                contextObject = multipleValue.get(0);
            }
        }
        return contextObject;
    }

    protected boolean hasDynamic(JsonNode node) {
        return node.toString().contains("${");
    }

    public boolean isEncodeNull() {
        return encodeNull;
    }

    @Override
    public DynamicValueBuilder copy(boolean includeChildren) {
        return new DynamicValueBuilder(this, includeChildren);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cql, xpath, contextPos, namespaces, encodeNull);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DynamicValueBuilder that = (DynamicValueBuilder) o;
        return contextPos == that.contextPos
                && encodeNull == that.encodeNull
                && Objects.equals(cql, that.cql)
                && Objects.equals(xpath, that.xpath)
                && Objects.equals(namespaces, that.namespaces);
    }
}
