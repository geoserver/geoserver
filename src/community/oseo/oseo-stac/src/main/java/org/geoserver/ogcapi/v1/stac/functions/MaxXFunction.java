/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac.functions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.capability.FunctionName;

/** Returns the maximum X value of the given geometry */
public class MaxXFunction extends FunctionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl("maxx", Double.class, parameter("geometry", Geometry.class));

    public MaxXFunction() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object feature) {
        Geometry g = getParameters().get(0).evaluate(feature, Geometry.class);
        if (g == null) return null;
        return g.getEnvelopeInternal().getMaxX();
    }
}
