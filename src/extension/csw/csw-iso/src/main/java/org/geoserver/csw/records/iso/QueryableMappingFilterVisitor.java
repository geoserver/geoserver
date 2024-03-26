/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.And;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.FilterVisitor;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.api.filter.MultiValuedFilter.MatchAction;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsNil;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.PropertyIsNull;
import org.geotools.api.filter.expression.Add;
import org.geotools.api.filter.expression.BinaryExpression;
import org.geotools.api.filter.expression.Divide;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.ExpressionVisitor;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.Multiply;
import org.geotools.api.filter.expression.NilExpression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.expression.Subtract;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Beyond;
import org.geotools.api.filter.spatial.BinarySpatialOperator;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Crosses;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Equals;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Overlaps;
import org.geotools.api.filter.spatial.Touches;
import org.geotools.api.filter.spatial.Within;
import org.geotools.api.filter.temporal.After;
import org.geotools.api.filter.temporal.AnyInteracts;
import org.geotools.api.filter.temporal.Before;
import org.geotools.api.filter.temporal.Begins;
import org.geotools.api.filter.temporal.BegunBy;
import org.geotools.api.filter.temporal.BinaryTemporalOperator;
import org.geotools.api.filter.temporal.During;
import org.geotools.api.filter.temporal.EndedBy;
import org.geotools.api.filter.temporal.Ends;
import org.geotools.api.filter.temporal.Meets;
import org.geotools.api.filter.temporal.MetBy;
import org.geotools.api.filter.temporal.OverlappedBy;
import org.geotools.api.filter.temporal.TContains;
import org.geotools.api.filter.temporal.TEquals;
import org.geotools.api.filter.temporal.TOverlaps;
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.factory.GeoTools;

/**
 * This Filter Visitor will replace queryables by their mapped XPaths. In the case that queryables
 * are mapped to more than one XPath, the visitor will automatically multiply the relevant part of
 * the filter and combine them with the logical "OR" operator.
 */
public class QueryableMappingFilterVisitor implements FilterVisitor, ExpressionVisitor {

    protected final FilterFactory ff;

    protected AttributeDescriptor featureDescriptor;

    protected Map<String, List<PropertyName>> queryableMapping;

