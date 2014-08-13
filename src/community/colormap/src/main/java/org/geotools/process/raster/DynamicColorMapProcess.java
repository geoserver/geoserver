/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.GeoTools;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.raster.RasterProcess;
import org.geotools.renderer.lite.gridcoverage2d.RasterSymbolizerHelper;
import org.geotools.styling.ColorMap;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.StyleBuilder;

/**
 * Render a GridCoverage based on a dynamic colormap
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@DescribeProcess(title = "dynamicColorMap", description = "Apply a Dynamic colorMap to a coverage")
public class DynamicColorMapProcess implements RasterProcess {

    public final static String NAME=  "dynamicColorMap";

    public DynamicColorMapProcess() {

    }

    @DescribeResult(name = "result", description = "output raster")
    public GridCoverage2D execute(
            @DescribeParameter(name = "data", description = "Input raster") GridCoverage2D coverage,
            @DescribeParameter(name = "colorRamp", description = "The name of the color ramp.") ColorMap colorMap)
            throws ProcessException {

            RasterSymbolizerHelper rsh = new RasterSymbolizerHelper(coverage, GeoTools.getDefaultHints());
            // build the RasterSymbolizer
            StyleBuilder sldBuilder = new StyleBuilder();
    
            final RasterSymbolizer rsb_1 = sldBuilder.createRasterSymbolizer();
            rsb_1.setColorMap(colorMap);
            rsh.visit(rsb_1);
            return (GridCoverage2D) rsh.getOutput();
    }

}
