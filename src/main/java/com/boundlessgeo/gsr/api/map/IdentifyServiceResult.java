package com.boundlessgeo.gsr.api.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boundlessgeo.gsr.core.geometry.Geometry;
import com.boundlessgeo.gsr.core.geometry.GeometryTypeEnum;

/**
 * Holder for identify results
 */
public class IdentifyServiceResult {

    public List<IdentifyResult> results = new ArrayList<>();

    public List<IdentifyResult> getResults() {
        return results;
    }

    public void setResults(List<IdentifyResult> results) {
        this.results = results;
    }

    public static class IdentifyResult {
        private String layerId;

        private String layerName;

        private String value;

        private String displayFieldName;

        private Map<String, Object> attributes;

        private GeometryTypeEnum geometryType;

        private Geometry geometry;

        private boolean hasZ = false;

        private boolean hasM = false;

        public String getLayerId() {
            return layerId;
        }

        public void setLayerId(String layerId) {
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
