/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.geoserver.mapml.MapMLConstants.DATE_FORMAT;
import static org.geoserver.mapml.MapMLConstants.MAPML_CREATE_FEATURE_LINKS;
import static org.geoserver.mapml.MapMLConstants.MAPML_CREATE_FEATURE_LINKS_DEFAULT;
import static org.geoserver.mapml.MapMLConstants.MAPML_FEATURE_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_MIME_TYPE;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_ATTRIBUTES_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_STYLES_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_REMOTE;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES_REP;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES_REP_DEFAULT;
import static org.geoserver.mapml.template.MapMLMapTemplate.MAPML_PREVIEW_HEAD_FTL;
import static org.geoserver.mapml.template.MapMLMapTemplate.MAPML_XML_HEAD_FTL;
import static org.geoserver.wms.capabilities.DimensionHelper.getDataType;

import freemarker.template.TemplateMethodModelEx;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.MapMLProjection;
import org.geoserver.mapml.tcrs.TiledCRS;
import org.geoserver.mapml.template.MapMLMapTemplate;
import org.geoserver.mapml.xml.AxisType;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Input;
import org.geoserver.mapml.xml.InputRelType;
import org.geoserver.mapml.xml.InputType;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.mapml.xml.MimeType;
import org.geoserver.mapml.xml.Option;
import org.geoserver.mapml.xml.PositionType;
import org.geoserver.mapml.xml.ProjType;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.mapml.xml.Select;
import org.geoserver.mapml.xml.UnitType;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geoserver.wms.capabilities.DimensionHelper;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSubset;
import org.locationtech.jts.geom.Envelope;

/** Builds a MapML document from a WMSMapContent object */
public class MapMLDocumentBuilder {
    private static final Logger LOGGER = Logging.getLogger(MapMLDocumentBuilder.class);

    private static final Pattern ALL_COMMAS = Pattern.compile("^,+$");

    public static final String MINIMUM_WIDTH_HEIGHT = "1";
    private static final int BYTES_PER_PIXEL_TRANSPARENT = 4;
    private static final int BYTES_PER_KILOBYTE = 1024;
    public static final String DEFAULT_MIME_TYPE = "image/png";

    private final WMS wms;
    private static final String BBOX_PARAMS = "{xmin},{ymin},{xmax},{ymax}";
    private static final String BBOX_PARAMS_YX = "{ymin},{xmin},{ymax},{xmax}";

    private final WMSMapContent mapContent;
    private final HttpServletRequest request;
    private final List<RawLayer> layers;
    private final String proj;
    private final Optional<List<String>> styles;

    private final Optional<List<String>> cqlFilter;
    private final Optional<Boolean> transparent;
    private final Optional<Object> format;
    private final GWC gwc = GWC.get();
    private final String layersCommaDelimited;
    private final String layerTitlesCommaDelimited;
    private final String stylesCommaDelimited;
    private final String cqlCommadDelimited;

    private String defaultStyle;
    private String layerTitle;
    private String imageFormat = DEFAULT_MIME_TYPE;
    private String baseUrl;
    private String baseUrlPattern;
    private MapMLProjection projType;
    private MetadataMap layerMeta;
    private int height;
    private int width;
    private ReferencedEnvelope projectedBox;
    private String bbox;

    private static final String MAP_STYLE_OPEN_TAG = "<map-style>";
    private static final String MAP_STYLE_CLOSE_TAG = "</map-style>";
    private static final Pattern MAP_STYLE_REGEX =
            Pattern.compile(MAP_STYLE_OPEN_TAG + "(.+?)" + MAP_STYLE_CLOSE_TAG, Pattern.DOTALL);
    private static final Pattern MAP_LINK_REGEX = Pattern.compile("<map-link (.+?)/>", Pattern.DOTALL);

    private static final Pattern MAP_LINK_HREF_REGEX = Pattern.compile("href=\"(.+?)\"");

    private static final Pattern MAP_LINK_TITLE_REGEX = Pattern.compile("title=\"(.+?)\"");

    private List<Object> extentList;

    private Input zoomInput;

    private List<MapMLLayerMetadata> mapMLLayerMetadataList = new ArrayList<>();

    private Mapml mapml;

    private Boolean isMultiExtent = MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT_DEFAULT;
    private Boolean useFeatures = MAPML_CREATE_FEATURE_LINKS_DEFAULT;
    private Boolean useTiles = MapMLConstants.MAPML_USE_TILES_REP_DEFAULT;
    private MapMLMapTemplate mapMLMapTemplate = new MapMLMapTemplate();
    private boolean forceYX = false;

    /**
     * Constructor
     *
     * @param mapContent WMSMapContent object
     * @param wms WMS object
     * @param request HttpServletRequest object
     */
    public MapMLDocumentBuilder(WMSMapContent mapContent, WMS wms, HttpServletRequest request) {
        this.wms = wms;
        this.request = request;
        this.mapContent = mapContent;
        GetMapRequest getMapRequest = mapContent.getRequest();
        this.useFeatures = useFeatures(getMapRequest);
        this.useTiles = useTiles(getMapRequest);
        isMultiExtent = Boolean.TRUE.equals(getMultiExtent(mapContent.getRequest()));
        String rawLayersCommaDL = getMapRequest.getRawKvp().get("layers");
        this.layers = toRawLayers(rawLayersCommaDL);
        this.stylesCommaDelimited = getMapRequest.getRawKvp().get("styles") != null
                ? getMapRequest.getRawKvp().get("styles")
                : "";
        styles = Optional.ofNullable(
                stylesCommaDelimited.isEmpty() ? null : Arrays.asList(stylesCommaDelimited.split(",", -1)));
        this.cqlCommadDelimited = getMapRequest.getRawKvp().get("cql_filter") != null
                ? getMapRequest.getRawKvp().get("cql_filter")
                : "";
        cqlFilter = Optional.ofNullable(
                cqlCommadDelimited.isEmpty() ? null : Arrays.asList(cqlCommadDelimited.split(";", -1)));
        this.proj = extractCRS(getMapRequest.getRawKvp());
        this.height = getMapRequest.getHeight();
        this.width = getMapRequest.getWidth();
        this.bbox = toCommaDelimitedBbox(getMapRequest.getBbox());
        this.projectedBox = new ReferencedEnvelope(getMapRequest.getBbox(), getMapRequest.getCrs());
        this.transparent = Optional.ofNullable(
                getMapRequest.getRawKvp().get("transparent") == null
                        ? null
                        : Boolean.valueOf(getMapRequest.getRawKvp().get("transparent")));
        this.format = getFormat(getMapRequest);
        this.layersCommaDelimited = layers.stream()
                .map(l -> {
                    return l.getLayerGroupName() != null ? l.getLayerGroupName() : l.getName();
                })
                .distinct()
                .collect(Collectors.joining(","));
        this.layerTitlesCommaDelimited = layers.stream()
                .map(l -> {
                    return l.getLayerGroupTitle() != null ? l.getLayerGroupTitle() : l.getTitle();
                })
                .distinct()
                .collect(Collectors.joining(","));
    }

    /**
     * Get the CRS from the request key-value pairs
     *
     * @param rawKvp key-value pairs from the request
     * @return the CRS string
     */
    public static String extractCRS(Map<String, String> rawKvp) {
        String srs = null;
        String version = rawKvp.get("VERSION");
        if ("1.3.0".equalsIgnoreCase(version)) {
            srs = rawKvp.get("CRS");
        }
        if (srs == null) {
            // Fallback on SRS, just in case.
            srs = rawKvp.get("SRS");
        }
        return srs;
    }

    /**
     * Convert Envelope to comma delimited string
     *
     * @param bbox Envelope object
     * @return comma delimited string
     */
    public static String toCommaDelimitedBbox(Envelope bbox) {
        return bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY();
    }

    /**
     * Convert comma delimited list of layer names to a list of RawLayer objects
     *
     * @param rawLayersCommaDL comma delimited list of layer names
     * @return list of RawLayer objects
     */
    private List<RawLayer> toRawLayers(String rawLayersCommaDL) {
        List<RawLayer> rawLayers = new ArrayList<>();
        if (rawLayersCommaDL == null || rawLayersCommaDL.isEmpty()) {
            return rawLayers;
        }
        String[] rawLayersArray = rawLayersCommaDL.split(",");
        for (String rawLayerTitle : rawLayersArray) {
            LayerInfo layerInfo = wms.getLayerByName(rawLayerTitle);

            if (layerInfo == null) {
                LayerGroupInfo layerGroupInfo = wms.getLayerGroupByName(rawLayerTitle);
                if (!useFeatures || isMultiExtent) {
                    RawLayer rawLayer = new RawLayer();
                    rawLayer.setTitle(getTitle(layerGroupInfo, rawLayerTitle));
                    rawLayer.setName(layerGroupInfo.getName());
                    rawLayer.setLayerGroup(true);
                    rawLayer.setPublishedInfo(layerGroupInfo);
                    rawLayers.add(rawLayer);
                } else {
                    extractFromLayerGroup(layerGroupInfo, rawLayers);
                }
            } else {
                addToRawLayers(rawLayers, rawLayerTitle, layerInfo, null, null);
            }
        }
        return rawLayers;
    }

    private void extractFromLayerGroup(LayerGroupInfo layerGroupInfo, List<RawLayer> rawLayers) {
        for (PublishedInfo publishedInfo : layerGroupInfo.getLayers()) {
            if (publishedInfo instanceof LayerGroupInfo) {
                extractFromLayerGroup((LayerGroupInfo) publishedInfo, rawLayers);
            } else {
                addToRawLayers(
                        rawLayers,
                        publishedInfo.getTitle(),
                        (LayerInfo) publishedInfo,
                        layerGroupInfo.getTitle(),
                        layerGroupInfo.prefixedName());
            }
        }
    }

    private void addToRawLayers(
            List<RawLayer> rawLayers,
            String rawLayerTitle,
            LayerInfo layerInfo,
            String layerGroupTitle,
            String layerGroupName) {
        RawLayer rawLayer = new RawLayer();
        rawLayer.setTitle(getTitle(layerInfo, rawLayerTitle));
        rawLayer.setLayerGroup(false);
        rawLayer.setName(layerInfo.getName());
        rawLayer.setPublishedInfo(layerInfo);
        rawLayer.setLayerGroupTitle(layerGroupTitle);
        rawLayer.setLayerGroupName(layerGroupName);
        rawLayers.add(rawLayer);
    }

    /**
     * Get the format from the GetMapRequest object
     *
     * @param getMapRequest GetMapRequest object
     * @return Optional<Object> containing the format
     */
    private Optional<Object> getFormat(GetMapRequest getMapRequest) {
        return Optional.ofNullable(getMapRequest.getFormatOptions().get(MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION));
    }

    private boolean getMultiExtent(GetMapRequest getMapRequest) {
        return Boolean.parseBoolean(
                (String) getMapRequest.getFormatOptions().get(MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT));
    }

    /**
     * Initialize fields, generate and return MapML document
     *
     * @return MapML document
     * @throws ServiceException In the event of a service error.
     */
    public Mapml getMapMLDocument() throws ServiceException {
        initialize();
        prepareDocument();
        return this.mapml;
    }

    /**
     * Initialize fields
     *
     * @throws ServiceException In the event of a service error.
     */
    public void initialize() throws ServiceException {
        if (isMultiExtent || layers.size() == 1) {
            for (int i = 0; i < layers.size(); i++) {
                RawLayer layer = layers.get(i);
                String style = null;
                String cql = null;
                if (styles.isPresent()) {
                    try {
                        style = styles.get().get(i);
                    } catch (IndexOutOfBoundsException e) {
                        // if there are more layers than styles
                        throw new ServiceException("Number of styles does not match number of layers");
                    }
                }
                if (cqlFilter.isPresent()) {
                    try {
                        cql = cqlFilter.get().get(i);
                    } catch (IndexOutOfBoundsException e) {
                        // if there are more layers than cql filters
                        throw new ServiceException("Number of cql filters does not match number of layers");
                    }
                }
                MapMLLayerMetadata mapMLLayerMetadata = layerToMapMLLayerMetadata(layer, style, cql);
                mapMLLayerMetadataList.add(mapMLLayerMetadata);
            }
        } else {
            MapMLLayerMetadata mapMLLayerMetadata = layersToOneMapMLLayerMetadata(layers);
            mapMLLayerMetadataList.add(mapMLLayerMetadata);
        }
        // populate Map-wide variables using the first layer
        if (!mapMLLayerMetadataList.isEmpty()) {
            defaultStyle = stylesCommaDelimited == null || stylesCommaDelimited.isEmpty()
                    ? getDefaultLayerStyles(mapMLLayerMetadataList)
                    : stylesCommaDelimited;
            MapMLLayerMetadata mapMLLayerMetadata = mapMLLayerMetadataList.get(0);
            projType = mapMLLayerMetadata.getProjType();
            layerTitle = layerTitlesCommaDelimited;
            layerMeta = mapMLLayerMetadata.getLayerMeta();
            imageFormat = (String) format.orElse(mapMLLayerMetadata.getDefaultMimeType());
            baseUrl = ResponseUtils.baseURL(request);
            baseUrlPattern = baseUrl;
            forceYX = isYX();
        }
    }

    private boolean isYX() {
        if (!projType.isBuiltIn()) {
            String code = projType.getCRSCode();
            code = WMS.toInternalSRS(code, WMS.version("1.3.0"));
            CoordinateReferenceSystem crs13;
            try {
                crs13 = CRS.decode(code);
                return CRS.getAxisOrder(crs13) == CRS.AxisOrder.NORTH_EAST;
            } catch (FactoryException e) {
                throw new ServiceException(e);
            }
        }
        return false;
    }

