/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import static org.geoserver.featurestemplating.builders.EncodingHints.SKIP_OBJECT_ENCODING;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.expressions.TemplateCQLManager;
import org.geotools.filter.AttributeExpressionImpl;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.xml.sax.helpers.NamespaceSupport;

/** Abstract class for builders that can set the context for their children through source xpath */
public abstract class SourceBuilder extends AbstractTemplateBuilder {

    private Expression source;

    /**
     * A SourceBuilder hasNotOwnOutput when it not invoke the writer to encode any output but simply
     * call the evaluation of children builder. This is the case when the builder does not map any
     * feature attribute but part of an output format that are handle by ${@link
     * org.geoserver.featurestemplating.writers.TemplateOutputWriter#startTemplateOutput(EncodingHints)}
     */
    protected boolean ownOutput = true;

    /**
     * A SourceBuilder is topLevelFeature when its mapping the start of a Feature or of the root
     * Feature in case of complex features.
     */
    protected boolean topLevelFeature;

    public SourceBuilder(String key, NamespaceSupport namespaces, boolean topLevelFeature) {
        super(key, namespaces);
        this.children = new LinkedList<>();
        this.topLevelFeature = topLevelFeature;
    }

    protected SourceBuilder(SourceBuilder sourceBuilder, boolean includeChildren) {
        super(sourceBuilder, includeChildren);
        this.topLevelFeature = sourceBuilder.isTopLevelFeature();
        this.ownOutput = sourceBuilder.hasOwnOutput();
        this.source = sourceBuilder.getSource();
    }

    /**
     * Evaluates the source against the provided template context
     *
     * @param context the current context to be used for evaluation
     * @return the new context produced by source evaluation
     */
    public TemplateBuilderContext evaluateSource(TemplateBuilderContext context) {

        if (source != null && !getStrSource().equals(context.getCurrentSource())) {
            Object o = evaluateSource(context.getCurrentObj());
            TemplateBuilderContext newContext = new TemplateBuilderContext(o, getStrSource());
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
        if (!(source instanceof AttributeExpressionImpl)) {
            AttributeExpressionImpl sourceToEval =
                    new AttributeExpressionImpl(source.evaluate(null).toString(), namespaces);
            return sourceToEval.evaluate(o);
        }
        return source.evaluate(o);
    }

    /**
     * Get the source as an Expression
     *
     * @return the source as an Expression
     */
    public Expression getSource() {
        if (!(source instanceof AttributeExpressionImpl) && source != null) {
            return new AttributeExpressionImpl(source.evaluate(null).toString(), namespaces);
        }
        return source;
    }

    /**
     * Get the source as string
     *
     * @return the source a string
     */
    public String getStrSource() {
        if (source == null) return null;

        if (source instanceof AttributeExpressionImpl)
            return ((AttributeExpressionImpl) source).getPropertyName();
        else return Optional.ofNullable(source.evaluate(null)).map(o -> o.toString()).orElse(null);
    }

    /**
     * Set the source to the builder
     *
     * @param source the string source
     */
    public void setSource(String source) {
        TemplateCQLManager cqlManager = new TemplateCQLManager(source, namespaces);
        Expression sourceExpr = cqlManager.getExpressionFromString();
        if (sourceExpr instanceof Literal)
            this.source =
                    new AttributeExpressionImpl(sourceExpr.evaluate(null).toString(), namespaces);
        else this.source = sourceExpr;
    }

    public boolean hasOwnOutput() {
        return ownOutput;
    }

    public void setOwnOutput(boolean ownOutput) {
        this.ownOutput = ownOutput;
    }

    protected void addSkipObjectEncodingHint(TemplateBuilderContext context) {
        if (topLevelFeature) addEncodingHint(SKIP_OBJECT_ENCODING, true);
    }

    public boolean isTopLevelFeature() {
        return topLevelFeature;
    }

    public void setTopLevelFeature(boolean topLevelFeature) {
        this.topLevelFeature = topLevelFeature;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), source, ownOutput, topLevelFeature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SourceBuilder that = (SourceBuilder) o;
        return ownOutput == that.ownOutput
                && topLevelFeature == that.topLevelFeature
                && Objects.equals(source, that.source);
    }
}
