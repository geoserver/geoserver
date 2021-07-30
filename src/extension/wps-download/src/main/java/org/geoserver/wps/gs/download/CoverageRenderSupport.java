/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.vectorbin.ROIGeometry;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ColorUtilities;
import org.geotools.util.logging.Logging;

/**
 * Some code stolen from WMS RenderedImageMapOutputFormat to help build a transparent image, at
 * least for the RGB and indexed image case. the idea is that this hidden class can stay here for
 * backports, while main can be reconfigured to use the DirectRasterRenderer being factored out in
 * this PR: https://github.com/geoserver/geoserver/pull/5177
 */
class CoverageRenderSupport {

    public static final String RASTER_CHAIN_DEBUG_KEY = "wms.raster.enableRasterChainDebug";
    static final Logger LOGGER = Logging.getLogger(CoverageRenderSupport.class);

    RenderedImage directRasterRender(GridCoverage2D coverage, int mapWidth, int mapHeight) {

        RenderedImage image = coverage.getRenderedImage();
        final Rectangle imageBounds = PlanarImage.wrapRenderedImage(image).getBounds();
        Rectangle mapRasterArea = new Rectangle(0, 0, mapWidth, mapHeight);

        // we need to do a mosaic, let's prepare a layout
        // prepare a final image layout should we need to perform a mosaic or a crop
        final ImageLayout layout = new ImageLayout();
        layout.setMinX(0);
        layout.setMinY(0);
        layout.setWidth(mapWidth);
        layout.setHeight(mapHeight);

        ColorModel cm = image.getColorModel();
        // collecting alpha channels as needed
        PlanarImage[] alphaChannels = null;

        //
        // IndexColorModel
        //
        final ImageWorker worker = new ImageWorker(image);
        final int transparencyType = cm.getTransparency();

        // in case of index color model we try to preserve it, so that output
        // formats that can work with it can enjoy its extra compactness
        double[] bgValues = null;
        if (cm instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) cm;
            // try to find the index that matches the requested background color
            final int bgColorIndex = icm.getTransparentPixel();

            // we did not find the background color, well we have to expand to RGB and then tell
            // Mosaic to use the RGB(A) color as the
            // background
            if (bgColorIndex == -1) {
                // we need to expand the image to RGB
                bgValues = new double[] {0, 0, 0, 0};
                worker.setBackground(bgValues);
                image = worker.forceComponentColorModel().getRenderedImage();
                if (image.getColorModel().hasAlpha()) {
                    image = addAlphaChannel(image);
                    worker.setImage(image);
                }
                cm = image.getColorModel();
            } else {
                // we found the background color in the original image palette therefore we set its
                // index as the bkg value.
                // The final Mosaic will use the IndexColorModel of this image anywa, therefore all
                // we need to do is to force
                // the background to point to the right color in the palette
                bgValues = new double[] {bgColorIndex};
            }

            // collect alpha channels if we have them in order to reuse them later on for mosaic
            // operation
            if (cm.hasAlpha() && bgColorIndex == -1) {
                worker.forceComponentColorModel();
                final RenderedImage alpha = worker.retainLastBand().getRenderedImage();
                alphaChannels = new PlanarImage[] {PlanarImage.wrapRenderedImage(alpha)};
            }
        }

        //
        // ComponentColorModel
        //

