/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import org.geotools.util.NumberRange;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/** Extracts a range from rules with a "lower than" filter */
class LowerExtractor implements PropertyRangeExtractor {

    @Override
    public PropertyRange getRange(Filter filter) {
        if (!(filter instanceof PropertyIsLessThanOrEqualTo
                || filter instanceof PropertyIsLessThan)) return null;
        BinaryComparisonOperator upperBound = (BinaryComparisonOperator) filter;
        Expression property = upperBound.getExpression1();

        if (!(property instanceof PropertyName)) {
            return null;
        }
        String propertyName = ((PropertyName) property).getPropertyName();

        Expression max = upperBound.getExpression2();
        if (!(max instanceof Literal)) {
            return null;
        }
        Double maxAsDouble = max.evaluate(null, double.class);

        return new PropertyRange(
                propertyName, new NumberRange(Double.class, -Double.MAX_VALUE, maxAsDouble));
    }
}
