/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.InvalidParameterValueException;
import org.geoserver.ogcapi.ResourceNotFoundException;
import org.geoserver.ogcapi.tiles.VectorLayerMetadata.GeometryType;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/** Helper class building a TileJSON specification from a Tile */
class TileJSONBuilder {

    String collectionId;
    String styleId;
    String tileFormat;
    String tileMatrixSetId;
    TileLayer tileLayer;

    public TileJSONBuilder(
            String collectionId, String tileFormat, String tileMatrixSetId, TileLayer tileLayer) {
        this.collectionId = collectionId;
        this.tileFormat = tileFormat;
        this.tileMatrixSetId = tileMatrixSetId;
        this.tileLayer = tileLayer;
    }

    public TileJSONBuilder style(String styleId) {
        this.styleId = styleId;
        return this;
    }

    public TileJSON build() throws FactoryException, TransformException, IOException {
        // generic information available from every layer
        TileJSON tileJSON = new TileJSON();
        tileJSON.setName(collectionId);
        GridSubset subset = tileLayer.getGridSubset(tileMatrixSetId);
        if (subset == null) {
            throw new ResourceNotFoundException(
                    "Tiled collection "
                            + collectionId
                            + " does not support tile matrix set "
                            + tileMatrixSetId);
        }
        tileJSON.setMinZoom(
                Optional.ofNullable(subset.getMinCachedZoom()).orElse(subset.getZoomStart()));
        tileJSON.setMaxZoom(
                Optional.ofNullable(subset.getMaxCachedZoom()).orElse(subset.getZoomStop()));
        LayerMetaInformation metadata = tileLayer.getMetaInformation();
        if (metadata != null) {
            tileJSON.setDescription(metadata.getDescription());
        }
        // first tentative at getting the bounds, using the tile coverage (we can be more precise
        // with GeoServer layers, later)
        BoundingBox bounds = subset.getCoverageBestFitBounds();
        if (bounds != null) {
            ReferencedEnvelope envelope =
                    new ReferencedEnvelope(
                            bounds.getMinX(),
                            bounds.getMaxX(),
                            bounds.getMinY(),
                            bounds.getMaxY(),
                            CRS.decode(subset.getSRS().toString()));
            ReferencedEnvelope wgs84Envelope = envelope.transform(DefaultGeographicCRS.WGS84, true);
            ReferencedEnvelope intersection =
                    wgs84Envelope.intersection(new Envelope(-180, 180, -90, 90));
            tileJSON.setBounds(
                    new double[] {
                        intersection.getMinX(),
                        intersection.getMinY(),
                        intersection.getMaxX(),
                        intersection.getMaxY()
                    });

            // compute a center too
            long[] coverageBestFit = subset.getCoverageBestFit();
            if (coverageBestFit != null) {
                tileJSON.setCenter(
                        new double[] {
                            intersection.getMedian(0), intersection.getMedian(1), coverageBestFit[4]
                        });
            }
        }
        // the tile links
        String tilesURL;
        APIRequestInfo requestInfo = APIRequestInfo.get();
        String baseURL = requestInfo.getBaseURL();
        if (styleId != null) {
            tilesURL =
                    ResponseUtils.buildURL(
                            baseURL,
                            "ogc/tiles/collections/"
                                    + urlEncode(collectionId)
                                    + "/map/"
                                    + urlEncode(styleId)
                                    + "/tiles/"
                                    + tileMatrixSetId
                                    + "/{z}/{y}/{x}",
                            Collections.singletonMap("f", tileFormat),
                            URLMangler.URLType.SERVICE);
        } else {
            tilesURL =
                    ResponseUtils.buildURL(
                            baseURL,
                            "ogc/tiles/collections/"
                                    + urlEncode(collectionId)
                                    + "/tiles/"
                                    + tileMatrixSetId
                                    + "/{z}/{y}/{x}",
                            Collections.singletonMap("f", tileFormat),
                            URLMangler.URLType.SERVICE);
        }
        tileJSON.setTiles(new String[] {tilesURL});

        if (!(tileLayer instanceof GeoServerTileLayer)) {
            throw new InvalidParameterValueException(
                    "TileJSON metadata is not supported on this layer");
        }

        GeoServerTileLayer gtl = (GeoServerTileLayer) tileLayer;
        PublishedInfo published = gtl.getPublishedInfo();
        if (published instanceof LayerInfo) {
            decorateTileJSON((LayerInfo) published, tileJSON, styleId == null);
        } else if (published instanceof LayerGroupInfo) {
            return decorateTileJSON((LayerGroupInfo) published, tileJSON, styleId == null);
        }

        return tileJSON;
    }

