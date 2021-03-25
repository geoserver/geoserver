package org.geoserver.ogcapi.features.tiled;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.geoserver.gwc.GWC;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.tiles.TileJSON;
import org.geoserver.ogcapi.tiles.TileMatrixSetDocument;
import org.geoserver.ogcapi.tiles.TileMatrixSetLink;
import org.geoserver.ogcapi.tiles.TileMatrixSets;
import org.geoserver.ogcapi.tiles.TilesDocument;
import org.geoserver.ogcapi.tiles.TilesService;
import org.geoserver.wfs.WFSInfo;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Extends the {@link org.geoserver.ogcapi.features.FeatureService} with native tiling abilities, as
 * provided by the {@link org.geotools.tile.TileService}
 */
@APIService(
    service = "Features",
    version = "1.0",
    landingPage = "ogc/features",
    serviceClass = WFSInfo.class,
    core = false
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/features")
public class TiledFeatureService {

    TilesService delegate;
    GWC gwc;

    public TiledFeatureService(TilesService delegate, GWC gwc) {
        this.delegate = delegate;
        this.gwc = gwc;
    }

    @HTMLResponseBody(
        templateName = "tileMatrixSets.ftl",
        fileName = "tileMatrixSets.html",
        baseClass = TilesService.class
    )
    @ResponseBody
    @GetMapping(path = "tileMatrixSets", name = "getTileMatrixSets")
    public TileMatrixSets getTileMatrixSets() {
        TileMatrixSets response = delegate.getTileMatrixSets();
        rebaseLinks(response);
        return response;
    }

    @HTMLResponseBody(
        templateName = "tileMatrixSet.ftl",
        fileName = "tileMatrixSet.html",
        baseClass = TilesService.class
    )
    @ResponseBody
    @GetMapping(path = "tileMatrixSets/{tileMatrixSetId}", name = "getTileMatrixSet")
    public TileMatrixSetDocument getTileMatrixSet(
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId) {
        TileMatrixSetDocument response = delegate.getTileMatrixSet(tileMatrixSetId);
        rebaseLinks(response);
        return response;
    }

    @GetMapping(path = "collections/{collectionId}/tiles", name = "describeTiles")
    @ResponseBody
    @HTMLResponseBody(
        templateName = "tiles.ftl",
        fileName = "tiles.html",
        baseClass = TilesService.class
    )
    public TilesDocument describeTiles(@PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        if (!isTiledVectorLayer(collectionId)) {
            throw new APIException(
                    "NotATileLayer", collectionId + " is not a tiled layer", HttpStatus.NOT_FOUND);
        }
        TilesDocument response = delegate.describeTiles(collectionId);
        rebaseLinks(response);
        for (TileMatrixSetLink tmsLink : response.getTileMatrixSetLinks()) {
            String uri = tmsLink.getTileMatrixSetURI();
            if (uri.contains("tiles/tileMatrixSets")) {
                String rebasedURI =
                        uri.replace("/tiles/tileMatrixSets", "/features/tileMatrixSets");
                tmsLink.setTileMatrixSetURI(rebasedURI);
            }
        }

        return response;
    }

    @GetMapping(
        path = "/collections/{collectionId}/tiles/{tileMatrixSetId}/metadata",
        name = "getTilesMetadata"
    )
    @ResponseBody
    public TileJSON getTileJSON(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId)
            throws FactoryException, TransformException, NoSuchAlgorithmException,
                    GeoWebCacheException, IOException {
        TileJSON response = delegate.getTileJSON(collectionId, tileMatrixSetId);
        String[] uris =
                Arrays.stream(response.getTiles())
                        .map(uri -> uri.replace("/ogc/tiles", "/ogc/features"))
                        .toArray(s -> new String[s]);
        response.setTiles(uris);
        return response;
    }

    @ResponseBody
    @GetMapping(
        path =
                "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow"
                        + "}/{tileCol}",
        name = "getTile"
    )
    public ResponseEntity<byte[]> getRawTile(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId,
            @PathVariable(name = "tileMatrix") String tileMatrix,
            @PathVariable(name = "tileRow") long tileRow,
            @PathVariable(name = "tileCol") long tileCol,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "filter-lang", required = false) String filterLanguage)
            throws GeoWebCacheException, IOException, NoSuchAlgorithmException {
        if (!isTiledVectorLayer(collectionId)) {
            throw new APIException(
                    "NotATileLayer", collectionId + " is not a tiled layer", HttpStatus.NOT_FOUND);
        }
        return delegate.getRawTile(
                collectionId,
                tileMatrixSetId,
                tileMatrix,
                tileRow,
                tileCol,
                filter,
                filterLanguage);
    }

    private void rebaseLinks(AbstractDocument document) {
        for (Link link : document.getLinks()) {
            String href = link.getHref();
            if (href.contains("/tiles/tileMatrixSets")) {
                String rebasedHref =
                        href.replace("/tiles/tileMatrixSets", "/features/tileMatrixSets");
                link.setHref(rebasedHref);
            } else if (href.contains("/tiles/collections")) {
                String rebasedHref = href.replace("/tiles/collections", "/features/collections");
                link.setHref(rebasedHref);
            }
        }
    }

    /** Exposes the TilesService API for the callbacks to use */
    OpenAPI tileServiceAPI() throws IOException {
        return delegate.api();
    }

    /**
     * Checks if the layer in question has a tile configuration, and exposes vector based tile
     * formats.
     */
    public boolean isTiledVectorLayer(String collectionId) {
        try {
            TileLayer layer = gwc.getTileLayerByName(collectionId);
            return layer.getMimeTypes().stream().anyMatch(mt -> mt.isVector());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
