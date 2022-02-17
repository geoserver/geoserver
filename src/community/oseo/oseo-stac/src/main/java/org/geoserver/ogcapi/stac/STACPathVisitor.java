/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.util.Map;
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

    public STACPathVisitor(Map<String, Expression> propertyMap) {
        this.propertyMap = propertyMap;
    }

    @Override
    public Object visit(PropertyName pn, Object extraData) {
        String propertyName = pn.getPropertyName();

        Expression expression = propertyMap.get(propertyName);
        if (expression != null) return expression;

        // not found, so will always be null
        return ff.literal(null);
    }
}
