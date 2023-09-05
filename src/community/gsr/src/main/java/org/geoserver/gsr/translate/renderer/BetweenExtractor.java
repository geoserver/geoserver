/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsBetween;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.util.NumberRange;

public class BetweenExtractor implements PropertyRangeExtractor {

    @Override
    public PropertyRange getRange(Filter filter) {
        if (!(filter instanceof PropertyIsBetween)) return null;
        PropertyIsBetween between = (PropertyIsBetween) filter;
        Expression property = between.getExpression();
        if (!(property instanceof PropertyName)) {
            return null;
        }
        String propertyName = ((PropertyName) property).getPropertyName();

        Expression min = between.getLowerBoundary();
        if (!(min instanceof Literal)) {
            return null;
        }
        Double minAsDouble = min.evaluate(null, double.class);

        Expression max = between.getUpperBoundary();
        if (!(max instanceof Literal)) {
            return null;
        }
        Double maxAsDouble = max.evaluate(null, double.class);

        return new PropertyRange(
                propertyName, new NumberRange(Double.class, minAsDouble, maxAsDouble));
    }
}
