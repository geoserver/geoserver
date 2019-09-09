/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.api.APIException;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.CollectionExtents;
import org.geoserver.api.Link;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class TiledCollectionDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(TiledCollectionDocument.class);
    public static final String DEFAULT_STYLE_NAME = "";
    String title;
    String description;
    WMS wms;
    TileLayer layer;
    String mapPreviewURL;
    CollectionExtents extent;
    TilesDocument tiles;
    List<StyleDocument> styles = new ArrayList<>();

    /**
     * Builds a description of a tiled collection
     *
     * @param tileLayer The tile layer being described
     * @param summary If true, the info provided is minimal and assumed to be part of a {@link
     *     TiledCollectionsDocument}, otherwise it's full and assumed to be the main response
     * @throws FactoryException
     * @throws TransformException
     */
    public TiledCollectionDocument(WMS wms, TileLayer tileLayer, boolean summary)
            throws FactoryException, TransformException, IOException {
        // basic info
        this.layer = tileLayer;
        this.id = tileLayer.getName();

        String baseURL = APIRequestInfo.get().getBaseURL();
        if (tileLayer instanceof GeoServerTileLayer) {
            PublishedInfo published =
                    (PublishedInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo();
            setTitle(published.getTitle());
            setDescription(published.getAbstract());
            this.extent = getExtentsFromPublished(published);
        } else {
            this.extent = getExtentFromGridsets(tileLayer);
        }

        if (summary) {
            // links to the collection description in each format
            Collection<MediaType> metadataFormats =
                    APIRequestInfo.get()
                            .getProducibleMediaTypes(TiledCollectionDocument.class, true);
            for (MediaType format : metadataFormats) {
                String metadataURL =
                        buildURL(
                                baseURL,
                                "ogc/tiles/collections/" + id,
                                Collections.singletonMap("f", format.toString()),
                                URLMangler.URLType.SERVICE);

                Link link =
                        new Link(
                                metadataURL,
                                "collection",
                                format.toString(),
                                "The collection metadata as " + format);
                addLink(link);
            }
        } else {
            // backlinks in same and other formats
            addSelfLinks("ogc/tiles/collections/" + id);

            List<MimeType> tileTypes = tileLayer.getMimeTypes();

            // direct links to tiles are only for vector formats (for now)
            for (MimeType dataFormat :
                    tileTypes.stream().filter(mt -> mt.isVector()).collect(Collectors.toList())) {
                addLinkForFormat(
                        this.id,
                        baseURL,
                        dataFormat.getFormat(),
                        "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                        "tiles");
            }
            // and then links for rendered formats too
            List<MimeType> imageFormats =
                    tileTypes.stream().filter(mt -> !mt.isVector()).collect(Collectors.toList());
            for (MimeType imgeFormat : imageFormats) {
                addLinkForFormat(
                        this.id,
                        baseURL,
                        imgeFormat.getFormat(),
                        "/maps/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                        "tiles");
            }
            // add the info links (might be needed only for maps, but we always have a style so...)
            for (String infoFormat : wms.getAvailableFeatureInfoFormats()) {
                addLinkForFormat(
                        this.id,
                        baseURL,
                        infoFormat,
                        "/maps/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}/info",
                        "info");
            }

            // tile matrixes
            this.tiles = new TilesDocument(tileLayer);

            // styles
            if (tileLayer instanceof GeoServerTileLayer) {
                PublishedInfo published = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
                if (published instanceof LayerInfo) {
                    LayerInfo layer = (LayerInfo) published;
                    LinkedHashSet<StyleInfo> styles =
                            new LinkedHashSet<>(Arrays.asList(layer.getDefaultStyle()));
                    styles.addAll(layer.getStyles());
                    for (StyleInfo style : styles) {
                        this.styles.add(new StyleDocument(style));
                    }
                } else {
                    // layer group? no named styles for the moment
                    this.styles.add(
                            new StyleDocument(DEFAULT_STYLE_NAME, "The layer default style"));
                }
            } else {
                String style = tileLayer.getStyles();
                if (style != null) {
                    this.styles.add(new StyleDocument(style, "The layer default style"));
                } else {
                    this.styles.add(
                            new StyleDocument(DEFAULT_STYLE_NAME, "The layer default style"));
                }
            }
        }
    }

    protected void addLinkForFormat(
            String layerName, String baseURL, String format, String path, String rel) {
        String apiUrl =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/collections/" + ResponseUtils.urlEncode(layerName) + path,
                        Collections.singletonMap("f", format),
                        URLMangler.URLType.SERVICE);
        addLink(new Link(apiUrl, rel, format, layerName + " tiles as " + format));
    }

    CollectionExtents getExtentFromGridsets(TileLayer tileLayer)
            throws FactoryException, TransformException {
        Set<String> srsSet =
                layer.getGridSubsets()
                        .stream()
                        .map(gs -> tileLayer.getGridSubset(gs).getSRS().toString())
                        .collect(Collectors.toSet());
        if (srsSet.isEmpty()) {
            throw new APIException(
                    "IllegalState",
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
            GridSubset subset = layer.getGridSubsetForSRS(SRS.getEPSG4326());
            return getExtentsFromGridSubset(subset);
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

    public CollectionExtents getExtentsFromPublished(PublishedInfo published) {
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

    private boolean isWMSAvailable(GeoServer geoServer) {
        ServiceInfo si =
                geoServer
                        .getServices()
                        .stream()
                        .filter(s -> "WMS".equals(s.getId()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JacksonXmlProperty(localName = "Id")
    public String getId() {
        return id;
    }

    public void setId(String collectionId) {
        id = collectionId;
    }

    @JacksonXmlProperty(localName = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JacksonXmlProperty(localName = "Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CollectionExtents getExtent() {
        return extent;
    }

    public void setExtent(CollectionExtents extent) {
        this.extent = extent;
    }

    public List<Link> getLinks() {
        return links;
    }

    @JsonIgnore
    public String getMapPreviewURL() {
        return mapPreviewURL;
    }

    public TilesDocument getTiles() {
        return tiles;
    }

    public List<StyleDocument> getStyles() {
        return styles;
    }
}
