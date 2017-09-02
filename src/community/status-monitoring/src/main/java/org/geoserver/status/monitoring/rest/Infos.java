package org.geoserver.status.monitoring.rest;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.status.monitoring.collector.SystemInfoProperty;

public class Infos {

    private List<SystemInfoProperty> data = new ArrayList<>();

    public void addData(SystemInfoProperty info) {
        this.data.add(info);
    }

    public List<SystemInfoProperty> getData() {
        return this.data;
    }

}
