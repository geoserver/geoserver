/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import java.io.IOException;
import java.util.List;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/** A generic TemplateBuilder Wrapper. */
public class TemplateBuilderWrapper extends AbstractTemplateBuilder {
    protected AbstractTemplateBuilder delegate;

    public TemplateBuilderWrapper(AbstractTemplateBuilder templateBuilder) {
        this.delegate = retypeBuilder(templateBuilder);
    }

    /**
     * Method to allows the override of the wrapped instance
     *
     * @param templateBuilder the template builder being wrapped.
     * @return the template builder retyped.
     */
    protected AbstractTemplateBuilder retypeBuilder(AbstractTemplateBuilder templateBuilder) {
        return templateBuilder;
    }

    @Override
    public AbstractTemplateBuilder copy(boolean includeChildren) {
        return delegate.copy(includeChildren);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        delegate.evaluate(writer, context);
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return delegate.accept(visitor, value);
    }

    @Override
    protected boolean evaluateFilter(TemplateBuilderContext context) {
        return delegate.evaluateFilter(context);
    }

    @Override
    public Expression getKey() {
        return delegate.getKey();
    }

    @Override
    public String getKey(TemplateBuilderContext context) {
        return delegate.getKey(context);
    }

    @Override
    public void setKey(String key) {
        delegate.setKey(key);
    }

    @Override
    public void setKey(Expression key) {
        delegate.setKey(key);
    }

    @Override
    public Filter getFilter() {
        return delegate.getFilter();
    }

    @Override
    public void setFilter(String filter) {
        delegate.setFilter(filter);
    }

    @Override
    public void setFilter(Filter filter) {
        delegate.setFilter(filter);
    }

    @Override
    public int getFilterContextPos() {
        return delegate.getFilterContextPos();
    }

    @Override
    public NamespaceSupport getNamespaces() {
        return delegate.getNamespaces();
    }

    @Override
    public List<TemplateBuilder> getChildren() {
        return delegate.getChildren();
    }

    @Override
    public EncodingHints getEncodingHints() {
        return delegate.getEncodingHints();
    }

    @Override
    public void addEncodingHint(String key, Object value) {
        delegate.addEncodingHint(key, value);
    }

    @Override
    public void addChild(TemplateBuilder builder) {
        delegate.addChild(builder);
    }

    @Override
    protected void addChildrenEvaluationToEncodingHints(
            TemplateOutputWriter writer, TemplateBuilderContext context) {
        delegate.addChildrenEvaluationToEncodingHints(writer, context);
    }

    @Override
    protected ChildrenEvaluation getChildrenEvaluation(
            TemplateOutputWriter writer, TemplateBuilderContext context) {
        return delegate.getChildrenEvaluation(writer, context);
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        return delegate.canWrite(context);
    }

    @Override
    public TemplateBuilder getParent() {
        return delegate.getParent();
    }

    @Override
    public void setParent(TemplateBuilder builder) {
        delegate.setParent(builder);
    }

    public AbstractTemplateBuilder getDelegate() {
        return delegate;
    }
}
