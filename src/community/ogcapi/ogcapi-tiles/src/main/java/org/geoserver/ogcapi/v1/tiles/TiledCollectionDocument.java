/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.wms.WMS;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.springframework.http.HttpStatus;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links", "styles"})
public class TiledCollectionDocument extends AbstractCollectionDocument<TileLayer> {
    static final Logger LOGGER = Logging.getLogger(TiledCollectionDocument.class);

    public static final String REL_TILESETS_MAP =
            "http://www.opengis.net/def/rel/ogc/1.0/tilesets-map";
    public static final String REL_TILESETS_VECTOR =
            "http://www.opengis.net/def/rel/ogc/1.0/tilesets-vector";

    WMS wms;
    TileLayer layer;
    List<StyleDocument> styles = new ArrayList<>();
    boolean dataTiles;
    boolean mapTiles;
    boolean queryable;

    /**
     * Builds a description of a tiled collection
     *
     * @param tileLayer The tile layer being described
     * @param summary If true, the info provided is minimal and assumed to be part of a {@link
     *     TiledCollectionsDocument}, otherwise it's full and assumed to be the main response
     */
    public TiledCollectionDocument(WMS wms, TileLayer tileLayer, boolean summary)
            throws FactoryException, TransformException {
        super(tileLayer);
        // basic info
        this.layer = tileLayer;
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
        if (tileLayer instanceof GeoServerTileLayer) {
            PublishedInfo published = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
            setTitle(published.getTitle());
            setDescription(published.getAbstract());
            this.extent = getExtentsFromPublished(published);
        } else {
            this.extent = getExtentFromGridsets(tileLayer);
        }

        // backlinks in same and other formats
        addSelfLinks("ogc/tiles/v1/collections/" + id);

        if (!summary) {
            // raw tiles links, if any (if the vector tiles plugin is missing or formats not
            // configured, will be empty)
            List<MimeType> tileTypes = tileLayer.getMimeTypes();

            dataTiles = tileTypes.stream().anyMatch(mt -> mt.isVector());
            if (dataTiles) {
                // tiles
                new LinksBuilder(TilesDocument.class, "ogc/tiles/v1/collections/")
                        .segment(id, true)
                        .segment("tiles")
                        .title("Tiles metadata as ")
                        .rel(REL_TILESETS_VECTOR)
                        .classification("tilesets-vector")
                        .add(this);
            }

            // map tiles links (a layer might not have image tiles configured, need to check)
            mapTiles = tileTypes.stream().anyMatch(mt -> !mt.isVector());
            if (mapTiles) {
                new LinksBuilder(TilesDocument.class, "ogc/tiles/v1/collections/")
                        .segment(id, true)
                        .segment("map/tiles")
                        .title("Map tiles metadata as ")
                        .rel(REL_TILESETS_MAP)
                        .classification("tilesets-maps")
                        .add(this);
            }

            // styles document links
            new LinksBuilder(StylesDocument.class, "ogc/tiles/v1/collections/")
                    .segment(id, true)
                    .segment("/styles")
                    .title("Styles for this layer as ")
                    .rel("styles")
                    .add(this);

            // style links
            if (tileLayer instanceof GeoServerTileLayer) {
                PublishedInfo published = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
                if (published instanceof LayerInfo) {
                    LayerInfo layerInfo = (LayerInfo) published;
                    LinkedHashSet<StyleInfo> stylesInfo =
                            new LinkedHashSet<>(Arrays.asList(layerInfo.getDefaultStyle()));
                    stylesInfo.addAll(layerInfo.getStyles());
                    stylesInfo.forEach(
                            style -> {
                                this.styles.add(new StyleDocument(style));
                            });
                } else {
                    LayerGroupInfo group = (LayerGroupInfo) published;
                    if (group != null && TilesService.isStyleGroup(group)) {
                        StyleDocument styleDocument = new StyleDocument(group.getStyles().get(0));
                        this.styles.add(styleDocument);
                    } else {
                        // layer group? no named styles for the moment
                        this.styles.add(
                                new StyleDocument(
                                        StyleDocument.DEFAULT_STYLE_NAME,
                                        "The layer default style"));
                    }
                }
            } else {
                String style = tileLayer.getStyles();
                if (style != null) {
                    this.styles.add(new StyleDocument(style, "The layer default style"));
                } else {
                    this.styles.add(
                            new StyleDocument(
                                    StyleDocument.DEFAULT_STYLE_NAME, "The layer default style"));
                }
            }

            // filtering support
            if (TilesService.supportsFiltering(tileLayer)) {
                this.queryable = true;
                new LinksBuilder(Queryables.class, "ogc/tiles/v1/collections/")
                        .segment(id, true)
                        .segment("/queryables")
                        .title("Collection queryables as ")
                        .rel("queryables")
                        .add(this);
            }
        }
    }

