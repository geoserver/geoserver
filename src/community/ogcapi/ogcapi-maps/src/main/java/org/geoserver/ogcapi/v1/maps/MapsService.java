/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.geoserver.ogcapi.APIException.INVALID_PARAMETER_VALUE;
import static org.geoserver.ogcapi.SwaggerJSONAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;
import static org.springframework.http.MediaType.APPLICATION_YAML_VALUE;

import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.CRSURIs;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.QueryablesBuilder;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.ogcapi.SwaggerJSONSchemaMessageConverter;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.kvp.ElevationParser;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.DimensionWarning;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.capabilities.DimensionHelper;
import org.geoserver.wms.legendgraphic.GetLegendGraphicKvpReader;
import org.geoserver.wms.legendgraphic.LegendGraphic;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.api.referencing.cs.RangeMeaning;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.util.ColorConverterFactory;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(service = "Maps", version = "1.0.1", landingPage = "ogc/maps/v1", serviceClass = WMSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/maps/v1")
public class MapsService {

    /**
     * How many collections the HTML preview offers in its layer chooser; a longer list is unusable and bloats the page.
     * A larger catalog is truncated, and the preview says so. The cap is the chooser's alone: the {@code collections}
     * parameter still accepts a collection left out of it.
     */
    static final int PALETTE_COLLECTIONS = 500;

    static final Logger LOGGER = Logging.getLogger(MapsService.class);

    private static final String DISPLAY_NAME = "OGC API Maps";

    /** The display resolution assumed when {@code mm-per-pixel} is not given, by spec. */
    private static final double DEFAULT_MM_PER_PIXEL = 0.28;

    private static final double MM_PER_INCH = 25.4;

    private TimeParser timeParser = new TimeParser();
    private final APIFilterParser filterParser = new APIFilterParser();

    private final GeoServer geoServer;
    private final WebMapService wms;
    private final WMS wmsFacade;
    private final GetLegendGraphicKvpReader legendReader;

    public MapsService(
            GeoServer geoServer,
            @Qualifier("wmsService2") WebMapService wms,
            WMS wmsFacade,
            GetLegendGraphicKvpReader legendReader) {
        this.geoServer = geoServer;
        this.wms = wms;
        this.wmsFacade = wmsFacade;
        this.legendReader = legendReader;
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

    /**
     * The CRSs a collection can be delivered in, as URIs, CRS84 first, matching what OGC API - Features advertises (see
     * {@code /req/collection-map/desc-crs}).
     */
    List<String> serviceCRSList() {
        return CRSURIs.serviceList(getService().getSRS());
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public MapsLandingPage landingPage() {
        return new MapsLandingPage(getService(), getCatalog(), "ogc/maps/v1", serviceCRSList());
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
        return new CollectionsDocument(geoServer, serviceCRSList());
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId) throws IOException {
        return new CollectionDocument(geoServer, getPublished(collectionId), serviceCRSList());
    }

    @GetMapping(path = "collections/{collectionId}/styles", name = "getStyles")
    @ResponseBody
    @HTMLResponseBody(templateName = "styles.ftl", fileName = "styles.html")
    public StylesDocument styles(@PathVariable(name = "collectionId") String collectionId) {
        PublishedInfo p = getPublished(collectionId);
        WMSInfo wmsInfo = getService();
        List<MediaType> legendFormats = MapsConformance.configuration(wmsInfo).legend(wmsInfo)
                ? new ArrayList<>(APIRequestInfo.get().getProducibleMediaTypes(LegendResponse.class, false))
                : List.of();
        return new StylesDocument(p, legendFormats);
    }

    @GetMapping(
            path = "collections/{collectionId}/queryables",
            name = "getQueryables",
            produces = SwaggerJSONSchemaMessageConverter.SCHEMA_TYPE_VALUE)
    @ResponseBody
    @HTMLResponseBody(templateName = "queryables.ftl", fileName = "queryables.html")
    public Queryables queryables(@PathVariable(name = "collectionId") String collectionId) throws IOException {
        WMSInfo wmsInfo = getService();
        if (!MapsConformance.configuration(wmsInfo).queryablesAvailable(wmsInfo)) {
            throw new APIException(
                    APIException.NOT_FOUND, "Queryables are not enabled on this server", HttpStatus.NOT_FOUND);
        }
        PublishedInfo p = getPublished(collectionId);
        FeatureType schema = queryableSchema(p);
        if (schema == null) {
            throw new APIException(
                    APIException.NOT_FOUND,
                    "Collection " + collectionId + " does not expose queryables",
                    HttpStatus.NOT_FOUND);
        }
        String id = ResponseUtils.buildURL(
                APIRequestInfo.get().getBaseURL(),
                "ogc/maps/v1/collections/" + ResponseUtils.urlEncode(collectionId) + "/queryables",
                null,
                URLMangler.URLType.RESOURCE);
        Queryables queryables = new QueryablesBuilder(id).forType(schema).build();
        queryables.setCollectionId(collectionId);
        queryables.setTitle(Optional.ofNullable(p.getTitle()).orElse(collectionId));
        queryables.setDescription(p.getAbstract());
        return queryables;
    }

    /**
     * The attributes a map filter can use on the given collection, null if it does not expose any: a vector layer
     * filters on its own feature type, a structured coverage on its granule index, while a simple raster has nothing to
     * filter on, and a layer group has one schema per member.
     */
    static FeatureType queryableSchema(PublishedInfo published) throws IOException {
        if (!(published instanceof LayerInfo layer)) return null;
        ResourceInfo resource = layer.getResource();
        if (resource instanceof FeatureTypeInfo ft) return ft.getFeatureType();
        if (!(resource instanceof CoverageInfo ci)) return null;
        if (!(ci.getGridCoverageReader(null, null) instanceof StructuredGridCoverage2DReader reader)) return null;
        // a single coverage reader is allowed to leave the native name unset in the configuration
        String nativeName = ci.getNativeCoverageName();
        if (nativeName == null) nativeName = reader.getGridCoverageNames()[0];
        return reader.getGranules(nativeName, true).getSchema();
    }

    private PublishedInfo getPublished(String collectionId) {
        // a collection the service does not offer is reported as unknown, it has no resource of its own here
        PublishedInfo p = DatasetCollections.published(getCatalog(), collectionId);
        if (p == null || !DatasetCollections.isMappable(p))
            throw new ServiceException(
                    "Unknown collection " + collectionId, ServiceException.INVALID_PARAMETER_VALUE, "collectionId");

        return p;
    }

    /**
     * The collections a dataset map renders, failing with a 404 when the dataset map class is disabled: outside the
     * standard the resource must not exist. The {@code collections} parameter of a disabled class is ignored, not
     * rejected, as the OGC API convention asks.
     */
    private List<PublishedInfo> datasetCollections(MapQuery query) {
        WMSInfo wmsInfo = getService();
        MapsConformance conf = MapsConformance.configuration(wmsInfo);
        if (!conf.datasetMap(wmsInfo)) {
            throw new APIException(
                    APIException.NOT_FOUND, "Dataset maps are not enabled on this server", HttpStatus.NOT_FOUND);
        }
        String selection = conf.collectionsSelection(wmsInfo) ? query.collections() : null;
        return DatasetCollections.resolve(getCatalog(), selection, MapsSettings.defaultCollections(wmsInfo));
    }

    /**
     * The dataset map: a single map of several collections, drawn in painter's order following the {@code collections}
     * parameter when present, or in the order {@link DatasetCollections#mappable} picks otherwise.
     */
    @GetMapping(path = "map", name = "getDatasetMap")
    @ResponseBody
    @DefaultContentType(MediaType.IMAGE_PNG_VALUE)
    public WebMap datasetMap(@RequestParam(name = "f", required = false) String format, @ModelAttribute MapQuery query)
            throws IOException, FactoryException, ParseException {
        List<PublishedInfo> collections = datasetCollections(query);
        WebMap map = map(collections, null, format, query);
        if (map instanceof HTMLMap html) addCollectionChooser(html, collections);
        return map;
    }

    /**
     * Offers the collections of the dataset for the preview to switch between, the ones being drawn marked as chosen
     * and listed first, at most {@link #PALETTE_COLLECTIONS} of them.
     */
    private void addCollectionChooser(HTMLMap map, List<PublishedInfo> drawn) {
        WMSInfo wmsInfo = getService();
        MapsConformance conf = MapsConformance.configuration(wmsInfo);
        if (!conf.collectionsSelection(wmsInfo)) return;
        // selected vs available. Since available is also limited, start from selected and add from there
        List<String> selected = drawn.stream().map(PublishedInfo::prefixedName).toList();
        Set<String> available = new LinkedHashSet<>(selected);
        DatasetCollections.pickable(getCatalog(), PALETTE_COLLECTIONS + 1).stream()
                .map(PublishedInfo::prefixedName)
                .forEach(available::add);
        boolean cut = available.size() > PALETTE_COLLECTIONS;
        map.setCollections(
                available.stream().limit(PALETTE_COLLECTIONS).sorted().toList(), selected, cut);
    }

    @GetMapping(
            path = {
                "collections/{collectionId}/styles/{styleId}/map",
                "collections/{collectionId}/map" // default style variation
            },
            name = "getCollectionMap")
    @ResponseBody
    @DefaultContentType(MediaType.IMAGE_PNG_VALUE)
    public WebMap map(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId", required = false) String styleId,
            @RequestParam(name = "f", required = false) String format,
            @ModelAttribute MapQuery query)
            throws IOException, FactoryException, ParseException {
        return map(List.of(getPublished(collectionId)), styleId, format, query);
    }

    private WebMap map(List<PublishedInfo> collections, String styleId, String format, MapQuery query)
            throws IOException, FactoryException, ParseException {
        String encoding = mapFormat(format);
        checkFormatConformance(encoding);
        GetMapRequest request = toGetMapRequest(collections, styleId, encoding, query);

        if ("text/html".equals(encoding) || "html".equals(encoding)) {
            DefaultWebMapService.autoSetBoundsAndSize(request);
            if (request.getCrs() != null) request.setSRS(ResourcePool.lookupIdentifier(request.getCrs(), false));
            request.getRawKvp().put("width", String.valueOf(request.getWidth()));
            request.getRawKvp().put("height", String.valueOf(request.getHeight()));
            if (query.height() != null) request.setHeight(query.height());
            return new HTMLMap(new WMSMapContent(request));
        }
        WebMap map;
        try {
            map = wms.reflect(request);
        } catch (ServiceException e) {
            // checks if the exception is related to a dimension mismatch, otherwise returns it as-is
            throw notFoundOnDimensionMismatch(e);
        }
        addContentHeaders(request);
        return map;
    }

    /**
     * Turns the WMS "no match for a dimension value" failure into a 404, per standard compliance when a subset falls
     * entirely outside the values valid for its axis ({@code /req/datetime/subset-definition} D and
     * {@code /req/general-subsetting/subset-definition} D). Any other service exception travels on unchanged.
     *
     * <p>The check itself is the WMS one, so it obeys {@code WMSInfo#exceptionOnInvalidDimension()}: turned off, the
     * server draws an empty map and answers 200 instead.
     */
    private static RuntimeException notFoundOnDimensionMismatch(ServiceException e) {
        if (!ServiceException.INVALID_DIMENSION_VALUE.equals(e.getCode())) return e;
        // the locator is the WMS request key, so a custom dimension arrives as DIM_<NAME>; name the axis as the
        // client wrote it in the subset instead
        String axis = e.getLocator().replaceFirst("^DIM_", "");
        return new APIException(
                APIException.NOT_FOUND,
                "The requested " + axis + " value falls outside the values available for that dimension",
                HttpStatus.NOT_FOUND,
                e);
    }

    /** Sets the OGC API - Maps content headers describing the delivered CRS, extent, orientation and time. */
    private void addContentHeaders(GetMapRequest request) throws FactoryException {
        HttpServletResponse response = APIRequestInfo.get().getResponse();
        if (response == null) return;
        // the rotation actually applied, in decimal degrees, zero when the map is north up
        // (/req/orientation/response-headers A)
        response.setHeader("Content-Orientation", String.valueOf(request.getAngle()));
        if (request.getBbox() != null) {
            String[] headers = contentCrsAndBbox(new ReferencedEnvelope(request.getBbox(), request.getCrs()));
            if (headers[0] != null) response.setHeader("Content-Crs", headers[0]);
            response.setHeader("Content-Bbox", headers[1]);
        }
        String datetime = contentDatetime(request);
        if (datetime != null) response.setHeader("Content-Datetime", datetime);
    }

    /**
     * The {@code Content-Datetime} value: the time as the client asked for it (the parser widens an instant to a range
     * of its own precision, and the spec also accepts the shortened {@code yyyy} and {@code yyyy-mm} forms). An open
     * bound is not a datetime, so those requests, and the ones with no time at all, report the instants actually
     * rendered, an open side showing as the far away instant that stands for it. Null when the map has no temporal
     * aspect.
     */
    private String contentDatetime(GetMapRequest request) {
        String requested = request.getRawKvp().get("time");
        if (requested != null && !requested.contains("..") && !requested.startsWith("/") && !requested.endsWith("/")) {
            // nearest match draws a time the client did not ask for, and the header reports what was drawn
            Object nearest = nearestTime();
            return nearest != null ? formatDatetime(nearest) : requested;
        }
        Object time = request.getTime() != null && !request.getTime().isEmpty()
                ? request.getTime().get(0)
                : getDefaultTime(request);
        return time != null ? formatDatetime(time) : null;
    }

    /**
     * The time nearest match snapped to, read off the warnings the WMS dimension handling collected while rendering.
     * Null when nearest match is off, or when no value was found within the acceptable interval.
     */
    private static Object nearestTime() {
        return HTTPWarningAppender.getWarnings().stream()
                .filter(w -> w.getWarningType() == WarningType.Nearest)
                .filter(w -> ResourceInfo.TIME.equals(w.getDimensionName()))
                .map(DimensionWarning::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * The time rendered when the request did not ask for one: the dimension default of the first layer having a time
     * dimension, which the renderer uses in that case. Null when no layer has one, the map then having no temporal
     * aspect to report.
     */
    private Object getDefaultTime(GetMapRequest request) {
        ResourceInfo timed = timeDimensionResource(request);
        return timed != null ? wmsFacade.getDefaultTime(timed) : null;
    }

    /** The first requested layer having a time dimension enabled, null when none of them has one. */
    private static ResourceInfo timeDimensionResource(GetMapRequest request) {
        for (MapLayerInfo layer : request.getLayers()) {
            ResourceInfo resource = layer.getResource();
            DimensionInfo time = resource.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            if (time != null && time.isEnabled()) return resource;
        }
        return null;
    }

    /**
     * A rendered time as RFC 3339: an instant, or {@code instant/instant} for an interval (OGC API - Maps,
     * {@code /req/core/map-response}).
     */
    private static String formatDatetime(Object time) {
        if (time instanceof Date date) return ISO_INSTANT.format(date.toInstant());
        if (time instanceof DateRange range) {
            return ISO_INSTANT.format(range.getMinValue().toInstant()) + "/"
                    + ISO_INSTANT.format(range.getMaxValue().toInstant());
        }
        return String.valueOf(time);
    }

    /**
     * Content-Crs and Content-Bbox header values for a longitude-first delivered envelope: element 0 is the Content-Crs
     * (null when the CRS has no authority identifier, e.g. a custom projection), element 1 the Content-Bbox in the CRS
     * authority axis order.
     */
    static String[] contentCrsAndBbox(ReferencedEnvelope bbox) throws FactoryException {
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        String identifier = crs != null ? ResourcePool.lookupIdentifier(crs, false) : null;
        boolean latFirst = false;
        if (identifier != null) {
            // the envelope is longitude-first; the header must follow the CRS authority axis order. Decode via the
            // WMS 1.3.0 SRS so forceXY does not flatten it back to EAST_NORTH (see APIBBoxParser#parseCRS)
            String srs13 = WMS.toInternalSRS(identifier, WMS.version("1.3.0"));
            latFirst = CRS.getAxisOrder(CRS.decode(srs13)) == CRS.AxisOrder.NORTH_EAST;
        }
        String contentBbox = latFirst
                ? bbox.getMinY() + "," + bbox.getMinX() + "," + bbox.getMaxY() + "," + bbox.getMaxX()
                : bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY();
        // the Content-Crs header wraps the URI in angle brackets, as the examples of /req/core/map-response show
        return new String[] {identifier != null ? "<" + CRSURIs.uri(identifier) + ">" : null, contentBbox};
    }

    /**
     * The encoding of a map: the best offered match for what the client asked, which {@code /req/core/map-op} uses to
     * negotiate the media type. The OGC API Common {@code f} parameter already overrides the {@code Accept} header in
     * the requested types. PNG is preferred among equally acceptable encodings, and used when the client stated no
     * preference at all. An encoding this server cannot produce is a failed negotiation, not a rendering error.
     */
    private String mapFormat(String format) {
        List<MediaType> offered = new ArrayList<>(APIRequestInfo.get().getProducibleMediaTypes(WebMap.class, true));
        offered.sort(Comparator.comparing(m -> MediaType.IMAGE_PNG.equalsTypeAndSubtype(m) ? 0 : 1));
        // the requested types arrive sorted by specificity and quality, so the first offered match is the best one
        List<MediaType> requestedTypes = APIRequestInfo.get().getRequestedMediaTypes();
        for (MediaType requested : requestedTypes) {
            for (MediaType candidate : offered) {
                if (requested.isCompatibleWith(candidate)) return candidate.toString();
            }
        }
        throw notAcceptableFormat(format != null ? format : requestedTypes.toString());
    }

    /**
     * A TIFF or SVG output format whose optional conformance class is disabled is not an offered encoding, so the
     * request fails content negotiation with a 406. The format is chosen by HTTP content negotiation, with the OGC API
     * Common {@code f} parameter overriding the {@code Accept} header.
     */
    private void checkFormatConformance(String format) {
        if (format == null) return;
        WMSInfo wms = getService();
        MapsConformance conf = MapsConformance.configuration(wms);
        String f = format.toLowerCase();
        if (f.contains("tiff") && !conf.tiff(wms)) throw notAcceptableFormat("TIFF");
        if (f.contains("svg") && !conf.svg(wms)) throw notAcceptableFormat("SVG");
    }

    private APIException notAcceptableFormat(String format) {
        return new APIException(
                "NotAcceptable",
                "The " + format + " output format is not available on this server",
                HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * Query parameters shared by the map operations, bound from the request by Spring constructor binding. Some
     * parameter names cannot be Java identifiers, hence the {@link BindParam} mapping.
     */
    record MapQuery(
            String collections,
            String bbox,
            @BindParam("bbox-crs") String bboxCrs,
            String subset,
            @BindParam("subset-crs") String subsetCrs,
            String center,
            @BindParam("center-crs") String centerCrs,
            String crs,
            String datetime,
            Integer width,
            Integer height,
            @BindParam("scale-denominator") Double scaleDenominator,
            @BindParam("mm-per-pixel") Double mmPerPixel,
            Double orientation,
            Boolean transparent,
            String bgcolor,
            @BindParam("void-color") String voidColor,
            @BindParam("void-transparent") Boolean voidTransparent,
            String filter,
            @BindParam("filter-lang") String filterLang,
            @BindParam("filter-crs") String filterCrs) {

        /**
         * Maps are transparent unless asked otherwise, or unless a background color is given, which would otherwise
         * never show (OGC API - Maps, {@code /req/background/transparent-definition} C and D). The void settings act as
         * the fallback, GeoServer having a single background for both the no data areas and the projection void, see
         * {@link MapsService#applyBackground}.
         */
        boolean isTransparent() {
            if (transparent != null) return transparent;
            if (voidTransparent != null) return voidTransparent;
            return bgcolor == null && voidColor == null;
        }

        /**
         * The query the feature info resource uses. The pixel comes from the same map, so every parameter shaping it is
         * kept, with two exceptions: the output crs applies to the bbox too when no bbox-crs is given, and the
         * orientation is dropped, the wms-core identifiers not being able to query a rotated map.
         */
        MapQuery forInfo() {
            return new MapQuery(
                    collections,
                    bbox,
                    bboxCrs != null ? bboxCrs : crs,
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
                    null,
                    transparent,
                    bgcolor,
                    voidColor,
                    voidTransparent,
                    filter,
                    filterLang,
                    filterCrs);
        }
    }

    /** Fails with a 404 when the operation is disabled: its conformance class is not declared, so it must not exist. */
    private void checkEnabled(boolean enabled, String operation) {
        if (enabled) return;
        throw new APIException(
                APIException.NOT_FOUND, operation + " is not enabled on this server", HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "map/info", name = "getDatasetInfo")
    @ResponseBody
    public FeatureInfoResponse datasetInfo(
            @RequestParam(name = "f") String format,
            @RequestParam(name = "i") int i,
            @RequestParam(name = "j") int j,
            @RequestParam(name = "limit", required = false, defaultValue = "1") int limit,
            @ModelAttribute MapQuery query)
            throws IOException, FactoryException, ParseException {
        return info(datasetCollections(query), null, format, i, j, limit, query);
    }

    @GetMapping(
            path = {"collections/{collectionId}/styles/{styleId}/map/info", "collections/{collectionId}/map/info"},
            name = "getCollectionInfo")
    @ResponseBody
    public FeatureInfoResponse info(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId", required = false) String styleId,
            @RequestParam(name = "f") String format,
            @RequestParam(name = "i") int i,
            @RequestParam(name = "j") int j,
            @RequestParam(name = "limit", required = false, defaultValue = "1") int limit,
            @ModelAttribute MapQuery query
            // TODO: add all the vendor parameters we normally support in WMS
            ) throws IOException, FactoryException, ParseException {
        return info(List.of(getPublished(collectionId)), styleId, format, i, j, limit, query);
    }

    private FeatureInfoResponse info(
            List<PublishedInfo> collections, String styleId, String format, int i, int j, int limit, MapQuery query)
            throws IOException, FactoryException, ParseException {
        WMSInfo wmsInfo = getService();
        checkEnabled(MapsConformance.configuration(wmsInfo).featureInfo(wmsInfo), "GetFeatureInfo");
        if (limit < 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE, "limit must be greater than zero, got " + limit, HttpStatus.BAD_REQUEST);
        }
        GetMapRequest getMapRequest = toGetMapRequest(collections, styleId, "image/png", query.forInfo());
        // fills in the per layer default styles besides the bounds and the size: the identifiers render the map to
        // find the features under the pixel, so they need a style for every layer
        DefaultWebMapService.autoSetMissingProperties(getMapRequest);

        GetFeatureInfoRequest request = new GetFeatureInfoRequest();
        request.setGetMapRequest(getMapRequest);
        request.setXPixel(i);
        request.setYPixel(j);
        request.setInfoFormat(format);
        request.setFeatureCount(limit);
        request.setQueryLayers(getMapRequest.getLayers());

        FeatureCollectionType collection;
        try {
            collection = wms.getFeatureInfo(request);
        } catch (ServiceException e) {
            throw notFoundOnDimensionMismatch(e);
        }
        return new FeatureInfoResponse(collection, request);
    }

    @GetMapping(
            path = {"collections/{collectionId}/styles/{styleId}/legend", "collections/{collectionId}/legend"},
            name = "getCollectionLegend")
    @ResponseBody
    public LegendResponse legend(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId", required = false) String styleId,
            @RequestParam(name = "f", required = false, defaultValue = "image/png") String format,
            @RequestParam(name = "width", required = false) Integer width,
            @RequestParam(name = "height", required = false) Integer height,
            @RequestParam(name = "scale", required = false) Double scale,
            @RequestParam(name = "rule", required = false) String rule,
            @RequestParam(name = "lang", required = false) String lang,
            @RequestParam(name = "transparent", required = false, defaultValue = "true") boolean transparent,
            @RequestParam(name = "bgcolor", required = false) String bgcolor,
            @RequestParam(name = "legend-options", required = false) String legendOptions)
            throws Exception {
        WMSInfo wmsInfo = getService();
        checkEnabled(MapsConformance.configuration(wmsInfo).legend(wmsInfo), "GetLegendGraphic");
        PublishedInfo p = getPublished(collectionId);
        if (styleId != null) {
            checkStyle(p, styleId);
        }

        if (wmsFacade.getLegendGraphicOutputFormat(format) == null) throw notAcceptableFormat(format);

        GetLegendGraphicRequest request = new GetLegendGraphicRequest();
        request.setWms(wmsFacade);
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.setStrict(false);
        request.setFormat(format);
        request.setTransparent(transparent);
        if (scale != null) request.setScale(scale);
        if (lang != null) request.setLocale(Locale.forLanguageTag(lang));
        if (width != null) request.setWidth(width);
        if (height != null) request.setHeight(height);

        request.getLegends().addAll(legends(p, styleId, request));
        // like WMS, a single rule applies to the first legend only, the others keep all of their rules
        if (rule != null) request.setRule(rule);
        applyConfiguredSize(request, width == null, height == null);

        Map<String, Object> options = new LinkedHashMap<>();
        if (legendOptions != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) new FormatOptionsKvpParser().parse(legendOptions);
            options.putAll(parsed);
        }
        if (bgcolor != null) options.put("bgColor", bgcolor);
        if (!options.isEmpty()) request.setLegendOptions(options);

        LegendGraphic legend = (LegendGraphic) wms.getLegendGraphic(request);
        return new LegendResponse(legend, request);
    }

    /**
     * The legends to draw, one per layer, a layer group unfolded into its members drawn each in the style the group
     * assigns it. The legend graphic reader is used as a library here, it owns the layer lookups that have no
     * equivalent outside WMS.
     */
    private List<LegendRequest> legends(PublishedInfo p, String styleId, GetLegendGraphicRequest request)
            throws Exception {
        if (p instanceof LayerInfo layer) {
            StyleInfo style = styleId != null && !StyleDocument.DEFAULT_STYLE_NAME.equals(styleId)
                    ? getCatalog().getStyleByName(styleId)
                    : layer.getDefaultStyle();
            return List.of(legendRequest(layer, style, request));
        }
        LayerGroupInfo group = (LayerGroupInfo) p;
        List<LayerInfo> layers = group.layers();
        List<StyleInfo> styles = group.styles();
        List<LegendRequest> legends = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            LayerInfo layer = layers.get(i);
            StyleInfo style = i < styles.size() && styles.get(i) != null ? styles.get(i) : layer.getDefaultStyle();
            LegendRequest legend = legendRequest(layer, style, request);
            legend.setLayerGroupInfo(group);
            legends.add(legend);
        }
        return legends;
    }

    /** The legend of a single layer, asking the remote server for it when the layer cascades one. */
    private LegendRequest legendRequest(LayerInfo layer, StyleInfo style, GetLegendGraphicRequest request)
            throws Exception {
        if (layer.getResource() instanceof WMSLayerInfo) {
            return legendReader.getCascadeLegendRequest(layer, request);
        }
        LegendRequest legend = new LegendRequest(
                legendReader.getLayerFeatureType(layer), layer.getResource().getQualifiedName());
        legend.setLayer(layer.prefixedName());
        legend.setLayerInfo(layer);
        legend.setStyle(style.getStyle());
        // a label configured on the layer is the legend title, in the requested language when there is one
        MapLayerInfo mapLayer = new MapLayerInfo(layer, request.getLocale());
        if (mapLayer.getLabel() != null) legend.setTitle(mapLayer.getLabel());
        // a legend graphic on the style wins over one on the layer, as in WMS; both are resolved against the
        // styles directory, and are dropped when the reference does not resolve
        LegendInfo legendInfo = legendReader.resolveLegendInfo(style.getLegend(), request, style);
        if (legendInfo == null) legendInfo = legendReader.resolveLegendInfo(layer.getLegend(), request, null);
        legend.setLegendInfo(legendInfo);
        return legend;
    }

    /** Sizes the legend after the graphic configured on the style or layer, for the dimensions the caller left out. */
    private static void applyConfiguredSize(GetLegendGraphicRequest request, boolean noWidth, boolean noHeight) {
        for (LegendRequest legend : request.getLegends()) {
            LegendInfo legendInfo = legend.getLegendInfo();
            if (legendInfo == null) continue;
            if (noWidth && legendInfo.getWidth() > 0) request.setWidth(legendInfo.getWidth());
            if (noHeight && legendInfo.getHeight() > 0) request.setHeight(legendInfo.getHeight());
        }
    }

    private GetMapRequest toGetMapRequest(
            List<PublishedInfo> collections, String styleId, String format, MapQuery query)
            throws IOException, FactoryException, ParseException {
        WMSInfo wmsInfo = getService();
        MapsConformance conf = MapsConformance.configuration(wmsInfo);
        MapQuery q = ignoreDisabled(query, conf, wmsInfo);

        // a viewport has at least one pixel per side (OGC API - Maps, Scaling, width/height requirement C)
        checkPositiveSize("width", q.width());
        checkPositiveSize("height", q.height());

        // a rendering device pixel has a positive size (/req/display-resolution/mm-per-pixel-definition B)
        if (q.mmPerPixel() != null && q.mmPerPixel() <= 0) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "mm-per-pixel must be a positive number, got " + q.mmPerPixel(),
                    HttpStatus.BAD_REQUEST);
        }

        // scale-denominator with an explicit width/height is only defined when spatial subsetting is available
        // (OGC API - Maps, Scaling, scale-denominator requirement D)
        boolean explicitSize = q.width() != null || q.height() != null;
        if (q.scaleDenominator() != null && explicitSize && !conf.spatialSubsetting(wmsInfo)) {
            rejectCombination("scale-denominator with width/height requires the spatial subsetting conformance class");
        }

        // one collection resolves a style as the styled map resources do; several of them are drawn each in its own
        // default style, which GetMapDefaults fills in, there being no single style covering unrelated schemas
        PublishedInfo single = collections.size() == 1 ? collections.get(0) : null;
        if (single != null) {
            if (styleId != null) {
                checkStyle(single, styleId);
            } else if (single instanceof LayerInfo l) {
                styleId = l.getDefaultStyle().prefixedName();
            } else {
                styleId = StyleDocument.DEFAULT_STYLE_NAME;
            }
        }
        StyleInfo styleInfo = styleId != null ? getCatalog().getStyleByName(styleId) : null;

        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.setLayers(getMapLayers(collections));
        if (styleInfo != null) request.setStyles(Arrays.asList(styleInfo.getStyle()));
        request.setFormat(format);

        // accept the SafeCURIE/URN forms for the output CRS, but render in longitude/latitude like the rest of the
        // pipeline; the delivered axis order is reported back through the Content-Crs and Content-Bbox headers
        CoordinateReferenceSystem outputCrs =
                q.crs() != null ? APIBBoxParser.toLonLat(APIBBoxParser.parseCRS(q.crs())) : null;

        // area of interest: bbox, or subset spatial ranges, or a box built around a center point
        String datetime = q.datetime();
        ReferencedEnvelope region = q.bbox() != null ? parseSingleBBox(q.bbox(), q.bboxCrs()) : null;
        SubsetResult subset = q.subset() != null
                ? parseSubset(q.subset(), q.subsetCrs(), wmsInfo.isCiteCompliant() && conf.spatialSubsetting(wmsInfo))
                : null;

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
        // a bbox or spatial subset is an explicit extent; center is not (it needs width/height and scale-denominator)
        boolean explicitExtent = region != null;
        if (region == null && q.center() != null) {
            region = boundsAround(parseCenter(q), q, width, height);
        } else if (region == null && q.scaleDenominator() != null && width != null && height != null) {
            // no spatial subset at all: the scale and the image size define the extent, laid out around the middle
            // of the data (/req/scaling/scale-denominator-definition F)
            region = boundsAround(dataCenter(request), q, width, height);
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
        // width/height together with a bbox/subset extent and scale-denominator is spec invalid
        if (q.scaleDenominator() != null && explicitSize && explicitExtent) {
            rejectCombination("scale-denominator cannot be combined with width/height and a spatial extent");
        }

        // set both CRS and SRS: wms.reflect() guesses the layer SRS when getSRS() is null, which would silently
        // overwrite the requested output CRS
        if (region != null) {
            // the same CRS spelled as an OGC URI, a URN or a safe CURIE decodes to differently identified objects, and
            // only some of those identifiers can be decoded back; normalise to the longitude/latitude twin, which the
            // ordinates already follow and whose SRS the rendering pipeline understands
            CoordinateReferenceSystem delivered = APIBBoxParser.toLonLat(region.getCoordinateReferenceSystem());
            request.setBbox(region);
            request.setCrs(delivered);
            request.setSRS(CRS.toSRS(delivered));
        } else if (outputCrs != null) {
            request.setCrs(outputCrs);
            request.setSRS(CRS.toSRS(outputCrs));
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
        applyBackground(q, request);
        applyDisplayResolution(q, request);
        if (datetime != null) {
            setupTimeSubset(datetime, request);
        }

        Map<String, String> rawParamers = new LinkedHashMap<>();
        if (q.bbox() != null) rawParamers.put("bbox", q.bbox());
        if (q.crs() != null) rawParamers.put("crs", q.crs());
        // the requested time, kept for the Content-Datetime header: the parsed value is a range even for an instant
        if (datetime != null) rawParamers.put("time", datetime);
        rawParamers.put("width", String.valueOf(width));
        rawParamers.put("height", String.valueOf(height));
        rawParamers.put(
                "layers", collections.stream().map(PublishedInfo::prefixedName).collect(Collectors.joining(",")));
        if (styleId != null) rawParamers.put("styles", styleId);
        // carried so that the HTML preview of a dataset map keeps rendering the collections that were asked for
        if (q.collections() != null) rawParamers.put("collections", q.collections());
        if (datetime != null) rawParamers.put("datetime", datetime);
        // the attribute filter, combined with bbox, datetime and subset by AND (/req/filter/mixing-expressions):
        // the renderer applies it on top of the spatial and dimension restrictions already set above
        if (q.filter() != null) {
            Filter parsed = filterParser.parse(q.filter(), q.filterLang(), q.filterCrs());
            checkFilterAttributes(parsed, request.getLayers());
            request.setFilter(Collections.nCopies(request.getLayers().size(), parsed));
            rawParamers.put("filter", q.filter());
            if (q.filterLang() != null) rawParamers.put("filter-lang", q.filterLang());
        }
        if (subset != null && conf.generalSubsetting(wmsInfo)) {
            applyExtraDimensions(subset, request, rawParamers);
        }
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
        // one subset string carries axes of three different classes, so it survives when any of them is on, and each
        // axis is then applied, or ignored, where the parsed result is used
        boolean anySubset = subsetting || conf.datetime(wms) || conf.generalSubsetting(wms);
        // the language is resolved here, so that the parser gets the same default the API document declares
        String filterLang = q.filter() != null ? filterLanguage(q.filterLang(), conf, wms) : null;
        return new MapQuery(
                conf.collectionsSelection(wms) ? q.collections() : null,
                subsetting ? q.bbox() : null,
                subsetting ? q.bboxCrs() : null,
                anySubset ? q.subset() : null,
                subsetting ? q.subsetCrs() : null,
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
                conf.background(wms) ? q.voidTransparent() : null,
                filterLang != null ? q.filter() : null,
                filterLang,
                q.filterCrs());
    }

    /**
     * The filter language to use, {@code null} when the filter parameters must be ignored because filtering is not
     * enabled. Fails the request if the language is not one of those advertised in the API document.
     */
    private String filterLanguage(String filterLang, MapsConformance conf, WMSInfo wms) {
        if (!conf.filtering(wms)) {
            LOGGER.info(() -> "Ignoring the filter parameter, it requires the "
                    + MapsConformance.FILTER.getId()
                    + " and "
                    + MapsConformance.MAP_FILTER.getId()
                    + " conformance classes, plus one filter language, to be enabled");
            return null;
        }
        return APIFilterParser.resolveLanguage(filterLang, wms);
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

    /**
     * The layers to render, bottom to top: the collections in the order they were given, each layer group unfolded into
     * its own members, nested groups included.
     */
    private static List<MapLayerInfo> getMapLayers(List<PublishedInfo> collections) {
        List<MapLayerInfo> layers = new ArrayList<>();
        for (PublishedInfo p : collections) {
            if (p instanceof LayerGroupInfo group) {
                group.layers().forEach(l -> layers.add(new MapLayerInfo(l)));
            } else if (p instanceof LayerInfo layer) {
                layers.add(new MapLayerInfo(layer));
            } else {
                throw new IllegalStateException("Unexpected published object " + p);
            }
        }
        return layers;
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

    /**
     * Rejects a filter naming an attribute that no drawn layer exposes as a queryable, which would otherwise be
     * silently dropped by the renderer. The comparison uses the queryables, not the raw schemas, so that restricting
     * them one day also restricts what a filter can name.
     */
    private static void checkFilterAttributes(Filter filter, List<MapLayerInfo> layers) throws IOException {
        FilterAttributeExtractor extractor = new FilterAttributeExtractor();
        filter.accept(extractor, null);
        Set<String> unknown = new LinkedHashSet<>(extractor.getAttributeNameSet());
        if (unknown.isEmpty()) return;

        // a map draws several layers, each with its own schema, so an attribute known to any of them is usable
        for (MapLayerInfo layer : layers) {
            FeatureType schema = queryableSchema(layer.getLayerInfo());
            if (schema == null) continue;
            unknown.removeAll(new QueryablesBuilder("")
                    .forType(schema)
                    .build()
                    .getProperties()
                    .keySet());
            if (unknown.isEmpty()) return;
        }
        throw new APIException(
                INVALID_PARAMETER_VALUE,
                "The filter references attributes that are not queryable on any of the requested collections: "
                        + String.join(", ", unknown),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Applies the background parameters. GeoServer paints the areas with no data and the ones outside the valid area of
     * the projection with the same colour and opacity, so the {@code void-color} and {@code void-transparent} values
     * act as the defaults of the background pair rather than the other way around, which is the direction OGC API -
     * Maps defines them in ({@code /req/background/void-color-definition} C, {@code /req/background/void-transparent-
     * definition} B). With neither given the renderer paints an opaque map white, as requirement D asks.
     */
    private static void applyBackground(MapQuery q, GetMapRequest request) {
        String parameter = q.bgcolor() != null ? "bgcolor" : "void-color";
        String color = q.bgcolor() != null ? q.bgcolor() : q.voidColor();
        if (color != null) request.setBgColor(parseColor(parameter, color));
        request.setTransparent(q.isTransparent());
    }

    /**
     * A background colour: a hexadecimal red-green-blue value, with or without a {@code 0x} or {@code #} prefix, or a
     * case insensitive W3C web colour name (OGC API - Maps, {@code /req/background/bgcolor-definition} A and B).
     */
    private static Color parseColor(String parameter, String value) {
        Color named = ColorConverterFactory.CSS_COLORS.get(value.toLowerCase());
        if (named != null) return named;
        // a bare value is hexadecimal, the only numeric notation the standard defines
        String hex = value.startsWith("#") || value.startsWith("0x") || value.startsWith("0X") ? value : "0x" + value;
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    parameter + " must be a hexadecimal RGB value or a W3C web color name, got " + value,
                    HttpStatus.BAD_REQUEST,
                    e);
        }
    }

    /**
     * The map extent the {@code bbox} parameter defines, as one envelope. {@link APIBBoxParser} rolls longitudes into
     * [-180, 180] and splits the box in two when they then come out in the wrong order; a map is drawn on one
     * continuous canvas instead, so the longitudes are taken from the parameter as written. GeoServer renders such an
     * extent the way WMS does, 170..190 across the dateline and -185..185 as the whole world with its edges repeated.
     */
    private ReferencedEnvelope parseSingleBBox(String bbox, String bboxCrs) throws FactoryException {
        ReferencedEnvelope parsed = horizontal(APIBBoxParser.parse(bbox, bboxCrs)[0]);

        String[] ordinates = bbox.split(",");
        CoordinateReferenceSystem requestCrs = APIBBoxParser.parseCRS(bboxCrs);
        // the ordinates follow the axis order the CRS authority declares, the default CRS84 being longitude first
        int low = requestCrs != null && CRS.getAxisOrder(requestCrs) == CRS.AxisOrder.NORTH_EAST ? 1 : 0;
        int high = (ordinates.length >= 6 ? 3 : 2) + low;
        double minX = Double.parseDouble(ordinates[low].trim());
        double maxX = Double.parseDouble(ordinates[high].trim());
        // in degrees, a high longitude lower than the low one crosses the antimeridian, which OGC API - Maps writes
        // that way (e.g. 170..-170); a continuous canvas needs it as 170..190. A projected box in that order never
        // gets here, the parser refuses it
        if (maxX < minX) maxX += 360;
        return new ReferencedEnvelope(
                minX, maxX, parsed.getMinY(), parsed.getMaxY(), parsed.getCoordinateReferenceSystem());
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
        return (q.mmPerPixel() != null ? q.mmPerPixel() : DEFAULT_MM_PER_PIXEL) / 1000d;
    }

    /**
     * Passes {@code mm-per-pixel} down to the renderer as the WMS {@code dpi} format option, so that the scale the
     * symbology rules are selected with reflects the requested display resolution and not just the image size
     * ({@code /req/display-resolution/map-success} B). The default is the renderer own one, so it needs no option.
     */
    private static void applyDisplayResolution(MapQuery q, GetMapRequest request) {
        if (q.mmPerPixel() == null || q.mmPerPixel() == DEFAULT_MM_PER_PIXEL) return;
        // the rendering pipeline reads the option as an integer, see VectorRenderingLayerIdentifier
        request.getFormatOptions().put("dpi", (int) Math.round(MM_PER_INCH / q.mmPerPixel()));
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

    /** A map centre in longitude/latitude order, with the XY twin of the CRS it is expressed in. */
    private record Center(double lon, double lat, CoordinateReferenceSystem crs) {}

    /** The centre asked for by the {@code center} and {@code center-crs} parameters. */
    private static Center parseCenter(MapQuery q) throws FactoryException {
        String[] ordinates = q.center().split(",");
        if (ordinates.length < 2) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "center must have at least two ordinates",
                    HttpStatus.BAD_REQUEST);
        }
        double[] c = {Double.parseDouble(ordinates[0].trim()), Double.parseDouble(ordinates[1].trim())};
        CoordinateReferenceSystem crs =
                q.centerCrs() != null ? APIBBoxParser.parseCRS(q.centerCrs()) : DefaultGeographicCRS.WGS84;
        // center ordinates follow the identifier axis order; read them into longitude/latitude and work in the XY twin
        boolean northEast = CRS.getAxisOrder(crs) == CRS.AxisOrder.NORTH_EAST;
        return new Center(northEast ? c[1] : c[0], northEast ? c[0] : c[1], APIBBoxParser.toLonLat(crs));
    }

    /** The middle of the data, used as the map centre when the request states none. */
    private static Center dataCenter(GetMapRequest request) throws IOException {
        ReferencedEnvelope centers = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        for (MapLayerInfo layer : request.getLayers()) {
            centers.expandToInclude(layerCenter(layer));
        }
        if (centers.isNull()) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Cannot place the map, the collection has no known extent, use bbox, subset or center",
                    HttpStatus.BAD_REQUEST);
        }
        return new Center(centers.getMedian(0), centers.getMedian(1), DefaultGeographicCRS.WGS84);
    }

    /**
     * The centre of a layer, in WGS84. Reprojecting the native centre keeps the point in the middle of the data: the
     * declared latitude/longitude box is the envelope of the reprojected footprint, whose centre drifts away from the
     * data when the footprint curves.
     */
    private static Coordinate layerCenter(MapLayerInfo layer) throws IOException {
        ReferencedEnvelope bounds;
        try {
            bounds = layer.getBoundingBox();
        } catch (Exception e) {
            throw new IOException("Failed to read the bounds of layer " + layer.getName(), e);
        }
        // remote sources and layers with no native box fall back to the declared one, already in WGS84
        if (bounds == null || bounds.isEmpty()) bounds = layer.getLatLongBoundingBox();
        Coordinate center = new Coordinate(bounds.getMedian(0), bounds.getMedian(1));
        try {
            MathTransform tx =
                    CRS.findMathTransform(bounds.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84, true);
            return JTS.transform(center, null, tx);
        } catch (FactoryException | TransformException e) {
            throw new IOException("Failed to locate the centre of layer " + layer.getName(), e);
        }
    }

    /** The extent a map of the given pixel size covers around a centre, at the requested scale and resolution. */
    private ReferencedEnvelope boundsAround(Center center, MapQuery q, Integer width, Integer height) {
        if (width == null || height == null || q.scaleDenominator() == null) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "center requires width, height and scale-denominator to define the map extent",
                    HttpStatus.BAD_REQUEST);
        }
        double metersPerUnit = RendererUtilities.toMeters(1d, center.crs());
        double groundResolution = q.scaleDenominator() * pixelSizeMeters(q); // meters per pixel
        double halfWidth = (width / 2d) * groundResolution / metersPerUnit;
        double halfHeight = (height / 2d) * groundResolution / metersPerUnit;
        return new ReferencedEnvelope(
                center.lon() - halfWidth,
                center.lon() + halfWidth,
                center.lat() - halfHeight,
                center.lat() + halfHeight,
                center.crs());
    }

    /** Result of an OGC subset expression: a spatial envelope and/or a temporal value. */
    private static class SubsetResult {
        ReferencedEnvelope envelope;
        String time;
        // axes that are neither spatial nor time, kept as name -> raw range for the general subsetting class
        final Map<String, String> extraDimensions = new LinkedHashMap<>();
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

    /*
     * The subset axis names, lower cased: the ones the standard requires plus the aliases it recommends, see
     * /req/spatial-subsetting/subset-definition and /req/datetime/subset-definition. An axis outside these sets and
     * outside the collection additional dimensions is rejected with a 400.
     */
    private static final Set<String> X_AXES = Set.of("lon", "long", "longitude", "e", "easting", "x");

    private static final Set<String> Y_AXES = Set.of("lat", "latitude", "n", "northing", "y");
    private static final Set<String> TIME_AXES = Set.of("time", "t");

    /** The vertical axis names, which a flat map applies as the WMS elevation dimension. */
    private static final Set<String> VERTICAL_AXES = Set.of("h", "z");

    /**
     * Parses a {@code Lat(30:60),Lon(10:20)} / {@code time(...)} subset, mapping Lat/Lon (or N/E, Y/X) to a box and the
     * time axis apart; any other axis (elevation or a custom dimension) is collected as an extra dimension for the
     * general subsetting class to apply.
     *
     * @param checkRanges whether a spatial range outside its axis is a 404, which only holds in CITE compliant mode and
     *     only when the spatial subsetting class can act on that range
     */
    private SubsetResult parseSubset(String subset, String subsetCrs, boolean checkRanges) throws FactoryException {
        SubsetResult result = new SubsetResult();
        // the ranges are keyed by axis name (Lat/Lon), so the envelope is built in longitude/latitude order and only
        // needs the matching XY CRS regardless of the identifier axis order
        CoordinateReferenceSystem crs = subsetCrs != null
                ? APIBBoxParser.toLonLat(APIBBoxParser.parseCRS(subsetCrs))
                : DefaultGeographicCRS.WGS84;
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
            String name = axis.toLowerCase();
            String range = dim.substring(open + 1, dim.length() - 1);
            if (TIME_AXES.contains(name)) {
                result.time = toDatetime(range);
                continue;
            }
            boolean isX = X_AXES.contains(name);
            boolean isY = Y_AXES.contains(name);
            if (!isX && !isY) {
                // a vertical axis is the elevation of a flat map; any other one is a custom dimension, whose raw
                // range is kept as given since its values need not be numeric
                result.extraDimensions.put(VERTICAL_AXES.contains(name) ? "elevation" : axis, range.trim());
                continue;
            }
            String[] bounds = range.split(":");
            double low = Double.parseDouble(bounds[0].trim());
            double high = bounds.length > 1 ? Double.parseDouble(bounds[1].trim()) : low;
            if (checkRanges) checkAxisRange(axis, crs.getCoordinateSystem().getAxis(isX ? 0 : 1), low, high);
            if (isX) {
                minX = low;
                // a low longitude greater than the high one means an extent crossing the wrapping point, see
                // /req/spatial-subsetting/subset-definition; extend past 180 like the bbox path does
                maxX = low > high && crs instanceof GeographicCRS ? high + 360 : high;
            } else {
                minY = low;
                maxY = high;
            }
        }
        if (minX != null && minY != null) {
            result.envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
        }
        return result;
    }

    /**
     * Fails with a 404 when a subset interval falls entirely outside the valid range of its axis
     * ({@code /req/spatial-subsetting/subset-definition} D). Only called in CITE compliant mode.
     */
    private static void checkAxisRange(String axis, CoordinateSystemAxis csAxis, double low, double high) {
        if (csAxis.getRangeMeaning() == RangeMeaning.WRAPAROUND) return;
        double min = csAxis.getMinimumValue();
        double max = csAxis.getMaximumValue();
        if (Math.max(low, high) >= min && Math.min(low, high) <= max) return;
        throw new APIException(
                APIException.NOT_FOUND,
                "The " + axis + " subset " + low + ":" + high + " falls entirely outside the valid range of the axis, "
                        + min + " to " + max,
                HttpStatus.NOT_FOUND);
    }

    /**
     * Applies the requested time to the map. Like WMS, a single time value drives every requested layer having a time
     * dimension, the others being drawn whole, so the map needs one such layer and not all of them.
     */
    private void setupTimeSubset(String datetime, GetMapRequest request) throws ParseException {
        if (timeDimensionResource(request) == null) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "The time dimension is not enabled on any of the requested collections",
                    HttpStatus.BAD_REQUEST);
        }
        @SuppressWarnings("unchecked")
        Collection<Object> times = timeParser.parse(closeOpenBounds(datetime));
        if (times.size() != 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Invalid datetime specification, must be a single time, or a time range",
                    HttpStatus.BAD_REQUEST);
        }
        request.setTime(List.copyOf(times));
    }

    /**
     * Rewrites an open bound of a {@code datetime} interval, written {@code ..} or left empty, as an instant far enough
     * away to leave that side unconstrained. The WMS time syntax has no open interval, so the parser needs both ends.
     * Returns the value unchanged when it is a single instant, or an interval with both bounds given.
     */
    private static String closeOpenBounds(String datetime) {
        int separator = datetime.indexOf('/');
        if (separator < 0) return datetime;
        String low = datetime.substring(0, separator);
        String high = datetime.substring(separator + 1);
        if (!isOpen(low) && !isOpen(high)) return datetime;
        // year one and year 9999, not the extremes of Date: a range is turned into a filter, and databases reject a
        // timestamp outside the years they can store
        return (isOpen(low) ? "0001-01-01T00:00:00Z" : low) + "/" + (isOpen(high) ? "9999-12-31T23:59:59Z" : high);
    }

    private static boolean isOpen(String bound) {
        return bound.isEmpty() || "..".equals(bound);
    }

    /**
     * Applies the subset's elevation and custom axes to the WMS request, rejecting an axis that none of the requested
     * layers knows with a 400. As in WMS, one value drives every layer having that dimension: an axis is known when at
     * least one of them has it.
     */
    private void applyExtraDimensions(SubsetResult subset, GetMapRequest request, Map<String, String> rawKvp) {
        if (subset.extraDimensions.isEmpty()) return;

        boolean elevation = false;
        Set<String> customNames = new LinkedHashSet<>();
        for (MapLayerInfo layer : request.getLayers()) {
            ResourceInfo resource = layer.getResource();
            DimensionInfo dimension = resource.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
            elevation |= dimension != null && dimension.isEnabled();
            customNames.addAll(DimensionHelper.getCustomDimensions(resource).keySet());
        }

        for (Map.Entry<String, String> e : subset.extraDimensions.entrySet()) {
            String axis = e.getKey();
            // OGC subset uses low:high for an interval, WMS dimension KVP uses low/high
            String value = e.getValue().replace(':', '/');

            // elevation is a first-class WMS dimension, set directly on the request
            if (axis.equalsIgnoreCase("elevation") && elevation) {
                request.setElevation(parseElevation(value));
                continue;
            }

            // a custom dimension goes into the WMS pipeline as its DIM_<NAME> (upper-cased) raw KVP entry
            String custom = customNames.stream()
                    .filter(n -> n.equalsIgnoreCase(axis))
                    .findFirst()
                    .orElse(null);
            if (custom == null) {
                // known parameter, invalid value: the spec mandates a 4xx (/req/general-subsetting/subset-definition)
                throw new APIException(INVALID_PARAMETER_VALUE, "Unknown subset axis: " + axis, HttpStatus.BAD_REQUEST);
            }
            rawKvp.put(DimensionInfo.getDimensionKey(custom), value);
        }
    }

    /** Parses an elevation subset (WMS {@code low/high} syntax) into the list a GetMapRequest expects. */
    private static List<Object> parseElevation(String value) {
        try {
            @SuppressWarnings("unchecked")
            Collection<Object> parsed = new ElevationParser().parse(value);
            return new ArrayList<>(parsed);
        } catch (ParseException ex) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE, "Invalid elevation subset: " + value, HttpStatus.BAD_REQUEST, ex);
        }
    }
}