    private TileJSON decorateTileJSON(LayerGroupInfo group, TileJSON tileJSON, boolean vector)
            throws TransformException, FactoryException, IOException {
        ReferencedEnvelope groupBounds = group.getBounds();
        ReferencedEnvelope bounds = groupBounds.transform(DefaultGeographicCRS.WGS84, true);
        tileJSON.setBounds(
                new double[] {
                    bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()
                });

        if (vector) {
            LayerGroupHelper helper = new LayerGroupHelper(group);
            List<LayerInfo> layers = helper.allLayers();
            List<VectorLayerMetadata> metadatas = new ArrayList<>();
            for (LayerInfo layer : layers) {
                ResourceInfo resource = layer.getResource();
                if (resource instanceof FeatureTypeInfo) {
                    VectorLayerMetadata metadata =
                            getVectorLayerMetadata((FeatureTypeInfo) resource);
                    metadatas.add(metadata);
                }
            }
            tileJSON.setLayers(metadatas);
        }

        return tileJSON;
    }

    private void decorateTileJSON(LayerInfo published, TileJSON tileJSON, boolean vector)
            throws IOException {
        ResourceInfo resource = published.getResource();
        ReferencedEnvelope bounds = resource.getLatLonBoundingBox();
        tileJSON.setBounds(
                new double[] {
                    bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()
                });

        if (vector && resource instanceof FeatureTypeInfo) {
            VectorLayerMetadata layerMetadata = getVectorLayerMetadata((FeatureTypeInfo) resource);
            tileJSON.setLayers(Arrays.asList(layerMetadata));
        }
    }

    private VectorLayerMetadata getVectorLayerMetadata(FeatureTypeInfo fti) throws IOException {
        VectorLayerMetadata metadata = new VectorLayerMetadata();
        metadata.setId(fti.getName());
        metadata.setDescription(fti.getAbstract());
        FeatureType featureType = fti.getFeatureType();
        if (!(featureType instanceof SimpleFeatureType)) {
            return null;
        }

        // set the geometry type
        SimpleFeatureType simple = (SimpleFeatureType) featureType;
        if (simple.getGeometryDescriptor() != null) {
            Class<?> binding = simple.getGeometryDescriptor().getType().getBinding();
            if (LineString.class.isAssignableFrom(binding)
                    || MultiLineString.class.isAssignableFrom(binding)) {
                metadata.setGeometryType(GeometryType.line);
            } else if (Polygon.class.isAssignableFrom(binding)
                    || MultiPolygon.class.isAssignableFrom(binding)) {
                metadata.setGeometryType(GeometryType.polygon);
            } else if (Point.class.isAssignableFrom(binding)
                    || MultiPoint.class.isAssignableFrom(binding)) {
                metadata.setGeometryType(GeometryType.point);
            }
        }
        Map<String, String> fields =
                simple.getAttributeDescriptors()
                        .stream()
                        .filter(d -> !(d instanceof GeometryDescriptor))
                        .collect(
                                Collectors.toMap(
                                        d -> d.getLocalName(), d -> toTypeName(d.getType())));
        metadata.setFields(fields);

        return metadata;
    }

    private String toTypeName(AttributeType type) {
        // use the same mappings used for the queryables
        return org.geoserver.ogcapi.AttributeType.fromClass(type.getBinding()).getType();
    }
}
