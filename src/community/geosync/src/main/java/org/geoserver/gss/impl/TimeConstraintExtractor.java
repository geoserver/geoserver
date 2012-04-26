package org.geoserver.gss.impl;

import java.util.Date;

import org.geotools.filter.visitor.AbstractSearchFilterVisitor;
import org.geotools.util.Range;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.springframework.util.Assert;

/**
 * 
 *
 */
public class TimeConstraintExtractor extends AbstractSearchFilterVisitor {

    private static final Range<Date> ALWAYS = new Range<Date>(Date.class, new Date(0), true,
            new Date(Long.MAX_VALUE), true);

    private Range<Date> validTimeWindow;

    public TimeConstraintExtractor() {
        validTimeWindow = ALWAYS;
    }

    public Range<Date> getValidTimeWindow() {
        return validTimeWindow;
    }

    private boolean isTimeProperty(Expression expression) {
        if (expression instanceof PropertyName) {
            PropertyName prop = (PropertyName) expression;
            String propertyName = prop.getPropertyName();
            if (propertyName.endsWith("updated")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsEqualTo,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        if (isTimeProperty(filter.getExpression1())) {
            Date instant = filter.getExpression2().evaluate(null, Date.class);
            validTimeWindow = new Range<Date>(Date.class, instant, true, instant, true);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsBetween,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsBetween filter, Object extraData) {
        if (isTimeProperty(filter.getExpression())) {
            Date startTime = filter.getLowerBoundary().evaluate(null, Date.class);
            Assert.notNull(startTime);
            Date endTime = filter.getUpperBoundary().evaluate(null, Date.class);
            Assert.notNull(endTime);
            validTimeWindow = new Range<Date>(Date.class, startTime, false, endTime, false);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsGreaterThan,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        if (isTimeProperty(filter.getExpression1())) {
            Date startTime = filter.getExpression2().evaluate(null, Date.class);
            validTimeWindow = new Range<Date>(Date.class, startTime, false, ALWAYS.getMaxValue(),
                    true);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsLessThan,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        if (isTimeProperty(filter.getExpression1())) {
            Date endTime = filter.getExpression2().evaluate(null, Date.class);
            validTimeWindow = new Range<Date>(Date.class, ALWAYS.getMinValue(), true, endTime,
                    false);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsLessThanOrEqualTo,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        if (isTimeProperty(filter.getExpression1())) {
            Date endTime = filter.getExpression2().evaluate(null, Date.class);
            validTimeWindow = new Range<Date>(Date.class, ALWAYS.getMinValue(), true, endTime, true);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsGreaterThanOrEqualTo,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        if (isTimeProperty(filter.getExpression1())) {
            Date startTime = filter.getExpression2().evaluate(null, Date.class);
            validTimeWindow = new Range<Date>(Date.class, startTime, true, ALWAYS.getMaxValue(),
                    true);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see org.geotools.filter.visitor.AbstractSearchFilterVisitor#visit(org.opengis.filter.PropertyIsNotEqualTo,
     *      java.lang.Object)
     */
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        if (isTimeProperty(filter.getExpression1())) {
            Date endTime = filter.getExpression2().evaluate(null, Date.class);
            validTimeWindow = new Range<Date>(Date.class, ALWAYS.getMinValue(), true, endTime,
                    false);
            return validTimeWindow;
        }
        return super.visit(filter, extraData);
    }
}
