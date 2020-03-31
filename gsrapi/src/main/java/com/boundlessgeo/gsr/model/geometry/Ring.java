/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import java.util.Arrays;

/**
 *
 * @author Juan Marin - OpenGeo
 *
 */
public class Ring {

    private Number[][] coordinates;

    public Number[][] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Number[][] coordinates) {
        this.coordinates = coordinates;
    }

    public Ring(Number[][] coords) {
        this.coordinates = coords;
    }

    public boolean isValid() {
        int size = coordinates.length;
        if (size > 0) {
            Number[] firstCoordinate = coordinates[0];
            Number[] lastCoordinate = coordinates[coordinates.length - 1];
            return Arrays.equals(firstCoordinate, lastCoordinate);
        }
        return false;
    }

}
