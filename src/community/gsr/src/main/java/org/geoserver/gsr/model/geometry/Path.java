/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.geometry;

/** @author Juan Marin - OpenGeo */
public class Path {

    private Number[] coordinates;

    public Path(Number[] coords) {
        this.coordinates = coords;
    }

    public Number[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Number[] coordinates) {
        this.coordinates = coordinates;
    }
}
