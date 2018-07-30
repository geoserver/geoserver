package com.boundlessgeo.gsr.api.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boundlessgeo.gsr.model.geometry.*;
import com.boundlessgeo.gsr.translate.geometry.SpatialReferences;
import com.boundlessgeo.gsr.translate.geometry.GeometryEncoder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import com.boundlessgeo.gsr.model.map.LayerOrTable;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;

/**
 * Holder for identify results
 */
class IdentifyServiceResult {

    private List<IdentifyResult> results = new ArrayList<>();

    public List<IdentifyResult> getResults() {
        return results;
    }

    public static List<IdentifyResult> encode(FeatureCollection collection, LayerOrTable layer) {
        final List<IdentifyResult> results = new ArrayList<>();
        FeatureIterator iterator = collection.features();
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            //Get the default geometry from the FeatureType for consistency (see GEOS-8852)
            GeometryDescriptor defaultGeometry;
            FeatureTypeInfo featureType = (FeatureTypeInfo) layer.layer.getResource();
            try {
                defaultGeometry = featureType.getFeatureType().getGeometryDescriptor();
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to determine geometry type for identify request");
            }

            SpatialReference spatialReference;
            try {
                spatialReference = SpatialReferences.fromCRS(defaultGeometry.getCoordinateReferenceSystem());
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
            IdentifyResult result = new IdentifyResult();
            result.setLayerName(layer.getName());
            result.setLayerId(layer.getId());
            result.setGeometry(new GeometryEncoder().toRepresentation(
                (com.vividsolutions.jts.geom.Geometry) feature.getProperty(defaultGeometry.getName()).getValue(), spatialReference));
            result.setAttributes(FeatureEncoder.attributeList(feature, FeatureEncoder.OBJECTID_FIELD_NAME));
            result.setGeometryType(result.getGeometry().getGeometryType());
            result.getGeometry().setSpatialReference(spatialReference);
            result.setValue(feature.getIdentifier().toString());
            result.getAttributes().put("synthetic_id", feature.getIdentifier().toString());
            result.setDisplayFieldName("synthetic_id");
            results.add(result);
        }

        return results;
    }

    public static class IdentifyResult {
        private Integer layerId;

        private String layerName;

        private String value;

        private String displayFieldName;

        private Map<String, Object> attributes;

        private GeometryTypeEnum geometryType;

        private Geometry geometry;

        private boolean hasZ = false;

        private boolean hasM = false;

        public Integer getLayerId() {
            return layerId;
        }

        public void setLayerId(Integer layerId) {
            this.layerId = layerId;
        }

        public String getLayerName() {
            return layerName;
        }

        public void setLayerName(String layerName) {
            this.layerName = layerName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDisplayFieldName() {
            return displayFieldName;
        }

        public void setDisplayFieldName(String displayFieldName) {
            this.displayFieldName = displayFieldName;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public GeometryTypeEnum getGeometryType() {
            return geometryType;
        }

        public void setGeometryType(GeometryTypeEnum geometryType) {
            this.geometryType = geometryType;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }

        public boolean isHasZ() {
            return hasZ;
        }

        public void setHasZ(boolean hasZ) {
            this.hasZ = hasZ;
        }

        public boolean isHasM() {
            return hasM;
        }

        public void setHasM(boolean hasM) {
            this.hasM = hasM;
        }
    }
}
