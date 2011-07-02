/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.RenderedImageList;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.MetatileMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ows.Layer;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WebMapServer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.filter.Filters;
import org.geotools.filter.function.EnvFunction;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.WMSLayer;
import org.geotools.map.WMSMapLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * WMS GetMap operation default implementation.
 * 
 * @author Gabriel Roldan
 */
public class GetMap {

    private static final Logger LOGGER = Logging.getLogger(GetMap.class);

    private FilterFactory ff;

    private final WMS wms;

    public GetMap(final WMS wms) {
        this.wms = wms;
        this.ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
    }

    public void setFilterFactory(final FilterFactory filterFactory) {
        this.ff = filterFactory;
    }

    /**
     * Implements the map production logic for a WMS GetMap request, delegating the encoding to the
     * appropriate output format to a {@link GetMapOutputFormat} appropriate for the required
     * format.
     * 
     * <p>
     * Preconditions:
     * <ul>
     * <li>request.getLayers().size() > 0
     * <li>request.getStyles().length == request.getLayers().size()
     * </ul>
     * </p>
     * 
     * @param req
     *            a {@link GetMapRequest}
     * 
     * @throws ServiceException
     *             if an error occurs creating the map from the provided request
     */
    public WebMap run(final GetMapRequest request) throws ServiceException {
        // JD/GR:hold a reference in order to release resources later. mapcontext can leak memory --
        // we make sure we done (see
        // finally block)
        WMSMapContext mapContext = new WMSMapContext(request);
        try {
            return run(request, mapContext);
        } catch (ServiceException e) {
            mapContext.dispose();
            throw e;
        } catch (RuntimeException e) {
            mapContext.dispose();
            throw (RuntimeException) e;
        } catch (Exception e) {
            mapContext.dispose();
            throw new ServiceException("Internal error ", e);
        }
    }

    /**
     * TODO: This method have become a 300+ lines monster, refactor it to private methods from which
     * names one can infer what's going on... but get a decent test coverage on it first as to avoid
     * regressions as much as possible
     */
    public WebMap run(final GetMapRequest request, WMSMapContext mapContext)
            throws ServiceException, IOException {
        assertMandatory(request);

        final String outputFormat = request.getFormat();

        GetMapOutputFormat delegate = getDelegate(outputFormat);

        // if there's a crs in the request, use that. If not, assume its 4326
        final CoordinateReferenceSystem mapcrs = request.getCrs();

        final List<MapLayerInfo> layers = request.getLayers();
        final Style[] styles = request.getStyles().toArray(new Style[] {});
        final Filter[] filters = buildLayersFilters(request.getFilter(), layers);
        final boolean isTiled = MetatileMapOutputFormat.isRequestTiled(request, delegate);

        //
        // check the capabilities for this delegate raster map produces
        //
        final MapProducerCapabilities cap = delegate.getCapabilities(request
                .getFormat());

        // is the request tiled? We support that?
        if (cap != null && !cap.isTiledRequestsSupported() && isTiled) {
            throw new ServiceException("Format " + request.getFormat()
                    + " does not support tiled requests");
        }
        // enable on the fly meta tiling if request looks like a tiled one
        if (MetatileMapOutputFormat.isRequestTiled(request, delegate)) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Tiled request detected, activating on the fly meta tiler");
            }

