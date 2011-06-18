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

/**
 * Abstract base class for geomtry scalar functions
 * 
 * @author Christian Mueller
 * 
 */
public abstract class GeometryScalarFunction extends GeoXACMLFunctionBase {

    public GeometryScalarFunction(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, int minParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, minParams, returnType,
                returnsBag);

    }

    public GeometryScalarFunction(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, returnType, returnsBag);
    }

    public GeometryScalarFunction(String functionName, int functionId, String returnType,
            boolean returnsBag) {
        super(functionName, functionId, returnType, returnsBag);
    }

    public GeometryScalarFunction(String functionName, int functionId, String[] paramTypes,
            boolean[] paramIsBag, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramTypes, paramIsBag, returnType, returnsBag);
    }

}
