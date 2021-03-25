/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Maps to the JSON structure describing a layer of vector tiles */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VectorLayerMetadata {

    /** Only reports the general type, not the specific one */
    enum GeometryType {
        point,
        line,
        polygon
    };

    String id;
    String description;

    @JsonProperty("minzoom")
    Integer minZoom;

    @JsonProperty("maxzoom")
    Integer maxZoom;

    Map<String, String> fields;

    /** This is an extension to report the type of geometry, used by some tools out there */
    @JsonProperty("geometry_type")
    GeometryType geometryType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(Integer minZoom) {
        this.minZoom = minZoom;
    }

    public Integer getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(Integer maxZoom) {
        this.maxZoom = maxZoom;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public GeometryType getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryType geometryType) {
        this.geometryType = geometryType;
    }
}
