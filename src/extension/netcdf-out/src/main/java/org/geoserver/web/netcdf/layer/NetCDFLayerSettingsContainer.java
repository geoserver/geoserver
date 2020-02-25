/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.netcdf.layer;

import org.geoserver.web.netcdf.NetCDFSettingsContainer;

/**
 * Extension for {@link NetCDFSettingsContainer} class for supporting Layer name and Unit of
 * Measure. This class stores the NetCDF output settings for a single layer.
 */
@SuppressWarnings("serial")
public class NetCDFLayerSettingsContainer extends NetCDFSettingsContainer {

    /** Layer name */
    private String layerName;

    /** Layer Unit of Measure */
    private String layerUOM;

    public NetCDFLayerSettingsContainer() {}

    public NetCDFLayerSettingsContainer(NetCDFSettingsContainer globalContainer) {
        setCompressionLevel(globalContainer.getCompressionLevel());
        setDataPacking(globalContainer.getDataPacking());
        setShuffle(globalContainer.isShuffle());
        setCopyAttributes(globalContainer.isCopyAttributes());
        setCopyGlobalAttributes(globalContainer.isCopyGlobalAttributes());
        setGlobalAttributes(globalContainer.getGlobalAttributes());
        setVariableAttributes(globalContainer.getVariableAttributes());
        setExtraVariables(globalContainer.getExtraVariables());
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
