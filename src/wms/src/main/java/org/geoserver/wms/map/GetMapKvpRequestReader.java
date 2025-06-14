/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.catalog.LayerGroupHelper.isSingleOrOpaque;
import static org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import javax.media.jai.Interpolation;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wms.CacheConfiguration;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSErrorCode;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geoserver.wms.clip.ClipWMSGetMapCallBack;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.NamedStyle;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.UserLayer;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.ows.URLCheckerException;
import org.geotools.data.ows.URLCheckers;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.renderer.style.StyleAttributeExtractor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.vfny.geoserver.util.Requests;
import org.vfny.geoserver.util.SLDValidator;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

public class GetMapKvpRequestReader extends KvpRequestReader implements DisposableBean {

    private static Map<String, Integer> interpolationMethods;

    static {
        interpolationMethods = new HashMap<>();
        interpolationMethods.put("NEAREST NEIGHBOR", Interpolation.INTERP_NEAREST);
        interpolationMethods.put("BILINEAR", Interpolation.INTERP_BILINEAR);
        interpolationMethods.put("BICUBIC", Interpolation.INTERP_BICUBIC);
    }

    /** filter factory */
    private FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    /** Flag to control wether styles shall be parsed. */
    private boolean parseStyles = true;

    /** The WMS configuration facade, that we use to pick up base layer definitions */
    private WMS wms;

    /**
     * EntityResolver provider, used in SLD parsing. Lazily created, only to be accessed through
     * {@link #getEntityResolverProvider()}
     */
    private EntityResolverProvider _entityResolverProvider;

    /**
     * HTTP client used to fetch remote styles. Lazily created, only to be accessed through {@link #getHttpClient} and
     * {@link #closeHttpClient}
     */
    private volatile CloseableHttpClient _httpClient;

    /**
     * Current WMS remote styles cache configuration. State managed by {@link #wmsCacheConfigListener} and
     * {@link #getCacheConfig()}
     */
    private CacheConfiguration cacheCfg;
    /**
     * This flags allows the kvp reader to go beyond the SLD library mode specification and match the first style that
     * can be applied to a given layer. This is for backwards compatibility
     */
    private boolean laxStyleMatchAllowed = true;

    private @Nullable HttpClientConnectionManager httpClientConnectionManager;

