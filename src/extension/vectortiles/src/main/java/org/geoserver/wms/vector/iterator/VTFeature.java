/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

import java.util.Map;
import org.locationtech.jts.geom.Geometry;

/** Represents the essential components of a vector tile feature */
public class VTFeature {
    String featureId;
    Map<String, Object> properties;
    Geometry geometry;

    public VTFeature(String featureId, Geometry geometry, Map<String, Object> properties) {
        this.featureId = featureId;
        this.properties = properties;
        this.geometry = geometry;
    }

    /** Returns the feature identifier */
    public String getFeatureId() {
        return featureId;
    }

    /** Sets the feature identifier */
    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    /** Returns the feature properties */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /** Sets the feature properties */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /** Returns the feature geometry */
    public Geometry getGeometry() {
        return geometry;
    }

    /** Sets the feature geometry */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    @Override
    public String toString() {
        return "VTFeature{"
                + "featureId='"
                + featureId
                + '\''
                + ", properties="
                + properties
                + ", geometry="
                + geometry
                + '}';
    }
}
