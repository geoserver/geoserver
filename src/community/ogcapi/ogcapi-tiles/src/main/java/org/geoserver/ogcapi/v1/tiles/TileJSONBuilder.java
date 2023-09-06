/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geoserver.catalog.AttributionInfo;
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
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.NumberRange;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileJSONProvider;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.layer.meta.VectorLayerMetadata;
import org.geowebcache.layer.meta.VectorLayerMetadata.GeometryType;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

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
        LayerMetaInformation metadata = tileLayer.getMetaInformation();

        tileJSON.setFormat(tileFormat);
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
        boolean isVector = (styleId == null) && isVector(tileFormat);
        // isVector allows controlling if vector_layers metadata should be returned or not,
        // as well as whether the "map" prefix should be used in tile urls
        if (styleId != null) {
            tilesURL =
                    ResponseUtils.buildURL(
                            baseURL,
                            "ogc/tiles/v1/collections/"
                                    + urlEncode(collectionId)
                                    + "/styles/"
                                    + urlEncode(styleId)
                                    + "/map"
                                    + "/tiles/"
                                    + tileMatrixSetId
                                    + "/{z}/{y}/{x}",
                            Collections.singletonMap("f", tileFormat),
                            URLMangler.URLType.SERVICE);
        } else {
            String tilesPrefix = isVector ? "" : "/map";
            tilesURL =
                    ResponseUtils.buildURL(
                            baseURL,
                            "ogc/tiles/v1/collections/"
                                    + urlEncode(collectionId)
                                    + tilesPrefix
                                    + "/tiles/"
                                    + tileMatrixSetId
                                    + "/{z}/{y}/{x}",
                            Collections.singletonMap("f", tileFormat),
                            URLMangler.URLType.SERVICE);
        }
        tileJSON.setTiles(new String[] {tilesURL});

        if (!(tileLayer instanceof GeoServerTileLayer)) {
            TileJSONProvider tileJSONProvider = (TileJSONProvider) tileLayer;
            if (tileJSONProvider.supportsTileJSON()) {
                tileJSON = tileJSONProvider.getTileJSON();
                tileJSON.setTiles(new String[] {tilesURL});
                tileJSON.setFormat(tileFormat);
                return tileJSON;
            }
            throw new InvalidParameterValueException(
                    "TileJSON metadata is not supported on this layer");
        }

        GeoServerTileLayer gtl = (GeoServerTileLayer) tileLayer;
        PublishedInfo published = gtl.getPublishedInfo();

        AttributionInfo attributionInfo = published.getAttribution();
        if (attributionInfo != null) tileJSON.setAttribution(attributionInfo.getTitle());
        String description = published.getAbstract();
        if (description == null && metadata != null) {
            description = metadata.getDescription();
        }
        tileJSON.setDescription(description);

        if (published instanceof LayerInfo) {
            decorateTileJSON((LayerInfo) published, tileJSON, isVector, subset);
        } else if (published instanceof LayerGroupInfo) {
            tileJSON = decorateTileJSON((LayerGroupInfo) published, tileJSON, isVector, subset);
        }
        setTileJSONZoomLevels(tileJSON, subset);
        return tileJSON;
    }

    private boolean isVector(String tileFormat) {
        MimeType format = null;
        if (tileFormat != null) {
            try {
                format = MimeType.createFromFormat(tileFormat);
            } catch (MimeException e) {
                // Ignore the exception
                // we will return false if something goes wrong
            }
        }
        return format != null ? format.isVector() : false;
    }

    private TileJSON decorateTileJSON(
            LayerGroupInfo group, TileJSON tileJSON, boolean vector, GridSubset subset)
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
                VectorLayerMetadata metadata = getVectorLayerMetadata(layer, subset);
                if (metadata != null) metadatas.add(metadata);
            }
            tileJSON.setLayers(metadatas);
        }

        return tileJSON;
    }

    private void decorateTileJSON(
            LayerInfo published, TileJSON tileJSON, boolean vector, GridSubset subset)
            throws IOException {
        ResourceInfo resource = published.getResource();
        ReferencedEnvelope bounds = resource.getLatLonBoundingBox();
        tileJSON.setBounds(
                new double[] {
                    bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()
                });

        if (vector) {
            VectorLayerMetadata layerMetadata = getVectorLayerMetadata(published, subset);
            if (layerMetadata != null) tileJSON.setLayers(Arrays.asList(layerMetadata));
        }
    }

    private VectorLayerMetadata getVectorLayerMetadata(LayerInfo layerInfo, GridSubset subset)
            throws IOException {
        ResourceInfo ri = layerInfo.getResource();
        if (!(ri instanceof FeatureTypeInfo)) return null;
        FeatureTypeInfo fti = (FeatureTypeInfo) ri;
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
                simple.getAttributeDescriptors().stream()
                        .filter(d -> !(d instanceof GeometryDescriptor))
                        .collect(
                                Collectors.toMap(
                                        d -> d.getLocalName(), d -> toTypeName(d.getType())));
        metadata.setFields(fields);
        NumberRange<Integer> zoomRange = findLayerZoomLevel(layerInfo, subset);
        metadata.setMinZoom(zoomRange.getMinValue());
        metadata.setMaxZoom(zoomRange.getMaxValue());
        return metadata;
    }

    private String toTypeName(AttributeType type) {
        // use the same mappings used for the queryables
        return org.geoserver.ogcapi.AttributeType.fromClass(type.getBinding()).getType();
    }

    // find layer's min max scale denominators and matches them with the ones in the gridset
    // to find the corresponding min and max zoom levels
    private NumberRange<Integer> findLayerZoomLevel(LayerInfo layerInfo, GridSubset subset)
            throws IOException {
        NumberRange<Double> scaleRange = CapabilityUtil.searchMinMaxScaleDenominator(layerInfo);

        double minScale = scaleRange.getMinimum();
        double maxScale = scaleRange.getMaximum();

        GridSet gridSet = subset.getGridSet();

        int startLevel =
                Optional.ofNullable(subset.getMinCachedZoom()).orElse(subset.getZoomStart());
        int endLevel = Optional.ofNullable(subset.getMaxCachedZoom()).orElse(subset.getZoomStop());
        int limit = startLevel + gridSet.getNumLevels();

        double startScale = gridSet.getGrid(startLevel).getScaleDenominator();
        double endScale = gridSet.getGrid(endLevel).getScaleDenominator();

        boolean foundStartLevel = maxScale > startScale ? true : false;
        boolean foundEndLevel = minScale < endScale ? true : false;
        double maxScaleDist = Double.MAX_VALUE;
        double minScaleDist = Double.MAX_VALUE;

        for (int zoomLevel = startLevel; zoomLevel < limit; zoomLevel++) {
            Grid grid = gridSet.getGrid(zoomLevel);
            double gridScale = grid.getScaleDenominator();
            if (!foundStartLevel) {
                double distance = Math.abs(gridScale - maxScale);
                if (distance < maxScaleDist && gridScale < maxScale) {
                    startLevel = zoomLevel;
                    maxScaleDist = distance;
                }
            }
            if (!foundEndLevel) {
                double distance = gridScale - minScale;
                if (distance < minScaleDist && gridScale > minScale) {
                    endLevel = zoomLevel;
                    minScaleDist = distance;
                }
            }
        }
        return NumberRange.create(startLevel, endLevel);
    }

    // makes sure that the zoom level set to the TileJson are actually those
    // where at least one layer will be available
    private void setTileJSONZoomLevels(TileJSON tileJSON, GridSubset subset) {
        List<VectorLayerMetadata> layersMetadata = tileJSON.getLayers();
        int globalMinZoomLevel =
                Optional.ofNullable(subset.getMinCachedZoom()).orElse(subset.getZoomStart());
        int globalMaxZoomLevel =
                Optional.ofNullable(subset.getMaxCachedZoom()).orElse(subset.getZoomStop());
        if (layersMetadata != null && !layersMetadata.isEmpty()) {
            globalMinZoomLevel =
                    layersMetadata.stream().map(m -> m.getMinZoom()).min(Integer::compare).get();
            globalMaxZoomLevel =
                    layersMetadata.stream().map(m -> m.getMaxZoom()).max(Integer::compare).get();
        }
        tileJSON.setMinZoom(globalMinZoomLevel);
        tileJSON.setMaxZoom(globalMaxZoomLevel);
    }
}