    private final ConfigurationListenerAdapter wmsCacheConfigListener = new ConfigurationListenerAdapter() {
        @Override
        public void handleServiceChange(
                ServiceInfo service, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {
            if (service instanceof WMSInfo) {
                WMSInfo info = (WMSInfo) service;
                CacheConfiguration newCacheCfg = info.getCacheConfiguration();
                if (cacheCfg != null && !newCacheCfg.equals(cacheCfg)) {
                    // close the client, wait for the next time it's needed to re-create it
                    try {
                        closeHttpClient();
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error closing HTTPClient", e);
                    }
                }
            }
        }
    };

    public GetMapKvpRequestReader(WMS wms) {
        this(wms, null);
        this.wms = wms;
    }

    public GetMapKvpRequestReader(WMS wms, HttpClientConnectionManager manager) {
        super(GetMapRequest.class);
        this.wms = wms;
        this.httpClientConnectionManager = manager;
    }

    private void addWmsCacheConfigListener() {
        GeoServer geoServer = wms.getGeoServer();
        geoServer.addListener(wmsCacheConfigListener);
    }

    private void removeWmsCacheConfigListener() {
        GeoServer geoServer = wms.getGeoServer();
        if (null != geoServer) {
            geoServer.removeListener(wmsCacheConfigListener);
        }
    }

    private CloseableHttpClient getHttpClient() {
        CloseableHttpClient client = this._httpClient;
        if (null == client) {
            synchronized (this) {
                if (null == this._httpClient) {
                    CacheConfiguration wmsCacheConfig = getCacheConfig();
                    RequestConfig requestConfig = createHttpRequestConfig();
                    this._httpClient = createHttpClient(requestConfig, wmsCacheConfig);
                    addWmsCacheConfigListener();
                    return this._httpClient;
                }
            }
        }
        return client;
    }

    private RequestConfig createHttpRequestConfig() {
        // configure the http client used to fetch remote styles
        Builder builder = RequestConfig.copy(RequestConfig.DEFAULT);
        int timeoutMillis = getTimeoutMillis();
        builder.setConnectTimeout(timeoutMillis);
        builder.setSocketTimeout(timeoutMillis);
        return builder.build();
    }

    private CacheConfiguration getCacheConfig() {
        if (null == this.cacheCfg) this.cacheCfg = wms.getRemoteResourcesCacheConfiguration();
        return this.cacheCfg;
    }

    EntityResolverProvider getEntityResolverProvider() {
        if (null == _entityResolverProvider)
            this._entityResolverProvider = new EntityResolverProvider(wms.getGeoServer());
        return this._entityResolverProvider;
    }

    private int getTimeoutMillis() {
        return wms.getServiceInfo().getRemoteStyleTimeout();
    }

    private synchronized CloseableHttpClient createHttpClient(
            RequestConfig requestConfig, CacheConfiguration wmsCacheConfig) {
        Objects.requireNonNull(requestConfig);
        Objects.requireNonNull(wmsCacheConfig);

        HttpClientBuilder builder;
        if (wmsCacheConfig.isEnabled()) {
            CacheConfig cacheConfig = CacheConfig.custom()
                    .setMaxCacheEntries(wmsCacheConfig.getMaxEntries())
                    .setMaxObjectSize(wmsCacheConfig.getMaxEntrySize())
                    .build();

            builder = CachingHttpClientBuilder.create()
                    .setCacheConfig(cacheConfig)
                    .setConnectionManager(httpClientConnectionManager)
                    .setDefaultRequestConfig(requestConfig);
        } else {
            builder = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
        }
        return builder.build();
    }

    private void closeHttpClient() throws IOException {
        synchronized (this) {
            if (this._httpClient != null) {
                // assign to a local variable, don't risk not being able to null-ify the instance
                // variable if close() fails
                @SuppressWarnings("PMD.CloseResource")
                CloseableHttpClient client = this._httpClient;
                this._httpClient = null;
                this.cacheCfg = null;
                removeWmsCacheConfigListener();
                client.close();
            }
        }
    }

    public void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    public boolean isParseStyle() {
        return parseStyles;
    }

    public void setParseStyle(boolean styleRequired) {
        this.parseStyles = styleRequired;
    }

    @Override
    public GetMapRequest createRequest() throws Exception {
        GetMapRequest request = new GetMapRequest();
        HttpServletRequest httpRequest = Optional.ofNullable(Dispatcher.REQUEST.get())
                .map(r -> r.getHttpRequest())
                .orElse(null);
        if (httpRequest != null) {
            request.setRequestCharset(httpRequest.getCharacterEncoding());
            request.setGet("GET".equalsIgnoreCase(httpRequest.getMethod()));
            List<String> headerNames = EnumerationUtils.toList(httpRequest.getHeaderNames());
            for (String headerName : headerNames) {
                request.putHttpRequestHeader(headerName, httpRequest.getHeader(headerName));
            }
        }
        return request;
    }

    /** Returns whether the specified resource must be skipped in the context of the current request. */
    protected boolean skipResource(Object theResource) {
        return false;
    }

    @Override
    public GetMapRequest read(Object request, Map<String, Object> kvp, Map<String, Object> rawKvp) throws Exception {
        GetMapRequest getMap = (GetMapRequest) super.read(request, kvp, rawKvp);
        // set the raw params used to create the request
        getMap.setRawKvp(KvpUtils.toStringKVP(rawKvp));

        boolean citeCompliant = wms.getServiceInfo().isCiteCompliant();

        // wms 1.3, srs changed to crs
        if (kvp.containsKey("crs")) {
            getMap.setSRS((String) kvp.get("crs"));
        } else if (citeCompliant && WMS.VERSION_1_3_0.equals(WMS.version(getMap.getVersion()))) {
            throw new ServiceException("GetMap CRS parameter is mandatory in WMS 1.3");
        }
        // do some additional checks

        if (citeCompliant && rawKvp != null && rawKvp.containsKey("transparent")) {
            String trans = (String) rawKvp.get("transparent");

            if (!trans.equalsIgnoreCase("false") && !trans.equalsIgnoreCase("true")) {
                throw new Exception("Invalid value of GetMap TRANSPARENT parameter, choose between true or false");
            }
        }

        // srs
        parseCRS(getMap);

        // remote OWS
        String remoteOwsType = getMap.getRemoteOwsType();
        remoteOwsType = remoteOwsType != null ? remoteOwsType.toUpperCase() : null;
        if (remoteOwsType != null && !"WFS".equals(remoteOwsType)) {
            throw new ServiceException("Unsupported remote OWS type '" + remoteOwsType + "'");
        }

        // remote OWS url
        URL remoteOwsUrl = getMap.getRemoteOwsURL();
        if (remoteOwsUrl != null && remoteOwsType == null) {
            throw new ServiceException("REMOTE_OWS_URL specified, but REMOTE_OWS_TYPE is missing");
        }

        final List<Object> requestedLayerInfos = new ArrayList<>();
        // layers
        String layerParam = (String) rawKvp.get("LAYERS");
        if (layerParam != null) {
            List<String> layerNames = KvpUtils.readFlat(layerParam);
            requestedLayerInfos.addAll(parseLayers(layerNames, remoteOwsUrl, remoteOwsType));
        } else if (citeCompliant && getMap.getSldBody() == null && getMap.getSld() == null) {
            // The SLD extensions to WMS allow a request not to have layers, as long as a full SLD
            // is specified either using &sld or &sld_body. The error must not be thrown in these
            // conditions (which are probably not what INSPIRE had in mind, but nonetheless a OGC
            // specification.
            throw new ServiceException("GetMap LAYERS parameter is mandatory if SLD nor SLD_BODY are not specified");
        }

        // raw styles parameter
        String stylesParam = (String) kvp.get("STYLES");
        List<String> styleNameList = new ArrayList<>();
        if (stylesParam != null) {
            styleNameList.addAll(KvpUtils.readFlat(stylesParam));
        } else if (citeCompliant && getMap.getSldBody() == null && getMap.getSld() == null) {
            // The SLD extensions to WMS allow a request not to have styles, as long as a full SLD
            // is specified either using &sld or &sld_body. The error must not be thrown in these
            // conditions (which are probably not what INSPIRE had in mind, but nonetheless a OGC
            // specification.
            throw new ServiceException("GetMap STYLES parameter is mandatory if SLD nor SLD_BODY are not specified");
        }

        // raw interpolations parameter
        String interpolationParam = (String) kvp.get("INTERPOLATIONS");
        List<String> interpolationList = new ArrayList<>();
        if (interpolationParam != null) {
            interpolationList.addAll(KvpUtils.readFlat(interpolationParam));
        }

        // raw filter and cql_filter parameters
        List<Filter> rawFilters =
                ((getMap.getFilter() != null) ? new ArrayList<>(getMap.getFilter()) : Collections.emptyList());
        List<Filter> cqlFilters =
                ((getMap.getCQLFilter() != null) ? new ArrayList<>(getMap.getCQLFilter()) : Collections.emptyList());
        List<List<SortBy>> rawSortBy = Optional.ofNullable(getMap.getSortBy()).orElse(Collections.emptyList());

        // remove skipped resources along with their corresponding parameters
        List<MapLayerInfo> newLayers = new ArrayList<>();
        for (int i = 0; i < requestedLayerInfos.size(); ) {
            Object o = requestedLayerInfos.get(i);
            if (skipResource(o)) {
                // remove the layer, style, interpolation, filter and cql_filter
                requestedLayerInfos.remove(i);
                if (i < styleNameList.size()) {
                    styleNameList.remove(i);
                }
                if (i < interpolationList.size()) {
                    interpolationList.remove(i);
                }
                if (i < rawFilters.size()) {
                    rawFilters.remove(i);
                }
                if (i < cqlFilters.size()) {
                    cqlFilters.remove(i);
                }
                if (i < rawSortBy.size()) {
                    rawSortBy.remove(i);
                }
            } else {
                if (o instanceof LayerInfo) {
                    newLayers.add(new MapLayerInfo((LayerInfo) o));
                } else if (o instanceof LayerGroupInfo) {
                    addGroupLayers(newLayers, (LayerGroupInfo) o, i, styleNameList);
                } else if (o instanceof MapLayerInfo) {
                    // it was a remote OWS layer, add it directly
                    newLayers.add((MapLayerInfo) o);
                }
                i++;
            }
        }
        getMap.setLayers(newLayers);

        if (!interpolationList.isEmpty()) {
            getMap.setInterpolations(parseInterpolations(requestedLayerInfos, interpolationList));
        }

        // pre parse filters
        List<Filter> filters = parseFilters(getMap, rawFilters, cqlFilters);
        List<List<SortBy>> sortBy = rawSortBy.isEmpty() ? null : rawSortBy;

        if ((getMap.getSldBody() != null || getMap.getSld() != null) && wms.isDynamicStylingDisabled()) {
            throw new ServiceException("Dynamic style usage is forbidden");
        }

        // styles
        // process SLD_BODY, SLD, then STYLES parameter
        if (getMap.getSldBody() != null) {
            processSLDBody(getMap, requestedLayerInfos, styleNameList, filters, sortBy);
        } else if (getMap.getSld() != null) {
            processSLDLink(getMap, requestedLayerInfos, styleNameList, filters, sortBy);
        } else {
            processLayersStyles(getMap, requestedLayerInfos, styleNameList, filters, sortBy);
        }

        // check the view params
        List<Map<String, String>> viewParams = getMap.getViewParams();
        if (viewParams != null && !viewParams.isEmpty()) {
            applyViewParams(getMap, viewParams, requestedLayerInfos);
        }

        // check if layers have time/elevation support
        boolean hasTime = false;
        boolean hasElevation = false;
        for (MapLayerInfo layer : getMap.getLayers()) {
            if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                MetadataMap metadata = layer.getResource().getMetadata();
                DimensionInfo elevationInfo = metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
                hasElevation |= elevationInfo != null && elevationInfo.isEnabled();
                DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
                hasTime |= timeInfo != null && timeInfo.isEnabled();
            } else if (layer.getType() == MapLayerInfo.TYPE_RASTER) {
                MetadataMap metadata = layer.getResource().getMetadata();
                //
                // Adding a coverage layer
                //
                GridCoverage2DReader reader;
                try {
                    reader = (GridCoverage2DReader) layer.getCoverageReader();
                } catch (IOException e) {
                    throw new ServiceException(e);
                }
                if (reader != null) {
                    ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
                    DimensionInfo elevationInfo = metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
                    hasElevation |= elevationInfo != null && elevationInfo.isEnabled() && dimensions.hasElevation();
                    DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
                    hasTime |= timeInfo != null && timeInfo.isEnabled() && dimensions.hasTime();
                }
            }
        }

        // force in the default if nothing was requested
        if (hasTime && (getMap.getTime() == null || getMap.getTime().isEmpty())) {
            // ask for "CURRENT"
            getMap.setTime(Arrays.asList((Object) null));
        }
        if (hasElevation
                && (getMap.getElevation() == null || getMap.getElevation().isEmpty())) {
            // ask for "DEFAULT"
            getMap.setElevation(Arrays.asList((Object) null));
        }

        // check that we don't have double dimensions listing
        if ((getMap.getElevation() != null && getMap.getElevation().size() > 1)
                && (getMap.getTime() != null && getMap.getTime().size() > 1)) {
            throw new ServiceException("TIME and ELEVATION values cannot be both multivalued");
        }

        if (rawKvp.get("clip") != null) {
            getMap.setClip(getClipGeometry(getMap));
        }

        return getMap;
    }