        // in case of component color model
        boolean noDataTransparencyApplied = false;
        if (cm instanceof ComponentColorModel) {

            // convert to RGB if necessary
            ComponentColorModel ccm = (ComponentColorModel) cm;
            boolean hasAlpha = cm.hasAlpha();

            // if we have a grayscale image see if we have to expand to RGB
            if (ccm.getNumColorComponents() == 1) {

                if (!hasAlpha) {
                    // no transparency in the original data, so no need to expand to RGB
                    // we need to expand the image with an alpha channel
                    // let's see if we can do that by directly mapping no data to transparent
                    // color
                    RenderedImage transparentImage = grayNoDataTransparent(image);
                    if (transparentImage == null) {
                        image = addAlphaChannel(image);
                        bgValues = new double[] {0, 0, 0, 0};
                    } else {
                        image = transparentImage;
                        noDataTransparencyApplied = true;
                    }
                } else {
                    // extract the alpha channel
                    final ImageWorker iw = new ImageWorker(image);
                    final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                    alphaChannels = new PlanarImage[] {PlanarImage.wrapRenderedImage(alpha)};

                    bgValues = new double[] {0, 0, 0, 0};
                }

                // get back the ColorModel
                cm = image.getColorModel();
                hasAlpha = cm.hasAlpha();
            }

            if (bgValues == null && !noDataTransparencyApplied) {
                if (hasAlpha) {
                    // get alpha
                    final ImageWorker iw = new ImageWorker(image);
                    final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                    alphaChannels = new PlanarImage[] {PlanarImage.wrapRenderedImage(alpha)};

                    bgValues = new double[] {0, 0, 0, 0};

                } else {
                    // If nodata is available, let's try to make it transparent when rgb.
                    RenderedImage imageTransparent = rgbNoDataTransparent(image);
                    if (imageTransparent != null) {
                        image = imageTransparent;
                        noDataTransparencyApplied = true;
                    } else {
                        image = addAlphaChannel(image);
                        // this will work fine for all situation where the color components are
                        // <= 3
                        // e.g., one band rasters with no colormap will have only one usually
                        bgValues = new double[] {0, 0, 0, 0};
                    }
                }
            }
        }

