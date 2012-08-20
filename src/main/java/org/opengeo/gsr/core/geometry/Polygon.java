package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Polygon extends Geometry {

    private double[][] rings;

    private SpatialReference spatialReference;

    public double[][] getRings() {
        return rings;
    }

    public void setRings(double[][] ring) {
        this.rings = ring;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polygon(double[][] rings, SpatialReference spatialRef) {
        this.rings = rings;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryType.POLYGON;
    }
}
