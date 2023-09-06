/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import org.geoserver.platform.ServiceException;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.Beyond;
import org.geotools.api.filter.spatial.BinarySpatialOperator;
import org.geotools.api.filter.spatial.Contains;
import org.geotools.api.filter.spatial.Crosses;
import org.geotools.api.filter.spatial.DWithin;
import org.geotools.api.filter.spatial.Disjoint;
import org.geotools.api.filter.spatial.Equals;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.filter.spatial.Overlaps;
import org.geotools.api.filter.spatial.Touches;
import org.geotools.api.filter.spatial.Within;
import org.geotools.filter.visitor.DefaultFilterVisitor;

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

    @Override
    public Object visit(final BBOX filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Beyond filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Contains filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Crosses filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Disjoint filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(DWithin filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Equals filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Intersects filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Overlaps filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Touches filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    @Override
    public Object visit(Within filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }
}
