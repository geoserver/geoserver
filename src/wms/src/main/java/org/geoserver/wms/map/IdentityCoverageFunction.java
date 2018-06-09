/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.opengis.filter.capability.FunctionName;

/**
 * Simple rendering transformation placeholder, just returns its input (specific to GridCoverage2D
 * so that the direct rendering path recognizes it as a function it can handle)
 */
class IdentityCoverageFunction extends InternalVolatileFunction {

    public static FunctionName NAME =
            new FunctionNameImpl("Identify", parameter("coverage", GridCoverage2D.class));

    public IdentityCoverageFunction() {
        super("Identity");
    }

    @Override
    public FunctionName getFunctionName() {
        return NAME;
    }

    @Override
    public Object evaluate(Object object) {
        return object;
    }
}
