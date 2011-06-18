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

package org.geotools.xacml.extensions;

import java.util.HashSet;
import java.util.Set;

import com.sun.xacml.cond.Function;
import com.sun.xacml.cond.cluster.FunctionCluster;

/**
 * Clusters all the functions supported by {@link WildCardFunction}.
 * 
 * @author Christian Mueller
 * 
 */
public class WildCardFunctionCluster implements FunctionCluster {

    public Set<Function> getSupportedFunctions() {
        Set<Function> set = new HashSet<Function>();

        for (String fn : WildCardFunction.getSupportedIdentifiers())
            set.add(new WildCardFunction(fn));

        return set;
    }

}
