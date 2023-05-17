package org.geoserver.wps.longitudinal;

public class ProfileInfo {

    public ProfileInfo(double totalDistance, double x, double y, double altitude, double slope) {
        this.totalDistanceToThisPoint = totalDistance;
        X = x;
        Y = y;
        this.altitude = altitude;
        this.slope = slope;
    }

    double totalDistanceToThisPoint;
    double X;
    double Y;
    double altitude;
    double slope;

    public double getTotalDistanceToThisPoint() {
        return totalDistanceToThisPoint;
    }

    public double getX() {
        return X;
    }

    public double getY() {
        return Y;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getSlope() {
        return slope;
    }
}
