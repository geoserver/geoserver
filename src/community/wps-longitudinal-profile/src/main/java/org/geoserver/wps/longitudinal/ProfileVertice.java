package org.geoserver.wps.longitudinal;

import org.geotools.geometry.Position2D;

public class ProfileVertice {
    Integer number;
    Position2D coordinate;
    Double altitude;

    public ProfileVertice() {}

    public ProfileVertice(Integer number, Position2D coordinate, Double altitude) {
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

    public Position2D getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Position2D coordinate) {
        this.coordinate = coordinate;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }
}
