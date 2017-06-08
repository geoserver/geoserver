/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.geometry;

import java.util.Arrays;

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
            return Arrays.equals(firstCoordinate, lastCoordinate);
        }
        return false;
    }

}
