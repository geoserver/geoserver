/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.geoserver.mapml.MapMLConstants.MAPML_FEATURE_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_ATTRIBUTES_FO;
import static org.geoserver.mapml.MapMLConstants.MAPML_SKIP_STYLES_FO;
import static org.geoserver.mapml.MapMLDocumentBuilder.extractCRS;
import static org.geoserver.mapml.MapMLDocumentBuilder.toCommaDelimitedBbox;
import static org.geoserver.mapml.MapMLDocumentBuilder.useTiles;
import static org.geoserver.mapml.template.MapMLMapTemplate.MAPML_FEATURE_FTL;
import static org.geoserver.mapml.template.MapMLMapTemplate.MAPML_FEATURE_HEAD_FTL;

import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geoserver.mapml.tcrs.MapMLProjection;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.mapml.template.MapMLMapTemplate;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.HeadContent;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.Meta;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.mapml.xml.Tile;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeodeticCRS;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.locationtech.jts.geom.Envelope;

public class MapMLFeatureUtil {
    private static final Logger LOGGER = Logging.getLogger(MapMLFeatureUtil.class);
    private static final double IS_TILE_TOL = 0.002D;
    public static final String STYLE_CLASS_PREFIX = ".";
    public static final String STYLE_CLASS_DELIMITER = " ";
    public static final String BBOX_DISPLAY_NONE = ".bbox {display:none}";
    private static final MapMLMapTemplate mapMLMapTemplate = new MapMLMapTemplate();
    private static final MapMLEncoder encoder;
    protected static GWC gwc = GWC.get();

