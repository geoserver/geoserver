package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Polyline extends Geometry {

    private double[][] paths;

    private SpatialReference spatialReference;

    public double[][] getPaths() {
        return paths;
    }

    public void setPaths(double[][] paths) {
        this.paths = paths;
    }

    public SpatialReference getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(SpatialReference spatialReference) {
        this.spatialReference = spatialReference;
    }

    public Polyline(double[][] paths, SpatialReference spatialRef) {
        this.paths = paths;
        this.spatialReference = spatialRef;
        this.geometryType = GeometryType.POLYLINE;
    }
}
