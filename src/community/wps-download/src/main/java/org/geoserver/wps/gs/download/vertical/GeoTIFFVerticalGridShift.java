/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical;

import it.geosolutions.jaiext.range.Range;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridEnvelope;

/** A Vertical Grid Shift implementation based on an underlying GeoTIFF file */
public class GeoTIFFVerticalGridShift implements VerticalGridShift {

    private static final Logger LOGGER = Logging.getLogger(GeoTIFFVerticalGridShift.class);

    private static final double DELTA = 1E-6;

    /** The valid area being covered by this grid */
    private final GeneralEnvelope validArea;

    /** The Grid's 2D CoordinateReferenceSystem (EPSG code number) */
    private final int crsCode;

    /** The Grid Datatype */
    private final int dataType;

    /** the image containing the grid values */
    private RenderedImage gridImage = null;

    // GRID's Layout
    /** The grid width */
    private int width;

    /** The grid height */
    private int height;

    /** The grid bbox 4 corners */
    private double minX;

    private double maxX;
    private double minY;
    private double maxY;

    private int tileWidth;
    private int tileHeight;
    private int tileGridXOffset;
    private int tileGridYOffset;

    /** The grid's resolution (dx,dy) */
    private double[] resolution;

    /** The geotiff reader used to load the grid file */
    private GeoTiffReader reader = null;

    private double noData = Double.NaN;

