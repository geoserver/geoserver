/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.imagen.PlanarImage;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.referencing.operation.MathTransform2D;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;

/**
 * Samples a grid coverage at a given point, returning a single pixel value. Meant to be used for repeated sampling of a
 * coverage along various points. Avoids the overhead of calling
 * {@link org.geotools.coverage.grid.GridCoverage2D#evaluate(org.locationtech.jts.geom.Coordinate, double[])} for each
 * point.
 */
class CoverageSampler {
    static final Logger LOGGER = Logging.getLogger(CoverageSampler.class);

    private final MathTransform2D modelToPixel;
    private final int band;
    private final ReferencedEnvelope coverageBounds;
    private final PlanarImage pi;
    private Raster lastTile;
    private Rectangle lastTileBounds;
    private final double[] position = new double[2];

    public CoverageSampler(GridCoverage2D coverage, int band) {
        this.pi = PlanarImage.wrapRenderedImage(coverage.getRenderedImage());
        this.coverageBounds = coverage.getEnvelope2D();
        this.modelToPixel = coverage.getGridGeometry().getCRSToGrid2D();
        this.band = band;
    }

    public double sample(Coordinate coordinate) throws TransformException {
        if (!coverageBounds.contains(coordinate.x, coordinate.y))
            throw new PointOutsideCoverageException("Point " + coordinate + " is outside coverage bounds");
        position[0] = coordinate.x;
        position[1] = coordinate.y;
        modelToPixel.transform(position, 0, position, 0, 1);
        final int col = (int) Math.round(position[0]);
        final int row = (int) Math.round(position[1]);
        if (lastTile == null || !lastTileBounds.contains(col, row)) {
            LOGGER.log(Level.FINE, () -> Thread.currentThread().getName() + " loading tile at " + col + "/" + row);
            lastTile = pi.getTile(pi.XToTileX(col), pi.YToTileY(row));
            lastTileBounds = lastTile.getBounds();
        }
        try {
            return lastTile.getSampleDouble(col, row, band);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(
                    "Failed to get sample at " + coordinate + " with " + row + "/" + col + " on tile with bounds "
                            + lastTileBounds,
                    e);
        }
    }
}
