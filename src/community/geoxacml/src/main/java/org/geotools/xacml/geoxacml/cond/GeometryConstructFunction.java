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

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Abstract base class for geometry construct functions
 * 
 * @author Christian Mueller
 * 
 */
public abstract class GeometryConstructFunction extends GeoXACMLFunctionBase {

    public GeometryConstructFunction(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, int minParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, minParams, returnType,
                returnsBag);
    }

    public GeometryConstructFunction(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, returnType, returnsBag);
    }

    public GeometryConstructFunction(String functionName, int functionId, String returnType,
            boolean returnsBag) {
        super(functionName, functionId, returnType, returnsBag);
    }

    public GeometryConstructFunction(String functionName, int functionId, String[] paramTypes,
            boolean[] paramIsBag, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramTypes, paramIsBag, returnType, returnsBag);
    }

    protected EvaluationResult createGeometryInBagResult(Geometry resultGeom, String targetSrsName) {
        GeometryAttribute resultGeomAttr = null;

        try {
            resultGeomAttr = new GeometryAttribute(resultGeom, targetSrsName, null, null, null);
        } catch (URISyntaxException e) {
            // should not happen
        }
        Set<AttributeValue> set = new HashSet<AttributeValue>();
        set.add(resultGeomAttr);
        BagAttribute bag = new BagAttribute(resultGeomAttr.getType(), set);
        return new EvaluationResult(bag);
    }
}
