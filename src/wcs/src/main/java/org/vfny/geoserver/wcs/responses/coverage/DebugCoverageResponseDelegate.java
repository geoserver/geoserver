/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wcs.responses.coverage;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.vfny.geoserver.wcs.responses.CoverageResponseDelegate;

/**
 * A basic text based output format designed to ease debugging GetCoverage calls (and actually read
 * the contents of a coverage without getting mad...)
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class DebugCoverageResponseDelegate implements CoverageResponseDelegate {

    private static final Set<String> FORMATS = new HashSet<String>(Arrays.asList("text/debug"));

    private GridCoverage2D coverage;

    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && (outputFormat.equalsIgnoreCase("DEBUG") || FORMATS.contains(outputFormat
                        .toLowerCase()));
    }

    public String getContentDisposition() {
        return null;
    }

    public String getContentEncoding() {
        return null;
    }

    public String getContentType() {
        return "text/plain";
    }

    public String getFileExtension() {
        return "txt";
    }

    public String getMimeFormatFor(String outputFormat) {
        if (canProduce(outputFormat))
            return "text/debug";
        else
            return null;
    }

    public void prepare(String outputFormat, GridCoverage2D coverage) throws IOException {
        this.coverage = coverage;
    }

    public void encode(OutputStream output) throws ServiceException, IOException {
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
                    else
                        ps.print(raster.getSample(i, j, band));
                    if (i < (raster.getMinX() + raster.getWidth() - 1))
                        ;
                    ps.print(" ");
                }
                ps.println();
            }

        }
        ps.flush();
    }

}
