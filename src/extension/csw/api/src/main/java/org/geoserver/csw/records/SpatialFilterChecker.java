/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import org.geoserver.platform.ServiceException;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;

/**
 * Checks if spatial filters are used against non spatial properties, if so, throws a
 * ServiceException (mandated for CSW cite compliance). Works fine for simple data models, but you
 * might need to use a custom one for more complex models (e.g., SpatialFilterChecker is known not
 * to work with ebRIM)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SpatialFilterChecker extends DefaultFilterVisitor {

    FeatureType schema;

    public SpatialFilterChecker(FeatureType schema) {
        this.schema = schema;
    }

    private void checkBinarySpatialOperator(BinarySpatialOperator filter) {
        verifyGeometryProperty(filter.getExpression1());
        verifyGeometryProperty(filter.getExpression2());
    }

    private void verifyGeometryProperty(Expression expression) {
        if (expression instanceof PropertyName) {
            PropertyName pn = ((PropertyName) expression);

            if (!(pn.evaluate(schema) instanceof GeometryDescriptor)) {
                throw new ServiceException(
                        "Invalid spatial filter, property "
                                + pn.getPropertyName()
                                + " is not a geometry");
            }
        }
    }

    public Object visit(final BBOX filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Beyond filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Contains filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Crosses filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Disjoint filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(DWithin filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Equals filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Intersects filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Overlaps filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Touches filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Within filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }
}
