/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features.tiled;

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
import org.geoserver.ogcapi.v1.features.FeatureService;
import org.geoserver.ogcapi.v1.tiles.TileMatrixSetDocument;
import org.geoserver.ogcapi.v1.tiles.TileMatrixSets;
import org.geoserver.ogcapi.v1.tiles.TilesDocument;
import org.geoserver.ogcapi.v1.tiles.TilesService;
import org.geoserver.ogcapi.v1.tiles.Tileset;
import org.geoserver.wfs.WFSInfo;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.TileJSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Extends the {@link FeatureService} with native tiling abilities, as provided by the
 * {@link org.geotools.tile.TileService}
 */
@APIService(
        service = "Features",
        version = "1.0.1",
        landingPage = "ogc/features/v1",
        serviceClass = WFSInfo.class,
        core = false)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/features/v1")
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
            baseClass = TilesService.class)
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
            baseClass = TilesService.class)
    @ResponseBody
    @GetMapping(path = "tileMatrixSets/{tileMatrixSetId}", name = "getTileMatrixSet")
    public TileMatrixSetDocument getTileMatrixSet(@PathVariable(name = "tileMatrixSetId") String tileMatrixSetId) {
        TileMatrixSetDocument response = delegate.getTileMatrixSet(tileMatrixSetId);
        rebaseLinks(response);
        return response;
    }

    @GetMapping(path = "collections/{collectionId}/tiles", name = "describeTilesets")
    @ResponseBody
    @HTMLResponseBody(templateName = "tiles.ftl", fileName = "tiles.html", baseClass = TilesService.class)
    public TilesDocument describeTiles(@PathVariable(name = "collectionId") String collectionId)
            throws FactoryException, TransformException, IOException {
        if (!isTiledVectorLayer(collectionId)) {
            throw new APIException(
                    APIException.NOT_FOUND, collectionId + " is not a tiled layer", HttpStatus.NOT_FOUND);
        }
        TilesDocument response = delegate.describeTilesets(collectionId);
        rebaseLinks(response);
        for (Tileset tileset : response.getTilesets()) {
            String uri = tileset.getTileMatrixSetURI();
            if (uri.contains("tiles/v1/tileMatrixSets")) {
                String rebasedURI = uri.replace("/tiles/v1/tileMatrixSets", "/features/v1/tileMatrixSets");
                tileset.setTileMatrixSetURI(rebasedURI);
                tileset.setTileMatrixSetDefinition(rebasedURI);
            }
            rebaseLinks(tileset);
        }

        return response;
    }

    @GetMapping(path = "collections/{collectionId}/tiles/{tileMatrixId}", name = "describeTileset")
    @ResponseBody
    @HTMLResponseBody(templateName = "tileset.ftl", fileName = "tileset.html")
    public Tileset describeTileset(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixId") String tileMatrixId)
            throws FactoryException, TransformException, IOException {
        if (!isTiledVectorLayer(collectionId)) {
            throw new APIException(
                    APIException.NOT_FOUND, collectionId + " is not a tiled layer", HttpStatus.NOT_FOUND);
        }
        Tileset tileset = delegate.describeTileset(collectionId, tileMatrixId);
        rebaseLinks(tileset);
        String uri = tileset.getTileMatrixSetURI();
        if (uri.contains("tiles/v1/tileMatrixSets")) {
            String rebasedURI = uri.replace("/tiles/v1/tileMatrixSets", "/features/v1/tileMatrixSets");
            tileset.setTileMatrixSetURI(rebasedURI);
            tileset.setTileMatrixSetDefinition(rebasedURI);
        }
        rebaseLinks(tileset);

        return tileset;
    }

    @GetMapping(path = "/collections/{collectionId}/tiles/{tileMatrixSetId}/metadata", name = "getTilesMetadata")
    @ResponseBody
    public TileJSON getTileJSON(
            @PathVariable(name = "collectionId") String collectionId,
            @PathVariable(name = "tileMatrixSetId") String tileMatrixSetId)
            throws FactoryException, TransformException, NoSuchAlgorithmException, GeoWebCacheException, IOException {
        TileJSON response = delegate.getTileJSON(collectionId, tileMatrixSetId);
        String[] uris = Arrays.stream(response.getTiles())
                .map(uri -> uri.replace("/ogc/tiles/v1", "/ogc/features/v1"))
                .toArray(s -> new String[s]);
        response.setTiles(uris);
        return response;
    }

    @ResponseBody
    @GetMapping(
            path = "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow" + "}/{tileCol}",
            name = "getTile")
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
                    APIException.NOT_FOUND, collectionId + " is not a tiled layer", HttpStatus.NOT_FOUND);
        }
        return delegate.getRawTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, filter, filterLanguage);
    }

    private void rebaseLinks(AbstractDocument document) {
        for (Link link : document.getLinks()) {
            String href = link.getHref();
            if (href.contains("/tiles/v1/tileMatrixSets")) {
                String rebasedHref = href.replace("/tiles/v1/tileMatrixSets", "/features/v1/tileMatrixSets");
                link.setHref(rebasedHref);
            } else if (href.contains("/tiles/v1/collections")) {
                String rebasedHref = href.replace("/tiles/v1/collections", "/features/v1/collections");
                link.setHref(rebasedHref);
            }
        }
    }

    /** Exposes the TilesService API for the callbacks to use */
    OpenAPI tileServiceAPI() throws IOException {
        return delegate.api();
    }

    /** Checks if the layer in question has a tile configuration, and exposes vector based tile formats. */
    public boolean isTiledVectorLayer(String collectionId) {
        try {
            TileLayer layer = gwc.getTileLayerByName(collectionId);
            return layer.getMimeTypes().stream().anyMatch(mt -> mt.isVector());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
