/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.gsr.model.geometry.*;
import org.geoserver.gsr.model.map.LayerOrTable;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.gsr.translate.geometry.GeometryEncoder;
import org.geoserver.gsr.translate.geometry.SpatialReferences;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

/** Holder for identify results */
class IdentifyServiceResult {

    private List<IdentifyResult> results = new ArrayList<>();

    public List<IdentifyResult> getResults() {
        return results;
    }

    public static List<IdentifyResult> encode(FeatureCollection collection, LayerOrTable layer) {
        final List<IdentifyResult> results = new ArrayList<>();
        try (FeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                // Get the default geometry from the FeatureType for consistency (see GEOS-8852)
                GeometryDescriptor defaultGeometry;
                FeatureTypeInfo featureType = (FeatureTypeInfo) layer.layer.getResource();
                try {
                    defaultGeometry = featureType.getFeatureType().getGeometryDescriptor();
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Unable to determine geometry type for identify request");
                }

                SpatialReference spatialReference;
                try {
                    spatialReference =
                            SpatialReferences.fromCRS(
                                    defaultGeometry.getCoordinateReferenceSystem());
                } catch (FactoryException e) {
                    throw new RuntimeException(e);
                }
                IdentifyResult result = new IdentifyResult();
                result.setLayerName(layer.getName());
                result.setLayerId(layer.getId());
                result.setGeometry(
                        new GeometryEncoder()
                                .toRepresentation(
                                        (org.locationtech.jts.geom.Geometry)
                                                feature.getProperty(defaultGeometry.getName())
                                                        .getValue(),
                                        spatialReference));
                result.setAttributes(
                        FeatureEncoder.attributeList(feature, FeatureEncoder.OBJECTID_FIELD_NAME));
                result.setGeometryType(result.getGeometry().getGeometryType());
                result.getGeometry().setSpatialReference(spatialReference);
                result.setValue(feature.getIdentifier().toString());
                result.getAttributes().put("synthetic_id", feature.getIdentifier().toString());
                result.setDisplayFieldName("synthetic_id");
                results.add(result);
            }
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