    private CollectionExtents getExtentFromGridsets(TileLayer tileLayer)
            throws FactoryException, TransformException {
        Set<String> srsSet =
                layer.getGridSubsets().stream()
                        .map(gs -> tileLayer.getGridSubset(gs).getSRS().toString())
                        .collect(Collectors.toSet());
        if (srsSet.isEmpty()) {
            throw new APIException(
                    APIException.NO_APPLICABLE_CODE,
                    "Could not compute the extent for layer "
                            + tileLayer.getName()
                            + ", no gridsets are configured",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (srsSet.contains("EPSG:4326")) {
            GridSubset subset = layer.getGridSubsetForSRS(SRS.getEPSG4326());
            return getExtentsFromGridSubset(subset);
        } else {
            // take the first and reproject...
            String srs = srsSet.iterator().next();

            try {
                GridSubset subset = layer.getGridSubsetForSRS(SRS.getSRS(srs));
                return getExtentsFromGridSubset(subset);
            } catch (GeoWebCacheException ex) {
                throw new APIException(
                        APIException.NO_APPLICABLE_CODE,
                        "Could not convert " + srs + " value: " + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private CollectionExtents getExtentsFromGridSubset(GridSubset subset)
            throws FactoryException, TransformException {
        BoundingBox bbox = subset.getOriginalExtent();
        ReferencedEnvelope re =
                new ReferencedEnvelope(
                        bbox.getMinX(),
                        bbox.getMaxX(),
                        bbox.getMinY(),
                        bbox.getMaxY(),
                        CRS.decode(subset.getSRS().toString(), true));
        if (!CRS.equalsIgnoreMetadata(
                re.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84)) {
            re = re.transform(DefaultGeographicCRS.WGS84, true);
        }
        return new CollectionExtents(re);
    }

    private CollectionExtents getExtentsFromPublished(PublishedInfo published) {
        try {
            ReferencedEnvelope bbox = null;
            if (published instanceof LayerInfo) {
                bbox = ((LayerInfo) published).getResource().getLatLonBoundingBox();
            } else if (published instanceof LayerGroupInfo) {
                bbox = ((LayerGroupInfo) published).getBounds();
                if (!CRS.equalsIgnoreMetadata(bbox, DefaultGeographicCRS.WGS84)) {
                    bbox = bbox.transform(DefaultGeographicCRS.WGS84, true);
                }
            }
            if (bbox != null) {
                return new CollectionExtents(bbox);
            }
        } catch (TransformException | FactoryException e) {
            throw new APIException(
                    "InternalError",
                    "Failed to reproject native bounds to WGS84",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }

        return null;
    }

    @Override
    public List<StyleDocument> getStyles() {
        return styles;
    }

    public boolean isDataTiles() {
        return dataTiles;
    }

    public boolean isMapTiles() {
        return mapTiles;
    }

    @JsonIgnore
    public boolean isQueryable() {
        return queryable;
    }
}