    static {
        try {
            encoder = new MapMLEncoder();
        } catch (JAXBException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Convert a list of layers to a MapML document
     *
     * @param layerSimplfierContexts the layers to convert
     * @param clipBounds the bounds to clip the features to (or null if not clipping is desired)
     * @param requestCRS the CRS requested by the client
     * @param alternateProjections alternate projections for the feature collection
     * @param skipAttributes whether to skip attributes HTML representation in the output
     * @return a MapML document
     * @throws IOException if an error occurs while producing the MapML document
     */
    public static Mapml layerContextsToMapMLDocument(
            List<LayerSimplfierContext> layerSimplfierContexts,
            Envelope clipBounds,
            CoordinateReferenceSystem requestCRS,
            List<Link> alternateProjections,
            boolean skipHeadStyles,
            boolean skipAttributes,
            HttpServletRequest request,
            GetMapRequest getMapRequest)
            throws IOException {

        // Check if tiles were requested

        // Some stuff we are getting from the first layer
        ResourceInfo resourceInfo = layerSimplfierContexts.get(0).getResourceInfo();
        MetadataMap layerMeta = resourceInfo.getMetadata();

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(getTitle(layerSimplfierContexts));
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("UTF-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MapMLConstants.MAPML_MIME_TYPE);
        metas.add(meta);
        Set<Meta> projectionAndExtent = deduceProjectionAndExtent(requestCRS, resourceInfo);
        metas.addAll(projectionAndExtent);
        List<Link> links = head.getLinks();
        if (alternateProjections != null) {
            links.addAll(alternateProjections);
        }

        String licenseLink = layerMeta.get("mapml.licenseLink", String.class);
        String licenseTitle = layerMeta.get("mapml.licenseTitle", String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link link = new Link();
            link.setRel(RelType.LICENSE);
            if (licenseLink != null) {
                link.setHref(licenseLink);
            }
            if (licenseTitle != null) {
                link.setTitle(licenseTitle);
            }
            links.add(link);
        }

        // loop through the layers for head elements
        for (LayerSimplfierContext layerSimplfierContext : layerSimplfierContexts) {
            FeatureCollection featureCollection = layerSimplfierContext.getFeatureCollection();
            if (!(featureCollection instanceof SimpleFeatureCollection)) {
                throw new ServiceException("MapML OutputFormat does not support Complex Features.");
            }
            SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;
            boolean hasHeadTemplate = false;
            try {
                if (!mapMLMapTemplate.isTemplateEmpty(
                        fc.getSchema(), MAPML_FEATURE_HEAD_FTL, FeatureTemplate.class, "0\n")) {
                    hasHeadTemplate = true;
                }
            } catch (TemplateNotFoundException e) {
                LOGGER.log(Level.FINEST, MAPML_FEATURE_HEAD_FTL + " Template not found", e);
            }
            Map<String, MapMLStyle> styles = layerSimplfierContext.getStyles();
            if (!skipHeadStyles) {
                String style = getCSSStylesFull(styles);
                head.setStyle(style);
                if (hasHeadTemplate) {
                    getInterpolatedStylesFromTemplate(fc)
                            .ifPresent(interpolated -> appendTemplateCSSStyle(head, interpolated));
                }
            }
        }

        String fCaptionTemplate = layerMeta.get("mapml.featureCaption", String.class);
        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        mapml.setBody(body);
        List<Object> featuresOrTiles = body.getTilesOrFeatures();

        for (LayerSimplfierContext layerSimplfierContext : layerSimplfierContexts) {
            Map<String, MapMLStyle> styles = layerSimplfierContext.getStyles();
            FeatureCollection featureCollection = layerSimplfierContext.getFeatureCollection();
            if (!(featureCollection instanceof SimpleFeatureCollection)) {
                throw new ServiceException("MapML OutputFormat does not support Complex Features.");
            }
            SimpleFeatureCollection fc = (SimpleFeatureCollection) featureCollection;
            boolean hasTemplate = false;

            try {
                if (!mapMLMapTemplate.isTemplateEmpty(
                        fc.getSchema(), MAPML_FEATURE_FTL, FeatureTemplate.class, "0\n")) {
                    hasTemplate = true;
                }
            } catch (TemplateNotFoundException e) {
                LOGGER.log(Level.FINEST, MAPML_FEATURE_FTL + " Template not found", e);
            }
            boolean useTileLinks = false;
            if (layerSimplfierContext.getResourceInfo() != null
                    && layerSimplfierContext.getResourceInfo() instanceof FeatureTypeInfo) {
                MapMLGenerator featureBuilder = new MapMLGenerator();
                featureBuilder.setNumDecimals(layerSimplfierContext.getNumDecimals());
                featureBuilder.setForcedDecimal(layerSimplfierContext.isForcedDecimal());
                featureBuilder.setPadWithZeros(layerSimplfierContext.isPadWithZeros());
                featureBuilder.setClipBounds(clipBounds);
                featureBuilder.setSkipAttributes(skipAttributes);
                featureBuilder.setSimplifier(layerSimplfierContext.getSimplifier());
                try (SimpleFeatureIterator iterator = fc.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        Optional<Mapml> interpolatedOptional = Optional.empty();
                        if (hasTemplate) {
                            interpolatedOptional = getInterpolatedFromTemplate(fc, feature);
                        }
                        // convert feature to xml
                        if (styles != null) {
                            List<MapMLStyle> applicableStyles = getApplicableStyles(feature, styles);
                            Optional<Feature> f = featureBuilder.buildFeature(
                                    feature, fCaptionTemplate, applicableStyles, interpolatedOptional);
                            // feature will be skipped if geometry incompatible with style symbolizer
                            f.ifPresent(featuresOrTiles::add);
                        } else {
                            // WFS GETFEATURE request with no styles
                            Optional<Feature> f =
                                    featureBuilder.buildFeature(feature, fCaptionTemplate, null, interpolatedOptional);
                            f.ifPresent(featuresOrTiles::add);
                        }
                    }
                }
            } else if (layerSimplfierContext.getResourceInfo() != null
                    && layerSimplfierContext.getResourceInfo() instanceof CoverageInfo
                    && request != null) {
                if (getMapRequest != null && useTiles(getMapRequest)) {
                    String crs = extractCRS(getMapRequest.getRawKvp());
                    useTileLinks = gwc.hasTileLayer(layerSimplfierContext.getResourceInfo())
                            && gwc.getTileLayer(layerSimplfierContext.getResourceInfo())
                                            .getGridSubset(crs)
                                    != null;
                }
                try {
                    if (getMapRequest != null) {
                        GridSet chosen = getGridSet(requestCRS);
                        if (chosen == null) {
                            throw new ServiceException("No MapML gridset found for CRS: " + requestCRS);
                        }
                        BoundingBox bbox = new BoundingBox(
                                clipBounds.getMinX(), clipBounds.getMinY(), clipBounds.getMaxX(), clipBounds.getMaxY());
                        MapMLProjection projType = parseProjType(getMapRequest);
                        int zoomLevel = MapMLHTMLOutput.computeZoom(
                                projType,
                                (ReferencedEnvelope) clipBounds,
                                getMapRequest.getWidth(),
                                getMapRequest.getHeight());
                        GridSubset gridSubset = GridSubsetFactory.createGridSubSet(chosen, bbox, zoomLevel, zoomLevel);
                        long[][] tileRangeLevels = gridSubset.getWMTSCoverages();
                        long[] tileRange = tileRangeLevels[0];
                        long minTileX = tileRange[0];
                        long minTileY = tileRange[1];
                        long maxTileX = tileRange[2];
                        long maxTileY = tileRange[3];
                        List<Tile> tiles = new ArrayList<>();
                        for (long tileX = minTileX; tileX <= maxTileX; tileX++) {
                            for (long tileY = minTileY; tileY <= maxTileY; tileY++) {
                                long[] tile1 = {tileX, tileY, zoomLevel};
                                BoundingBox tileBbox = boundsFromIndex(tile1, gridSubset);
                                Envelope tileEnvelope = new Envelope(
                                        tileBbox.getMinX(), tileBbox.getMaxX(), tileBbox.getMinY(), tileBbox.getMaxY());
                                String link = null;
                                if (useTileLinks) {
                                    Map<String, Object> formatOptions = getMapRequest.getFormatOptions();
                                    String format = null;
                                    if (formatOptions != null) {
                                        format = formatOptions
                                                .get(MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION)
                                                .toString();
                                    } else {
                                        format = "image/png";
                                    }
                                    link = getWMTSLink(
                                            layerSimplfierContext,
                                            chosen.getSrs().toString(),
                                            request,
                                            String.valueOf(zoomLevel),
                                            String.valueOf(tileX),
                                            String.valueOf(tileY),
                                            format);
                                } else {
                                    link = getWMSLink(
                                            tileEnvelope,
                                            request,
                                            getMapRequest,
                                            layerSimplfierContext,
                                            chosen.getTileWidth(),
                                            chosen.getTileHeight());
                                }
                                Tile tile = new Tile();
                                tile.setRow(BigInteger.valueOf(tileY));
                                tile.setCol(BigInteger.valueOf(tileX));
                                tile.setZoom(BigInteger.valueOf(zoomLevel));
                                tile.setSrc(link);
                                tile.setDistance(JTS.orthodromicDistance(
                                        clipBounds.centre(), tileEnvelope.centre(), requestCRS));
                                tiles.add(tile);
                            }
                        }
                        Collections.sort(tiles);
                        boolean isTile = (getMapRequest.getWidth() == getMapRequest.getHeight()
                                && tiles.get(0).getDistance() < IS_TILE_TOL);
                        if (isTile) {
                            featuresOrTiles.add(tiles.get(0));
                        } else {
                            featuresOrTiles.addAll(tiles);
                        }
                    }
                } catch (FactoryException | GeoWebCacheException | TransformException e) {
                    throw new ServiceException("Error while looking up MapML gridset", e);
                }
            }
        }
        return mapml;
    }

    private static BoundingBox boundsFromIndex(long[] tileIndex, GridSubset gridSubset) {
        final int tileZ = (int) tileIndex[2];
        Grid grid = gridSubset.getGridSet().getGrid(tileZ);
        boolean yBaseToggle = gridSubset.getGridSet().isTopLeftAligned();

        final long tileX = tileIndex[0];
        final long tileY = tileIndex[1];

        double width = grid.getResolution() * gridSubset.getGridSet().getTileWidth();
        double height = grid.getResolution() * gridSubset.getGridSet().getTileHeight();

        final double[] tileOrigin = gridSubset.getGridSet().tileOrigin();
        BoundingBox tileBounds = new BoundingBox(
                tileOrigin[0] + width * tileX,
                yBaseToggle ? tileOrigin[1] - (height * tileY) : tileOrigin[1] + (height * tileY),
                tileOrigin[0] + width * (tileX + 1),
                yBaseToggle ? tileOrigin[1] - ((height * tileY) + height) : tileOrigin[1] + height * (tileY + 1));
        return tileBounds;
    }

    /** Parses the projection into a ProjType, or throws a proper service exception indicating the unsupported CRS */
    private static MapMLProjection parseProjType(GetMapRequest getMapRequest) {
        try {
            return new MapMLProjection(getMapRequest.getSRS().toUpperCase());
        } catch (IllegalArgumentException | FactoryException iae) {
            // figure out the parameter name (version dependent) and the actual original
            // string value for the srs/crs parameter
            String parameterName = Optional.ofNullable(getMapRequest.getVersion())
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

    private static String getWMTSLink(
            LayerSimplfierContext layerSimplfierContext,
            String tilematrixset,
            HttpServletRequest request,
            String zoomLevel,
            String column,
            String row,
            String format) {
        String path = "gwc/service/wmts";
        HashMap<String, String> params = new HashMap<>();
        String layerName = null;
        if (layerSimplfierContext.getResourceInfo().getNamespace().getPrefix() != null) {
            layerName = layerSimplfierContext.getResourceInfo().getNamespace().getPrefix() + ":"
                    + layerSimplfierContext.getResourceInfo().getName();
        } else {
            layerName = layerSimplfierContext.getResourceInfo().getName();
        }
        params.put("layer", layerName);
        params.put("tilematrixset", tilematrixset);
        params.put("service", "WMTS");
        params.put("request", "GetTile");
        params.put("version", "1.0.0");
        params.put("tilematrix", layerName + ":" + zoomLevel);
        params.put("TileCol", column);
        params.put("TileRow", row);
        params.put("format", format);
        String baseUrl = ResponseUtils.baseURL(request);
        return ResponseUtils.buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
    }

    private static String getWMSLink(
            Envelope clipBounds,
            HttpServletRequest request,
            GetMapRequest getMapRequest,
            LayerSimplfierContext layerSimplfierContext,
            int tileWidth,
            int tileHeight) {
        String baseUrl = ResponseUtils.baseURL(request);
        Map<String, String> kvp = new LinkedHashMap<>();
        kvp.put(
                "LAYERS",
                layerSimplfierContext.getResourceInfo().getNamespace().getPrefix() != null
                        ? layerSimplfierContext.getResourceInfo().getNamespace().getPrefix() + ":"
                                + layerSimplfierContext.getResourceInfo().getName()
                        : layerSimplfierContext.getResourceInfo().getName());
        kvp.put("BBOX", toCommaDelimitedBbox(clipBounds));
        kvp.put("HEIGHT", String.valueOf(tileHeight));
        kvp.put("WIDTH", String.valueOf(tileWidth));
        kvp.put("CRS", escapeHtml4(extractCRS(getMapRequest.getRawKvp())));
        kvp.put("FORMAT", "image/png");
        kvp.put("TRANSPARENT", String.valueOf(getMapRequest.isTransparent()));
        kvp.put("SERVICE", "WMS");
        kvp.put("REQUEST", "GetMap");
        kvp.put("VERSION", "1.3.0");
        return ResponseUtils.buildURL(baseUrl, "wms", kvp, URLMangler.URLType.SERVICE);
    }

    private static GridSet getGridSet(CoordinateReferenceSystem requestCRS)
            throws FactoryException, GeoWebCacheException {
        String epsg = CRS.lookupIdentifier(requestCRS, true);
        MapMLGridsets mapMlGridsets = (MapMLGridsets) GeoServerExtensions.bean("mapMLGridsets");
        GridSet chosen = null;
        for (String candidate : mapMlGridsets.getGridSetNames()) {
            Optional<GridSet> optionalGridSet = mapMlGridsets.getGridSet(candidate);
            if (optionalGridSet.isPresent()) {
                GridSet testGridset = optionalGridSet.get();
                if (testGridset.getSrs().equals(SRS.getSRS(epsg))) {
                    chosen = optionalGridSet.get();
                    break;
                }
            }
        }
        return chosen;
    }

    private static String getTitle(List<LayerSimplfierContext> layerSimplfierContexts) {
        StringJoiner title = new StringJoiner(", ");
        for (LayerSimplfierContext layerSimplfierContext : layerSimplfierContexts) {
            title.add(layerSimplfierContext.getResourceInfo().getName());
        }
        return title.toString();
    }

    private static Optional<Mapml> getInterpolatedFromTemplate(SimpleFeatureCollection fc, SimpleFeature feature) {
        String templateOutput = "Error parsing template output";
        try {
            templateOutput = mapMLMapTemplate.features(fc.getSchema(), feature);
            Mapml out = encoder.decode(new StringReader(templateOutput));
            return Optional.of(out);
        } catch (Exception e) {
            LOGGER.info("Error unmarshalling template output for MapML features "
                    + "Output from template: "
                    + templateOutput
                    + " Error: "
                    + e.getLocalizedMessage());
            throw new ServiceException(e, templateOutput);
        }
    }

    /**
     * Append the CSS style from the template to the feature
     *
     * @param head the head content
     * @param interpolated the interpolated object from the template
     */
    private static void appendTemplateCSSStyle(HeadContent head, Mapml interpolated) {
        if (head != null) {
            if (interpolated.getHead() != null && interpolated.getHead().getStyle() != null) {
                String interpolatedCSSStyle = interpolated.getHead().getStyle();
                if (head.getStyle() == null) {
                    head.setStyle(interpolatedCSSStyle);
                } else {
                    head.setStyle(head.getStyle() + " " + interpolatedCSSStyle);
                }
            }
        }
    }

    private static Optional<Mapml> getInterpolatedStylesFromTemplate(SimpleFeatureCollection fc) throws IOException {
        String templateOutput = mapMLMapTemplate.featureHead(fc.getSchema());
        StringReader reader = new StringReader(templateOutput);
        try {
            return Optional.of(encoder.decode(reader));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error unmarshalling template output: " + templateOutput, e);
            throw new ServiceException(e, templateOutput);
        }
    }

    /**
     * Get an empty MapML document populated with basic request related metadata
     *
     * @param layerInfo metadata for the feature class
     * @param requestCRS the CRS requested by the client
     * @return an empty MapML document
     * @throws IOException if an error occurs while producing the MapML document
     */
    public static Mapml getEmptyMapML(LayerInfo layerInfo, CoordinateReferenceSystem requestCRS) throws IOException {

        ResourceInfo resourceInfo = layerInfo.getResource();
        MetadataMap layerMeta = resourceInfo.getMetadata();

        // build the mapML doc
        Mapml mapml = new Mapml();

        // build the head
        HeadContent head = new HeadContent();
        head.setTitle(layerInfo.getName());
        List<Meta> metas = head.getMetas();
        Meta meta = new Meta();
        meta.setCharset("UTF-8");
        metas.add(meta);
        meta = new Meta();
        meta.setHttpEquiv("Content-Type");
        meta.setContent(MapMLConstants.MAPML_MIME_TYPE);
        metas.add(meta);
        Set<Meta> projectionAndExtent = deduceProjectionAndExtent(requestCRS, resourceInfo);
        metas.addAll(projectionAndExtent);
        List<Link> links = head.getLinks();

        String licenseLink = layerMeta.get("mapml.licenseLink", String.class);
        String licenseTitle = layerMeta.get("mapml.licenseTitle", String.class);
        if (licenseLink != null || licenseTitle != null) {
            Link link = new Link();
            link.setRel(RelType.LICENSE);
            if (licenseLink != null) {
                link.setHref(licenseLink);
            }
            if (licenseTitle != null) {
                link.setTitle(licenseTitle);
            }
            links.add(link);
        }

        mapml.setHead(head);

        // build the body
        BodyContent body = new BodyContent();
        mapml.setBody(body);

        return mapml;
    }

    /**
     * Get the applicable styles for a feature based on the filters
     *
     * @param sf the feature
     * @param styles the styles
     * @return the applicable styles
     */
    private static List<MapMLStyle> getApplicableStyles(SimpleFeature sf, Map<String, MapMLStyle> styles) {
        List<MapMLStyle> applicableStyles = new ArrayList<>();
        for (MapMLStyle style : styles.values()) {
            if (!style.isElseFilter()
                    && (style.getFilter() == null || style.getFilter().evaluate(sf))) {
                applicableStyles.add(style);
            }
        }
        // if no styles are applicable, add the else filter styles
        if (applicableStyles.isEmpty()) {
            for (MapMLStyle style : styles.values()) {
                if (style.isElseFilter()) {
                    applicableStyles.add(style);
                }
            }
        }
        if (applicableStyles.isEmpty() && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.finer("No applicable SLD styles found for feature " + sf.getID());
        }
        return applicableStyles;
    }

    /**
     * Get the CSS styles as a string, including the "bbox" style used for transparency
     *
     * @param styles the styles
     * @return the CSS styles as a string
     */
    public static String getCSSStylesFull(Map<String, MapMLStyle> styles) {
        if (styles == null) return BBOX_DISPLAY_NONE;
        String style = getCSSStyles(styles);
        return BBOX_DISPLAY_NONE + " " + style.toString();
    }

    /**
     * Get the CSS styles as a string, without the "bbox" style used for transparency
     *
     * @param styles
     * @return
     */
    public static String getCSSStyles(Map<String, MapMLStyle> styles) {
        if (styles == null) return "";
        StringJoiner css = new StringJoiner(STYLE_CLASS_DELIMITER);
        for (Map.Entry<String, MapMLStyle> entry : styles.entrySet()) {
            MapMLStyle mapMLStyle = entry.getValue();
            // empty properties can happen when style elements are not supported
            if (mapMLStyle != null && !mapMLStyle.getProperties().isEmpty()) {
                css.add(STYLE_CLASS_PREFIX + mapMLStyle.getStyleAsCSS());
            }
        }
        return css.toString();
    }

    /**
     * @param requestCRS the CRS requested by the client
     * @param resourceInfo metadata for the feature class
     * @return
     */
    private static Set<Meta> deduceProjectionAndExtent(
            CoordinateReferenceSystem requestCRS, ResourceInfo resourceInfo) {
        Set<Meta> metas = new HashSet<>();
        TiledCRSParams tcrs = null;
        CoordinateReferenceSystem sourceCRS = resourceInfo.getCRS();
        CoordinateReferenceSystem responseCRS = sourceCRS;
        String sourceCRSCode = CRS.toSRS(sourceCRS);
        String responseCRSCode = sourceCRSCode;
        Meta projection = new Meta();
        Meta extent = new Meta();
        Meta coordinateSystem = new Meta();
        coordinateSystem.setName("cs");
        extent.setName("extent");
        projection.setName("projection");
        String cite = Citations.fromName("MapML").getTitle().toString();
        String crs;
        try {

            if (requestCRS != null) {
                responseCRS = requestCRS;
                responseCRSCode = CRS.toSRS(requestCRS);
                tcrs = TiledCRSConstants.lookupTCRSParams(responseCRSCode);
                if (tcrs != null) {
                    projection.setContent(tcrs.getName());
                    crs = (responseCRS instanceof GeodeticCRS) ? "gcrs" : "pcrs";
                    coordinateSystem.setContent(crs);
                }
            }
        } catch (Exception e) {
        }
        // if tcrs is not set, either exception encountered deciphering the
        // response CRS or the requested projection is not known to MapML.
        if (tcrs == null) {
            // this crs has no TCRS match, so if it's a gcrs, tag it with the
            // "MapML" CRS 'authority'
            // so that nobody can be surprised by x,y axis order in WGS84 data
            crs = (responseCRS instanceof GeodeticCRS) ? "gcrs" : "pcrs";
            projection.setContent(crs.equalsIgnoreCase("gcrs") ? cite + ":" + responseCRSCode : responseCRSCode);
            coordinateSystem.setContent(crs);
        }
        extent.setContent(getExtent(resourceInfo, responseCRSCode, responseCRS));
        metas.add(projection);
        metas.add(coordinateSystem);
        metas.add(extent);
        return metas;
    }

    /**
     * @param resourceInfo source of bbox info
     * @param responseCRSCode output CRS code
     * @param responseCRS used to transform source bbox to, if necessary
     * @return
     */
    private static String getExtent(
            ResourceInfo resourceInfo, String responseCRSCode, CoordinateReferenceSystem responseCRS) {
        String extent = "";
        ReferencedEnvelope latLonBbox = null;
        String name = "";
        latLonBbox = resourceInfo.getLatLonBoundingBox();
        name = resourceInfo.getName();

        ReferencedEnvelope re;
        String gcrsFormat =
                "top-left-longitude=%1$.6f,top-left-latitude=%2$.6f,bottom-right-longitude=%3$.6f,bottom-right-latitude=%4$.6f";
        String pcrsFormat =
                "top-left-easting=%1$.2f,top-left-northing=%2$.2f,bottom-right-easting=%3$.2f,bottom-right-northing=%4$.2f";
        double minLong, minLat, maxLong, maxLat;
        double minEasting, minNorthing, maxEasting, maxNorthing;
        TiledCRSParams tcrs = TiledCRSConstants.lookupTCRSParams(responseCRSCode);
        try {
            assert latLonBbox != null : "latLonBbox null for MapML layer " + name;
            if (responseCRS instanceof GeodeticCRS) {
                minLong = latLonBbox.getMinX();
                minLat = latLonBbox.getMinY();
                maxLong = latLonBbox.getMaxX();
                maxLat = latLonBbox.getMaxY();
                extent = String.format(gcrsFormat, minLong, maxLat, maxLong, minLat);
            } else {
                re = latLonBbox.transform(responseCRS, true);
                minEasting = re.getMinX();
                minNorthing = re.getMinY();
                maxEasting = re.getMaxX();
                maxNorthing = re.getMaxY();
                extent = String.format(pcrsFormat, minEasting, maxNorthing, maxEasting, minNorthing);
            }
        } catch (Exception e) {
            if (tcrs != null) {
                if (tcrs.getName().equalsIgnoreCase("WGS84")) {
                    minLong = tcrs.getBounds().getMin().x;
                    minLat = tcrs.getBounds().getMin().y;
                    maxLong = tcrs.getBounds().getMax().x;
                    maxLat = tcrs.getBounds().getMax().y;
                    extent = String.format(gcrsFormat, maxLong, maxLat, minLong, minLat);
                } else {
                    minEasting = tcrs.getBounds().getMin().x;
                    minNorthing = tcrs.getBounds().getMin().y;
                    maxEasting = tcrs.getBounds().getMax().x;
                    maxNorthing = tcrs.getBounds().getMax().y;
                    extent = String.format(pcrsFormat, minEasting, maxNorthing, maxEasting, minNorthing);
                }
            }
        }
        return extent;
    }

    /**
     * Format TCRS as alternate projection links for use in a WFS response, allowing projection negotiation
     *
     * @param base the base URL
     * @param path the path to the service
     * @param query the query parameters
     * @return list of link elements with rel=alternate projection=proj-name
     */
    public static List<Link> alternateProjections(String base, String path, Map<String, Object> query) {
        ArrayList<Link> links = new ArrayList<>();
        Set<String> projections = TiledCRSConstants.tiledCRSBySrsName.keySet();
        projections.forEach((String proj) -> {
            Link l = new Link();
            TiledCRSParams projection = TiledCRSConstants.lookupTCRSParams(proj);
            l.setProjection(projection.getName());
            l.setRel(RelType.ALTERNATE);
            query.put("srsName", "MapML:" + projection.getName());
            HashMap<String, String> kvp = new HashMap<>(query.size());
            query.keySet().forEach(key -> {
                kvp.put(key, query.getOrDefault(key, "").toString());
            });
            l.setHref(ResponseUtils.urlDecode(ResponseUtils.buildURL(base, path, kvp, URLMangler.URLType.SERVICE)));
            links.add(l);
        });
        return links;
    }

    /**
     * Get the MapML styles based on Layer Styles
     *
     * @return the MapML styles
     * @throws IOException if an error occurs
     */
    public static Map<String, MapMLStyle> getMapMLStyleMap(Style style, double scaleDenominator) {
        MapMLStyleVisitor styleVisitor = new MapMLStyleVisitor();
        if (style.getName() != null) {
            styleVisitor.setStyleId(style.getName().replace(":", MapMLStyle.NAME_DELIMITER));
        }
        styleVisitor.setScaleDenominator(scaleDenominator);
        style.accept(styleVisitor);
        return styleVisitor.getStyles();
    }

    /** Checks if the request should dump features instead of HTML */
    public static boolean isFeaturesRequest(Request request) {
        // case 1: the format_options parameter is set to include the mapml feature format
        if (getBoleanFormatOption(request, MAPML_FEATURE_FO)) return true;

        // case 2: it's a GWC tile seeding request, can only be a features request
        return isGWCTiledRequest(request);
    }

    /** Checks if the request should skip attributes */
    public static boolean isSkipAttributes(Request request) {
        // case 1: the format_options parameter is set to include the mapml feature format
        if (getBoleanFormatOption(request, MAPML_SKIP_ATTRIBUTES_FO)) return true;

        // case 2: it's a GWC tile seeding request, we want to skip attributes as well
        return isGWCTiledRequest(request);
    }

    /** Checks if the request should skip style bodies */
    public static boolean isSkipHeadStyles(Request request) {
        // case 1: the format_options parameter is set to include the mapml feature format
        if (getBoleanFormatOption(request, MAPML_SKIP_STYLES_FO)) return true;

        // case 2: it's a GWC tile seeding request, we want to skip styles in head as well
        return isGWCTiledRequest(request);
    }

    private static boolean isGWCTiledRequest(Request request) {
        return "true".equals(request.getRawKvp().get(GeoServerTileLayer.GWC_SEED_INTERCEPT_TOKEN));
    }

    /** Tests if the specified format option is present and evaluates to true */
    private static boolean getBoleanFormatOption(Request request, String key) {
        Map formatOptions = (Map) request.getKvp().get("format_options");
        if (formatOptions != null && Boolean.valueOf((String) formatOptions.get(key))) return true;
        return false;
    }

    /** Convenience class to hold the information needed to simplify a feature collection */
    public static class LayerSimplfierContext {
        private final FeatureCollection featureCollection;
        private final ResourceInfo resourceInfo;
        private final MapMLSimplifier simplifier;
        private final int numDecimals;
        private final boolean forcedDecimal;
        private final boolean padWithZeros;
        private final Map<String, MapMLStyle> styles;

        /**
         * Constructor
         *
         * @param featureCollection
         * @param resourceInfo
         * @param simplifier
         * @param numDecimals
         * @param forcedDecimal
         * @param padWithZeros
         */
        public LayerSimplfierContext(
                FeatureCollection featureCollection,
                ResourceInfo resourceInfo,
                MapMLSimplifier simplifier,
                int numDecimals,
                boolean forcedDecimal,
                boolean padWithZeros,
                Map<String, MapMLStyle> styles) {
            this.featureCollection = featureCollection;
            this.resourceInfo = resourceInfo;
            this.simplifier = simplifier;
            this.numDecimals = numDecimals;
            this.forcedDecimal = forcedDecimal;
            this.padWithZeros = padWithZeros;
            this.styles = styles;
        }

        public FeatureCollection getFeatureCollection() {
            return featureCollection;
        }

        public ResourceInfo getResourceInfo() {
            return resourceInfo;
        }

        public MapMLSimplifier getSimplifier() {
            return simplifier;
        }

        public int getNumDecimals() {
            return numDecimals;
        }

        public boolean isForcedDecimal() {
            return forcedDecimal;
        }

        public boolean isPadWithZeros() {
            return padWithZeros;
        }

        public Map<String, MapMLStyle> getStyles() {
            return styles;
        }
    }

    protected static void setGWC(GWC gwc) {
        MapMLFeatureUtil.gwc = gwc;
    }
}
