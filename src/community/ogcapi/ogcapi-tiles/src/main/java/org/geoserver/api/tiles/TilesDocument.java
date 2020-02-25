/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;

public class TilesDocument extends AbstractDocument {

    public static final String TILE_REL = "item";

    public enum Type {
        RenderedTiles,
        RawTiles
    }

    List<TileMatrixSetLink> tileMatrixSetLinks;

    public TilesDocument(WMS wms, TileLayer tileLayer, Type type) {
        this.tileMatrixSetLinks =
                tileLayer
                        .getGridSubsets()
                        .stream()
                        .map(id -> new TileMatrixSetLink(tileLayer.getGridSubset(id)))
                        .collect(Collectors.toList());
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();

        // backlinks in same and other formats
        if (type == Type.RawTiles) {
            addSelfLinks("ogc/tiles/collections/" + id + "/tiles");
        } else {
            addSelfLinks("ogc/tiles/collections/" + id + "/map/tiles");
        }

        // links to tiles
        List<MimeType> tileTypes = tileLayer.getMimeTypes();
        String baseURL = APIRequestInfo.get().getBaseURL();
        if (type == Type.RawTiles) {
            for (MimeType dataFormat :
                    tileTypes.stream().filter(mt -> mt.isVector()).collect(Collectors.toList())) {
                addLinkForFormat(
                        this.id,
                        baseURL,
                        dataFormat.getFormat(),
                        "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                        TILE_REL);
            }
        } else {
            List<MimeType> imageFormats =
                    tileTypes.stream().filter(mt -> !mt.isVector()).collect(Collectors.toList());
            for (MimeType imgeFormat : imageFormats) {
                addLinkForFormat(
                        this.id,
                        baseURL,
                        imgeFormat.getFormat(),
                        "/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                        TILE_REL);
            }
            // add the info links (might be needed only for maps, but we always have a style so...)
            for (String infoFormat : wms.getAvailableFeatureInfoFormats()) {
                addLinkForFormat(
                        this.id,
                        baseURL,
                        infoFormat,
                        "/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}/info",
                        "info");
            }
        }
    }

    public List<TileMatrixSetLink> getTileMatrixSetLinks() {
        return tileMatrixSetLinks;
    }

    protected void addLinkForFormat(
            String layerName, String baseURL, String format, String path, String rel) {
        String apiUrl =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/collections/" + ResponseUtils.urlEncode(layerName) + path,
                        Collections.singletonMap("f", format),
                        URLMangler.URLType.SERVICE);
        addLink(new Link(apiUrl, rel, format, layerName + " tiles as " + format.toString()));
    }
}
