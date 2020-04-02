/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import it.geosolutions.jaiext.lookup.LookupTable;
import it.geosolutions.jaiext.lookup.LookupTableFactory;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.vectorbin.ROIGeometry;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSPartialMapException;
import org.geoserver.wms.WMSServiceExceptionHandler;
import org.geoserver.wms.WatermarkInfo;
import org.geoserver.wms.decoration.MapDecoration;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geoserver.wms.decoration.MetatiledMapDecorationLayout;
import org.geoserver.wms.decoration.WatermarkDecoration;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ColorUtilities;
import org.geotools.map.Layer;
import org.geotools.map.StyleLayer;
import org.geotools.parameter.Parameter;
import org.geotools.process.Processors;
import org.geotools.process.function.ProcessFunction;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.LabelCache;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.RenderingTransformationHelper;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.lite.gridcoverage2d.ChannelSelectionUpdateStyleVisitor;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.Format;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;

/**
 * A {@link GetMapOutputFormat} that produces {@link RenderedImageMap} instances to be encoded in
 * the constructor supplied MIME-Type.
 *
 * <p>Instances of this class are expected to be declared in the application context supplying the
 * prescribed MIME-Type to create maps for, and the list of output format names to be declared in
 * the GetCapabilities document. Note that the prescribed MIME-Type (the MIME Type the produced
 * images are to be encoded as and that is to be set in the response HTTP Content-Type header) may
 * differ from what's declared in the capabilities document, hence the separation of concerns and
 * the two different arguments in the constructor (for example, a declared output format of {@code
 * image/geotiff8} may indicate to create an indexed geotiff image with 8-bit pixel depth, but the
 * resulting MIME-Type be {@code image/tiff}.
 *
 * <p>Whether or not the output format instance permits images with transparency and/or indexed
 * 8-bit color model is described by the {@link #isTransparencySupported() transparencySupported}
 * and {@link #isPaletteSupported() paletteSupported} properties respectively.
 *
 * @author Chris Holmes, TOPP
 * @author Simone Giannecchini, GeoSolutions
 * @author Andrea Aime
 * @author Gabriel Roldan
 * @version $Id$
 * @see RenderedImageMapOutputFormat
 * @see PNGMapResponse
 * @see GIFMapResponse
 * @see TIFFMapResponse
 * @see GeoTIFFMapResponse
 * @see JPEGMapResponse
 */
public class RenderedImageMapOutputFormat extends AbstractMapOutputFormat {

    /** An object keeping track of the reader and related params used to perform the rendering */
    static class ReadingContext {

        GridCoverage2DReader reader;
        Object params;
    }

    private static final Interpolation NN_INTERPOLATION = new InterpolationNearest();

    private static final Interpolation BIL_INTERPOLATION = new InterpolationBilinear();

    private static final Interpolation BIC_INTERPOLATION = new InterpolationBicubic2(0);

    // antialiasing settings, no antialias, only text, full antialias
    private static final String AA_NONE = "NONE";

    private static final String AA_TEXT = "TEXT";

    private static final String AA_FULL = "FULL";

    private static final List<String> AA_SETTINGS = Arrays.asList(AA_NONE, AA_TEXT, AA_FULL);

    private static final String MAP_WRAPPING_FORMAT_OPTION = "mapWrapping";
    private static final String ADV_PROJECTION_HANDLING_FORMAT_OPTION =
            "advancedProjectionHandling";
    private static final String ADV_PROJECTION_DENSIFICATION_FORMAT_OPTION =
            "advancedProjectionHandlingDensification";
    private static final String DISABLE_DATELINE_WRAPPING_HEURISTIC_FORMAT_OPTION =
            "disableDatelineWrappingHeuristic";

    /** Disable Gutter key */
    public static final String DISABLE_GUTTER_KEY = "wms.raster.disableGutter";

    /** Disable Gutter */
    private static Boolean DISABLE_GUTTER = Boolean.getBoolean(DISABLE_GUTTER_KEY);

    /** The size of a megabyte */
    private static final int KB = 1024;

    private static final int MAX_TILE_SIZE = 1024;

    /** The lookup table used for data type transformation (it's really the identity one) */
    private static LookupTableJAI IDENTITY_TABLE = new LookupTableJAI(getTable());

    private Function<WMSMapContent, LabelCache> labelCache = null;

