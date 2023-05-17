/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

public class OperationInfo {

    double altitudePositive;

    double altitudeNegative;

    double totalDistance;

    double firstPointX;

    double firstPointY;

    double lastPointX;

    double lastPointY;

    String layer;

    int processedPoints;

    long executedTime;

    public double getAltitudePositive() {
        return altitudePositive;
    }

    public void setAltitudePositive(double altitudePositive) {
        this.altitudePositive = altitudePositive;
    }

    public double getAltitudeNegative() {
        return altitudeNegative;
    }

    public void setAltitudeNegative(double altitudeNegative) {
        this.altitudeNegative = altitudeNegative;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public double getFirstPointX() {
        return firstPointX;
    }

    public void setFirstPointX(double firstPointX) {
        this.firstPointX = firstPointX;
    }

    public double getFirstPointY() {
        return firstPointY;
    }

    public void setFirstPointY(double firstPointY) {
        this.firstPointY = firstPointY;
    }

    public double getLastPointX() {
        return lastPointX;
    }

    public void setLastPointX(double lastPointX) {
        this.lastPointX = lastPointX;
    }

    public double getLastPointY() {
        return lastPointY;
    }

    public void setLastPointY(double lastPointY) {
        this.lastPointY = lastPointY;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public int getProcessedPoints() {
        return processedPoints;
    }

    public void setProcessedPoints(int processedPoints) {
        this.processedPoints = processedPoints;
    }

    public long getExecutedTime() {
        return executedTime;
    }

    public void setExecutedTime(long executedTime) {
        this.executedTime = executedTime;
    }
}
