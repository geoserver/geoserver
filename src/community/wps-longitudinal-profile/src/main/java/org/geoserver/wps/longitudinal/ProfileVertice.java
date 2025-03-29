/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import org.locationtech.jts.geom.Coordinate;

public class ProfileVertice {
    public static final int UNSET = -1;
    int number;
    Coordinate coordinate;
    double altitude;
    double distancePrevious = UNSET;
    double slope = 0;

    public ProfileVertice() {}

    public ProfileVertice(int number, Coordinate coordinate, double altitude) {
        super();
        this.number = number;
        this.coordinate = coordinate;
        this.altitude = altitude;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getDistancePrevious() {
        return distancePrevious;
    }

    public void setDistancePrevious(double distancePrevious) {
        this.distancePrevious = distancePrevious;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }
}
