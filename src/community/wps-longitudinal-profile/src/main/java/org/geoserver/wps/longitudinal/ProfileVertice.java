/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import org.locationtech.jts.geom.Coordinate;

public class ProfileVertice {
    Integer number;
    Coordinate coordinate;
    Double altitude;

    public ProfileVertice() {}

    public ProfileVertice(Integer number, Coordinate coordinate, Double altitude) {
        super();
        this.number = number;
        this.coordinate = coordinate;
        this.altitude = altitude;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }
}
