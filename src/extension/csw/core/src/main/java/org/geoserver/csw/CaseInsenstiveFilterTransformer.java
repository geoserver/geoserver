/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;

/**
 * Transforms like filters from case sensitive to case insensitive
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CaseInsenstiveFilterTransformer extends DuplicatingFilterVisitor {

    @Override
    public Object visit(PropertyIsLike filter, Object extraData) {
        Expression expr = (Expression) filter.getExpression().accept(this, extraData);
        String pattern = filter.getLiteral();
        String wildcard = filter.getWildCard();
        String singleChar = filter.getSingleChar();
        String escape = filter.getEscape();
        return getFactory(extraData).like(expr, pattern, wildcard, singleChar, escape, false);
    }
}