    public QueryableMappingFilterVisitor(
            AttributeDescriptor featureDescriptor,
            Map<String, List<PropertyName>> queryableMapping) {
        this(
                CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints()),
                featureDescriptor,
                queryableMapping);
    }

    public QueryableMappingFilterVisitor(
            FilterFactory ff,
            AttributeDescriptor featureDescriptor,
            Map<String, List<PropertyName>> queryableMapping) {
        this.ff = ff;
        this.featureDescriptor = featureDescriptor;
        this.queryableMapping = queryableMapping;
    }

    /** Null safe expression cloning */
    protected Expression visit(Expression expression, Object extraData) {
        if (expression == null) return null;
        return (Expression) expression.accept(this, extraData);
    }

    /**
     * This interface is in support of a generic function (combine) that gets rid of the
     * multi-valued literals, with any type of filter that takes two expressions.
     */
    protected static interface FilterReplacer<F extends Filter> {

        public Expression getExpression1(F filter);

        public Expression getExpression2(F filter);

        /** Replace the expressions in a filter */
        public Filter replaceExpressions(F filter, Expression expression1, Expression expression2);
    }

    /**
     * This interface is in support of a generic function (combine) that gets rid of the
     * multi-valued literals, with any type of filter that takes two expressions.
     */
    protected static interface ExpressionReplacer<E extends Expression> {

        public Expression getExpression1(E expr);

        public Expression getExpression2(E expr);

        /** Replace the expressions in a filter */
        public Expression replaceExpressions(
                E expression, Expression expression1, Expression expression2);
    }

    /**
     * An implementation for Binary Comparison Operators Takes the method name in the FilterFactory
     * to create the filter
     */
    protected class BinaryComparisonOperatorReplacer
            implements FilterReplacer<BinaryComparisonOperator> {

        protected Method method;

        public BinaryComparisonOperatorReplacer(String methodName) {

            try {
                method =
                        ff.getClass()
                                .getMethod(
                                        methodName,
                                        Expression.class,
                                        Expression.class,
                                        boolean.class,
                                        MatchAction.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Expression getExpression1(BinaryComparisonOperator filter) {
            return filter.getExpression1();
        }

        @Override
        public Expression getExpression2(BinaryComparisonOperator filter) {
            return filter.getExpression2();
        }

        @Override
        public Filter replaceExpressions(
                BinaryComparisonOperator filter, Expression expression1, Expression expression2) {
            try {
                return (Filter)
                        method.invoke(
                                ff,
                                expression1,
                                expression2,
                                filter.isMatchingCase(),
                                filter.getMatchAction());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * An implementation for Binary Spatial Operators Takes the method name in the FilterFactory to
     * create the filter
     */
    protected class BinarySpatialOperatorReplacer implements FilterReplacer<BinarySpatialOperator> {

        protected Method method;

        public BinarySpatialOperatorReplacer(String methodName) {

            try {
                method =
                        ff.getClass()
                                .getMethod(
                                        methodName,
                                        Expression.class,
                                        Expression.class,
                                        MatchAction.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Expression getExpression1(BinarySpatialOperator filter) {
            return filter.getExpression1();
        }

        @Override
        public Expression getExpression2(BinarySpatialOperator filter) {
            return filter.getExpression2();
        }

        @Override
        public Filter replaceExpressions(
                BinarySpatialOperator filter, Expression expression1, Expression expression2) {
            try {
                return (Filter)
                        method.invoke(ff, expression1, expression2, filter.getMatchAction());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * An implementation for Binary Expression takes the method name in the FilterFactory to create
     * the filter
     */
    protected class BinaryExpressionReplacer implements ExpressionReplacer<BinaryExpression> {

        protected Method method;

        public BinaryExpressionReplacer(String methodName) {
            try {
                method =
                        ff.getClass()
                                .getMethod(
                                        methodName,
                                        Expression.class,
                                        Expression.class,
                                        boolean.class,
                                        MatchAction.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Expression getExpression1(BinaryExpression expr) {
            return expr.getExpression1();
        }

        @Override
        public Expression getExpression2(BinaryExpression expr) {
            return expr.getExpression2();
        }

        @Override
        public Expression replaceExpressions(
                BinaryExpression expr, Expression expression1, Expression expression2) {
            try {
                return (Expression) method.invoke(ff, expression1, expression2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * An implementation for Binary Temporal Operators Takes the method name in the FilterFactory to
     * create the filter
     */
    protected class BinaryTemporalOperatorReplacer
            implements FilterReplacer<BinaryTemporalOperator> {

        protected Method method;

        public BinaryTemporalOperatorReplacer(String methodName) {

            try {
                method =
                        ff.getClass()
                                .getMethod(
                                        methodName,
                                        Expression.class,
                                        Expression.class,
                                        MatchAction.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Expression getExpression1(BinaryTemporalOperator filter) {
            return filter.getExpression1();
        }

        @Override
        public Expression getExpression2(BinaryTemporalOperator filter) {
            return filter.getExpression2();
        }

        @Override
        public Filter replaceExpressions(
                BinaryTemporalOperator filter, Expression expression1, Expression expression2) {
            try {
                return (Filter)
                        method.invoke(ff, expression1, expression2, filter.getMatchAction());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * combines filter applied to multiple expressions using OR
     *
     * @param filter The filter
     * @param replacer The filter replacer
     * @return the new filter
     */
    protected <T extends Filter> Filter combine(
            T filter, FilterReplacer<T> replacer, Object extraData) {

        Expression one = replacer.getExpression1(filter);
        Expression two = replacer.getExpression2(filter);

        @SuppressWarnings("unchecked")
        List<Expression> demOne = (List<Expression>) one.accept(this, extraData);
        @SuppressWarnings("unchecked")
        List<Expression> demTwo = (List<Expression>) two.accept(this, extraData);

        List<Filter> filters = new ArrayList<>(); // list of all filters
        for (Expression exprOne : demOne) {
            for (Expression exprTwo : demTwo) {
                filters.add(replacer.replaceExpressions(filter, exprOne, exprTwo));
            }
        }
        return ff.or(filters);
    }

    /**
     * combines expression applied to multiple expressions using OR
     *
     * @param expression The expression
     * @param replacer The expression replacer
     * @return the new expression
     */
    protected <T extends Expression> List<Expression> combine(
            T expression, ExpressionReplacer<T> replacer, Object extraData) {

        Expression one = replacer.getExpression1(expression);
        Expression two = replacer.getExpression2(expression);

        @SuppressWarnings("unchecked")
        List<Expression> repOne = (List<Expression>) one.accept(this, extraData);
        @SuppressWarnings("unchecked")
        List<Expression> repTwo = (List<Expression>) two.accept(this, extraData);

        List<Expression> expressions = new ArrayList<>(); // list of all filters
        for (Expression exprOne : repOne) {
            for (Expression exprTwo : repTwo) {
                expressions.add(replacer.replaceExpressions(expression, exprOne, exprTwo));
            }
        }

        return expressions;
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        Expression one = filter.getExpression();
        Expression two = filter.getLowerBoundary();
        Expression three = filter.getUpperBoundary();

        @SuppressWarnings("unchecked")
        List<Expression> repOne = (List<Expression>) one.accept(this, extraData);
        @SuppressWarnings("unchecked")
        List<Expression> repTwo = (List<Expression>) two.accept(this, extraData);
        @SuppressWarnings("unchecked")
        List<Expression> repThree = (List<Expression>) three.accept(this, extraData);

        List<Filter> filters = new ArrayList<>(); // list of all filters
        for (Expression exprOne : repOne) {
            for (Expression exprTwo : repTwo) {
                for (Expression exprThree : repThree) {
                    filters.add(ff.between(exprOne, exprTwo, exprThree));
                }
            }
        }

        return ff.or(filters);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        return combine(filter, new BinaryComparisonOperatorReplacer("equal"), extraData);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        return combine(filter, new BinaryComparisonOperatorReplacer("notEqual"), extraData);
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        return combine(filter, new BinaryComparisonOperatorReplacer("greater"), extraData);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        return combine(filter, new BinaryComparisonOperatorReplacer("greaterOrEqual"), extraData);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        return combine(filter, new BinaryComparisonOperatorReplacer("less"), extraData);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        return combine(filter, new BinaryComparisonOperatorReplacer("lessOrEqual"), extraData);
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("bbox"), extraData);
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        return combine(
                filter,
                new FilterReplacer<
                        Beyond>() { // beyond filter takes extra properties, therefore needs its own
                    // filterreplacer

                    @Override
                    public Expression getExpression1(Beyond filter) {
                        return filter.getExpression1();
                    }

                    @Override
                    public Expression getExpression2(Beyond filter) {
                        return filter.getExpression2();
                    }

                    @Override
                    public Filter replaceExpressions(
                            Beyond filter, Expression expression1, Expression expression2) {
                        return ff.beyond(
                                expression1,
                                expression2,
                                filter.getDistance(),
                                filter.getDistanceUnits(),
                                filter.getMatchAction());
                    }
                },
                extraData);
    }

    @Override
    public Object visit(Contains filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("contains"), extraData);
    }

    @Override
    public Object visit(Crosses filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("crosses"), extraData);
    }

    @Override
    public Object visit(Disjoint filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("disjoint"), extraData);
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        return combine(
                filter,
                new FilterReplacer<
                        DWithin>() { // DWithin filter takes extra properties, therefore needs its
                    // own filterreplacer

                    @Override
                    public Expression getExpression1(DWithin filter) {
                        return filter.getExpression1();
                    }

                    @Override
                    public Expression getExpression2(DWithin filter) {
                        return filter.getExpression2();
                    }

                    @Override
                    public Filter replaceExpressions(
                            DWithin filter, Expression expression1, Expression expression2) {
                        return ff.dwithin(
                                expression1,
                                expression2,
                                filter.getDistance(),
                                filter.getDistanceUnits(),
                                filter.getMatchAction());
                    }
                },
                extraData);
    }

    @Override
    public Object visit(Equals filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("equal"), extraData);
    }

    @Override
    public Object visit(Intersects filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("intersects"), extraData);
    }

    @Override
    public Object visit(Overlaps filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("overlaps"), extraData);
    }

    @Override
    public Object visit(Touches filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("touches"), extraData);
    }

    @Override
    public Object visit(Within filter, Object extraData) {
        return combine(filter, new BinarySpatialOperatorReplacer("within"), extraData);
    }

    @Override
    public Object visit(After after, Object extraData) {
        return combine(after, new BinaryTemporalOperatorReplacer("after"), extraData);
    }

    @Override
    public Object visit(AnyInteracts anyInteracts, Object extraData) {
        return combine(anyInteracts, new BinaryTemporalOperatorReplacer("anyInteracts"), extraData);
    }

    @Override
    public Object visit(Before before, Object extraData) {
        return combine(before, new BinaryTemporalOperatorReplacer("before"), extraData);
    }

    @Override
    public Object visit(Begins begins, Object extraData) {
        return combine(begins, new BinaryTemporalOperatorReplacer("begins"), extraData);
    }

    @Override
    public Object visit(BegunBy begunBy, Object extraData) {
        return combine(begunBy, new BinaryTemporalOperatorReplacer("begunBy"), extraData);
    }

    @Override
    public Object visit(During during, Object extraData) {
        return combine(during, new BinaryTemporalOperatorReplacer("during"), extraData);
    }

    @Override
    public Object visit(EndedBy endedBy, Object extraData) {
        return combine(endedBy, new BinaryTemporalOperatorReplacer("endedBy"), extraData);
    }

    @Override
    public Object visit(Ends ends, Object extraData) {
        return combine(ends, new BinaryTemporalOperatorReplacer("ends"), extraData);
    }

    @Override
    public Object visit(Meets meets, Object extraData) {
        return combine(meets, new BinaryTemporalOperatorReplacer("meets"), extraData);
    }

    @Override
    public Object visit(MetBy metBy, Object extraData) {
        return combine(metBy, new BinaryTemporalOperatorReplacer("metBy"), extraData);
    }

    @Override
    public Object visit(OverlappedBy overlappedBy, Object extraData) {
        return combine(overlappedBy, new BinaryTemporalOperatorReplacer("overlappedBy"), extraData);
    }

    @Override
    public Object visit(TContains contains, Object extraData) {
        return combine(contains, new BinaryTemporalOperatorReplacer("tcontains"), extraData);
    }

    @Override
    public Object visit(TEquals equals, Object extraData) {
        return combine(equals, new BinaryTemporalOperatorReplacer("tequals"), extraData);
    }

    @Override
    public Object visit(TOverlaps overlaps, Object extraData) {
        return combine(overlaps, new BinaryTemporalOperatorReplacer("toverlaps"), extraData);
    }

    @Override
    public Object visitNullFilter(Object extraData) {
        return null;
    }

    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        return Filter.EXCLUDE;
    }

    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        return Filter.INCLUDE;
    }

    @Override
    public Object visit(And filter, Object extraData) {
        List<Filter> children = filter.getChildren();
        List<Filter> newChildren = new ArrayList<>();
        for (Filter child : children) {
            if (child != null) {
                Filter newChild = (Filter) child.accept(this, extraData);
                newChildren.add(newChild);
            }
        }
        return ff.and(newChildren);
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        List<Filter> children = filter.getChildren();
        List<Filter> newChildren = new ArrayList<>();
        for (Filter child : children) {
            if (child != null) {
                Filter newChild = (Filter) child.accept(this, extraData);
                newChildren.add(newChild);
            }
        }
        return ff.or(newChildren);
    }

    @Override
    public Object visit(Not filter, Object extraData) {
        return ff.not((Filter) filter.getFilter().accept(this, extraData));
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        return ff.id(filter.getIdentifiers());
    }

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        @SuppressWarnings("unchecked")
        List<Expression> repExpr =
                (List<Expression>) filter.getExpression().accept(this, extraData);

        List<Filter> filters = new ArrayList<>(); // list of all filters
        for (Expression expr : repExpr) {
            filters.add(
                    ff.like(
                            expr,
                            filter.getLiteral(),
                            filter.getWildCard(),
                            filter.getSingleChar(),
                            filter.getEscape(),
                            filter.isMatchingCase(),
                            filter.getMatchAction()));
        }
        return ff.or(filters);
    }

    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        @SuppressWarnings("unchecked")
        List<Expression> repExpr =
                (List<Expression>) filter.getExpression().accept(this, extraData);

        List<Filter> filters = new ArrayList<>(); // list of all filters
        for (Expression expr : repExpr) {
            filters.add(ff.isNull(expr));
        }
        return ff.or(filters);
    }

    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        @SuppressWarnings("unchecked")
        List<Expression> repExpr =
                (List<Expression>) filter.getExpression().accept(this, extraData);

        List<Filter> filters = new ArrayList<>(); // list of all filters
        for (Expression expr : repExpr) {
            filters.add(ff.isNil(expr, extraData));
        }
        return ff.or(filters);
    }

    @Override
    public Object visit(NilExpression expression, Object extraData) {
        return Expression.NIL;
    }

    @Override
    public Object visit(Add add, Object extraData) {
        return combine(add, new BinaryExpressionReplacer("add"), extraData);
    }

    @Override
    public Object visit(Divide divide, Object extraData) {
        return combine(divide, new BinaryExpressionReplacer("divide"), extraData);
    }

    @Override
    public Object visit(Function func, Object extraData) {
        List<List<Expression>> parameters = new ArrayList<>();
        for (Expression param : func.getParameters()) {
            @SuppressWarnings("unchecked")
            List<Expression> repExpr = (List<Expression>) param.accept(this, extraData);

            List<List<Expression>> newParameters = new ArrayList<>();
            for (Expression newParam : repExpr) {
                if (!parameters.isEmpty()) {
                    for (List<Expression> parameterList : parameters) {
                        List<Expression> list = new ArrayList<>();
                        list.addAll(parameterList);
                        list.add(newParam);
                        newParameters.add(list);
                    }
                } else {
                    newParameters.add(Collections.singletonList(newParam));
                }
            }
        }

        List<Expression> result = new ArrayList<>();
        for (List<Expression> list : parameters) {
            result.add(ff.function(func.getName(), list.toArray(new Expression[list.size()])));
        }
        return result;
    }

    @Override
    public Object visit(Literal expression, Object extraData) {
        return Collections.singletonList(ff.literal(expression.getValue()));
    }

    @Override
    public Object visit(Multiply multiply, Object extraData) {
        return combine(multiply, new BinaryExpressionReplacer("multiply"), extraData);
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        XPathUtil.StepList steps =
                XPathUtil.steps(
                        featureDescriptor,
                        expression.getPropertyName(),
                        MetaDataDescriptor.NAMESPACES);

        if (steps.size() == 1 && steps.get(0).getName().getNamespaceURI() == null
                || steps.get(0)
                        .getName()
                        .getNamespaceURI()
                        .equals(MetaDataDescriptor.NAMESPACE_APISO)) {
            List<PropertyName> fullPath =
                    queryableMapping.get(steps.get(0).getName().getLocalPart());
            if (fullPath != null) {
                return fullPath;
            }
        }

        return Collections.singletonList(expression);
    }

    @Override
    public Object visit(Subtract subtract, Object extraData) {
        return combine(subtract, new BinaryExpressionReplacer("subtract"), extraData);
    }
}
