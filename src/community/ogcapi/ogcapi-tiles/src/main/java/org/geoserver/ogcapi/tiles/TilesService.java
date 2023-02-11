/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;
import static org.geowebcache.conveyor.Conveyor.CacheResult.MISS;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.InvalidParameterValueException;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.QueryablesBuilder;
import org.geoserver.ogcapi.ResourceNotFoundException;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(
        service = "Tiles",
        version = "1.0.1",
        landingPage = "ogc/tiles",
        serviceClass = TilesServiceInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/tiles")
public class TilesService {

    static final Logger LOGGER = Logging.getLogger(TilesService.class);

    public static final String CC_TILE_CORE =
            "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/core";

    public static final String CC_TILESET =
            "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tileset";
    public static final String CC_MULTITILES =
            "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/multitiles";
    public static final String CC_INFO = "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/info";

    public static final String CC_TILES_TILE_MATRIX_SET =
            "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tmxs";
    public static final String CC_TILE_MATRIX_SET =
            "http://www.opengis.net/spec/tilematrixset/1.0/conf/tilematrixset";
    public static final String CC_TILE_MATRIX_SET_JSON =
            "http://www.opengis.net/spec/tilematrixset/1.0/conf/json-tilematrixset";

    private static final String DISPLAY_NAME = "OGC API Tiles";

    private final GeoServer geoServer;
    private final GWC gwc;
    private final WMS wms;
    private final StorageBroker storageBroker;
    private final APIFilterParser filterParser;

    public TilesService(
            GeoServer geoServer,
            WMS wms,
            GWC gwc,
            StorageBroker storageBroker,
            APIFilterParser filterParser) {
        this.geoServer = geoServer;
        this.gwc = gwc;
        this.wms = wms;
        this.storageBroker = storageBroker;
        this.filterParser = filterParser;
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public TilesLandingPage getLandingPage() {
        TilesServiceInfo service = getService();
        return new TilesLandingPage(
                (service.getTitle() == null) ? "Tiles server" : service.getTitle(),
                (service.getAbstract() == null) ? "" : service.getAbstract());
    }

    public TilesServiceInfo getService() {
        return geoServer.getService(TilesServiceInfo.class);
    }

    @GetMapping(
            path = "openapi",
            name = "getApi",
            produces = {
                OPEN_API_MEDIA_TYPE_VALUE,
                APPLICATION_YAML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new TilesAPIBuilder(gwc).build(getService());
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(
                        ConformanceClass.CORE,
                        ConformanceClass.COLLECTIONS,
                        CC_TILE_CORE,
                        CC_TILESET,
                        CC_MULTITILES,
                        CC_INFO,
                        CC_TILES_TILE_MATRIX_SET,
                        CC_TILE_MATRIX_SET,
                        CC_TILE_MATRIX_SET_JSON);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(path = "tileMatrixSets", name = "getTileMatrixSets")
    @ResponseBody
    @HTMLResponseBody(templateName = "tileMatrixSets.ftl", fileName = "tileMatrixSets.html")
    public TileMatrixSets getTileMatrixSets() {
        return new TileMatrixSets(gwc);
    }

    @GetMapping(path = "tileMatrixSets/{tileMatrixSetId}", name = "getTileMatrixSet")
    @ResponseBody
    @HTMLResponseBody(templateName = "tileMatrixSet.ftl", fileName = "tileMatrixSet.html")
    public TileMatrixSetDocument getTileMatrixSet(
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId) {
        GridSet gridSet = gwc.getGridSetBroker().get(tileMatrixSetId);
        if (gridSet == null) {
            throw new ResourceNotFoundException(
                    "Tile matrix set " + tileMatrixSetId + " not recognized");
        }
        return new TileMatrixSetDocument(gridSet, false);
    }

    @GetMapping(path = "collections", name = "getCollections")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public TiledCollectionsDocument getCollections() {
        return new TiledCollectionsDocument(geoServer, wms, gwc);
    }

    @GetMapping(path = "collections/{collectionId}/styles", name = "getCollectionStyles")
    @ResponseBody
    @HTMLResponseBody(templateName = "styles.ftl", fileName = "styles.html")
    public StylesDocument getCollectionStyles(
            @PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        StylesDocument styles = new StylesDocument(tileLayer);

        return styles;
    }

    @GetMapping(path = "collections/{collectionId}/tiles", name = "describeTilesets")
    @ResponseBody
    @HTMLResponseBody(templateName = "tiles.ftl", fileName = "tiles.html")
    public TilesDocument describeTilesets(@PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        TilesDocument tiles = new TilesDocument(wms, tileLayer, Tileset.DataType.vector);

        return tiles;
    }

    @GetMapping(path = "collections/{collectionId}/tiles/{tileMatrixId}", name = "describeTileset")
    @ResponseBody
    @HTMLResponseBody(templateName = "tileset.ftl", fileName = "tileset.html")
    public Tileset describeTileset(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixId") String tileMatrixId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        Tileset tiles = new Tileset(wms, tileLayer, Tileset.DataType.vector, tileMatrixId, true);

        return tiles;
    }

    @GetMapping(path = "collections/{collectionId}/map/tiles", name = "describeDefaultMapTilesets")
    @ResponseBody
    @HTMLResponseBody(templateName = "tiles.ftl", fileName = "tiles.html")
    public TilesDocument describeDefaultMapTilesets(
            @PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        return describeStyledMapTilesets(collectionId, null);
    }

    @GetMapping(
            path = "collections/{collectionId}/styles/{styleId}/map/tiles",
            name = "describeStyledMapTilesets")
    @ResponseBody
    @HTMLResponseBody(templateName = "tiles-style.ftl", fileName = "tiles.html")
    public TilesDocument describeStyledMapTilesets(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId") String styleId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        TilesDocument tiles = new TilesDocument(wms, tileLayer, Tileset.DataType.map, styleId);

        return tiles;
    }

    @GetMapping(
            path = "collections/{collectionId}/map/tiles/{tileMatrixId}",
            name = "describeMapTileset")
    @ResponseBody
    @HTMLResponseBody(templateName = "tileset.ftl", fileName = "tileset.html")
    public Tileset describeMapTileset(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixId") String tileMatrixId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        Tileset tiles = new Tileset(wms, tileLayer, Tileset.DataType.map, tileMatrixId, true);

        return tiles;
    }

    @GetMapping(
            path = "collections/{collectionId}/styles/{styleId}/map/tiles/{tileMatrixId}",
            name = "describeMapTileset")
    @ResponseBody
    @HTMLResponseBody(templateName = "tileset-style.ftl", fileName = "tileset.html")
    public Tileset describeStyledMapTileset(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId") String styleId,
            @PathVariable(name = "tileMatrixId") String tileMatrixId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        Tileset tiles =
                new Tileset(wms, tileLayer, Tileset.DataType.map, tileMatrixId, styleId, true);

        return tiles;
    }

    private TileLayer getTileLayer(String collectionId) {
        try {
            return gwc.getTileLayerByName(collectionId);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(
                    "Tiled collection " + collectionId + " not found", e);
        }
    }

    @GetMapping(path = "collections/{collectionId}", name = "describeCollection")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public TiledCollectionDocument collection(
            @PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        TiledCollectionDocument collection = new TiledCollectionDocument(wms, tileLayer, false);

        return collection;
    }

    @GetMapping(
            path =
                    "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
            name = "getTile")
    @ResponseBody
    public ResponseEntity<byte[]> getRawTile(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @PathVariable(name = "tileMatrix") String tileMatrix,
            @PathVariable(name = "tileRow") long tileRow,
            @PathVariable(name = "tileCol") long tileCol,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        return getTileInternal(
                collectionId,
                tileMatrixSetId,
                tileMatrix,
                tileRow,
                tileCol,
                null,
                filter,
                filterLanguage,
                false);
    }

    @GetMapping(
            path =
                    "/collections/{collectionId}/styles/{styleId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
            name = "getStyledMapTile")
    @ResponseBody
    public ResponseEntity<byte[]> getStyledMapTile(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @PathVariable(name = "tileMatrix") String tileMatrix,
            @PathVariable(name = "tileRow") long tileRow,
            @PathVariable(name = "tileCol") long tileCol,
            @PathVariable(name = "styleId") String styleId,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        return getTileInternal(
                collectionId,
                tileMatrixSetId,
                tileMatrix,
                tileRow,
                tileCol,
                styleId,
                filter,
                filterLanguage,
                true);
    }

    @GetMapping(
            path =
                    "/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
            name = "getDefaultMapTile")
    @ResponseBody
    public ResponseEntity<byte[]> getDefaultMapTileTile(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @PathVariable(name = "tileMatrix") String tileMatrix,
            @PathVariable(name = "tileRow") long tileRow,
            @PathVariable(name = "tileCol") long tileCol,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        return getTileInternal(
                collectionId,
                tileMatrixSetId,
                tileMatrix,
                tileRow,
                tileCol,
                null,
                filter,
                filterLanguage,
                true);
    }

    ResponseEntity<byte[]> getTileInternal(
            String collectionId,
            String tileMatrixSetId,
            String tileMatrix,
            long tileRow,
            long tileCol,
            String styleId,
            String filterSpec,
            String filterLanguage,
            boolean renderedTile)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        // run the request
        TileLayer tileLayer = getTileLayer(collectionId);
        if (styleId != null) {
            validateStyle(tileLayer, styleId);
            if (isLayerGroup(tileLayer)) {
                // only a notion of default style, remove the styleId
                styleId = null;
            }
        }
        MimeType requestedFormat =
                getRequestedFormat(
                        tileLayer, renderedTile, APIRequestInfo.get().getRequestedMediaTypes());
        long[] tileIndex = getTileIndex(tileMatrixSetId, tileMatrix, tileRow, tileCol, tileLayer);
        String name = getTileLayerId(tileLayer);
        Filter filter = filterParser.parse(filterSpec, filterLanguage);
        String cqlSpecification = toCQLSpecification(tileLayer, filter);
        ConveyorTile tile =
                new ConveyorTile(
                        storageBroker,
                        name, // using the tile id won't work with storage broker
                        tileMatrixSetId,
                        tileIndex,
                        requestedFormat,
                        filterParameters(styleId, cqlSpecification),
                        null,
                        null);
        boolean tileIsCacheable = filterSpec == null || supportsCQLFilter(tileLayer, filterSpec);
        if (tileIsCacheable) {
            tile = tileLayer.getTile(tile);
        } else {
            if (!(tileLayer instanceof GeoServerTileLayer)) {
                throw new InvalidParameterValueException("Filter is not supported on this layer");
            }
            // if geoserver tile layer, run the filter with no meta tiling, otherwise throw an
            // exception
            VolatileGeoServerTileLayer volatileLayer =
                    new VolatileGeoServerTileLayer((GeoServerTileLayer) tileLayer);
            volatileLayer.getTile(tile);
            TileObject so = tile.getStorageObject();
            if (so != null) {
                so.setCreated(System.currentTimeMillis());
            }
        }

        if (tile == null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("geowebcache-cache-result", MISS.toString());
            headers.add("geowebcache-miss-reason", "unknown");
            return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
        }

        final byte[] tileBytes;
        {
            final Resource mapContents = tile.getBlob();
            if (mapContents instanceof ByteArrayResource) {
                tileBytes = ((ByteArrayResource) mapContents).getContents();
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                mapContents.transferTo(Channels.newChannel(out));
                tileBytes = out.toByteArray();
            }
        }

        // Handle Etags
        HttpServletRequest httpRequest = APIRequestInfo.get().getRequest();
        final String ifNoneMatch = httpRequest.getHeader("If-None-Match");
        final String etag = getETag(tileBytes);
        if (etag.equals(ifNoneMatch)) {
            // Client already has the current version
            LOGGER.finer("ETag matches, returning 304");
            return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
        }

        LOGGER.finer("No matching ETag, returning cached tile");

        // setup general GWC headers
        LinkedHashMap<String, String> tmpHeaders = new LinkedHashMap<>();
        GWC.setCacheControlHeaders(tmpHeaders, tileLayer, (int) tileIndex[2]);
        GWC.setConditionalGetHeaders(
                tmpHeaders, tile, etag, httpRequest.getHeader("If-Modified-Since"));
        GWC.setCacheMetadataHeaders(tmpHeaders, tile, tileLayer);
        // override for workspace specific services
        tmpHeaders.put(
                "geowebcache-layer",
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName());
        if (filterSpec != null && !tileIsCacheable) {
            tmpHeaders.put("geowebcache-cache-result", MISS.toString());
            tmpHeaders.put(
                    "geowebcache-miss-reason",
                    "CQL_FILTER filter parameter not cached or not condition not matched");
        }
        HttpHeaders headers = new HttpHeaders();
        tmpHeaders.forEach((k, v) -> headers.add(k, v));
        // content type and disposition
        headers.add(HttpHeaders.CONTENT_TYPE, tile.getMimeType().getMimeType());
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                getTileFileName(tileMatrixSetId, tileMatrix, tileRow, tileCol, tileLayer, tile));

        return new ResponseEntity<>(tileBytes, headers, HttpStatus.OK);
    }

    static String getTileLayerId(TileLayer tileLayer) {
        return tileLayer instanceof GeoServerTileLayer
                ? ((GeoServerTileLayer) tileLayer).getContextualName()
                : tileLayer.getName();
    }

    private String getETag(byte[] tileBytes) throws NoSuchAlgorithmException {
        if (tileBytes == null) {
            return "EMPTY_TILE";
        }
        final byte[] hash = MessageDigest.getInstance("MD5").digest(tileBytes);
        return GWC.getETag(tileBytes);
    }

    private String toCQLSpecification(TileLayer tileLayer, Filter filter) {
        if (filter == null) {
            return null;
        }
        // we might use the exact filter provided in input, but it should round trip ok
        // and makes it easier to support multiple filter languages in the future
        String cqlSpec = ECQL.toCQL(filter);
        return tileLayer.getParameterFilters().stream()
                .filter(pf -> pf.getKey().equalsIgnoreCase("CQL_FILTER") && pf.applies((cqlSpec)))
                .map(
                        pf -> {
                            try {
                                return pf.apply(cqlSpec);
                            } catch (ParameterException e) {
                                throw new RuntimeException(
                                        "Tested before if it was applicable, this exception should not happen",
                                        e);
                            }
                        })
                .findFirst()
                .orElse(cqlSpec);
    }

    private boolean supportsCQLFilter(TileLayer tileLayer, String cqlSpec) {
        return tileLayer.getParameterFilters().stream()
                .anyMatch(pf -> pf.getKey().equalsIgnoreCase("CQL_FILTER") && pf.applies(cqlSpec));
    }

    public static void validateStyle(TileLayer tileLayer, String styleId) {
        // is it the default style? if so, nothing to check
        if (styleId.equalsIgnoreCase(tileLayer.getStyles())) {
            return;
        }
        if (isLayerGroup(tileLayer)) {
            String name = getLayerGroupStyleName(tileLayer);
            if (!styleId.equals(name)) {
                throw new InvalidParameterValueException(
                        "Invalid style name, please check the collection description for valid style names: "
                                + name);
            }
        } else {
            // look for the other possible values
            Optional<ParameterFilter> styles =
                    tileLayer.getParameterFilters().stream()
                            .filter(pf -> "styles".equalsIgnoreCase(pf.getKey()))
                            .findFirst();
            if (!styles.isPresent() || !styles.get().applies(styleId)) {
                throw new InvalidParameterValueException(
                        "Invalid style name, please check the collection description for valid style names: "
                                + tileLayer.getStyles());
            }
        }
    }

    static boolean isLayerGroup(TileLayer tileLayer) {
        if (tileLayer instanceof GeoServerTileLayer) {
            return ((GeoServerTileLayer) tileLayer).getPublishedInfo() instanceof LayerGroupInfo;
        }

        return false;
    }

    static boolean isStyleGroup(LayerGroupInfo lg) {
        if (lg.getRootLayer() != null) return false;

        // a simple style group uses a single top level style as its definition, and no layers
        return lg.getStyles().size() == 1
                && lg.getStyles().get(0) != null
                && lg.getLayers().stream().allMatch(l -> l == null);
    }

    static String getLayerGroupStyleName(TileLayer tileLayer) {
        LayerGroupInfo group = (LayerGroupInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo();
        if (isStyleGroup(group)) {
            return group.getStyles().get(0).getName();
        } else {
            return StyleDocument.DEFAULT_STYLE_NAME;
        }
    }

    /** Checks the specified gridset is supported by the tile layer, and returns it */
    public static GridSubset getGridSubset(TileLayer tileLayer, String tileMatrixSetId) {
        GridSubset gridSubset = tileLayer.getGridSubset(tileMatrixSetId);
        if (gridSubset == null) {
            throw new InvalidParameterValueException("Invalid tileMatrixSetId " + tileMatrixSetId);
        }

        return gridSubset;
    }

    private Map<String, String> filterParameters(String styleId, String cqlSpec) {
        Map<String, String> params = new HashMap<>();
        if (styleId != null) {
            params.put("styles", styleId);
        }
        if (cqlSpec != null) {
            params.put("cql_filter", cqlSpec);
        }
        return params;
    }

    public String getTileFileName(
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @PathVariable(name = "tileMatrix") String tileMatrix,
            @PathVariable(name = "tileRow") long tileRow,
            @PathVariable(name = "tileCol") long tileCol,
            TileLayer tileLayer,
            ConveyorTile tile) {
        String layerName = getTileLayerId(tileLayer);
        return layerName
                + "_"
                + getExternalZIndex(tileMatrixSetId, tileMatrix, tileLayer)
                + "_"
                + tileRow
                + "_"
                + tileCol
                + tile.getMimeType().getFileExtension();
    }

    private long[] getTileIndex(
            String tileMatrixSetId,
            String tileMatrix,
            long tileRow,
            long tileCol,
            TileLayer tileLayer) {
        GridSubset gridSubset = tileLayer.getGridSubset(tileMatrixSetId);
        if (gridSubset == null) {
            throw new InvalidParameterValueException("Invalid tileMatrixSetId " + tileMatrixSetId);
        }
        long z = gridSubset.getGridIndex(tileMatrix);

        if (z < 0) {
            throw new InvalidParameterValueException("Unknown tileMatrix " + tileMatrix);
        }
        final long tilesHigh = gridSubset.getNumTilesHigh((int) z);
        long y = tilesHigh - tileRow - 1;
        long x = tileCol;
        long[] gridCov = gridSubset.getCoverage((int) z);
        if (x < gridCov[0] || x > gridCov[2]) {
            throw new APIException(
                    APIException.NOT_FOUND,
                    "Column " + x + " is out of range, min: " + gridCov[0] + " max:" + gridCov[2],
                    HttpStatus.NOT_FOUND);
        }
        if (y < gridCov[1] || y > gridCov[3]) {
            long minRow = tilesHigh - gridCov[3] - 1;
            long maxRow = tilesHigh - gridCov[1] - 1;

            throw new APIException(
                    APIException.NOT_FOUND,
                    "Row " + tileRow + " is out of range, min: " + minRow + " max:" + maxRow,
                    HttpStatus.NOT_FOUND);
        }

        return new long[] {x, y, z};
    }

    private long getExternalZIndex(String tileMatrixSetId, String tileMatrix, TileLayer tileLayer) {
        GridSubset gridSubset = tileLayer.getGridSubset(tileMatrixSetId);
        return gridSubset.getGridIndex(tileMatrix);
    }

    public static MimeType getRequestedFormat(
            TileLayer tileLayer, boolean renderedTile, List<MediaType> requestedTypes) {
        Map<MediaType, MimeType> layerTypes =
                tileLayer.getMimeTypes().stream()
                        .filter(mt -> renderedTile ? !mt.isVector() : mt.isVector())
                        .collect(
                                Collectors.toMap(
                                        mt -> MediaType.parseMediaType(mt.getFormat()),
                                        mt -> mt,
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        if (layerTypes.isEmpty()) {
            throw new InvalidParameterValueException(
                    "The layer does not seem to have any cached format suitable for "
                            + "this type of resource, check the resource is listed "
                            + "among the tiled collection links");
        }

        if (requestedTypes == null || requestedTypes.isEmpty()) {
            // default to the first found
            return layerTypes.values().iterator().next();
        } else {
            // process the requested types in order, return the first compatible layer type
            for (MediaType requestedType : requestedTypes) {
                for (Map.Entry<MediaType, MimeType> layerType : layerTypes.entrySet()) {
                    if (requestedType.equals(layerType.getKey())) {
                        // requested types can be generic, layer types are specific
                        return layerType.getValue();
                    }
                }
            }
        }

        // could not find a match? The request did not follow the advertised formats then
        throw new APIException(
                APIException.INVALID_PARAMETER_VALUE,
                "Could not find a tile media type matching the requested resource (either invalid format, or not supported on this resource)",
                HttpStatus.BAD_REQUEST);
    }

    @GetMapping(path = "collections/{collectionId}/queryables", name = "getQueryables")
    @ResponseBody
    @HTMLResponseBody(templateName = "queryables.ftl", fileName = "queryables.html")
    public Queryables queryables(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        if (!supportsFiltering(tileLayer)) {
            throw new ResourceNotFoundException(
                    "Collection '"
                            + collectionId
                            + "' cannot be filtered, no queryables available");
        }

        FeatureTypeInfo ft =
                (FeatureTypeInfo)
                        ((LayerInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo())
                                .getResource();
        String id =
                ResponseUtils.buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        "ogc/tiles/collections/"
                                + ResponseUtils.urlEncode(collectionId)
                                + "/queryables",
                        null,
                        URLMangler.URLType.RESOURCE);
        return new QueryablesBuilder(id).forType(ft).build();
    }

    /** Utility method to check if a given tile layer supports filtering */
    public static boolean supportsFiltering(TileLayer tileLayer) {
        return (tileLayer instanceof GeoServerTileLayer)
                && (((GeoServerTileLayer) tileLayer).getPublishedInfo() instanceof LayerInfo)
                && (((LayerInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo()).getResource()
                        instanceof FeatureTypeInfo);
    }

    @GetMapping(
            path = "/collections/{collectionId}/map/tiles/{tileMatrixSetId}/metadata",
            name = "getTilesMetadata")
    @ResponseBody
    public TileJSON getTileJSON(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @RequestParam(name = "tileFormat") String format)
            throws FactoryException, TransformException, NoSuchAlgorithmException,
                    GeoWebCacheException, IOException {
        return getTileJSONInternal(collectionId, null, format, tileMatrixSetId);
    }

    @GetMapping(
            path =
                    "/collections/{collectionId}/styles/{styleId}/map/tiles/{tileMatrixSetId}/metadata",
            name = "getTilesMetadata")
    @ResponseBody
    public TileJSON getTileJSON(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "styleId") String styleId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @RequestParam(name = "tileFormat") String format)
            throws FactoryException, TransformException, NoSuchAlgorithmException,
                    GeoWebCacheException, IOException {
        return getTileJSONInternal(collectionId, styleId, format, tileMatrixSetId);
    }

    @GetMapping(
            path = "/collections/{collectionId}/tiles/{tileMatrixSetId}/metadata",
            name = "getTilesMetadata")
    @ResponseBody
    public TileJSON getTileJSON(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId)
            throws FactoryException, TransformException, NoSuchAlgorithmException,
                    GeoWebCacheException, IOException {
        return getTileJSONInternal(
                collectionId, null, "application/vnd.mapbox-vector-tile", tileMatrixSetId);
    }

    private TileJSON getTileJSONInternal(
            String collectionId, String styleId, String tileFormat, String tileMatrixSetId)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException, TransformException,
                    FactoryException {
        TileLayer tileLayer = getTileLayer(collectionId);
        if (styleId != null) {
            validateStyle(tileLayer, styleId);
        }
        return new TileJSONBuilder(collectionId, tileFormat, tileMatrixSetId, tileLayer)
                .style(styleId)
                .build();
    }
}
