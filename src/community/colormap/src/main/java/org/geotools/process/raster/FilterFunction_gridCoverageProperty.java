/* (c) 2013 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

/**
 * Filter function to retrieve a grid coverage property value from the underlying GridCoverage2D
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class FilterFunction_gridCoverageProperty extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "gridCoverageProperty",
                    parameter("value", Number.class),
                    parameter("property", String.class));

    public FilterFunction_gridCoverageProperty() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String arg0;

        try { // attempt to get value and perform conversion
            arg0 = (getExpression(0).evaluate(feature, String.class));
            if (feature instanceof GridCoverage2D) {
                GridCoverage2D coverage = (GridCoverage2D) feature;
                Object prop = coverage.getProperty(arg0);
                if (prop != null) {
                    Number number = (Number) prop;
                    return number;
                }
            }
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageProperty: Unable to find the property "
                            + arg0
                            + " from the input object of type "
                            + feature.getClass());
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageProperty", e);
        }
    }
}
