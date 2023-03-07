/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import static org.geoserver.ows.util.ResponseUtils.appendPath;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.mime.MimeType;

public class Tileset extends AbstractDocument {

    public static final String TILE_REL = "item";
    private final String styleId;
    private final String tileMatrixId;

    /** The type of tileset, according to the spec */
    public enum DataType {
        map,
        vector,
        coverage
    }

    String tileMatrixSetURI;
    String tileMatrixSetDefinition;
    DataType dataType;
    private final String gridSubsetId;

    List<TileMatrixSetLimit> tileMatrixSetLimits = new ArrayList<>();

    public Tileset(
            WMS wms,
            TileLayer tileLayer,
            DataType dataType,
            String tileMatrixId,
            boolean addDetails) {
        this(wms, tileLayer, dataType, tileMatrixId, null, addDetails);
    }

    public Tileset(
            WMS wms,
            TileLayer tileLayer,
            DataType dataType,
            String tileMatrixId,
            String styleId,
            boolean addDetails) {
        GridSubset gridSubset = tileLayer.getGridSubset(tileMatrixId);
        this.gridSubsetId = tileMatrixId;
        this.dataType = dataType;
        this.styleId = styleId;
        this.tileMatrixId = tileMatrixId;
        String baseURL = APIRequestInfo.get().getBaseURL();

        // TODO: link definition to local URL, but if the matrix is a well known one,
        // use the well known URI for tileMatrixSetURI instead
        this.tileMatrixSetURI =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/v1/tileMatrixSets/"
                                + ResponseUtils.urlEncode(gridSubset.getGridSet().getName()),
                        null,
                        URLMangler.URLType.SERVICE);
        this.tileMatrixSetDefinition = this.tileMatrixSetURI;

        boolean hasLimits =
                !gridSubset.fullGridSetCoverage()
                        || (gridSubset.getGridSet().getNumLevels()
                                != gridSubset.getZoomStop() - gridSubset.getZoomStart() + 1);
        if (hasLimits && addDetails) {
            String[] levelNames = gridSubset.getGridNames();
            long[][] wmtsLimits = gridSubset.getWMTSCoverages();

            for (int i = 0; i < levelNames.length; i++) {
                TileMatrixSetLimit limit =
                        new TileMatrixSetLimit(
                                levelNames[i],
                                wmtsLimits[i][1],
                                wmtsLimits[i][3],
                                wmtsLimits[i][0],
                                wmtsLimits[i][2]);
                validateLimits(limit, gridSubset, i);
                tileMatrixSetLimits.add(limit);
            }
        }

        // go for the links
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
        if (dataType == DataType.vector) {
            addSelfLinks("ogc/tiles/v1/collections/" + id + "/tiles/" + tileMatrixId);
        } else if (dataType == DataType.map) {
            if (styleId == null) {
                addSelfLinks("ogc/tiles/v1/collections/" + id + "/map/tiles/" + tileMatrixId);
            } else {
                addSelfLinks(
                        "ogc/tiles/v1/collections/"
                                + id
                                + "/styles/"
                                + styleId
                                + "/map/tiles/"
                                + tileMatrixId);
            }
        } else {
            throw new IllegalArgumentException("Cannot handle data type: " + dataType);
        }

