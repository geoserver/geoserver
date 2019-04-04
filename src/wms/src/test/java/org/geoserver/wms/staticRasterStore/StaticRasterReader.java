/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.staticRasterStore;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.media.jai.ImageLayout;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.datum.PixelInCell;

/**
 * This reader will always return the same static image, in the future it may be configured to
 * return more static images based on the read parameters.
 */
final class StaticRasterReader extends AbstractGridCoverage2DReader {

    // static image to return
    private static final BufferedImage STATIC_IMAGE =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    StaticRasterReader(Object source) {
        coverageFactory = new GridCoverageFactory();
        crs = DefaultGeographicCRS.WGS84;
        // instantiate the bounds based on the default CRS
        originalEnvelope = new GeneralEnvelope(CRS.getEnvelope(crs));
        originalEnvelope.setCoordinateReferenceSystem(crs);
        originalGridRange = new GeneralGridEnvelope(originalEnvelope, PixelInCell.CELL_CENTER);
        // create a default layout based on the static image
        setlayout(new ImageLayout(STATIC_IMAGE));
    }

    @Override
    public Format getFormat() {
        // the only available format
        return new StaticRasterFormat();
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] readParameters)
            throws IOException {
        // return he static image
        return coverageFactory.create(coverageName, STATIC_IMAGE, originalEnvelope);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        // return he static image
        return coverageFactory.create(coverageName, STATIC_IMAGE, originalEnvelope);
    }

    @Override
    public String[] getGridCoverageNames() {
        // we only have the static image
        return new String[] {"STATIC_IMAGE"};
    }

    @Override
    protected boolean checkName(String coverageName) {
        // no need to check the name
        return true;
    }
}
