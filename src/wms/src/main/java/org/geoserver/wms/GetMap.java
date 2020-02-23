/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.RenderedImageList;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.MetatileMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Filters;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.map.WMSLayer;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.map.WMTSMapLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * WMS GetMap operation default implementation.
 *
 * @author Gabriel Roldan
 */
public class GetMap {

    private static final Logger LOGGER = Logging.getLogger(GetMap.class);

    private FilterFactory ff;

    private final WMS wms;

    private List<GetMapCallback> callbacks;

    public GetMap(final WMS wms) {
        this.wms = wms;
        this.ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        this.callbacks = GeoServerExtensions.extensions(GetMapCallback.class);
    }

    public void setFilterFactory(final FilterFactory filterFactory) {
        this.ff = filterFactory;
    }

    public void setGetMapCallbacks(List<GetMapCallback> callbacks) {
        this.callbacks.clear();
        this.callbacks.addAll(callbacks);
    }

    /**
     * Implements the map production logic for a WMS GetMap request, delegating the encoding to the
     * appropriate output format to a {@link GetMapOutputFormat} appropriate for the required
     * format.
     *
     * <p>Preconditions:
     *
     * <ul>
     *   <li>request.getLayers().size() > 0
     *   <li>request.getStyles().length == request.getLayers().size()
     * </ul>
     *
     * @param request a {@link GetMapRequest}
     * @throws ServiceException if an error occurs creating the map from the provided request
     */
    public WebMap run(GetMapRequest request) throws ServiceException {
        request = fireInitRequest(request);
        // JD/GR:hold a reference in order to release resources later. mapcontext can leak memory --
        // we make sure we done (see finally block)
        WMSMapContent mapContent = new WMSMapContent(request);
        mapContent.setGetMapCallbacks(callbacks);
        try {
            WebMap map = run(request, mapContent);
            map = fireFinished(map);
            return map;
        } catch (Throwable t) {
            mapContent.dispose();
            fireFailed(t);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new ServiceException("Internal error ", t);
            }
        }
    }

    private GetMapRequest fireInitRequest(GetMapRequest request) {
        for (GetMapCallback callback : callbacks) {
            request = callback.initRequest(request);
        }

        return request;
    }

    private void fireMapContentInit(WMSMapContent mapContent) {
        for (GetMapCallback callback : callbacks) {
            callback.initMapContent(mapContent);
        }
    }

    private WMSMapContent fireBeforeRender(WMSMapContent mapContent) {
        for (GetMapCallback callback : callbacks) {
            mapContent = callback.beforeRender(mapContent);
        }

        return mapContent;
    }

    private WebMap fireFinished(WebMap result) {
        for (GetMapCallback callback : callbacks) {
            result = callback.finished(result);
        }

        return result;
    }

    private void fireFailed(Throwable t) {
        for (GetMapCallback callback : callbacks) {
            callback.failed(t);
        }
    }

    /**
     * TODO: This method have become a 300+ lines monster, refactor it to private methods from which
     * names one can infer what's going on... but get a decent test coverage on it first as to avoid
     * regressions as much as possible
     */
    public WebMap run(final GetMapRequest request, WMSMapContent mapContent)
            throws ServiceException, IOException {
        assertMandatory(request);

        final String outputFormat = request.getFormat();

        GetMapOutputFormat delegate = getDelegate(outputFormat);

        final boolean isTiled = MetatileMapOutputFormat.isRequestTiled(request, delegate);

        //
        // check the capabilities for this delegate raster map produces
        //
        final MapProducerCapabilities cap = delegate.getCapabilities(request.getFormat());

        // is the request tiled? We support that?
        if (cap != null && !cap.isTiledRequestsSupported() && isTiled) {
            throw new ServiceException(
                    "Format " + request.getFormat() + " does not support tiled requests");
        }
        // enable on the fly meta tiling if request looks like a tiled one
        if (MetatileMapOutputFormat.isRequestTiled(request, delegate)) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Tiled request detected, activating on the fly meta tiler");
            }

            delegate =
                    new MetatileMapOutputFormat(request, (RenderedImageMapOutputFormat) delegate);
        }

        // check if the format can do animations
        final boolean isMultivaluedSupported =
                (cap != null ? cap.isMultivalueRequestsSupported() : false);

        //
        // Test if the parameter "TIME" or ELEVATION are present in the WMS
        // request
        // TIME
        List<Object> times = request.getTime();
        final int numTimes = times.size();
        boolean singleTimeRange = numTimes == 1 && times.get(0) instanceof DateRange;

        // ELEVATION
        List<Object> elevations = request.getElevation();
        final int numElevations = elevations.size();
        boolean singleElevationRange =
                numElevations == 1 && elevations.get(0) instanceof NumberRange;

        // handling time series and elevation series
        MaxAnimationTimeHelper maxAnimationTimeHelper = new MaxAnimationTimeHelper(wms, request);
        int maxAllowedFrames = wms.getMaxAllowedFrames();
        if ((numTimes > 1 || singleTimeRange) && isMultivaluedSupported) {
            WebMap map = null;
            List<RenderedImage> images = new ArrayList<RenderedImage>();
            if (singleTimeRange) {
                List<Object> expandTimeList =
                        expandTimeList((DateRange) times.get(0), request, maxAllowedFrames);
                if (expandTimeList.size() == 0) {
                    return executeInternal(
                            mapContent, request, delegate, Arrays.asList(times.get(0)), elevations);
                } else {
                    times = expandTimeList;
                }
            }
            for (Object currentTime : times) {
                maxAnimationTimeHelper.checkTimeout();
                map =
                        executeInternal(
                                mapContent,
                                request,
                                delegate,
                                Arrays.asList(currentTime),
                                elevations);

                // remove layers to start over again
                mapContent.layers().clear();

                // collect the layer
                images.add(((RenderedImageMap) map).getImage());
            }
            RenderedImageList imageList = new RenderedImageList(images);
            return new RenderedImageMap(mapContent, imageList, map.getMimeType());
        } else if ((numElevations > 1 || singleElevationRange) && isMultivaluedSupported) {
            WebMap map = null;
            List<RenderedImage> images = new ArrayList<RenderedImage>();
            if (singleElevationRange) {
                List<Object> expandElevationList =
                        expandElevationList(
                                (NumberRange) elevations.get(0), request, maxAllowedFrames);
                if (expandElevationList.size() == 0) {
                    map =
                            executeInternal(
                                    mapContent,
                                    request,
                                    delegate,
                                    times,
                                    Arrays.asList(elevations.get(0)));
                } else {
                    elevations = expandElevationList;
                }
            }
            for (Object currentElevation : elevations) {
                maxAnimationTimeHelper.checkTimeout();
                map =
                        executeInternal(
                                mapContent,
                                request,
                                delegate,
                                times,
                                Arrays.asList(currentElevation));

                // remove layers to start over again
                mapContent.layers().clear();

                // collect the layer
                images.add(((RenderedImageMap) map).getImage());
            }
            RenderedImageList imageList = new RenderedImageList(images);
            return new RenderedImageMap(mapContent, imageList, map.getMimeType());
        } else {
            return executeInternal(mapContent, request, delegate, times, elevations);
        }
    }

    private List<Object> expandTimeList(
            DateRange queryRange, GetMapRequest request, int maxAllowedFrames) {
        TreeSet<Date> result = new TreeSet<>();
        try {
            for (MapLayerInfo l : request.getLayers()) {
                ResourceInfo ri = l.getLayerInfo().getResource();
                if (ri == null) {
                    continue;
                }
                DimensionInfo timeInfo =
                        ri.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo == null) {
                    continue;
                }

                // it has time configured
                if (l.getType() == MapLayerInfo.TYPE_VECTOR) {
                    TreeSet<Object> times =
                            wms.queryFeatureTypeTimes(l.getFeature(), queryRange, maxAllowedFrames);
                    accumulateTimes(result, times, maxAllowedFrames);
                } else if (l.getType() == MapLayerInfo.TYPE_RASTER) {
                    TreeSet<Object> times =
                            wms.queryCoverageTimes(l.getCoverage(), queryRange, maxAllowedFrames);
                    accumulateTimes(result, times, maxAllowedFrames);
                }
            }
        } catch (IOException e) {
            throw new ServiceException(
                    "Failed to compute list of times in the range " + queryRange, e);
        }

        return new ArrayList<>(result);
    }

    private void accumulateTimes(
            TreeSet<Date> result, TreeSet<Object> times, int maxAllowedFrames) {
        for (Object time : times) {
            if (time instanceof Date) {
                result.add((Date) time);
            } else if (time instanceof DateRange) {
                DateRange range = ((DateRange) time);
                Date rangeMid =
                        new Date(
                                (range.getMinValue().getTime() + range.getMaxValue().getTime())
                                        / 2);
                result.add(rangeMid);
            }
            if (result.size() > maxAllowedFrames) {
                throw new ServiceException("Too many steps in the animation");
            }
        }
    }

    private List<Object> expandElevationList(
            NumberRange queryRange, GetMapRequest request, int maxAllowedFrames) {
        TreeSet<Double> result = new TreeSet<>();
        try {
            for (MapLayerInfo l : request.getLayers()) {
                ResourceInfo ri = l.getLayerInfo().getResource();
                if (ri == null) {
                    continue;
                }
                DimensionInfo elevationInfo =
                        ri.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
                if (elevationInfo == null) {
                    continue;
                }

                // it has elevaton configured
                if (l.getType() == MapLayerInfo.TYPE_VECTOR) {
                    TreeSet<Object> elevations =
                            wms.queryFeatureTypeElevations(
                                    l.getFeature(), queryRange, maxAllowedFrames);
                    accumulateElevations(result, elevations, maxAllowedFrames);
                } else if (l.getType() == MapLayerInfo.TYPE_RASTER) {
                    TreeSet<Object> elevations =
                            wms.queryCoverageElevations(
                                    l.getCoverage(), queryRange, maxAllowedFrames);
                    accumulateElevations(result, elevations, maxAllowedFrames);
                }
            }
        } catch (IOException e) {
            throw new ServiceException(
                    "Failed to compute list of times in the range " + queryRange, e);
        }

        return new ArrayList<>(result);
    }

    private void accumulateElevations(
            TreeSet<Double> result, TreeSet<Object> elevations, int maxAllowedFrames) {
        for (Object elevation : elevations) {
            if (elevation instanceof Number) {
                result.add(((Number) elevation).doubleValue());
            } else if (elevation instanceof NumberRange) {
                NumberRange range = ((NumberRange) elevation);
                double rangeMid = (range.getMinimum() + range.getMaximum()) / 2;
                result.add(rangeMid);
            }
            if (result.size() > maxAllowedFrames) {
                throw new ServiceException("Too many steps in the animation");
            }
        }
    }

    /**
     * Actually computes the WebMap, either in a single shot, or for a particular time/elevation
     * value should there be a list of them
     */
    WebMap executeInternal(
            WMSMapContent mapContent,
            final GetMapRequest request,
            GetMapOutputFormat delegate,
            List<Object> times,
            List<Object> elevations)
            throws IOException {
        final Envelope envelope = request.getBbox();
        final List<MapLayerInfo> layers = request.getLayers();
        final List<Map<String, String>> viewParams = request.getViewParams();

        final Style[] styles = request.getStyles().toArray(new Style[] {});
        final Filter[] filters = buildLayersFilters(request.getFilter(), layers);
        final List<SortBy[]> sorts = request.getSortByArrays();

        // if there's a crs in the request, use that. If not, assume its 4326
        final CoordinateReferenceSystem mapcrs = request.getCrs();

        // DJB: added this to be nicer about the "NONE" srs.
        if (mapcrs != null) {
            mapContent.getViewport().setBounds(new ReferencedEnvelope(envelope, mapcrs));
        } else {
            mapContent
                    .getViewport()
                    .setBounds(new ReferencedEnvelope(envelope, DefaultGeographicCRS.WGS84));
        }

        mapContent.setMapWidth(request.getWidth());
        mapContent.setMapHeight(request.getHeight());
        mapContent.setAngle(request.getAngle());
        mapContent.setBgColor(request.getBgColor());
        mapContent.setTransparent(request.isTransparent());
        mapContent.setBuffer(request.getBuffer());
        mapContent.setPalette(request.getPalette());

        // //
        //
        // Check to see if we really have something to display. Sometimes width
        // or height or both are non positive or the requested area is null.
        //
        // ///
        if ((request.getWidth() <= 0)
                || (request.getHeight() <= 0)
                || (mapContent.getRenderingArea().getSpan(0) <= 0)
                || (mapContent.getRenderingArea().getSpan(1) <= 0)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "We are not going to render anything because either the area is null or the dimensions are not positive.");
            }

            return null;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("setting up map");
        }

        fireMapContentInit(mapContent);

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
            final Filter layerFilter = SimplifyingFilterVisitor.simplify(filters[i]);
            final SortBy[] layerSort = sorts != null ? sorts.get(i) : null;

            final org.geotools.map.Layer layer;

            int layerType = mapLayerInfo.getType();
            if (layerType == MapLayerInfo.TYPE_REMOTE_VECTOR) {

                final SimpleFeatureSource source = mapLayerInfo.getRemoteFeatureSource();
                FeatureLayer featureLayer = new FeatureLayer(source, layerStyle);
                featureLayer.setTitle(
                        mapLayerInfo.getRemoteFeatureSource().getSchema().getTypeName());

                final Query definitionQuery = new Query(source.getSchema().getTypeName());
                definitionQuery.setFilter(layerFilter);
                definitionQuery.setVersion(featureVersion);
                definitionQuery.setSortBy(layerSort);
                int maxFeatures =
                        request.getMaxFeatures() != null
                                ? request.getMaxFeatures()
                                : Integer.MAX_VALUE;
                definitionQuery.setMaxFeatures(maxFeatures);
                featureLayer.setQuery(definitionQuery);

                mapContent.addLayer(featureLayer);

                layer = featureLayer;
            } else if (layerType == MapLayerInfo.TYPE_VECTOR) {
                FeatureSource<? extends FeatureType, ? extends Feature> source;
                // /////////////////////////////////////////////////////////
                //
                // Adding a feature layer
                //
                // /////////////////////////////////////////////////////////
                try {
                    source = mapLayerInfo.getFeatureSource(true, request.getCrs());

                    if (layerSort != null) {
                        // filter gets validated down in the renderer, but
                        // sorting is done without the renderer knowing, perform validation here
                        validateSort(source, layerSort, mapLayerInfo);
                    }

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
                        LOGGER.log(
                                Level.SEVERE,
                                new StringBuffer("Getting feature source: ")
                                        .append(exp.getMessage())
                                        .toString(),
                                exp);
                    }

                    throw new ServiceException("Internal error", exp);
                }
                FeatureLayer featureLayer = new FeatureLayer(source, layerStyle);
                featureLayer.setTitle(mapLayerInfo.getFeature().prefixedName());
                featureLayer.getUserData().put("abstract", mapLayerInfo.getDescription());

                // mix the dimension related filter with the layer filter
                Filter dimensionFilter =
                        buildDimensionFilter(times, elevations, mapLayerInfo, request);
                Filter filter =
                        SimplifyingFilterVisitor.simplify(
                                Filters.and(ff, layerFilter, dimensionFilter));

                final Query definitionQuery =
                        new Query(source.getSchema().getName().getLocalPart());
                definitionQuery.setVersion(featureVersion);
                definitionQuery.setFilter(filter);
                definitionQuery.setSortBy(layerSort);
                if (viewParams != null) {
                    definitionQuery.setHints(
                            new Hints(Hints.VIRTUAL_TABLE_PARAMETERS, viewParams.get(i)));
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
                        throw new ServiceException(
                                "startIndex is not supported for the "
                                        + mapLayerInfo.getName()
                                        + " layer");
                    }
                }

                int maxFeatures =
                        request.getMaxFeatures() != null
                                ? request.getMaxFeatures()
                                : Integer.MAX_VALUE;
                definitionQuery.setMaxFeatures(maxFeatures);

                featureLayer.setQuery(definitionQuery);
                mapContent.addLayer(featureLayer);

                layer = featureLayer;
            } else if (layerType == MapLayerInfo.TYPE_RASTER) {

                // /////////////////////////////////////////////////////////
                //
                // Adding a coverage layer
                //
                // /////////////////////////////////////////////////////////
                final GridCoverage2DReader reader =
                        (GridCoverage2DReader) mapLayerInfo.getCoverageReader();
                if (reader != null) {

                    // get the group of parameters tha this reader supports
                    GeneralParameterValue[] readParameters =
                            wms.getWMSReadParameters(
                                    request,
                                    mapLayerInfo,
                                    layerFilter,
                                    layerSort,
                                    times,
                                    elevations,
                                    reader,
                                    false);
                    try {

                        try {
                            layer = new CachedGridReaderLayer(reader, layerStyle, readParameters);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        layer.setTitle(mapLayerInfo.getCoverage().prefixedName());

                        mapContent.addLayer(layer);
                    } catch (IllegalArgumentException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    new StringBuilder("Wrapping GC in feature source: ")
                                            .append(e.getLocalizedMessage())
                                            .toString(),
                                    e);
                        }

                        throw new ServiceException(
                                "Internal error : unable to get reader for this coverage layer "
                                        + mapLayerInfo);
                    }
                } else {
                    throw new ServiceException(
                            new StringBuffer(
                                            "Internal error : unable to get reader for this coverage layer ")
                                    .append(mapLayerInfo.toString())
                                    .toString());
                }
            } else if (layerType == MapLayerInfo.TYPE_WMS) {
                WMSLayerInfo wmsLayer = (WMSLayerInfo) mapLayerInfo.getResource();
                if (!checkWMSLayerMinMaxScale(wmsLayer, mapContent.getScaleDenominator())) continue;
                WebMapServer wms = wmsLayer.getStore().getWebMapServer(null);
                Layer gt2Layer = wmsLayer.getWMSLayer(null);
                if (wmsLayer.isMetadataBBoxRespected()) {
                    boolean isInsideBounnds =
                            checkEnvelopOverLapWithNativeBounds(
                                    mapContent.getViewport().getBounds(),
                                    wmsLayer.getNativeBoundingBox());
                    if (!isInsideBounnds) {
                        if (LOGGER.isLoggable(Level.FINE))
                            LOGGER.fine(
                                    "Get Map Request BBOX is outside Layer "
                                            + request.getLayers().get(i).getName()
                                            + " metada BoundsIgnoring Layer,Ignoring");

                        continue;
                    }
                }

                // see if we can merge this layer with the previous one
                boolean merged = false;
                if (mapContent.layers().size() > 0) {
                    org.geotools.map.Layer lastLayer =
                            mapContent.layers().get(mapContent.layers().size() - 1);
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
                    WMSLayer Layer = null;

                    String style = request.getStyles().get(i).getName();
                    style = (style == null) ? wmsLayer.getForcedRemoteStyle() : style;
                    String imageFormat = request.getFormat();
                    // if passed style does not exist in remote, throw exception
                    if (!wmsLayer.isSelectedRemoteStyles(style))
                        throw new IllegalArgumentException(
                                "Unknown remote style "
                                        + style
                                        + " in cascaded layer "
                                        + wmsLayer.getName()
                                        + ", , re-configure the layer and WMS Store");

                    // if the format is not selected then fall back to preffered
                    if (!wmsLayer.isFormatValid(imageFormat))
                        imageFormat = wmsLayer.getPreferredFormat();

                    Layer = new WMSLayer(wms, gt2Layer, style, imageFormat);

                    Layer.setTitle(wmsLayer.prefixedName());
                    mapContent.addLayer(Layer);
                }
            } else if (layerType == MapLayerInfo.TYPE_WMTS) {
                WMTSLayerInfo wmtsLayer = (WMTSLayerInfo) mapLayerInfo.getResource();
                WebMapTileServer wmts = wmtsLayer.getStore().getWebMapTileServer(null);
                Layer gt2Layer = wmtsLayer.getWMTSLayer(null);

                WMTSMapLayer mapLayer = new WMTSMapLayer(wmts, gt2Layer);
                mapLayer.setTitle(wmtsLayer.prefixedName());

                mapLayer.setRawTime((String) Dispatcher.REQUEST.get().getRawKvp().get("time"));

                mapContent.addLayer(mapLayer);

            } else {
                throw new IllegalArgumentException("Unknown layer type " + layerType);
            }
        }

        RenderingVariables.setupEnvironmentVariables(mapContent);

        // set the buffer value if the admin has set a specific value for some layers
        // in this map
        // GR: question: does setupRenderingBuffer need EnvFunction.setLocalValues to be already
        // set? otherwise move this call out of the try block and above setLovalValues
        setupRenderingBuffer(mapContent, layers);

        // /////////////////////////////////////////////////////////
        //
        // Producing the map in the requested format.
        //
        // /////////////////////////////////////////////////////////
        mapContent = fireBeforeRender(mapContent);
        WebMap map = delegate.produceMap(mapContent);

        if (cachingPossible) {
            map.setResponseHeader("Cache-Control", "max-age=" + maxAge + ", must-revalidate");

            final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            calendar.add(Calendar.SECOND, maxAge);
            DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            map.setResponseHeader("Expires", format.format(calendar.getTime()));
        }

        return map;
    }

    protected Filter buildDimensionFilter(
            List<Object> times,
            List<Object> elevations,
            final MapLayerInfo mapLayerInfo,
            final GetMapRequest request)
            throws IOException {
        final Filter dimensionFilter =
                wms.getTimeElevationToFilter(times, elevations, mapLayerInfo.getFeature());
        final Filter customDimensionsfilter =
                wms.getDimensionsToFilter(request.getRawKvp(), mapLayerInfo.getFeature());
        return SimplifyingFilterVisitor.simplify(
                Filters.and(ff, dimensionFilter, customDimensionsfilter));
    }

    private void validateSort(
            FeatureSource<? extends FeatureType, ? extends Feature> source,
            SortBy[] sort,
            MapLayerInfo mapLayerInfo) {
        FeatureType ft = source.getSchema();
        for (SortBy sortBy : sort) {
            if (sortBy.getPropertyName().evaluate(ft) == null) {
                throw new ServiceException(
                        "Sort property '"
                                + sortBy.getPropertyName().getPropertyName()
                                + "' not available in "
                                + mapLayerInfo.getName(),
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "sortBy");
            }
        }
    }

    /**
     * Computes the rendering buffer in case the user did not specify one in the request, and the
     * admin setup some rendering buffer hints in the layer configurations
     */
    public static void setupRenderingBuffer(WMSMapContent map, List<MapLayerInfo> layers) {
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
            final double scaleDenominator = map.getScaleDenominator(true);
            int buffer = 0;

            // either use the preset buffer, or if missing, compute one on the fly based
            // on an analysis of the active rules at the current scale
            for (int i = 0; i < layers.size(); i++) {
                int layerBuffer = layerBuffers[i];
                if (layerBuffer == 0) {
                    layerBuffer =
                            computeLayerBuffer(map.layers().get(i).getStyle(), scaleDenominator);
                }
                if (layerBuffer > buffer) {
                    buffer = layerBuffer;
                }
            }

            map.setBuffer(buffer);
        }
    }

    /** Computes the rendering buffer for this layer */
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
     * Asserts the mandatory GetMap parameters have been provided.
     *
     * <p>With the exception of the SRS and STYLES parameters, for which default values are
     * assigned.
     *
     * @throws ServiceException if any mandatory parameter has not been set on the request
     */
    private void assertMandatory(GetMapRequest request) throws ServiceException {
        if (0 >= request.getWidth() || 0 >= request.getHeight()) {
            throw new ServiceException(
                    "Missing or invalid requested map size. Parameters"
                            + " WIDTH and HEIGHT shall be present and be integers > 0. Got "
                            + "WIDTH="
                            + request.getWidth()
                            + ", HEIGHT="
                            + request.getHeight(),
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
            throw new ServiceException(
                    "GetMap requests must include a BBOX parameter.", "MissingBBox");
        }
        if (env.isNull() || (env.getWidth() <= 0) || (env.getHeight() <= 0)) {
            throw new ServiceException(
                    new StringBuffer("The request bounding box has zero area: ")
                            .append(env)
                            .toString(),
                    "InvalidBBox");
        }
    }

    /**
     * Returns the list of filters resulting of combining the layers definition filters with the per
     * layer filters made by the user.
     *
     * <p>If <code>requestFilters != null</code>, it shall contain the same number of elements than
     * <code>layers</code>, as filters are requested one per layer.
     *
     * @param requestFilters the list of filters sent by the user, or <code>null</code>
     * @param layers the layers requested in the GetMap request, where to get the per layer
     *     definition filters from.
     * @return a list of filters, one per layer, resulting of anding the user requested filter and
     *     the layer definition filter
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
            if (layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR
                    || layer.getType() == MapLayerInfo.TYPE_RASTER) {
                combinedList[i] = userRequestedFilter;
            } else if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                layerDefinitionFilter = layer.getFeature().filter();

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
     * @param outputFormat a request parameter object wich holds the processed request objects, such
     *     as layers, bbox, outpu format, etc.
     * @return A specialization of <code>GetMapDelegate</code> wich can produce the requested output
     *     map format
     * @throws ServiceException if no specialization is configured for the output format specified
     *     in <code>request</code> or if it can't be instantiated or the format is not allowed
     */
    protected GetMapOutputFormat getDelegate(final String outputFormat) throws ServiceException {

        final GetMapOutputFormat producer = wms.getMapOutputFormat(outputFormat);
        if (producer == null) {
            ServiceException e =
                    new ServiceException(
                            "There is no support for creating maps in " + outputFormat + " format",
                            "InvalidFormat");
            e.setCode("InvalidFormat");
            throw e;
        }
        if (wms.isAllowedGetMapFormat(producer) == false) {
            throw wms.unallowedGetMapFormatException(outputFormat);
        }
        return producer;
    }

    private boolean checkWMSLayerMinMaxScale(WMSLayerInfo wmsLayerInfo, double mapScale) {

        // if none configured
        if (wmsLayerInfo.getMinScale() == null && wmsLayerInfo.getMaxScale() == null) return true;
        // return false map scale is below min
        if (wmsLayerInfo.getMinScale() != null && mapScale < wmsLayerInfo.getMinScale())
            return false;
        // return false map scale is above max
        if (wmsLayerInfo.getMaxScale() != null && mapScale > wmsLayerInfo.getMaxScale())
            return false;

        return true;
    }

    private boolean checkEnvelopOverLapWithNativeBounds(
            ReferencedEnvelope requestEnevelope, ReferencedEnvelope layerEnevelope) {

        try {
            // transform requested enevelope to resource`s native bounds
            ReferencedEnvelope transformedRequestEnv;
            transformedRequestEnv =
                    requestEnevelope.transform(layerEnevelope.getCoordinateReferenceSystem(), true);
            return !layerEnevelope.intersection(transformedRequestEnv).isEmpty();
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE, "Error in WMSLayerInfo.checkEnvelopOverLapWithNativeBounds", e);
        }
        return false;
    }
}
