/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.layer;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;

import org.apache.commons.io.IOUtils;
import org.geoserver.coverage.GridCoveragesCache;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.TiffCompression;
import org.geotools.factory.Hints;
import org.geotools.image.crop.GTCropDescriptor;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.resources.image.ImageUtilities;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.mime.MimeType;

/**
 * A MetaTile implementation used for Coverage tiles.
 * Tiles will be stored as TIFF images on cache.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Nicola Lagomarsini, GeoSolutions SAS
 *
 */
public class CoverageMetaTile extends MetaTile {
    private static final float ONE = 1.0f;

    private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(CoverageMetaTile.class);

    protected CoverageTileLayer coverageTileLayer = null;

    protected boolean requestTiled = false;

    protected Map<String, String> fullParameters;

    private final static TIFFImageReaderSpi readerSpi = new TIFFImageReaderSpi();

    private final static TIFFImageWriterSpi writerSpi = new TIFFImageWriterSpi();

    final boolean isSingleImage;

    private int gutterValue;

    public int[] getGutter() {
        return gutter.clone();
    }

    protected CoverageMetaTile(CoverageTileLayer layer, GridSubset gridSubset, MimeType responseFormat,
            long[] tileGridPosition, int metaX, int metaY,
            Map<String, String> fullParameters, int gutter) {

        super(gridSubset, responseFormat, null, tileGridPosition, metaX, metaY, gutter);
        this.coverageTileLayer = layer;
        this.gutterValue = gutter;
        this.fullParameters = fullParameters;
        this.isSingleImage = metaX == 1 && metaY == 1;
    }

    protected CoverageTileLayer getLayer() {
        return coverageTileLayer;
    }

    /**
     * Creates the {@link RenderedImage} corresponding to the tile at index {@code tileIdx} and uses a {@link TIFFImageWriter} to encode it.
     * 
     * @see org.geowebcache.layer.MetaTile#writeTileToStream(int, org.geowebcache.io.Resource)
     * 
     */
    @Override
    public boolean writeTileToStream(final int tileIdx, Resource target) throws IOException {

        TIFFImageWriter writer = null;
        FileCacheImageOutputStream iios = null;
        try {
            writer = (TIFFImageWriter) writerSpi.createWriterInstance();
            // TODO: we may consider using a stream adapter to link outputStream 
            // to ImageOutputStream needed by tiff writer. 
            iios = new FileCacheImageOutputStream(target.getOutputStream(),
                    GridCoveragesCache.getTempdir());
            writer.setOutput(iios);
            TiffCompression compression = coverageTileLayer.getTiffCompression();

            // crop subsection of metaTileImage
            RenderedImage ri = getSubTile(tileIdx);

            if (compression == TiffCompression.NONE) {
                writer.write(ri);
            } else {
                writer.write(null, new IIOImage(ri, null, null), compression.getCompressionParams());
            }
            iios.flush();
        } finally {
            IOUtils.closeQuietly(iios);
            if (writer != null) {
                try {
                    writer.dispose();
                } catch (Throwable t) {

                }
            }

        }
        return true;
    }

