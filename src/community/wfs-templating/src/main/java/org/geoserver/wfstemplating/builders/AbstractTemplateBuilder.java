/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders;

import java.io.IOException;
import org.geoserver.wfstemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.wfstemplating.expressions.JsonLdCQLManager;
import org.geoserver.wfstemplating.writers.TemplateOutputWriter;
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

    public AbstractTemplateBuilder(String key, NamespaceSupport namespaces) {
        this.key = getKeyAsExpression(key);
        this.namespaces = namespaces;
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

    public String getKey() {
        return key != null ? key.evaluate(null).toString() : null;
    }

    public void setKey(String key) {
        this.key = getKeyAsExpression(key);
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
    public void setFilter(String filter) throws CQLException {
        JsonLdCQLManager cqlManager = new JsonLdCQLManager(filter, namespaces);
        this.filter = cqlManager.getFilterFromString();
        this.filterContextPos = cqlManager.getContextPos();
    }

    /**
     * Get context position of the filter if present
     *
     * @return
     */
    public int getFilterContextPos() {
        return filterContextPos;
    }

    /**
     * Write the attribute key if present
     *
     * @param writer the template writer
     * @throws IOException
     */
    protected void writeKey(TemplateOutputWriter writer) throws IOException {
        if (key != null && !key.evaluate(null).equals(""))
            writer.writeElementName(key.evaluate(null));
        else throw new RuntimeException("Cannot write null key value");
    }

    public NamespaceSupport getNamespaces() {
        return namespaces;
    }

    private Expression getKeyAsExpression(String key) {
        Expression keyExpr;
        if (key != null) {
            if (key.startsWith("$${")) {
                JsonLdCQLManager cqlManager = new JsonLdCQLManager(key, null);
                keyExpr = cqlManager.getExpressionFromString();
            } else {
                keyExpr = new LiteralExpressionImpl(key);
            }
        } else {
            keyExpr = null;
        }
        return keyExpr;
    }
}
