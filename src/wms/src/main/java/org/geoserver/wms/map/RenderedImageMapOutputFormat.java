/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import it.geosolutions.jaiext.lookup.LookupTable;
import it.geosolutions.jaiext.lookup.LookupTableFactory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSPartialMapException;
import org.geoserver.wms.WMSServiceExceptionHandler;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geotools.api.style.Style;
import org.geotools.image.ImageWorker;
import org.geotools.map.Layer;
import org.geotools.map.StyleLayer;
import org.geotools.renderer.lite.LabelCache;
import org.geotools.renderer.lite.StreamingRenderer;

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

    private static final Interpolation NN_INTERPOLATION = new InterpolationNearest();

    private static final Interpolation BIL_INTERPOLATION = new InterpolationBilinear();

    private static final Interpolation BIC_INTERPOLATION = new InterpolationBicubic2(0);

    // antialiasing settings, no antialias, only text, full antialias
    private static final String AA_NONE = "NONE";

    private static final String AA_TEXT = "TEXT";

    private static final String AA_FULL = "FULL";

    private static final List<String> AA_SETTINGS = Arrays.asList(AA_NONE, AA_TEXT, AA_FULL);

    public static final String MAP_WRAPPING_FORMAT_OPTION = "mapWrapping";
    public static final String ADV_PROJECTION_HANDLING_FORMAT_OPTION = "advancedProjectionHandling";
    private static final String ADV_PROJECTION_DENSIFICATION_FORMAT_OPTION =
            "advancedProjectionHandlingDensification";
    private static final String DISABLE_DATELINE_WRAPPING_HEURISTIC_FORMAT_OPTION =
            "disableDatelineWrappingHeuristic";

    /**
     * Decorations Only option, which allows to get an empty request map output, but keeps visible
     * associated decorations
     */
    public static final String DECORATIONS_ONLY_FORMAT_OPTION = "decorationsOnly";

    /** The size of a megabyte */
    private static final int KB = 1024;

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

    /** Which format to encode the image in if one is not supplied */
    private static final String DEFAULT_MAP_FORMAT = "image/png";

    private boolean palleteSupported = true;

    private boolean transparencySupported = true;

    /** The file extension (minus the .) */
    private String extension = null;

    /** The known producer capabilities */
    private final Map<String, MapProducerCapabilities> capabilities = new HashMap<>();

    private final MarkFactoryHintsInjector markFactoryHintsInjector;

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
        this.markFactoryHintsInjector = new MarkFactoryHintsInjector(wms.getGeoServer());

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

    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return capabilities.get(format);
    }

    public void setLabelCache(Function<WMSMapContent, LabelCache> labelCache) {
        this.labelCache = labelCache;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    @Override
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

        // check if vendoroption decorationsonly is true, so we will generate an empty map with only
        // decorations applied
        String decorationsOnly =
                (String) request.getFormatOptions().get(DECORATIONS_ONLY_FORMAT_OPTION);
        boolean emptyMap = false;
        if (decorationsOnly != null && decorationsOnly.toLowerCase().equals("true")) {
            emptyMap = true;
        }

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
            for (Layer layer : layers) {
                pe.visit(layer.getStyle());
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
                            + "KB",
                    ServiceException.MAX_MEMORY_EXCEEDED);
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
            try {
                Interpolation interpolation = null;
                if (request.getInterpolations() != null && request.getInterpolations().size() > 0) {
                    interpolation = request.getInterpolations().get(0);
                }

                image =
                        new DirectRasterRenderer(
                                        wms, mapContent, 0, interpolation, transparencySupported)
                                .render();
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
        final Map<RenderingHints.Key, Object> hintsMap = new HashMap<>();

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
        // add the GeoServer MarkFactories provider settings
        markFactoryHintsInjector.addMarkFactoryHints(hints);
        renderer.setJava2DHints(hints);

        // setup the renderer hints
        Map<Object, Object> rendererParams = new HashMap<>();
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
            for (Layer value : layers) {
                if (value instanceof StyleLayer) {
                    StyleLayer layer = (StyleLayer) value;
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
        final RenderExceptionStrategy nonIgnorableExceptionListener =
                new RenderExceptionStrategy(renderer);
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
            if (!emptyMap) {
                renderer.paint(
                        graphic,
                        paintArea,
                        mapContent.getRenderingArea(),
                        mapContent.getRenderingTransform());
            } else {
                LOGGER.fine("we only want to get the layout, if it's not null");
            }

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
                                        + " rendering errors occurred, bailing out. Layers: "
                                        + buildMapLayerNameList(mapContent),
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
                                        + "s. Layers: "
                                        + buildMapLayerNameList(mapContent));
            }
            // check if a non ignorable error occurred
            if (nonIgnorableExceptionListener.exceptionOccurred()) {
                Exception renderError = nonIgnorableExceptionListener.getException();
                serviceException =
                        new ServiceException(
                                "Rendering process failed. Layers: "
                                        + buildMapLayerNameList(mapContent),
                                renderError,
                                "internalError");
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

    /** Helper method to build a comma separated list of layer names in the map. * */
    private String buildMapLayerNameList(WMSMapContent mapContent) {
        List<MapLayerInfo> layers = mapContent.getRequest().getLayers();
        return layers == null
                ? ""
                : layers.stream().map(MapLayerInfo::getName).collect(Collectors.joining(", "));
    }

    /**
     * Creates a {@link StreamingRenderer} instance (subclasses can provide their own specialized
     * subclasses of {@link StreamingRenderer}
     */
    protected StreamingRenderer buildRenderer() {
        return new StreamingRenderer();
    }

    public static boolean getFormatOptionAsBoolean(
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

    static LayerInfo.WMSInterpolation getConfiguredLayerInterpolation(LayerInfo layer) {

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

    static Interpolation toInterpolationObject(LayerInfo.WMSInterpolation interpolationMethod) {
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

    static Interpolation toInterpolationObject(WMSInfo.WMSInterpolation interpolationMethod) {
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
}
