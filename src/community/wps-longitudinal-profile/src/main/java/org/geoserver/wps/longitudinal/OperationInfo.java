package org.geoserver.wps.longitudinal;

public class OperationInfo {

    public double getElevationPositive() {
        return elevationPositive;
    }

    public void setElevationPositive(double elevationPositive) {
        this.elevationPositive = elevationPositive;
    }

    public double getElevationNegative() {
        return elevationNegative;
    }

    public void setElevationNegative(double elevationNegative) {
        this.elevationNegative = elevationNegative;
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

    double elevationPositive;

    double elevationNegative;

    double totalDistance;

    double firstPointX;

    double firstPointY;

    double lastPointX;

    double lastPointY;

    String layer;

    int processedPoints;

    long executedTime;
}