        if (addDetails) {
            // links depend on the data type
            List<MimeType> tileTypes = tileLayer.getMimeTypes();
            if (dataType == DataType.vector) {
                tileTypes.stream()
                        .filter(mt -> mt.isVector())
                        .collect(Collectors.toList())
                        .forEach(
                                dataFormat ->
                                        addTilesLinkForFormat(
                                                this.id,
                                                baseURL,
                                                dataFormat.getFormat(),
                                                appendPath(
                                                        "/tiles/",
                                                        tileMatrixId,
                                                        "/{tileMatrix}/{tileRow}/{tileCol}"),
                                                TILE_REL));

                // tileJSON
                new LinksBuilder(TileJSON.class, "ogc/tiles/v1/collections")
                        .segment(id, true)
                        .segment("tiles")
                        .segment(tileMatrixId, true)
                        .segment("metadata")
                        .title("Tiles metadata as ")
                        .rel("describedBy")
                        .classification("metadata")
                        .updater((m, l) -> l.setTemplated(true))
                        .add(this);
            } else if (dataType == DataType.map) {
                List<MimeType> imageFormats =
                        tileTypes.stream()
                                .filter(mt -> !mt.isVector())
                                .collect(Collectors.toList());
                String base =
                        styleId != null ? "/styles/" + styleId + "/map/tiles/" : "/map/tiles/";
                imageFormats.forEach(
                        imageFormat ->
                                addTilesLinkForFormat(
                                        this.id,
                                        baseURL,
                                        imageFormat.getFormat(),
                                        appendPath(
                                                base,
                                                tileMatrixId,
                                                "/{tileMatrix}/{tileRow}/{tileCol}"),
                                        TILE_REL));

                // add the info links (might be needed only for maps, but we always have a style
                // so...)
                wms.getAvailableFeatureInfoFormats()
                        .forEach(
                                infoFormat ->
                                        addTilesLinkForFormat(
                                                this.id,
                                                baseURL,
                                                infoFormat,
                                                appendPath(
                                                        base,
                                                        tileMatrixId,
                                                        "/{tileMatrix}/{tileRow}/{tileCol}/info"),
                                                "info"));

                // tileJSON
                new LinksBuilder(TileJSON.class, "ogc/tiles/v1/collections/")
                        .segment(id, true)
                        .segment(base)
                        .segment(tileMatrixId, true)
                        .segment("metadata")
                        .title("Tiles metadata as ")
                        .rel("describedBy")
                        .classification("metadata")
                        .updater(
                                (m, l) -> {
                                    l.setTemplated(true);
                                    l.setHref(l.getHref() + "&tileFormat={tileFormat}");
                                })
                        .add(this);
            } else {
                throw new IllegalArgumentException(
                        "Tiles of this type are not yet supported: " + dataType);
            }
        }
    }

    private void validateLimits(TileMatrixSetLimit limit, GridSubset gridSubset, int zoomLevel) {

        long numTilesHeight = gridSubset.getGridSet().getGrid(zoomLevel).getNumTilesHigh();
        long numTilesWide = gridSubset.getGridSet().getGrid(zoomLevel).getNumTilesWide();
        if (limit.getMinTileRow() < 0) limit.setMinTileRow(0);
        if (limit.getMinTileCol() < 0) limit.setMinTileCol(0);
        if (limit.getMaxTileRow() > numTilesHeight - 1) limit.setMaxTileRow(numTilesHeight - 1);
        if (limit.getMaxTileCol() > numTilesWide - 1) limit.setMaxTileCol(numTilesWide - 1);
    }

    public String getTileMatrixSetURI() {
        return tileMatrixSetURI;
    }

    public void setTileMatrixSetURI(String tileMatrixSetURI) {
        this.tileMatrixSetURI = tileMatrixSetURI;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<TileMatrixSetLimit> getTileMatrixSetLimits() {
        return tileMatrixSetLimits;
    }

    public void setTileMatrixSetLimits(List<TileMatrixSetLimit> tileMatrixSetLimits) {
        this.tileMatrixSetLimits = tileMatrixSetLimits;
    }

    public String getTileMatrixSetDefinition() {
        return tileMatrixSetDefinition;
    }

    public void setTileMatrixSetDefinition(String tileMatrixSetDefinition) {
        this.tileMatrixSetDefinition = tileMatrixSetDefinition;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @JsonIgnore
    public String getGridSubsetId() {
        return gridSubsetId;
    }

    @Override
    public String toString() {
        return "Tileset{"
                + "tileMatrixSetURI='"
                + tileMatrixSetURI
                + '\''
                + ", tileMatrixSetDefinition='"
                + tileMatrixSetDefinition
                + '\''
                + ", dataType="
                + dataType
                + ", tileMatrixSetLimits="
                + tileMatrixSetLimits
                + '}';
    }

    protected final void addTilesLinkForFormat(
            String layerName, String baseURL, String format, String path, String rel) {
        String apiUrl =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/v1/collections/" + ResponseUtils.urlEncode(layerName) + path,
                        Collections.singletonMap("f", format),
                        URLMangler.URLType.SERVICE);
        Link link = new Link(apiUrl, rel, format, layerName + " tiles as " + format);
        link.setTemplated(true);
        addLink(link);
    }

    @JsonIgnore
    public String getStyleId() {
        return styleId;
    }

    @JsonIgnore
    public String getTileMatrixId() {
        return tileMatrixId;
    }
}
