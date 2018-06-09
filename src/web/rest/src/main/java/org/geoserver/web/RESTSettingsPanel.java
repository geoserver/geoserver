/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.web.data.settings.SettingsPluginPanel;
import org.geoserver.web.util.MetadataMapModel;

/**
 * Simple Panel which adds a TextField for setting the Root Directory for the WorkSpace or Global
 * Settings.
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public class RESTSettingsPanel extends SettingsPluginPanel {

    public RESTSettingsPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);
        // Model associated to the metadata map
        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "metadata");

        // TextField associated to the root directory to map
        TextField rootField =
                new TextField(
                        "rootdir",
                        new MetadataMapModel(metadata, RESTUtils.ROOT_KEY, String.class));
        add(rootField);

        // CheckBox associated to the root directory to map
        CheckBox quietCheckBox =
                new CheckBox(
                        "quiet",
                        new MetadataMapModel(
                                metadata, RESTUtils.QUIET_ON_NOT_FOUND_KEY, Boolean.class));
        add(quietCheckBox);
    }
}