            delegate = new MetatileMapOutputFormat(request, (RenderedImageMapOutputFormat) delegate);
        }

        //
        // Test if the parameter "TIME" or ELEVATION are present in the WMS
        // request
        // TIME
        List<Object> times = request.getTime();
        final int numTimes = times.size();

        // ELEVATION
        final List<Object> elevations = request.getElevation();
        final int numElevations = elevations.size();

        // handling time series and elevation series
        final boolean isMultivaluedSupported = (cap != null ? cap.isMultivalueRequestsSupported() : false);
        if(numTimes > 1 && isMultivaluedSupported) {
            WebMap map = null;
            List<RenderedImage> images = new ArrayList<RenderedImage>();
            for (Object currentTime : times){
                map = executeInternal(mapContext, request, delegate, Arrays.asList(currentTime), elevations);
                
                // remove layers to start over again
                mapContext.clearLayerList();
                
                // collect the layer
                images.add(((RenderedImageMap)map).getImage());
            }
            RenderedImageList imageList = new RenderedImageList(images);
            return new  RenderedImageMap(mapContext, imageList , map.getMimeType());
        } else if(numElevations > 1 && isMultivaluedSupported) {
            WebMap map = null;
            List<RenderedImage> images = new ArrayList<RenderedImage>();
            for (Object currentElevation : elevations){
                map = executeInternal(mapContext, request, delegate, times, Arrays.asList(currentElevation));
                
                // remove layers to start over again
                mapContext.clearLayerList();
                
                // collect the layer
                images.add(((RenderedImageMap)map).getImage());
            }
            RenderedImageList imageList = new RenderedImageList(images);
            return new  RenderedImageMap(mapContext, imageList , map.getMimeType());
        } else {
            return executeInternal(mapContext, request, delegate, times, elevations);    
        }

    }

    /**
     * Actually computes the WebMap, either in a single shot, or for a particular
     * time/elevation value should there be a list of them
     * @param request
     * @param mapContext
     * @param delegate
     * @param env
     * @return
     * @throws IOException
     */
    WebMap executeInternal(WMSMapContext mapContext, final GetMapRequest request,
            GetMapOutputFormat delegate, List<Object> times, List<Object> elevations) throws IOException {
        final Envelope envelope = request.getBbox();       
        final List<MapLayerInfo> layers = request.getLayers();
        final List<Map<String, String>> viewParams = request.getViewParams();
        
        final Style[] styles = request.getStyles().toArray(new Style[] {});
        final Filter[] filters = buildLayersFilters(request.getFilter(), layers);

        // if there's a crs in the request, use that. If not, assume its 4326
        final CoordinateReferenceSystem mapcrs = request.getCrs();

        // DJB: added this to be nicer about the "NONE" srs.
        if (mapcrs != null) {
            mapContext.setAreaOfInterest(envelope, mapcrs);
        } else {
            mapContext.setAreaOfInterest(envelope, DefaultGeographicCRS.WGS84);
        }

        mapContext.setMapWidth(request.getWidth());
        mapContext.setMapHeight(request.getHeight());
        mapContext.setAngle(request.getAngle());
        mapContext.setBgColor(request.getBgColor());
        mapContext.setTransparent(request.isTransparent());
        mapContext.setBuffer(request.getBuffer());
        mapContext.setPaletteInverter(request.getPalette());

        // //
        //
        // Check to see if we really have something to display. Sometimes width
        // or height or both are non positive or the requested area is null.
        //
        // ///
        if ((request.getWidth() <= 0) || (request.getHeight() <= 0)
                || (mapContext.getAreaOfInterest().getLength(0) <= 0)
                || (mapContext.getAreaOfInterest().getLength(1) <= 0)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("We are not going to render anything because either the area is null or the dimensions are not positive.");
            }

            return null;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("setting up map");
        }

        // track the external caching strategy for any map layers
        boolean cachingPossible = request.isGet();
        final String featureVersion = request.getFeatureVersion();
        int maxAge = Integer.MAX_VALUE;
        for (int i = 0; i < layers.size(); i++) {
            final MapLayerInfo mapLayerInfo = layers.get(i);

            cachingPossible &= mapLayerInfo.isCachingEnabled();
            if (cachingPossible) {
                maxAge = Math.min(maxAge, mapLayerInfo.getCacheMaxAge());
            } else {
                cachingPossible = false;
            }

            final Style layerStyle = styles[i];
            final Filter layerFilter = filters[i];

            final org.geotools.map.Layer layer;

            int layerType = mapLayerInfo.getType();
            if (layerType == MapLayerInfo.TYPE_REMOTE_VECTOR) {

                final SimpleFeatureSource source = mapLayerInfo.getRemoteFeatureSource();
                FeatureLayer featureLayer = new FeatureLayer(source, layerStyle);
                featureLayer.setTitle(mapLayerInfo.getRemoteFeatureSource().getSchema().getTypeName());

                final Query definitionQuery = new Query(source.getSchema().getTypeName());
                definitionQuery.setFilter(layerFilter);
                definitionQuery.setVersion(featureVersion);
                int maxFeatures = request.getMaxFeatures() != null ? request.getMaxFeatures()
                        : Integer.MAX_VALUE;
                definitionQuery.setMaxFeatures(maxFeatures);
                featureLayer.setQuery(definitionQuery);
                
                mapContext.addLayer(featureLayer);
                
                layer = featureLayer;
            } else if (layerType == MapLayerInfo.TYPE_VECTOR) {
                FeatureSource<? extends FeatureType, ? extends Feature> source;
                // /////////////////////////////////////////////////////////
                //
                // Adding a feature layer
                //
                // /////////////////////////////////////////////////////////
                try {
                    source = mapLayerInfo.getFeatureSource(true);

                    // NOTE for the feature. Here there was some code that
                    // sounded like:
                    // * get the bounding box from feature source
                    // * eventually reproject it to the actual CRS used for
                    // map
                    // * if no intersection, don't bother adding the feature
                    // source to the map
                    // This is not an optimization, on the contrary,
                    // computing the bbox may be
                    // very expensive depending on the data size. Using
                    // sigma.openplans.org data
                    // and a tiled client like OpenLayers, it dragged the
                    // server to his knees
                    // and the client simply timed out
                } catch (IOException exp) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, new StringBuffer("Getting feature source: ")
                                .append(exp.getMessage()).toString(), exp);
                    }

                    throw new ServiceException("Internal error", exp);
                }
                FeatureLayer featureLayer = new FeatureLayer(source, layerStyle);
                featureLayer.setTitle(mapLayerInfo.getFeature().getPrefixedName());
                
                // mix the dimension related filter with the layer filter
                Filter dimensionFilter = wms.getTimeElevationToFilter(times, elevations, mapLayerInfo.getFeature());
                Filter filter = Filters.and(ff, layerFilter, dimensionFilter); 

                final Query definitionQuery = new Query(source.getSchema().getName().getLocalPart());
                definitionQuery.setVersion(featureVersion);
                definitionQuery.setFilter(filter);
            	if (viewParams != null) {
                    definitionQuery.setHints(new Hints(Hints.VIRTUAL_TABLE_PARAMETERS, viewParams.get(i)));
            	}

                // check for startIndex + offset
                final Integer startIndex = request.getStartIndex();
                if (startIndex != null) {
                    QueryCapabilities queryCapabilities = source.getQueryCapabilities();
                    if (queryCapabilities.isOffsetSupported()) {
                        // fsource is required to support
                        // SortBy.NATURAL_ORDER so we don't bother checking
                        definitionQuery.setStartIndex(startIndex);
                    } else {
                        // source = new PagingFeatureSource(source,
                        // request.getStartIndex(), limit);
                        throw new ServiceException("startIndex is not supported for the "
                                + mapLayerInfo.getName() + " layer");
                    }
                }

                int maxFeatures = request.getMaxFeatures() != null ? request.getMaxFeatures()
                        : Integer.MAX_VALUE;
                definitionQuery.setMaxFeatures(maxFeatures);

                featureLayer.setQuery(definitionQuery);
                mapContext.addLayer(featureLayer);
                
                layer = featureLayer;
            } else if (layerType == MapLayerInfo.TYPE_RASTER) {

                // /////////////////////////////////////////////////////////
                //
                // Adding a coverage layer
                //
                // /////////////////////////////////////////////////////////
                final AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) mapLayerInfo
                        .getCoverageReader();
                if (reader != null) {

                    // get the group of parameters tha this reader supports
                    GeneralParameterValue[] readParameters = wms.getWMSReadParameters(request,
                            mapLayerInfo, layerFilter, times, elevations, reader, false);
                    try {

                        try {
                            layer = new GridReaderLayer( reader, layerStyle,  readParameters);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        layer.setTitle(mapLayerInfo.getCoverage().getPrefixedName());

                        mapContext.addLayer(layer);
                    } catch (IllegalArgumentException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    new StringBuilder("Wrapping GC in feature source: ").append(
                                            e.getLocalizedMessage()).toString(), e);
                        }

                        throw new ServiceException(
                                "Internal error : unable to get reader for this coverage layer "
                                        + mapLayerInfo);
                    }
                } else {
                    throw new ServiceException(new StringBuffer(
                            "Internal error : unable to get reader for this coverage layer ")
                            .append(mapLayerInfo.toString()).toString());
                }
            } else if (layerType == MapLayerInfo.TYPE_WMS) {
                WMSLayerInfo wmsLayer = (WMSLayerInfo) mapLayerInfo.getResource();
                WebMapServer wms = wmsLayer.getStore().getWebMapServer(null);
                Layer gt2Layer = wmsLayer.getWMSLayer(null);

                // see if we can merge this layer with the previous one
                boolean merged = false;
                if (mapContext.getLayerCount() > 0) {
                    org.geotools.map.Layer lastLayer = mapContext.layers().get(mapContext.getLayerCount() - 1);
                    if (lastLayer instanceof WMSLayer) {
                        WMSLayer lastWMS = (WMSLayer) lastLayer;
                        WebMapServer otherWMS = lastWMS.getWebMapServer();
                        if (otherWMS.equals(wms)) {
                            lastWMS.addLayer(gt2Layer);
                            merged = true;
                        }
                    }
                }
                if (!merged) {
                    WMSMapLayer Layer = new WMSMapLayer(wms, gt2Layer);
                    Layer.setTitle(wmsLayer.getPrefixedName());
                    mapContext.addLayer(Layer);
                }
            } else {
                throw new IllegalArgumentException("Unkown layer type " + layerType);
            }
        }

        // setup the SLD variable substitution environment
        Map envMap = new HashMap(request.getEnv());
        envMap.put("wms_bbox", mapContext.getAreaOfInterest());
        envMap.put("wms_crs", mapContext.getAreaOfInterest().getCoordinateReferenceSystem());
        envMap.put("wms_srs", mapContext.getRequest().getSRS());
        envMap.put("wms_width", mapContext.getMapWidth());
        envMap.put("wms_height", mapContext.getMapHeight());
        EnvFunction.setLocalValues(envMap);

        WebMap map;
        try {
            // set the buffer value if the admin has set a specific value for some layers
            // in this map
            // GR: question: does setupRenderingBuffer need EnvFunction.setLocalValues to be already
            // set? otherwise move this call out of the try block and above setLovalValues
            setupRenderingBuffer(mapContext, layers);

            // /////////////////////////////////////////////////////////
            //
            // Producing the map in the requested format.
            //
            // /////////////////////////////////////////////////////////
            map = delegate.produceMap(mapContext);

        } finally {
            EnvFunction.clearLocalValues();
        }
        
        if (cachingPossible) {
            map.setResponseHeader("Cache-Control", "max-age=" + maxAge + ", must-revalidate");

            Date expires = new Date();
            expires.setTime(expires.getTime() + maxAge * 1000);
            DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            map.setResponseHeader("Expires", format.format(expires));
        }

        return map;
    }

    

	/**
     * Computes the rendering buffer in case the user did not specify one in the request, and the
     * admin setup some rendering buffer hints in the layer configurations
     * 
     * @param map
     * @param layers
     */
    public static void setupRenderingBuffer(WMSMapContext map, List<MapLayerInfo> layers) {
        // easy case, the buffer is already set in the call
        if (map.getBuffer() > 0) {
            return;
        }

        // let's collect the layers that do have a buffer set, we can
        // skip the computation if there is none set
        int[] layerBuffers = new int[layers.size()];
        boolean computeBuffer = false;
        for (int i = 0; i < layers.size(); i++) {
            final LayerInfo layerInfo = layers.get(i).getLayerInfo();
            if (layerInfo != null) { // it is a local layer
                Integer layerBuffer = layerInfo.getMetadata().get(LayerInfo.BUFFER, Integer.class);
                if (layerBuffer != null && layerBuffer > 0) {
                    computeBuffer = true;
                    layerBuffers[i] = layerBuffer;
                }
            }
        }

        if (computeBuffer) {
            final double scaleDenominator = getRequestScale(map);
            int buffer = 0;

            // either use the preset buffer, or if missing, compute one on the fly based
            // on an analysis of the active rules at the current scale
            for (int i = 0; i < layers.size(); i++) {
                int layerBuffer = layerBuffers[i];
                if (layerBuffer == 0) {
                    layerBuffer = computeLayerBuffer(map.getLayer(i).getStyle(), scaleDenominator);
                }
                if (layerBuffer > buffer) {
                    buffer = layerBuffer;
                }
            }

            map.setBuffer(buffer);
        }
    }

    /**
     * Computes the rendering buffer for this layer
     * 
     * @param style
     * @param scaleDenominator
     * @return
     */
    static int computeLayerBuffer(Style style, double scaleDenominator) {
        final double TOLERANCE = 1e-6;
        MetaBufferEstimator estimator = new MetaBufferEstimator();
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule rule : fts.rules()) {
                if (((rule.getMinScaleDenominator() - TOLERANCE) <= scaleDenominator)
                        && ((rule.getMaxScaleDenominator() + TOLERANCE) > scaleDenominator)) {
                    estimator.visit(rule);
                }
            }
        }

        // we get any estimate, it's better than nothing...
        return estimator.getBuffer();
    }

    /**
     * Returns the rendering scale taking into account rotation and dpi
     * 
     * @param map
     * @return
     */
    static double getRequestScale(WMSMapContext map) {
        java.util.Map hints = new HashMap();
        if (map.getRequest().getFormatOptions().get("dpi") != null) {
            hints.put(StreamingRenderer.DPI_KEY, ((Integer) map.getRequest().getFormatOptions()
                    .get("dpi")));
        }
        return RendererUtilities.calculateOGCScaleAffine(map.getCoordinateReferenceSystem(),
                map.getRenderingTransform(), hints);
    }

    /**
     * Asserts the mandatory GetMap parameters have been provided.
     * <p>
     * With the exception of the SRS and STYLES parameters, for which default values are assigned.
     * </p>
     * 
     * @param request
     * @throws ServiceException
     *             if any mandatory parameter has not been set on the request
     */
    private void assertMandatory(GetMapRequest request) throws ServiceException {
        if (0 >= request.getWidth() || 0 >= request.getHeight()) {
            throw new ServiceException("Missing or invalid requested map size. Parameters"
                    + " WIDTH and HEIGHT shall be present and be integers > 0. Got " + "WIDTH="
                    + request.getWidth() + ", HEIGHT=" + request.getHeight(),
                    "MissingOrInvalidParameter");
        }

        if (request.getLayers().size() == 0) {
            throw new ServiceException("No layers have been requested", "LayerNotDefined");
        }

        if (request.getStyles().size() == 0) {
            throw new ServiceException("No styles have been requested", "StyleNotDefined");
        }

        if (request.getFormat() == null) {
            throw new ServiceException("No output map format requested", "InvalidFormat");
        }

        // DJB: the WMS spec says that the request must not be 0 area
        // if it is, throw a service exception!
        final Envelope env = request.getBbox();
        if (env == null) {
            throw new ServiceException("GetMap requests must include a BBOX parameter.",
                    "MissingBBox");
        }
        if (env.isNull() || (env.getWidth() <= 0) || (env.getHeight() <= 0)) {
            throw new ServiceException(new StringBuffer("The request bounding box has zero area: ")
                    .append(env).toString(), "InvalidBBox");
        }
    }

    /**
     * Returns the list of filters resulting of comining the layers definition filters with the per
     * layer filters made by the user.
     * <p>
     * If <code>requestFilters != null</code>, it shall contain the same number of elements than
     * <code>layers</code>, as filters are requested one per layer.
     * </p>
     * 
     * @param requestFilters
     *            the list of filters sent by the user, or <code>null</code>
     * @param layers
     *            the layers requested in the GetMap request, where to get the per layer definition
     *            filters from.
     * @return a list of filters, one per layer, resulting of anding the user requested filter and
     *         the layer definition filter
     */
    private Filter[] buildLayersFilters(List<Filter> requestFilters, List<MapLayerInfo> layers) {
        final int nLayers = layers.size();
        if (requestFilters == null || requestFilters.size() == 0) {
            requestFilters = Collections.nCopies(layers.size(), (Filter) Filter.INCLUDE);
        } else if (requestFilters.size() != nLayers) {
            throw new IllegalArgumentException(
                    "requested filters and number of layers do not match");
        }
        Filter[] combinedList = new Filter[nLayers];
        Filter layerDefinitionFilter;
        Filter userRequestedFilter;
        Filter combined;

        MapLayerInfo layer;
        for (int i = 0; i < nLayers; i++) {
            layer = layers.get(i);
            userRequestedFilter = requestFilters.get(i);
            if (layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR || layer.getType() == MapLayerInfo.TYPE_RASTER) {
                combinedList[i] = userRequestedFilter;
            } else if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                layerDefinitionFilter = layer.getFeature().getFilter();

                // heck, how I wish we use the null objects more
                if (layerDefinitionFilter == null) {
                    layerDefinitionFilter = Filter.INCLUDE;
                }
                combined = ff.and(layerDefinitionFilter, userRequestedFilter);

                FeatureTypeConstraint[] featureTypeConstraints = layer.getLayerFeatureConstraints();
                if (featureTypeConstraints != null) {
                    List<Filter> filters = new ArrayList<Filter>();
                    for (int j = 0; j < featureTypeConstraints.length; j++) {
                        FeatureTypeConstraint featureTypeConstraint = featureTypeConstraints[j];
                        filters.add(featureTypeConstraint.getFilter());
                    }
                    ;
                    combined = ff.and(combined, ff.and(filters));
                }
                combinedList[i] = combined;
            }
        }
        return combinedList;
    }

    /**
     * Finds out a {@link GetMapOutputFormat} specialized in generating the requested map format,
     * registered in the spring context.
     * 
     * @param outputFormat
     *            a request parameter object wich holds the processed request objects, such as
     *            layers, bbox, outpu format, etc.
     * 
     * @return A specialization of <code>GetMapDelegate</code> wich can produce the requested output
     *         map format
     * 
     * @throws ServiceException
     *             if no specialization is configured for the output format specified in
     *             <code>request</code> or if it can't be instantiated
     */
    private GetMapOutputFormat getDelegate(final String outputFormat) throws ServiceException {

        final GetMapOutputFormat producer = wms.getMapOutputFormat(outputFormat);
        if (producer == null) {
            ServiceException e = new ServiceException("There is no support for creating maps in "
                    + outputFormat + " format", "InvalidFormat");
            e.setCode("InvalidFormat");
            throw e;
        }
        return producer;
    }

}
