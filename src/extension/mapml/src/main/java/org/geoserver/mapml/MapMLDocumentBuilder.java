/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.geoserver.mapml.MapMLConstants.DATE_FORMAT;
import static org.geoserver.mapml.MapMLConstants.MAPML_FEATURE_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_MIME_TYPE;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_ATTRIBUTES_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_STYLES_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.tcrs.Point;
import org.geoserver.mapml.tcrs.TiledCRS;
import org.geoserver.mapml.xml.AxisType;
import org.geoserver.mapml.xml.Base;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Datalist;
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
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.Style;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.GridSubset;
import org.locationtech.jts.geom.Envelope;

/** Builds a MapML document from a WMSMapContent object */
public class MapMLDocumentBuilder {
    private static final Logger LOGGER = Logging.getLogger(MapMLDocumentBuilder.class);
    private static final Bounds DISPLAY_BOUNDS_DESKTOP_LANDSCAPE =
            new Bounds(new Point(0, 0), new Point(768, 1024));

    private static final Pattern ALL_COMMAS = Pattern.compile("^,+$");
    public static final HashMap<String, TiledCRS> PREVIEW_TCRS_MAP = new HashMap<>();

    /**
     * The key for the metadata entry that controls whether a multi-layer request is rendered as a
     * single extent or multiple extents.
     */
    public static final String MAPML_MULTILAYER_AS_MULTIEXTENT = "mapmlMultiLayerAsMultiExtent";

    protected static final Boolean MAPML_MULTILAYER_AS_MULTIEXTENT_DEFAULT = Boolean.FALSE;
    public static final String MINIMUM_WIDTH_HEIGHT = "1";
    private static final int BYTES_PER_PIXEL_TRANSPARENT = 4;
    private static final int BYTES_PER_KILOBYTE = 1024;
    public static final String DEFAULT_MIME_TYPE = "image/png";

    private final WMS wms;

    private final GeoServer geoServer;

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
    private Boolean enableSharding;
    private String[] shardArray;
    private ProjType projType;
    private MetadataMap layerMeta;
    private int height;
    private int width;
    private ReferencedEnvelope projectedBox;
    private String bbox;

    private List<Object> extentList;

    private Input zoomInput;

    private List<MapMLLayerMetadata> mapMLLayerMetadataList = new ArrayList<>();

    private Mapml mapml;

    private Boolean isMultiExtent = MAPML_MULTILAYER_AS_MULTIEXTENT_DEFAULT;

    static {
        PREVIEW_TCRS_MAP.put("OSMTILE", new TiledCRS("OSMTILE"));
        PREVIEW_TCRS_MAP.put("CBMTILE", new TiledCRS("CBMTILE"));
        PREVIEW_TCRS_MAP.put("APSTILE", new TiledCRS("APSTILE"));
        PREVIEW_TCRS_MAP.put("WGS84", new TiledCRS("WGS84"));
    }

    /**
     * Constructor
     *
     * @param mapContent WMSMapContent object
     * @param wms WMS object
     * @param request HttpServletRequest object
     */
    public MapMLDocumentBuilder(
            WMSMapContent mapContent, WMS wms, GeoServer geoServer, HttpServletRequest request) {
        this.wms = wms;
        this.geoServer = geoServer;
        this.request = request;
        this.mapContent = mapContent;
        GetMapRequest getMapRequest = mapContent.getRequest();
        String rawLayersCommaDL = getMapRequest.getRawKvp().get("layers");
        this.layers = toRawLayers(rawLayersCommaDL);
        this.stylesCommaDelimited =
                getMapRequest.getRawKvp().get("styles") != null
                        ? getMapRequest.getRawKvp().get("styles")
                        : "";
        styles =
                Optional.ofNullable(
                        stylesCommaDelimited.isEmpty()
                                ? null
                                : Arrays.asList(stylesCommaDelimited.split(",", -1)));
        this.cqlCommadDelimited =
                getMapRequest.getRawKvp().get("cql_filter") != null
                        ? getMapRequest.getRawKvp().get("cql_filter")
                        : "";
        cqlFilter =
                Optional.ofNullable(
                        cqlCommadDelimited.isEmpty()
                                ? null
                                : Arrays.asList(cqlCommadDelimited.split(";", -1)));
        this.proj = getMapRequest.getSRS();
        this.height = getMapRequest.getHeight();
        this.width = getMapRequest.getWidth();
        this.bbox = toCommaDelimitedBbox(getMapRequest.getBbox());
        this.projectedBox = new ReferencedEnvelope(getMapRequest.getBbox(), getMapRequest.getCrs());
        this.transparent =
                Optional.ofNullable(
                        getMapRequest.getRawKvp().get("transparent") == null
                                ? null
                                : Boolean.valueOf(getMapRequest.getRawKvp().get("transparent")));
        this.format = getFormat(getMapRequest);
        this.layersCommaDelimited =
                layers.stream().map(RawLayer::getName).collect(Collectors.joining(","));
        this.layerTitlesCommaDelimited =
                layers.stream().map(RawLayer::getTitle).collect(Collectors.joining(","));
    }

