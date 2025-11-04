/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.libdeflate.LibdeflateSettings;
import org.geoserver.web.data.settings.SettingsPluginPanel;
import org.geoserver.web.util.MetadataMapModel;

/** Pluggable panel containing {@link LibdeflateSettings}} configuration, to show up on the Global Settings page */
public class LibdeflateSettingsPluginPanel extends SettingsPluginPanel {

    public LibdeflateSettingsPluginPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);
        // Model associated to the metadata map
        final PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "metadata");

        IModel<LibdeflateSettings> settingsModel =
                new MetadataMapModel<>(metadata, LibdeflateSettings.LIBDEFLATE_SETTINGS_KEY, LibdeflateSettings.class);

        // New Container
        // container for ajax updates
        LibdeflateSettingsPanel<LibdeflateSettings> panel = new LibdeflateSettingsPanel<>("panel", settingsModel);
        add(panel);
    }
}
