/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.MosaicDescriptor;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.JpegOrPngChooser;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileMatrix;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ColorUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.logging.Logging;

public class GeopkgRasterPPIO extends GeopkgPPIO {

    static final Logger LOGGER = Logging.getLogger(GeopkgRasterPPIO.class);

    private static final int TILE_SIZE = 256;

    /** Constructor. */
    protected GeopkgRasterPPIO() {
        super(GridCoverage2D.class, GridCoverage2D.class);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        GridCoverage2D coverage = (GridCoverage2D) value;
        RenderedImage ri = coverage.getRenderedImage();
        checkImageStructure(ri);

        // expand the image so that it's an exact multiple of the tile size
        CoverageRenderSupport support = new CoverageRenderSupport();
        RenderedImage expandedImage =
                support.directRasterRender(
                        coverage,
                        upRound(ri.getWidth(), TILE_SIZE),
                        upRound(ri.getHeight(), TILE_SIZE));

        File file = File.createTempFile("geopkg", ".tmp.gpkg");
        try {
            // write the geopackage
            try (GeoPackage geopkg = getGeoPackage(file)) {
                // create base tables
                geopkg.init();

                // start with a tentative name, if the coverages carries a ResourceInfo we'll
                // do better later
                TileEntry te = new TileEntry();
                te.setTableName(coverage.getName().toString());

                // setup the meta information
                if (coverage instanceof MetaGridCoverage2D) {
                    MetaGridCoverage2D meta = (MetaGridCoverage2D) coverage;
                    Object resourceInfo = meta.getUserData().get(ResourceInfo.class);
                    setupEntryMetadata(te, resourceInfo);
                }

                // setup the tile matrix set info
                Integer srid = CRS.lookupEpsgCode(coverage.getCoordinateReferenceSystem2D(), true);
                if (srid == null)
                    throw new ServiceException(
                            "Could not lookup the SRID code from the coverage, this is unexpected");
                te.setSrid(srid);
                ReferencedEnvelope envelope =
                        ReferencedEnvelope.reference(coverage.getEnvelope2D());
                te.setBounds(envelope);

                // build the tile matrix best matching the data
                TileMatrix tm = buildTileMatrix(coverage);
                te.getTileMatricies().add(tm);
                te.setTileMatrixSetBounds(getTileMatrixBounds(envelope, tm));

                // setup the layer and populate the tiles
                geopkg.create(te);
                encodeTiles(expandedImage, geopkg, te, tm);
            }

            // copy over to the output
            try (InputStream is = new FileInputStream(file)) {
                IOUtils.copy(is, os);
            }
        } finally {
            file.delete();
        }
    }

    private int upRound(int value, int size) {
        if (value % size == 0) return value;
        return (value / size + 1) * size;
    }

    /** Assuming a perfect match on the top/left corner and grid going down */
    private ReferencedEnvelope getTileMatrixBounds(ReferencedEnvelope envelope, TileMatrix tm) {
        double minX = envelope.getMinX();
        double maxY = envelope.getMaxY();
        ReferencedEnvelope re =
                new ReferencedEnvelope(
                        minX,
                        minX + tm.getMatrixWidth() * tm.getTileWidth() * tm.getXPixelSize(),
                        maxY - tm.getMatrixHeight() * tm.getTileHeight() * tm.getYPixelSize(),
                        maxY,
                        envelope.getCoordinateReferenceSystem());
        return re;
    }