    /**
     * Convert Envelope to comma delimited string
     *
     * @param bbox Envelope object
     * @return comma delimited string
     */
    private String toCommaDelimitedBbox(Envelope bbox) {
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
            RawLayer rawLayer = new RawLayer();
            if (layerInfo == null) {
                LayerGroupInfo layerGroupInfo = wms.getLayerGroupByName(rawLayerTitle);
                rawLayer.setTitle(getTitle(layerGroupInfo, rawLayerTitle));
                rawLayer.setName(layerGroupInfo.getName());
                rawLayer.setLayerGroup(true);
                rawLayer.setPublishedInfo(layerGroupInfo);
            } else {
                rawLayer.setTitle(getTitle(layerInfo, rawLayerTitle));
                rawLayer.setLayerGroup(false);
                rawLayer.setName(layerInfo.getName());
                rawLayer.setPublishedInfo(layerInfo);
            }
            rawLayers.add(rawLayer);
        }
        return rawLayers;
    }

    /**
     * Get the format from the GetMapRequest object
     *
     * @param getMapRequest GetMapRequest object
     * @return Optional<Object> containing the format
     */
    private Optional<Object> getFormat(GetMapRequest getMapRequest) {
        return Optional.ofNullable(
                getMapRequest.getFormatOptions().get(MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION));
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
        WMSInfo wmsInfo = geoServer.getService(WMSInfo.class);
        isMultiExtent =
                wmsInfo.getMetadata().get(MAPML_MULTILAYER_AS_MULTIEXTENT, Boolean.class) != null
                        ? wmsInfo.getMetadata().get(MAPML_MULTILAYER_AS_MULTIEXTENT, Boolean.class)
                        : MAPML_MULTILAYER_AS_MULTIEXTENT_DEFAULT;
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
                        throw new ServiceException(
                                "Number of styles does not match number of layers");
                    }
                }
                if (cqlFilter.isPresent()) {
                    try {
                        cql = cqlFilter.get().get(i);
                    } catch (IndexOutOfBoundsException e) {
                        // if there are more layers than cql filters
                        throw new ServiceException(
                                "Number of cql filters does not match number of layers");
                    }
                }
                MapMLLayerMetadata mapMLLayerMetadata =
                        layerToMapMLLayerMetadata(layer, style, cql);
                mapMLLayerMetadataList.add(mapMLLayerMetadata);
            }
        } else {
            MapMLLayerMetadata mapMLLayerMetadata = layersToOneMapMLLayerMetadata(layers);
            mapMLLayerMetadataList.add(mapMLLayerMetadata);
        }
        // populate Map-wide variables using the first layer
        if (!mapMLLayerMetadataList.isEmpty()) {
            defaultStyle =
                    stylesCommaDelimited == null || stylesCommaDelimited.isEmpty()
                            ? getDefaultLayerStyles(mapMLLayerMetadataList)
                            : stylesCommaDelimited;
            MapMLLayerMetadata mapMLLayerMetadata = mapMLLayerMetadataList.get(0);
            projType = mapMLLayerMetadata.getProjType();
            layerTitle = layerTitlesCommaDelimited;
            layerMeta = mapMLLayerMetadata.getLayerMeta();
            imageFormat = (String) format.orElse(mapMLLayerMetadata.getDefaultMimeType());
            baseUrl = ResponseUtils.baseURL(request);
            baseUrlPattern = baseUrl;
            // handle shard config
            enableSharding = layerMeta.get("mapml.enableSharding", Boolean.class);
            String shardListString = layerMeta.get("mapml.shardList", String.class);
            shardArray = new String[0];
            if (shardListString != null) {
                shardArray = shardListString.split("[,\\s]+");
            }
            String shardServerPattern = layerMeta.get("mapml.shardServerPattern", String.class);
            if (shardArray.length < 1
                    || shardServerPattern == null
                    || shardServerPattern.isEmpty()) {
                enableSharding = Boolean.FALSE;
            }
            // if we have a valid shard config
            if (Boolean.TRUE.equals(enableSharding)) {
                baseUrlPattern = shardBaseURL(request, shardServerPattern);
            }
        }
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
                if (layerGroupInfo != null && !layerGroupInfo.getLayerGroupStyles().isEmpty()) {
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
        mapMLLayerMetadata.setUseTiles(false);
        boolean useFeatures = false;
        if (layers.size() == 1) {
            useFeatures =
                    useFeatures(layers.get(0), layers.get(0).getPublishedInfo().getMetadata());
        }
        mapMLLayerMetadata.setUseFeatures(useFeatures);
        mapMLLayerMetadata.setLayerName(layersCommaDelimited);
        mapMLLayerMetadata.setStyleName(stylesCommaDelimited);
        mapMLLayerMetadata.setCqlFilter(cqlCommadDelimited);
        mapMLLayerMetadata.setTimeEnabled(false);
        mapMLLayerMetadata.setElevationEnabled(false);
        mapMLLayerMetadata.setTransparent(transparent.orElse(false));
        ProjType projType = parseProjType();
        mapMLLayerMetadata.setBbbox(layersToBBBox(layers, projType));
        mapMLLayerMetadata.setQueryable(layersToQueryable(layers));
        mapMLLayerMetadata.setLayerLabel(layersToLabel(layers));
        mapMLLayerMetadata.setProjType(projType);
        mapMLLayerMetadata.setDefaultMimeType(imageFormat);

        return mapMLLayerMetadata;
    }

    /**
     * Parses the projection into a ProjType, or throws a proper service exception indicating the
     * unsupported CRS
     */
    private ProjType parseProjType() {
        try {
            return ProjType.fromValue(proj.toUpperCase());
        } catch (IllegalArgumentException | FactoryException iae) {
            // figure out the parameter name (version dependent) and the actual original
            // string value for the srs/crs parameter
            String parameterName =
                    Optional.ofNullable(mapContent.getRequest().getVersion())
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
    private ReferencedEnvelope layersToBBBox(List<RawLayer> layers, ProjType projType) {

        ReferencedEnvelope bbbox;
        bbbox = new ReferencedEnvelope(PREVIEW_TCRS_MAP.get(projType.value()).getCRS());
        for (int i = 0; i < layers.size(); i++) {
            RawLayer layer = layers.get(i);
            try {
                ReferencedEnvelope layerBbbox =
                        layer.isLayerGroup()
                                ? ((LayerGroupInfo) layer.getPublishedInfo()).getBounds()
                                : ((LayerInfo) layer.getPublishedInfo())
                                        .getResource()
                                        .boundingBox();
                if (i == 0) {
                    bbbox =
                            layerBbbox.transform(
                                    PREVIEW_TCRS_MAP.get(projType.value()).getCRS(), true);
                } else {
                    bbbox.expandToInclude(
                            layerBbbox.transform(
                                    PREVIEW_TCRS_MAP.get(projType.value()).getCRS(), true));
                }
            } catch (Exception e) {
                // get the default max/min of the pcrs from the TCRS
                Bounds defaultBounds = PREVIEW_TCRS_MAP.get(projType.value()).getBounds();
                double x1, x2, y1, y2;
                x1 = defaultBounds.getMin().x;
                x2 = defaultBounds.getMax().x;
                y1 = defaultBounds.getMin().y;
                y2 = defaultBounds.getMax().y;
                // use the bounds of the TCRS as the default bounds for this layer
                bbbox =
                        new ReferencedEnvelope(
                                x1, x2, y1, y2, PREVIEW_TCRS_MAP.get(projType.value()).getCRS());
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
            workspace =
                    (layerGroupInfo.getWorkspace() != null
                            ? layerGroupInfo.getWorkspace().getName()
                            : "");
            queryable = !layerGroupInfo.isQueryDisabled();
            layerName = layerGroupInfo.getName();
            layerTitle = getTitle(layerGroupInfo, layerName);
            defaultMimeType =
                    Optional.ofNullable(layerGroupInfo.getMetadata().get(MapMLConstants.MAPML_MIME))
                            .orElse(DEFAULT_MIME_TYPE)
                            .toString();
        } else {
            layerInfo = (LayerInfo) layer.getPublishedInfo();
            resourceInfo = layerInfo.getResource();
            bbox = layerInfo.getResource().getLatLonBoundingBox();
            layerMeta = resourceInfo.getMetadata();
            workspace =
                    (resourceInfo.getStore().getWorkspace() != null
                            ? resourceInfo.getStore().getWorkspace().getName()
                            : "");
            queryable = layerInfo.isQueryable();
            isTransparent = transparent.orElse(!layerInfo.isOpaque());
            layerName = layerInfo.getName().isEmpty() ? layer.getTitle() : layerInfo.getName();
            layerTitle = getTitle(layerInfo, layerName);
            // set the actual style name from the layer info
            if (style == null) styleName = layerInfo.getDefaultStyle().getName();
            defaultMimeType =
                    Optional.ofNullable(resourceInfo.getMetadata().get(MapMLConstants.MAPML_MIME))
                            .orElse(DEFAULT_MIME_TYPE)
                            .toString();
        }
        ProjType projType = parseProjType();
        cqlFilter = cql != null ? cql : "";
        tileLayerExists =
                gwc.hasTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                        && gwc.getTileLayer(isLayerGroup ? layerGroupInfo : layerInfo)
                                        .getGridSubset(projType.value())
                                != null;
        boolean useTiles = Boolean.TRUE.equals(layerMeta.get(MAPML_USE_TILES, Boolean.class));
        boolean useFeatures = useFeatures(layer, layerMeta);

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
                useFeatures,
                cqlFilter,
                defaultMimeType);
    }

    /**
     * Check if layer should be represented as a feature
     *
     * @param layer RawLayer
     * @param layerMeta MetadataMap for layer
     * @return boolean
     */
    private static boolean useFeatures(RawLayer layer, MetadataMap layerMeta) {
        return (Boolean.TRUE.equals(layerMeta.get(MAPML_USE_FEATURES, Boolean.class)))
                && (PublishedType.VECTOR == layer.getPublishedInfo().getType());
    }

    /**
     * Match the CRS of the layer to the CRS of the bbox
     *
     * @param li LayerInfo
     * @param bbox ReferencedEnvelope with CRS
     * @return ReferencedEnvelope with matching CRS
     */
    private static ReferencedEnvelope matchReferencedEnvelopeCRS(
            LayerInfo li, ReferencedEnvelope bbox) {
        ReferencedEnvelope referencedEnvelope = li.getResource().getLatLonBoundingBox();
        if (!CRS.equalsIgnoreMetadata(
                bbox.getCoordinateReferenceSystem(),
                referencedEnvelope.getCoordinateReferenceSystem())) {
            try {
                referencedEnvelope =
                        referencedEnvelope.transform(bbox.getCoordinateReferenceSystem(), true);
            } catch (TransformException | FactoryException e) {
                throw new ServiceException(
                        "Unable to transform layer bounds to WGS84 for layer" + li.getName());
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
                return li.getName().trim().isEmpty() ? defaultTitle : li.getName().trim();
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
                return li.getName().trim().isEmpty() ? defaultTitle : li.getName().trim();
            }
        }
    }

    /**
     * @param req the request
     * @param shardServerPattern the shard server pattern
     * @return the shard base URL
     */
    private String shardBaseURL(HttpServletRequest req, String shardServerPattern) {
        StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://")
                .append(shardServerPattern)
                .append(":")
                .append(req.getServerPort())
                .append(req.getContextPath())
                .append("/");
        return sb.toString();
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
        wmsParams.put(
                "format_options", MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION + ":" + imageFormat);
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
                    String url =
                            ResponseUtils.buildURL(
                                    baseUrl, "wms", styleParams, URLMangler.URLType.SERVICE);
                    styleLink.setHref(url);
                    links.add(styleLink);
                }
            } else {
                LayerGroupInfo layerGroupInfo = mapMLLayerMetadata.getLayerGroupInfo();
                String effectiveStyleName = mapMLLayerMetadata.getStyleName();
                if (effectiveStyleName == null || effectiveStyleName.isEmpty()) {
                    effectiveStyleName = "default-style-" + mapMLLayerMetadata.getLayerName();
                }
                StyleInfo si;
                for (LayerGroupStyle layerGroupStyle : layerGroupInfo.getLayerGroupStyles()) {
                    si = layerGroupStyle.getName();
                    // Copy the base params to create one for each style
                    Map<String, String> styleParams = new HashMap<>(wmsParams);
                    // skip the self style case (if it is even listed)
                    if (layerGroupStyle.getName().getName().equalsIgnoreCase(effectiveStyleName))
                        continue;
                    Link styleLink = new Link();
                    styleLink.setRel(RelType.STYLE);
                    styleLink.setTitle(si.getName());
                    styleParams.put("styles", si.getName());
                    if (cqlFilter.isPresent()) styleParams.put("cql_filter", cqlCommadDelimited);
                    styleParams.put("width", Integer.toString(width));
                    styleParams.put("height", Integer.toString(height));
                    styleParams.put("bbox", bbox);
                    String url =
                            ResponseUtils.buildURL(
                                    baseUrl, "wms", styleParams, URLMangler.URLType.SERVICE);
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
                stylesCommaDelimited == null || stylesCommaDelimited.isEmpty()
                        ? defaultStyle
                        : stylesCommaDelimited);
        // Copy the base params to create one for self style
        Map<String, String> selfStyleParams = new HashMap<>(wmsParams);
        selfStyleParams.put(
                "styles",
                stylesCommaDelimited == null || stylesCommaDelimited.isEmpty()
                        ? defaultStyle
                        : stylesCommaDelimited);
        selfStyleParams.put("width", Integer.toString(width));
        selfStyleParams.put("height", Integer.toString(height));
        selfStyleParams.put("bbox", bbox);
        String selfStyleURL =
                ResponseUtils.buildURL(baseUrl, "wms", selfStyleParams, URLMangler.URLType.SERVICE);
        selfStyleLink.setHref(selfStyleURL);
        links.add(selfStyleLink);
        // alternate projection links
        for (ProjType pt : ProjType.values()) {
            // skip the current proj
            if (pt.equals(projType)) continue;
            try {
                Link projectionLink = new Link();
                projectionLink.setRel(RelType.ALTERNATE);
                projectionLink.setProjection(pt);
                // reproject the bounds
                ReferencedEnvelope reprojectedBounds = reproject(projectedBox, pt);
                // Copy the base params to create one for self style
                Map<String, String> projParams = new HashMap<>(wmsParams);
                projParams.put("crs", pt.getCRSCode());
                projParams.put("width", Integer.toString(width));
                projParams.put("height", Integer.toString(height));
                projParams.put("bbox", toCommaDelimitedBbox(reprojectedBounds));
                String projURL =
                        ResponseUtils.buildURL(
                                baseUrl, "wms", projParams, URLMangler.URLType.SERVICE);
                projectionLink.setHref(projURL);
                links.add(projectionLink);
            } catch (Exception e) {
                // we gave it our best try but reprojection failed anyways, log and skip this link
                LOGGER.log(Level.INFO, "Unable to reproject bounds for " + pt.value(), e);
            }
        }
        String styles = buildStyles();
        if (styles != null) head.setStyle(styles);
        return head;
    }

    /** Builds the CSS styles for all the layers involved in this GetMap */
    private String buildStyles() throws IOException {
        List<String> cssStyles = new ArrayList<>();
        for (MapMLLayerMetadata mapMLLayerMetadata : mapMLLayerMetadataList) {
            if (!mapMLLayerMetadata.isLayerGroup()) {
                String styleName = mapMLLayerMetadata.getStyleName();
                Style style = wms.getStyleByName(styleName);
                if (style != null) {
                    Map<String, MapMLStyle> styles =
                            MapMLFeatureUtil.getMapMLStyleMap(
                                    style, mapContent.getScaleDenominator());
                    String css = MapMLFeatureUtil.getCSSStyles(styles);
                    cssStyles.add(css);
                } else {
                    LOGGER.log(Level.INFO, "Could not find style named " + styleName);
                }
            }
        }
        if (cssStyles.isEmpty()) return null;
        return MapMLFeatureUtil.BBOX_DISPLAY_NONE + " " + String.join(" ", cssStyles);
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
    private ReferencedEnvelope reproject(ReferencedEnvelope bounds, ProjType pt)
            throws FactoryException, TransformException {
        CoordinateReferenceSystem targetCRS = PREVIEW_TCRS_MAP.get(pt.value()).getCRS();
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
            if (isMultiExtent) {
                extent.setHidden(null); // not needed for multi-extent
                extent.setLabel(mapMLLayerMetadata.layerTitle);
            }
            extent.setUnits(projType);
            extentList = extent.getInputOrDatalistOrLink();

            // zoom
            NumberRange<Double> scaleDenominators = null;
            // layerInfo is null when layer is a layer group or multi layer request for multi-extent
            if (!mapMLLayerMetadata.isLayerGroup() && mapMLLayerMetadata.getLayerInfo() != null) {
                scaleDenominators =
                        CapabilityUtil.searchMinMaxScaleDenominator(
                                mapMLLayerMetadata.getLayerInfo());
            } else if (mapMLLayerMetadata.getLayerGroupInfo() != null) {
                scaleDenominators =
                        CapabilityUtil.searchMinMaxScaleDenominator(
                                mapMLLayerMetadata.getLayerGroupInfo());
            }

            Input extentZoomInput = new Input();
            TiledCRS tiledCRS = PREVIEW_TCRS_MAP.get(projType.value());
            extentZoomInput.setName("z");
            extentZoomInput.setType(InputType.ZOOM);
            // passing in max sld denominator to get min zoom
            extentZoomInput.setMin(
                    scaleDenominators != null
                            ? String.valueOf(
                                    tiledCRS.getMinZoomForDenominator(
                                            scaleDenominators.getMaxValue().intValue()))
                            : "0");
            int mxz = PREVIEW_TCRS_MAP.get(projType.value()).getScales().length - 1;
            // passing in min sld denominator to get max zoom
            String maxZoom =
                    scaleDenominators != null
                            ? String.valueOf(
                                    tiledCRS.getMaxZoomForDenominator(
                                            scaleDenominators.getMinValue().intValue()))
                            : String.valueOf(mxz);
            extentZoomInput.setMax(maxZoom);
            extentList.add(extentZoomInput);

            Input input;
            // shard list
            if (Boolean.TRUE.equals(enableSharding)) {
                input = new Input();
                input.setName("s");
                input.setType(InputType.HIDDEN);
                input.setShard("true");
                input.setList("servers");
                extentList.add(input);
                Datalist datalist = new Datalist();
                datalist.setId("servers");
                List<Option> options = datalist.getOptions();
                for (String sa : shardArray) {
                    Option o = new Option();
                    o.setValue(sa);
                    options.add(o);
                }
                extentList.add(datalist);
            }

            String dimension = layerMeta.get("mapml.dimension", String.class);
            prepareExtentForLayer(mapMLLayerMetadata, dimension);
            generateTemplatedLinks(mapMLLayerMetadata);
            extents.add(extent);
        }

        return extents;
    }

    /**
     * Prepare the extent for a layer
     *
     * @param mapMLLayerMetadata MapMLLayerMetadata object
     * @param dimension dimension name
     * @throws IOException In the event of an I/O error.
     */
    private void prepareExtentForLayer(MapMLLayerMetadata mapMLLayerMetadata, String dimension)
            throws IOException {
        if (dimension == null || mapMLLayerMetadata.isLayerGroup()) {
            return;
        }
        LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();
        ResourceInfo resourceInfo = layerInfo.getResource();
        if ("Time".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo timeInfo =
                        typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo.isEnabled()) {
                    mapMLLayerMetadata.setTimeEnabled(true);
                    Set<Date> dates = wms.getFeatureTypeTimes(typeInfo);
                    Select select = new Select();
                    select.setId("time");
                    select.setName("time");
                    extentList.add(select);
                    List<Option> options = select.getOptions();
                    for (Date date : dates) {
                        Option o = new Option();
                        o.setContent(new SimpleDateFormat(DATE_FORMAT).format(date));
                        options.add(o);
                    }
                }
            }
        } else if ("Elevation".equalsIgnoreCase(dimension)) {
            if (resourceInfo instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) resourceInfo;
                DimensionInfo elevInfo =
                        typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
                if (elevInfo.isEnabled()) {
                    mapMLLayerMetadata.setElevationEnabled(true);
                    Set<Double> elevs = wms.getFeatureTypeElevations(typeInfo);
                    Select select = new Select();
                    select.setId("elevation");
                    select.setName("elevation");
                    extentList.add(select);
                    List<Option> options = select.getOptions();
                    for (Double elev : elevs) {
                        Option o = new Option();
                        o.setContent(elev.toString());
                        options.add(o);
                    }
                }
            }
        }
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

        GeoServerTileLayer gstl =
                gwc.getTileLayer(
                        mapMLLayerMetadata.isLayerGroup()
                                ? mapMLLayerMetadata.getLayerGroupInfo()
                                : layerInfo.getResource());
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
                    MAPML_FEATURE_FO
                            + ":true;"
                            + MAPML_SKIP_ATTRIBUTES_FO
                            + ":true;"
                            + MAPML_SKIP_STYLES_FO
                            + ":true");
            tileLink.setType(MimeType.TEXT_MAPML);
        } else {
            params.put("format", imageFormat);
        }
        if (mapMLLayerMetadata.isTimeEnabled()) {
            params.put("time", "{time}");
        }
        if (mapMLLayerMetadata.isElevationEnabled()) {
            params.put("elevation", "{elevation}");
        }
        if (cqlFilter.isPresent()) params.put("cql_filter", mapMLLayerMetadata.getCqlFilter());
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        tileLink.setTref(urlTemplate);
        extentList.add(tileLink);
    }

    /**
     * Gnerate inputs and templated links that the client will use to make WMS requests for
     * individual tiles i.e. a GetMap for each 256x256 tile image
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
            bbbox = new ReferencedEnvelope(PREVIEW_TCRS_MAP.get(projType.value()).getCRS());
            LayerInfo layerInfo = mapMLLayerMetadata.getLayerInfo();

            try {
                bbbox =
                        mapMLLayerMetadata.isLayerGroup()
                                ? mapMLLayerMetadata.getLayerGroupInfo().getBounds()
                                : layerInfo.getResource().boundingBox();
                bbbox = bbbox.transform(PREVIEW_TCRS_MAP.get(projType.value()).getCRS(), true);
            } catch (Exception e) {
                // sometimes, when the geographicBox is right to 90N or 90S, in epsg:3857,
                // the transform method will throw. In that case, use the
                // bounds of the TCRS to define the geographicBox for the layer
                TiledCRS t = PREVIEW_TCRS_MAP.get(projType.value());
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
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
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
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
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
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
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
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
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
        if (mapMLLayerMetadata.isTimeEnabled()) {
            params.put("time", "{time}");
        }
        if (mapMLLayerMetadata.isElevationEnabled()) {
            params.put("elevation", "{elevation}");
        }
        if (cqlFilter.isPresent()) params.put("cql_filter", mapMLLayerMetadata.getCqlFilter());
        params.put("bbox", "{txmin},{tymin},{txmax},{tymax}");
        if (mapMLLayerMetadata.isUseFeatures()) {
            params.put("format", MAPML_MIME_TYPE);
            params.put(
                    "format_options",
                    MAPML_FEATURE_FO
                            + ":true;"
                            + MAPML_SKIP_ATTRIBUTES_FO
                            + ":true;"
                            + MAPML_SKIP_STYLES_FO
                            + ":true");
            tileLink.setType(MimeType.TEXT_MAPML);
        } else {
            params.put("format", imageFormat);
        }
        params.put("transparent", Boolean.toString(mapMLLayerMetadata.isTransparent()));
        params.put("width", "256");
        params.put("height", "256");
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        tileLink.setTref(urlTemplate);
        extentList.add(tileLink);
    }

    /**
     * Generate inputs and links that the client will use to create WMS GetMap requests for full map
     * images
     */
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
                bbbox = new ReferencedEnvelope(PREVIEW_TCRS_MAP.get(projType.value()).getCRS());
                bbbox =
                        mapMLLayerMetadata.isLayerGroup
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
                bbbox = bbbox.transform(PREVIEW_TCRS_MAP.get(projType.value()).getCRS(), true);
            } catch (Exception e) {
                // get the default max/min of the pcrs from the TCRS
                Bounds defaultBounds = PREVIEW_TCRS_MAP.get(projType.value()).getBounds();
                double x1, x2, y1, y2;
                x1 = defaultBounds.getMin().x;
                x2 = defaultBounds.getMax().x;
                y1 = defaultBounds.getMin().y;
                y2 = defaultBounds.getMax().y;
                // use the bounds of the TCRS as the default bounds for this layer
                bbbox =
                        new ReferencedEnvelope(
                                x1, x2, y1, y2, PREVIEW_TCRS_MAP.get(projType.value()).getCRS());
            }
        }

        // image inputs
        // xmin
        Input input = new Input();
        input.setName("xmin");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // ymin
        input = new Input();
        input.setName("ymin");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.BOTTOM_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
        input.setMin(Double.toString(bbbox.getMinY()));
        input.setMax(Double.toString(bbbox.getMaxY()));
        extentList.add(input);

        // xmax
        input = new Input();
        input.setName("xmax");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_RIGHT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LONGITUDE : AxisType.EASTING);
        input.setMin(Double.toString(bbbox.getMinX()));
        input.setMax(Double.toString(bbbox.getMaxX()));
        extentList.add(input);

        // ymax
        input = new Input();
        input.setName("ymax");
        input.setType(InputType.LOCATION);
        input.setUnits(projType == projType.WGS_84 ? UnitType.GCRS : UnitType.PCRS);
        input.setPosition(PositionType.TOP_LEFT);
        input.setRel(InputRelType.IMAGE);
        input.setAxis(projType == projType.WGS_84 ? AxisType.LATITUDE : AxisType.NORTHING);
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
        if (cqlFilter.isPresent()) params.put("cql_filter", mapMLLayerMetadata.getCqlFilter());
        if (mapMLLayerMetadata.isTimeEnabled()) {
            params.put("time", "{time}");
        }
        if (mapMLLayerMetadata.isElevationEnabled()) {
            params.put("elevation", "{elevation}");
        }
        params.put("bbox", "{xmin},{ymin},{xmax},{ymax}");
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
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
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

    /**
     * Generate inputs and links that the client will use to generate WMTS GetFeatureInfo requests
     */
    private void generateWMTSQueryClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {
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
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        queryLink.setTref(urlTemplate);
        extentList.add(queryLink);
    }

    /** Generate inputs and links the client will use to create WMS GetFeatureInfo requests */
    private void generateWMSQueryClientLinks(MapMLLayerMetadata mapMLLayerMetadata) {
        UnitType units = UnitType.MAP;
        if (mapMLLayerMetadata.isUseTiles()) {
            units = UnitType.TILE;
        }
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
        if (mapMLLayerMetadata.getCqlFilter() != null) {
            params.put("cql_filter", mapMLLayerMetadata.getCqlFilter());
        }
        if (mapMLLayerMetadata.isTimeEnabled()) {
            params.put("time", "{time}");
        }
        if (mapMLLayerMetadata.isElevationEnabled()) {
            params.put("elevation", "{elevation}");
        }
        if (mapMLLayerMetadata.isUseTiles()) {
            params.put("bbox", "{txmin},{tymin},{txmax},{tymax}");
            params.put("width", "256");
            params.put("height", "256");
        } else {
            params.put("bbox", "{xmin},{ymin},{xmax},{ymax}");
            params.put("width", "{w}");
            params.put("height", "{h}");
        }
        params.put("info_format", "text/mapml");
        params.put("transparent", Boolean.toString(mapMLLayerMetadata.isTransparent()));
        params.put("x", "{i}");
        params.put("y", "{j}");
        String urlTemplate = "";
        try {
            urlTemplate =
                    URLDecoder.decode(
                            ResponseUtils.buildURL(
                                    baseUrlPattern, path, params, URLMangler.URLType.SERVICE),
                            "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        queryLink.setTref(urlTemplate);
        extentList.add(queryLink);
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
        int zoom = 0;
        Double latitude = 0.0;
        Double longitude = 0.0;
        ReferencedEnvelope projectedBbox = this.projectedBox;
        ReferencedEnvelope geographicBox = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        TiledCRS tcrs = PREVIEW_TCRS_MAP.get(projType.value());
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
                            getLabel(
                                            mapMLLayerMetadata.getLayerGroupInfo(),
                                            mapMLLayerMetadata.getLayerName(),
                                            request)
                                    + ",";

                } else {
                    layerLabel +=
                            getLabel(
                                            mapMLLayerMetadata.getLayerInfo(),
                                            mapMLLayerMetadata.getLayerName(),
                                            request)
                                    + ",";
                }
            }
            try {
                geographicBox = projectedBbox.transform(DefaultGeographicCRS.WGS84, true);
                longitude = geographicBox.centre().getX();
                latitude = geographicBox.centre().getY();
            } catch (TransformException | FactoryException e) {
                throw new ServiceException("Unable to transform bbox to WGS84", e);
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
        final Bounds pb =
                new Bounds(
                        new Point(projectedBbox.getMinX(), projectedBbox.getMinY()),
                        new Point(projectedBbox.getMaxX(), projectedBbox.getMaxY()));
        // allowing for the data to be displayed at 1024x768 pixels, figure out
        // the zoom level at which the projected bounds fits into 1024x768
        // in both dimensions
        zoom = tcrs.fitProjectedBoundsToDisplay(pb, DISPLAY_BOUNDS_DESKTOP_LANDSCAPE);
        String base = ResponseUtils.baseURL(request);
        String viewerPath =
                ResponseUtils.buildURL(
                        base,
                        "/mapml/viewer/widget/mapml-viewer.js",
                        null,
                        URLMangler.URLType.RESOURCE);
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<title>")
                .append(escapeHtml4(layerLabel))
                .append("</title>\n")
                .append("<meta charset='utf-8'>\n")
                .append("<script type=\"module\"  src=\"")
                .append(viewerPath)
                .append("\"></script>\n")
                .append("<style>\n")
                .append("html, body { height: 100%; }\n")
                .append("* { margin: 0; padding: 0; }\n")
                .append(
                        "mapml-viewer:defined { max-width: 100%; width: 100%; height: 100%; border: none; vertical-align: middle }\n")
                .append("mapml-viewer:not(:defined) > * { display: none; } n")
                .append("layer- { display: none; }\n")
                .append("</style>\n")
                .append("<noscript>\n")
                .append("<style>\n")
                .append("mapml-viewer:not(:defined) > :not(layer-) { display: initial; }\n")
                .append("</style>\n")
                .append("</noscript>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<mapml-viewer projection=\"")
                .append(projType.value())
                .append("\" ")
                .append("zoom=\"")
                .append(zoom)
                .append("\" lat=\"")
                .append(latitude)
                .append("\" ")
                .append("lon=\"")
                .append(longitude)
                .append("\" controls controlslist=\"geolocation\">\n")
                .append("<layer- label=\"")
                .append(escapeHtml4(layerLabel))
                .append("\" ")
                .append("src=\"")
                .append(
                        buildGetMap(
                                layer,
                                projectedBbox,
                                width,
                                height,
                                escapeHtml4(proj),
                                styleName,
                                format,
                                cqlFilter))
                .append("\" checked></layer->\n")
                .append("</mapml-viewer>\n")
                .append("</body>\n")
                .append("</html>");
        return sb.toString();
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
            String cqlFilter) {
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
        String formatOptions =
                MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                        + ":"
                        + escapeHtml4((String) format.orElse(imageFormat));
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

    /** Raw KVP layer info */
    static class RawLayer {

        private String title;
        private String name;

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
        private ProjType projType;
        private String styleName;
        private boolean tileLayerExists;

        private boolean useTiles;

        private boolean timeEnabled;
        private boolean elevationEnabled;

        private ReferencedEnvelope bbbox;

        private String layerLabel;
        private String defaultMimeType;

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
         * @param projType ProjType object
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
                ProjType projType,
                String styleName,
                boolean tileLayerExists,
                boolean useTiles,
                boolean useFeatures,
                String cqFilter,
                String defaultMimeType) {
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
            this.useFeatures = useFeatures;
            this.cqlFilter = cqFilter;
            this.defaultMimeType = defaultMimeType;
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
            return (getWorkspace() == null || getWorkspace().isEmpty() ? "" : getWorkspace() + ":")
                    + getLayerName();
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
        public ProjType getProjType() {
            return projType;
        }

        /**
         * set the projection type
         *
         * @param projType ProjType
         */
        public void setProjType(ProjType projType) {
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
         * set the default mime type
         *
         * @param defaultMimeType String
         */
        public void setDefaultMimeType(String defaultMimeType) {
            this.defaultMimeType = defaultMimeType;
        }
    }
}
