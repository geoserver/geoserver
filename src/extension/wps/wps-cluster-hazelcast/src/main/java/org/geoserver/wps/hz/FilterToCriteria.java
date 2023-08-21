/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.hz;

import static com.hazelcast.query.Predicates.and;
import static com.hazelcast.query.Predicates.between;
import static com.hazelcast.query.Predicates.equal;
import static com.hazelcast.query.Predicates.greaterEqual;
import static com.hazelcast.query.Predicates.greaterThan;
import static com.hazelcast.query.Predicates.ilike;
import static com.hazelcast.query.Predicates.in;
import static com.hazelcast.query.Predicates.lessEqual;
import static com.hazelcast.query.Predicates.lessThan;
import static com.hazelcast.query.Predicates.like;
import static com.hazelcast.query.Predicates.not;
import static com.hazelcast.query.Predicates.or;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.impl.predicates.TruePredicate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.functors.FalsePredicate;
import org.geotools.api.filter.And;
import org.geotools.api.filter.BinaryComparisonOperator;
import org.geotools.api.filter.ExcludeFilter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterVisitor;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.IncludeFilter;
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
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Beyond;
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
import org.geotools.filter.LikeToRegexConverter;

/**
 * Converts a OGC Filter to the Hazelcast Criteria API (a {@link Predicate}).
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FilterToCriteria implements FilterVisitor {

    private class PropertyComparable {
        String property;

        Comparable literal;

        boolean inverted;

        public PropertyComparable(BinaryComparisonOperator op) {
            if (op.getExpression1() instanceof PropertyName
                    && op.getExpression2() instanceof Literal) {
                property = getPropertyName(op.getExpression1());
                literal = (Comparable) ((Literal) op.getExpression2()).getValue();
            } else if (op.getExpression2() instanceof PropertyName
                    && op.getExpression1() instanceof Literal) {
                property = getPropertyName(op.getExpression2());
                literal = (Comparable) ((Literal) op.getExpression1()).getValue();
                inverted = true;
            } else {
                throw new IllegalArgumentException(
                        "Unsupported comparison, only comparison between an attribute and a static value are supported: "
                                + op);
            }
        }

        public PropertyComparable(BinaryTemporalOperator op) {
            if (op.getExpression1() instanceof PropertyName
                    && op.getExpression2() instanceof Literal) {
                property = ((PropertyName) op.getExpression1()).getPropertyName();
                literal = (Comparable) ((Literal) op.getExpression2()).getValue();
            } else if (op.getExpression2() instanceof PropertyName
                    && op.getExpression1() instanceof Literal) {
                property = ((PropertyName) op.getExpression2()).getPropertyName();
                literal = (Comparable) ((Literal) op.getExpression1()).getValue();
                inverted = true;
            } else {
                throw new IllegalArgumentException(
                        "Unsupported comparison, only comparison between an attribute and a static value are supported: "
                                + op);
            }
        }
    }

    @Override
    public Object visitNullFilter(Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        return FalsePredicate.INSTANCE;
    }

    @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        // we are going to filter against ExecutionStatus, this ensure we match everything
        return TruePredicate.INSTANCE;
    }

    @Override
    public Object visit(And filter, Object extraData) {
        List<Predicate> predicates = new ArrayList<>();
        for (Filter child : filter.getChildren()) {
            predicates.add((Predicate) child.accept(this, extraData));
        }
        return and(predicates.toArray(new Predicate[predicates.size()]));
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        List<String> ids = new ArrayList<>();
        for (Object id : filter.getIDs()) {
            ids.add(id.toString());
        }
        return in("executionId", ids.toArray(new String[ids.size()]));
    }

    @Override
    public Object visit(Not filter, Object extraData) {
        return not((Predicate) filter.getFilter().accept(this, extraData));
    }

    @Override
    public Object visit(Or filter, Object extraData) {
        List<Predicate> predicates = new ArrayList<>();
        for (Filter child : filter.getChildren()) {
            predicates.add((Predicate) child.accept(this, extraData));
        }
        return or(predicates.toArray(new Predicate[predicates.size()]));
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        String propertyName = getPropertyName(filter.getExpression());
        Comparable low = (Comparable) ((Literal) filter.getLowerBoundary()).getValue();
        Comparable high = (Comparable) ((Literal) filter.getLowerBoundary()).getValue();
        return between(propertyName, low, high);
    }

    private String getPropertyName(Expression expression) {
        if (!(expression instanceof PropertyName)) {
            throw new IllegalArgumentException(
                    "Was expecting a property name, but found: " + expression);
        }
        String propertyName = ((PropertyName) expression).getPropertyName();
        if ("processName".equals(propertyName)) {
            propertyName = "simpleProcessName";
        }

        return propertyName;
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        PropertyComparable components = new PropertyComparable(filter);
        return equal(components.property, components.literal);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        PropertyComparable components = new PropertyComparable(filter);
        return not(equal(components.property, components.literal));
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        PropertyComparable components = new PropertyComparable(filter);
        return components.inverted
                ? lessEqual(components.property, components.literal)
                : greaterThan(components.property, components.literal);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        PropertyComparable components = new PropertyComparable(filter);
        return components.inverted
                ? lessThan(components.property, components.literal)
                : greaterEqual(components.property, components.literal);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        PropertyComparable components = new PropertyComparable(filter);
        return components.inverted
                ? greaterEqual(components.property, components.literal)
                : lessThan(components.property, components.literal);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        PropertyComparable components = new PropertyComparable(filter);
        return components.inverted
                ? greaterThan(components.property, components.literal)
                : lessEqual(components.property, components.literal);
    }

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        String propertyName = getPropertyName(filter.getExpression());
        String pattern = new LikeToRegexConverter(filter).getPattern();
        if (filter.isMatchingCase()) {
            return like(propertyName, pattern);
        } else {
            return ilike(propertyName, pattern);
        }
    }

    @Override
    public Object visit(PropertyIsNull filter, Object extraData) {
        String propertyName = getPropertyName(filter.getExpression());
        return equal(propertyName, null);
    }

    @Override
    public Object visit(PropertyIsNil filter, Object extraData) {
        String propertyName = getPropertyName(filter.getExpression());
        return equal(propertyName, null);
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Contains filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Crosses filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Disjoint filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Equals filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Intersects filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Overlaps filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Touches filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Within filter, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(After after, Object extraData) {
        PropertyComparable components = new PropertyComparable(after);
        return components.inverted
                ? lessEqual(components.property, components.literal)
                : greaterThan(components.property, components.literal);
    }

    @Override
    public Object visit(AnyInteracts anyInteracts, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Before before, Object extraData) {
        PropertyComparable components = new PropertyComparable(before);
        return components.inverted
                ? greaterEqual(components.property, components.literal)
                : lessThan(components.property, components.literal);
    }

    @Override
    public Object visit(Begins begins, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(BegunBy begunBy, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(During during, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(EndedBy endedBy, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Ends ends, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Meets meets, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(MetBy metBy, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(OverlappedBy overlappedBy, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(TContains contains, Object extraData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(TEquals equals, Object extraData) {
        PropertyComparable components = new PropertyComparable(equals);
        return equal(components.property, components.literal);
    }

    @Override
    public Object visit(TOverlaps contains, Object extraData) {
        throw new UnsupportedOperationException();
    }
}
