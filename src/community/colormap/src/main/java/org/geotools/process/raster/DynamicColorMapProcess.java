/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.renderer.lite.gridcoverage2d.RasterSymbolizerHelper;
import org.geotools.styling.ColorMap;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.StyleBuilder;
import org.geotools.util.factory.GeoTools;

/**
 * Render a GridCoverage based on a dynamic colormap
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@DescribeProcess(title = "dynamicColorMap", description = "Apply a Dynamic colorMap to a coverage")
public class DynamicColorMapProcess implements RasterProcess {

    public static final String NAME = "DynamicColorMap";

    public DynamicColorMapProcess() {}

    @DescribeResult(name = "result", description = "output raster")
    public GridCoverage2D execute(
            @DescribeParameter(name = "data", description = "Input raster") GridCoverage2D coverage,
            @DescribeParameter(name = "colorRamp", description = "The name of the color ramp.")
                    ColorMap colorMap,
            @DescribeParameter(
                        name = "opacity",
                        description = "The opacity level, between 0 and 1.",
                        defaultValue = "1",
                        min = 0,
                        minValue = 0,
                        maxValue = 1
                    )
                    float opacity)
            throws ProcessException {

        final RasterSymbolizer rsb_1 = buildRasterSymbolizer(colorMap, opacity);

        RasterSymbolizerHelper rsh =
                new RasterSymbolizerHelper(coverage, GeoTools.getDefaultHints());
        rsh.visit(rsb_1);
        return (GridCoverage2D) rsh.getOutput();
    }

    private RasterSymbolizer buildRasterSymbolizer(ColorMap colorMap, float opacity) {
        // build the RasterSymbolizer
        StyleBuilder sldBuilder = new StyleBuilder();

        final RasterSymbolizer rsb_1 = sldBuilder.createRasterSymbolizer();
        rsb_1.setColorMap(colorMap);
        rsb_1.setOpacity(sldBuilder.getFilterFactory().literal(opacity));
        return rsb_1;
    }
}
