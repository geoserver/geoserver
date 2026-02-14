/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.style.Rule;
import org.geotools.filter.function.CategorizeFunction;
import org.geotools.filter.function.InterpolateFunction;
import org.geotools.filter.function.RecodeFunction;
import org.geotools.renderer.style.StyleAttributeExtractor;

/**
 * A visitor that collects all transformation functions used in the symbolizers of a style, and makes them available on
 * a rule by rule basis, as a Set.
 *
 * <p>Extends StyleAttributeExtractor to leverage its existing functionality, but it is not meant to collect attributes,
 * only transformation functions.
 */
class TransformationFunctionCollector extends StyleAttributeExtractor {

    Rule currentRule;
    Map<Rule, Set<Function>> ruleTransformations = new HashMap<>();

    @Override
    public void visit(Rule rule) {
        this.currentRule = rule;
        super.visit(rule);
        this.currentRule = null;
    }

    @Override
    public Object visit(Function f, Object data) {
        // Only interested in transformation functions inside a rule
        if (currentRule != null && isTransformationFunction(f)) {
            ruleTransformations
                    .computeIfAbsent(currentRule, r -> new HashSet<>())
                    .add(f);
        }

        return data;
    }

    /** Checks if the given function is a transformation function (Recode, Categorize, Interpolate). */
    private static boolean isTransformationFunction(Function f) {
        return f instanceof RecodeFunction || f instanceof CategorizeFunction || f instanceof InterpolateFunction;
    }

    /** Returns a map of rules to the set of transformation functions used in their symbolizers. */
    public Map<Rule, Set<Function>> getRuleFunctions() {
        return ruleTransformations;
    }

    /**
     * Returns a map of rules to the single transformation function used in their symbolizers. Only rules with exactly
     * one transformation function are included in the map.
     */
    public Map<Rule, Function> getTargetRule() {
        Map<Rule, Function> result = new HashMap<>();
        for (Map.Entry<Rule, Set<Function>> entry : ruleTransformations.entrySet()) {
            if (entry.getValue().size() == 1) {
                Function f = entry.getValue().iterator().next();
                result.put(entry.getKey(), f);
            }
        }
        return result;
    }
}
