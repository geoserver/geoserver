/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.wms.map.RenderedImageMapOutputFormat.ADV_PROJECTION_HANDLING_FORMAT_OPTION;
import static org.geoserver.wms.map.RenderedImageMapOutputFormat.MAP_WRAPPING_FORMAT_OPTION;
import static org.geoserver.wms.map.RenderedImageMapOutputFormat.getConfiguredLayerInterpolation;
import static org.geoserver.wms.map.RenderedImageMapOutputFormat.getFormatOptionAsBoolean;
import static org.geoserver.wms.map.RenderedImageMapOutputFormat.toInterpolationObject;
import static org.geotools.renderer.lite.gridcoverage2d.ChannelSelectionUpdateStyleVisitor.getBandIndicesFromSelectionChannels;

import it.geosolutions.jaiext.lookup.LookupTable;
import it.geosolutions.jaiext.lookup.LookupTableFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.vectorbin.ROIGeometry;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterDescriptorGroup;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ColorUtilities;
import org.geotools.map.Layer;
import org.geotools.parameter.Parameter;
import org.geotools.process.Processors;
import org.geotools.process.function.ProcessFunction;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.RenderingTransformationHelper;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.util.logging.Logging;

class DirectRasterRenderer {

    static final Logger LOGGER = Logging.getLogger(DirectRasterRenderer.class);

    private static final int MAX_TILE_SIZE = 1024;

    /** Disable Gutter key */
    public static final String DISABLE_GUTTER_KEY = "wms.raster.disableGutter";

    /** Disable Gutter */
    private static Boolean DISABLE_GUTTER = Boolean.getBoolean(DISABLE_GUTTER_KEY);

    /** Show Chain key */
    public static final String RASTER_CHAIN_DEBUG_KEY = "wms.raster.enableRasterChainDebug";

    /** Show Chain */
    private static Boolean RASTER_CHAIN_DEBUG = Boolean.getBoolean(RASTER_CHAIN_DEBUG_KEY);

    private final int mapWidth;
    private final int mapHeight;
    private final Layer layer;

    /** An object keeping track of the reader and related params used to perform the rendering */
    static class ReadingContext {

        GridCoverage2DReader reader;
        Object params;
    }

    private final WMSMapContent mapContent;
    private final int layerIndex;
    private final Interpolation layerInterpolation;
    private final WMS wms;
    private Expression transformation;
    private final ReferencedEnvelope mapEnvelope;
    private final Rectangle mapRasterArea;
    private final AffineTransform worldToScreen;
    private final Color bgColor;
    private final boolean transparent;
    private final Interpolation interpolation;
    private int tileSizeX;
    private int tileSizeY;
    private final int[] bandIndices;

    private RasterSymbolizer symbolizer;

    /**
     * @param wms
     * @param mapContent The map definition (used for map size and transparency/color management) *
     * @param layerIndex the layer that is supposed to contain a coverage *
     * @param layerInterpolation
     * @param transparencySupported
     */
    public DirectRasterRenderer(
            WMS wms,
            WMSMapContent mapContent,
            int layerIndex,
            Interpolation layerInterpolation,
            boolean transparencySupported)
            throws FactoryException {
        this.wms = wms;
        this.mapContent = mapContent;
        this.layerIndex = layerIndex;
        this.layerInterpolation = layerInterpolation;

        //
        // extract the raster symbolizers and the eventual rendering transformation
        //
        double scaleDenominator = mapContent.getScaleDenominator(true);
        this.layer = mapContent.layers().get(layerIndex);
        FeatureType featureType = layer.getFeatureSource().getSchema();
        Style style = layer.getStyle();

        RasterSymbolizerVisitor visitor =
                new RasterSymbolizerVisitor(scaleDenominator, featureType);
        style.accept(visitor);

        List<RasterSymbolizer> symbolizers = visitor.getRasterSymbolizers();
        if (symbolizers.size() == 1) {
            this.symbolizer = symbolizers.get(0);
            this.transformation = visitor.getRasterRenderingTransformation();
        }
        //
        // Dimensions
        //
        this.mapWidth = mapContent.getMapWidth();
        this.mapHeight = mapContent.getMapHeight();
        // force east/north, otherwise the reading code might think we are reprojecting
        // and start adding padding around the requests
        this.mapEnvelope = getEastNorthEnvelope(mapContent.getRenderingArea());
        this.mapRasterArea = new Rectangle(0, 0, mapWidth, mapHeight);
        this.worldToScreen = RendererUtilities.worldToScreenTransform(mapEnvelope, mapRasterArea);

        //
        // Check transparency and bg color
        //
        this.transparent = mapContent.isTransparent() && transparencySupported;
        this.bgColor = getBackgroundColor(transparent);

        //
        // Grab the interpolation
        //
        this.interpolation = getInterpolation();

        //
        // Tiling
        //
        // if there is a output tile size hint, use it, otherwise use the output size itself
        this.tileSizeX = -1;
        this.tileSizeY = -1;
        if (mapContent.getTileSize() != -1) {
            tileSizeX = tileSizeY = mapContent.getTileSize();
        } else if (mapWidth < MAX_TILE_SIZE && mapHeight < MAX_TILE_SIZE) {
            tileSizeX = mapWidth;
            tileSizeY = mapHeight;
        }

        //
        // Band selection
        //
        if (transformation == null && symbolizer != null) {
            this.bandIndices = getBandIndicesFromSelectionChannels(symbolizer);
        } else {
            this.bandIndices = null;
        }
    }

