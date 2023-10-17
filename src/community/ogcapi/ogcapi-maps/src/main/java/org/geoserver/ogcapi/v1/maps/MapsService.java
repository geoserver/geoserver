/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.geoserver.ogcapi.APIException.INVALID_PARAMETER_VALUE;

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
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIBBoxParser;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.StyleDocument;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(
        service = "Maps",
        version = "1.0.1",
        landingPage = "ogc/maps/v1",
        serviceClass = WMSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/maps/v1")
public class MapsService {

    public static final String CONF_CLASS_CORE =
            "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/core";
    public static final String CONF_CLASS_GEODATA =
            "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/geodata";
    public static final String CONF_CLASS_BBOX =
            "http://www.opengis.net/spec/ogcapi-maps-2/1.0/conf/bbox";
    public static final String CONF_CLASS_CRS =
            "http://www.opengis.net/spec/ogcapi-maps-2/1.0/conf/crs";

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

    private Catalog getCatalog() {
        return geoServer.getCatalog();
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public MapsLandingPage landingPage() {
        return new MapsLandingPage(getService(), getCatalog(), "ogc/maps/v1");
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(
                        ConformanceClass.CORE,
                        ConformanceClass.COLLECTIONS,
                        CONF_CLASS_CORE,
                        CONF_CLASS_GEODATA,
                        CONF_CLASS_BBOX,
                        CONF_CLASS_CRS);
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
    public CollectionDocument collection(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
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
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");

        return p;
    }

    @GetMapping(path = "collections/{collectionId}/styles/{styleId}/map", name = "getCollectionMap")
    @ResponseBody
    public WebMap map(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId") String styleId,
            @RequestParam(name = "f") String format,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "width", required = false) Integer width,
            @RequestParam(name = "height", required = false) Integer height,
            @RequestParam(name = "transparent", required = false, defaultValue = "true")
                    boolean transparent,
            @RequestParam(name = "bgcolor", required = false) String bgcolor
            // TODO: add all the vendor parameters we normally support in WMS
            ) throws IOException, FactoryException, ParseException {
        GetMapRequest request =
                toGetMapRequest(
                        collectionId,
                        styleId,
                        format,
                        bbox,
                        crs,
                        datetime,
                        width,
                        height,
                        transparent,
                        bgcolor);

        if ("text/html".equals(format) || "html".equals(format)) {
            DefaultWebMapService.autoSetBoundsAndSize(request);
            if (request.getCrs() != null)
                request.setSRS("EPSG:" + CRS.lookupEpsgCode(request.getCrs(), true));
            request.getRawKvp().put("width", String.valueOf(request.getWidth()));
            request.getRawKvp().put("height", String.valueOf(request.getHeight()));
            if (height != null) request.setHeight(height);
            return new HTMLMap(new WMSMapContent(request));
        } else {
            return wms.reflect(request);
        }
    }

    private List<MapLayerInfo> getMapLayers(PublishedInfo p) {
        if (p instanceof LayerGroupInfo) {
            return ((LayerGroupInfo) p)
                    .layers().stream().map(l -> new MapLayerInfo(l)).collect(Collectors.toList());
        } else if (p instanceof LayerInfo) {
            return Arrays.asList(new MapLayerInfo((LayerInfo) p));
        } else {
            throw new RuntimeException("Unexpected published object" + p);
        }
    }

    private void checkStyle(PublishedInfo p, String styleId) {
        if (p instanceof LayerGroupInfo && StyleDocument.DEFAULT_STYLE_NAME.equals(styleId)) {
            return;
        } else if (p instanceof LayerInfo) {
            LayerInfo l = (LayerInfo) p;
            if (l.getDefaultStyle().prefixedName().equals(styleId)
                    || l.getStyles().stream().anyMatch(s -> s.prefixedName().equals(styleId)))
                return;
        } else {
            throw new RuntimeException("Unexpected published object" + p);
        }
        // in any other case, the style was not recognized
        throw new APIException(
                APIException.INVALID_PARAMETER_VALUE,
                "Invalid style identifier: " + styleId,
                HttpStatus.BAD_REQUEST);
    }

    @GetMapping(
            path = "collections/{collectionId}/styles/{styleId}/map/info",
            name = "getCollectionInfo")
    @ResponseBody
    public FeatureInfoResponse info(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId") String styleId,
            @RequestParam(name = "f") String format,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "crs", required = false) String crs,
            @RequestParam(name = "datetime", required = false) String datetime,
            @RequestParam(name = "width", required = false) Integer width,
            @RequestParam(name = "height", required = false) Integer height,
            @RequestParam(name = "transparent", required = false, defaultValue = "true")
                    boolean transparent,
            @RequestParam(name = "bgcolor", required = false) String bgcolor,
            @RequestParam(name = "i") int i,
            @RequestParam(name = "j") int j
            // TODO: add all the vendor parameters we normally support in WMS
            ) throws IOException, FactoryException, ParseException {
        GetMapRequest getMapRequest =
                toGetMapRequest(
                        collectionId,
                        styleId,
                        "image/png",
                        bbox,
                        crs,
                        datetime,
                        width,
                        height,
                        transparent,
                        bgcolor);
        DefaultWebMapService.autoSetBoundsAndSize(getMapRequest);

        GetFeatureInfoRequest request = new GetFeatureInfoRequest();
        request.setGetMapRequest(getMapRequest);
        request.setXPixel(i);
        request.setYPixel(j);
        request.setInfoFormat(format);
        request.setQueryLayers(getMapRequest.getLayers());

        FeatureCollectionType collection = wms.getFeatureInfo(request);
        return new FeatureInfoResponse(collection, request);
    }

    private GetMapRequest toGetMapRequest(
            String collectionId,
            String styleId,
            String format,
            String bbox,
            String crs,
            String datetime,
            Integer width,
            Integer height,
            boolean transparent,
            String bgcolor)
            throws IOException, FactoryException, ParseException {
        PublishedInfo p = getPublished(collectionId);
        checkStyle(p, styleId);
        StyleInfo styleInfo = getCatalog().getStyleByName(styleId);

        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        request.setLayers(getMapLayers(p));
        if (styleInfo != null) request.setStyles(Arrays.asList(styleInfo.getStyle()));
        request.setFormat(format);
        if (bbox != null) {
            ReferencedEnvelope[] parsed = APIBBoxParser.parse(bbox, crs);
            if (parsed.length > 1)
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Cannot handle dateline crossing requests",
                        HttpStatus.BAD_REQUEST);
            ReferencedEnvelope envelope = parsed[0];
            request.setBbox(envelope);
            request.setCrs(envelope.getCoordinateReferenceSystem());
        }
        if (width != null) request.setWidth(width);
        if (height != null) request.setHeight(height);
        if (bgcolor != null) request.setBgColor(Color.decode(bgcolor));
        request.setTransparent(transparent);
        if (datetime != null) {
            setupTimeSubset(datetime, p, request);
        }
        Map<String, String> rawParamers = new LinkedHashMap<>();
        if (bbox != null) rawParamers.put("bbox", bbox);
        if (crs != null) rawParamers.put("crs", crs);
        rawParamers.put("width", String.valueOf(width));
        rawParamers.put("height", String.valueOf(height));
        rawParamers.put("layers", collectionId);
        rawParamers.put("styles", styleId);
        if (datetime != null) rawParamers.put("datetime", datetime);
        request.setRawKvp(rawParamers);
        // TODO: add other parameters
        return request;
    }

    private void setupTimeSubset(String datetime, PublishedInfo p, GetMapRequest request)
            throws ParseException {
        if (!(p instanceof LayerInfo)) {
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Can only handle time subset on layers, not layer groups",
                    HttpStatus.BAD_REQUEST);
        }
        LayerInfo layer = (LayerInfo) p;
        DimensionInfo time =
                layer.getResource().getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Time dimension is not enabled in this coverage",
                    HttpStatus.BAD_REQUEST);
        }
        @SuppressWarnings("unchecked")
        Collection<Object> times = timeParser.parse(datetime);
        if (times.size() != 1) {
            throw new APIException(
                    INVALID_PARAMETER_VALUE,
                    "Invalid datetime specification, must be a single time, or a time range",
                    HttpStatus.BAD_REQUEST);
        }
        request.setTime(List.copyOf(times));
    }
}
