/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Abstract implementation of {@link TemplateBuilder} who groups some common attributes and methods.
 */
public abstract class AbstractTemplateBuilder implements TemplateBuilder {

    protected Expression key;

    protected Filter filter;

    protected int filterContextPos = 0;

    protected NamespaceSupport namespaces;

    protected List<TemplateBuilder> children;

    protected EncodingHints encodingHints;

    public AbstractTemplateBuilder(String key, NamespaceSupport namespaces) {
        this.key = getKeyAsExpression(key, namespaces);
        this.namespaces = namespaces;
        this.children = new ArrayList<>();
    }

    /**
     * Evaluate the filter against the provided template context
     *
     * @param context the current context
     * @return return the result of filter evaluation
     */
    protected boolean evaluateFilter(TemplateBuilderContext context) {
        if (filter == null) return true;
        TemplateBuilderContext evaluationContenxt = context;
        for (int i = 0; i < filterContextPos; i++) {
            evaluationContenxt = evaluationContenxt.getParent();
        }
        return filter.evaluate(evaluationContenxt.getCurrentObj());
    }

    public Expression getKey() {
        return key;
    }

    public String getKey(TemplateBuilderContext context) {
        if (key == null) return null;
        Object currentObj = context != null ? context.getCurrentObj() : null;
        return key.evaluate(currentObj, String.class);
    }

    public void setKey(String key) {
        this.key = getKeyAsExpression(key, null);
    }

    /**
     * Get the filter if present
     *
     * @return
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set the filter to the builder
     *
     * @param filter the filter to be setted
     * @throws CQLException
     */
    public void setFilter(String filter) {
        TemplateCQLManager cqlManager = new TemplateCQLManager(filter, namespaces);
        try {
            this.filter = cqlManager.getFilterFromString();
        } catch (CQLException e) {
            throw new RuntimeException(e);
        }
        this.filterContextPos = cqlManager.getContextPos();
    }

    /**
     * Set the filter to the builder
     *
     * @param filter the filter to be setted
     * @throws CQLException
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Get context position of the filter if present
     *
     * @return
     */
    public int getFilterContextPos() {
        return filterContextPos;
    }

    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    private Expression getKeyAsExpression(String key, NamespaceSupport namespaces) {
        Expression keyExpr;
        if (key != null) {
            if (key.startsWith("$${") || key.startsWith("${")) {
                TemplateCQLManager cqlManager = new TemplateCQLManager(key, namespaces);
                if (key.startsWith("$${")) {
                    return cqlManager.getExpressionFromString();
                } else if (key.equals("${.}")) {
                    return cqlManager.getThis();
                } else if (key.startsWith("${")) {
                    return cqlManager.getAttributeExpressionFromString();
                } else {
                    // should not really happen, but need to pacify the compiler
                    throw new IllegalArgumentException("Invalid key: " + key);
                }
            } else {
                keyExpr = new LiteralExpressionImpl(key);
            }
        } else {
            keyExpr = null;
        }
        return keyExpr;
    }

    @Override
    public List<TemplateBuilder> getChildren() {
        return children;
    }

    @Override
    public EncodingHints getEncodingHints() {
        if (this.encodingHints == null) this.encodingHints = new EncodingHints();
        return encodingHints;
    }

    @Override
    public void addEncodingHint(String key, Object value) {
        if (this.encodingHints == null) this.encodingHints = new EncodingHints();
        this.encodingHints.put(key, value);
    }

    @Override
    public void addChild(TemplateBuilder builder) {
        if (this.children == null) this.children = new ArrayList<>();
        this.children.add(builder);
    }

    protected void addChildrenEvaluationToEncodingHints(
            TemplateOutputWriter writer, TemplateBuilderContext context) {
        if (children != null && !children.isEmpty()) {
            ChildrenEvaluation childrenEvaluation = getChildrenEvaluation(writer, context);
            getEncodingHints().put(EncodingHints.CHILDREN_EVALUATION, childrenEvaluation);
        }
    }

    protected ChildrenEvaluation getChildrenEvaluation(
            TemplateOutputWriter writer, TemplateBuilderContext context) {
        ChildrenEvaluation action =
                () -> {
                    for (TemplateBuilder b : children) {
                        b.evaluate(writer, context);
                    }
                };
        return action;
    }

    @FunctionalInterface
    public interface ChildrenEvaluation {
        void evaluate() throws IOException;
    }
}
