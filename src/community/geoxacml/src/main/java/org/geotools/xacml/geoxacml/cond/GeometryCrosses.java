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

/**
 * Checks if 2 geoms cross
 * 
 * @author Christian Mueller
 * 
 */
public class GeometryCrosses extends GeometryTopologicalFunction {

    public static final String NAME = NAME_PREFIX + "geometry-crosses";

    public GeometryCrosses() {
        super(NAME);

    }

    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {

        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        GeometryAttribute[] geomAttrs = new GeometryAttribute[2];
        geomAttrs[0] = (GeometryAttribute) (argValues[0]);
        geomAttrs[1] = (GeometryAttribute) (argValues[1]);

        boolean evalResult = false;

        try {
            transformOnDemand(geomAttrs);
            evalResult = geomAttrs[0].getGeometry().crosses(geomAttrs[1].getGeometry());
        } catch (Throwable t) {
            return exceptionError(t);
        }

        return EvaluationResult.getInstance(evalResult);

    }

}
