/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf.layer;

import org.geoserver.web.netcdf.NetCDFSettingsContainer;

/**
 * Extension for {@link NetCDFSettingsContainer} class for supporting Layer name and Unit of Measure
 * 
 */
public class NetCDFLayerSettingsContainer extends NetCDFSettingsContainer {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** Layer name*/
    private String layerName;

    /** Layer Unit of Measure*/
    private String layerUOM;

    public NetCDFLayerSettingsContainer(){}
    
    public NetCDFLayerSettingsContainer(NetCDFSettingsContainer globalContainer) {
        setCompressionLevel(globalContainer.getCompressionLevel());
        setDataPacking(globalContainer.getDataPacking());
        setGlobalAttributes(globalContainer.getGlobalAttributes());
        setShuffle(globalContainer.isShuffle());
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getLayerUOM() {
        return layerUOM;
    }

    public void setLayerUOM(String layerUOM) {
        this.layerUOM = layerUOM;
    }
}