    private static byte[] getTable() {
        byte[] arr = new byte[256];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) i;
        }
        return arr;
    }

    /** A logger for this class. */
    public static final Logger LOGGER = Logging.getLogger(RenderedImageMapOutputFormat.class);

    /** Which format to encode the image in if one is not supplied */
    private static final String DEFAULT_MAP_FORMAT = "image/png";

    /** WMS Service configuration * */
    protected final WMS wms;

    private boolean palleteSupported = true;

    private boolean transparencySupported = true;

    /** The file extension (minus the .) */
    private String extension = null;

    /** The known producer capabilities */
    private final Map<String, MapProducerCapabilities> capabilities =
            new HashMap<String, MapProducerCapabilities>();

    /** */
    public RenderedImageMapOutputFormat(WMS wms) {
        this(DEFAULT_MAP_FORMAT, wms);
    }

    /**
     * @param mime the mime type to be written down as an HTTP header when a map of this format is
     *     generated
     */
    public RenderedImageMapOutputFormat(String mime, WMS wms) {
        this(mime, new String[] {mime}, wms);
    }

    /**
     * @param mime the actual MIME Type resulting for the image created using this output format
     * @param outputFormats the list of output format names to declare in the GetCapabilities
     *     document, does not need to match {@code mime} (e.g., an output format of {@code
     *     image/geotiff8} may result in a map returned with MIME Type {@code image/tiff})
     */
    public RenderedImageMapOutputFormat(String mime, String[] outputFormats, WMS wms) {
        super(mime, outputFormats);
        this.wms = wms;

        // the capabilities of this produce are actually linked to the map response that is going to
        // be used, this class just generates a rendered image
        final Collection<RenderedImageMapResponse> responses = this.wms.getAvailableMapResponses();
        for (RenderedImageMapResponse response : responses) {
            for (String outFormat : outputFormats) {
                if (response.getOutputFormats().contains(outFormat)) {
                    MapProducerCapabilities cap = response.getCapabilities(outFormat);
                    if (cap != null) {
                        capabilities.put(outFormat, cap);
                    }
                }
            }
        }
    }

    /** Returns the extension used for the file name in the content disposition header */
    public String getExtension() {
        return extension;
    }

    /** Sets the extension used for the file name in the content disposition header */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return capabilities.get(format);
    }

    public void setLabelCache(Function<WMSMapContent, LabelCache> labelCache) {
        this.labelCache = labelCache;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    public final RenderedImageMap produceMap(WMSMapContent mapContent) throws ServiceException {
        return produceMap(mapContent, false);
    }

    /**
     * Actually produces the map image, caring about meta tiling if {@code tiled == true}.
     *
     * @param tiled Indicates whether metatiling is activated for this map producer.
     */
    public RenderedImageMap produceMap(final WMSMapContent mapContent, final boolean tiled)
            throws ServiceException {
        Rectangle paintArea =
                new Rectangle(0, 0, mapContent.getMapWidth(), mapContent.getMapHeight());

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("setting up " + paintArea.width + "x" + paintArea.height + " image");
        }

        // extra antialias setting
        final GetMapRequest request = mapContent.getRequest();
        String antialias = (String) request.getFormatOptions().get("antialias");
        if (antialias != null) antialias = antialias.toUpperCase();

        // figure out a palette for buffered image creation
        IndexColorModel potentialPalette = null;
        final boolean transparent = mapContent.isTransparent() && isTransparencySupported();
        final Color bgColor = mapContent.getBgColor();
        if (AA_NONE.equals(antialias)) {
            potentialPalette = mapContent.getPalette();
        } else if (AA_NONE.equals(antialias)) {
            PaletteExtractor pe = new PaletteExtractor(transparent ? null : bgColor);
            List<Layer> layers = mapContent.layers();
            for (int i = 0; i < layers.size(); i++) {
                pe.visit(layers.get(i).getStyle());
                if (!pe.canComputePalette()) break;
            }
            if (pe.canComputePalette()) potentialPalette = pe.getPalette();
        }
        final IndexColorModel palette = potentialPalette;

        // before even preparing the rendering surface, check it's not too big,
        // if so, throw a service exception
        long maxMemory = wms.getMaxRequestMemory() * KB;
        // ... base image memory
        long memory =
                getDrawingSurfaceMemoryUse(paintArea.width, paintArea.height, palette, transparent);
        // .. use a fake streaming renderer to evaluate the extra back buffers used when rendering
        // multiple featureTypeStyles against the same layer
        StreamingRenderer testRenderer = buildRenderer();
        testRenderer.setMapContent(mapContent);
        memory += testRenderer.getMaxBackBufferMemory(paintArea.width, paintArea.height);
        if (maxMemory > 0 && memory > maxMemory) {
            long kbUsed = memory / KB;
            long kbMax = maxMemory / KB;
            throw new ServiceException(
                    "Rendering request would use "
                            + kbUsed
                            + "KB, whilst the "
                            + "maximum memory allowed is "
                            + kbMax
                            + "KB");
        }

        final MapDecorationLayout layout = findDecorationLayout(request, tiled);

        // TODO: allow rendering to continue with vector layers
        // TODO: allow rendering to continue with layout
        // TODO: handle rotated rasters
        // TODO: handle color conversions
        // TODO: handle meta-tiling
        // TODO: how to handle timeout here? I guess we need to move it into the dispatcher?
        RenderedImage image = null;

        // fast path for pure coverage rendering
        if (DefaultWebMapService.isDirectRasterPathEnabled()
                && mapContent.layers().size() == 1
                && mapContent.getAngle() == 0.0
                && (layout == null || layout.isEmpty())) {
            List<GridCoverage2D> renderedCoverages = new ArrayList<GridCoverage2D>(2);
            try {
                Interpolation interpolation = null;
                if (request.getInterpolations() != null && request.getInterpolations().size() > 0) {
                    interpolation = request.getInterpolations().get(0);
                }

                image = directRasterRender(mapContent, 0, renderedCoverages, interpolation);

            } catch (Exception e) {
                throw new ServiceException("Error rendering coverage on the fast path", e);
            }

            if (image != null) {
                image = new RenderedImageTimeDecorator(image);
                // setting the layer triggers layerStartEvent
                ((RenderedImageTimeDecorator) image).setLayer(mapContent.layers().get(0));
                return buildMap(mapContent, image);
            }
        }

        // we use the alpha channel if the image is transparent or if the meta tiler
        // is enabled, since apparently the Crop operation inside the meta-tiler
        // generates striped images in that case (see GEOS-
        boolean useAlpha = transparent || MetatileMapOutputFormat.isRequestTiled(request, this);
        final RenderedImage preparedImage =
                prepareImage(paintArea.width, paintArea.height, palette, useAlpha);
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();

        final Graphics2D graphic = getGraphics(transparent, bgColor, preparedImage, hintsMap);

        // set up the antialias hints
        if (AA_NONE.equals(antialias)) {
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (preparedImage.getColorModel() instanceof IndexColorModel) {
                // otherwise we end up with dithered colors where the match is
                // not 100%
                hintsMap.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            }
        } else if (AA_TEXT.equals(antialias)) {
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            hintsMap.put(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            if (antialias != null && !AA_FULL.equals(antialias)) {
                LOGGER.warning(
                        "Unrecognized antialias setting '"
                                + antialias
                                + "', valid values are "
                                + AA_SETTINGS);
            }
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // these two hints improve text layout in diagonal labels and reduce artifacts
        // in line rendering (without hampering performance)
        hintsMap.put(
                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hintsMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // turn off/on interpolation rendering hint
        if (wms != null) {
            if (WMSInterpolation.Nearest.equals(wms.getInterpolation())) {
                hintsMap.put(JAI.KEY_INTERPOLATION, NN_INTERPOLATION);
                hintsMap.put(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            } else if (WMSInterpolation.Bilinear.equals(wms.getInterpolation())) {
                hintsMap.put(JAI.KEY_INTERPOLATION, BIL_INTERPOLATION);
                hintsMap.put(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            } else if (WMSInterpolation.Bicubic.equals(wms.getInterpolation())) {
                hintsMap.put(JAI.KEY_INTERPOLATION, BIC_INTERPOLATION);
                hintsMap.put(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
        }

        // make sure the hints are set before we start rendering the map
        graphic.setRenderingHints(hintsMap);

        RenderingHints hints = new RenderingHints(hintsMap);
        StreamingRenderer renderer = buildRenderer();
        renderer.setThreadPool(DefaultWebMapService.getRenderingPool());
        renderer.setMapContent(mapContent);
        renderer.setJava2DHints(hints);

        // setup the renderer hints
        Map<Object, Object> rendererParams = new HashMap<Object, Object>();
        rendererParams.put("optimizedDataLoadingEnabled", Boolean.TRUE);
        rendererParams.put("renderingBuffer", Integer.valueOf(mapContent.getBuffer()));
        rendererParams.put("maxFiltersToSendToDatastore", DefaultWebMapService.getMaxFilterRules());
        rendererParams.put(
                StreamingRenderer.SCALE_COMPUTATION_METHOD_KEY,
                mapContent.getRendererScaleMethod());
        if (AA_NONE.equals(antialias)) {
            rendererParams.put(
                    StreamingRenderer.TEXT_RENDERING_KEY, StreamingRenderer.TEXT_RENDERING_STRING);
        } else {
            // used to be TEXT_RENDERING_ADAPTIVE always, but since java 7 calling drawGlyphVector
            // just generates very ugly results
            rendererParams.put(
                    StreamingRenderer.TEXT_RENDERING_KEY, StreamingRenderer.TEXT_RENDERING_OUTLINE);
        }
        if (DefaultWebMapService.isLineWidthOptimizationEnabled()) {
            rendererParams.put(StreamingRenderer.LINE_WIDTH_OPTIMIZATION_KEY, true);
        }

        // turn on advanced projection handling
        if (wms.isAdvancedProjectionHandlingEnabled()) {
            rendererParams.put(StreamingRenderer.ADVANCED_PROJECTION_HANDLING_KEY, true);
            if (request.getFormatOptions().get(ADV_PROJECTION_DENSIFICATION_FORMAT_OPTION)
                    != null) {
                rendererParams.put(
                        StreamingRenderer.ADVANCED_PROJECTION_DENSIFICATION_KEY,
                        getFormatOptionAsBoolean(
                                request, ADV_PROJECTION_DENSIFICATION_FORMAT_OPTION));
            } else if (wms.isAdvancedProjectionDensificationEnabled()) {
                rendererParams.put(StreamingRenderer.ADVANCED_PROJECTION_DENSIFICATION_KEY, true);
            }
            if (wms.isContinuousMapWrappingEnabled()) {
                rendererParams.put(StreamingRenderer.CONTINUOUS_MAP_WRAPPING, true);
            }
            if (request.getFormatOptions().get(DISABLE_DATELINE_WRAPPING_HEURISTIC_FORMAT_OPTION)
                    != null) {
                rendererParams.put(
                        StreamingRenderer.DATELINE_WRAPPING_HEURISTIC_KEY,
                        !getFormatOptionAsBoolean(
                                request, DISABLE_DATELINE_WRAPPING_HEURISTIC_FORMAT_OPTION));
            } else if (wms.isDateLineWrappingHeuristicDisabled()) {
                rendererParams.put(StreamingRenderer.DATELINE_WRAPPING_HEURISTIC_KEY, false);
            }
        }

        if (getFormatOptionAsBoolean(request, ADV_PROJECTION_HANDLING_FORMAT_OPTION) == false) {
            rendererParams.put(StreamingRenderer.ADVANCED_PROJECTION_HANDLING_KEY, false);
            rendererParams.put(StreamingRenderer.CONTINUOUS_MAP_WRAPPING, false);
        }

        if (getFormatOptionAsBoolean(request, MAP_WRAPPING_FORMAT_OPTION) == false) {
            rendererParams.put(StreamingRenderer.CONTINUOUS_MAP_WRAPPING, false);
        }

        // see if the user specified a dpi
        if (request.getFormatOptions().get("dpi") != null) {
            rendererParams.put(StreamingRenderer.DPI_KEY, (request.getFormatOptions().get("dpi")));
        }

        if (labelCache != null) {
            try {
                rendererParams.put(StreamingRenderer.LABEL_CACHE_KEY, labelCache.apply(mapContent));
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }

        boolean kmplacemark = false;
        if (request.getFormatOptions().get("kmplacemark") != null)
            kmplacemark = ((Boolean) request.getFormatOptions().get("kmplacemark")).booleanValue();
        if (kmplacemark) {
            // create a StyleVisitor that copies a style, but removes the
            // PointSymbolizers and TextSymbolizers
            KMLStyleFilteringVisitor dupVisitor = new KMLStyleFilteringVisitor();

            // Remove PointSymbolizers and TextSymbolizers from the
            // layers' Styles to prevent their rendering on the
            // raster image. Both are better served with the
            // placemarks.
            List<Layer> layers = mapContent.layers();
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i) instanceof StyleLayer) {
                    StyleLayer layer = (StyleLayer) layers.get(i);
                    Style style = layer.getStyle();
                    style.accept(dupVisitor);
                    Style copy = (Style) dupVisitor.getCopy();
                    layer.setStyle(copy);
                }
            }
        }

        for (int i = 0; i < request.getLayers().size(); i++) {

            Interpolation interpolationToSet = null;
            // check interpolations vendor parameter first
            if (request.getInterpolations() != null && request.getInterpolations().size() > i) {
                interpolationToSet = request.getInterpolations().get(i);
            }
            // if vendor param not set, check by layer interpolation configuration
            if (interpolationToSet == null) {
                LayerInfo layerInfo = request.getLayers().get(i).getLayerInfo();

                LayerInfo.WMSInterpolation byLayerInterpolation =
                        getConfiguredLayerInterpolation(layerInfo);
                if (byLayerInterpolation != null) {
                    interpolationToSet = toInterpolationObject(byLayerInterpolation);
                }
            }

            if (interpolationToSet != null) {
                Layer layer = mapContent.layers().get(i);
                layer.getUserData()
                        .put(StreamingRenderer.BYLAYER_INTERPOLATION, interpolationToSet);
            }
        }

        renderer.setRendererHints(rendererParams);

        // if abort already requested bail out
        // if (this.abortRequested) {
        // graphic.dispose();
        // return null;
        // }

        // enforce no more than x rendering errors
        int maxErrors = wms.getMaxRenderingErrors();
        MaxErrorEnforcer errorChecker = new MaxErrorEnforcer(renderer, maxErrors);

        // Add a render listener that ignores well known rendering exceptions and reports back non
        // ignorable ones
        final RenderExceptionStrategy nonIgnorableExceptionListener;
        nonIgnorableExceptionListener = new RenderExceptionStrategy(renderer);
        renderer.addRenderListener(nonIgnorableExceptionListener);
        RenderTimeStatistics statistics = null;
        if (!request.getRequest().equalsIgnoreCase("GETFEATUREINFO")) {
            statistics = new RenderTimeStatistics();
            renderer.addRenderListener(statistics);
        }
        onBeforeRender(renderer);

        int maxRenderingTime = wms.getMaxRenderingTime(request);
        ServiceException serviceException = null;
        boolean saveMap =
                (request.getRawKvp() != null
                        && WMSServiceExceptionHandler.isPartialMapExceptionType(
                                request.getRawKvp().get("EXCEPTIONS")));
        RenderingTimeoutEnforcer timeout =
                new RenderingTimeoutEnforcer(maxRenderingTime, renderer, graphic, saveMap) {

                    /** Save the map before disposing of the graphics */
                    @Override
                    public void saveMap() {
                        this.map = optimizeAndBuildMap(palette, preparedImage, mapContent);
                    }
                };
        timeout.start();
        try {
            // finally render the image;
            renderer.paint(
                    graphic,
                    paintArea,
                    mapContent.getRenderingArea(),
                    mapContent.getRenderingTransform());

            // apply watermarking
            if (layout != null) {
                try {
                    layout.paint(graphic, paintArea, mapContent);
                } catch (Exception e) {
                    throw new ServiceException(
                            "Problem occurred while trying to watermark data", e);
                }
            }
            timeout.stop();

            // Determine what (if any) exception should be thrown

            // check if too many errors occurred
            if (errorChecker.exceedsMaxErrors()) {
                serviceException =
                        new ServiceException(
                                "More than "
                                        + maxErrors
                                        + " rendering errors occurred, bailing out.",
                                errorChecker.getLastException(),
                                "internalError");
            }
            // check if the request did timeout
            if (timeout.isTimedOut()) {
                serviceException =
                        new ServiceException(
                                "This request used more time than allowed and has been forcefully stopped. "
                                        + "Max rendering time is "
                                        + (maxRenderingTime / 1000.0)
                                        + "s");
            }
            // check if a non ignorable error occurred
            if (nonIgnorableExceptionListener.exceptionOccurred()) {
                Exception renderError = nonIgnorableExceptionListener.getException();
                serviceException =
                        new ServiceException(
                                "Rendering process failed", renderError, "internalError");
            }

            // If there were no exceptions, return the map
            if (serviceException == null) {
                return optimizeAndBuildMap(palette, preparedImage, mapContent);

                // If the exception format is PARTIALMAP, return whatever did get rendered with the
                // exception
            } else if (saveMap) {
                RenderedImageMap map = (RenderedImageMap) timeout.getMap();
                // We hit an error other than a timeout during rendering
                if (map == null) {
                    map = optimizeAndBuildMap(palette, preparedImage, mapContent);
                }
                // Wrap the serviceException in a WMSServiceException to hold the map
                serviceException = new WMSPartialMapException(serviceException, map);
            }
        } finally {
            timeout.stop();
            graphic.dispose();
            if (statistics != null) {
                statistics.renderingComplete();
            }
        }
        throw serviceException;
    }

    /**
     * Creates a {@link StreamingRenderer} instance (subclasses can provide their own specialized
     * subclasses of {@link StreamingRenderer}
     */
    protected StreamingRenderer buildRenderer() {
        return new StreamingRenderer();
    }

    private boolean getFormatOptionAsBoolean(
            final GetMapRequest request, final String formatOptionKey) {
        if (request.getFormatOptions().get(formatOptionKey) != null) {
            String formatOptionValue = (String) request.getFormatOptions().get(formatOptionKey);
            return (!"false".equalsIgnoreCase(formatOptionValue));
        }
        // else key not present
        return true;
    }

    private RenderedImageMap optimizeAndBuildMap(
            IndexColorModel palette, RenderedImage preparedImage, WMSMapContent mapContent) {
        RenderedImage image;
        if (palette != null && palette.getMapSize() < 256) {
            image = optimizeSampleModel(preparedImage);
        } else {
            image = preparedImage;
        }
        return buildMap(mapContent, image);
    }

    protected Graphics2D getGraphics(
            final boolean transparent,
            final Color bgColor,
            final RenderedImage preparedImage,
            final Map<RenderingHints.Key, Object> hintsMap) {
        return ImageUtils.prepareTransparency(transparent, bgColor, preparedImage, hintsMap);
    }

    /** Allows subclasses to customize the renderer before the paint method gets invoked */
    protected void onBeforeRender(StreamingRenderer renderer) {
        // TODO Auto-generated method stub
    }

    protected RenderedImageMap buildMap(final WMSMapContent mapContent, RenderedImage image) {
        RenderedImageMap map = new RenderedImageMap(mapContent, image, getMimeType());
        if (extension != null) {
            map.setContentDispositionHeader(mapContent, "." + extension, false);
        }
        return map;
    }

    protected MapDecorationLayout findDecorationLayout(GetMapRequest request, final boolean tiled) {
        String layoutName = null;
        if (request.getFormatOptions() != null) {
            layoutName = (String) request.getFormatOptions().get("layout");
        }

        MapDecorationLayout layout = null;
        if (layoutName != null && !layoutName.trim().isEmpty()) {
            try {
                GeoServerResourceLoader loader = wms.getCatalog().getResourceLoader();
                Resource layouts = loader.get("layouts");
                if (layouts.getType() == Type.DIRECTORY) {
                    Resource layoutConfig = layouts.get(layoutName + ".xml");

                    if (layoutConfig.getType() == Type.RESOURCE) {
                        layout = MapDecorationLayout.fromFile(layoutConfig, tiled);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown layout requested: " + layoutName);
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No layouts directory defined");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to load layout: " + layoutName, e);
            }

            if (layout == null) {
                throw new ServiceException("Could not find decoration layout named: " + layoutName);
            }
        }

        if (layout == null) {
            layout = tiled ? new MetatiledMapDecorationLayout() : new MapDecorationLayout();
        }

        MapDecorationLayout.Block watermark = getWatermark(wms.getServiceInfo());
        if (watermark != null) {
            layout.addBlock(watermark);
        }

        return layout;
    }

    public static MapDecorationLayout.Block getWatermark(WMSInfo wms) {
        WatermarkInfo watermark = (wms == null ? null : wms.getWatermark());
        if (watermark != null && watermark.isEnabled()) {
            Map<String, String> options = new HashMap<String, String>();
            options.put("url", watermark.getURL());
            options.put("opacity", Float.toString((255f - watermark.getTransparency()) / 2.55f));

            MapDecoration d = new WatermarkDecoration();
            try {
                d.loadOptions(options);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Couldn't construct watermark from configuration", e);
                throw new ServiceException(e);
            }

            MapDecorationLayout.Block.Position p = null;

            switch (watermark.getPosition()) {
                case TOP_LEFT:
                    p = MapDecorationLayout.Block.Position.UL;
                    break;
                case TOP_CENTER:
                    p = MapDecorationLayout.Block.Position.UC;
                    break;
                case TOP_RIGHT:
                    p = MapDecorationLayout.Block.Position.UR;
                    break;
                case MID_LEFT:
                    p = MapDecorationLayout.Block.Position.CL;
                    break;
                case MID_CENTER:
                    p = MapDecorationLayout.Block.Position.CC;
                    break;
                case MID_RIGHT:
                    p = MapDecorationLayout.Block.Position.CR;
                    break;
                case BOT_LEFT:
                    p = MapDecorationLayout.Block.Position.LL;
                    break;
                case BOT_CENTER:
                    p = MapDecorationLayout.Block.Position.LC;
                    break;
                case BOT_RIGHT:
                    p = MapDecorationLayout.Block.Position.LR;
                    break;
                default:
                    throw new ServiceException(
                            "Unknown WatermarkInfo.Position value.  Something is seriously wrong.");
            }

            return new MapDecorationLayout.Block(d, p, null, new Point(0, 0));
        }

        return null;
    }

    /**
     * Sets up a {@link BufferedImage#TYPE_4BYTE_ABGR} if the paletteInverter is not provided, or a
     * indexed image otherwise. Subclasses may override this method should they need a special kind
     * of image
     */
    protected RenderedImage prepareImage(
            int width, int height, IndexColorModel palette, boolean transparent) {
        return ImageUtils.createImage(
                width,
                height,
                isPaletteSupported() ? palette : null,
                transparent && isTransparencySupported());
    }

    /**
     * Returns true if the format supports image transparency, false otherwise (defaults to {@code
     * true})
     *
     * @return true if the format supports image transparency, false otherwise
     */
    public boolean isTransparencySupported() {
        return transparencySupported;
    }

    public void setTransparencySupported(boolean supportsTransparency) {
        this.transparencySupported = supportsTransparency;
    }

    /**
     * Returns true if the format supports palette encoding, false otherwise (defaults to {@code
     * true}).
     *
     * @return true if the format supports palette encoding, false otherwise
     */
    public boolean isPaletteSupported() {
        return palleteSupported;
    }

    public void setPaletteSupported(boolean supportsPalette) {
        this.palleteSupported = supportsPalette;
    }

    /**
     * When you override {@link #prepareImage(int, int, IndexColorModel, boolean)} remember to
     * override this one as well
     */
    protected long getDrawingSurfaceMemoryUse(
            int width, int height, IndexColorModel palette, boolean transparent) {
        return ImageUtils.getDrawingSurfaceMemoryUse(
                width,
                height,
                isPaletteSupported() ? palette : null,
                transparent && isTransparencySupported());
    }

    /**
     * This takes an image with an indexed color model that uses less than 256 colors and has a 8bit
     * sample model, and transforms it to one that has the optimal sample model (for example, 1bit
     * if the palette only has 2 colors)
     */
    private static RenderedImage optimizeSampleModel(RenderedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        ImageLayout layout = new ImageLayout();
        layout.setColorModel(source.getColorModel());
        layout.setSampleModel(source.getColorModel().createCompatibleSampleModel(w, h));
        // if I don't force tiling off with this setting an exception is thrown
        // when writing the image out...
        layout.setTileWidth(w);
        layout.setTileHeight(h);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        LookupTable table = LookupTableFactory.create(IDENTITY_TABLE);
        // TODO SIMONE why not format?
        ImageWorker worker = new ImageWorker(source);
        worker.setRenderingHints(hints);
        worker.lookup(table);
        return worker.getRenderedImage();
    }

    /**
     * Renders a single coverage as the final RenderedImage to be encoded, skipping all of the
     * Java2D machinery and using a pure JAI chain of transformations instead. This considerably
     * improves both scalability and performance
     *
     * @param mapContent The map definition (used for map size and transparency/color management)
     * @param layerIndex the layer that is supposed to contain a coverage
     * @param renderedCoverages placeholder where to deposit rendered coverages, if any, so that
     *     they can be disposed later
     * @return the result of rendering the coverage, or null if there was no coverage, or the
     *     coverage could not be renderer for some reason
     */
    private RenderedImage directRasterRender(
            WMSMapContent mapContent,
            int layerIndex,
            List<GridCoverage2D> renderedCoverages,
            Interpolation layerInterpolation)
            throws IOException, FactoryException {

        //
        // extract the raster symbolizers and the eventual rendering transformation
        //
        double scaleDenominator = mapContent.getScaleDenominator(true);
        Layer layer = mapContent.layers().get(layerIndex);
        FeatureType featureType = layer.getFeatureSource().getSchema();
        Style style = layer.getStyle();

        RasterSymbolizerVisitor visitor =
                new RasterSymbolizerVisitor(scaleDenominator, featureType);
        style.accept(visitor);

        List<RasterSymbolizer> symbolizers = visitor.getRasterSymbolizers();
        if (symbolizers.size() != 1) {
            return null;
        }
        RasterSymbolizer symbolizer = symbolizers.get(0);
        Expression transformation = visitor.getRasterRenderingTransformation();

        // direct raster rendering uses Query.ALL for the style query which is
        // inefficient for vector sources
        if (isVectorSource(transformation)) {
            return null;
        }
        //
        // Dimensions
        //
        final int mapWidth = mapContent.getMapWidth();
        final int mapHeight = mapContent.getMapHeight();
        // force east/north, otherwise the reading code might think we are reprojecting
        // and start adding padding around the requests
        final ReferencedEnvelope mapEnvelope = getEastNorthEnvelope(mapContent.getRenderingArea());
        final CoordinateReferenceSystem mapCRS = mapEnvelope.getCoordinateReferenceSystem();
        final Rectangle mapRasterArea = new Rectangle(0, 0, mapWidth, mapHeight);
        final AffineTransform worldToScreen =
                RendererUtilities.worldToScreenTransform(mapEnvelope, mapRasterArea);

        //
        // Check transparency and bg color
        //
        final boolean transparent = mapContent.isTransparent() && isTransparencySupported();
        Color bgColor = mapContent.getBgColor();
        // set transparency
        if (transparent) {
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
        } else {
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255);
        }

        //
        // Grab the interpolation
        //
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

        //
        // Tiling
        //
        // if there is a output tile size hint, use it, otherwise use the output size itself
        int tileSizeX = -1;
        int tileSizeY = -1;
        if (mapContent.getTileSize() != -1) {
            tileSizeX = tileSizeY = mapContent.getTileSize();
        } else if (mapWidth < MAX_TILE_SIZE && mapHeight < MAX_TILE_SIZE) {
            tileSizeX = mapWidth;
            tileSizeY = mapHeight;
        }

        //
        // Band selection
        //
        final int[] bandIndices =
                transformation == null
                        ? ChannelSelectionUpdateStyleVisitor.getBandIndicesFromSelectionChannels(
                                symbolizer)
                        : null;

        // actual read
        final ReadingContext context = new ReadingContext();
        RenderedImage image = null;
        GridCoverage2D coverage = null;
        RenderingHints interpolationHints =
                new RenderingHints(JAI.KEY_INTERPOLATION, interpolation);
        try {
            final Color readerBgColor = transparent ? null : bgColor;
            if (transformation == null && wms.isAdvancedProjectionHandlingEnabled()) {
                //
                // Get the reader
                //
                final Feature feature =
                        mapContent
                                .layers()
                                .get(0)
                                .getFeatureSource()
                                .getFeatures()
                                .features()
                                .next();
                final GridCoverage2DReader reader =
                        (GridCoverage2DReader) feature.getProperty("grid").getValue();
                // render via grid coverage renderer, that will apply the advanced projection
                // handling
                final Object params = feature.getProperty("params").getValue();
                GeneralParameterValue[] readParameters =
                        getReadParameters(
                                params, null, null, interpolation, readerBgColor, bandIndices);
                final GridCoverageRenderer gcr =
                        new GridCoverageRenderer(
                                mapEnvelope.getCoordinateReferenceSystem(),
                                mapEnvelope,
                                mapRasterArea,
                                worldToScreen,
                                interpolationHints);
                gcr.setAdvancedProjectionHandlingEnabled(true);
                gcr.setWrapEnabled(wms.isContinuousMapWrappingEnabled());
                // use null background here, background color is handled afterwards
                image =
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
            } else {
                //
                // Prepare the reading parameters (for the RT case)
                //
                final CoordinateReferenceSystem coverageCRS =
                        layer.getFeatureSource().getSchema().getCoordinateReferenceSystem();
                final GridGeometry2D readGG;
                boolean useGutter = !DISABLE_GUTTER;
                if (useGutter) {
                    final boolean equalsMetadata = CRS.equalsIgnoreMetadata(mapCRS, coverageCRS);
                    boolean sameCRS;
                    try {
                        sameCRS =
                                equalsMetadata
                                        || CRS.findMathTransform(mapCRS, coverageCRS, true)
                                                .isIdentity();
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

                if (transformation != null) {
                    RenderingTransformationHelper helper =
                            new GCRRenderingTransformationHelper(
                                    mapContent,
                                    interpolation,
                                    wms.isAdvancedProjectionHandlingEnabled(),
                                    wms.isContinuousMapWrappingEnabled());
                    Object result =
                            helper.applyRenderingTransformation(
                                    transformation,
                                    layer.getFeatureSource(),
                                    layer.getQuery(),
                                    Query.ALL,
                                    readGG,
                                    coverageCRS,
                                    interpolationHints);
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
                    //
                    // Get the reader
                    //
                    final Feature feature =
                            mapContent
                                    .layers()
                                    .get(0)
                                    .getFeatureSource()
                                    .getFeatures()
                                    .features()
                                    .next();
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
        // If
        // we need to enlarge the image we go with Mosaic if we need to crop we use Crop. Notice
        // that
        // if we need to mess up with the background color we need to go by Mosaic and we cannot use
        // Crop
        // since it does not support changing the bkg color.
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
        final ImageWorker worker = new ImageWorker(image);
        final int transparencyType = cm.getTransparency();

        // in case of index color model we try to preserve it, so that output
        // formats that can work with it can enjoy its extra compactness
        if (cm instanceof IndexColorModel) {
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
                ccm = (ComponentColorModel) cm;
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
        return image;
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

    private static boolean isVectorSource(Expression tranformation) {
        // instanceof is sufficient for null check
        if (tranformation instanceof ProcessFunction) {
            ProcessFunction processFunction = (ProcessFunction) tranformation;
            Name processName = processFunction.getProcessName();
            Map<String, org.geotools.data.Parameter<?>> params =
                    Processors.getParameterInfo(processName);
            for (org.geotools.data.Parameter<?> param : params.values()) {
                if (SimpleFeatureCollection.class.isAssignableFrom(param.getType())) {
                    return true;
                }
            }
        }
        return false;
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

    private ReferencedEnvelope getEastNorthEnvelope(ReferencedEnvelope envelope)
            throws FactoryException {
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (CRS.getAxisOrder(crs) != AxisOrder.NORTH_EAST) {
            return envelope;
        }
        Integer epsg = CRS.lookupEpsgCode(crs, false);
        if (epsg == null) {
            return envelope;
        } else {
            CoordinateReferenceSystem eastNorthCrs = CRS.decode("EPSG:" + epsg, true);
            return new ReferencedEnvelope(
                    envelope.getMinY(),
                    envelope.getMaxY(),
                    envelope.getMinX(),
                    envelope.getMaxX(),
                    eastNorthCrs);
        }
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
        final Number[] bands =
                new Byte[] {
                    (byte) bgColor.getRed(),
                    (byte) bgColor.getGreen(),
                    (byte) bgColor.getBlue(),
                    (byte) bgColor.getAlpha()
                };
        return ConstantDescriptor.create(width, height, bands, renderingHints);
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

        GridCoverage2D coverage;
        GeneralParameterValue[] readParams =
                getReadParameters(
                        params, envelope, requestedRasterArea, interpolation, bgColor, bandIndices);

        coverage = reader.read(readParams);
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
                List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
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

    private static LayerInfo.WMSInterpolation getConfiguredLayerInterpolation(LayerInfo layer) {

        LayerInfo.WMSInterpolation configuredInterpolation = null;

        if (layer != null && layer.getDefaultWMSInterpolationMethod() != null) {
            try {
                configuredInterpolation = layer.getDefaultWMSInterpolationMethod();
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return configuredInterpolation;
    }

    private static Interpolation toInterpolationObject(
            LayerInfo.WMSInterpolation interpolationMethod) {
        if (interpolationMethod == null) {
            return null;
        }

        switch (interpolationMethod) {
            case Bilinear:
                return Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            case Bicubic:
                return Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            case Nearest:
            default:
                return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        }
    }

    private static Interpolation toInterpolationObject(
            WMSInfo.WMSInterpolation interpolationMethod) {
        if (interpolationMethod == null) {
            return null;
        }

        switch (interpolationMethod) {
            case Bilinear:
                return Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            case Bicubic:
                return Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            case Nearest:
            default:
                return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        }
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
                RenderedImage ri =
                        gcr.renderImage(
                                reader,
                                (GeneralParameterValue[]) readParams,
                                null,
                                interpolation,
                                null,
                                256,
                                256);
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
    }
}