        //
        // If we need to add a collar use mosaic or if we need to blend/apply a bkg color
        ImageWorker iw = new ImageWorker(image);
        Object roiCandidate = image.getProperty("ROI");
        if (!(imageBounds.contains(mapRasterArea) || imageBounds.equals(mapRasterArea))
                || transparencyType != Transparency.OPAQUE
                || iw.getNoData() != null
                || roiCandidate instanceof ROI) {
            image =
                    applyBackgroundTransparency(
                            mapRasterArea,
                            image,
                            mapRasterArea,
                            layout,
                            bgValues,
                            alphaChannels,
                            transparencyType,
                            iw,
                            roiCandidate,
                            noDataTransparencyApplied);
        } else {
            // Check if we need to crop a subset of the produced image, else return it right away
            if (imageBounds.contains(mapRasterArea)
                    && !imageBounds.equals(
                            mapRasterArea)) { // the produced image does not need a final mosaicking
                // operation but a crop!
                iw.setBackground(bgValues);
                iw.crop(0, 0, mapWidth, mapHeight);
                image = iw.getRenderedImage();
            }
        }
        if (LOGGER.isLoggable(Level.FINE) && image != null) {
            LOGGER.log(
                    Level.FINE,
                    "Direct rendering path produced the following image chain:\n"
                            + RenderedImageBrowser.dumpChain(image));
        }
        return image;
    }

    private RenderedImage addAlphaChannel(RenderedImage image) {
        final ImageLayout tempLayout = new ImageLayout(image);
        tempLayout
                .unsetValid(ImageLayout.COLOR_MODEL_MASK)
                .unsetValid(ImageLayout.SAMPLE_MODEL_MASK);
        RenderedImage alpha =
                ConstantDescriptor.create(
                        Float.valueOf(image.getWidth()),
                        Float.valueOf(image.getHeight()),
                        new Byte[] {Byte.valueOf((byte) 255)},
                        new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tempLayout));

        // Using an ImageWorker
        ImageWorker iw = new ImageWorker(image);

        // Adding Alpha band
        iw.addBand(alpha, false, true, null);
        return iw.getRenderedImage();
    }

    /** Optimized method for RGB images to turn noData value to transparent. */
    private RenderedImage rgbNoDataTransparent(RenderedImage image) {
        return makeNoDataTransparent(image, 3);
    }

    /** Optimized method for Gray Scale Byte images to turn noData value to transparent. */
    private RenderedImage grayNoDataTransparent(RenderedImage image) {
        return makeNoDataTransparent(image, 1);
    }

    /** Optimized method to turn noData value to transparent. */
    private RenderedImage makeNoDataTransparent(RenderedImage image, final int numBands) {
        // Using an ImageWorker
        ImageWorker iw = new ImageWorker(image);
        Range noData = iw.getNoData();
        ColorModel cm = image.getColorModel();
        final int numColorBands = cm.getNumColorComponents();
        if (noData != null
                && image.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE
                && numColorBands == numBands
                && cm instanceof ComponentColorModel) {
            int minValue = noData.getMin().intValue();
            int maxValue = noData.getMax().intValue();
            if (minValue == maxValue && minValue >= Byte.MIN_VALUE && minValue <= Byte.MAX_VALUE) {
                // Optimization on images with noData value. Make that value transparent
                Color transparentColor = new Color(minValue, minValue, minValue);
                iw.makeColorTransparent(transparentColor);
                return iw.getRenderedImage();
            }
        }
        return null;
    }

    /**
     * Given a one band (plus eventual alpha) color model and the red part of a gray color returns
     * the appropriate background color to be used in the mosaic operation
     */
    double mapToGrayColor(Color gray, ComponentColorModel cm) {
        double[] rescaleFactors = new double[DataBuffer.TYPE_UNDEFINED + 1];
        rescaleFactors[DataBuffer.TYPE_BYTE] = 1;
        rescaleFactors[DataBuffer.TYPE_SHORT] = 255;
        rescaleFactors[DataBuffer.TYPE_INT] = Integer.MAX_VALUE / 255;
        rescaleFactors[DataBuffer.TYPE_USHORT] = 512;
        rescaleFactors[DataBuffer.TYPE_DOUBLE] = 1 / 255.0;
        rescaleFactors[DataBuffer.TYPE_FLOAT] = 1 / 255.0;
        rescaleFactors[DataBuffer.TYPE_UNDEFINED] = 1;
        return gray.getRed() / rescaleFactors[cm.getTransferType()];
    }

    private RenderedImage applyBackgroundTransparency(
            final Rectangle mapRasterArea,
            RenderedImage image,
            Rectangle intersection,
            final ImageLayout layout,
            double[] bgValues,
            PlanarImage[] alphaChannels,
            final int transparencyType,
            ImageWorker iw,
            Object roiCandidate,
            boolean preProcessedWithTransparency) {
        ROI roi;
        if (roiCandidate instanceof ROI) {
            ROI imageROI = (ROI) roiCandidate;
            try {
                roi = new ROIGeometry(mapRasterArea).intersect(imageROI);
            } catch (IllegalArgumentException e) {
                // in the unlikely event that the ROI does not intersect the target map
                // area an exception will be thrown. Catching the exception instead of checking
                // every time a full intersects test is less expensive, a ROI based image
                // will allocate the full ROI as a single byte[] and then scan it posing
                // memory boundness concerns
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "Failed to intersect image ROI with target bounds, returning empty result",
                            e);
                }
                return null;
            }
        } else {
            roi = new ROIShape(!intersection.isEmpty() ? intersection : mapRasterArea);
        }
        ROI[] rois = (!preProcessedWithTransparency) ? new ROI[] {roi} : null;

        // build the transparency thresholds
        double[][] thresholds =
                (!preProcessedWithTransparency)
                        ? new double[][] {
                            {ColorUtilities.getThreshold(image.getSampleModel().getDataType())}
                        }
                        : null;
        // apply the mosaic
        iw.setRenderingHint(JAI.KEY_IMAGE_LAYOUT, layout);
        iw.setBackground(bgValues);
        iw.mosaic(
                new RenderedImage[] {image},
                alphaChannels != null && transparencyType == Transparency.TRANSLUCENT
                        ? MosaicDescriptor.MOSAIC_TYPE_BLEND
                        : MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                alphaChannels,
                rois,
                thresholds,
                null);
        image = iw.getRenderedImage();
        return image;
    }
}
