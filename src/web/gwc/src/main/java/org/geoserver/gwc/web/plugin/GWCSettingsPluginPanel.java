/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.plugin;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.web.GWCSettingsPage;

/**
 * Abstract class which must be extended by the subclasses for creating a new {@link Panel} object.
 * This class will be used as an extension point for the {@link GWCSettingsPage}.
 * 
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 *
 */
public abstract class GWCSettingsPluginPanel extends Panel{

    public GWCSettingsPluginPanel(String id, IModel<GWCConfig> model) {
        super(id, model);
    }
    
    public abstract void doSave();
}