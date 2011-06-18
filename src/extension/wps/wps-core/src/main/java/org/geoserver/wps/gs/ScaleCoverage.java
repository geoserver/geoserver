/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;

import javax.media.jai.Interpolation;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Applies a generic scale and translate operation to a coverage 
 * 
 * @author Andrea Aime - GeoSolutions
 * @author ETj <etj at geo-solutions.it>
 */
@DescribeProcess(title = "scaleCoverage", description = "Applies a generic scale and translate operation to a coverage")
public class ScaleCoverage implements GeoServerProcess {

    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();
    private static final Operation SCALE = PROCESSOR.getOperation("Scale");

    @DescribeResult(name = "result", description = "The scaled raster")
    public GridCoverage2D execute(
            @DescribeParameter(name = "coverage", description = "The raster to be scaled") GridCoverage2D coverage,
            @DescribeParameter(name = "xScale", description = "Scale factor along the x axis") double xScale,
            @DescribeParameter(name = "yScale", description = "Scale factor along the y axis") double yScale,
            @DescribeParameter(name = "xTranslate", description = "Offset along the x axis") double xTranslate,
            @DescribeParameter(name = "yTranslate", description = "Offset along the y axis") double yTranslate,
            @DescribeParameter(name = "interpolation", description = "Interpolation type", min = 0) Interpolation interpolation) throws IOException {
        final ParameterValueGroup param = SCALE.getParameters();
        
        param.parameter("Source").setValue(coverage);
        param.parameter("xScale").setValue(xScale);
        param.parameter("yScale").setValue(yScale);
        param.parameter("xTrans").setValue(Float.valueOf(0.0f));
        param.parameter("yTrans").setValue(Float.valueOf(0.0f));
        if(interpolation != null) {
            param.parameter("Interpolation").setValue(interpolation);
        }

        return (GridCoverage2D) PROCESSOR.doOperation(param);
    }

}
