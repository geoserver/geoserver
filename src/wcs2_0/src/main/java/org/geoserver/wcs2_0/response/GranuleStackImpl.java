/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.TiledImage;
import org.geotools.api.coverage.CannotEvaluateException;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralBounds;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.operation.transform.ProjectiveTransform;

/**
 * A GridCoverage instance composed of several GridCoverage2D Granules which may be obtained through the getGranules()
 * method.
 *
 * <p>TODO: note that we extends GridCoverage2D since all coverageResponseDelegate.encode has a GridCoverage2D as input
 * parameter. we should propose an API change where we encode a GridCoverage instead and where GridCoverage has a
 * dispose method to be implemented.
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class GranuleStackImpl extends GridCoverage2D /*AbstractGridCoverage*/ implements GranuleStack {

    /**
     * Right now, all CoverageResponseDelegate work with GridCoverage2D. Therefore, in order to encode a granuleStack we
     * need to implement it as a GridCoverage2D. So we pass a Dummy GridCoverage2D with dummy information to avoid
     * constructor failures. The provided information will be ignored anyway.
     *
     * <p>Once we move to extending AbstractGridCoverage instead of GridCoverage2D we may remove this dummy class.
     */
    static class DummyGridCoverage2D extends GridCoverage2D {

        static GridEnvelope SAMPLE_GRID_ENVELOPE = new GridEnvelope2D(new Rectangle(0, 0, 1, 1));

        static MathTransform SAMPLE_TRANSFORM = ProjectiveTransform.create(AffineTransform.getScaleInstance(1, 1));

        static PlanarImage SAMPLE_IMAGE = new TiledImage(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY), false);

        protected DummyGridCoverage2D(CharSequence name, CoordinateReferenceSystem crs)
                throws IllegalArgumentException {
            super(
                    name,
                    SAMPLE_IMAGE,
                    new GridGeometry2D(
                            SAMPLE_GRID_ENVELOPE,
                            new GeneralBounds(SAMPLE_GRID_ENVELOPE, PixelInCell.CELL_CENTER, SAMPLE_TRANSFORM, crs)),
                    null,
                    null,
                    null,
                    null);
        }
    }

    /** The list of all dimensions available for this stak */
    private List<DimensionBean> dimensions;

    @Override
    public String toString() {
        return "GranuleStackImpl [dimensions=" + dimensions + ", coverages=" + coverages + "]";
    }

    /** The coverages stored by this Granule stack */
    private List<GridCoverage2D> coverages;

    /** Granule stack constructor. */
    public GranuleStackImpl(CharSequence name, CoordinateReferenceSystem crs, List<DimensionBean> dimensions) {
        super(name, new DummyGridCoverage2D(name, crs));
        this.dimensions = dimensions;
        this.coverages = new ArrayList<>();
    }

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public RenderedImage getRenderedImage() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Unable to return a RenderedImage for a GranuleStack which is made of different coverages: returning null");
        }
        return null;
    }

    @Override
    public Object evaluate(Position point) throws PointOutsideCoverageException, CannotEvaluateException {
        throw new UnsupportedOperationException(
                "This is a multidimensional coverage, you should access its contents calling getGranules");
    }

    @Override
    public int getNumSampleDimensions() {
        throw new UnsupportedOperationException(
                "This is a multidimensional coverage, you should access its contents calling getGranules");
    }

    @Override
    public List<DimensionBean> getDimensions() {
        return dimensions;
    }

    @Override
    public List<GridCoverage2D> getGranules() {
        return coverages;
    }

    public void addCoverage(GridCoverage2D coverage) {
        coverages.add(coverage);
    }

    @Override
    public boolean dispose(boolean force) {
        boolean disposed = true;
        for (GridCoverage2D coverage : coverages) {
            RenderedImage ri = coverage.getRenderedImage();
            disposed &= coverage.dispose(force);
            if (ri instanceof PlanarImage planarImage) {
                ImageUtilities.disposePlanarImageChain(planarImage);
            }
        }
        return disposed;
    }
}
