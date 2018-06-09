/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.settings;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.SettingsInfo;

/**
 * Abstract class which must be extended by the subclasses for creating a new {@link Panel} object.
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public abstract class SettingsPluginPanel extends Panel {
    private static final long serialVersionUID = 2747074530701938992L;

    public SettingsPluginPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);
    }
}
