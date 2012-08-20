package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 *
 */
public abstract class Geometry {

    protected GeometryType geometryType;

    public GeometryType getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryType geometryType) {
        this.geometryType = geometryType;
    }
}
