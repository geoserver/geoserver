/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.hz;

import static com.hazelcast.query.Predicates.*;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.TruePredicate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.functors.FalsePredicate;
import org.geotools.filter.LikeToRegexConverter;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;

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
