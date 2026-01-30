/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

public class ProfileInfo {

    double totalDistanceToThisPoint;
    double x;
    double y;
    double altitude;
    double slope;

    public ProfileInfo(double totalDistance, double x, double y, double altitude, double slope) {
        this.totalDistanceToThisPoint = totalDistance;
        this.x = x;
        this.y = y;
        this.altitude = altitude;
        this.slope = slope;
    }

    public double getTotalDistanceToThisPoint() {
        return totalDistanceToThisPoint;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getSlope() {
        return slope;
    }
}
