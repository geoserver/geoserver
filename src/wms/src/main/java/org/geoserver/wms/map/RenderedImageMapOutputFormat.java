/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.media.jai.operator.BandMergeDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.LookupDescriptor;
import javax.media.jai.operator.MosaicDescriptor;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WatermarkInfo;
import org.geoserver.wms.decoration.MapDecoration;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geoserver.wms.decoration.MetatiledMapDecorationLayout;
import org.geoserver.wms.decoration.WatermarkDecoration;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.map.Layer;
import org.geotools.map.StyleLayer;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.resources.image.ColorUtilities;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.geometry.BoundingBox;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.vfny.geoserver.global.GeoserverDataDirectory;


/**
 * A {@link GetMapOutputFormat} that produces {@link RenderedImageMap} instances to be encoded in
 * the constructor supplied MIME-Type.
 * <p>
 * Instances of this class are expected to be declared in the application context supplying the
 * prescribed MIME-Type to create maps for, and the list of output format names to be declared in
 * the GetCapabilities document. Note that the prescribed MIME-Type (the MIME Type the produced
 * images are to be encoded as and that is to be set in the response HTTP Content-Type header) may
 * differ from what's declared in the capabilities document, hence the separation of concerns and
 * the two different arguments in the constructor (for example, a declared output format of
 * {@code image/geotiff8} may indicate to create an indexed geotiff image with 8-bit pixel depth,
 * but the resulting MIME-Type be {@code image/tiff}.
 * </p>
 * <p>
 * Whether or not the output format instance permits images with transparency and/or indexed 8-bit
 * color model is described by the {@link #isTransparencySupported() transparencySupported} and
 * {@link #isPaletteSupported() paletteSupported} properties respectively.
 * </p>
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
    
    private final static Interpolation NN_INTERPOLATION = new InterpolationNearest();

    private final static Interpolation BIL_INTERPOLATION = new InterpolationBilinear();

    private final static Interpolation BIC_INTERPOLATION = new InterpolationBicubic2(0);

    // antialiasing settings, no antialias, only text, full antialias
    private final static String AA_NONE = "NONE";

    private final static String AA_TEXT = "TEXT";

    private final static String AA_FULL = "FULL";

    private final static List<String> AA_SETTINGS = Arrays.asList(new String[] { AA_NONE, AA_TEXT,
            AA_FULL });

    /**
     * The size of a megabyte
     */
    private static final int KB = 1024;

    /**
     * The lookup table used for data type transformation (it's really the identity one)
     */
    private static LookupTableJAI IDENTITY_TABLE = new LookupTableJAI(getTable());

    private static byte[] getTable() {
        byte[] arr = new byte[256];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) i;
        }
        return arr;
    }

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(RenderedImageMapOutputFormat.class);

    /** Which format to encode the image in if one is not supplied */
    private static final String DEFAULT_MAP_FORMAT = "image/png";

    /** WMS Service configuration * */
    protected final WMS wms;

    private boolean palleteSupported = true;

    private boolean transparencySupported = true;
    
    /**
     * The file extension (minus the .)
     */
    private String extension = null;
    
    /**
     * The known producer capabilities
     */
    private final Map<String, MapProducerCapabilities> capabilities= new HashMap<String, MapProducerCapabilities>();

    /**
     * 
     */
    public RenderedImageMapOutputFormat(WMS wms) {
        this(DEFAULT_MAP_FORMAT, wms);
    }

    /**
     * @param the
     *            mime type to be written down as an HTTP header when a map of this format is
     *            generated
     */
    public RenderedImageMapOutputFormat(String mime, WMS wms) {
        this(mime, new String[] {mime}, wms);
    }

    /**
     * 
     * @param mime
     *            the actual MIME Type resulting for the image created using this output format
     * @param outputFormats
     *            the list of output format names to declare in the GetCapabilities document, does
     *            not need to match {@code mime} (e.g., an output format of {@code image/geotiff8}
     *            may result in a map returned with MIME Type {@code image/tiff})
     * @param wms
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

    /**
     * Returns the extension used for the file name in the content disposition header
     * @param extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Sets the extension used for the file name in the content disposition header
     * @param extension
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return capabilities.get(format);
    }

    /**
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    public final RenderedImageMap produceMap(WMSMapContent mapContent) throws ServiceException {
        return produceMap(mapContent, false);
    }
    
    /**
     * Actually produces the map image, careing about meta tiling if {@code tiled == true}.
     * 
     * @param mapContent
     * @param tiled
     *            Indicates whether metatiling is activated for this map producer.
     */
    public RenderedImageMap produceMap(final WMSMapContent mapContent, final boolean tiled)
            throws ServiceException {
        Rectangle paintArea = new Rectangle(0, 0, mapContent.getMapWidth(),
                mapContent.getMapHeight());

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("setting up " + paintArea.width + "x" + paintArea.height + " image");
        }

        // extra antialias setting
        final GetMapRequest request = mapContent.getRequest();
        String antialias = (String) request.getFormatOptions().get("antialias");
        if (antialias != null)
            antialias = antialias.toUpperCase();

        // figure out a palette for buffered image creation
        IndexColorModel palette = null;
        final boolean transparent = mapContent.isTransparent() && isTransparencySupported();
        final Color bgColor = mapContent.getBgColor();
        if (AA_NONE.equals(antialias)) {
            palette = mapContent.getPalette();
        } else if (AA_NONE.equals(antialias)) {
            PaletteExtractor pe = new PaletteExtractor(transparent ? null : bgColor);
            List<Layer> layers = mapContent.layers();
            for (int i = 0; i < layers.size(); i++) {
                pe.visit(layers.get(i).getStyle());
                if (!pe.canComputePalette())
                    break;
            }
            if (pe.canComputePalette())
                palette = pe.getPalette();
        }

        // before even preparing the rendering surface, check it's not too big,
        // if so, throw a service exception
        long maxMemory = wms.getMaxRequestMemory() * KB;
        // ... base image memory
        long memory = getDrawingSurfaceMemoryUse(paintArea.width, paintArea.height, palette,
                transparent);
        // .. use a fake streaming renderer to evaluate the extra back buffers used when rendering
        // multiple featureTypeStyles against the same layer
        StreamingRenderer testRenderer = new StreamingRenderer();
        testRenderer.setMapContent(mapContent);
        memory += testRenderer.getMaxBackBufferMemory(paintArea.width, paintArea.height);
        if (maxMemory > 0 && memory > maxMemory) {
            long kbUsed = memory / KB;
            long kbMax = maxMemory / KB;
            throw new ServiceException("Rendering request would use " + kbUsed + "KB, whilst the "
                    + "maximum memory allowed is " + kbMax + "KB");
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
        if (DefaultWebMapService.isDirectRasterPathEnabled() && 
                mapContent.layers().size() == 1 
                && mapContent.getAngle() == 0.0
                && (layout == null || layout.isEmpty())) {
            List<GridCoverage2D> renderedCoverages = new ArrayList<GridCoverage2D>(2);
            try {
                image = directRasterRender(mapContent, 0, renderedCoverages);
            } catch (Exception e) {
                throw new ServiceException("Error rendering coverage on the fast path", e);
            }

            if (image != null) {
                return buildMap(mapContent, image);
            }
        }

        // we use the alpha channel if the image is transparent or if the meta tiler
        // is enabled, since apparently the Crop operation inside the meta-tiler
        // generates striped images in that case (see GEOS-
        boolean useAlpha = transparent || MetatileMapOutputFormat.isRequestTiled(request, this);
        final RenderedImage preparedImage = prepareImage(paintArea.width, paintArea.height,
                palette, useAlpha);
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
            hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            if (antialias != null && !AA_FULL.equals(antialias)) {
                LOGGER.warning("Unrecognized antialias setting '" + antialias
                        + "', valid values are " + AA_SETTINGS);
            }
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        // these two hints improve text layout in diagonal labels and reduce artifacts
        // in line rendering (without hampering performance)
        hintsMap.put(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hintsMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // turn off/on interpolation rendering hint
        if (wms != null) {
            if (WMSInterpolation.Nearest.equals(wms.getInterpolation())) {
                hintsMap.put(JAI.KEY_INTERPOLATION, NN_INTERPOLATION);
                hintsMap.put(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            } else if (WMSInterpolation.Bilinear.equals(wms.getInterpolation())) {
                hintsMap.put(JAI.KEY_INTERPOLATION, BIL_INTERPOLATION);
                hintsMap.put(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            } else if (WMSInterpolation.Bicubic.equals(wms.getInterpolation())) {
                hintsMap.put(JAI.KEY_INTERPOLATION, BIC_INTERPOLATION);
                hintsMap.put(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
        }

        // make sure the hints are set before we start rendering the map
        graphic.setRenderingHints(hintsMap);

        RenderingHints hints = new RenderingHints(hintsMap);
        StreamingRenderer renderer = new StreamingRenderer();
        renderer .setThreadPool(DefaultWebMapService.getRenderingPool());
        renderer.setMapContent(mapContent);
        renderer.setJava2DHints(hints);

        // setup the renderer hints
        Map<Object, Object> rendererParams = new HashMap<Object, Object>();
        rendererParams.put("optimizedDataLoadingEnabled", new Boolean(true));
        rendererParams.put("renderingBuffer", new Integer(mapContent.getBuffer()));
        rendererParams.put("maxFiltersToSendToDatastore", DefaultWebMapService.getMaxFilterRules());
        rendererParams.put(StreamingRenderer.SCALE_COMPUTATION_METHOD_KEY,
                StreamingRenderer.SCALE_OGC);
        if (AA_NONE.equals(antialias)) {
            rendererParams.put(StreamingRenderer.TEXT_RENDERING_KEY,
                    StreamingRenderer.TEXT_RENDERING_STRING);
        } else {
            // used to be TEXT_RENDERING_ADAPTIVE always, but since java 7 calling drawGlyphVector
            // just generates very ugly results
            rendererParams.put(StreamingRenderer.TEXT_RENDERING_KEY,
                    StreamingRenderer.TEXT_RENDERING_OUTLINE);
        }
        if (DefaultWebMapService.isLineWidthOptimizationEnabled()) {
            rendererParams.put(StreamingRenderer.LINE_WIDTH_OPTIMIZATION_KEY, true);
        }
        
        // turn on advanced projection handling
        rendererParams.put(StreamingRenderer.ADVANCED_PROJECTION_HANDLING_KEY, true);
        if(DefaultWebMapService.isContinuousMapWrappingEnabled()) {
            rendererParams.put(StreamingRenderer.CONTINUOUS_MAP_WRAPPING, true);
        }
        
        // see if the user specified a dpi
        if (request.getFormatOptions().get("dpi") != null) {
            rendererParams.put(StreamingRenderer.DPI_KEY, ((Integer) request
                    .getFormatOptions().get("dpi")));
        }

        boolean kmplacemark = false;
        if (request.getFormatOptions().get("kmplacemark") != null)
            kmplacemark = ((Boolean) request.getFormatOptions().get("kmplacemark"))
                    .booleanValue();
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
        
        onBeforeRender(renderer);

        // setup the timeout enforcer (the enforcer is neutral when the timeout is 0)
        int maxRenderingTime = wms.getMaxRenderingTime() * 1000;
        RenderingTimeoutEnforcer timeout = new RenderingTimeoutEnforcer(maxRenderingTime, renderer,
                graphic);
        timeout.start();
        try {
            // finally render the image;
            renderer.paint(graphic, paintArea, mapContent.getRenderingArea(),
                    mapContent.getRenderingTransform());

            // apply watermarking
            if (layout != null) {
                try {
                    layout.paint(graphic, paintArea, mapContent);
                } catch (Exception e) {
                    throw new ServiceException("Problem occurred while trying to watermark data", e);
                }
            }
        } finally {
            timeout.stop();
            graphic.dispose();
        }

        // check if the request did timeout
        if (timeout.isTimedOut()) {
            throw new ServiceException(
                    "This requested used more time than allowed and has been forcefully stopped. "
                            + "Max rendering time is " + (maxRenderingTime / 1000.0) + "s");
        }

        // check if a non ignorable error occurred
        if (nonIgnorableExceptionListener.exceptionOccurred()) {
            Exception renderError = nonIgnorableExceptionListener.getException();
            throw new ServiceException("Rendering process failed", renderError, "internalError");
        }

        // check if too many errors occurred
        if (errorChecker.exceedsMaxErrors()) {
            throw new ServiceException("More than " + maxErrors
                    + " rendering errors occurred, bailing out.", errorChecker.getLastException(),
                    "internalError");
        }

        if (palette != null && palette.getMapSize() < 256) {
            image = optimizeSampleModel(preparedImage);
        } else {
            image = preparedImage;
        }

        RenderedImageMap map = buildMap(mapContent, image);
        return map;
    }

    protected Graphics2D getGraphics(final boolean transparent, final Color bgColor,
            final RenderedImage preparedImage, final Map<RenderingHints.Key, Object> hintsMap) {
        return ImageUtils.prepareTransparency(transparent, bgColor,
                preparedImage, hintsMap);
    }

    /**
     * Allows subclasses to customize the renderer before the paint method gets invoked
     * 
     * @param renderer
     */
    protected void onBeforeRender(StreamingRenderer renderer) {
        // TODO Auto-generated method stub
    }

    protected RenderedImageMap buildMap(final WMSMapContent mapContent, RenderedImage image) {
        RenderedImageMap map = new RenderedImageMap(mapContent, image, getMimeType());
        if(extension != null) {
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
        if (layoutName != null) {
            try {
                File layoutDir = GeoserverDataDirectory.findConfigDir(
                        GeoserverDataDirectory.getGeoserverDataDirectory(), "layouts");

                if (layoutDir != null) {
                    File layoutConfig = new File(layoutDir, layoutName + ".xml");

                    if (layoutConfig.exists() && layoutConfig.canRead()) {
                        layout = MapDecorationLayout.fromFile(layoutConfig, tiled);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown layout requested: " + layoutName);
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No layout directory defined");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load layout: " + layoutName, e);
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
     * 
     * @param width
     * @param height
     * @param paletteInverter
     * @return
     */
    protected RenderedImage prepareImage(int width, int height, IndexColorModel palette,
            boolean transparent) {
        return ImageUtils.createImage(width, height, isPaletteSupported() ? palette : null,
                transparent && isTransparencySupported());
    }

    /**
     * Returns true if the format supports image transparency, false otherwise (defaults to
     * {@code true})
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
     * Returns true if the format supports palette encoding, false otherwise (defaults to
     * {@code true}).
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
     * 
     * @param width
     * @param height
     * @param palette
     * @param transparent
     * @return
     */
    protected long getDrawingSurfaceMemoryUse(int width, int height, IndexColorModel palette,
            boolean transparent) {
        return ImageUtils.getDrawingSurfaceMemoryUse(width, height, isPaletteSupported() ? palette
                : null, transparent && isTransparencySupported());
    }

    /**
     * This takes an image with an indexed color model that uses less than 256 colors and has a 8bit
     * sample model, and transforms it to one that has the optimal sample model (for example, 1bit
     * if the palette only has 2 colors)
     * 
     * @param source
     * @return
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
        // TODO SIMONE why not format?
        return LookupDescriptor.create(source, IDENTITY_TABLE, hints);

    }

    /**
     * Renders a single coverage as the final RenderedImage to be encoded, skipping all of the
     * Java2D machinery and using a pure JAI chain of transformations instead. This considerably
     * improves both scalability and performance
     * 
     * @param mapContent
     *            The map definition (used for map size and transparency/color management)
     * @param layerIndex
     *            the layer that is supposed to contain a coverage
     * @param renderedCoverages
     *            placeholder where to deposit rendered coverages, if any, so that they can be
     *            disposed later
     * @return the result of rendering the coverage, or null if there was no coverage, or the
     *         coverage could not be renderer for some reason
     */
    private RenderedImage directRasterRender(WMSMapContent mapContent, int layerIndex,
            List<GridCoverage2D> renderedCoverages) throws IOException {
        
        //
        // extract the raster symbolizers 
        //
        List<RasterSymbolizer> symbolizers = getRasterSymbolizers(mapContent, 0);
        if (symbolizers.size() != 1){
            return null;
        }
        RasterSymbolizer symbolizer = symbolizers.get(0);

        //
        // Get the reader
        //
        final Feature feature = mapContent.layers().get(0).getFeatureSource().getFeatures().features().next();
        final GridCoverage2DReader reader = (GridCoverage2DReader) feature.getProperty("grid").getValue();
        final Object params = feature.getProperty("params").getValue();

        // 
        // Tiling
        //
        // if there is a output tile size hint, use it, otherwise use the output size itself
        final int tileSizeX;
        final int tileSizeY;
        if (mapContent.getTileSize() != -1) {
            tileSizeX = tileSizeY = mapContent.getTileSize();
        } else {
            tileSizeX = mapContent.getMapWidth();
            tileSizeY = mapContent.getMapHeight();
        }
        
        //
        // Dimensions
        //
        final int mapWidth = mapContent.getMapWidth();
        final int mapHeight= mapContent.getMapHeight();
        final ReferencedEnvelope mapEnvelope = mapContent.getRenderingArea();
        final CoordinateReferenceSystem mapCRS=mapContent.getCoordinateReferenceSystem();        
        final Rectangle mapRasterArea = new Rectangle(0, 0, mapWidth,mapHeight);
        final AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(mapEnvelope, mapRasterArea);        
         

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
        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (wms != null) {
            if (WMSInterpolation.Nearest.equals(wms.getInterpolation())) {
                interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            } else if (WMSInterpolation.Bilinear.equals(wms.getInterpolation())) {
                interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            } else if (WMSInterpolation.Bicubic.equals(wms.getInterpolation())) {
                interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            }
        }

        //
        // Dead best available coverage and render it
        //        
        final CoordinateReferenceSystem coverageCRS= reader.getCoordinateReferenceSystem();
        final GridGeometry2D readGG;
        final boolean equalsMetadata=CRS.equalsIgnoreMetadata(mapCRS, coverageCRS);
        boolean sameCRS;
        try {
            sameCRS = equalsMetadata?true:CRS.findMathTransform(mapCRS, coverageCRS,true).isIdentity();
        } catch (FactoryException e1) {
            final IOException ioe= new IOException();
            ioe.initCause(e1);
            throw ioe;
        }
        final boolean needsGutter=!sameCRS||!(interpolation instanceof InterpolationNearest);
        if(!needsGutter){
            readGG = new GridGeometry2D(
                        new GridEnvelope2D(mapRasterArea),
                        mapEnvelope
                     );
            
        }else{
            //
            // SG added gutter to the drawing. We need to investigate much more and also we need to do this only when needed
            //
            // enlarge raster area
            Rectangle bufferedTargetArea = (Rectangle) mapRasterArea.clone();
            bufferedTargetArea.add(mapRasterArea.x+mapRasterArea.width+10, mapRasterArea.y+mapRasterArea.height+10);
            bufferedTargetArea.add(mapRasterArea.x-10, mapRasterArea.y-10);
            
            // now create the final envelope accordingly
            try {
                readGG = new GridGeometry2D(
                        new GridEnvelope2D(bufferedTargetArea),
                        PixelInCell.CELL_CORNER,
                        new AffineTransform2D(worldToScreen.createInverse()),
                        mapCRS, 
                        null );
            } catch (Exception e) {
                final IOException ioe= new IOException();
                ioe.initCause(e);
                throw ioe;
            }    
        }

        // actual read
        RenderedImage image = null;
        try {
            
            
            final Color readerBgColor = transparent ? null : bgColor;
            final GridCoverage2D coverage = readBestCoverage(
                        reader, 
                        params,
                        ReferencedEnvelope.reference(readGG.getEnvelope()),
                        readGG.getGridRange2D(), 
                        interpolation,
                        readerBgColor);
            

            //
            // now, render the coverage using the gridcoverage renderer
            //
            try {
                if (coverage == null) { 
                    // we're outside of the coverage definition area, return an empty space
                    image = createBkgImage((float) mapWidth,(float) mapHeight, bgColor,null);
                } else {
                    
                    final GridCoverageRenderer gcr = new GridCoverageRenderer(
                            mapCRS,
                            ReferencedEnvelope.reference(readGG.getEnvelope()),
                            readGG.getGridRange2D(), 
                            worldToScreen, 
                            new RenderingHints(JAI.KEY_INTERPOLATION,interpolation));   
                    
                    // create a solid color empty image
                    image = gcr.renderImage(coverage, symbolizer, interpolation,
                            mapContent.getBgColor(), tileSizeX, tileSizeY);
                }
            } finally {
                // once the final image is rendered we need to clean up the planar image chain
                // that the coverage references to
                if (coverage != null)
                    renderedCoverages.add(coverage);
                
            }
        } catch (Throwable e) {
            throw new ServiceException(e);
        }

        // check if we managed to process the coverage into an image
        if (image == null) {
            return null;
        }
        
        ////
        //
        // Final Touch 
        ////
        //
        // We need to prepare the background values for the finalcut on the image we have prepared. If
        // we need to enlarge the image we go with Mosaic if we need to crop we use Crop. Notice that 
        // if we need to mess up with the background color we need to go by Mosaic and we cannot use Crop 
        // since it does not support changing the bkg color.
        //
        ////        
        final Rectangle imageBounds = PlanarImage.wrapRenderedImage(image).getBounds(); 
        
        // we need to do a mosaic, let's prepare a layout
        // prepare a final image layout should we need to perform a mosaic or a crop
        final ImageLayout layout = new ImageLayout();
        layout.setMinX(0);
        layout.setMinY(0);
        layout.setWidth(mapWidth);
        layout.setHeight(mapHeight);
        layout.setTileGridXOffset(0);
        layout.setTileGridYOffset(0);
        layout.setTileWidth(tileSizeX);
        layout.setTileHeight(tileSizeY);
        
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
        final int transparencyType=cm.getTransparency();
        
        // in case of index color model we try to preserve it, so that output
        // formats that can work with it can enjoy its extra compactness
        if (cm instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) cm;
            // try to find the index that matches the requested background color
            final int bgColorIndex;
            if(transparent) {
                bgColorIndex = icm.getTransparentPixel();
            } else {
                if(icm.hasAlpha() && icm.isAlphaPremultiplied()) {
                    // uncommon case that we don't have the code to handle directly
                    bgColorIndex = -1;
                } else {
                    if(icm.getTransparency() != Transparency.OPAQUE) {
                        // we have a translucent image, so the bg color needs to be merged into 
                        // the palette
                        icm = ColorUtilities.applyBackgroundColor(icm, bgColor);
                        cm = icm;
                        ImageLayout ilColorModel = new ImageLayout(image);
                        ilColorModel.setColorModel(icm);
                        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, ilColorModel);
                        image = FormatDescriptor.create(image, image.getSampleModel().getDataType(), hints);
                        worker.setImage(image);
                    } 
                    bgColorIndex = ColorUtilities.findColorIndex(bgColor, icm);
                }
            }
            
            // we did not find the background color, well we have to expand to RGB and then tell Mosaic to use the RGB(A) color as the
            // background
            if (bgColorIndex == -1) {
                // we need to expand the image to RGB
                image = worker.forceComponentColorModel().getRenderedImage();
                if(transparent) {
                    image = addAlphaChannel(image);
                    worker.setImage(image);
                }
                bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
                        transparent ? 0 : 255 };
                cm = image.getColorModel();
            } else {
            	// we found the background color in the original image palette therefore we set its index as the bkg value.
            	// The final Mosaic will use the IndexColorModel of this image anywa, therefore all we need to do is to force
            	// the background to point to the right color in the palette
                bgValues = new double[] { bgColorIndex };
            }
            
            // collect alpha channels if we have them in order to reuse them later on for mosaic operation
            if (cm.hasAlpha() && bgColorIndex == -1) {
                worker.forceComponentColorModel();
                final RenderedImage alpha = worker.retainLastBand().getRenderedImage();
                alphaChannels = new PlanarImage[] { PlanarImage.wrapRenderedImage(alpha) };
            } 
        }
        
        //
        // ComponentColorModel
        //
        
        // in case of component color model
        if (cm instanceof ComponentColorModel) {

            // convert to RGB if necessary
            ComponentColorModel ccm = (ComponentColorModel) cm;
            boolean hasAlpha = cm.hasAlpha();

            // if we have a grayscale image see if we have to expand to RGB
            if (ccm.getNumColorComponents() == 1) {
                if((!isLevelOfGray(bgColor) && !transparent) || (ccm.getTransferType() == DataBuffer.TYPE_DOUBLE || 
                        ccm.getTransferType() == DataBuffer.TYPE_FLOAT 
                        || ccm.getTransferType() == DataBuffer.TYPE_UNDEFINED)) {
                    // expand to RGB, this is not a case we can optimize
                    final ImageWorker iw = new ImageWorker(image);
                    if (hasAlpha) {
                        final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                        // get first band
                        final RenderedImage gray = new ImageWorker(image).retainFirstBand()
                                .getRenderedImage();
                        image = new ImageWorker(gray).bandMerge(3).addBand(alpha, false)
                                .forceComponentColorModel().forceColorSpaceRGB().getRenderedImage();
                    } else {
                        image = iw.bandMerge(3).forceComponentColorModel().forceColorSpaceRGB()
                                .getRenderedImage();
                    }
                } else if(!hasAlpha) {
                    // no transparency in the original data, so no need to expand to RGB
                    if(transparent) {
                        // we need to expand the image with an alpha channel
                        image = addAlphaChannel(image);
                        bgValues = new double[] { mapToGrayColor(bgColor, ccm), 0 };
                    } else {
                        bgValues = new double[] { mapToGrayColor(bgColor, ccm) };
                    }
                } else {
                    // extract the alpha channel
                    final ImageWorker iw = new ImageWorker(image);
                    final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                    alphaChannels = new PlanarImage[] { PlanarImage.wrapRenderedImage(alpha) };
                    
                    if (transparent) {
                        bgValues = new double[] { mapToGrayColor(bgColor, ccm), 0 };
                    } else {
                        bgValues = new double[] { mapToGrayColor(bgColor, ccm), 255 };
                    }
                } 

                // get back the ColorModel
                cm = image.getColorModel();
                ccm = (ComponentColorModel) cm;
                hasAlpha = cm.hasAlpha();
            }

            if(bgValues == null) {
                if (hasAlpha) {
                    // get alpha
    	            final ImageWorker iw = new ImageWorker(image);
                    final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
                    alphaChannels = new PlanarImage[] { PlanarImage.wrapRenderedImage(alpha) };
    
                    if (transparent) {
                        bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(),
                                bgColor.getBlue(), 0 };
                    } else {
                        bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(),
                                bgColor.getBlue(), 255 };
                    }
                } else {
                    if (transparent) {
                        image = addAlphaChannel(image);
                        // this will work fine for all situation where the color components are <= 3
                        // e.g., one band rasters with no colormap will have only one usually
                        bgValues = new double[] { 0, 0, 0, 0 };
                    } else {
                        // TODO: handle the case where the component color model is not RGB
                        // We cannot use ImageWorker as is because it basically seems to assume
                        // component -> 3 band in forceComponentColorModel()
                        // but I guess we'll need to turn the image into a 3 band RGB one.
                        bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(),
                                bgColor.getBlue() };
                    }
                }
            }
        }
        
        //
        // If we need to add a collar use mosaic or if we need to blend/apply a bkg color
        if(!(imageBounds.contains(mapRasterArea) || imageBounds.equals(mapRasterArea))||transparencyType!=Transparency.OPAQUE) {
            ROI[] rois = new ROI[] { new ROIShape(imageBounds) };

            // build the transparency thresholds
            double[][] thresholds = new double[][] { { ColorUtilities.getThreshold(image
                    .getSampleModel().getDataType()) } };
            // apply the mosaic
            image = MosaicDescriptor.create(new RenderedImage[] { image },
                    alphaChannels != null && transparencyType==Transparency.TRANSLUCENT ? MosaicDescriptor.MOSAIC_TYPE_BLEND: MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                    alphaChannels, rois, thresholds, bgValues, new RenderingHints(
                            JAI.KEY_IMAGE_LAYOUT, layout));
        } else {
            // Check if we need to crop a subset of the produced image, else return it right away
            if (imageBounds.contains(mapRasterArea) && !imageBounds.equals(mapRasterArea)) { // the produced image does not need a final mosaicking operation but a crop!
                ImageWorker iw = new ImageWorker(image);
                iw.crop(0, 0, mapWidth, mapHeight);
                image = iw.getRenderedImage();
            }
        }
        
//         RenderedImageBrowser.showChain(image);
        return image;
    }

    private RenderedImage addAlphaChannel(RenderedImage image) {
        final ImageLayout tempLayout= new ImageLayout(image);
        tempLayout.unsetValid(ImageLayout.COLOR_MODEL_MASK).unsetValid(ImageLayout.SAMPLE_MODEL_MASK);                    
        RenderedImage alpha = ConstantDescriptor.create(
                Float.valueOf( image.getWidth()),
                Float.valueOf(image.getHeight()),
                new Byte[] { Byte.valueOf((byte) 255) }, 
                new RenderingHints(JAI.KEY_IMAGE_LAYOUT,tempLayout));
        image = BandMergeDescriptor.create(image, alpha, null);
        return image;
    }

    /**
     * Given a one band (plus eventual alpha) color model and the red part of a gray
     * color returns the appropriate background color to be used in the mosaic operation
     * @param red
     * @param cm
     * @return
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

    /**
     * Returns true if the color is a level of gray
     * @param color
     * @return
     */
    private static boolean isLevelOfGray(Color color) {
        return color.getRed() == color.getBlue() && color.getRed() == color.getGreen();
    }

    /**
     * Creates a bkg image using the supplied parameters.
     * @param width the width of the timage to create
     * @param height the height of the image to create
     * @param bgColor the background color of the image to create
     * @param renderingHints the hints to apply
     * @return a {@link RenderedImage} with constant values as fill
     */
    private final static RenderedImage createBkgImage(float width, float height, Color bgColor,
            RenderingHints renderingHints) {
        // prepare bands for constant image if needed
        final Number[] bands = new Byte[] { (byte) bgColor.getRed(), (byte) bgColor.getGreen(),
                    (byte) bgColor.getBlue(), (byte) bgColor.getAlpha() };    
        return ConstantDescriptor.create(width,height, bands, renderingHints);
    }

    /**
     * Reads the best matching grid out of a grid coverage applying sub-sampling and using overviews
     * as necessary
     * 
     * @param mapContent
     * @param reader
     * @param params
     * @param requestedRasterArea
     * @param interpolation
     * @return
     * @throws IOException
     */
    private static GridCoverage2D readBestCoverage(
            final GridCoverage2DReader reader, 
            final Object params,
            final ReferencedEnvelope envelope,
            final Rectangle requestedRasterArea,
            final Interpolation interpolation,
            final Color bgColor) throws IOException {

        ////
        //
        // Intersect the present envelope with the request envelope, also in WGS 84 to make sure
        // there is an actual intersection
        //
        ////
        try {
            final CoordinateReferenceSystem coverageCRS=reader.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem requestCRS= envelope.getCoordinateReferenceSystem();
            final ReferencedEnvelope coverageEnvelope=new ReferencedEnvelope(reader.getOriginalEnvelope());
            if(CRS.equalsIgnoreMetadata(coverageCRS, requestCRS)){
                if(!coverageEnvelope.intersects((BoundingBox)envelope))
                    return null;
            }else{
                
                ReferencedEnvelope dataEnvelopeWGS84 = coverageEnvelope.transform(DefaultGeographicCRS.WGS84, true);
                ReferencedEnvelope requestEnvelopeWGS84 = envelope.transform(DefaultGeographicCRS.WGS84, true);
                if (!dataEnvelopeWGS84.intersects((BoundingBox) requestEnvelopeWGS84))
                    return null;                
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to compare data and request envelopes, proceeding with rendering anyways",
                    e);
        }

        // //
        // It is an GridCoverage2DReader, let's use parameters
        // if we have any supplied by a user.
        // //
        // first I created the correct ReadGeometry
        final Parameter<GridGeometry2D> readGG = (Parameter<GridGeometry2D>) AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
        readGG.setValue(new GridGeometry2D(new GridEnvelope2D(requestedRasterArea), envelope));
        
        final Parameter<Interpolation> readInterpolation=(Parameter<Interpolation>) ImageMosaicFormat.INTERPOLATION.createValue(); 
        readInterpolation.setValue(interpolation);
        
        final Parameter<Color> bgColorParam;
        if(bgColor != null) {
            bgColorParam = (Parameter<Color>) AbstractGridFormat.BACKGROUND_COLOR.createValue();
            bgColorParam.setValue(bgColor);
        } else {
            bgColorParam = null;
        }
        
        
        // then I try to get read parameters associated with this
        // coverage if there are any.
        GridCoverage2D coverage = null;
        GeneralParameterValue[] readParams = (GeneralParameterValue[]) params;
        final int length = readParams == null ? 0 :readParams.length;
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
            final String readInterpolationName = ImageMosaicFormat.INTERPOLATION.getName().toString();
            final String bgColorName = AbstractGridFormat.BACKGROUND_COLOR.getName().toString();
            int i = 0;
            boolean foundInterpolation = false;
            boolean foundGG = false;
            boolean foundBgColor = false;
            for (; i < length; i++) {
                final String paramName = readParams[i].getDescriptor().getName().toString();
                if (paramName.equalsIgnoreCase(readGGName)){
                    ((Parameter) readParams[i]).setValue(readGG);
                    foundGG = true;
                } else if(paramName.equalsIgnoreCase(readInterpolationName)){
                    ((Parameter) readParams[i]).setValue(interpolation);
                    foundInterpolation = true;
                } else if(paramName.equalsIgnoreCase(bgColorName) && bgColor != null) {
                    ((Parameter) readParams[i]).setValue(bgColor);
                    foundBgColor = true;
                }
            }
            
            // did we find anything?
            if (!foundGG || !foundInterpolation || !(foundBgColor && bgColor != null)) {
                // add the correct read geometry to the supplied
                // params since we did not find anything
                List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
                paramList.addAll(Arrays.asList(readParams));
                if(!foundGG) {
                     paramList.add(readGG);
                } 
                if(!foundInterpolation) {
                    paramList.add(readInterpolation);
                } 
                if(!foundBgColor && bgColor != null) {
                    paramList.add(bgColorParam);
                }
                readParams = (GeneralParameterValue[]) paramList.toArray(new GeneralParameterValue[paramList
                        .size()]);
            }
            coverage = (GridCoverage2D) reader.read(readParams);
        } else { 
            // if for any reason the previous block did not produce a coverage (no params, empty params)
            if(bgColorParam != null) {
                coverage = (GridCoverage2D) reader.read(new GeneralParameterValue[] {readGG ,readInterpolation, bgColorParam});
            } else {
                coverage = (GridCoverage2D) reader.read(new GeneralParameterValue[] {readGG ,readInterpolation});
            }
        }

        return coverage;
    }

    /**
     * Returns the list of raster symbolizers contained in a specific layer of the map context (the
     * full map context is provided in order to compute the current scale and thus determine the
     * active rules)
     * 
     * @param mc
     * @param layerIndex
     * @return
     */
    static List<RasterSymbolizer> getRasterSymbolizers(WMSMapContent mc, int layerIndex) {
        double scaleDenominator = mc.getScaleDenominator();
        Layer layer = mc.layers().get(layerIndex);
        FeatureType featureType = layer.getFeatureSource().getSchema();
        Style style = layer.getStyle();

        RasterSymbolizerVisitor visitor = new RasterSymbolizerVisitor(scaleDenominator, featureType);
        style.accept(visitor);

        return visitor.getRasterSymbolizers();
    }


}