    private void addGroupLayers(
            List<MapLayerInfo> newLayers, LayerGroupInfo lg, int index, List<String> requestedStyles) {
        if (index < requestedStyles.size()) {
            String style = requestedStyles.get(index);
            addLayersFromGroup(lg, newLayers, style);
        } else {
            addLayersFromGroup(lg, newLayers, null);
        }
    }

    private void addLayersFromGroup(LayerGroupInfo groupInfo, List<MapLayerInfo> newLayers, String styleName) {
        List<LayerInfo> layers;
        boolean isDefault = isDefaultLgStyle(styleName, groupInfo);
        if (!isDefault && isGroupStyleName(styleName, groupInfo)) {
            layers = groupInfo.layers(styleName);
        } else {
            layers = groupInfo.layers();
        }
        if (layers != null && !layers.isEmpty()) {
            for (LayerInfo l : layers) {
                newLayers.add(new MapLayerInfo(l, groupInfo.getMetadata()));
            }
        }
    }

    private void applyViewParams(
            GetMapRequest getMap, List<Map<String, String>> viewParams, List<Object> requestedLayerInfos) {
        int layerCount = getMap.getLayers().size();
        if (viewParams.size() == layerCount) return;

        List<Map<String, String>> replacement = new ArrayList<>();
        if (viewParams.size() == 1 && layerCount > 1) {
            // if we have just one replicate over all layers
            for (int i = 0; i < layerCount; i++) {
                replacement.add(viewParams.get(0));
            }
        } else {
            // expand based on group/layer/other
            for (int i = 0; i < requestedLayerInfos.size(); i++) {
                Object o = requestedLayerInfos.get(i);
                Map<String, String> layerParams = viewParams.get(i);
                if (o instanceof LayerGroupInfo) {
                    LayerGroupInfo groupInfo = (LayerGroupInfo) o;
                    List<LayerInfo> layers = groupInfo.layers();
                    if (layers != null) layers.stream().forEach(l -> replacement.add(layerParams));
                } else {
                    replacement.add(layerParams);
                }
            }
        }
        getMap.setViewParams(replacement);

        // final check, did we re-align? otherwiser report based on original list,
        // that is, what the user actually provided
        if (replacement.size() != layerCount) {
            String msg = layerCount + " layers requested, but found " + viewParams.size() + " view params specified. ";
            throw new ServiceException(msg, getClass().getName());
        }

        // update view params

    }

    private void processLayersStyles(
            GetMapRequest getMap,
            List<Object> requestedLayerInfos,
            List<String> styleNameList,
            List<Filter> filters,
            List<List<SortBy>> sortBy)
            throws Exception {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting layers and styles from LAYERS and STYLES");
        }

        // different List for LayerGroup style name since the list
        // for layers use the Style type while for LayerGroup we have named configurations.
        // The size of this list is kept in sync with the Style one adding null for empty
        // style names or for layers' styles.
        List<String> groupStyleNames = new ArrayList<>();
        // ok, parse the styles parameter in isolation
        if (!styleNameList.isEmpty()) {
            List<Style> parsedStyle = new ArrayList<>();
            parseStyles(styleNameList, requestedLayerInfos, parsedStyle, groupStyleNames);
            getMap.setStyles(parsedStyle);
        }

        // first, expand base layers and default styles
        if (isParseStyle() && !requestedLayerInfos.isEmpty()) {
            List<Style> oldStyles =
                    getMap.getStyles() != null ? new ArrayList<>(getMap.getStyles()) : new ArrayList<>();
            List<Style> newStyles = new ArrayList<>();
            List<Filter> newFilters = filters == null ? null : new ArrayList<>();
            List<List<SortBy>> newSortBy = sortBy == null ? null : new ArrayList<>();
            for (int i = 0; i < requestedLayerInfos.size(); i++) {
                Object o = requestedLayerInfos.get(i);
                Style style = oldStyles.isEmpty() ? null : oldStyles.get(i);

                if (o instanceof LayerGroupInfo) {
                    LayerGroupInfo groupInfo = (LayerGroupInfo) o;
                    resolveLayerGroup(i, groupStyleNames, groupInfo, newStyles, newFilters, newSortBy, filters, sortBy);
                } else if (o instanceof LayerInfo) {
                    style = oldStyles.isEmpty() ? null : oldStyles.get(i);
                    if (style != null) {
                        newStyles.add(style);
                    } else {
                        LayerInfo layer = (LayerInfo) o;
                        newStyles.add(getDefaultStyle(layer));
                    }
                    // add filter if needed
                    if (filters != null) {
                        newFilters.add(getFilter(filters, i));
                    }
                    if (sortBy != null) {
                        newSortBy.add(getSortBy(sortBy, i));
                    }
                } else if (o instanceof MapLayerInfo) {
                    style = oldStyles.isEmpty() ? null : oldStyles.get(i);
                    if (style != null) {
                        newStyles.add(style);
                    } else {
                        throw new ServiceException(
                                "no style requested for layer " + ((MapLayerInfo) o).getName(), "NoDefaultStyle");
                    }
                    // add filter if needed
                    if (filters != null) {
                        newFilters.add(getFilter(filters, i));
                    }
                    if (sortBy != null) {
                        newSortBy.add(getSortBy(sortBy, i));
                    }
                }
            }
            getMap.setStyles(newStyles);
            if (newFilters != null
                    && !newFilters.isEmpty()
                    && getMap.getCQLFilter() != null
                    && !getMap.getCQLFilter().isEmpty()) {
                getMap.setCQLFilter(newFilters);
            }
            getMap.setFilter(newFilters);
            getMap.setSortBy(newSortBy);
        }

