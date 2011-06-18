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

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;

import com.sun.xacml.attr.BooleanAttribute;

/**
 * Abstract base class for geomtry topological functions
 * 
 * @author Christian Mueller
 * 
 */
public abstract class GeometryTopologicalFunction extends GeoXACMLFunctionBase {

    protected static final String params[] = { GeometryAttribute.identifier,
            GeometryAttribute.identifier };

    protected static final boolean bagParams[] = { false, false };

    public GeometryTopologicalFunction(String name) {
        super(name, 0, params, bagParams, BooleanAttribute.identifier, false);
    }

}