    /**
     * Get the default for one or more layers or layergroups
     *
     * @param mapMLLayerMetadataList List of MapMLLayerMetadata objects
     * @return comma delimited string of default styles
     */
    private String getDefaultLayerStyles(List<MapMLLayerMetadata> mapMLLayerMetadataList) {
        String defaultStyle = "";
        for (MapMLLayerMetadata mapMLLayerMetadata : mapMLLayerMetadataList) {
            if (!mapMLLayerMetadata.isLayerGroup()) {
                LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
                if (layerInfo != null && layerInfo.getDefaultStyle() != null) {
                    defaultStyle += layerInfo.getDefaultStyle().getName() + ",";
                } else {
                    defaultStyle += ",";
                }
            } else {
                LayerGroupInfo layerGroupInfo = mapMLLayerMetadata.getLayerGroupInfo();
                if (layerGroupInfo != null
                        && !layerGroupInfo.getLayerGroupStyles().isEmpty()) {
                    defaultStyle += "default-style-" + mapMLLayerMetadata.getLayerName() + ",";
                }
            }
        }
        return defaultStyle.replaceAll(",$", "");
    }

    /**
     * Generate a merged MapMLLayerMetadata for a collection of raw layers
     *
     * @param layers List of RawLayer objects
     * @return MapMLLayerMetadata object
     */
    private MapMLLayerMetadata layersToOneMapMLLayerMetadata(List<RawLayer> layers) {
        MapMLLayerMetadata mapMLLayerMetadata = new MapMLLayerMetadata();
        mapMLLayerMetadata.setLayerMeta(new MetadataMap());
        mapMLLayerMetadata.setUseTiles(useTiles);
        mapMLLayerMetadata.setUseFeatures(useFeatures);
        mapMLLayerMetadata.setLayerName(layersCommaDelimited);
        mapMLLayerMetadata.setStyleName(stylesCommaDelimited);
        mapMLLayerMetadata.setCqlFilter(cqlCommadDelimited);
        mapMLLayerMetadata.setTimeEnabled(false);
        mapMLLayerMetadata.setElevationEnabled(false);
        mapMLLayerMetadata.setTransparent(transparent.orElse(true));
        MapMLProjection projType = parseProjType();
        mapMLLayerMetadata.setBbbox(layersToBBBox(layers, projType));
        mapMLLayerMetadata.setQueryable(layersToQueryable(layers));
        mapMLLayerMetadata.setLayerLabel(layersToLabel(layers));
        mapMLLayerMetadata.setProjType(projType);
        mapMLLayerMetadata.setDefaultMimeType(imageFormat);
        // this is a single layer, so we can check for tile cache
        if (!layersCommaDelimited.contains(",")) {
            LayerInfo layerInfo = wms.getLayerByName(layersCommaDelimited);
            LayerGroupInfo layerGroupInfo = null;
            boolean isLayerGroup = false;
            mapMLLayerMetadata.setLayerInfo(layerInfo);
            if (layerInfo == null) {
                isLayerGroup = true;
                layerGroupInfo = wms.getLayerGroupByName(layersCommaDelimited);
                mapMLLayerMetadata.setLayerGroupInfo(layerGroupInfo);
            }
            mapMLLayerMetadata.setTileLayerExists(checkForTileCache(isLayerGroup, layerGroupInfo, layerInfo, projType));
        }

        return mapMLLayerMetadata;
    }

    /** Parses the projection into a ProjType, or throws a proper service exception indicating the unsupported CRS */
    private MapMLProjection parseProjType() {
        try {
            return new MapMLProjection(proj.toUpperCase());
        } catch (IllegalArgumentException | FactoryException iae) {
            // figure out the parameter name (version dependent) and the actual original
            // string value for the srs/crs parameter
            String parameterName = Optional.ofNullable(mapContent.getRequest().getVersion())
                    .filter(v -> v.equals("1.3.0"))
                    .map(v -> "crs")
                    .orElse("srs");
            Map<String, Object> rawKvp = Dispatcher.REQUEST.get().getRawKvp();
            String value = (String) rawKvp.get("srs");
            if (value == null) value = (String) rawKvp.get("crs");
            throw new ServiceException(
                    "This projection is not supported by MapML: " + value,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    parameterName);
        }
    }

    /**
     * Generate a merged queryable flag for a collection of raw layers
     *
     * @param layers List of RawLayer objects
     * @return boolean queryable flag
     */
    private boolean layersToQueryable(List<RawLayer> layers) {
        boolean queryable = true;
        for (RawLayer layer : layers) {
            if (layer.isLayerGroup()) {
                if (((LayerGroupInfo) layer.getPublishedInfo()).isQueryDisabled()) {
                    queryable = false;
                    break;
                }
            } else {
                if (!((LayerInfo) layer.getPublishedInfo()).isQueryable()) {
                    queryable = false;
                    break;
                }
            }
        }
        return queryable;
    }

    /**
     * Generate a merged label for a collection of raw layers
     *
     * @param layers List of RawLayer objects
     * @return String containing the merged label
     */
    private String layersToLabel(List<RawLayer> layers) {
        String layerLabel = "";
        for (RawLayer layer : layers) {
            layerLabel += getLabel(layer.getPublishedInfo(), layer.getTitle(), request) + ",";
        }
        return layerLabel;
    }

    /**
     * Get merged Bounding Box for a collection of raw layers
     *
     * @param layers List of RawLayer objects
     * @param projType ProjType object
     * @return ReferencedEnvelope object
     */
    private ReferencedEnvelope layersToBBBox(List<RawLayer> layers, MapMLProjection projType) {
        ReferencedEnvelope bbbox;
        bbbox = new ReferencedEnvelope(projType.getCRS());
        for (int i = 0; i < layers.size(); i++) {
            RawLayer layer = layers.get(i);
            try {
                ReferencedEnvelope layerBbbox = layer.isLayerGroup()
                        ? ((LayerGroupInfo) layer.getPublishedInfo()).getBounds()
                        : ((LayerInfo) layer.getPublishedInfo()).getResource().boundingBox();
                if (i == 0) {
                    bbbox = layerBbbox.transform(projType.getCRS(), true);
                } else {
                    bbbox.expandToInclude(layerBbbox.transform(projType.getCRS(), true));
                }
            } catch (Exception e) {
                // get the default max/min of the pcrs from the TCRS
                Bounds defaultBounds = projType.getTiledCRS().getBounds();
                double x1, x2, y1, y2;
                x1 = defaultBounds.getMin().x;
                x2 = defaultBounds.getMax().x;
                y1 = defaultBounds.getMin().y;
                y2 = defaultBounds.getMax().y;
                // use the bounds of the TCRS as the default bounds for this layer
                bbbox = new ReferencedEnvelope(x1, x2, y1, y2, projType.getCRS());
            }
        }

        return bbbox;
    }

    /**
     * Convert a RawLayer object to a MapMLLayerMetadata object
     *
     * @param layer RawLayer object
     * @param style style name
     * @param cql CQL filter
     * @return MapMLLayerMetadata object
     * @throws ServiceException In the event of a service error.
     */
    private MapMLLayerMetadata layerToMapMLLayerMetadata(RawLayer layer, String style, String cql)
            throws ServiceException {
        ReferencedEnvelope bbox = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        boolean isLayerGroup = (layer.isLayerGroup());
        LayerInfo layerInfo = null;
        LayerGroupInfo layerGroupInfo = null;
        MetadataMap layerMeta = null;
        String workspace = null;
        boolean queryable = false;
        String layerName = null;
        String layerTitle = null;
        ResourceInfo resourceInfo = null;
        boolean isTransparent = true;
        String styleName = style != null ? style : "";
        String cqlFilter = null;
        boolean tileLayerExists = false;
        String defaultMimeType = DEFAULT_MIME_TYPE;
        if (isLayerGroup) {
            layerGroupInfo = (LayerGroupInfo) layer.getPublishedInfo();
            if (layerGroupInfo == null) {
                throw new ServiceException("Invalid layer or layer group name");
            }
            for (LayerInfo li : layerGroupInfo.layers()) {
                ReferencedEnvelope referencedEnvelope = matchReferencedEnvelopeCRS(li, bbox);
                bbox.expandToInclude(referencedEnvelope);
            }
            layerMeta = layerGroupInfo.getMetadata();
            workspace = (layerGroupInfo.getWorkspace() != null
                    ? layerGroupInfo.getWorkspace().getName()
                    : "");
            queryable = !layerGroupInfo.isQueryDisabled();
            layerName = layerGroupInfo.getName();
            layerTitle = getTitle(layerGroupInfo, layerName);
            defaultMimeType = Optional.ofNullable(layerGroupInfo.getMetadata().get(MapMLConstants.MAPML_MIME))
                    .orElse(DEFAULT_MIME_TYPE)
                    .toString();
        } else {
            layerInfo = (LayerInfo) layer.getPublishedInfo();
            resourceInfo = layerInfo.getResource();
            bbox = layerInfo.getResource().getLatLonBoundingBox();
            layerMeta = resourceInfo.getMetadata();
            workspace = (resourceInfo.getStore().getWorkspace() != null
                    ? resourceInfo.getStore().getWorkspace().getName()
                    : "");
            queryable = layerInfo.isQueryable();
            isTransparent = transparent.orElse(!layerInfo.isOpaque());
            layerName = layerInfo.getName().isEmpty() ? layer.getTitle() : layerInfo.getName();
            layerTitle = getTitle(layerInfo, layerName);
            // set the actual style name from the layer info
            if (style == null) styleName = layerInfo.getDefaultStyle().getName();
            defaultMimeType = Optional.ofNullable(resourceInfo.getMetadata().get(MapMLConstants.MAPML_MIME))
                    .orElse(DEFAULT_MIME_TYPE)
                    .toString();
        }
        MapMLProjection projType = parseProjType();
        cqlFilter = cql != null ? cql : "";
        tileLayerExists = checkForTileCache(isLayerGroup, layerGroupInfo, layerInfo, projType);
        boolean useRemote = Boolean.TRUE.equals(layerMeta.get(MAPML_USE_REMOTE, Boolean.class));

        String legendURL =
                calculateLegendURL(isLayerGroup, layerInfo, layerGroupInfo, workspace, layerName, styleName, useRemote);

        return new MapMLLayerMetadata(
                layerInfo,
                bbox,
                isLayerGroup,
                layerGroupInfo,
                layerMeta,
                workspace,
                queryable,
                isTransparent,
                layerName,
                layerTitle,
                projType,
                styleName,
                tileLayerExists,
                useTiles,
                useRemote,
                useFeatures,
                cqlFilter,
                defaultMimeType,
                legendURL);
    }

    private boolean checkForTileCache(
            boolean isLayerGroup, LayerGroupInfo layerGroupInfo, LayerInfo layerInfo, MapMLProjection projType) {
        return gwc.hasTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                && gwc.getTileLayer(isLayerGroup ? layerGroupInfo : layerInfo).getGridSubset(projType.value()) != null;
    }

    /**
     * Calculates the legend URL for a layer or layer group.
     *
     * @param isLayerGroup Whether this is a layer group
     * @param layerInfo Layer information (null if this is a layer group)
     * @param layerGroupInfo Layer group information (null if this is a regular layer)
     * @param workspace Workspace name
     * @param layerName Layer or layer group name
     * @param styleName Style name
     * @param useRemote true if URL should be cascaded when available
     * @return The legend URL, or null if one cannot be generated
     */
    private String calculateLegendURL(
            boolean isLayerGroup,
            LayerInfo layerInfo,
            LayerGroupInfo layerGroupInfo,
            String workspace,
            String layerName,
            String styleName,
            boolean useRemote) {

        String baseUrl = ResponseUtils.baseURL(request);
        String workspacePrefix = workspace.isEmpty() ? "" : workspace + ":";

        if (isLayerGroup && layerGroupInfo != null) {
            return createLayerGroupLegendURL(baseUrl, workspacePrefix + layerName, styleName);
        } else if (!isLayerGroup && layerInfo != null) {
            return createLayerLegendURL(baseUrl, layerInfo, workspacePrefix + layerName, styleName, useRemote);
        }

        return null;
    }

    /** Creates a legend URL for a layer group. */
    private String createLayerGroupLegendURL(String baseUrl, String fullLayerName, String styleName) {
        Map<String, String> params = createBaseWMSParams(fullLayerName, styleName);
        return ResponseUtils.buildURL(baseUrl, "ows", params, URLMangler.URLType.SERVICE);
    }

    /** Creates a legend URL for a regular layer. */
    private String createLayerLegendURL(
            String baseUrl, LayerInfo layerInfo, String fullLayerName, String styleName, boolean useRemote) {
        Catalog catalog = layerInfo.getResource().getCatalog();

        // Find the appropriate style
        StyleInfo styleInfo = findStyleInfo(catalog, layerInfo, styleName);

        // Check for user-defined legend
        if (styleInfo != null) {
            LegendInfo legend = styleInfo.getLegend();
            if (legend != null && legend.getOnlineResource() != null && useRemote) {
                return legend.getOnlineResource();
            }

            // No user-defined legend, generate WMS URL
            Map<String, String> params = createBaseWMSParams(
                    fullLayerName, (styleName != null && !styleName.isEmpty()) ? styleName : styleInfo.getName());
            return ResponseUtils.buildURL(baseUrl, "ows", params, URLMangler.URLType.SERVICE);
        }

        // Fallback to basic legend URL
        Map<String, String> params = createBaseWMSParams(fullLayerName, styleName);
        return ResponseUtils.buildURL(baseUrl, "ows", params, URLMangler.URLType.SERVICE);
    }

