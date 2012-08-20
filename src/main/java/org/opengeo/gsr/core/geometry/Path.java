package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Path {

    private double[] coordinates;

    public Path(double[] coords) {
        this.coordinates = coords;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }
}
