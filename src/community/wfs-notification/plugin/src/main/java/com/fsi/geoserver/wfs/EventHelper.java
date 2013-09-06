package com.fsi.geoserver.wfs;

public interface EventHelper {
    void fireNotification(String byteString);
    boolean isReady();
}