    /**
     * Renders a single coverage as the final RenderedImage to be encoded, skipping all of the
     * Java2D machinery and using a pure JAI chain of transformations instead. This considerably
     * improves both scalability and performance
     *
     * @return the result of rendering the coverage, or null if there was no coverage, or the
     *     coverage could not be renderer for some reason
     */
    public RenderedImage render() throws FactoryException {
        // if the symbolizer was not extracted in the contructor, we cannot proceed
        if (symbolizer == null) return null;

        // direct raster rendering uses Query.ALL for the style query which is
        // inefficient for vector sources
        if (isVectorSource(transformation)) {
            return null;
        }

        // actual read
        final ReadingContext context = new ReadingContext();
        RenderedImage image = null;
        GridCoverage2D coverage = null;
        RenderingHints interpolationHints =
                new RenderingHints(JAI.KEY_INTERPOLATION, interpolation);
        try {
            final Color readerBgColor = transparent ? null : bgColor;
            CoordinateReferenceSystem mapCRS = mapEnvelope.getCoordinateReferenceSystem();
            boolean advancedProjectionHandling =
                    wms.isAdvancedProjectionHandlingEnabled()
                            && getFormatOptionAsBoolean(
                                    mapContent.getRequest(), ADV_PROJECTION_HANDLING_FORMAT_OPTION);
            if (transformation == null && advancedProjectionHandling) {
                image = readWithProjectionHandling(interpolationHints, readerBgColor, mapCRS);
            } else {
                //
                // Prepare the reading parameters (for the RT case)
                //
                final CoordinateReferenceSystem coverageCRS =
                        layer.getFeatureSource().getSchema().getCoordinateReferenceSystem();
                final GridGeometry2D readGG = getReadGeometry(mapCRS, coverageCRS);

                if (transformation != null) {
                    Object result = readAndTransform(interpolationHints, coverageCRS, readGG);
                    if (result == null) {
                        coverage = null;
                    } else if (result instanceof GridCoverage2D) {
                        coverage = (GridCoverage2D) result;
                        symbolizer =
                                updateSymbolizerForBandSelection(context, symbolizer, bandIndices);
                    } else {
                        // we don't know how to handle this case, we'll let streaming renderer fall
                        // back on this one
                        return null;
                    }
                } else {
                    coverage = readCoverage(context, readerBgColor, readGG);
                }
                // Nothing found, we return a constant image with background value
                if (coverage == null) {
                    // we're outside of the coverage definition area, return an empty space
                    image = createBkgImage(mapWidth, mapHeight, bgColor, null);
                }
                // If the image has not already been prepared, we render the image using the
                // GridCoverageRenderer
                if (image == null) {
                    // apply the grid coverage renderer
                    final GridCoverageRenderer gcr =
                            new GridCoverageRenderer(
                                    mapCRS,
                                    ReferencedEnvelope.reference(readGG.getEnvelope()),
                                    readGG.getGridRange2D(),
                                    worldToScreen,
                                    interpolationHints);
                    gcr.setAdvancedProjectionHandlingEnabled(false);

                    // create a solid color empty image
                    // use null background, background is handled separately
                    image =
                            gcr.renderImage(
                                    coverage,
                                    symbolizer,
                                    interpolation,
                                    null,
                                    tileSizeX,
                                    tileSizeY);
                }
            }
        } catch (Throwable e) {
            throw new ServiceException(e);
        }

        // check if we managed to process the coverage into an image
        if (image == null) {
            return null;
        }

        // check if the image intersects the requested area at all return null and be done with it
        final Rectangle imageBounds = PlanarImage.wrapRenderedImage(image).getBounds();
        Rectangle intersection = imageBounds.intersection(mapRasterArea);
        if (intersection.isEmpty()) {
            return null;
        }

        ////
        //
        // Final Touch
        ////
        //
        // We need to prepare the background values for the finalcut on the image we have prepared.
        // If we need to enlarge the image we go with Mosaic if we need to crop we use Crop. Notice
        // that if we need to mess up with the background color we need to go by Mosaic and we
        // cannot use
        // Crop since it does not support changing the bkg color.
        //
        ////

        // we need to do a mosaic, let's prepare a layout
        // prepare a final image layout should we need to perform a mosaic or a crop
        final ImageLayout layout = new ImageLayout();
        layout.setMinX(0);
        layout.setMinY(0);
        layout.setWidth(mapWidth);
        layout.setHeight(mapHeight);
        if (tileSizeX > 0 && tileSizeY > 0) {
            layout.setTileGridXOffset(0);
            layout.setTileGridYOffset(0);
            layout.setTileWidth(tileSizeX);
            layout.setTileHeight(tileSizeY);
        }

        // We need to find the background color expressed in terms of image color components
        // (which depends on the color model nature, the input and output transparency)
        // TODO: there must be a more general way to turn a color into the
        // required components for a certain color model... right???
        ColorModel cm = image.getColorModel();
        double[] bgValues = null;
        // collecting alpha channels as needed
        PlanarImage[] alphaChannels = null;

        //
        // IndexColorModel
        //
        int transparencyType = cm.getTransparency();

        // in case of index color model we try to preserve it, so that output
        // formats that can work with it can enjoy its extra compactness
        if (cm instanceof IndexColorModel) {
            final ImageWorker worker = new ImageWorker(image);
            IndexColorModel icm = (IndexColorModel) cm;
            // try to find the index that matches the requested background color
            final int bgColorIndex;
            if (transparent) {
                bgColorIndex = icm.getTransparentPixel();
            } else {
                if (icm.hasAlpha() && icm.isAlphaPremultiplied()) {
                    // uncommon case that we don't have the code to handle directly
                    bgColorIndex = -1;
                } else {
                    if (icm.getTransparency() != Transparency.OPAQUE) {
                        // we have a translucent image, so the bg color needs to be merged into
                        // the palette
                        icm = ColorUtilities.applyBackgroundColor(icm, bgColor);
                        cm = icm;
                        ImageLayout ilColorModel = new ImageLayout(image);
                        ilColorModel.setColorModel(icm);
                        RenderingHints hints =
                                new RenderingHints(JAI.KEY_IMAGE_LAYOUT, ilColorModel);
                        worker.setRenderingHints(hints);
                        worker.format(image.getSampleModel().getDataType());
                        image = worker.getRenderedImage();
                    }
                    bgColorIndex = ColorUtilities.findColorIndex(bgColor, icm);
                }
            }

            // we did not find the background color, well we have to expand to RGB and then tell
            // Mosaic to use the RGB(A) color as the
            // background
            if (bgColorIndex == -1) {
                // we need to expand the image to RGB
                bgValues =
                        new double[] {
                            bgColor.getRed(),
                            bgColor.getGreen(),
                            bgColor.getBlue(),
                            transparent ? 0 : 255
                        };
                worker.setBackground(bgValues);
                image = worker.forceComponentColorModel().getRenderedImage();
                if (transparent && !image.getColorModel().hasAlpha()) {
                    image = addAlphaChannel(image);
                    worker.setImage(image);
                    transparencyType = image.getColorModel().getTransparency();
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
                if ((!isLevelOfGray(bgColor) && !transparent)
                        || (ccm.getTransferType() == DataBuffer.TYPE_DOUBLE
                                || ccm.getTransferType() == DataBuffer.TYPE_FLOAT
                                || ccm.getTransferType() == DataBuffer.TYPE_UNDEFINED)) {
                    // expand to RGB, this is not a case we can optimize
                    final ImageWorker iw = new ImageWorker(image);
                    if (hasAlpha) {
                        final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                        // get first band
                        final RenderedImage gray =
                                new ImageWorker(image).retainFirstBand().getRenderedImage();
                        image =
                                new ImageWorker(gray)
                                        .bandMerge(3)
                                        .addBand(alpha, false)
                                        .forceComponentColorModel()
                                        .forceColorSpaceRGB()
                                        .getRenderedImage();
                    } else {
                        image =
                                iw.bandMerge(3)
                                        .forceComponentColorModel()
                                        .forceColorSpaceRGB()
                                        .getRenderedImage();
                    }
                } else if (!hasAlpha) {
                    // no transparency in the original data, so no need to expand to RGB
                    if (transparent) {
                        // we need to expand the image with an alpha channel
                        // let's see if we can do that by directly mapping no data to transparent
                        // color
                        RenderedImage transparentImage = grayNoDataTransparent(image);
                        if (transparentImage == null) {
                            image = addAlphaChannel(image);
                            bgValues = new double[] {mapToGrayColor(bgColor, ccm), 0};
                        } else {
                            image = transparentImage;
                            noDataTransparencyApplied = true;
                        }
                    } else {
                        bgValues = new double[] {mapToGrayColor(bgColor, ccm)};
                    }
                } else {
                    // extract the alpha channel
                    final ImageWorker iw = new ImageWorker(image);
                    final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                    alphaChannels = new PlanarImage[] {PlanarImage.wrapRenderedImage(alpha)};

                    if (transparent) {
                        bgValues = new double[] {mapToGrayColor(bgColor, ccm), 0};
                    } else {
                        bgValues = new double[] {mapToGrayColor(bgColor, ccm), 255};
                    }
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

                    if (transparent) {
                        bgValues =
                                new double[] {
                                    bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0
                                };
                    } else {
                        bgValues =
                                new double[] {
                                    bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255
                                };
                    }
                } else {
                    if (transparent) {
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
                    } else {
                        // TODO: handle the case where the component color model is not RGB
                        // We cannot use ImageWorker as is because it basically seems to assume
                        // component -> 3 band in forceComponentColorModel()
                        // but I guess we'll need to turn the image into a 3 band RGB one.
                        bgValues =
                                new double[] {
                                    bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue()
                                };
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
                            intersection,
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
        Map<String, String> rawKvp = null;
        if (RASTER_CHAIN_DEBUG
                && ((rawKvp = mapContent.getRequest().getRawKvp()) != null)
                && Boolean.valueOf(rawKvp.get("showchain"))) {
            RenderedImageBrowser.showChainAndWaitOnClose(image);
        }
        return image;
    }

    private GridCoverage2D readCoverage(
            ReadingContext context, Color readerBgColor, GridGeometry2D readGG) throws IOException {
        GridCoverage2D coverage;
        //
        // Get the reader
        //
        final Feature feature =
                mapContent.layers().get(0).getFeatureSource().getFeatures().features().next();
        final GridCoverage2DReader reader =
                (GridCoverage2DReader) feature.getProperty("grid").getValue();
        // render via grid coverage renderer, that will apply the advanced projection
        // handling
        final Object params = feature.getProperty("params").getValue();
        context.reader = reader;
        context.params = params;
        coverage =
                readBestCoverage(
                        context,
                        ReferencedEnvelope.reference(readGG.getEnvelope()),
                        readGG.getGridRange2D(),
                        interpolation,
                        readerBgColor,
                        bandIndices);

        symbolizer = updateSymbolizerForBandSelection(context, symbolizer, bandIndices);
        return coverage;
    }

    private Object readAndTransform(
            RenderingHints interpolationHints,
            CoordinateReferenceSystem coverageCRS,
            GridGeometry2D readGG)
            throws IOException, SchemaException, TransformException, FactoryException {

        boolean advancedProjectionHandling =
                wms.isAdvancedProjectionHandlingEnabled()
                        && getFormatOptionAsBoolean(
                                mapContent.getRequest(), ADV_PROJECTION_HANDLING_FORMAT_OPTION);
        boolean continuousMapWrapping =
                wms.isContinuousMapWrappingEnabled()
                        && getFormatOptionAsBoolean(
                                mapContent.getRequest(), MAP_WRAPPING_FORMAT_OPTION);
        RenderingTransformationHelper helper =
                new GCRRenderingTransformationHelper(
                        mapContent,
                        interpolation,
                        advancedProjectionHandling,
                        continuousMapWrapping);
        Object result =
                helper.applyRenderingTransformation(
                        transformation,
                        layer.getFeatureSource(),
                        layer.getQuery(),
                        Query.ALL,
                        readGG,
                        coverageCRS,
                        interpolationHints);
        return result;
    }

    private GridGeometry2D getReadGeometry(
            CoordinateReferenceSystem mapCRS, CoordinateReferenceSystem coverageCRS)
            throws IOException {
        final GridGeometry2D readGG;
        boolean useGutter = !DISABLE_GUTTER;
        if (useGutter) {
            final boolean equalsMetadata = CRS.equalsIgnoreMetadata(mapCRS, coverageCRS);
            boolean sameCRS;
            try {
                sameCRS =
                        equalsMetadata
                                || CRS.findMathTransform(mapCRS, coverageCRS, true).isIdentity();
            } catch (FactoryException e1) {
                final IOException ioe = new IOException();
                ioe.initCause(e1);
                throw ioe;
            }
            useGutter = !sameCRS || !(interpolation instanceof InterpolationNearest);
        }

        if (!useGutter) {
            readGG = new GridGeometry2D(new GridEnvelope2D(mapRasterArea), mapEnvelope);
        } else {
            //
            // SG added gutter to the drawing. We need to investigate much more and also we
            // need to do this only when needed
            //
            // enlarge raster area
            Rectangle bufferedTargetArea = (Rectangle) mapRasterArea.clone();
            bufferedTargetArea.add(
                    mapRasterArea.x + mapRasterArea.width + 10,
                    mapRasterArea.y + mapRasterArea.height + 10);
            bufferedTargetArea.add(mapRasterArea.x - 10, mapRasterArea.y - 10);

            // now create the final envelope accordingly
            try {
                readGG =
                        new GridGeometry2D(
                                new GridEnvelope2D(bufferedTargetArea),
                                PixelInCell.CELL_CORNER,
                                new AffineTransform2D(worldToScreen.createInverse()),
                                mapCRS,
                                null);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return readGG;
    }

    private RenderedImage readWithProjectionHandling(
            RenderingHints interpolationHints,
            Color readerBgColor,
            CoordinateReferenceSystem mapCRS)
            throws IOException, TransformException, NoninvertibleTransformException,
                    FactoryException {
        //
        // Get the reader
        //
        final Feature feature =
                DataUtilities.first(mapContent.layers().get(0).getFeatureSource().getFeatures());
        if (feature == null || feature.getProperty("grid") == null) {
            return null;
        }
        final GridCoverage2DReader reader =
                (GridCoverage2DReader) feature.getProperty("grid").getValue();
        // render via grid coverage renderer, that will apply the advanced projection
        // handling
        final Object params = feature.getProperty("params").getValue();
        GeneralParameterValue[] readParameters =
                getReadParameters(params, null, null, interpolation, readerBgColor, bandIndices);
        final GridCoverageRenderer gcr =
                new GridCoverageRenderer(
                        mapCRS, mapEnvelope, mapRasterArea, worldToScreen, interpolationHints);
        gcr.setAdvancedProjectionHandlingEnabled(true);
        boolean continuousMapWrappingEnabled =
                wms.isContinuousMapWrappingEnabled()
                        && getFormatOptionAsBoolean(
                                mapContent.getRequest(), MAP_WRAPPING_FORMAT_OPTION);
        gcr.setWrapEnabled(continuousMapWrappingEnabled);
        // use null background here, background color is handled afterwards
        RenderedImage image =
                gcr.renderImage(
                        reader,
                        readParameters,
                        symbolizer,
                        interpolation,
                        null,
                        tileSizeX,
                        tileSizeY);
        if (image == null) {
            // we're outside of the coverage definition area, return an empty space
            image = createBkgImage(mapWidth, mapHeight, bgColor, null);
        }
        return image;
    }

    private Interpolation getInterpolation() {
        final Interpolation interpolation;
        if (layerInterpolation != null) {
            interpolation = layerInterpolation;
        } else {
            LayerInfo.WMSInterpolation byLayerInterpolation = null;
            if (mapContent.getRequest().getLayers().size() > layerIndex) {
                LayerInfo layerInfo =
                        mapContent.getRequest().getLayers().get(layerIndex).getLayerInfo();
                byLayerInterpolation = getConfiguredLayerInterpolation(layerInfo);
            }

            WMSInfo.WMSInterpolation byServiceInterpolation = null;
            if (byLayerInterpolation == null && wms != null) {
                // if interpolation method is not configured for this layer, use service default
                byServiceInterpolation = wms.getInterpolation();
            }

            if (byLayerInterpolation != null) {
                interpolation = toInterpolationObject(byLayerInterpolation);
            } else if (byServiceInterpolation != null) {
                interpolation = toInterpolationObject(byServiceInterpolation);
            } else {
                // default to Nearest Neighbor
                interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            }
        }
        return interpolation;
    }

    private Color getBackgroundColor(boolean transparent) {
        Color bgColor = mapContent.getBgColor();
        // set transparency
        if (transparent) {
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
        } else {
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255);
        }
        return bgColor;
    }

    private static boolean isVectorSource(Expression tranformation) {
        // instanceof is sufficient for null check
        if (tranformation instanceof ProcessFunction) {
            ProcessFunction processFunction = (ProcessFunction) tranformation;
            Name processName = processFunction.getProcessName();
            Map<String, org.geotools.api.data.Parameter<?>> params =
                    Processors.getParameterInfo(processName);
            for (org.geotools.api.data.Parameter<?> param : params.values()) {
                if (SimpleFeatureCollection.class.isAssignableFrom(param.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private ReferencedEnvelope getEastNorthEnvelope(ReferencedEnvelope envelope)
            throws FactoryException {
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (CRS.getAxisOrder(crs) != CRS.AxisOrder.NORTH_EAST) {
            return envelope;
        }
        String code = ResourcePool.lookupIdentifier(crs, true);
        if (code == null) {
            return envelope;
        } else {
            CoordinateReferenceSystem eastNorthCrs =
                    CRS.decode(SrsSyntax.AUTH_CODE.getSRS(code), true);
            return new ReferencedEnvelope(
                    envelope.getMinY(),
                    envelope.getMaxY(),
                    envelope.getMinX(),
                    envelope.getMaxX(),
                    eastNorthCrs);
        }
    }

    /**
     * Reads the best matching grid out of a grid coverage applying sub-sampling and using overviews
     * as necessary
     */
    private static GridCoverage2D readBestCoverage(
            final ReadingContext context,
            final ReferencedEnvelope envelope,
            final Rectangle requestedRasterArea,
            final Interpolation interpolation,
            final Color bgColor,
            final int[] bandIndices)
            throws IOException {

        final GridCoverage2DReader reader = context.reader;
        final Object params = context.params;

        GeneralParameterValue[] readParams =
                getReadParameters(
                        params, envelope, requestedRasterArea, interpolation, bgColor, bandIndices);

        GridCoverage2D coverage = reader.read(readParams);
        context.params = readParams;
        return coverage;
    }

    private static GeneralParameterValue[] getReadParameters(
            final Object params,
            final ReferencedEnvelope envelope,
            final Rectangle requestedRasterArea,
            final Interpolation interpolation,
            final Color bgColor,
            int[] bandIndices) {
        Parameter<GridGeometry2D> readGG = null;
        if (envelope != null) {
            // //
            // It is an GridCoverage2DReader, let's use parameters
            // if we have any supplied by a user.
            // //
            // first I created the correct ReadGeometry
            readGG =
                    (Parameter<GridGeometry2D>)
                            AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            readGG.setValue(new GridGeometry2D(new GridEnvelope2D(requestedRasterArea), envelope));
        }

        final Parameter<Interpolation> readInterpolation =
                (Parameter<Interpolation>) ImageMosaicFormat.INTERPOLATION.createValue();
        readInterpolation.setValue(interpolation);

        final Parameter<Color> bgColorParam;
        if (bgColor != null) {
            bgColorParam = (Parameter<Color>) AbstractGridFormat.BACKGROUND_COLOR.createValue();
            bgColorParam.setValue(bgColor);
        } else {
            bgColorParam = null;
        }

        // Inject bandIndices read param
        Parameter<int[]> bandIndicesParam = null;
        if (bandIndices != null) {
            bandIndicesParam = (Parameter<int[]>) AbstractGridFormat.BANDS.createValue();
            bandIndicesParam.setValue(bandIndices);
        }

        // then I try to get read parameters associated with this
        // coverage if there are any.
        GeneralParameterValue[] readParams = (GeneralParameterValue[]) params;
        final int length = readParams == null ? 0 : readParams.length;
        if (length > 0) {
            // //
            //
            // Getting parameters to control how to read this coverage.
            // Remember to check to actually have them before forwarding
            // them to the reader.
            //
            // //

            // we have a valid number of parameters, let's check if
            // also have a READ_GRIDGEOMETRY2D. In such case we just
            // override it with the one we just build for this
            // request.
            final String readGGName = AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();
            final String readInterpolationName =
                    ImageMosaicFormat.INTERPOLATION.getName().toString();
            final String bgColorName = AbstractGridFormat.BACKGROUND_COLOR.getName().toString();
            final String bandsListName = AbstractGridFormat.BANDS.getName().toString();
            int i = 0;
            boolean foundInterpolation = false;
            boolean foundGG = false;
            boolean foundBgColor = false;
            boolean foundBandIndices = false;
            for (; i < length; i++) {
                final String paramName = readParams[i].getDescriptor().getName().toString();
                if (paramName.equalsIgnoreCase(readGGName) && readGG != null) {
                    ((Parameter) readParams[i]).setValue(readGG);
                    foundGG = true;
                } else if (paramName.equalsIgnoreCase(readInterpolationName)) {
                    ((Parameter) readParams[i]).setValue(interpolation);
                    foundInterpolation = true;
                } else if (paramName.equalsIgnoreCase(bgColorName) && bgColor != null) {
                    ((Parameter) readParams[i]).setValue(bgColor);
                    foundBgColor = true;
                } else if (paramName.equalsIgnoreCase(bandsListName) && bandIndices != null) {
                    ((Parameter) readParams[i]).setValue(bandIndices);
                    foundBandIndices = true;
                }
            }

            // did we find anything?
            if (!foundGG
                    || !foundInterpolation
                    || !(foundBgColor && bgColor != null)
                    || !foundBandIndices) {
                // add the correct read geometry to the supplied
                // params since we did not find anything
                List<GeneralParameterValue> paramList = new ArrayList<>();
                paramList.addAll(Arrays.asList(readParams));
                if (!foundGG && readGG != null) {
                    paramList.add(readGG);
                }
                if (!foundInterpolation) {
                    paramList.add(readInterpolation);
                }
                if (!foundBgColor && bgColor != null) {
                    paramList.add(bgColorParam);
                }
                if (!foundBandIndices && bandIndices != null) {
                    paramList.add(bandIndicesParam);
                }
                readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);
            }
        } else {
            List<GeneralParameterValue> paramList = new ArrayList<>();
            if (readGG != null) {
                paramList.add(readGG);
            }
            if (bgColor != null) {
                paramList.add(bgColorParam);
            }
            if (bandIndices != null) {
                paramList.add(bandIndicesParam);
            }
            paramList.add(readInterpolation);
            readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);
        }
        return readParams;
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

    private RasterSymbolizer updateSymbolizerForBandSelection(
            ReadingContext context, RasterSymbolizer symbolizer, int[] bandIndices) {
        GridCoverage2DReader reader = context != null ? context.reader : null;
        Object params = context != null ? context.params : null;

        if (params != null && reader != null && bandIndices != null) {
            Format format = reader.getFormat();
            ParameterValueGroup readParameters = null;
            ParameterDescriptorGroup descriptorGroup = null;
            List<GeneralParameterDescriptor> descriptors = null;
            if (format != null
                    && ((readParameters = format.getReadParameters()) != null)
                    && ((descriptorGroup = readParameters.getDescriptor()) != null)
                    && ((descriptors = descriptorGroup.descriptors()) != null)
                    && (descriptors.contains(AbstractGridFormat.BANDS))
                    && bandIndices != null) {
                // if bands are selected, alter the symbolizer to use bands in order 0,1,2,...
                // since the channel order defined by it previously is taken care of the reader
                symbolizer = GridCoverageRenderer.setupSymbolizerForBandsSelection(symbolizer);
            }
        }
        return symbolizer;
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

    private RenderedImage addAlphaChannel(RenderedImage image) {
        RenderedImage alpha = buildAlphaBand(image);

        // Adding Alpha band
        ImageWorker iw = new ImageWorker(image);
        iw.addBand(alpha, false, true, null);
        return iw.getRenderedImage();
    }

    /** Creates an alpha band, ready to be added to the specified image */
    private RenderedImage buildAlphaBand(RenderedImage image) {
        final ImageLayout tempLayout = new ImageLayout(image);
        tempLayout
                .unsetValid(ImageLayout.COLOR_MODEL_MASK)
                .unsetValid(ImageLayout.SAMPLE_MODEL_MASK);
        int width = image.getWidth();
        int height = image.getHeight();

        // in case of ROI, create an alpha band that is transparent where the ROI is zero,
        // and solid where the ROI is one. The most efficient way is to use a Lookup
        Object roiCandidate = image.getProperty("ROI");
        if (roiCandidate instanceof ROI) {
            PlanarImage roiImage = ((ROI) roiCandidate).getAsImage();
            ImageWorker iw = new ImageWorker(roiImage);
            byte[] lookup = new byte[256];
            Arrays.fill(lookup, (byte) 255);
            lookup[0] = 0;
            LookupTable lookupTable = LookupTableFactory.create(lookup);
            SampleModel sm =
                    RasterFactory.createPixelInterleavedSampleModel(
                            DataBuffer.TYPE_BYTE, width, height, 1);
            ColorModel cm = PlanarImage.createColorModel(sm);
            tempLayout.setSampleModel(sm);
            tempLayout.setColorModel(cm);
            iw.setRenderingHints(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tempLayout));
            iw.lookup(lookupTable);
            return iw.getRenderedImage();
        }

        // if there is no ROI instead, a constant image will do
        return ConstantDescriptor.create(
                Float.valueOf(width),
                Float.valueOf(height),
                new Byte[] {Byte.valueOf((byte) 255)},
                new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tempLayout));
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

    /** Returns true if the color is a level of gray */
    private static boolean isLevelOfGray(Color color) {
        return color.getRed() == color.getBlue() && color.getRed() == color.getGreen();
    }

    /**
     * Creates a bkg image using the supplied parameters.
     *
     * @param width the width of the timage to create
     * @param height the height of the image to create
     * @param bgColor the background color of the image to create
     * @param renderingHints the hints to apply
     * @return a {@link RenderedImage} with constant values as fill
     */
    private static final RenderedImage createBkgImage(
            float width, float height, Color bgColor, RenderingHints renderingHints) {
        // prepare bands for constant image if needed
        final Byte[] bands = {
            (byte) bgColor.getRed(),
            (byte) bgColor.getGreen(),
            (byte) bgColor.getBlue(),
            (byte) bgColor.getAlpha()
        };
        return ConstantDescriptor.create(width, height, bands, renderingHints);
    }

    private class GCRRenderingTransformationHelper extends RenderingTransformationHelper {

        private final Interpolation interpolation;
        private final boolean advancedProjectionHandling;
        private final boolean mapWrapping;
        private final WMSMapContent mapContent;

        public GCRRenderingTransformationHelper(
                WMSMapContent mapContent,
                Interpolation interpolation,
                boolean advancedProjectionHandling,
                boolean mapWrapping) {
            this.mapContent = mapContent;
            this.interpolation = interpolation;
            this.advancedProjectionHandling = advancedProjectionHandling;
            this.mapWrapping = mapWrapping;
        }

        @Override
        protected GridCoverage2D readCoverage(
                GridCoverage2DReader reader, Object readParams, GridGeometry2D readGG)
                throws IOException {
            RenderingHints interpolationHints =
                    new RenderingHints(JAI.KEY_INTERPOLATION, interpolation);
            final GridCoverageRenderer gcr;

            try {
                final int mapWidth = mapContent.getMapWidth();
                final int mapHeight = mapContent.getMapHeight();
                final ReferencedEnvelope mapEnvelope =
                        getEastNorthEnvelope(mapContent.getRenderingArea());
                final Rectangle mapRasterArea = new Rectangle(0, 0, mapWidth, mapHeight);
                final AffineTransform worldToScreen =
                        RendererUtilities.worldToScreenTransform(mapEnvelope, mapRasterArea);

                gcr =
                        new GridCoverageRenderer(
                                mapEnvelope.getCoordinateReferenceSystem(),
                                mapEnvelope,
                                mapRasterArea,
                                worldToScreen,
                                interpolationHints);
                gcr.setAdvancedProjectionHandlingEnabled(advancedProjectionHandling);
                gcr.setWrapEnabled(mapWrapping);
                GeneralParameterValue[] readingParams =
                        setInterpolation(interpolation, (GeneralParameterValue[]) readParams);

                RenderedImage ri =
                        gcr.renderImage(reader, readingParams, null, interpolation, null, 256, 256);
                if (ri != null) {
                    PlanarImage pi = PlanarImage.wrapRenderedImage(ri);
                    GridCoverage2D gc2d =
                            (GridCoverage2D)
                                    pi.getProperty(GridCoverageRenderer.PARENT_COVERAGE_PROPERTY);
                    return gc2d;
                }
                return null;
            } catch (TransformException | NoninvertibleTransformException | FactoryException e) {
                throw new IOException("Failure rendering the coverage", e);
            }
        }

        private GeneralParameterValue[] setInterpolation(
                Interpolation interpolation, GeneralParameterValue[] readParams) {
            if (interpolation != null) {
                List<GeneralParameterValue> paramList = new ArrayList<>();
                if (readParams != null) {
                    paramList =
                            Arrays.stream(readParams)
                                    .filter(param -> notInterpolation(param))
                                    .collect(Collectors.toList());
                }
                final Parameter<Interpolation> readInterpolation =
                        (Parameter<Interpolation>) ImageMosaicFormat.INTERPOLATION.createValue();
                readInterpolation.setValue(interpolation);
                paramList.add(readInterpolation);
                readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);
            }
            return readParams;
        }

        private boolean notInterpolation(GeneralParameterValue param) {
            return !param.getDescriptor().equals(ImageMosaicFormat.INTERPOLATION);
        }
    }
}
