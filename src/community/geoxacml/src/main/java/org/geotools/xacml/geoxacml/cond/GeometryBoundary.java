/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.xacml.geoxacml.cond;

import java.util.List;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.Expression;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Calulates the boundary of a geometry
 * 
 * @author Christian Mueller
 * 
 */
public class GeometryBoundary extends GeometryConstructFunction {

    public static final String NAME = NAME_PREFIX + "geometry-boundary";

    public GeometryBoundary() {
        super(NAME, 0, new String[] { GeometryAttribute.identifier }, new boolean[] { false },
                GeometryAttribute.identifier, true);
    }

    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {

        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        GeometryAttribute geomAttr = (GeometryAttribute) (argValues[0]);

        Geometry resultGeom = null;

        try {
            resultGeom = geomAttr.getGeometry().getBoundary();
        } catch (Throwable t) {
            return exceptionError(t);
        }

        return createGeometryInBagResult(resultGeom, geomAttr.getSrsName());

    }

}
