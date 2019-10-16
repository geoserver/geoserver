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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIException;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.APIService;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.api.OpenAPIMessageConverter;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.wms.WMS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    static final String CORE = "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/core";
    static final String MULTITILE = "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/multitile";
    static final String INFO = "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/info";

    private final GeoServer geoServer;
    private final GWC gwc;
    private final WMS wms;
    private final StorageBroker storageBroker;

    public TilesService(GeoServer geoServer, WMS wms, GWC gwc, StorageBroker storageBroker) {
        this.geoServer = geoServer;
        this.gwc = gwc;
        this.wms = wms;
        this.storageBroker = storageBroker;
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
            OpenAPIMessageConverter.OPEN_API_VALUE,
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
        List<String> classes = Arrays.asList(CORE, MULTITILE, INFO);
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
            throw new APIException(
                    "NotFound",
                    "Tile matrix set " + tileMatrixSetId + " not recognized",
                    HttpStatus.NOT_FOUND);
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
            throw new APIException(
                    "InvalidParameter",
                    "Tiled collection " + collectionId + " not found",
                    HttpStatus.NOT_FOUND,
                    e);
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
            @PathVariable(name = "tileCol") long tileCol)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        return getTileInternal(
                collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, null, false);
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
            @PathVariable(name = "styleId") String styleId)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        return getTileInternal(
                collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, styleId, true);
    }

    ResponseEntity<byte[]> getTileInternal(
            String collectionId,
            String tileMatrixSetId,
            String tileMatrix,
            long tileRow,
            long tileCol,
            String styleId,
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
        ConveyorTile tile =
                new ConveyorTile(
                        storageBroker,
                        name, // using the tile id won't work with storage broker
                        tileMatrixSetId,
                        tileIndex,
                        requestedFormat,
                        filterParameters(styleId),
                        null,
                        null);
        tile = tileLayer.getTile(tile);
        if (tile == null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("geowebcache-cache-result", MISS.toString());
            headers.add("geowebcache-miss-reason", "unknown");
            ResponseEntity response = new ResponseEntity(headers, HttpStatus.NOT_FOUND);
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
        final byte[] hash = MessageDigest.getInstance("MD5").digest(tileBytes);
        final String etag = GWC.getETag(tileBytes);
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
        HttpHeaders headers = new HttpHeaders();
        tmpHeaders.forEach((k, v) -> headers.add(k, v));
        // content type and disposition
        headers.add(HttpHeaders.CONTENT_TYPE, tile.getMimeType().getMimeType());
        headers.add(
                HttpHeaders.CONTENT_DISPOSITION,
                getTileFileName(tileMatrixSetId, tileMatrix, tileRow, tileCol, tileLayer, tile));

        return new ResponseEntity(tileBytes, headers, HttpStatus.OK);
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
            throw new APIException(
                    "InvalidParameterValue",
                    "Invalid style name, please check the collection description for valid style names: "
                            + tileLayer.getStyles(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Checks the specified griset is supported by the tile layer, and returns it
     *
     * @param tileLayer
     * @param tileMatrixSetId
     * @return
     */
    public static GridSubset getGridSubset(TileLayer tileLayer, String tileMatrixSetId) {
        GridSubset gridSubset = tileLayer.getGridSubset(tileMatrixSetId);
        if (gridSubset == null) {
            throw new APIException(
                    "InvalidParameterValue",
                    "Invalid tileMatrixSetId " + tileMatrixSetId,
                    HttpStatus.BAD_REQUEST);
        }

        return gridSubset;
    }

    private Map<String, String> filterParameters(String styleId) {
        return styleId != null
                ? Collections.singletonMap("styles", styleId)
                : Collections.emptyMap();
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
            throw new APIException(
                    "InvalidParameterValue",
                    "Invalid tileMatrixSetId " + tileMatrixSetId,
                    HttpStatus.BAD_REQUEST);
        }
        long z = gridSubset.getGridIndex(tileMatrix);

        if (z < 0) {
            throw new APIException(
                    "InvalidParameterValue",
                    "Unknown tileMatrix " + tileMatrix,
                    HttpStatus.BAD_REQUEST);
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
            throw new APIException(
                    "InvalidParameterValue",
                    "The layer does not seem to have any cached format suitable for "
                            + "this type of resource, check the resource is listed "
                            + "among the tiled collection links",
                    HttpStatus.BAD_REQUEST);
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
}