    /** Creates base WMS parameters for legend requests. */
    private Map<String, String> createBaseWMSParams(String fullLayerName, String styleName) {
        Map<String, String> params = new HashMap<>();
        params.put("service", "WMS");
        params.put("version", "1.3.0");
        params.put("request", "GetLegendGraphic");
        params.put("format", "image/png");
        params.put("layer", fullLayerName);

        if (styleName != null && !styleName.isEmpty()) {
            params.put("style", styleName);
        }

        return params;
    }

    /** Finds the appropriate StyleInfo for a layer. */
    private StyleInfo findStyleInfo(Catalog catalog, LayerInfo layerInfo, String styleName) {
        if (styleName == null || styleName.isEmpty()) {
            return layerInfo.getDefaultStyle();
        }

        StyleInfo styleInfo = catalog.getStyleByName(styleName);

        // If not in catalog, check the layer's style collection
        if (styleInfo == null && layerInfo.getStyles() != null) {
            for (StyleInfo si : layerInfo.getStyles()) {
                if (si != null && styleName.equals(si.getName())) {
                    return si;
                }
            }
        }

        return styleInfo != null ? styleInfo : layerInfo.getDefaultStyle();
    }

    /**
     * Check if layer should be represented as a feature
     *
     * @param getMapRequest GetMapRequest
     * @return boolean true if layer should be represented as a feature
     */
    @SuppressWarnings("unchecked")
    private static boolean useFeatures(GetMapRequest getMapRequest) {
        Optional useFeaturesOptional = Optional.ofNullable(getMapRequest
                .getFormatOptions()
                .getOrDefault(
                        MAPML_CREATE_FEATURE_LINKS.toUpperCase(),
                        getMapRequest.getFormatOptions().get(MAPML_CREATE_FEATURE_LINKS.toLowerCase())));
        return (Boolean.parseBoolean(
                (String) useFeaturesOptional.orElse(MAPML_CREATE_FEATURE_LINKS_DEFAULT.toString())));
    }

    /**
     * Check if layer should be represented with tiles
     *
     * @param getMapRequest GetMapRequest
     * @return boolean useTiles
     */
    @SuppressWarnings("unchecked")
    public static boolean useTiles(GetMapRequest getMapRequest) {
        Optional useTilesOptional = Optional.ofNullable(getMapRequest
                .getFormatOptions()
                .getOrDefault(
                        MAPML_USE_TILES_REP.toUpperCase(),
                        getMapRequest.getFormatOptions().get(MAPML_USE_TILES_REP.toLowerCase())));
        return Boolean.TRUE.equals(
                Boolean.parseBoolean((String) useTilesOptional.orElse(MAPML_USE_TILES_REP_DEFAULT.toString())));
    }

    /**
     * Match the CRS of the layer to the CRS of the bbox
     *
     * @param li LayerInfo
     * @param bbox ReferencedEnvelope with CRS
     * @return ReferencedEnvelope with matching CRS
     */
    private static ReferencedEnvelope matchReferencedEnvelopeCRS(LayerInfo li, ReferencedEnvelope bbox) {
        ReferencedEnvelope referencedEnvelope = li.getResource().getLatLonBoundingBox();
        if (!CRS.equalsIgnoreMetadata(
                bbox.getCoordinateReferenceSystem(), referencedEnvelope.getCoordinateReferenceSystem())) {
            try {
                referencedEnvelope = referencedEnvelope.transform(bbox.getCoordinateReferenceSystem(), true);
            } catch (TransformException | FactoryException e) {
                throw new ServiceException("Unable to transform layer bounds to WGS84 for layer" + li.getName());
            }
        }
        return referencedEnvelope;
    }

    /**
     * Get the localized title for a MapML layer or layer group
     *
     * @param p LayerInfo or LayerGroupInfo
     * @param defaultTitle the default value for title, usually layer name
     * @return potentially localized title string
     */
    public String getTitle(PublishedInfo p, String defaultTitle) {
        if (p instanceof LayerGroupInfo) {
            LayerGroupInfo li = (LayerGroupInfo) p;
            if (li.getInternationalTitle() != null
                    && li.getInternationalTitle().toString(request.getLocale()) != null) {
                // use international title per request or default locale
                return li.getInternationalTitle().toString(request.getLocale()).trim();
            } else if (li.getTitle() != null && !li.getTitle().trim().isEmpty()) {
                return li.getTitle().trim();
            } else {
                return li.getName().trim().isEmpty()
                        ? defaultTitle
                        : li.getName().trim();
            }
        } else {
            LayerInfo li = (LayerInfo) p;
            if (li.getInternationalTitle() != null
                    && li.getInternationalTitle().toString(request.getLocale()) != null) {
                // use international title per request or default locale
                return li.getInternationalTitle().toString(request.getLocale()).trim();
            } else if (li.getTitle() != null && !li.getTitle().trim().isEmpty()) {
                return li.getTitle().trim();
            } else {
                return li.getName().trim().isEmpty()
                        ? defaultTitle
                        : li.getName().trim();
            }
        }
    }

    /** Create and return MapML document */
    private void prepareDocument() {
        // build the mapML doc
        try {
            mapml = new Mapml();
            mapml.setHead(prepareHead());
            mapml.setBody(prepareBody());
        } catch (IOException e) {
            throw new ServiceException("Error building MapML document", e);
        }
    }

