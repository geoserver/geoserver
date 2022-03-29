/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.util.Map;
import java.util.Set;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * STAC specific mapper that:
 *
 * <ul>
 *   <li>Assumes the filter is referencing properties inside "features.properties" directly.
 *   <li>Handles the case where multiple templates are used against the same template
 * </ul>
 */
public class STACPathVisitor extends DuplicatingFilterVisitor {

    private final Map<String, Expression> propertyMap;
    private final Set<String> queryables;
    private final Set<String> notIncluded;

    public STACPathVisitor(
            Map<String, Expression> propertyMap, Set<String> queryables, Set<String> notIncluded) {
        this.propertyMap = propertyMap;
        this.queryables = queryables;
        this.notIncluded = notIncluded;
    }

    @Override
    public Object visit(PropertyName pn, Object extraData) {
        String propertyName = pn.getPropertyName();

        if (queryables != null) {
            if (!queryables.contains(propertyName)) {
                if (notIncluded != null) {
                    notIncluded.add(propertyName);
                }
                // not in queryables so return as null
                return ff.literal(null);
            }
        }
        Expression expression = propertyMap.get(propertyName);
        if (expression != null) return expression;

        // not found, so will always be null
        return ff.literal(null);
    }
}