    public GeoTIFFVerticalGridShift(File file, int crsCode) throws IOException {
        super();
        this.crsCode = crsCode;
        reader = new GeoTiffReader(file);
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        this.validArea = envelope;

        // Initialize Grid's layout
        ImageLayout layout = reader.getImageLayout();
        resolution = reader.getResolutionLevels(reader.getGridCoverageNames()[0])[0];
        GridEnvelope gridRange = reader.getOriginalGridRange();
        width = gridRange.getSpan(0);
        height = gridRange.getSpan(1);
        minX = envelope.getMinimum(0);
        minY = envelope.getMinimum(1);
        maxX = envelope.getMaximum(0);
        maxY = envelope.getMaximum(1);

        dataType = layout.getSampleModel(null).getDataType();
        GridCoverage2D coverage = reader.read(null);
        gridImage = coverage.getRenderedImage();
        tileWidth = gridImage.getTileWidth();
        tileHeight = gridImage.getTileHeight();
        tileGridXOffset = gridImage.getTileGridXOffset();
        tileGridYOffset = gridImage.getTileGridYOffset();
        ImageWorker iw = new ImageWorker(gridImage);
        Range nodata = iw.getNoData();
        if (nodata != null) {
            this.noData = nodata.getMin().doubleValue();
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isInValidArea(double x, double y) {
        return validArea.contains(new DirectPosition2D(x, y));
    }

    @Override
    public GeneralEnvelope getValidArea() {
        return validArea;
    }

    @Override
    public double[] getResolution() {
        return resolution;
    }

    @Override
    public int getCRSCode() {
        return crsCode;
    }

    @Override
    public boolean shift(double x, double y, double[] z) {
        double gridX = ((x - minX) / resolution[0]);
        // Note that row index grows in the opposite direction of the world's Y axis
        double gridY = ((maxY - y) / resolution[1]);

        // Going from world coordinates X,Y to raster coordinates I,J
        int gridI0 = (int) Math.round(Math.floor(gridX));
        int gridJ0 = (int) Math.round(Math.floor(gridY));

        if (!(gridI0 >= 0 && gridI0 < width)) {
            return false;
        }
        if (!(gridJ0 >= 0 && gridJ0 < height)) {
            return false;
        }

        // The distance from the integer point in the range (0 - 1)
        gridX -= gridI0;
        gridY -= gridJ0;

        // Getting the neighbor pixels
        int gridI1 = gridI0 + 1;
        if (gridI1 >= width) {
            gridI1 = width - 1;
        }
        int gridJ1 = gridJ0 + 1;
        if (gridJ1 >= height) {
            // Don't exceed the limits
            gridJ1 = height - 1;
        }

        double pixelValue = Double.NaN;
        switch (dataType) {
            case DataBuffer.TYPE_FLOAT:
                pixelValue = interpolateFloat(gridX, gridY, gridI0, gridI1, gridJ0, gridJ1);
                break;
            default:
                pixelValue = interpolateDouble(gridX, gridY, gridI0, gridI1, gridJ0, gridJ1);
                break;
        }

        if (Double.isNaN(pixelValue)) {
            return false;
        }
        // Finally apply the shift
        z[0] += pixelValue;
        return true;
    }

    private double interpolateDouble(
            double gridX, double gridY, int gridI0, int gridI1, int gridJ0, int gridJ1) {
        // Get the 4 pixels of the 2x2 matrix:
        // (I0,J0) ----- (I1,J0)
        //   |              |
        //   |              |
        //   |              |
        // (I0,J1) ----- (I1,J1)
        double pixelValue = 0.0;
        double v00 = readDouble(gridI0, gridJ0);
        double v10 = readDouble(gridI1, gridJ0);
        double v01 = readDouble(gridI0, gridJ1);
        double v11 = readDouble(gridI1, gridJ1);

        double pixelsWeight = 0.0;
        double weight = 0.0;
        int contributingPixels = 0;

        if (!Double.isNaN(v00)) {
            weight = (1.0 - gridX) * (1.0 - gridY);
            pixelValue += v00 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (!Double.isNaN(v10)) {
            weight = (gridX) * (1.0 - gridY);
            pixelValue += v10 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (!Double.isNaN(v01)) {
            weight = (1.0 - gridX) * (gridY);
            pixelValue += v01 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (!Double.isNaN(v11)) {
            weight = (gridX) * (gridY);
            pixelValue += v11 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (contributingPixels == 0) {
            pixelValue = Double.NaN;
        } else if (contributingPixels != 4) {
            // Adapt the value to contributing pixels
            pixelValue /= pixelsWeight;
        }
        return pixelValue;
    }

    private float interpolateFloat(
            double gridX, double gridY, int gridI0, int gridI1, int gridJ0, int gridJ1) {
        // Get the 4 pixels of the 2x2 matrix:
        // (I0,J0) ----- (I1,J0)
        //   |              |
        //   |              |
        //   |              |
        // (I0,J1) ----- (I1,J1)
        float pixelValue = 0.0f;
        float v00 = readFloat(gridI0, gridJ0);
        float v10 = readFloat(gridI1, gridJ0);
        float v01 = readFloat(gridI0, gridJ1);
        float v11 = readFloat(gridI1, gridJ1);

        float pixelsWeight = 0.0f;
        float weight = 0.0f;
        int contributingPixels = 0;

        if (!Float.isNaN(v00)) {
            weight = (float) ((1.0 - gridX) * (1.0 - gridY));
            pixelValue += v00 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (!Float.isNaN(v10)) {
            weight = (float) ((gridX) * (1.0 - gridY));
            pixelValue += v10 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (!Float.isNaN(v01)) {
            weight = (float) ((1.0 - gridX) * (gridY));
            pixelValue += v01 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (!Float.isNaN(v11)) {
            weight = (float) ((gridX) * (gridY));
            pixelValue += v11 * weight;
            pixelsWeight += weight;
            contributingPixels++;
        }
        if (contributingPixels == 0) {
            pixelValue = Float.NaN;
        } else if (contributingPixels != 4) {
            // Adapt the value to contributing pixels
            pixelValue /= pixelsWeight;
        }
        return pixelValue;
    }

    private double readDouble(int gridI, int gridJ) {
        Raster tile =
                gridImage.getTile(
                        PlanarImage.XToTileX(gridI, tileGridXOffset, tileWidth),
                        PlanarImage.XToTileX(gridJ, tileGridYOffset, tileHeight));
        double val = tile.getSampleDouble(gridI, gridJ, 0);
        if (Math.abs(val - noData) < DELTA) {
            val = Double.NaN;
        }

        return val;
    }

    private float readFloat(int gridI, int gridJ) {
        Raster tile =
                gridImage.getTile(
                        PlanarImage.XToTileX(gridI, tileGridXOffset, tileWidth),
                        PlanarImage.XToTileX(gridJ, tileGridYOffset, tileHeight));
        float val = tile.getSampleFloat(gridI, gridJ, 0);
        if (Math.abs(val - noData) < DELTA) {
            val = Float.NaN;
        }
        return val;
    }

    @Override
    public void dispose() {
        if (gridImage != null) {
            try {
                ImageUtilities.disposeImage(gridImage);
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(t.getLocalizedMessage() + " During image disposal");
                }
            }
        }
        if (reader != null) {
            try {
                reader.dispose();
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(t.getLocalizedMessage() + " During reader disposal");
                }
            }
        }
    }
}
