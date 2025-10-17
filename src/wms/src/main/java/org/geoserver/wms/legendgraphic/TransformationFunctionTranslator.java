/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.geoserver.wms.legendgraphic.RuleContextExtractor.RuleContext;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.style.Description;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.styling.DescriptionImpl;
import org.geotools.styling.FeatureTypeStyleImpl;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;

/**
 * A visitor that returns a copy of the style with all rules using a single transformation function (Recode, Categorize,
 * Interpolate) are replaced by equivalent ones that move the function up to the filter level, by generating multiple
 * rules. This is useful to simplify legend generation, which can handle multiple rules but not transformation
 * functions. If a rule contains multiple transformation functions (or none at all), it is left unchanged.
 */
class TransformationFunctionTranslator extends DuplicatingStyleVisitor {

    /**
     * Translates the given style by replacing rules using transformation functions with multiple rules using literal
     * values instead.
     *
     * @param style The style to translate.
     * @return A new style with the transformation functions replaced by multiple rules, or the original style if no
     *     transformation functions were found.
     */
    public static Style translate(Style style) {
        // collect transformation functions used in the style
        TransformationFunctionCollector collector = new TransformationFunctionCollector();
        style.accept(collector);
        Map<Rule, Function> targetRules = collector.getTargetRule();

        // if none is found, return the original style
        if (targetRules.isEmpty()) return style;

        // otherwise, create a translator and apply it to the style
        TransformationFunctionTranslator translator = new TransformationFunctionTranslator(targetRules);
        style.accept(translator);
        return (Style) translator.getCopy();
    }

    Map<Rule, Function> targetRules;
    RuleContext ruleContext = null;
    private final DuplicatingFilterVisitor expressionTransformer;

    private TransformationFunctionTranslator(Map<Rule, Function> targetRules) {
        this.targetRules = targetRules;
        // A filter visitor that replaces the current function with a replacement expression, wherever the
        // transformation
        // function might be located in the expression tree (it's not necessarily in the root of it, although most of
        // the time, it will be)
        this.expressionTransformer = new DuplicatingFilterVisitor() {
            @Override
            public Object visit(Function function, Object extraData) {
                // important to use Object.equals as the same function could be used twice in the same rule
                // and the parser will generate two separate (but equal) function objects
                if (ruleContext != null && Objects.equals(function, ruleContext.sourceFunction())) {
                    return ruleContext.replacementValue();
                }
                return super.visit(function, extraData);
            }
        };
    }

    /**
     * Overrides the visit method for FeatureTypeStyle to handle the fact that rules can be replaced by multiple rules,
     * and thus we need to collect them all and add them to the copy.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void visit(FeatureTypeStyle fts) {
        FeatureTypeStyle copy = new FeatureTypeStyleImpl(fts);

        List<Rule> rulesCopy = new ArrayList<>();
        for (Rule rule : fts.rules()) {
            rule.accept(this);
            Object popped = pages.pop();
            if (popped instanceof List) {
                // multiple rules generated, add them all
                rulesCopy.addAll((List<Rule>) popped);
            } else if (popped instanceof Rule rule1) {
                // single rule generated, add it
                rulesCopy.add(rule1);
            } else {
                throw new IllegalStateException("Expected a Rule or List<Rule> to be returned from rule visit, got: "
                        + (popped != null ? popped.getClass().getName() : "null"));
            }
        }
        copy.rules().clear();
        copy.rules().addAll(rulesCopy);

        if (fts.getTransformation() != null) {
            copy.setTransformation(copy(fts.getTransformation()));
        }
        if (fts.getOnlineResource() != null) {
            copy.setOnlineResource(fts.getOnlineResource());
        }
        copy.getOptions().clear();
        copy.getOptions().putAll(fts.getOptions());

        pages.push(copy);
    }

    @Override
    public void visit(Rule rule) {
        Function transformation = targetRules.get(rule);
        if (transformation == null || transformation.getParameters().size() < 3) {
            // No transformation function, nothing to do
            super.visit(rule);
            return;
        }

        try {
            List<RuleContext> contexts = RuleContextExtractor.extract(transformation);
            List<Rule> generated = new ArrayList<>();
            for (RuleContext context : contexts) {
                Rule modified = transformRule(rule, context);
                generated.add(modified);
            }

            pages.push(generated);
        } finally {
            ruleContext = null;
        }
    }

    private Rule transformRule(Rule rule, RuleContext context) {
        this.ruleContext = context;
        // duplicating the rule with the function replaced by the (hopefully!) static value
        super.visit(rule);
        Rule modified = (Rule) pages.pop();
        Description description = modified.getDescription();
        if (description == null) {
            description = new DescriptionImpl();
            description.setTitle(context.titleSuffix());
        } else if (description.getTitle() == null) {
            description.setTitle(context.titleSuffix());
        } else {
            description.setTitle(description.getTitle().toString() + context.separator() + context.titleSuffix());
        }
        return modified;
    }

    /**
     * If we're in the process of duplicating a rule with a transformation function, we need to replace occurrences of
     * the function with the replacement value for the current rule context.
     */
    @Override
    protected Expression copy(Expression expression) {
        if (expression == null) return null;
        if (ruleContext != null) return (Expression) expression.accept(expressionTransformer, ff);
        return super.copy(expression);
    }
}
