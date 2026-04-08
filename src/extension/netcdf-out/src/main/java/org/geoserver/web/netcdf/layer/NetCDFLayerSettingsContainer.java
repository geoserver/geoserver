/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.netcdf.layer;

import org.geoserver.web.netcdf.NetCDFSettingsContainer;

/**
 * Extension for {@link NetCDFSettingsContainer} class for supporting Layer name and Unit of Measure. This class stores
 * the NetCDF output settings for a single layer.
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

    /**
     * Returns a shallow copy of the NetCDFLayerSettingsContainer.
     *
     * <p>The returned copy is a new instance with the same values as this one. However, any mutable fields such as
     * lists or maps are not deeply copied; instead, references to the original collections are reused.
     *
     * <p>Modifying a list or other mutable field in the copied object will also affect the original, and vice versa.
     *
     * @return a shallow copy of the settings
     */
    public NetCDFLayerSettingsContainer copy() {
        NetCDFLayerSettingsContainer copy = new NetCDFLayerSettingsContainer();
        copy.setCompressionLevel(this.getCompressionLevel());
        copy.setDataPacking(this.getDataPacking());
        copy.setShuffle(this.isShuffle());
        copy.setCopyAttributes(this.isCopyAttributes());
        copy.setCopyGlobalAttributes(this.isCopyGlobalAttributes());
        copy.setGlobalAttributes(this.getGlobalAttributes());
        copy.setVariableAttributes(this.getVariableAttributes());
        copy.setExtraVariables(this.getExtraVariables());
        return copy;
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
