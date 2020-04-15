/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import org.geotools.util.NumberRange;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

public class GreaterExtractor implements PropertyRangeExtractor {

    @Override
    public PropertyRange getRange(Filter filter) {
        if (!(filter instanceof PropertyIsGreaterThanOrEqualTo
                || filter instanceof PropertyIsGreaterThan)) return null;
        BinaryComparisonOperator lowerBound = (BinaryComparisonOperator) filter;
        Expression property = lowerBound.getExpression1();

        if (!(property instanceof PropertyName)) {
            return null;
        }
        String propertyName = ((PropertyName) property).getPropertyName();

        Expression min = lowerBound.getExpression2();
        if (!(min instanceof Literal)) {
            return null;
        }
        Double minAsDouble = min.evaluate(null, double.class);

        return new PropertyRange(
                propertyName, new NumberRange(Double.class, minAsDouble, Double.MAX_VALUE));
    }
}