    /**
     * Create and return Mapml HeadContent JAXB object
     *
     * @return
     */
    private HeadContent prepareHead() throws IOException {
        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(layerTitle);
        Base base = new Base();
        Map<String, String> wmsParams = new HashMap<>();
        wmsParams.put("format", MapMLConstants.MAPML_MIME_TYPE);
        String formatOptions =
                MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION + ":" + escapeHtml4((String) format.orElse(imageFormat)) + ";"
                        + MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT + ":" + isMultiExtent + ";"
                        + MAPML_USE_TILES_REP + ":" + useTiles + ";" + MAPML_CREATE_FEATURE_LINKS + ":" + useFeatures;
        wmsParams.put("format_options", formatOptions);
        wmsParams.put("layers", layersCommaDelimited);
        wmsParams.put("crs", projType.getCRSCode());
        wmsParams.put("version", "1.3.0");
        wmsParams.put("service", "WMS");
        wmsParams.put("request", "GetMap");
        if (transparent.isPresent()) {
            wmsParams.put("transparent", Boolean.toString(transparent.orElse(false)));
        }
        base.setHref(ResponseUtils.buildURL(baseUrl, "wms", null, URLMangler.URLType.RESOURCE));
        head.setBase(base);
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("utf-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MAPML_MIME_TYPE + ";projection=" + projType.value());
        metas.add(meta);
        meta = new Meta();
        meta.setName("projection");
        meta.setContent(projType.value());
        List<Link> links = head.getLinks();

        String licenseLink = layerMeta.get(MapMLConstants.LICENSE_LINK, String.class);
        String licenseTitle = layerMeta.get(MapMLConstants.LICENSE_TITLE, String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link titleLink = new Link();
            titleLink.setRel(RelType.LICENSE);
            if (licenseTitle != null) {
                titleLink.setTitle(licenseTitle);
            }
            if (licenseLink != null) {
                titleLink.setHref(licenseLink);
            }
            links.add(titleLink);
        }
        // only create style links for single layer requests
        if (mapMLLayerMetadataList.size() == 1 && layers.size() == 1) {
            MapMLLayerMetadata mapMLLayerMetadata = mapMLLayerMetadataList.get(0);
            if (!mapMLLayerMetadata.isLayerGroup()) {
                LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
                // styles
                Set<StyleInfo> layerInfoStyles = layerInfo.getStyles();
                String effectiveStyleName = mapMLLayerMetadata.getStyleName();
                if (effectiveStyleName == null || effectiveStyleName.isEmpty()) {
                    effectiveStyleName = layerInfo.getDefaultStyle().getName();
                }
                addLegendLink(mapMLLayerMetadata, links);
                // style links
                for (StyleInfo si : layerInfoStyles) {
                    // Copy the base params to create one for each style
                    Map<String, String> styleParams = new HashMap<>(wmsParams);
                    // skip the self style case (if it is even listed)
                    if (si.getName().equalsIgnoreCase(effectiveStyleName)) continue;
                    Link styleLink = new Link();
                    styleLink.setRel(RelType.STYLE);
                    styleLink.setTitle(si.getName());
                    styleParams.put("styles", si.getName());
                    if (cqlFilter.isPresent()) styleParams.put("cql_filter", cqlCommadDelimited);
                    styleParams.put("width", Integer.toString(width));
                    styleParams.put("height", Integer.toString(height));
                    styleParams.put("bbox", bbox);

                    String url = ResponseUtils.buildURL(baseUrl, "wms", styleParams, URLMangler.URLType.SERVICE);
                    styleLink.setHref(url);
                    links.add(styleLink);
                }
            } else {
                LayerGroupInfo layerGroupInfo = mapMLLayerMetadata.getLayerGroupInfo();
                String effectiveStyleName = mapMLLayerMetadata.getStyleName();
                if (effectiveStyleName == null || effectiveStyleName.isEmpty()) {
                    effectiveStyleName = "default-style-" + mapMLLayerMetadata.getLayerName();
                }
                addLegendLink(mapMLLayerMetadata, links);
                StyleInfo si;
                for (LayerGroupStyle layerGroupStyle : layerGroupInfo.getLayerGroupStyles()) {
                    si = layerGroupStyle.getName();
                    // Copy the base params to create one for each style
                    Map<String, String> styleParams = new HashMap<>(wmsParams);
                    // skip the self style case (if it is even listed)
                    if (layerGroupStyle.getName().getName().equalsIgnoreCase(effectiveStyleName)) continue;
                    Link styleLink = new Link();
                    styleLink.setRel(RelType.STYLE);
                    styleLink.setTitle(si.getName());
                    styleParams.put("styles", si.getName());
                    if (cqlFilter.isPresent()) styleParams.put("cql_filter", cqlCommadDelimited);
                    styleParams.put("width", Integer.toString(width));
                    styleParams.put("height", Integer.toString(height));
                    styleParams.put("bbox", bbox);
                    String url = ResponseUtils.buildURL(baseUrl, "wms", styleParams, URLMangler.URLType.SERVICE);
                    styleLink.setHref(url);
                    links.add(styleLink);
                }
            }
        }
        // output the self style link, taking care to handle the default empty string
        // styleName case
        Link selfStyleLink = new Link();
        selfStyleLink.setRel(RelType.SELF_STYLE);
        selfStyleLink.setTitle(
                stylesCommaDelimited == null || stylesCommaDelimited.isEmpty() ? defaultStyle : stylesCommaDelimited);
        // Copy the base params to create one for self style
        Map<String, String> selfStyleParams = new HashMap<>(wmsParams);
        selfStyleParams.put(
                "styles",
                stylesCommaDelimited == null || stylesCommaDelimited.isEmpty() ? defaultStyle : stylesCommaDelimited);
        selfStyleParams.put("width", Integer.toString(width));
        selfStyleParams.put("height", Integer.toString(height));
        selfStyleParams.put("bbox", bbox);
        String selfStyleURL = ResponseUtils.buildURL(baseUrl, "wms", selfStyleParams, URLMangler.URLType.SERVICE);
        selfStyleLink.setHref(selfStyleURL);
        links.add(selfStyleLink);
        // alternate projection links
        ProjType builtInProj = projType.unwrap();
        for (ProjType pt : ProjType.values()) {
            // skip the current proj

            if (pt.equals(builtInProj)) continue;
            try {
                Link projectionLink = new Link();
                projectionLink.setRel(RelType.ALTERNATE);
                projectionLink.setProjection(pt.value());
                // reproject the bounds
                ReferencedEnvelope reprojectedBounds = reproject(projectedBox, new MapMLProjection(pt));
                // Copy the base params to create one for self style
                Map<String, String> projParams = new HashMap<>(wmsParams);
                projParams.put("crs", pt.getCRSCode());
                projParams.put("width", Integer.toString(width));
                projParams.put("height", Integer.toString(height));
                projParams.put("bbox", toCommaDelimitedBbox(reprojectedBounds));
                String projURL = ResponseUtils.buildURL(baseUrl, "wms", projParams, URLMangler.URLType.SERVICE);
                projectionLink.setHref(projURL);
                links.add(projectionLink);
            } catch (Exception e) {
                // we gave it our best try but reprojection failed anyways, log and skip this link
                LOGGER.log(Level.INFO, "Unable to reproject bounds for " + pt.value(), e);
            }
        }
        String styles = buildStyles();
        // get the styles and links from the head template
        List<String> stylesAndLinks = getHeaderTemplates(MAPML_XML_HEAD_FTL, getFeatureTypes());
        styles = appendStylesFromHeadTemplate(styles, stylesAndLinks);
        if (styles != null) head.setStyle(styles);
        links.addAll(getLinksFromHeadTemplate(stylesAndLinks));
        return head;
    }

    private static void addLegendLink(MapMLLayerMetadata mapMLLayerMetadata, List<Link> links) {
        Link legendLink = new Link();
        legendLink.setRel(RelType.LEGEND);
        String legendUrl = mapMLLayerMetadata.getLegendURL();
        legendLink.setHref(legendUrl);
        if (legendUrl != null && !legendUrl.isBlank()) {
            links.add(legendLink);
        }
    }

    /**
     * Get Links generated from the head template
     *
     * @param stylesAndLinks Styles and links from the head template
     * @return List of Link objects
     */
    private List<Link> getLinksFromHeadTemplate(List<String> stylesAndLinks) {
        List<Link> outLinks = new ArrayList<>();
        List<String> extractedLinks = extractLinks(stylesAndLinks);
        for (String extractedLink : extractedLinks) {
            Link link = new Link();
            Matcher matcherTitle = MAP_LINK_TITLE_REGEX.matcher(extractedLink);
            if (matcherTitle.find()) {
                link.setTitle(matcherTitle.group(1));
            }
            Matcher matcherHref = MAP_LINK_HREF_REGEX.matcher(extractedLink);
            if (matcherHref.find()) {
                link.setRel(RelType.STYLE);
                link.setHref(matcherHref.group(1));
                // only add if mandatory href attribute is found
                outLinks.add(link);
            }
        }
        return outLinks;
    }

    private String appendStylesFromHeadTemplate(String styles, List<String> stylesAndLinks) {

        List<String> extractedStyles = extractStyles(stylesAndLinks);
        for (String extractedStyle : extractedStyles) {
            if (styles == null) {
                styles = extractedStyle;
            } else {
                styles = styles + " " + extractedStyle;
            }
        }
        return styles;
    }

    private List<String> extractLinks(List<String> stylesAndLinks) {
        List<String> extractedStyles = new ArrayList<>();
        for (String stylesAndLink : stylesAndLinks) {
            Matcher matcher = MAP_LINK_REGEX.matcher(stylesAndLink);
            while (matcher.find()) {
                extractedStyles.add(matcher.group());
            }
        }
        return extractedStyles;
    }

    private List<String> extractStyles(List<String> stylesAndLinks) {
        List<String> extractedStyles = new ArrayList<>();
        for (String stylesAndLink : stylesAndLinks) {
            Matcher matcher = MAP_STYLE_REGEX.matcher(stylesAndLink);
            while (matcher.find()) {
                extractedStyles.add(
                        matcher.group().replaceAll(MAP_STYLE_OPEN_TAG, "").replace(MAP_STYLE_CLOSE_TAG, ""));
            }
        }
        return extractedStyles;
    }

    /** Builds the CSS styles for all the layers involved in this GetMap */
    private String buildStyles() throws IOException {
        List<String> cssStyles = new ArrayList<>();
        for (MapMLLayerMetadata mapMLLayerMetadata : mapMLLayerMetadataList) {
            if (!mapMLLayerMetadata.isLayerGroup()
                    && !isLayerGroup(mapMLLayerMetadata.getLayerName())
                    && mapMLLayerMetadata.getLayerInfo() != null) {
                String styleNames = mapMLLayerMetadata.getStyleName();
                Style style = convertStylesToCSS(styleNames, cssStyles);
                if (style == null) {
                    // No style found, get default
                    getDefaultStyle(mapMLLayerMetadata, style, cssStyles);
                }
            } else if (isLayerGroup(mapMLLayerMetadata.getLayerName())) {
                String styleNames = mapMLLayerMetadata.getStyleName();
                String[] styleNameArray = styleNames.split(",");
                LayerGroupInfo layerGroupInfo = mapMLLayerMetadata.getLayerGroupInfo() != null
                        ? mapMLLayerMetadata.getLayerGroupInfo()
                        : getLayerGroupInfo(mapMLLayerMetadata.getLayerName());
                for (String styleName : styleNameArray) {
                    if (styleName == null || styleName.isEmpty()) continue;
                    List<Style> styles = getLayerGroupStyle(layerGroupInfo, styleName);
                    for (Style style : styles) {
                        styleToCSS(cssStyles, style);
                    }
                }
                // if no styles were found in the request for this layergroup, get the default styles
                if (styleNameArray == null
                        || styleNameArray.length < 1
                        || Arrays.stream(styleNameArray).allMatch(String::isEmpty)) {
                    getLayerGroupStyles(layerGroupInfo, cssStyles);
                }
            } else {
                // neither layer nor layer group, this is the case where multiple layers are requested for one extent
                if (mapMLLayerMetadata.getLayerName().contains(",")) {
                    String styleNames = mapMLLayerMetadata.getStyleName();
                    String[] styleNameArray = null;
                    if (styleNames != null && !styleNames.isEmpty()) {
                        styleNameArray = styleNames.split(",");
                        convertStylesToCSS(styleNames, cssStyles);
                    }
                    int i = 0;
                    for (String layerName : mapMLLayerMetadata.getLayerName().split(",")) {
                        LayerInfo layerInfo = wms.getLayerByName(layerName);
                        if (layerInfo != null) {
                            StyleInfo styleInfo = layerInfo.getDefaultStyle();
                            addCSS(cssStyles, styleInfo);
                        } else {
                            LayerGroupInfo layerGroupInfo = wms.getLayerGroupByName(layerName);
                            if (layerGroupInfo != null && !isIndexValid(styleNameArray, i)) {
                                // no style was found in the request for this layergroup, get the default styles
                                getLayerGroupStyles(layerGroupInfo, cssStyles);
                            }
                        }
                        i++;
                    }
                }
            }
        }
        if (cssStyles.isEmpty()) return null;
        return MapMLFeatureUtil.BBOX_DISPLAY_NONE + " " + String.join(" ", cssStyles);
    }

    private static boolean isIndexValid(String[] array, int index) {
        if (array == null || index >= array.length) {
            return false;
        } else {
            return array[index] != null && !array[index].trim().isEmpty();
        }
    }

    public static boolean isElementEmpty(String[] array, int index) {
        return array[index] == null || array[index].isEmpty();
    }

    private Style convertStylesToCSS(String styleNames, List<String> cssStyles) throws IOException {
        String[] styleNameArray = styleNames.split(",");
        Style style = null;
        for (String styleName : styleNameArray) {
            if (styleName == null || styleName.isEmpty()) continue;
            style = wms.getStyleByName(styleName);
            if (style != null) {
                styleToCSS(cssStyles, style);
            } else {
                LOGGER.log(Level.INFO, "Could not find style named " + styleName);
            }
        }
        return style;
    }

    private void getLayerGroupStyles(LayerGroupInfo layerGroupInfo, List<String> cssStyles) throws IOException {
        List<LayerGroupStyle> layerGroupStyles = layerGroupInfo.getLayerGroupStyles();
        // no styles in the layer group, get the default styles for the individual layers
        if (layerGroupStyles == null || layerGroupStyles.isEmpty()) {
            List<StyleInfo> styleInfos = layerGroupInfo.getStyles();
            List<PublishedInfo> publishedInfos = layerGroupInfo.getLayers();
            getStyles(cssStyles, styleInfos, publishedInfos);
        } else {
            for (LayerGroupStyle layerGroupStyle : layerGroupStyles) {
                List<StyleInfo> styleInfos = layerGroupStyle.getStyles();
                List<PublishedInfo> layerInfos = layerGroupStyle.getLayers();
                getStyles(cssStyles, styleInfos, layerInfos);
            }
        }
    }

    private void getStyles(List<String> cssStyles, List<StyleInfo> styleInfos, List<PublishedInfo> publishedInfos)
            throws IOException {
        for (int i = 0; i < styleInfos.size(); i++) {
            StyleInfo styleInfo = styleInfos.get(i);
            Style style = null;
            if (styleInfo != null) {
                style = styleInfo.getStyle();
            } else {
                PublishedInfo publishedInfo = publishedInfos.get(i);
                if (publishedInfo instanceof LayerInfo) {
                    // if the style is null, get the default style from the layer
                    LayerInfo layerInfo = (LayerInfo) publishedInfos.get(i);
                    if (layerInfo != null) {
                        StyleInfo defaultStyle = layerInfo.getDefaultStyle();
                        style = defaultStyle.getStyle();
                    } else {
                        LOGGER.log(Level.INFO, "Could not find style for layer " + publishedInfos.get(i));
                    }
                } else if (publishedInfo instanceof LayerGroupInfo) {
                    getLayerGroupStyles((LayerGroupInfo) publishedInfos.get(i), cssStyles);
                }
            }
            if (style != null) {
                styleToCSS(cssStyles, style);
            }
        }
    }

    private void getDefaultStyle(MapMLLayerMetadata mapMLLayerMetadata, Style style, List<String> cssStyles)
            throws IOException {
        StyleInfo styleInfo = mapMLLayerMetadata.getLayerInfo().getDefaultStyle();
        addCSS(cssStyles, styleInfo);
    }

    private void addCSS(List<String> cssStyles, StyleInfo styleInfo) throws IOException {
        Style style;
        if (styleInfo != null) {
            style = styleInfo.getStyle();
            styleToCSS(cssStyles, style);
        }
    }

    private void styleToCSS(List<String> cssStyles, Style style) {
        Map<String, MapMLStyle> styles = MapMLFeatureUtil.getMapMLStyleMap(style, mapContent.getScaleDenominator());
        String css = MapMLFeatureUtil.getCSSStyles(styles);
        cssStyles.add(css);
    }

    private boolean isLayerGroup(String layerName) {
        return wms.getLayerGroupByName(layerName) != null;
    }

    private LayerGroupInfo getLayerGroupInfo(String layerName) {
        LayerGroupInfo layerGroupInfo = wms.getLayerGroupByName(layerName);
        if (layerGroupInfo == null) {
            throw new ServiceException("Layer group " + layerName + " not found");
        }
        return layerGroupInfo;
    }

    private List<Style> getLayerGroupStyle(LayerGroupInfo layerGroupInfo, String styleName) throws IOException {
        List<Style> styles = new ArrayList<>();
        for (LayerGroupStyle layerGroupStyle : layerGroupInfo.getLayerGroupStyles()) {
            if (layerGroupStyle.getName().getName().equalsIgnoreCase(styleName)) {
                List<StyleInfo> styleInfos = layerGroupStyle.getStyles();
                for (StyleInfo styleInfo : styleInfos) {
                    if (styleInfo != null) {
                        styles.add(styleInfo.getStyle());
                    }
                }
            }
        }
        return styles;
    }

    /**
     * Reproject the bounds to the target CRS
     *
     * @param bounds ReferencedEnvelope object
     * @param pt ProjType object
     * @return ReferencedEnvelope object
     * @throws FactoryException In the event of a factory error.
     * @throws TransformException In the event of a transform error.
     */
    private ReferencedEnvelope reproject(ReferencedEnvelope bounds, MapMLProjection pt)
            throws FactoryException, TransformException {
        CoordinateReferenceSystem targetCRS = pt.getCRS();
        // leverage the rendering ProjectionHandlers to build a set of envelopes
        // inside the valid area of the target CRS, and fuse them
        ProjectionHandler ph = ProjectionHandlerFinder.getHandler(bounds, targetCRS, true);
        ReferencedEnvelope targetBounds = null;
        if (ph != null) {
            List<ReferencedEnvelope> queryEnvelopes = ph.getQueryEnvelopes();
            for (ReferencedEnvelope envelope : queryEnvelopes) {
                if (targetBounds == null) {
                    targetBounds = envelope;
                } else {
                    targetBounds.expandToInclude(envelope);
                }
            }
        } else {
            targetBounds = bounds.transform(targetCRS, true);
        }
        return targetBounds;
    }

    /**
     * Create and return MapML BodyContent JAXB object
     *
     * @return BodyContent
     */
    private BodyContent prepareBody() {
        BodyContent body = new BodyContent();
        try {
            body.setExtents(prepareExtents());
        } catch (IOException ioe) {

        }
        return body;
    }

    /**
     * Create and return a Mapml Extent JAXB object
     *
     * @return Extent
     * @throws IOException In the event of an I/O error.
     */
    private List<Extent> prepareExtents() throws IOException {
        List<Extent> extents = new ArrayList<>();
        for (MapMLLayerMetadata mapMLLayerMetadata : mapMLLayerMetadataList) {
            Extent extent = new Extent();
            TiledCRS tiledCRS = projType.getTiledCRS();
            extent.setUnits(tiledCRS.getName());
            extentList = extent.getInputOrDatalistOrLink();

            // zoom
            NumberRange<Double> scaleDenominators = null;
            // layerInfo is null when layer is a layer group or multi layer request for multi-extent
            if (!mapMLLayerMetadata.isLayerGroup() && mapMLLayerMetadata.getLayerInfo() != null) {
                scaleDenominators = CapabilityUtil.searchMinMaxScaleDenominator(mapMLLayerMetadata.getLayerInfo());
            } else if (mapMLLayerMetadata.getLayerGroupInfo() != null) {
                scaleDenominators = CapabilityUtil.searchMinMaxScaleDenominator(mapMLLayerMetadata.getLayerGroupInfo());
            }

            Input extentZoomInput = new Input();
            extentZoomInput.setName("z");
            extentZoomInput.setType(InputType.ZOOM);
            // passing in max sld denominator to get min zoom
            extentZoomInput.setMin(
                    scaleDenominators != null
                            ? String.valueOf(tiledCRS.getMinZoomForDenominator(
                                    scaleDenominators.getMaxValue().intValue()))
                            : "0");
            int mxz = tiledCRS.getScales().length - 1;
            // passing in min sld denominator to get max zoom
            String maxZoom = scaleDenominators != null
                    ? String.valueOf(tiledCRS.getMaxZoomForDenominator(
                            scaleDenominators.getMinValue().intValue()))
                    : String.valueOf(mxz);
            extentZoomInput.setMax(maxZoom);
            extentList.add(extentZoomInput);

            String dimension = layerMeta.get("mapml.dimension", String.class);
            prepareExtentForLayer(mapMLLayerMetadata, dimension);
            generateTemplatedLinks(mapMLLayerMetadata);
            if (isMultiExtent || isSingleLayerWithDimensionOptions(mapMLLayerMetadataList)) {
                extent.setHidden(null); // not needed for multi-extent
                extent.setLabel(mapMLLayerMetadata.layerTitle);
            }
            extents.add(extent);
        }

        return extents;
    }

    private boolean isSingleLayerWithDimensionOptions(List<MapMLLayerMetadata> mapMLLayerMetadataList) {
        if (mapMLLayerMetadataList.size() == 1) {
            MapMLLayerMetadata metadata = mapMLLayerMetadataList.get(0);
            return metadata.isTimeEnabled()
                    || metadata.isElevationEnabled()
                    || StringUtils.isNotBlank(metadata.getCustomDimension());
        }
        return false;
    }

    /**
     * Prepare the extent for a layer
     *
     * @param mapMLLayerMetadata MapMLLayerMetadata object
     * @param dimension dimension name
     * @throws IOException In the event of an I/O error.
     */
    private void prepareExtentForLayer(MapMLLayerMetadata mapMLLayerMetadata, String dimension) throws IOException {
        if (dimension == null || mapMLLayerMetadata.isLayerGroup()) {
            return;
        }
        LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            prepareFeatureExtent((FeatureTypeInfo) resourceInfo, mapMLLayerMetadata, dimension);
        } else if (resourceInfo instanceof CoverageInfo) {
            prepareCoverageExtent((CoverageInfo) resourceInfo, mapMLLayerMetadata, dimension);
        }
    }

