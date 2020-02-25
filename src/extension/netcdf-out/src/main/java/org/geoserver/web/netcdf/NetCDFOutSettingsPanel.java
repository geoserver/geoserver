/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.web.data.settings.SettingsPluginPanel;
import org.geoserver.web.util.MetadataMapModel;

/** @author Nicola Lagomarsini Geosolutions S.A.S. */
public class NetCDFOutSettingsPanel extends SettingsPluginPanel {

    public NetCDFOutSettingsPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);
        // Model associated to the metadata map
        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "metadata");

        // Getting the NetcdfSettingsContainer model from MetadataMap
        IModel<NetCDFSettingsContainer> netcdfModel =
                new MetadataMapModel(
                        metadata,
                        NetCDFSettingsContainer.NETCDFOUT_KEY,
                        NetCDFSettingsContainer.class);

        // New Container
        // container for ajax updates
        NetCDFPanel panel = new NetCDFPanel("panel", netcdfModel);
        add(panel);
    }
}