    /** Encodes tiles starting from an image whose size in an exact multiple of the tile size */
    private void encodeTiles(RenderedImage ri, GeoPackage geopkg, TileEntry te, TileMatrix tm)
            throws IOException {
        int tileHeight = tm.getTileHeight();
        int tileWidth = tm.getTileWidth();

        // encode, top to bottom, left to right
        for (int r = 0; r < tm.getMatrixHeight(); r++) {
            int bottom = ri.getMinY() + r * tileHeight;
            for (int c = 0; c < tm.getMatrixWidth(); c++) {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    int left = c * tileWidth + ri.getMinX();
                    ImageWorker iw = new ImageWorker(ri);

                    // crop tile out of image (might result in a tile smaller than desired)
                    Rectangle crop = new Rectangle(left, bottom, tileWidth, tileHeight);
                    iw.crop(crop.x, crop.y, crop.width, crop.height);

                    // check if there is any data in the tile, the format allows to just
                    // skip the tile if it has empty contents
                    if (isFullyTransparent(iw.getRenderedImage())) {
                        LOGGER.log(
                                Level.FINE,
                                "Tile at row {0}, col {1} is empty, skipping",
                                new Object[] {r, c});
                        continue;
                    }

                    // since everything is top/left aligned, expansion might have to be done
                    // only on the right or bottom (in other words, same image origin, larger size)
                    if (needsExpansion(iw)) {
                        expandImageToTile(iw);
                    }

                    // encode in JPEG if solid, PNG otherwise
                    RenderedImage finalImage = iw.getRenderedImage();
                    JpegOrPngChooser chooser = new JpegOrPngChooser(finalImage);
                    if (chooser.isJpegPreferred()) {
                        iw.writeJPEG(bos, "JPEG", 0.75f, false);
                    } else {
                        // tried PNGJ too but got weird output with the built-in nurc:mosaic
                        // empty tiles, repeated ones, it does not happen with IW
                        iw.writePNG(
                                bos,
                                "FILTERED",
                                0.75F,
                                false,
                                finalImage.getColorModel() instanceof IndexColorModel);
                    }

                    // finally add to the geopackage
                    Tile tile = new Tile(0, c, r, bos.toByteArray());
                    geopkg.add(te, tile);
                }
            }
        }
    }

    private boolean isFullyTransparent(RenderedImage ri) {
        ColorModel cm = ri.getColorModel();

        // if there is no way to express a missing pixel, exit fast
        ImageWorker iw = new ImageWorker(ri);
        if (cm.getTransparency() == Transparency.OPAQUE && iw.getNoData() == null) return false;

        // in case of index color model, there could be a transparent pixel
        if (cm instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) cm;
            int transparentPixel = icm.getTransparentPixel();
            if (transparentPixel != -1) {
                // are all pixels equal to the transparent pixel?
                return iw.getMaximums()[0] == transparentPixel
                        && iw.getMinimums()[0] == transparentPixel;
            }

            // the palette contains alpha then (the first test showed it has a transparency)
            boolean multipleTransparentPixels = false;
            for (int i = 0; i < icm.getMapSize(); i++) {
                if (icm.getAlpha(i) == 0) {
                    if (transparentPixel > -1) multipleTransparentPixels = true;
                    transparentPixel = i;
                }
            }

            // only one fully transparent pixel? if so, same as having the designed tx pixel
            if (!multipleTransparentPixels) {
                return iw.getMaximums()[0] == transparentPixel
                        && iw.getMinimums()[0] == transparentPixel;
            } else {
                // too complicated, expand to RGBA then
                iw.forceComponentColorModel();
                cm = iw.getRenderedImage().getColorModel();
            }
        }

        if (cm instanceof PackedColorModel) {
            // too complicated, expand to RGBA then
            iw.forceComponentColorModel();
            cm = iw.getRenderedImage().getColorModel();
        }

        if (cm instanceof ComponentColorModel) {
            // assume last band is alpha (cannot do bitmask tx in a CCM no?)
            iw.retainLastBand();
            // if the maximum on the tile is zero, then it's empty
            return iw.getMaximums()[0] == 0;
        }

        // we either don't know or there is some pixel that is not transparent
        return false;
    }

    private boolean needsExpansion(ImageWorker iw) {
        RenderedImage ri = iw.getRenderedImage();
        int width = ri.getWidth();
        int height = ri.getHeight();
        return width < TILE_SIZE || height < TILE_SIZE;
    }

    private void expandImageToTile(ImageWorker iw) {
        LOGGER.warning("Still missing transparency/nodata handling");
        RenderedImage ri = iw.getRenderedImage();
        double[][] thresholds = {{ColorUtilities.getThreshold(ri.getSampleModel().getDataType())}};
        ImageLayout layout = new ImageLayout(ri);
        layout.setWidth(TILE_SIZE);
        layout.setHeight(TILE_SIZE);
        iw.setRenderingHint(JAI.KEY_IMAGE_LAYOUT, layout);
        iw.mosaic(
                new RenderedImage[] {ri},
                MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                null,
                null,
                thresholds,
                null);
    }

    private void checkImageStructure(RenderedImage ri) {
        SampleModel sm = ri.getSampleModel();
        if (sm.getDataType() != DataBuffer.TYPE_BYTE)
            throw new ServiceException(
                    "Cannot encode images with this data type, only byte (0) is supported: "
                            + sm.getDataType()
                            + "\n See https://docs.oracle.com/javase/7/docs/api/constant-values.html#java.awt.image.DataBuffer.TYPE_BYTE for the meaning of the code type");

        if (sm.getNumBands() > 4)
            throw new ServiceException("Cannot encode images with more than four bands");

        ColorModel cm = ri.getColorModel();
        if (sm.getNumBands() == 2 && !cm.hasAlpha())
            throw new ServiceException(
                    "Cannot encode two banded images unless they are gray/alpha");
    }

    /**
     * Builds a tile matrix that is as close as possible to the phisical layout of the data
     *
     * @param coverage
     * @return
     */
    private TileMatrix buildTileMatrix(GridCoverage2D coverage) {
        GridGeometry2D gg = coverage.getGridGeometry();
        GridEnvelope2D range = gg.getGridRange2D();
        double matrixWidth = getTilesNumber(range.getWidth());
        double matrixHeight = getTilesNumber(range.getHeight());

        MathTransform gridToWorld = gg.getGridToCRS(PixelInCell.CELL_CORNER);
        if (!(gridToWorld instanceof AffineTransform2D)) {
            throw new ServiceException(
                    "Need an affine grid to world tranform in order to setup the tile matrix, but found: "
                            + gridToWorld);
        }
        AffineTransform2D at = (AffineTransform2D) gridToWorld;

        TileMatrix tm = new TileMatrix();
        tm.setZoomLevel(0);
        tm.setTileWidth(TILE_SIZE);
        tm.setTileHeight(TILE_SIZE);
        tm.setMatrixWidth((int) matrixWidth);
        tm.setMatrixHeight((int) matrixHeight);
        tm.setXPixelSize(Math.abs(at.getScaleX()));
        tm.setYPixelSize(Math.abs(at.getScaleY()));

        return tm;
    }

    private double getTilesNumber(double width) {
        double count = width / TILE_SIZE;
        if (count % 1 != 0) count++;
        return count;
    }
}
