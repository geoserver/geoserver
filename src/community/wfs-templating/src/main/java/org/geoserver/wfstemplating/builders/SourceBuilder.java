/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders;

import java.util.LinkedList;
import java.util.List;
import org.geoserver.wfstemplating.builders.impl.TemplateBuilderContext;
import org.geotools.filter.AttributeExpressionImpl;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

/** Abstract class for builders that can set the context for their children through source xpath */
public abstract class SourceBuilder extends AbstractTemplateBuilder {

    private AttributeExpressionImpl source;

    protected List<TemplateBuilder> children;

    public SourceBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.children = new LinkedList<TemplateBuilder>();
    }

    /**
     * Evaluates the source against the provided template context
     *
     * @param context the current context to be used for evaluation
     * @return the new context produced by source evaluation
     */
    public TemplateBuilderContext evaluateSource(TemplateBuilderContext context) {

        if (source != null && !source.getPropertyName().equals(context.getCurrentSource())) {
            Object o = evaluateSource(context.getCurrentObj());
            TemplateBuilderContext newContext =
                    new TemplateBuilderContext(o, source.getPropertyName());
            newContext.setParent(context);
            return newContext;
        }
        return context;
    }

    /**
     * Evaluate the source against the Object passed
     *
     * @param o the value against which evaluating the source
     * @return the result of the evaluation
     */
    public Object evaluateSource(Object o) {
        return source.evaluate(o);
    }

    @Override
    public void addChild(TemplateBuilder builder) {
        this.children.add(builder);
    }

    @Override
    public List<TemplateBuilder> getChildren() {
        return children;
    }

    /**
     * Get the source as an Expression
     *
     * @return the source as an Expression
     */
    public Expression getSource() {
        return source;
    }

    /**
     * Get the source as string
     *
     * @return the source a string
     */
    public String getStrSource() {
        return source != null ? source.getPropertyName() : null;
    }

    /**
     * Set the source to the builder
     *
     * @param source the string source
     */
    public void setSource(String source) {
        this.source = new AttributeExpressionImpl(source, namespaces);
    }
}