    /**
     * Return the specified sub tile from this {@link MetaTile} instance.
     * 
     * @param tileIdx
     * @return
     */
    protected RenderedImage getSubTile(int tileIdx) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting subTile: " + tileIdx);
        }
        if (isSingleImage && gutterValue == 0) {
            // In case the image is a single one (which means metaTiling = 1,1) 
            // just return the metatile image
            return metaTileImage;
        }
        final Rectangle tileRect = tiles[tileIdx];
        final int x = tileRect.x;
        final int y = tileRect.y;
        final int tileWidth = tileRect.width;
        final int tileHeight = tileRect.height;
        // now do the splitting and return the requested 
        // sub section as a RenderedImage
        return getSubImage(x, y, tileWidth, tileHeight);
    }

    /**
     * Extract subImage from the available metaTile
     * 
     * @param x
     * @param y
     * @param tileWidth
     * @param tileHeight
     * @return
     * 
     * TODO: We can do this more efficiently. This method has been taken from GWC
     */
    private RenderedImage getSubImage(int x, int y, int tileWidth, int tileHeight) {
//        if (true) return super.createTile(x, y, tileWidth, tileHeight);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting subImage with parameters: x = "  + x + "; y ="
                    + y + " tileWidth = " + tileWidth + " tileHeight = " + tileHeight);
        }
        // check image type
        final int type;
        if (metaTileImage instanceof PlanarImage) {
            type = 1;
        } else if (metaTileImage instanceof BufferedImage) {
            type = 2;
        } else {
            type = 0;
        }
        RenderedImage tile = null;
        switch (type) {
        case 0:
            // do a crop, and then turn it into a buffered image so that we can release
            // the image chain
            RenderedOp cropped = GTCropDescriptor
                    .create(metaTileImage, Float.valueOf(x), Float.valueOf(y),
                            Float.valueOf(tileWidth), Float.valueOf(tileHeight), NO_CACHE);
            tile = cropped.getAsBufferedImage();
            disposeLater(cropped);
            break;
        case 1:
            final PlanarImage pImage = (PlanarImage) metaTileImage;
            final WritableRaster wTile = WritableRaster.createWritableRaster(pImage
                    .getSampleModel().createCompatibleSampleModel(tileWidth, tileHeight),
                    new Point(x, y));
            Rectangle sourceArea = new Rectangle(x, y, tileWidth, tileHeight);
            sourceArea = sourceArea.intersection(pImage.getBounds());

            // copying the data to ensure we don't have side effects when we clean the cache
            pImage.copyData(wTile);
            if (wTile.getMinX() != 0 || wTile.getMinY() != 0) {
                tile = new BufferedImage(pImage.getColorModel(),
                        (WritableRaster) wTile.createWritableTranslatedChild(0, 0), pImage.getColorModel()
                                .isAlphaPremultiplied(), null);
            } else {
                tile = new BufferedImage(pImage.getColorModel(), wTile, pImage.getColorModel()
                        .isAlphaPremultiplied(), null);
            }
            break;
        case 2:
            final BufferedImage image = (BufferedImage) metaTileImage;
            tile =  image.getSubimage(x, y, tileWidth, tileHeight);
            break;
        default:
            throw new IllegalStateException(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,
                    "metaTile class", metaTileImage.getClass().toString()));

        }
        return tile;
    }

    @Override
    public RenderedImage createTile(final int x, final int y, final int tileWidth,
            final int tileHeight) {
       return getSubImage(x, y, tileWidth, tileHeight);
    }

    /**
     * Extract the RenderedImage from the underlying tile.
     * 
     * @param conveyorTile
     * @return
     * @throws IOException
     */
    public static RenderedImage getResource(ConveyorTile conveyorTile) throws IOException {
        final Resource blob = conveyorTile.getBlob();

        InputStream stream = null;
        TIFFImageReader reader = null;
        stream = blob.getInputStream();
        reader = new TIFFImageReader(readerSpi);
        FileCacheImageInputStream fciis = null;
        try {
            fciis = new FileCacheImageInputStream(stream, GridCoveragesCache.getTempdir());
            reader.setInput(fciis);
            return reader.read(0);
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(fciis);

            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {

                }
            }
        }
    }

    /**
     * Create a constant Image using the specified parameters.
     * @param sampleModel
     * @param width
     * @param height
     * @param hints
     * @return
     */
    public static RenderedImage createConstantImage(final SampleModel sampleModel, final int width,
            final int height, final Hints hints, final Double noData) {
        Number[] backgroundValues = ImageUtilities.getBackgroundValues(sampleModel,
                noData != null ? new double[] { noData } : null);
        return ConstantDescriptor.create(width * ONE, height * ONE, backgroundValues, hints);
    }
}
