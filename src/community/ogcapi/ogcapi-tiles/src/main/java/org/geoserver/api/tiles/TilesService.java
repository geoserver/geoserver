/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

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
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIException;
import org.geoserver.api.APIFilterParser;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.APIService;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.api.InvalidParameterValueException;
import org.geoserver.api.OpenAPIMessageConverter;
import org.geoserver.api.QueryablesDocument;
import org.geoserver.api.ResourceNotFoundException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
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
    version = "1.0",
    landingPage = "ogc/tiles",
    serviceClass = TilesServiceInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/tiles")
public class TilesService {

    static final Logger LOGGER = Logging.getLogger(TilesService.class);

    public static final String CC_CORE = "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/core";
    public static final String CC_MULTITILE =
            "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/multitile";
    public static final String CC_INFO = "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/info";

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
        path = "api",
        name = "getApi",
        produces = {
            OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE,
            "application/x-yaml",
            MediaType.TEXT_XML_VALUE
        }
    )
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() {
        return new TilesAPIBuilder(gwc).build(getService());
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CC_CORE, CC_MULTITILE, CC_INFO);
        return new ConformanceDocument(classes);
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

    @GetMapping(path = "collections/{collectionId}/tiles", name = "describeTiles")
    @ResponseBody
    @HTMLResponseBody(templateName = "tiles.ftl", fileName = "tiles.html")
    public TilesDocument describeTiles(@PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        TilesDocument tiles = new TilesDocument(wms, tileLayer, TilesDocument.Type.RawTiles);

        return tiles;
    }

    @GetMapping(path = "collections/{collectionId}/map/tiles", name = "describeMapTiles")
    @ResponseBody
    @HTMLResponseBody(templateName = "tiles.ftl", fileName = "tiles.html")
    public TilesDocument describeMapTiles(@PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        TilesDocument tiles = new TilesDocument(wms, tileLayer, TilesDocument.Type.RenderedTiles);

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
        name = "getTile"
    )
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
                "/collections/{collectionId}/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
        name = "getMapTile"
    )
    @ResponseBody
    public ResponseEntity<byte[]> getRenderedTile(
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
        }
        MimeType requestedFormat =
                getRequestedFormat(
                        tileLayer, renderedTile, APIRequestInfo.get().getRequestedMediaTypes());
        long[] tileIndex = getTileIndex(tileMatrixSetId, tileMatrix, tileRow, tileCol, tileLayer);
        String name =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
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
            return new ResponseEntity(headers, HttpStatus.NOT_FOUND);
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
            return new ResponseEntity(HttpStatus.NOT_MODIFIED);
        }

        LOGGER.finer("No matching ETag, returning cached tile");

        // setup general GWC headers
        LinkedHashMap<String, String> tmpHeaders = new LinkedHashMap<>();
        GWC.setCacheControlHeaders(tmpHeaders, tileLayer);
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

        return new ResponseEntity(tileBytes, headers, HttpStatus.OK);
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
        return tileLayer
                .getParameterFilters()
                .stream()
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
        return tileLayer
                .getParameterFilters()
                .stream()
                .anyMatch(pf -> pf.getKey().equalsIgnoreCase("CQL_FILTER") && pf.applies(cqlSpec));
    }

    public static void validateStyle(TileLayer tileLayer, String styleId) {
        // is it the default style? if so, nothing to check
        if (styleId.equalsIgnoreCase(tileLayer.getStyles())) {
            return;
        }
        // look for the other possible values
        Optional<ParameterFilter> styles =
                tileLayer
                        .getParameterFilters()
                        .stream()
                        .filter(pf -> "styles".equalsIgnoreCase(pf.getKey()))
                        .findFirst();
        if (!styles.isPresent() && !styles.get().applies(styleId)) {
            throw new InvalidParameterValueException(
                    "Invalid style name, please check the collection description for valid style names: "
                            + tileLayer.getStyles());
        }
    }

    /** Checks the specified griset is supported by the tile layer, and returns it */
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
        String layerName =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
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
                    "TileOutOfRange",
                    "Column " + x + " is out of range, min: " + gridCov[0] + " max:" + gridCov[2],
                    HttpStatus.BAD_REQUEST);
        }
        if (y < gridCov[1] || y > gridCov[3]) {
            long minRow = tilesHigh - gridCov[3] - 1;
            long maxRow = tilesHigh - gridCov[1] - 1;

            throw new APIException(
                    "TileOutOfRange",
                    "Row " + tileRow + " is out of range, min: " + minRow + " max:" + maxRow,
                    HttpStatus.BAD_REQUEST);
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
                tileLayer
                        .getMimeTypes()
                        .stream()
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
                "InvalidParameter",
                "Could not find a tile media type matching the requested resource (either invalid format, or not supported on this resource)",
                HttpStatus.BAD_REQUEST);
    }

    @GetMapping(path = "collections/{collectionId}/queryables", name = "getQueryables")
    @ResponseBody
    @HTMLResponseBody(templateName = "queryables.ftl", fileName = "queryables.html")
    public QueryablesDocument queryables(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        TileLayer tileLayer = getTileLayer(collectionId);
        if (!supportsFiltering(tileLayer)) {
            throw new ResourceNotFoundException(
                    "Collection '"
                            + collectionId
                            + "' cannot be filtered, no queryables available");
        }

        return new QueryablesDocument(
                (FeatureTypeInfo)
                        ((LayerInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo())
                                .getResource());
    }

    /** Utility method to check if a given tile layer supports filtering */
    public static boolean supportsFiltering(TileLayer tileLayer) {
        return (tileLayer instanceof GeoServerTileLayer)
                && (((GeoServerTileLayer) tileLayer).getPublishedInfo() instanceof LayerInfo)
                && (((LayerInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo()).getResource()
                        instanceof FeatureTypeInfo);
    }
}
