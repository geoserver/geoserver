/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.renderer.lite.gridcoverage2d.RasterSymbolizerHelper;
import org.geotools.renderer.lite.gridcoverage2d.SubchainStyleVisitorCoverageProcessingAdapter;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;

/**
 * Applies a raster symbolizer to the coverage
 * 
 * @author Andrea Aime - GeoSolutions
 * @author ETj <etj at geo-solutions.it>
 */
@DescribeProcess(title = "styleCoverage", description = "Applies a raster symbolizer to the coverage")
public class StyleCoverage implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The styled raster")
    public GridCoverage2D execute(
            @DescribeParameter(name = "coverage", description = "The raster to be styled") GridCoverage2D coverage,
            @DescribeParameter(name = "style", description = "A SLD style containing a raster symbolizer") Style style)
            throws IOException {
        // TODO: perform a lookup in the entire style?
        final RasterSymbolizer symbolizer = (RasterSymbolizer) style.featureTypeStyles().get(0)
                .rules().get(0).symbolizers().get(0);

        SubchainStyleVisitorCoverageProcessingAdapter rsh = new RasterSymbolizerHelper(coverage,
                null);
        rsh.visit(symbolizer);
        GridCoverage2D g = ((GridCoverage2D) rsh.execute()).geophysics(false);
        return g;
    }

}
