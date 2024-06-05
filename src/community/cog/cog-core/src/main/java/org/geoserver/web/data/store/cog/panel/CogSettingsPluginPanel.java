/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.cog.panel;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.cog.CogSettings;
import org.geoserver.config.SettingsInfo;
import org.geoserver.web.data.settings.SettingsPluginPanel;
import org.geoserver.web.util.MetadataMapModel;

/**
 * Pluggable panel containing {@link CogSettings}} configuration, to show up on the Global Settings
 * page
 */
public class CogSettingsPluginPanel extends SettingsPluginPanel {

    public CogSettingsPluginPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);
        // Model associated to the metadata map
        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "metadata");

        IModel<CogSettings> cogSettingsModel =
                new MetadataMapModel(metadata, CogSettings.COG_SETTINGS_KEY, CogSettings.class);

        // New Container
        // container for ajax updates
        CogSettingsPanel panel = new CogSettingsPanel("panel", cogSettingsModel);
        add(panel);
    }
}
