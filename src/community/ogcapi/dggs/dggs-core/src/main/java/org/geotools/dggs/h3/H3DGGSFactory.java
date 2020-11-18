/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.dggs.h3;

import com.uber.h3core.H3Core;
import java.io.IOException;
import java.util.Map;
import org.geotools.data.Parameter;
import org.geotools.dggs.DGGSFactory;
import org.geotools.dggs.DGGSInstance;

public class H3DGGSFactory implements DGGSFactory {

    @Override
    public String getId() {
        return "H3";
    }

    @Override
    public Parameter[] getParametersInfo() {
        // no parameters
        return new Parameter[0];
    }

    @Override
    public DGGSInstance createInstance(Map<String, ?> params) throws IOException {
        return new H3DGGSInstance(H3Core.newInstance());
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.uber.h3core.H3Core");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