        // then proceed with standard processing
        List<MapLayerInfo> layers = getMap.getLayers();
        if (isParseStyle() && (layers != null) && (!layers.isEmpty())) {
            final List styles = getMap.getStyles();

            if (layers.size() != styles.size()) {
                String msg = layers.size() + " layers requested, but found " + styles.size() + " styles specified. ";
                throw new ServiceException(msg, getClass().getName());
            }

            for (int i = 0; i < styles.size(); i++) {
                Style currStyle = getMap.getStyles().get(i);
                if (currStyle == null)
                    throw new ServiceException(
                            "Could not find a style for layer "
                                    + getMap.getLayers().get(i).getName()
                                    + ", either none was specified or no default style is available for it",
                            "NoDefaultStyle");
                checkStyle(currStyle, layers.get(i));
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(new StringBuffer("establishing ")
                            .append(currStyle.getName())
                            .append(" style for ")
                            .append(layers.get(i).getName())
                            .toString());
                }
            }
        }

        // check filter size matches with the layer list size
        List mapFilters = getMap.getFilter();
        List<MapLayerInfo> mapLayers = getMap.getLayers();
        if (mapFilters != null && mapFilters.size() != mapLayers.size()) {
            String msg =
                    mapLayers.size() + " layers requested, but found " + mapFilters.size() + " filters specified. ";
            throw new ServiceException(msg, getClass().getName());
        }
        // do the same with sortBy
        List<List<SortBy>> mapSortBy = getMap.getSortBy();
        if (mapSortBy != null && mapSortBy.size() != mapLayers.size()) {
            String msg = mapLayers.size() + " layers requested, but found " + mapSortBy.size() + " sortBy specified. ";
            throw new ServiceException(msg, getClass().getName());
        }
    }

    // add to newStyles, newFilters and newSortBy lists values according to the request lg style.
    private void resolveLayerGroup(
            int i,
            List<String> groupStyleNames,
            LayerGroupInfo groupInfo,
            List<Style> newStyles,
            List<Filter> newFilters,
            List<List<SortBy>> newSortBy,
            List<Filter> filters,
            List<List<SortBy>> sortBy)
            throws IOException {
        List<LayerInfo> layers = null;
        List<StyleInfo> styles = null;
        if (!groupStyleNames.isEmpty()) {
            String styleName = groupStyleNames.get(i);
            if (!isDefaultLgStyle(styleName, groupInfo) && isGroupStyleName(styleName, groupInfo)) {
                layers = groupInfo.layers(styleName);
                styles = groupInfo.styles(styleName);
            }
        }
        if (layers == null) {
            layers = groupInfo.layers();
            styles = groupInfo.styles();
        }
        for (int j = 0; j < styles.size(); j++) {
            StyleInfo si = styles.get(j);
            if (si != null) {
                newStyles.add(si.getStyle());
            } else {
                LayerInfo layer = layers.get(j);
                newStyles.add(getDefaultStyle(layer));
            }
        }
        // expand the filter on the layer group to all its sublayers
        if (filters != null) {
            int j = 0;
            while (j < layers.size()) {
                newFilters.add(getFilter(filters, i));
                j++;
            }
        }
        if (sortBy != null) {
            int j = 0;
            while (j < layers.size()) {
                newSortBy.add(getSortBy(sortBy, i));
                j++;
            }
        }
    }

    private void processSLDLink(
            GetMapRequest getMap,
            List<Object> requestedLayerInfos,
            List<String> styleNameList,
            List<Filter> filters,
            List<List<SortBy>> sortBy)
            throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting layers and styles from reomte SLD");
        }

        // check we can actually connect to server, throw an exception with locator otherwise
        try {
            URLCheckers.confirm(getMap.getSld());
        } catch (URLCheckerException e) {
            throw new ServiceException("Invalid SLD URL: " + getMap.getSld(), e, INVALID_PARAMETER_VALUE, "sld");
        }

        try (InputStream input = getStream(getMap)) {
            if (input != null) {
                try (InputStreamReader reader = new InputStreamReader(input)) {
                    if (getMap.getValidateSchema().booleanValue()) {
                        List<Exception> errors = validateStyle(input, getMap);
                        if ((errors != null) && (!errors.isEmpty())) {
                            throw new ServiceException(SLDValidator.getErrorMessage(input, errors));
                        }
                    }

                    StyledLayerDescriptor sld = parseStyle(getMap, reader);
                    processSld(getMap, requestedLayerInfos, sld, styleNameList);
                } catch (Exception ex) {
                    final Level l = Level.WARNING;
                    // KMS: Kludge here to allow through certain exceptions without being
                    // hidden.
                    if (ex.getCause() instanceof SAXException) {
                        if (ex.getCause().getMessage().contains("Entity resolution disallowed")) {
                            throw ex;
                        }
                    }
                    LOGGER.log(l, "Exception while getting SLD.", ex);
                    // KMS: Replace with a generic exception so it can't be used to port scan
                    // the local network.
                    throw new ServiceException("Error while getting SLD.");
                }
            }
        }
        // set filter in, we'll check consistency later
        getMap.setFilter(filters);
        getMap.setSortBy(sortBy);
    }

    private void processSLDBody(
            GetMapRequest getMap,
            List<Object> requestedLayerInfos,
            List<String> styleNameList,
            List<Filter> filters,
            List<List<SortBy>> sortBy)
            throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting layers and styles from SLD_BODY");
        }

        if (getMap.getValidateSchema().booleanValue()) {
            try (StringReader reader = new StringReader(getMap.getSldBody())) {
                List<Exception> errors = validateStyle(reader, getMap);

                if (!errors.isEmpty()) {
                    throw new ServiceException(
                            SLDValidator.getErrorMessage(new StringReader(getMap.getSldBody()), errors));
                }
            }
        }

        try (StringReader input = new StringReader(getMap.getSldBody())) {
            StyledLayerDescriptor sld = parseStyle(getMap, input);
            processSld(getMap, requestedLayerInfos, sld, styleNameList);
        }

        // set filter in, we'll check consistency later
        getMap.setFilter(filters);
        getMap.setSortBy(sortBy);
    }

    private void parseCRS(GetMapRequest getMap) {
        String srs = getMap.getSRS();
        srs = WMS.toInternalSRS(srs, WMS.version(getMap.getVersion()));
        getMap.setSRS(srs);

        if (srs != null) {
            try {
                // set the crs as well
                CoordinateReferenceSystem mapcrs = CRS.decode(srs);
                getMap.setCrs(mapcrs);
            } catch (Exception e) {
                // couldnt make it - we send off a service exception with the
                // correct info
                throw new ServiceException(
                        "Error occurred decoding the espg code " + srs,
                        e,
                        WMSErrorCode.INVALID_CRS.get(getMap.getVersion()));
            }
        }
    }

    private InputStream getStream(GetMapRequest getMap) throws IOException {
        URL styleUrl = getMap.getStyleUrl().toURL();

        if (styleUrl.getProtocol().toLowerCase().indexOf("http") == 0) {
            return getHttpInputStream(styleUrl, getMap.getHttpRequestHeader("Authorization"));
        } else {
            try {
                return Requests.getInputStream(styleUrl);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Exception while getting SLD.", ex);
                // KMS: Replace with a generic exception so it can't be used to port scan the
                // local
                // network.
                throw new ServiceException("Error while getting SLD.");
            }
        }
    }

    private InputStream getHttpInputStream(URL styleUrl, String authorizationHeader) throws IOException {
        InputStream input = null;
        HttpCacheContext cacheContext = HttpCacheContext.create();

        HttpGet httpget = new HttpGet(styleUrl.toExternalForm());
        if (StringUtils.isNotBlank(authorizationHeader) && isAllowedURL(styleUrl)) {
            httpget.addHeader("Authorization", authorizationHeader);
        }
        try (CloseableHttpResponse response = executeRequest(cacheContext, httpget)) {
            if (cacheContext != null) {
                CacheResponseStatus responseStatus = cacheContext.getCacheResponseStatus();
                if (responseStatus != null) {
                    switch (responseStatus) {
                        case CACHE_HIT:
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine(
                                        "A response was generated from the cache with " + "no requests sent upstream");
                            }
                            break;
                        case CACHE_MODULE_RESPONSE:
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("The response was generated directly by the " + "caching module");
                            }
                            break;
                        case CACHE_MISS:
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("The response came from an upstream server");
                            }
                            break;
                        case VALIDATED:
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("The response was generated from the cache "
                                        + "after validating the entry with the origin server");
                            }
                            break;
                    }
                }
            }
            input = response.getEntity().getContent();
            ByteArrayInputStream styleData = new ByteArrayInputStream(IOUtils.toByteArray(input));
            input.close();
            input = styleData;
            input.reset();
            return input;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Exception while getting SLD.", ex);
            // KMS: Replace with a generic exception so it can't be used to port scan the
            // local network.
            throw new ServiceException("Error while getting SLD.");
        }
    }

    private boolean isAllowedURL(URL styleUrl) {
        String url = styleUrl.toString();
        return wms.getAllowedURLsForAuthForwarding().stream().anyMatch(s -> url.startsWith(s));
    }

    /** Executes the HTTP request with the max request time settings. */
    protected CloseableHttpResponse executeRequest(HttpCacheContext cacheContext, HttpGet httpget)
            throws IOException, ClientProtocolException {
        // get the max request time from WMS settings
        int hardTimeout = wms.getServiceInfo().getRemoteStyleMaxRequestTime();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (httpget != null) {
                    httpget.abort();
                }
            }
        };
        new Timer(true).schedule(task, hardTimeout);
        return getHttpClient().execute(httpget, cacheContext);
    }

    private List<Interpolation> parseInterpolations(List<Object> requestedLayers, List<String> interpolationList) {
        List<Interpolation> interpolations = new ArrayList<>();
        for (int i = 0; i < requestedLayers.size(); i++) {
            // null interpolation means:
            // use the default WMS one
            Interpolation interpolation = null;
            if (i < interpolationList.size()) {
                String interpolationName = interpolationList.get(i);
                if (!interpolationName.trim().equals("")) {
                    interpolation = getInterpolationObject(interpolationName);
                }
            }
            Object o = requestedLayers.get(i);
            if (o instanceof LayerInfo) {
                interpolations.add(interpolation);
            } else if (o instanceof LayerGroupInfo) {
                List<LayerInfo> subLayers = ((LayerGroupInfo) o).layers();
                interpolations.addAll(Collections.nCopies(subLayers.size(), interpolation));
            } else {
                throw new IllegalArgumentException("Unknown layer info type: " + o);
            }
        }

        return interpolations;
    }

    private Interpolation getInterpolationObject(String interpolation) {
        return Interpolation.getInstance(interpolationMethods.get(interpolation.toUpperCase()));
    }

    private Style getDefaultStyle(LayerInfo layer) throws IOException {
        if (layer.getResource() instanceof WMSLayerInfo) {
            // NamedStyle is a subclass of Style -> we use it as a way to convey
            // cascaded WMS layer styles
            NamedStyle namedStyle = CommonFactoryFinder.getStyleFactory(null).createNamedStyle();
            namedStyle.setName(null);
            return namedStyle;
        } else {
            StyleInfo defaultStyle = layer.getDefaultStyle();
            return defaultStyle.getStyle();
        }
    }

    Filter getFilter(List<Filter> filters, int index) {
        if (filters.size() == 1 && filters.get(0) instanceof Id) {
            // feature id filters must be expanded to all layers
            return filters.get(0);
        } else if (index < filters.size()) {
            return filters.get(index);
        } else {
            throw new ServiceException(
                    "Layers and filters are mismatched, " + "you need to provide one filter for each layer");
        }
    }

    List<SortBy> getSortBy(List<List<SortBy>> items, int index) {
        if (index < items.size()) {
            return items.get(index);
        } else {
            throw new ServiceException(
                    "Layers and sortBy are mismatched, " + "you need to provide one sortBy for each layer");
        }
    }

    /**
     * Checks the various options, OGC filter, fid filter, CQL filter, and returns a list of parsed filters
     *
     * @return the list of parsed filters, or null if none was found
     */
    private List<Filter> parseFilters(GetMapRequest getMap, List<Filter> rawFilters, List<Filter> cqlFilters) {
        List<Filter> filters = rawFilters;
        List featureId = (getMap.getFeatureId() != null) ? getMap.getFeatureId() : Collections.emptyList();

        if (!featureId.isEmpty()) {
            if (!filters.isEmpty()) {
                throw new ServiceException("GetMap KVP request contained "
                        + "conflicting filters.  Filter: "
                        + rawFilters
                        + ", fid: "
                        + featureId);
            }

            Set<FeatureId> ids = new HashSet<>();
            for (Object o : featureId) {
                ids.add(filterFactory.featureId((String) o));
            }
            filters = Collections.singletonList(filterFactory.id(ids));
        }

        if (!cqlFilters.isEmpty()) {
            if (!filters.isEmpty()) {
                throw new ServiceException("GetMap KVP request contained "
                        + "conflicting filters.  Filter: "
                        + rawFilters
                        + ", fid: "
                        + featureId
                        + ", cql: "
                        + cqlFilters);
            }

            filters = cqlFilters;
        }

        // return null in case we found no filters
        if (filters.isEmpty()) {
            filters = null;
        }
        return filters;
    }

    /** validates an style document. */
    private List<Exception> validateStyle(Object input, GetMapRequest getMap) {
        try {
            String language = getStyleFormat(getMap);
            EntityResolverProvider entityResolverProvider = getEntityResolverProvider();
            EntityResolver entityResolver = entityResolverProvider.getEntityResolver();

            return Styles.handler(language).validate(input, getMap.styleVersion(), entityResolver);
        } catch (IOException e) {
            throw new ServiceException("Error validating style", e);
        }
    }

    /** Parses an style document. */
    private StyledLayerDescriptor parseStyle(GetMapRequest getMap, Reader reader) {
        try {
            String format = getStyleFormat(getMap);
            EntityResolverProvider entityResolverProvider = getEntityResolverProvider();
            EntityResolver entityResolver = entityResolverProvider.getEntityResolver();

            return Styles.handler(format).parse(reader, getMap.styleVersion(), null, entityResolver);
        } catch (IOException e) {
            throw new ServiceException("Error parsing style", e);
        }
    }

    /*
     * Get style language from request, falling back on SLD as default.
     */
    private String getStyleFormat(GetMapRequest request) {
        return request.getStyleFormat() != null ? request.getStyleFormat() : SLDHandler.FORMAT;
    }

    private void processSld(
            final GetMapRequest request,
            final List<?> requestedLayers,
            final StyledLayerDescriptor sld,
            final List<String> styleNames)
            throws ServiceException, IOException {
        if (requestedLayers.isEmpty()) {
            sld.accept(new ProcessStandaloneSLDVisitor(wms, request));
        } else {
            processLibrarySld(request, sld, requestedLayers, styleNames);
        }
    }

    /**
     * Looks in <code>sld</code> for the layers and styles to use in the map composition and sets them to the <code>
     * request</code>
     *
     * <p>This method processes SLD in library mode Library mode engages when "SLD" or "SLD_BODY" are used in conjuction
     * with LAYERS and STYLES. From the spec: <br>
     * <cite> When an SLD is used as a style library, the STYLES CGI parameter is interpreted in the usual way in the
     * GetMap request, except that the handling of the style names is organized so that the styles defined in the SLD
     * take precedence over the named styles stored within the map server. The user-defined SLD styles can be given
     * names and they can be marked as being the default style for a layer. To be more specific, if a style named
     * �CenterLine� is referenced for a layer and a style with that name is defined for the corresponding layer in the
     * SLD, then the SLD style definition is used. Otherwise, the standard named-style mechanism built into the map
     * server is used. If the use of a default style is specified and a style is marked as being the default for the
     * corresponding layer in the SLD, then the default style from the SLD is used; otherwise, the standard default
     * style in the map server is used. </cite>
     *
     * @param request the GetMap request to which to set the layers and styles
     * @param sld a SLD document to take layers and styles from, following the "literal" or "library" rule.
     * @param requestedLayers the list of {@link LayerInfo} and {@link LayerGroupInfo} as requested by the LAYERS param
     * @param styleNames the list of requested style names
     */
    private void processLibrarySld(
            final GetMapRequest request,
            final StyledLayerDescriptor sld,
            final List<?> requestedLayers,
            List<String> styleNames)
            throws ServiceException, IOException {
        final StyledLayer[] styledLayers = sld.getStyledLayers();
        final int slCount = styledLayers.length;

        if (slCount == 0) {
            throw new ServiceException("SLD document contains no layers");
        }

        final List<MapLayerInfo> layers = new ArrayList<>();
        final List<Style> styles = new ArrayList<>();

        MapLayerInfo currLayer = null;
        String styleName = null;

        for (int i = 0; i < requestedLayers.size(); i++) {
            if (styleNames != null && !styleNames.isEmpty()) {
                styleName = styleNames.get(i);
            }
            Object o = requestedLayers.get(i);
            if (o instanceof LayerInfo) {
                currLayer = new MapLayerInfo((LayerInfo) o);

                if (styledLayers[i] instanceof NamedLayer) {
                    NamedLayer namedLayer = ((NamedLayer) styledLayers[i]);
                    currLayer.setLayerFeatureConstraints(namedLayer.getLayerFeatureConstraints());
                }

                layers.add(currLayer);
                Style style = findStyleOf(request, currLayer, styleName, styledLayers);
                styles.add(style);
            } else if (o instanceof LayerGroupInfo) {
                List<LayerInfo> subLayers = ((LayerGroupInfo) o).layers();
                for (LayerInfo layer : subLayers) {
                    currLayer = new MapLayerInfo(layer, ((LayerGroupInfo) o).getMetadata());
                    layers.add(currLayer);
                    Style style = findStyleOf(request, currLayer, styleName, styledLayers);
                    styles.add(style);
                }
            } else {
                throw new IllegalArgumentException("Unknown layer info type: " + o);
            }
        }

        request.setLayers(layers);
        request.setStyles(styles);
    }

    /**
     * the correct thing to do its grab the style from styledLayers[i] inside the styledLayers[i] will either be : a)
     * nothing - in which case grab the layer's default style b) a set of: i) NameStyle -- grab it from the pre-loaded
     * styles ii)UserStyle -- grab it from the sld the user uploaded
     *
     * <p>NOTE: we're going to get a set of layer->style pairs for (b). these are added to layers,styles
     *
     * <p>NOTE: we also handle some featuretypeconstraints
     */
    public static void addStyles(
            WMS wms,
            GetMapRequest request,
            MapLayerInfo currLayer,
            StyledLayer layer,
            List<MapLayerInfo> layers,
            List<Style> styles)
            throws ServiceException, IOException {
        if (currLayer == null) {
            return; // protection
        }
        Style[] layerStyles = null;

        if (layer instanceof NamedLayer) {
            layerStyles = ((NamedLayer) layer).getStyles();
        } else if (layer instanceof UserLayer) {
            layerStyles = ((UserLayer) layer).getUserStyles();
        }

        // handle no styles -- use default
        if ((layerStyles == null) || (layerStyles.length == 0)) {
            layers.add(currLayer);
            styles.add(currLayer.getDefaultStyle());

            return;
        }

        Style s;
        for (Style layerStyle : layerStyles) {
            if (layerStyle instanceof NamedStyle) {
                layers.add(currLayer);
                s = findStyle(wms, request, layerStyle.getName());

                if (s == null) {
                    throw new ServiceException("couldn't find style named '" + layerStyle.getName() + "'");
                }

                styles.add(s);
            } else {
                layers.add(currLayer);
                styles.add(layerStyle);
            }
        }
    }

    /**
     * @return the configured style named <code>currStyleName</code> or <code>null</code> if such a style does not exist
     *     on this server.
     */
    private static Style findStyle(final WMS wms, GetMapRequest request, String currStyleName) throws IOException {
        // Style currStyle;
        // Map configuredStyles = request.getWMS().getData().getStyles();
        //
        // currStyle = (Style) configuredStyles.get(currStyleName);
        //
        // return currStyle;
        return wms.getStyleByName(currStyleName);
    }

    /**
     * Finds the style for <code>layer</code> in <code>styledLayers</code> or the layer's default style if <code>
     * styledLayers</code> has no a UserLayer or a NamedLayer with the same name than <code>layer</code>
     *
     * <p>This method is used to parse the style of a layer for SLD and SLD_BODY parameters, both in library and literal
     * mode. Thus, once the declared style for the given layer is found, it is checked for validity of appliance for
     * that layer (i.e., whether the featuretype contains the attributes needed for executing the style filters).
     *
     * @param request used to find out an internally configured style when referenced by name by a NamedLayer
     * @param layer one of the layers that was requested through the LAYERS parameter or through and SLD document when
     *     the request is in literal mode.
     * @param styledLayers a set of StyledLayers from where to find the SLD layer with the same name as <code>layer
     *     </code> and extract the style to apply.
     * @return the Style applicable to <code>layer</code> extracted from <code>styledLayers</code>.
     * @throws RuntimeException if one of the StyledLayers is neither a UserLayer nor a NamedLayer. This shuoldn't
     *     happen, since the only allowed subinterfaces of StyledLayer are NamedLayer and UserLayer.
     */
    private Style findStyleOf(GetMapRequest request, MapLayerInfo layer, String styleName, StyledLayer[] styledLayers)
            throws ServiceException, IOException {
        Style style = null;
        String layerName = layer.getName();
        StyledLayer sl;

        for (StyledLayer value : styledLayers) {
            sl = value;

            if (layerName.equals(sl.getName())) {
                if (sl instanceof UserLayer) {
                    Style[] styles = ((UserLayer) sl).getUserStyles();

                    // if the style name has not been specified, look it up
                    // the default style, otherwise lookup the one requested
                    for (int j = 0; style == null && styles != null && j < styles.length; j++) {
                        if (styleName == null || styleName.equals("") && styles[j].isDefault()) style = styles[j];
                        else if (styleName != null && styleName.equals(styles[j].getName())) style = styles[j];
                    }
                } else if (sl instanceof NamedLayer) {
                    Style[] styles = ((NamedLayer) sl).getStyles();

                    // if the style name has not been specified, look it up
                    // the default style, otherwise lookup the one requested
                    for (int j = 0; style == null && styles != null && j < styles.length; j++) {
                        if ((styleName == null || styleName.equals("")) && styles[j].isDefault()) style = styles[j];
                        else if (styleName != null && styleName.equals(styles[j].getName())) style = styles[j];
                    }

                    if (style instanceof NamedStyle) {
                        style = findStyle(wms, request, style.getName());
                    }
                } else {
                    throw new RuntimeException("Unknown layer type: " + sl);
                }

                break;
            }
        }

        // fallback on the old GeoServer behaviour, if the style is not found find
        // the first style that matches the type name
        // TODO: would be nice to have a switch to turn this off since it's out of the spec
        if (style == null && laxStyleMatchAllowed) {
            for (StyledLayer styledLayer : styledLayers) {
                sl = styledLayer;

                if (layerName.equals(sl.getName())) {
                    if (sl instanceof UserLayer) {
                        Style[] styles = ((UserLayer) sl).getUserStyles();

                        if ((null != styles) && (0 < styles.length)) {
                            style = styles[0];
                        }
                    } else if (sl instanceof NamedLayer) {
                        Style[] styles = ((NamedLayer) sl).getStyles();

                        if ((null != styles) && (0 < styles.length)) {
                            style = styles[0];
                        }

                        if (style instanceof NamedStyle) {
                            style = findStyle(wms, request, style.getName());
                        }
                    } else {
                        throw new RuntimeException("Unknown layer type: " + sl);
                    }

                    break;
                }
            }
        }

        // still not found? Fall back on the server default ones
        if (style == null) {
            if (styleName == null || "".equals(styleName)) {
                style = layer.getDefaultStyle();
                if (style == null) throw new ServiceException("Could not find a default style for " + layer.getName());
            } else {
                style = wms.getStyleByName(styleName);
                if (style == null) {
                    String msg = "No such style: " + styleName;
                    throw new ServiceException(msg, "StyleNotDefined");
                }
            }
        }

        checkStyle(style, layer);

        return style;
    }

    /**
     * Checks to make sure that the style passed in can process the FeatureType.
     *
     * @param style The style to check
     * @param mapLayerInfo The source requested.
     */
    private static void checkStyle(Style style, MapLayerInfo mapLayerInfo) throws ServiceException {
        if (mapLayerInfo.getType() == mapLayerInfo.TYPE_RASTER) {
            // REVISIT: hey, don't we have to check it for rasters now that we support raster
            // symbolizer?
            return;
        }
        // if a rendering transform is present don't check the attributes, since they may be changed
        if (hasTransformation(style)) return;

        // extract attributes used in the style
        StyleAttributeExtractor sae = new StyleAttributeExtractor();
        sae.visit(style);
        Set<PropertyName> styleAttributes = sae.getAttributes();

        // see if we can collect any attribute out of the provided layer
        // Set attributes = new HashSet();
        FeatureType type = null;
        if (mapLayerInfo.getType() == MapLayerInfo.TYPE_VECTOR
                || mapLayerInfo.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
            try {
                if (mapLayerInfo.getType() == MapLayerInfo.TYPE_VECTOR)
                    type = mapLayerInfo.getFeature().getFeatureType();
                else type = mapLayerInfo.getRemoteFeatureSource().getSchema();
            } catch (IOException ioe) {
                throw new RuntimeException("Error getting FeatureType, this should never happen!", ioe);
            }
        }

        // check all attributes required by the style are available
        for (PropertyName attName : styleAttributes) {
            if (attName.evaluate(type) == null) {
                throw new ServiceException("The requested Style can not be used with this layer.  The style specifies "
                        + "an attribute named '"
                        + attName
                        + "', not found in the '"
                        + mapLayerInfo.getName()
                        + "' layer");
            }
        }
    }

    /**
     * Tests whether a style contains a Rendering Transformation.
     *
     * @param style the style to check
     * @return true if the style contains a rendering transformation
     */
    private static boolean hasTransformation(Style style) {
        for (FeatureTypeStyle fs : style.featureTypeStyles()) {
            if (fs.getTransformation() != null) return true;
        }
        return false;
    }

    /**
     * Returns the list of, possibly mixed, {@link MapLayerInfo} objects of a requested layer is a registered
     * {@link LayerInfo} or a remoteOWS one, or {@link LayerGroupInfo} objects for a requested layer name that refers to
     * a layer group.
     */
    protected List<Object> parseLayers(
            final List<String> requestedLayerNames, final URL remoteOwsUrl, final String remoteOwsType)
            throws Exception {

        List<Object> layersOrGroups = new ArrayList<>();

        // Grab remote OWS data store if needed
        DataStore remoteWFS = null;
        final List<String> remoteTypeNames = new ArrayList<>();
        if ("WFS".equals(remoteOwsType) && remoteOwsUrl != null) {
            remoteWFS = connectRemoteWFS(remoteOwsUrl);
            remoteTypeNames.addAll(Arrays.asList(remoteWFS.getTypeNames()));
            Collections.sort(remoteTypeNames);
        }

        // //
        // Layer lookup requires to:
        // * Look into the remote OWS first
        // * Look among the local layers
        // * expand local grouped layers (flatten them)
        // //
        for (String layerName : requestedLayerNames) {
            // search into the remote WFS if there is any
            if (remoteTypeNames.contains(layerName)) {
                SimpleFeatureSource remoteSource = remoteWFS.getFeatureSource(layerName);
                if (remoteSource != null) {
                    layersOrGroups.add(new MapLayerInfo(remoteSource));
                    continue;
                }
            }

            // not a remote layer, lets look up for a registered one
            LayerInfo layerInfo = wms.getLayerByName(layerName);
            if (layerInfo != null) {
                layersOrGroups.add(layerInfo);
            } else {
                LayerGroupInfo layerGroup = wms.getLayerGroupByName(layerName);
                if (layerGroup == null || LayerGroupInfo.Mode.CONTAINER.equals(layerGroup.getMode())) {
                    throw new ServiceException("Could not find layer " + layerName, "LayerNotDefined", "layers");
                }
                layersOrGroups.add(layerGroup);
            }
        }

        if (layersOrGroups.isEmpty()) {
            throw new ServiceException(
                    "No LAYERS has been requested", getClass().getName());
        }
        return layersOrGroups;
    }

    private static DataStore connectRemoteWFS(URL remoteOwsUrl) throws ServiceException {
        // check we can actually connect to server, throw an exception with locator otherwise
        try {
            URLCheckers.confirm(remoteOwsUrl);
        } catch (URLCheckerException e) {
            String msg = "Invalid remote URL: " + remoteOwsUrl;
            throw new ServiceException(msg, e, INVALID_PARAMETER_VALUE, "REMOTE_OWS_URL");
        }

        try {
            WFSDataStoreFactory factory = new WFSDataStoreFactory();
            Map<String, Object> params = new HashMap<>();
            params.put(
                    WFSDataStoreFactory.URL.key,
                    ResponseUtils.appendQueryString(
                            remoteOwsUrl.toExternalForm(), "REQUEST=GetCapabilities&SERVICE=WFS"));
            params.put(WFSDataStoreFactory.TRY_GZIP.key, Boolean.TRUE);
            return factory.createDataStore(params);
        } catch (Exception e) {
            throw new ServiceException("Could not connect to remote OWS", e, "RemoteOWSFailure");
        }
    }

    // pre GEOS-2652:
    // private MapLayerInfo buildMapLayerInfo(String layerName) throws Exception {
    // MapLayerInfo li = new MapLayerInfo();
    //
    // FeatureTypeInfo ftype = findFeatureLayer(layerName);
    // if (ftype != null) {
    // li.setFeature(ftype);
    // } else {
    // CoverageInfo cv = findCoverageLayer(layerName);
    // if (cv != null) {
    // li.setCoverage(cv);
    // } else {
    // if (wms.getBaseMapLayers().containsKey(layerName)) {
    // String styleCsl = (String) wms.getBaseMapStyles().get(layerName);
    // String layerCsl = (String) wms.getBaseMapLayers().get(layerName);
    // MapLayerInfo[] layerArray = (MapLayerInfo[]) parseLayers(KvpUtils
    // .readFlat(layerCsl), null, null);
    // List styleList = (List) parseStyles(KvpUtils.readFlat(styleCsl));
    // li.setBase(layerName, new ArrayList(Arrays.asList(layerArray)), styleList);
    // } else {
    // throw new ServiceException("Layer " + layerName + " could not be found");
    // }
    // }
    // }
    // return li;
    // }

    // FeatureTypeInfo findFeatureLayer(String layerName) throws ServiceException {
    // FeatureTypeInfo ftype = null;
    // Integer layerType = catalog.getLayerType(layerName);
    //
    // if (Data.TYPE_VECTOR != layerType) {
    // return null;
    // } else {
    // ftype = catalog.getFeatureTypeInfo(layerName);
    // }
    //
    // return ftype;
    // }
    //
    // CoverageInfo findCoverageLayer(String layerName) throws ServiceException {
    // CoverageInfo cv = null;
    // Integer layerType = catalog.getLayerType(layerName);
    //
    // if (Data.TYPE_RASTER != layerType) {
    // return null;
    // } else {
    // cv = catalog.getCoverageInfo(layerName);
    // }
    //
    // return cv;
    // }

    protected void parseStyles(
            List<String> styleNames, List<Object> requestedLayerInfos, List<Style> styles, List<String> groupStyles)
            throws Exception {
        for (int i = 0; i < styleNames.size(); i++) {
            String styleName = styleNames.get(i);
            if (styleName == null || "".equals(styleName)) {
                // return null, this should flag request reader to use default for
                // the associated layer
                styles.add(null);
                groupStyles.add(null);
            } else {
                Object layer = requestedLayerInfos.get(i);
                if (isRemoteWMSLayer(layer)) {
                    // GEOS-9312
                    // if the style belongs to a remote layer check inside the remote layer
                    // capabilities
                    // instead of local WMS
                    WMSLayerInfo remoteWMSLayer = (WMSLayerInfo) ((LayerInfo) layer).getResource();
                    Optional<Style> remoteStyle = remoteWMSLayer.findRemoteStyleByName(styleName);
                    if (remoteStyle.isPresent()) {
                        styles.add(remoteStyle.get());
                        groupStyles.add(null);
                    } else throw new ServiceException("No such remote style: " + styleName, "StyleNotDefined");
                } else {
                    parseStyle(layer, styleName, groupStyles, styles);
                }
            }
        }
    }

    private void parseStyle(Object layer, String styleName, List<String> groupStyles, List<Style> styles)
            throws IOException {
        if (layer instanceof LayerGroupInfo) {
            LayerGroupInfo groupInfo = (LayerGroupInfo) layer;
            boolean isDefaultStyle = isDefaultLgStyle(styleName, groupInfo);
            if (!isDefaultStyle && isGroupStyleName(styleName, groupInfo)) {
                groupStyles.add(styleName);
            } else {
                groupStyles.add(null);
            }
            styles.add(null);
        } else {
            groupStyles.add(null);
            final Style style = wms.getStyleByName(styleName);
            if (style != null) {
                styles.add(style);
            } else {
                String msg = "No such style: " + styleName;
                throw new ServiceException(msg, "StyleNotDefined");
            }
        }
    }

    /**
     * This flags allows the kvp reader to go beyond the SLD library mode specification and match the first style that
     * can be applied to a given layer. This is for backwards compatibility
     */
    public boolean isLaxStyleMatchAllowed() {
        return laxStyleMatchAllowed;
    }

    public void setLaxStyleMatchAllowed(boolean laxStyleMatchAllowed) {
        this.laxStyleMatchAllowed = laxStyleMatchAllowed;
    }

    @Override
    public void destroy() throws Exception {
        closeHttpClient();
    }

    // check if this requested layer is a cascaded WMS Layer
    private boolean isRemoteWMSLayer(Object o) {
        if (o == null) return false;
        else if (!(o instanceof LayerInfo)) return false;
        else return ((LayerInfo) o).getResource() instanceof WMSLayerInfo;
    }

    private Geometry getClipGeometry(GetMapRequest getMapRequest) {

        // no raw kvp or request has no crs
        if (getMapRequest.getRawKvp() == null || getMapRequest.getCrs() == null) return null;
        String wktString = getMapRequest.getRawKvp().get("clip");
        // not found
        if (wktString == null) return null;
        try {
            Geometry geom = ClipWMSGetMapCallBack.readGeometry(wktString, getMapRequest.getCrs());

            if (LOGGER.isLoggable(Level.FINE) && geom != null)
                LOGGER.fine("parsed Clip param to geometry " + geom.toText());
            return geom;
        } catch (Exception e) {
            LOGGER.severe("Ignoring clip param,Error parsing wkt in clip parameter : " + wktString);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    private boolean isDefaultLgStyle(String styleName, LayerGroupInfo groupInfo) {
        return CapabilityUtil.encodeGroupDefaultStyle(wms, groupInfo)
                && styleName != null
                && styleName.equals(CapabilityUtil.getGroupDefaultStyleName(groupInfo));
    }

    private boolean isGroupStyleName(String styleName, LayerGroupInfo groupInfo) {
        return styleName != null && !"".equals(styleName) && isSingleOrOpaque(groupInfo);
    }
}