    @SuppressWarnings("unchecked")
    private void prepareFeatureExtent(FeatureTypeInfo typeInfo, MapMLLayerMetadata layerMetadata, String dimension)
            throws IOException {
        MetadataMap metadataMap = typeInfo.getMetadata();
        DimensionOptions options;
        if ("Time".equalsIgnoreCase(dimension)) {
            options = initOptions(layerMetadata, metadataMap, ResourceInfo.TIME);
            if (options.isAvailable()) {
                options.addDates(wms.getFeatureTypeTimes(typeInfo));
            }
        } else if ("Elevation".equalsIgnoreCase(dimension)) {
            options = initOptions(layerMetadata, metadataMap, ResourceInfo.ELEVATION);
            if (options.isAvailable()) {
                options.addDoubles(wms.getFeatureTypeElevations(typeInfo));
            }
        } else {
            options = initOptions(layerMetadata, metadataMap, dimension);
            if (options.isAvailable()) {
                DimensionInfo info = options.getInfo();
                final TreeSet<Object> values = wms.getDimensionValues(typeInfo, info);
                final Optional<Class<?>> dataTypeOpt = getDataType(values);
                if (dataTypeOpt.isPresent()) {
                    final Class<?> type = dataTypeOpt.get();
                    if (Date.class.isAssignableFrom(type)) {
                        options.addDates((Collection<Date>) (Collection<?>) values);
                    } else if (Number.class.isAssignableFrom(type)) {
                        options.addNumbers((Collection<Number>) (Collection<?>) values);
                    } else {
                        final List<String> valuesList = values.stream()
                                .filter(x -> x != null)
                                .map(x -> x.toString())
                                .collect(Collectors.toList());
                        options.addStrings(valuesList);
                    }
                }
            }
        }
    }

    private void prepareCoverageExtent(CoverageInfo cvInfo, MapMLLayerMetadata layerMetadata, String dimension)
            throws IOException {
        MetadataMap metadataMap = cvInfo.getMetadata();
        DimensionOptions options;
        DimensionInfo dimInfo;
        ReaderDimensionsAccessor accessor;
        if ("Time".equalsIgnoreCase(dimension)) {
            options = initOptions(layerMetadata, metadataMap, ResourceInfo.TIME);
            if (options.isAvailable()) {
                dimInfo = options.getInfo();
                accessor = getAccessor(cvInfo);
                DimensionHelper.TemporalDimensionRasterHelper helper =
                        new DimensionHelper.TemporalDimensionRasterHelper(dimInfo, accessor);

                TreeSet<Object> domain = helper.getDomain();
                String values = helper.getRepresentation(domain);
                Collection<String> dates = Arrays.asList(values.split(","));
                options.addStrings(dates);
            }

        } else if ("elevation".equalsIgnoreCase(dimension)) {
            options = initOptions(layerMetadata, metadataMap, ResourceInfo.ELEVATION);
            if (options.isAvailable()) {
                dimInfo = options.getInfo();
                accessor = getAccessor(cvInfo);

                DimensionHelper.ElevationDimensionRasterHelper helper =
                        new DimensionHelper.ElevationDimensionRasterHelper(dimInfo, accessor);

                TreeSet<Object> domain = helper.getDomain();
                String values = helper.getRepresentation(domain);
                Collection<String> elevations = Arrays.asList(values.split(","));
                options.addStrings(elevations);
            }
        }
        /*
        TODO:
        custom dimensions setting is not trivial due to the custom_dimension_ prefix.
        We need to properly manage it and make sure the options are properly propagated
        */
    }

