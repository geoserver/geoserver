package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */
public class Ring {

    private double[][] coordinates;

    public double[][] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[][] coordinates) {
        this.coordinates = coordinates;
    }

    public Ring(double[][] coords) {
        this.coordinates = coords;
    }

    public boolean isValid() {
        int size = coordinates.length;
        if (size > 0) {
            double[] firstCoordinate = coordinates[0];
            double[] lastCoordinate = coordinates[coordinates.length - 1];
            if (firstCoordinate.equals(lastCoordinate)) {
                return true;
            }
            return false;
        }
        return false;
    }

}
