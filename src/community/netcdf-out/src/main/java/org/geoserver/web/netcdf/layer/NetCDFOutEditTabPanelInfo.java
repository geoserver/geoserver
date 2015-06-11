/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf.layer;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.resource.LayerEditTabPanelInfo;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;

/**
 * {@link LayerEditTabPanelInfo} implementation for the NetCDF output settings configuration
 * 
 */
public class NetCDFOutEditTabPanelInfo extends LayerEditTabPanelInfo {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public IModel<ResourceInfo> createOwnModel(IModel<? extends ResourceInfo> resourceModel,
            IModel<LayerInfo> layerModel, boolean isNew) {
        // Check if the input model is an instance of CoverageInfo model
        if (resourceModel.getObject() instanceof CoverageInfo) {
            // If so return the ResourceInfo object
            ResourceInfo info = resourceModel.getObject();
            // Check if the MetadataMap already contains the NetCDF Settings
            MetadataMap map = info.getMetadata();
            NetCDFLayerSettingsContainer container = map.get(NetCDFSettingsContainer.NETCDFOUT_KEY,
                    NetCDFLayerSettingsContainer.class);
            // If not present, add it to the map
            if (isNew || container == null) {
                //container = new NetCDFLayerSettingsContainer();
                // Getting NetCDF Settings from the Global ones
                GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
                MetadataMap globalMap = gs.getGlobal().getSettings().getMetadata();
                NetCDFSettingsContainer globalContainer = globalMap.get(NetCDFSettingsContainer.NETCDFOUT_KEY,
                    NetCDFSettingsContainer.class);
                // If not present, create a new container
                if(globalContainer == null){
                    globalContainer = new NetCDFSettingsContainer();
                }
                container = new NetCDFLayerSettingsContainer(globalContainer);
                map.put(NetCDFSettingsContainer.NETCDFOUT_KEY, container);
            }
            return (IModel<ResourceInfo>) resourceModel;
        }
        // Otherwise return an empty model
        return new Model<ResourceInfo>();
    }
}