    private ReaderDimensionsAccessor getAccessor(CoverageInfo cvInfo) throws IOException {
        GridCoverage2DReader reader = null;
        Catalog catalog = cvInfo.getCatalog();
        if (catalog == null)
            throw new ServiceException("Unable to acquire catalog resource for coverage: " + cvInfo.getName());

        CoverageStoreInfo csinfo = cvInfo.getStore();
        if (csinfo == null)
            throw new ServiceException("Unable to acquire coverage store resource for coverage: " + cvInfo.getName());

        try {
            reader = (GridCoverage2DReader) cvInfo.getGridCoverageReader(null, null);
        } catch (Throwable t) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to acquire a reader for this coverage with format: "
                            + csinfo.getFormat().getName(),
                    t);
        }
        if (reader == null) {
            throw new ServiceException("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());
        }
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        return accessor;
    }

    private DimensionOptions initOptions(MapMLLayerMetadata mapMLLayerMetadata, MetadataMap metadata, String dimName) {
        DimensionOptions options = new DimensionOptions(mapMLLayerMetadata, metadata, dimName);
        if (options.isAvailable()) {
            extentList.add(options.getSelect());
        }
        return options;
    }

    /**
     * Generate the JAXB Extent object contents: inputs and templated client links
     *
     * @param mapMLLayerMetadata MapMLLayerMetadata object
     */
    private void generateTemplatedLinks(MapMLLayerMetadata mapMLLayerMetadata) {
        if (mapMLLayerMetadata.isUseTiles()) {
            if (mapMLLayerMetadata.isTileLayerExists()) {
                generateWMTSClientLinks(mapMLLayerMetadata);
            } else {
                generateTiledWMSClientLinks(mapMLLayerMetadata);
            }
        } else {
            // will use full GetMap requests, no tiles involved
            generateWMSClientLinks(mapMLLayerMetadata);
        }

        // Query inputs: query links for WMS with images, and WMTS always
        // (for WMS features, the client is self sufficient, while it's not with tiled features yet)
        if (mapMLLayerMetadata.isQueryable()
                && (!mapMLLayerMetadata.isUseFeatures() || mapMLLayerMetadata.isUseTiles())) {
            if (mapMLLayerMetadata.isUseTiles() && mapMLLayerMetadata.isTileLayerExists()) {
                generateWMTSQueryClientLinks(mapMLLayerMetadata);
            } else {
                generateWMSQueryClientLinks(mapMLLayerMetadata);
            }
        }
    }

    /**
     * Generate inputs and templated links that the client will use to make WMTS tile requests
     *
     * @param mapMLLayerMetadata MapMLLayerMetadata object
     */
    private void generateWMTSClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {
        // emit MapML extent that uses TileMatrix coordinates, allowing
        // client requests for WMTS tiles (GetTile)
        LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
        CatalogInfo catalogInfo = null;
        if (layerInfo != null) {
            catalogInfo = layerInfo.getResource();
        } else {
            catalogInfo = mapMLLayerMetadata.getLayerGroupInfo();
        }
        GeoServerTileLayer gstl = gwc.getTileLayer(
                mapMLLayerMetadata.isLayerGroup() ? mapMLLayerMetadata.getLayerGroupInfo() : catalogInfo);
        GridSubset gss = gstl.getGridSubset(projType.value());

        long[][] minMax = gss.getWMTSCoverages();
        // zoom start/stop are the min/max published zoom levels
        zoomInput = (Input) extentList.get(0);
        // zoom value must be the same as that used to establish the axes min/max
        // on location inputs, below
        zoomInput.setValue(Integer.toString(gss.getZoomStop()));

        // tilematrix inputs
        Input input = new Input();
        input.setName("x");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setAxis(AxisType.COLUMN);
        input.setMin(Long.toString(minMax[minMax.length - 1][0]));
        input.setMax(Long.toString(minMax[minMax.length - 1][2]));
        // there's no way to specify min/max here because
        // the zoom is set by the client
        // need to specify min/max in pcrs or gcrs units
        // OR set the zoom value to the maximum and then
        // specify the min/max at that zoom level

        extentList.add(input);

        input = new Input();
        input.setName("y");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setAxis(AxisType.ROW);
        input.setMin(Long.toString(minMax[minMax.length - 1][1]));
        input.setMax(Long.toString(minMax[minMax.length - 1][3]));
        extentList.add(input);
        // tile link
        Link tileLink = new Link();
        tileLink.setRel(RelType.TILE);
        String path = "gwc/service/wmts";
        HashMap<String, String> params = new HashMap<>();
        params.put("layer", mapMLLayerMetadata.prefixedName());
        params.put("style", mapMLLayerMetadata.getStyleName());
        params.put("tilematrixset", projType.value());
        params.put("service", "WMTS");
        params.put("request", "GetTile");
        params.put("version", "1.0.0");
        params.put("tilematrix", "{z}");
        params.put("TileCol", "{x}");
        params.put("TileRow", "{y}");
        if (mapMLLayerMetadata.isUseFeatures()) {
            params.put("format", MAPML_MIME_TYPE);
            params.put(
                    "format_options",
                    MAPML_FEATURE_FO + ":true;" + MAPML_SKIP_ATTRIBUTES_FO + ":true;" + MAPML_SKIP_STYLES_FO + ":true");
            tileLink.setType(MimeType.TEXT_MAPML);
        } else {
            params.put("format", imageFormat);
        }
        setTimeParam(mapMLLayerMetadata, params, gstl);
        setElevationParam(mapMLLayerMetadata, params, gstl);
        setCustomDimensionParam(mapMLLayerMetadata, params, gstl);
        setCqlFilterParam(mapMLLayerMetadata, params);
        MapMLURLBuilder mangler =
                new MapMLURLBuilder(mapContent, mapMLLayerMetadata, baseUrlPattern, path, params, proj);
        String urlTemplate = mangler.getUrlTemplate();
        tileLink.setTref(urlTemplate);
        extentList.add(tileLink);
    }

    /**
     * Gnerate inputs and templated links that the client will use to make WMS requests for individual tiles i.e. a
     * GetMap for each 256x256 tile image
     */
    private void generateTiledWMSClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {
        // generateTiledWMSClientLinks
        // emit MapML extent that uses WMS GetMap requests to request tiles

        ReferencedEnvelope bbbox = null;
        if (mapMLLayerMetadata.getBbbox() != null) {
            bbbox = mapMLLayerMetadata.getBbbox();
        } else {
            // TODO the axis name should be gettable from the TCRS.
            // need an api like this, perhaps:
            // previewTcrsMap.get(projType.value()).getCRS(UnitType.PCRS).getAxis(AxisDirection.DISPLAY_RIGHT);
            // TODO what is the pcrs of WGS84 ? What are its units?
            // I believe the answer to the above question is that the PCRS
            // of WGS84 is a cartesian cs per the table on this page:
            // https://docs.geotools.org/stable/javadocs/org/opengis/referencing/cs/package-summary.html#AxisNames
            // input.setAxis(previewTcrsMap.get(projType.value()).getCRS(UnitType.PCRS).getAxisByDirection(AxisDirection.DISPLAY_RIGHT));
            bbbox = new ReferencedEnvelope(projType.getCRS());
            LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();

            try {
                bbbox = mapMLLayerMetadata.isLayerGroup()
                        ? mapMLLayerMetadata.getLayerGroupInfo().getBounds()
                        : layerInfo.getResource().boundingBox();
                bbbox = bbbox.transform(projType.getCRS(), true);
            } catch (Exception e) {
                // sometimes, when the geographicBox is right to 90N or 90S, in epsg:3857,
                // the transform method will throw. In that case, use the
                // bounds of the TCRS to define the geographicBox for the layer
                TiledCRS t = projType.getTiledCRS();
                double x1 = t.getBounds().getMax().x;
                double y1 = t.getBounds().getMax().y;
                double x2 = t.getBounds().getMin().x;
                double y2 = t.getBounds().getMin().y;
                bbbox = new ReferencedEnvelope(x1, x2, y1, y2, t.getCRS());
            }
        }

        // tile inputs
        // txmin
        Input input = new Input();
        input.setName("txmin");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.TILE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // tymin
        input = new Input();
        input.setName("tymin");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.BOTTOM_LEFT);
        input.setRel(InputRelType.TILE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // txmax
        input = new Input();
        input.setName("txmax");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.TOP_RIGHT);
        input.setRel(InputRelType.TILE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // tymax
        input = new Input();
        input.setName("tymax");
        input.setType(InputType.LOCATION);
        input.setUnits(UnitType.TILEMATRIX);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.TILE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // tile link
        Link tileLink = new Link();
        tileLink.setRel(RelType.TILE);
        String path = "wms";
        HashMap<String, String> params = new HashMap<>();
        params.put("version", "1.3.0");
        params.put("service", "WMS");
        params.put("request", "GetMap");
        params.put("crs", projType.getCRSCode());
        params.put("layers", mapMLLayerMetadata.getLayerName());
        params.put("language", this.request.getLocale().getLanguage());
        params.put("styles", mapMLLayerMetadata.getStyleName());
        setTimeParam(mapMLLayerMetadata, params, null);
        setElevationParam(mapMLLayerMetadata, params, null);
        setCustomDimensionParam(mapMLLayerMetadata, params, null);
        setCqlFilterParam(mapMLLayerMetadata, params);
        params.put("bbox", "{txmin},{tymin},{txmax},{tymax}");
        if (mapMLLayerMetadata.isUseFeatures()) {
            params.put("format", MAPML_MIME_TYPE);
            params.put(
                    "format_options",
                    MAPML_FEATURE_FO + ":true;" + MAPML_SKIP_ATTRIBUTES_FO + ":true;" + MAPML_SKIP_STYLES_FO + ":true");
            tileLink.setType(MimeType.TEXT_MAPML);
        } else {
            params.put("format", imageFormat);
        }
        params.put("transparent", Boolean.toString(mapMLLayerMetadata.isTransparent()));
        params.put("width", "256");
        params.put("height", "256");
        MapMLURLBuilder mangler =
                new MapMLURLBuilder(mapContent, mapMLLayerMetadata, baseUrlPattern, path, params, proj);
        String urlTemplate = mangler.getUrlTemplate();
        tileLink.setTref(urlTemplate);
        extentList.add(tileLink);
    }

    /** Generate inputs and links that the client will use to create WMS GetMap requests for full map images */
    public void generateWMSClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {
        // generateWMSClientLinks
        // emit MapML extent that uses WMS requests to request complete images
        LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
        ReferencedEnvelope bbbox = null;
        if (mapMLLayerMetadata.getBbbox() != null) {
            bbbox = mapMLLayerMetadata.getBbbox();
        } else {
            try {
                // initialization is necessary so as to set the PCRS to which
                // the resource's geographicBox will be transformed, below.
                bbbox = new ReferencedEnvelope(projType.getCRS());
                bbbox = mapMLLayerMetadata.isLayerGroup
                        ? mapMLLayerMetadata.getLayerGroupInfo().getBounds()
                        : layerInfo.getResource().boundingBox();
                // transform can cause an exception if the geographicBox coordinates fall
                // too near the pole (at least in OSMTILE, where the poles are
                // undefined/out of scope).
                // If it throws, we need to reset the projectedBox value so that its
                // crs is that of the underlying pcrs from the TCRS, because
                // the projectedBox.transform will leave the CRS set to that of whatever
                // was returned by layerInfo.getResource().boundingBox() or
                // layerGroupInfo.getBounds(), above.
                bbbox = bbbox.transform(projType.getCRS(), true);
            } catch (Exception e) {
                // get the default max/min of the pcrs from the TCRS
                Bounds defaultBounds = projType.getTiledCRS().getBounds();
                double x1, x2, y1, y2;
                x1 = defaultBounds.getMin().x;
                x2 = defaultBounds.getMax().x;
                y1 = defaultBounds.getMin().y;
                y2 = defaultBounds.getMax().y;
                // use the bounds of the TCRS as the default bounds for this layer
                bbbox = new ReferencedEnvelope(x1, x2, y1, y2, projType.getCRS());
            }
        }

        // image inputs
        // xmin
        Input input = new Input();
        input.setName("xmin");
        input.setType(InputType.LOCATION);
        input.setUnits(ProjType.WGS_84 == projType.unwrap() ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // ymin
        input = new Input();
        input.setName("ymin");
        input.setType(InputType.LOCATION);
        input.setUnits(ProjType.WGS_84 == projType.unwrap() ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.BOTTOM_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // xmax
        input = new Input();
        input.setName("xmax");
        input.setType(InputType.LOCATION);
        input.setUnits(ProjType.WGS_84 == projType.unwrap() ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_RIGHT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // ymax
        input = new Input();
        input.setName("ymax");
        input.setType(InputType.LOCATION);
        input.setUnits(ProjType.WGS_84 == projType.unwrap() ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(ProjType.WGS_84 == projType.unwrap() ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        createMinAndMaxWidthHeight();

        // image link
        Link imageLink = new Link();
        if (mapMLLayerMetadata.isUseFeatures()) {
            imageLink.setRel(RelType.FEATURES);
        } else {
            imageLink.setRel(RelType.IMAGE);
        }
        String path = "wms";
        HashMap<String, String> params = new HashMap<>();
        params.put("version", "1.3.0");
        params.put("service", "WMS");
        params.put("request", "GetMap");
        params.put("crs", projType.getCRSCode());
        params.put("layers", mapMLLayerMetadata.getLayerName());
        params.put("styles", mapMLLayerMetadata.getStyleName());
        setCqlFilterParam(mapMLLayerMetadata, params);
        setTimeParam(mapMLLayerMetadata, params, null);
        setElevationParam(mapMLLayerMetadata, params, null);
        setCustomDimensionParam(mapMLLayerMetadata, params, null);
        params.put("bbox", forceYX ? BBOX_PARAMS_YX : BBOX_PARAMS);
        if (mapMLLayerMetadata.isUseFeatures()) {
            params.put("format", MAPML_MIME_TYPE);
            params.put("format_options", MAPML_FEATURE_FO + ":true");
        } else {
            params.put("format", imageFormat);
        }
        params.put("transparent", Boolean.toString(mapMLLayerMetadata.isTransparent()));
        params.put("language", this.request.getLocale().getLanguage());
        params.put("width", "{w}");
        params.put("height", "{h}");
        MapMLURLBuilder mangler =
                new MapMLURLBuilder(mapContent, mapMLLayerMetadata, baseUrlPattern, path, params, proj);
        String urlTemplate = mangler.getUrlTemplate();
        imageLink.setTref(urlTemplate);
        extentList.add(imageLink);
    }

    private void createMinAndMaxWidthHeight() {
        String max = null;
        // Zero maxRequestMemory means no limit
        if (wms != null && wms.getMaxRequestMemory() != 0) {
            int maxMemory = wms.getMaxRequestMemory() * BYTES_PER_KILOBYTE;
            // square root because we assume a square image for the max calculation
            // 4 bytes per pixel @see ImageUtils#getDrawingSurfaceMemoryUse, assume worst case
            // scenario, which is transparent
            double memoryDividedByBytes = maxMemory / (double) BYTES_PER_PIXEL_TRANSPARENT;
            Long maxPixelsPerDimension = (long) Math.sqrt(memoryDividedByBytes);
            max = maxPixelsPerDimension.toString();
        }

        Input inputWidth;
        // width
        inputWidth = new Input();
        inputWidth.setName("w");
        inputWidth.setType(InputType.WIDTH);
        inputWidth.setMin(MINIMUM_WIDTH_HEIGHT);
        inputWidth.setMax(max);
        extentList.add(inputWidth);

        Input inputHeight;
        // height
        inputHeight = new Input();
        inputHeight.setName("h");
        inputHeight.setType(InputType.HEIGHT);
        inputHeight.setMin(MINIMUM_WIDTH_HEIGHT);
        inputHeight.setMax(max);
        extentList.add(inputHeight);
    }

    /** Generate inputs and links that the client will use to generate WMTS GetFeatureInfo requests */
    private void generateWMTSQueryClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {

        // query link
        Link queryLink = new Link();
        queryLink.setRel(RelType.QUERY);
        String path = "gwc/service/wmts";
        HashMap<String, String> params = new HashMap<>();
        params.put("layer", mapMLLayerMetadata.prefixedName());
        params.put("tilematrix", "{z}");
        params.put("TileCol", "{x}");
        params.put("TileRow", "{y}");
        params.put("tilematrixset", projType.value());
        params.put("service", "WMTS");
        params.put("version", "1.0.0");
        params.put("request", "GetFeatureInfo");
        params.put("feature_count", "50");
        params.put("format", imageFormat);
        params.put("style", mapMLLayerMetadata.getStyleName());
        params.put("infoformat", "text/mapml");
        params.put("i", "{i}");
        params.put("j", "{j}");
        MapMLURLBuilder mangler =
                new MapMLURLBuilder(mapContent, mapMLLayerMetadata, baseUrlPattern, path, params, proj);
        String urlTemplate = mangler.getUrlTemplate();
        // It may be that the mangler decided to not generate any query URL due
        // to unsupported info formats from the remote layer. So we are not
        // generating the query link.
        if (urlTemplate != null) {
            // query i value (x)
            Input input = new Input();
            input.setName("i");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.TILE);
            input.setAxis(AxisType.I);
            extentList.add(input);

            // query j value (y)
            input = new Input();
            input.setName("j");
            input.setType(InputType.LOCATION);
            input.setUnits(UnitType.TILE);
            input.setAxis(AxisType.J);
            extentList.add(input);

            queryLink.setTref(urlTemplate);
            extentList.add(queryLink);
        }
    }

    /** Generate inputs and links the client will use to create WMS GetFeatureInfo requests */
    private void generateWMSQueryClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {
        UnitType units = UnitType.MAP;
        if (mapMLLayerMetadata.isUseTiles()) {
            units = UnitType.TILE;
        }

        // query link
        Link queryLink = new Link();
        queryLink.setRel(RelType.QUERY);
        String path = "wms";
        HashMap<String, String> params = new HashMap<>();
        params.put("version", "1.3.0");
        params.put("service", "WMS");
        params.put("request", "GetFeatureInfo");
        params.put("feature_count", "50");
        params.put("crs", projType.getCRSCode());
        params.put("language", this.request.getLocale().getLanguage());
        params.put("layers", mapMLLayerMetadata.getLayerName());
        params.put("query_layers", mapMLLayerMetadata.getLayerName());
        params.put("styles", mapMLLayerMetadata.getStyleName());
        if (StringUtils.isNotBlank(mapMLLayerMetadata.getCqlFilter())) {
            params.put("cql_filter", mapMLLayerMetadata.getCqlFilter());
        }
        setTimeParam(mapMLLayerMetadata, params, null);
        setElevationParam(mapMLLayerMetadata, params, null);
        setCustomDimensionParam(mapMLLayerMetadata, params, null);

        if (mapMLLayerMetadata.isUseTiles()) {
            params.put("bbox", "{txmin},{tymin},{txmax},{tymax}");
            params.put("width", "256");
            params.put("height", "256");
        } else {
            params.put("bbox", forceYX ? BBOX_PARAMS_YX : BBOX_PARAMS);
            params.put("width", "{w}");
            params.put("height", "{h}");
        }
        params.put("info_format", "text/mapml");
        params.put("transparent", Boolean.toString(mapMLLayerMetadata.isTransparent()));
        params.put("x", "{i}");
        params.put("y", "{j}");
        MapMLURLBuilder mangler =
                new MapMLURLBuilder(mapContent, mapMLLayerMetadata, baseUrlPattern, path, params, proj);
        String urlTemplate = mangler.getUrlTemplate();
        // It may be that the mangler decided to not generate any query URL due
        // to unsupported info formats from the remote layer. So we are not
        // generating the query link.
        if (urlTemplate != null) {
            // query i value (x)
            Input input = new Input();
            input.setName("i");
            input.setType(InputType.LOCATION);
            input.setUnits(units);
            input.setAxis(AxisType.I);
            extentList.add(input);

            // query j value (y)
            input = new Input();
            input.setName("j");
            input.setType(InputType.LOCATION);
            input.setUnits(units);
            input.setAxis(AxisType.J);
            extentList.add(input);

            queryLink.setTref(urlTemplate);
            extentList.add(queryLink);
        }
    }

    private void setCqlFilterParam(MapMLLayerMetadata mapMLLayerMetadata, HashMap<String, String> params) {
        if (cqlFilter.isPresent()) {
            params.put("cql_filter", mapMLLayerMetadata.getCqlFilter());
        }
    }

    private void setElevationParam(
            MapMLLayerMetadata mapMLLayerMetadata, HashMap<String, String> params, GeoServerTileLayer tileLayer) {
        if (mapMLLayerMetadata.isElevationEnabled() && checkTileLayerParam(tileLayer, "elevation")) {
            params.put("elevation", "{elevation}");
        }
    }

    private void setTimeParam(
            MapMLLayerMetadata mapMLLayerMetadata, HashMap<String, String> params, GeoServerTileLayer tileLayer) {
        if (mapMLLayerMetadata.isTimeEnabled() && checkTileLayerParam(tileLayer, "time")) {
            params.put("time", "{time}");
        }
    }

    private void setCustomDimensionParam(
            MapMLLayerMetadata mapMLLayerMetadata, HashMap<String, String> params, GeoServerTileLayer tileLayer) {
        String customDimension = mapMLLayerMetadata.getCustomDimension();
        if (StringUtils.isNotBlank(customDimension) && checkTileLayerParam(tileLayer, customDimension)) {
            params.put(customDimension, "{" + customDimension + "}");
        }
    }

    private boolean checkTileLayerParam(GeoServerTileLayer tileLayer, String name) {
        // If no GeoServerTileLayer has been provided, we don't need to check it and
        // we can set the param.
        if (tileLayer == null) return true;
        // If a GeoServerTileLayer has been provided, we need to check if it contains
        // a filter to deal with that specific dimension and only in that case we add
        // the param.
        List<ParameterFilter> paramFilters = tileLayer.getParameterFilters();
        for (ParameterFilter param : paramFilters) {
            if (name.equalsIgnoreCase(param.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the MapML HTML document
     *
     * @return String
     */
    public String getMapMLHTMLDocument() {
        initialize();
        String layerLabel = "";
        String layer = "";
        String styleName = "";
        String cqlFilter = "";
        Double latitude = 0.0;
        Double longitude = 0.0;
        ReferencedEnvelope projectedBbox = this.projectedBox;
        String transparent = this.transparent.map(Object::toString).orElse("true");
        try {
            // odd preview lat/lon for non-orthogonal projections e.g. LCC
            // IN SOME CASES, particularly remote/cascaded layers where the
            // bounds don't tightly "fit" the data, per GEOS-11801
            Position2D destPos = new Position2D();
            MathTransform transform = CRS.findMathTransform(
                    projectedBbox.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84, true);
            CRS.AxisOrder axisOrder = CRS.getAxisOrder(projectedBbox.getCoordinateReferenceSystem());
            boolean xy = (axisOrder == CRS.AxisOrder.EAST_NORTH);
            Position2D projectedCenter = new Position2D(
                    projectedBbox.getCoordinateReferenceSystem(),
                    xy ? projectedBbox.getCenterX() : projectedBbox.getCenterY(),
                    xy ? projectedBbox.getCenterY() : projectedBbox.getCenterX());
            transform.transform(projectedCenter, destPos);
            longitude = destPos.getX();
            latitude = destPos.getY();
        } catch (TransformException | FactoryException e) {
            throw new ServiceException("Unable to transform bbox to WGS84", e);
        }
        List<String> headerContent = getPreviewTemplates(MAPML_PREVIEW_HEAD_FTL, getFeatureTypes());
        for (MapMLLayerMetadata mapMLLayerMetadata : mapMLLayerMetadataList) {
            layer += mapMLLayerMetadata.getLayerName() + ",";
            styleName += mapMLLayerMetadata.getStyleName() + ",";
            cqlFilter += mapMLLayerMetadata.getCqlFilter() + ",";
            // bbbox and layerLabel precomputed from multiple layers
            if (mapMLLayerMetadata.getBbbox() != null) {
                layerLabel = mapMLLayerMetadata.getLayerLabel();
            } else {
                if (mapMLLayerMetadata.isLayerGroup()) {
                    layerLabel +=
                            getLabel(mapMLLayerMetadata.getLayerGroupInfo(), mapMLLayerMetadata.getLayerName(), request)
                                    + ",";

                } else {
                    layerLabel +=
                            getLabel(mapMLLayerMetadata.getLayerInfo(), mapMLLayerMetadata.getLayerName(), request)
                                    + ",";
                }
            }
        }
        // remove trailing commas
        layerLabel = layerLabel.replaceAll(",$", "");
        layer = layer.replaceAll(",$", "");
        styleName = styleName.replaceAll(",$", "");
        cqlFilter = cqlFilter.replaceAll(",$", "");
        // if all commas, set to empty string
        if (ALL_COMMAS.matcher(styleName).matches()) {
            styleName = "";
        }
        if (ALL_COMMAS.matcher(cqlFilter).matches()) {
            cqlFilter = "";
        }
        MapMLHTMLOutput htmlOutput = new MapMLHTMLOutput.HTMLOutputBuilder()
                .setSourceUrL(buildGetMap(
                        layer,
                        projectedBbox,
                        width,
                        height,
                        escapeHtml4(proj),
                        styleName,
                        format,
                        transparent,
                        cqlFilter,
                        useTiles,
                        useFeatures))
                .setProjType(projType)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setRequest(request)
                .setProjectedBbox(projectedBbox)
                .setLayerLabel(layerLabel)
                .setTemplateHeader(String.join("\n", headerContent))
                .build();
        return htmlOutput.toHTML();
    }

    /**
     * Get FeatureTypes based on requested layers
     *
     * @return list of SimpleFeatureType
     */
    private List<SimpleFeatureType> getFeatureTypes() {
        List<SimpleFeatureType> featureTypes = new ArrayList<>();
        try {
            for (MapLayerInfo mapLayerInfo : mapContent.getRequest().getLayers()) {
                if (mapLayerInfo.getType() == MapLayerInfo.TYPE_VECTOR
                        && mapLayerInfo.getFeature() != null
                        && mapLayerInfo.getFeature().getFeatureType() != null
                        && mapLayerInfo.getFeature().getFeatureType() instanceof SimpleFeatureType) {
                    featureTypes.add(
                            (SimpleFeatureType) mapLayerInfo.getFeature().getFeatureType());
                } else if (mapLayerInfo.getType() == MapLayerInfo.TYPE_RASTER) {
                    LOGGER.fine("Templating not supported for raster layers: " + mapLayerInfo.getName());
                }
            }
        } catch (IOException | ClassCastException e) {
            LOGGER.fine("Error getting feature types: " + e.getMessage());
        }
        return featureTypes;
    }

    /**
     * Get Preview Header Content from templates
     *
     * @param templateName template name
     * @param featureTypes list of feature types
     * @return list of head content
     */
    private List<String> getPreviewTemplates(String templateName, List<SimpleFeatureType> featureTypes) {
        List<String> templates = new ArrayList<>();
        for (SimpleFeatureType featureType : featureTypes) {
            try {
                if (!mapMLMapTemplate.isTemplateEmpty(featureType, templateName, FeatureTemplate.class, "0\n")) {
                    templates.add(mapMLMapTemplate.preview(featureType));
                }

            } catch (IOException e) {
                LOGGER.fine("Template not found: " + templateName + " for schema: " + featureType.getTypeName());
            }
        }
        return templates;
    }

    /**
     * Get the MapML head content from templates
     *
     * @param templateName template name
     * @param featureTypes list of feature types
     * @return list of head content
     */
    private List<String> getHeaderTemplates(String templateName, List<SimpleFeatureType> featureTypes) {
        List<String> templates = new ArrayList<>();

        for (SimpleFeatureType featureType : featureTypes) {
            try {
                Map<String, Object> model =
                        getMapRequestElementsToModel(layersCommaDelimited, bbox, format, width, height);
                if (!mapMLMapTemplate.isTemplateEmpty(featureType, templateName, FeatureTemplate.class, "0\n")) {
                    templates.add(mapMLMapTemplate.head(model, featureType));
                }

            } catch (IOException e) {
                LOGGER.fine("Template not found: " + templateName + " for schema: " + featureType.getTypeName());
            }
        }
        return templates;
    }

    /** Builds the GetMap backlink to get MapML */
    private String buildGetMap(
            String layer,
            ReferencedEnvelope projectedBbox,
            int height,
            int width,
            String proj,
            String styleName,
            Optional<Object> format,
            String transparent,
            String cqlFilter,
            boolean useTiles,
            boolean useFeatures) {
        Map<String, String> kvp = new LinkedHashMap<>();
        kvp.put("LAYERS", escapeHtml4(layer));
        kvp.put("BBOX", toCommaDelimitedBbox(projectedBbox));
        kvp.put("HEIGHT", String.valueOf(height));
        kvp.put("WIDTH", String.valueOf(width));
        kvp.put("SRS", escapeHtml4(proj));
        kvp.put("STYLES", escapeHtml4(styleName));
        if (cqlFilter != null && !cqlFilter.isEmpty()) {
            kvp.put("CQL_FILTER", cqlFilter);
        }
        kvp.put("FORMAT", MAPML_MIME_TYPE);
        kvp.put("TRANSPARENT", transparent);
        boolean skipAttributes = useTiles && useFeatures;
        String formatOptions =
                MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION + ":" + escapeHtml4((String) format.orElse(imageFormat)) + ";"
                        + MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT + ":" + isMultiExtent + ";"
                        + MAPML_USE_TILES_REP + ":" + useTiles + ";" + MAPML_CREATE_FEATURE_LINKS + ":" + useFeatures
                        + ";" + MAPML_SKIP_ATTRIBUTES_FO + ":" + skipAttributes + ";";
        kvp.put("format_options", formatOptions);
        kvp.put("SERVICE", "WMS");
        kvp.put("REQUEST", "GetMap");
        kvp.put("VERSION", "1.3.0");
        return ResponseUtils.buildURL(baseUrl, "wms", kvp, URLMangler.URLType.SERVICE);
    }

    /**
     * Get the potentially localized label string for a layer or layer group
     *
     * @param p LayerInfo or LayerGroupInfo object
     * @param def default label string, usually pass in the layer name
     * @param request the localized servlet request
     * @return the potentially localized label string for a layer or layer group
     */
    String getLabel(PublishedInfo p, String def, HttpServletRequest request) {
        if (p instanceof LayerGroupInfo) {
            LayerGroupInfo li = (LayerGroupInfo) p;
            if (li.getInternationalTitle() != null
                    && li.getInternationalTitle().toString(request.getLocale()) != null) {
                // use international title per request or default locale
                return li.getInternationalTitle().toString(request.getLocale());
            } else if (li.getTitle() != null && !li.getTitle().trim().isEmpty()) {
                return li.getTitle().trim();
            } else {
                return li.getName().trim().isEmpty() ? def : li.getName().trim();
            }
        } else {
            LayerInfo li = (LayerInfo) p;
            if (li.getInternationalTitle() != null
                    && li.getInternationalTitle().toString(request.getLocale()) != null) {
                // use international title per request or default locale
                return li.getInternationalTitle().toString(request.getLocale());
            } else if (li.getTitle() != null && !li.getTitle().trim().isEmpty()) {
                return li.getTitle().trim();
            } else {
                return li.getName().trim().isEmpty() ? def : li.getName().trim();
            }
        }
    }

    /**
     * Converts URL query string to a map of key value pairs
     *
     * @param query URL query string
     * @return Map of key value pairs
     */
    private Map<String, String> getParametersFromQuery(String query) {
        return Arrays.stream(query.split("&"))
                .map(this::splitQueryParameter)
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (v1, v2) -> v2));
    }

    private AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String parameter) {
        final int idx = parameter.indexOf("=");
        final String key = idx > 0 ? parameter.substring(0, idx) : parameter;

        try {
            String value = null;
            if (idx > 0 && parameter.length() > idx + 1) {
                final String encodedValue = parameter.substring(idx + 1);
                value = URLDecoder.decode(encodedValue, "UTF-8");
            }
            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        } catch (UnsupportedEncodingException e) {
            // UTF-8 not supported??
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a link from the arguments passed into the template
     *
     * @param arguments List of arguments, the first argument is the base URL, the second is the path, and the third is
     *     the query string
     * @return URL string
     */
    private String serviceLink(List arguments) {
        Request request = Dispatcher.REQUEST.get();
        String baseURL = arguments.get(0) != null
                ? arguments.get(0).toString()
                : ResponseUtils.baseURL(request.getHttpRequest());
        Map<String, String> kvp = arguments.get(2) != null
                ? getParametersFromQuery(arguments.get(2).toString())
                : request.getKvp().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .toString()));

        return ResponseUtils.buildURL(baseURL, request.getPath(), kvp, URLMangler.URLType.SERVICE);
    }

    /**
     * Convert GetMapRequest elements to a map model for the template
     *
     * @param layersCommaDelimited Comma delimited list of layer names
     * @param bbox
     * @param format
     * @param width
     * @param height
     * @return
     */
    private Map<String, Object> getMapRequestElementsToModel(
            String layersCommaDelimited, String bbox, Optional<Object> format, int width, int height) {
        HashMap<String, Object> model = new HashMap<>();
        Request request = Dispatcher.REQUEST.get();
        String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        String kvp = request.getKvp().entrySet().stream()
                .map(entry -> {
                    if (entry.getValue() instanceof Map) {
                        Map<?, ?> internalMap = (Map<?, ?>) entry.getValue();
                        String internalKvp = internalMap.entrySet().stream()
                                .filter(e -> !e.getKey().toString().isEmpty())
                                .map(e -> URLEncoder.encode(e.getKey().toString(), StandardCharsets.UTF_8)
                                        + "="
                                        + URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8))
                                .reduce((p1, p2) -> p1 + "&" + p2)
                                .orElse("");
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "={" + internalKvp + "}";
                    } else {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                                + "="
                                + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8);
                    }
                })
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");
        String path = request.getPath();
        model.put("base", baseURL);
        model.put("path", path);
        model.put("kvp", kvp);
        model.put("rel", "style");
        model.put("serviceLink", (TemplateMethodModelEx) arguments -> serviceLink(arguments));
        return model;
    }

    /** Raw KVP layer info */
    static class RawLayer {

        private String title;
        private String name;
        private String layerGroupTitle;
        private String layerGroupName;

        /**
         * Get the layer name
         *
         * @return String
         */
        public String getName() {
            return name;
        }

        /**
         * set the layer name
         *
         * @param name String
         */
        public void setName(String name) {
            this.name = name;
        }

        private PublishedInfo publishedInfo;
        private boolean isLayerGroup = false;

        /**
         * get if the layer is a layer group
         *
         * @return boolean
         */
        public boolean isLayerGroup() {
            return isLayerGroup;
        }

        /** set if the layer is a layer group */
        public void setLayerGroup(boolean layerGroup) {
            isLayerGroup = layerGroup;
        }

        /**
         * get the PublishedInfo object
         *
         * @return PublishedInfo
         */
        public PublishedInfo getPublishedInfo() {
            return publishedInfo;
        }

        /**
         * set the PublishedInfo object
         *
         * @param publishedInfo PublishedInfo object
         */
        public void setPublishedInfo(PublishedInfo publishedInfo) {
            this.publishedInfo = publishedInfo;
        }

        /**
         * get the layer title
         *
         * @return String
         */
        public String getTitle() {
            return title;
        }

        /**
         * set the layer title
         *
         * @param title String
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * get the layer group title
         *
         * @return String
         */
        public String getLayerGroupTitle() {
            return layerGroupTitle;
        }
        /** set the layer group name */
        public void setLayerGroupTitle(String layerGroupTitle) {
            this.layerGroupTitle = layerGroupTitle;
        }
        /**
         * set the layergroup name
         *
         * @param layerGroupName String
         */
        public void setLayerGroupName(String layerGroupName) {
            this.layerGroupName = layerGroupName;
        }
        /**
         * get the layer group name
         *
         * @return String
         */
        public String getLayerGroupName() {
            return layerGroupName;
        }
    }

    static class DimensionOptions {

        DimensionInfo info;
        List<Option> options;
        Select select;
        boolean available;

        public DimensionOptions(MapMLLayerMetadata mapMLLayerMetadata, MetadataMap metadata, String name) {
            info = metadata.get(name, DimensionInfo.class);
            if (info != null && info.isEnabled()) {
                if ("elevation".equalsIgnoreCase(name)) {
                    mapMLLayerMetadata.setElevationEnabled(true);
                } else if ("time".equalsIgnoreCase(name)) {
                    mapMLLayerMetadata.setTimeEnabled(true);
                } else {
                    mapMLLayerMetadata.setCustomDimension(name);
                }
                available = true;
                select = new Select();
                select.setId(name);
                select.setName(name);
                options = select.getOptions();
            }
        }

        public DimensionInfo getInfo() {
            return info;
        }

        public List<Option> getOptions() {
            return options;
        }

        public Select getSelect() {
            return select;
        }

        public boolean isAvailable() {
            return available;
        }

        public void addStrings(Collection<String> values) {
            for (String value : values) {
                Option o = new Option();
                o.setContent(value);
                options.add(o);
            }
        }

        public void addDates(Collection<Date> dates) {
            for (Date date : dates) {
                Option o = new Option();
                o.setContent(new SimpleDateFormat(DATE_FORMAT).format(date));
                options.add(o);
            }
        }

        public void addDoubles(Collection<Double> values) {
            for (Double value : values) {
                Option o = new Option();
                o.setContent(value.toString());
                options.add(o);
            }
        }

        public void addNumbers(Collection<Number> values) {
            for (Number value : values) {
                Option o = new Option();
                o.setContent(value.toString());
                options.add(o);
            }
        }
    }

    /** MapML layer metadata */
    static class MapMLLayerMetadata {
        private String cqlFilter;
        private boolean useFeatures;
        private LayerInfo layerInfo;
        private ReferencedEnvelope bbox;
        private boolean isLayerGroup;
        private LayerGroupInfo layerGroupInfo;
        private MetadataMap layerMeta;
        private String workspace;
        private boolean isQueryable;
        private boolean isTransparent;
        private String layerName;
        private String layerTitle;
        private MapMLProjection projType;
        private String styleName;
        private boolean tileLayerExists;

        private boolean useTiles;
        private boolean useRemote;

        private boolean timeEnabled;
        private boolean elevationEnabled;
        private String customDimension;

        private ReferencedEnvelope bbbox;

        private String layerLabel;
        private String defaultMimeType;
        private String legendURL;
        /**
         * get if the layer uses features
         *
         * @return
         */
        public boolean isUseFeatures() {
            return useFeatures;
        }

        /**
         * set if the layer uses features
         *
         * @param useFeatures boolean
         */
        public void setUseFeatures(boolean useFeatures) {
            this.useFeatures = useFeatures;
        }

        /**
         * Constructor
         *
         * @param layerInfo LayerInfo object
         * @param bbox ReferencedEnvelope object
         * @param isLayerGroup boolean
         * @param layerGroupInfo LayerGroupInfo object
         * @param layerMeta MetadataMap object
         * @param workspace String
         * @param isQueryable boolean
         * @param isTransparent boolean
         * @param layerName String
         * @param layerTitle String
         * @param projType ProjType
         * @param styleName String
         * @param tileLayerExists boolean
         * @param useTiles boolean
         * @param defaultMimeType String
         */
        public MapMLLayerMetadata(
                LayerInfo layerInfo,
                ReferencedEnvelope bbox,
                boolean isLayerGroup,
                LayerGroupInfo layerGroupInfo,
                MetadataMap layerMeta,
                String workspace,
                boolean isQueryable,
                boolean isTransparent,
                String layerName,
                String layerTitle,
                MapMLProjection projType,
                String styleName,
                boolean tileLayerExists,
                boolean useTiles,
                boolean useRemote,
                boolean useFeatures,
                String cqFilter,
                String defaultMimeType,
                String legendURL) {
            this.layerInfo = layerInfo;
            this.bbox = bbox;
            this.isLayerGroup = isLayerGroup;
            this.layerGroupInfo = layerGroupInfo;
            this.layerMeta = layerMeta;
            this.workspace = workspace;
            this.isQueryable = isQueryable;
            this.layerName = layerName;
            this.layerTitle = layerTitle;
            this.projType = projType;
            this.styleName = styleName;
            this.isTransparent = isTransparent;
            this.tileLayerExists = tileLayerExists;
            this.useTiles = useTiles;
            this.useRemote = useRemote;
            this.useFeatures = useFeatures;
            this.cqlFilter = cqFilter;
            this.defaultMimeType = defaultMimeType;
            this.legendURL = legendURL;
        }

        /** Constructor */
        public MapMLLayerMetadata() {
            // empty constructor
        }

        /**
         * get name with workspace prefix
         *
         * @return
         */
        public String prefixedName() {
            return (getWorkspace() == null || getWorkspace().isEmpty() ? "" : getWorkspace() + ":") + getLayerName();
        }

        /**
         * get if the layer has elevation enabled
         *
         * @return boolean
         */
        public boolean isElevationEnabled() {
            return elevationEnabled;
        }

        /**
         * set if the layer has elevation enabled
         *
         * @param elevationEnabled
         */
        public void setElevationEnabled(boolean elevationEnabled) {
            this.elevationEnabled = elevationEnabled;
        }

        /**
         * get if the layer has time enabled
         *
         * @return
         */
        public boolean isTimeEnabled() {
            return timeEnabled;
        }

        /**
         * set if the layer has time enabled
         *
         * @param timeEnabled
         */
        public void setTimeEnabled(boolean timeEnabled) {
            this.timeEnabled = timeEnabled;
        }

        /**
         * get the layer's enabled custom dimension (if any)
         *
         * @return customDimension
         */
        public String getCustomDimension() {
            return customDimension;
        }

        /**
         * set the enabled customDimension
         *
         * @param customDimension
         */
        public void setCustomDimension(String customDimension) {
            this.customDimension = customDimension;
        }

        /**
         * get if the layer has a tile layer
         *
         * @return
         */
        public boolean isTileLayerExists() {
            return tileLayerExists;
        }

        /**
         * set if the layer has a tile layer
         *
         * @param tileLayerExists
         */
        public void setTileLayerExists(boolean tileLayerExists) {
            this.tileLayerExists = tileLayerExists;
        }

        /**
         * get if the layer is transparent
         *
         * @return boolean
         */
        public boolean isTransparent() {
            return isTransparent;
        }

        /**
         * set if the layer is transparent
         *
         * @param transparent boolean
         */
        public void setTransparent(boolean transparent) {
            isTransparent = transparent;
        }

        /**
         * get the LayerInfo object
         *
         * @return LayerInfo
         */
        public LayerInfo getLayerInfo() {
            return layerInfo;
        }

        /**
         * set the LayerInfo object
         *
         * @param layerInfo LayerInfo
         */
        public void setLayerInfo(LayerInfo layerInfo) {
            this.layerInfo = layerInfo;
        }

        /**
         * get the ReferencedEnvelope object
         *
         * @return ReferencedEnvelope
         */
        public ReferencedEnvelope getBbox() {
            return bbox;
        }

        /**
         * set the ReferencedEnvelope object
         *
         * @param bbox ReferencedEnvelope
         */
        public void setBbox(ReferencedEnvelope bbox) {
            this.bbox = bbox;
        }

        /**
         * get if the layer is a layer group
         *
         * @return boolean
         */
        public boolean isLayerGroup() {
            return isLayerGroup;
        }

        /**
         * set if the layer is a layer group
         *
         * @param layerGroup boolean
         */
        public void setLayerGroup(boolean layerGroup) {
            isLayerGroup = layerGroup;
        }

        /**
         * get the LayerGroupInfo object
         *
         * @return LayerGroupInfo
         */
        public LayerGroupInfo getLayerGroupInfo() {
            return layerGroupInfo;
        }

        /**
         * set the LayerGroupInfo object
         *
         * @param layerGroupInfo LayerGroupInfo
         */
        public void setLayerGroupInfo(LayerGroupInfo layerGroupInfo) {
            this.layerGroupInfo = layerGroupInfo;
        }

        /**
         * get the MetadataMap object
         *
         * @return MetadataMap
         */
        public MetadataMap getLayerMeta() {
            return layerMeta;
        }

        /**
         * set the MetadataMap object
         *
         * @param layerMeta MetadataMap
         */
        public void setLayerMeta(MetadataMap layerMeta) {
            this.layerMeta = layerMeta;
        }

        /**
         * get the workspace
         *
         * @return String
         */
        public String getWorkspace() {
            return workspace;
        }

        /**
         * set the workspace
         *
         * @param workspace String
         */
        public void setWorkspace(String workspace) {
            this.workspace = workspace;
        }

        /**
         * get if the layer is queryable
         *
         * @return boolean
         */
        public boolean isQueryable() {
            return isQueryable;
        }

        /**
         * set if the layer is queryable
         *
         * @param queryable boolean
         */
        public void setQueryable(boolean queryable) {
            isQueryable = queryable;
        }

        /**
         * get the layer name
         *
         * @return String
         */
        public String getLayerName() {
            return layerName;
        }

        /**
         * set the layer name
         *
         * @param layerName String
         */
        public void setLayerName(String layerName) {
            this.layerName = layerName;
        }

        /**
         * get the layer title
         *
         * @return String
         */
        public String getLayerTitle() {
            return layerTitle;
        }

        /**
         * set the layer title
         *
         * @param layerTitle String
         */
        public void setLayerTitle(String layerTitle) {
            this.layerTitle = layerTitle;
        }

        /**
         * get the projection type
         *
         * @return ProjType
         */
        public MapMLProjection getProjType() {
            return projType;
        }

        /**
         * set the projection type
         *
         * @param projType ProjType
         */
        public void setProjType(MapMLProjection projType) {
            this.projType = projType;
        }

        /**
         * get the style name
         *
         * @return String
         */
        public String getStyleName() {
            return styleName;
        }

        /**
         * set the style name
         *
         * @param styleName String
         */
        public void setStyleName(String styleName) {
            this.styleName = styleName;
        }

        /**
         * get if the layer uses tiles
         *
         * @return boolean
         */
        public boolean isUseTiles() {
            return useTiles;
        }

        /**
         * set if the layer uses tiles
         *
         * @param useTiles boolean
         */
        public void setUseTiles(boolean useTiles) {
            this.useTiles = useTiles;
        }

        /**
         * get if the layer uses remote
         *
         * @return boolean
         */
        public boolean isUseRemote() {
            return useRemote;
        }

        /**
         * set if the layer uses remote
         *
         * @param useRemote boolean
         */
        public void setUseRemote(boolean useRemote) {
            this.useRemote = useRemote;
        }

        /**
         * get the ReferencedEnvelope object
         *
         * @return ReferencedEnvelope
         */
        public ReferencedEnvelope getBbbox() {
            return bbbox;
        }

        /**
         * set the ReferencedEnvelope object
         *
         * @param bbbox ReferencedEnvelope
         */
        public void setBbbox(ReferencedEnvelope bbbox) {
            this.bbbox = bbbox;
        }

        /**
         * get the layer tag
         *
         * @return String
         */
        public String getLayerLabel() {
            return layerLabel;
        }

        /**
         * set the layer tag
         *
         * @param layerLabel String
         */
        public void setLayerLabel(String layerLabel) {
            this.layerLabel = layerLabel;
        }

        /**
         * get the cql filter
         *
         * @return String
         */
        public String getCqlFilter() {
            return cqlFilter;
        }

        /**
         * set the cql filter
         *
         * @param cqlFilter String
         */
        public void setCqlFilter(String cqlFilter) {
            this.cqlFilter = cqlFilter;
        }

        /**
         * get the default mime type
         *
         * @return String
         */
        public String getDefaultMimeType() {
            return defaultMimeType;
        }

        /**
         * get the legend URL
         *
         * @return String
         */
        public String getLegendURL() {
            return legendURL;
        }
        /**
         * set the default mime type
         *
         * @param defaultMimeType String
         */
        public void setDefaultMimeType(String defaultMimeType) {
            this.defaultMimeType = defaultMimeType;
        }
    }

    public static boolean isWMSOrWMTSStore(LayerInfo layerInfo) {
        if (layerInfo != null) {
            ResourceInfo resourceInfo = layerInfo.getResource();
            if (resourceInfo != null) {
                StoreInfo storeInfo = resourceInfo.getStore();
                return storeInfo instanceof WMSStoreInfo || storeInfo instanceof WMTSStoreInfo;
            }
        }
        return false;
    }
}
