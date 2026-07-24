/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.geotools.api.filter.And;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.function.InFunction;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;

/**
 * Normalizes filters to reduce cache key fragmentation. Extends {@link SimplifyingFilterVisitor} (which handles basics
 * like constant folding, double-negation, AND/OR flattening) and adds:
 *
 * <ul>
 *   <li>Comparison swap: {@code Literal op PropertyName} -> {@code PropertyName inverted-op Literal}
 *   <li>AND/OR operand sort: stable ordering by (class name, ECQL of child) to absorb producer-side ordering variation
 *   <li>IN-function value sort: literal arguments in {@link InFunction} sorted lexicographically
 * </ul>
 *
 * <p>Not a complete canonicalization: semantically equivalent filters with structural differences not covered above
 * will still produce different keys. The goal is reducing fragmentation for the common cases, not eliminating it.
 */
class NormalizingFilterVisitor extends SimplifyingFilterVisitor {

    // sort children by ECQL rather than toString: toString is not a stable cross-version contract, and a change
    // there would reorder operands -> new cache keys -> mass tile orphaning. children are already normalized here.
    // the try/catch is defensive: ECQL.toCQL covers the filters used in access limits (including id filters, encoded
    // as IN (...)), the toString fallback only guards exotic types it cannot encode.
    private static String sortKey(Filter f) {
        try {
            return ECQL.toCQL(f);
        } catch (RuntimeException e) {
            return f.toString();
        }
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof BinaryComparisonOperator op && isLiteralLeft(op)) {
            return getFactory(extraData).equal(op.getExpression2(), op.getExpression1(), op.isMatchingCase());
        }
        return result;
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof BinaryComparisonOperator op && isLiteralLeft(op)) {
            return getFactory(extraData).notEqual(op.getExpression2(), op.getExpression1(), op.isMatchingCase());
        }
        return result;
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof BinaryComparisonOperator op && isLiteralLeft(op)) {
            // lit > prop -> prop < lit
            return getFactory(extraData).less(op.getExpression2(), op.getExpression1(), op.isMatchingCase());
        }
        return result;
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof BinaryComparisonOperator op && isLiteralLeft(op)) {
            // lit >= prop -> prop <= lit
            return getFactory(extraData).lessOrEqual(op.getExpression2(), op.getExpression1(), op.isMatchingCase());
        }
        return result;
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof BinaryComparisonOperator op && isLiteralLeft(op)) {
            // lit < prop -> prop > lit
            return getFactory(extraData).greater(op.getExpression2(), op.getExpression1(), op.isMatchingCase());
        }
        return result;
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof BinaryComparisonOperator op && isLiteralLeft(op)) {
            // lit <= prop -> prop >= lit
            return getFactory(extraData).greaterOrEqual(op.getExpression2(), op.getExpression1(), op.isMatchingCase());
        }
        return result;
    }

    @Override
    public Object visit(And filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof And and) {
            List<Filter> sorted = sorted(and.getChildren());
            return sorted.equals(and.getChildren())
                    ? and
                    : getFactory(extraData).and(sorted);
        }
        return result;
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        Object result = super.visit(filter, extraData);
        if (result instanceof Or or) {
            List<Filter> sorted = sorted(or.getChildren());
            return sorted.equals(or.getChildren()) ? or : getFactory(extraData).or(sorted);
        }
        return result;
    }

    @Override
    public Object visit(Function expression, Object extraData) {
        Object result = super.visit(expression, extraData);
        if (InFunction.isInFunction((Expression) result)) {
            Function fn = (Function) result;
            List<Expression> args = fn.getParameters();
            if (args.size() > 2) {
                // args[0] is the candidate; args[1..N] are the values to match - sort those
                List<Expression> values = new ArrayList<>(args.subList(1, args.size()));
                values.sort(Comparator.comparing(Object::toString));
                if (!values.equals(args.subList(1, args.size()))) {
                    List<Expression> reordered = new ArrayList<>(args.size());
                    reordered.add(args.get(0));
                    reordered.addAll(values);
                    return getFactory(extraData).function(fn.getName(), reordered.toArray(Expression[]::new));
                }
            }
        }
        return result;
    }

    private static boolean isLiteralLeft(BinaryComparisonOperator op) {
        return op.getExpression1() instanceof Literal && op.getExpression2() instanceof PropertyName;
    }

    private static List<Filter> sorted(List<Filter> children) {
        // compute each child's ECQL sort key once rather than on every comparison
        record Keyed(Filter filter, String type, String ecql) {}
        return children.stream()
                .map(f -> new Keyed(f, f.getClass().getName(), sortKey(f)))
                .sorted(Comparator.comparing(Keyed::type).thenComparing(Keyed::ecql))
                .map(Keyed::filter)
                .toList();
    }
}
