/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toCollection;
import static org.geoserver.ogcapi.APIException.INVALID_PARAMETER_VALUE;
import static org.geoserver.ogcapi.SwaggerJSONAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;
import static org.springframework.http.MediaType.APPLICATION_YAML_VALUE;

import io.swagger.v3.oas.models.OpenAPI;
import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.ogcapi.TimeExtentCalculator;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.DateRange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(service = "Maps", version = "1.0.1", landingPage = "ogc/maps/v1", serviceClass = WMSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/maps/v1")
public class MapsService {

    private static final String DISPLAY_NAME = "OGC API Maps";
    private TimeParser timeParser = new TimeParser();

    private final GeoServer geoServer;
    private final WebMapService wms;

    public MapsService(GeoServer geoServer, @Qualifier("wmsService2") WebMapService wms) {
        this.geoServer = geoServer;
        this.wms = wms;
    }

    public WMSInfo getService() {
        return geoServer.getService(WMSInfo.class);
    }

    public WMSInfo getServiceInfo() {
        // required for DisabledServiceCheck class
        return getService();
    }

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public MapsLandingPage landingPage() {
        return new MapsLandingPage(getService(), getCatalog(), "ogc/maps/v1");
    }

    @GetMapping(
            path = {"openapi", "openapi.json", "openapi.yaml"},
            name = "getApi",
            produces = {OPEN_API_MEDIA_TYPE_VALUE, APPLICATION_YAML_VALUE, MediaType.TEXT_XML_VALUE})
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new MapsAPIBuilder().build(getService());
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        WMSInfo wms = getService();
        List<String> classes = MapsConformance.configuration(wms).conformances(wms).stream()
                .map(APIConformance::getId)
                .toList();
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public CollectionsDocument getCollections() {
        return new CollectionsDocument(geoServer);
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId) throws IOException {
        PublishedInfo p = getPublished(collectionId);
        CollectionDocument collection = new CollectionDocument(geoServer, p);

        return collection;
    }

    @GetMapping(path = "collections/{collectionId}/styles", name = "getStyles")
    @ResponseBody
    @HTMLResponseBody(templateName = "styles.ftl", fileName = "styles.html")
    public StylesDocument styles(@PathVariable(name = "collectionId") String collectionId) {
        PublishedInfo p = getPublished(collectionId);
        return new StylesDocument(p);
    }

    private PublishedInfo getPublished(String collectionId) {
        // single collection
        PublishedInfo p = getCatalog().getLayerByName(collectionId);
        if (p == null) {
            if (collectionId.contains(":")) {
                String[] split = collectionId.split(":");
                p = getCatalog().getLayerGroupByName(split[0], split[1]);
            } else {
                p = getCatalog().getLayerGroupByName(collectionId);
            }
        }

        if (p == null)
            throw new ServiceException(
                    "Unknown collection " + collectionId, ServiceException.INVALID_PARAMETER_VALUE, "collectionId");

        return p;
    }

    @GetMapping(
            path = {
                "collections/{collectionId}/styles/{styleId}/map",
                "collections/{collectionId}/map" // default style variation
            },
            name = "getCollectionMap")
    @ResponseBody
    public WebMap map(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId", required = false) String styleId,
            @RequestParam(name = "f") String format,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "bbox-crs", required = false) String bboxCrs,
            @RequestParam(name = "subset", required = false) String subset,
            @RequestParam(name = "subset-crs", required = false) String subsetCrs,
            @RequestParam(name = "center", required = false) String center,
            @RequestParam(name = "center-crs", required = false) String centerCrs,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "width", required = false) Integer width,
            @RequestParam(name = "height", required = false) Integer height,
            @RequestParam(name = "scale-denominator", required = false) Double scaleDenominator,
            @RequestParam(name = "mm-per-pixel", required = false) Double mmPerPixel,
            @RequestParam(name = "orientation", required = false) Double orientation,
            @RequestParam(name = "transparent", required = false, defaultValue = "true") boolean transparent,
            @RequestParam(name = "bgcolor", required = false) String bgcolor,
            @RequestParam(name = "void-color", required = false) String voidColor,
            @RequestParam(name = "void-transparent", required = false) Boolean voidTransparent)
            throws IOException, FactoryException, ParseException {
        MapQuery query = new MapQuery(
                bbox,
                bboxCrs,
                subset,
                subsetCrs,
                center,
                centerCrs,
                crs,
                datetime,
                width,
                height,
                scaleDenominator,
                mmPerPixel,
                orientation,
                transparent,
                bgcolor,
                voidColor,
                voidTransparent);
        GetMapRequest request = toGetMapRequest(collectionId, styleId, format, query);

        if ("text/html".equals(format) || "html".equals(format)) {
            DefaultWebMapService.autoSetBoundsAndSize(request);
            if (request.getCrs() != null) request.setSRS(ResourcePool.lookupIdentifier(request.getCrs(), false));
            request.getRawKvp().put("width", String.valueOf(request.getWidth()));
            request.getRawKvp().put("height", String.valueOf(request.getHeight()));
            if (height != null) request.setHeight(height);
            return new HTMLMap(new WMSMapContent(request));
        }
        return wms.reflect(request);
    }

    /** Query parameters shared by the map operations; hyphenated OGC names cannot be Java fields, hence a carrier. */
    record MapQuery(
            String bbox,
            String bboxCrs,
            String subset,
            String subsetCrs,
            String center,
            String centerCrs,
            String crs,
            String datetime,
            Integer width,
            Integer height,
            Double scaleDenominator,
            Double mmPerPixel,
            Double orientation,
            boolean transparent,
            String bgcolor,
            String voidColor,
            Boolean voidTransparent) {

        /** Query for the GetFeatureInfo endpoint, where the single crs applies to both the bbox and the output. */
        static MapQuery forInfo(
                String bbox,
                String crs,
                String datetime,
                Integer width,
                Integer height,
                boolean transparent,
                String bgcolor) {
            return new MapQuery(
                    bbox,
                    crs,
                    null,
                    null,
                    null,
                    null,
                    crs,
                    datetime,
                    width,
                    height,
                    null,
                    null,
                    null,
                    transparent,
                    bgcolor,
                    null,
                    null);
        }
    }

    /** Decodes a CRS identifier, mapping an unknown or invalid code to a 400 (OGC API - Maps, clause 13). */
    private CoordinateReferenceSystem decodeCrs(String code) {
        try {
            return CRS.decode(code, true);
        } catch (FactoryException e) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE, "Unsupported or invalid CRS: " + code, HttpStatus.BAD_REQUEST, e);
        }
    }

    @GetMapping(
            path = {"collections/{collectionId}/styles/{styleId}/map/info", "collections/{collectionId}/map/info"},
            name = "getCollectionInfo")
    @ResponseBody
    public FeatureInfoResponse info(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId", required = false) String styleId,
            @RequestParam(name = "f") String format,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "width", required = false) Integer width,
            @RequestParam(name = "height", required = false) Integer height,
            @RequestParam(name = "transparent", required = false, defaultValue = "true") boolean transparent,
            @RequestParam(name = "bgcolor", required = false) String bgcolor,
            @RequestParam(name = "i") int i,
            @RequestParam(name = "j") int j,
            @RequestParam(name = "limit", required = false, defaultValue = "1") int limit
            // TODO: add all the vendor parameters we normally support in WMS
            ) throws IOException, FactoryException, ParseException {
        WMSInfo wmsInfo = getService();
        if (!MapsConformance.configuration(wmsInfo).featureInfo(wmsInfo)) {
            throw new APIException(
                    APIException.NOT_FOUND,
                    "GetFeatureInfo is a GeoServer extension and is not enabled on this server",
                    HttpStatus.NOT_FOUND);
        }
        if (limit < 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE, "limit must be greater than zero, got " + limit, HttpStatus.BAD_REQUEST);
        }
        MapQuery query = MapQuery.forInfo(bbox, crs, datetime, width, height, transparent, bgcolor);
        GetMapRequest getMapRequest = toGetMapRequest(collectionId, styleId, "image/png", query);
        DefaultWebMapService.autoSetBoundsAndSize(getMapRequest);

        GetFeatureInfoRequest request = new GetFeatureInfoRequest();
        request.setGetMapRequest(getMapRequest);
        request.setXPixel(i);
        request.setYPixel(j);
        request.setInfoFormat(format);
        request.setFeatureCount(limit);
        request.setQueryLayers(getMapRequest.getLayers());

        FeatureCollectionType collection = wms.getFeatureInfo(request);
        return new FeatureInfoResponse(collection, request);
    }

    private GetMapRequest toGetMapRequest(String collectionId, String styleId, String format, MapQuery query)
            throws IOException, FactoryException, ParseException {
        MapsConformance conf = MapsConformance.configuration(getService());
        WMSInfo wmsInfo = getService();
        MapQuery q = ignoreDisabled(query, conf, wmsInfo);

        // a viewport has at least one pixel per side (OGC API - Maps, Scaling, width/height requirement C)
        checkPositiveSize("width", q.width());
        checkPositiveSize("height", q.height());

        // scale-denominator with an explicit width/height is only defined when spatial subsetting is available
        // (OGC API - Maps, Scaling, scale-denominator requirement D)
        boolean explicitSize = q.width() != null || q.height() != null;
        if (q.scaleDenominator() != null && explicitSize && !conf.spatialSubsetting(wmsInfo)) {
            rejectCombination("scale-denominator with width/height requires the spatial subsetting conformance class");
        }

        PublishedInfo p = getPublished(collectionId);
        if (styleId != null) {
            checkStyle(p, styleId);
        } else if (p instanceof LayerInfo l) {
            styleId = l.getDefaultStyle().prefixedName();
        } else if (p instanceof LayerGroupInfo) {
            styleId = StyleDocument.DEFAULT_STYLE_NAME;
        }
        StyleInfo styleInfo = styleId != null ? getCatalog().getStyleByName(styleId) : null;

        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.setLayers(getMapLayers(p));
        if (styleInfo != null) request.setStyles(Arrays.asList(styleInfo.getStyle()));
        request.setFormat(format);

        CoordinateReferenceSystem outputCrs = q.crs() != null ? decodeCrs(q.crs()) : null;

        // area of interest: bbox, or subset spatial ranges, or a box built around a center point
        String datetime = q.datetime();
        ReferencedEnvelope region = q.bbox() != null ? parseSingleBBox(q.bbox(), q.bboxCrs()) : null;
        SubsetResult subset = q.subset() != null ? parseSubset(q.subset(), q.subsetCrs()) : null;

        // bbox, center and the spatial axes of a subset all define the same map extent, so at most one of them can be
        // used (per spec). A subset for time or an additional dimension is no conflict.
        boolean spatialSubset = subset != null && subset.envelope != null && conf.spatialSubsetting(wmsInfo);
        if ((region != null ? 1 : 0) + (q.center() != null ? 1 : 0) + (spatialSubset ? 1 : 0) > 1) {
            rejectCombination("bbox, center and a spatial subset all define the map extent, use only one of them");
        }

        if (subset != null) {
            // the spatial axes and the time axis of a subset belong to different classes, split them apart
            if (subset.envelope != null && conf.spatialSubsetting(wmsInfo)) region = subset.envelope;
            if (subset.time != null && conf.datetime(wmsInfo)) datetime = subset.time;
        }
        Integer width = q.width();
        Integer height = q.height();
        if (region == null && q.center() != null) {
            region = boundsFromCenter(q, width, height);
        }
        if (region != null
                && outputCrs != null
                && !CRS.equalsIgnoreMetadata(outputCrs, region.getCoordinateReferenceSystem())) {
            try {
                region = region.transform(outputCrs, true);
            } catch (TransformException e) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Failed to reproject the requested area to the output CRS",
                        HttpStatus.BAD_REQUEST,
                        e);
            }
        }
        if (region != null) {
            request.setBbox(region);
            request.setCrs(region.getCoordinateReferenceSystem());
        } else if (outputCrs != null) {
            request.setCrs(outputCrs);
        }

        // width/height together with a spatial extent and scale-denominator is an over-constrained request
        // (OGC API - Maps, Scaling, scale-denominator requirement E)
        if (q.scaleDenominator() != null && explicitSize && region != null) {
            rejectCombination("scale-denominator cannot be combined with width/height and a spatial extent");
        }

        // scale-denominator sizes the image when width/height are not both given
        if ((width == null || height == null) && q.scaleDenominator() != null && region != null) {
            int[] size = sizeFromScale(region, q.scaleDenominator(), pixelSizeMeters(q));
            if (width == null) width = size[0];
            if (height == null) height = size[1];
        }
        if (width != null) request.setWidth(width);
        if (height != null) request.setHeight(height);
        if (q.orientation() != null) request.setAngle(q.orientation());
        String background = q.bgcolor() != null ? q.bgcolor() : q.voidColor();
        if (background != null) request.setBgColor(Color.decode(background));
        request.setTransparent(q.transparent());
        if (datetime != null) {
            setupTimeSubset(datetime, p, request);
        }

        Map<String, String> rawParamers = new LinkedHashMap<>();
        if (q.bbox() != null) rawParamers.put("bbox", q.bbox());
        if (q.crs() != null) rawParamers.put("crs", q.crs());
        rawParamers.put("width", String.valueOf(width));
        rawParamers.put("height", String.valueOf(height));
        rawParamers.put("layers", collectionId);
        rawParamers.put("styles", styleId);
        if (datetime != null) rawParamers.put("datetime", datetime);
        request.setRawKvp(rawParamers);
        return request;
    }

    /**
     * Returns a copy of the query with the parameters whose conformance class is disabled dropped to {@code null}.
     * According to OGC APIs, a parameter of an unsupported class is ignored, not rejected.
     */
    private MapQuery ignoreDisabled(MapQuery q, MapsConformance conf, WMSInfo wms) {
        boolean subsetting = conf.spatialSubsetting(wms);
        boolean scaling = conf.scaling(wms);
        // {@code width} and  {@code height} are supported by both the scaling and the spatial subsetting classes
        boolean size = scaling || subsetting;
        return new MapQuery(
                subsetting ? q.bbox() : null,
                subsetting ? q.bboxCrs() : null,
                q.subset(),
                q.subsetCrs(),
                subsetting ? q.center() : null,
                subsetting ? q.centerCrs() : null,
                conf.crs(wms) ? q.crs() : null,
                conf.datetime(wms) ? q.datetime() : null,
                size ? q.width() : null,
                size ? q.height() : null,
                scaling ? q.scaleDenominator() : null,
                conf.displayResolution(wms) ? q.mmPerPixel() : null,
                conf.orientation(wms) ? q.orientation() : null,
                q.transparent(),
                conf.background(wms) ? q.bgcolor() : null,
                conf.background(wms) ? q.voidColor() : null,
                conf.background(wms) ? q.voidTransparent() : null);
    }

    private static void checkPositiveSize(String parameter, Integer value) {
        if (value != null && value < 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    parameter + " must be a positive number of pixels, got " + value,
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void rejectCombination(String message) {
        throw new APIException(APIException.INVALID_PARAMETER_VALUE, message, HttpStatus.BAD_REQUEST);
    }

    private List<MapLayerInfo> getMapLayers(PublishedInfo p) {
        if (p instanceof LayerGroupInfo info1) {
            return info1.layers().stream().map(l -> new MapLayerInfo(l)).collect(Collectors.toList());
        } else if (p instanceof LayerInfo info) {
            return Arrays.asList(new MapLayerInfo(info));
        } else {
            throw new RuntimeException("Unexpected published object" + p);
        }
    }

    private void checkStyle(PublishedInfo p, String styleId) {
        if (p instanceof LayerGroupInfo && StyleDocument.DEFAULT_STYLE_NAME.equals(styleId)) {
            return;
        } else if (p instanceof LayerInfo l) {
            if (l.getDefaultStyle().prefixedName().equals(styleId)
                    || l.getStyles().stream().anyMatch(s -> s.prefixedName().equals(styleId))) return;
        } else {
            throw new RuntimeException("Unexpected published object" + p);
        }
        // in any other case, the style was not recognized
        throw new APIException(
                APIException.INVALID_PARAMETER_VALUE, "Invalid style identifier: " + styleId, HttpStatus.BAD_REQUEST);
    }

    private ReferencedEnvelope parseSingleBBox(String bbox, String bboxCrs) throws FactoryException {
        ReferencedEnvelope[] parsed = APIBBoxParser.parse(bbox, bboxCrs);
        if (parsed.length == 1) {
            return horizontal(parsed[0]);
        }
        // antimeridian crossing: the parser splits the box at +/-180 into [minx, 180] and [-180, maxx].
        // Rebuild a single continuous envelope extending past 180 (e.g. 170..190), which GeoServer renders
        // across the dateline; a low longitude greater than the high one is required by OGC API - Maps.
        ReferencedEnvelope west = horizontal(parsed[0]);
        ReferencedEnvelope east = parsed[1];
        return new ReferencedEnvelope(
                west.getMinX(),
                east.getMaxX() + 360,
                west.getMinY(),
                west.getMaxY(),
                west.getCoordinateReferenceSystem());
    }

    /**
     * The horizontal part of a bounding box: a map is flat, so the vertical range of the six ordinate form allowed by
     * {@code /req/spatial-subsetting/bbox-definition} is dropped, along with the third axis of its CRS.
     */
    private static ReferencedEnvelope horizontal(ReferencedEnvelope bbox) throws FactoryException {
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        if (crs == null || crs.getCoordinateSystem().getDimension() < 3) return bbox;
        CoordinateReferenceSystem horizontal = CRS.getHorizontalCRS(crs);
        // the derived CRS carries no authority code, so re-decode it: the rendering pipeline needs an SRS identifier,
        // and the ordinates are already longitude first
        String code = CRS.lookupIdentifier(horizontal, true);
        if (code != null) horizontal = CRS.decode(code, true);
        return new ReferencedEnvelope(bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY(), horizontal);
    }

    /** OGC pixel size in meters, from mm-per-pixel or the default 0.28 mm (see OGC API - Maps, Scaling). */
    private double pixelSizeMeters(MapQuery q) {
        return (q.mmPerPixel() != null ? q.mmPerPixel() : 0.28) / 1000d;
    }

    private int[] sizeFromScale(ReferencedEnvelope region, double scaleDenominator, double pixelSizeMeters) {
        double groundResolution = scaleDenominator * pixelSizeMeters; // meters per pixel
        CoordinateReferenceSystem crs = region.getCoordinateReferenceSystem();
        double widthMeters = RendererUtilities.toMeters(region.getWidth(), crs);
        double heightMeters = RendererUtilities.toMeters(region.getHeight(), crs);
        int width = Math.max(1, (int) Math.round(widthMeters / groundResolution));
        int height = Math.max(1, (int) Math.round(heightMeters / groundResolution));
        return new int[] {width, height};
    }

    private ReferencedEnvelope boundsFromCenter(MapQuery q, Integer width, Integer height) throws FactoryException {
        if (width == null || height == null || q.scaleDenominator() == null) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "center requires width, height and scale-denominator to define the map extent",
                    HttpStatus.BAD_REQUEST);
        }
        double[] c = parseNumbers(q.center());
        if (c.length < 2) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "center must have at least two ordinates",
                    HttpStatus.BAD_REQUEST);
        }
        CoordinateReferenceSystem crs = q.centerCrs() != null ? decodeCrs(q.centerCrs()) : DefaultGeographicCRS.WGS84;
        double metersPerUnit = RendererUtilities.toMeters(1d, crs);
        double groundResolution = q.scaleDenominator() * pixelSizeMeters(q); // meters per pixel
        double halfWidth = (width / 2d) * groundResolution / metersPerUnit;
        double halfHeight = (height / 2d) * groundResolution / metersPerUnit;
        return new ReferencedEnvelope(c[0] - halfWidth, c[0] + halfWidth, c[1] - halfHeight, c[1] + halfHeight, crs);
    }

    /** Result of an OGC subset expression: a spatial envelope and/or a temporal value. */
    private static class SubsetResult {
        ReferencedEnvelope envelope;
        String time;
    }

    /**
     * Rewrites a {@code time} subset range into the {@code datetime} syntax: the quotes go away, the {@code low:high}
     * separator becomes a slash, and an asterisk becomes the open bound {@code ..} (OGC API - Maps,
     * {@code /req/datetime/subset-definition}). A colon inside a timestamp is not a separator, so only the one outside
     * the quotes counts. A range with more than two bounds is a 400.
     */
    private static String toDatetime(String range) {
        // walk the string once, tracking whether we are inside quotes: a colon outside them separates the two
        // bounds, a colon inside is part of a timestamp. A trim has one separator at most, so a second is a 400
        int separator = -1, count = 0;
        boolean quoted = false;
        for (int i = 0; i < range.length(); i++) {
            char c = range.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ':' && !quoted) {
                if (separator < 0) separator = i;
                count++;
            }
        }
        if (count > 1) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Invalid time subset range: " + range,
                    HttpStatus.BAD_REQUEST);
        }
        // no separator means a time slice, a single instant
        if (separator < 0) return toDatetimeBound(range);
        return toDatetimeBound(range.substring(0, separator)) + "/" + toDatetimeBound(range.substring(separator + 1));
    }

    /** One end of a time subset trim, with the quotes gone and an asterisk turned into the open bound. */
    private static String toDatetimeBound(String bound) {
        String value = bound.replace("\"", "").trim();
        return "*".equals(value) ? ".." : value;
    }

    /** Parses a {@code Lat(30:60),Lon(10:20)} / {@code time(...)} subset, mapping Lat/Lon (or N/E, Y/X) to a box. */
    private SubsetResult parseSubset(String subset, String subsetCrs) throws FactoryException {
        SubsetResult result = new SubsetResult();
        CoordinateReferenceSystem crs = subsetCrs != null ? decodeCrs(subsetCrs) : DefaultGeographicCRS.WGS84;
        Double minX = null, maxX = null, minY = null, maxY = null;
        // look for commas immediately preceded by a closing parenthesis, as the separator
        for (String dim : subset.split("(?<=\\)),")) {
            int open = dim.indexOf('(');
            if (open < 0 || !dim.endsWith(")")) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Invalid subset expression: " + dim,
                        HttpStatus.BAD_REQUEST);
            }
            String axis = dim.substring(0, open).trim();
            String range = dim.substring(open + 1, dim.length() - 1);
            if (axis.equalsIgnoreCase("time")) {
                result.time = toDatetime(range);
                continue;
            }
            String[] bounds = range.split(":");
            double low = Double.parseDouble(bounds[0].trim());
            double high = bounds.length > 1 ? Double.parseDouble(bounds[1].trim()) : low;
            if (axis.equalsIgnoreCase("Lon") || axis.equalsIgnoreCase("E") || axis.equalsIgnoreCase("X")) {
                minX = low;
                // a low longitude greater than the high one means an extent crossing the wrapping point, see
                // /req/spatial-subsetting/subset-definition; extend past 180 like the bbox path does
                maxX = low > high && crs instanceof GeographicCRS ? high + 360 : high;
            } else if (axis.equalsIgnoreCase("Lat") || axis.equalsIgnoreCase("N") || axis.equalsIgnoreCase("Y")) {
                minY = low;
                maxY = high;
            } else {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Unsupported subset axis: " + axis,
                        HttpStatus.BAD_REQUEST);
            }
        }
        if (minX != null && minY != null) {
            result.envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
        }
        return result;
    }

    private double[] parseNumbers(String csv) {
        String[] parts = csv.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return values;
    }

    private void setupTimeSubset(String datetime, PublishedInfo p, GetMapRequest request)
            throws ParseException, IOException {
        if (!(p instanceof LayerInfo)) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Can only handle time subset on layers, not layer groups",
                    HttpStatus.BAD_REQUEST);
        }
        LayerInfo layer = (LayerInfo) p;
        DimensionInfo time = layer.getResource().getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE, "Time dimension is not enabled in this coverage", HttpStatus.BAD_REQUEST);
        }
        @SuppressWarnings("unchecked")
        Collection<Object> times = timeParser.parse(closeOpenBounds(datetime, layer.getResource()));
        if (times.size() != 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Invalid datetime specification, must be a single time, or a time range",
                    HttpStatus.BAD_REQUEST);
        }
        request.setTime(List.copyOf(times));
    }

    /**
     * Replaces the open bounds of a datetime interval, written {@code ..} or left empty, with the ends of the layer own
     * time extent, which the time parser needs as explicit instants. Returns the value unchanged when both bounds are
     * given.
     */
    private static String closeOpenBounds(String datetime, ResourceInfo resource) throws IOException {
        int separator = datetime.indexOf('/');
        if (separator < 0) return datetime;
        String low = datetime.substring(0, separator);
        String high = datetime.substring(separator + 1);
        boolean openLow = low.isEmpty() || "..".equals(low);
        boolean openHigh = high.isEmpty() || "..".equals(high);
        if (!openLow && !openHigh) return datetime;

        DateRange extent = TimeExtentCalculator.getTimeExtent(resource);
        if (extent == null) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Cannot resolve an open ended time interval, the layer has no known time extent",
                    HttpStatus.BAD_REQUEST);
        }
        if (openLow) low = ISO_INSTANT.format(extent.getMinValue().toInstant());
        if (openHigh) high = ISO_INSTANT.format(extent.getMaxValue().toInstant());
        return low + "/" + high;
    }
}
