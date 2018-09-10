package org.geoserver.test;

public class AliasStationsMockData extends StationsMockData {

    @Override
    public void addContent() {
        setLayerNamePrefix("lyr");
        super.addContent();
    }
}
