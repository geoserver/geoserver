/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.filter.function.JsonPointerFunction;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;

/**
 * Find JSONPath numeric comparisons and forces the number to be a double so that the indices will
 * optimize the resulting queries
 */
public class STACIndexOptimizerVisitor extends DuplicatingFilterVisitor {
    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        if (isJsonPointer(expr1)) {
            expr2 = forceDouble(expr2, extraData);
        }
        if (isJsonPointer(expr2)) {
            expr1 = forceDouble(expr1, extraData);
        }
        boolean matchCase = filter.isMatchingCase();
        return getFactory(extraData).equal(expr1, expr2, matchCase, filter.getMatchAction());
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        if (isJsonPointer(expr1)) {
            expr2 = forceDouble(expr2, extraData);
        }
        if (isJsonPointer(expr2)) {
            expr1 = forceDouble(expr1, extraData);
        }
        boolean matchCase = filter.isMatchingCase();
        return getFactory(extraData).notEqual(expr1, expr2, matchCase, filter.getMatchAction());
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        if (isJsonPointer(expr1)) {
            expr2 = forceDouble(expr2, extraData);
        }
        if (isJsonPointer(expr2)) {
            expr1 = forceDouble(expr1, extraData);
        }
        return getFactory(extraData)
                .greater(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        if (isJsonPointer(expr1)) {
            expr2 = forceDouble(expr2, extraData);
        }
        if (isJsonPointer(expr2)) {
            expr1 = forceDouble(expr1, extraData);
        }
        return getFactory(extraData)
                .greaterOrEqual(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        if (isJsonPointer(expr1)) {
            expr2 = forceDouble(expr2, extraData);
        }
        if (isJsonPointer(expr2)) {
            expr1 = forceDouble(expr1, extraData);
        }
        return getFactory(extraData)
                .less(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), extraData);
        if (isJsonPointer(expr1)) {
            expr2 = forceDouble(expr2, extraData);
        }
        if (isJsonPointer(expr2)) {
            expr1 = forceDouble(expr1, extraData);
        }
        return getFactory(extraData)
                .lessOrEqual(expr1, expr2, filter.isMatchingCase(), filter.getMatchAction());
    }

    private boolean isJsonPointer(Expression expression) {
        boolean out = false;
        if (expression instanceof JsonPointerFunction) {
            return true;
        }
        return out;
    }

    private Expression forceDouble(Expression in, Object extraData) {
        if (in instanceof Literal) {
            Literal literalExp = (Literal) in;
            if (literalExp.getValue() instanceof Number) {
                Number numberLiteral = (Number) literalExp.getValue();
                double converted = numberLiteral.doubleValue();
                return getFactory(extraData).literal(converted);
            } else {
                return in;
            }
        } else {
            return in;
        }
    }
}
