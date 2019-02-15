/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * A basic text based output format designed to ease debugging GetCoverage calls (and actually read
 * the contents of a coverage without getting mad...)
 *
 * @author Andrea Aime - TOPP
 */
public class DebugCoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    @SuppressWarnings("serial")
    public DebugCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("DEBUG", "text/debug"), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("DEBUG", "txt");
                        put("text/debug", "txt");
                        put("text/plain", "txt");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("DEBUG", "text/plain");
                        put("text/debug", "text/plain");
                    }
                });
    }

    public void encode(
            GridCoverage2D coverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        PrintStream ps = new PrintStream(output);
        ps.println("Grid bounds: " + coverage.getEnvelope());
        ps.println("Grid CRS: " + coverage.getCoordinateReferenceSystem());
        ps.println("Grid range: " + coverage.getGridGeometry().getGridRange());
        ps.println("Grid to world: " + coverage.getGridGeometry().getGridToCRS());
        ps.println("Contents:");
        RenderedImage ri = coverage.getRenderedImage();
        Raster raster = ri.getData();
        for (int band = 0; band < raster.getNumBands(); band++) {
            ps.println("Band " + band + ":");
            for (int j = raster.getMinY(); j < (raster.getMinY() + raster.getHeight()); j++) {
                for (int i = raster.getMinX(); i < (raster.getMinX() + raster.getWidth()); i++) {
                    if (raster.getTransferType() == DataBuffer.TYPE_DOUBLE)
                        ps.print(raster.getSampleDouble(i, j, band));
                    else if (raster.getTransferType() == DataBuffer.TYPE_FLOAT)
                        ps.print(raster.getSampleFloat(i, j, band));
                    else ps.print(raster.getSample(i, j, band));
                    if (i < (raster.getMinX() + raster.getWidth() - 1)) {
                        ps.print(" ");
                    }
                }
                ps.println();
            }
        }
        ps.flush();

        coverage.dispose(false);
    }
}
