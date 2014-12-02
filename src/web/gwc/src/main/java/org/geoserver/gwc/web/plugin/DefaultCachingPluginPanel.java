/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.plugin;

import org.apache.wicket.model.IModel;
import org.geoserver.gwc.config.GWCConfig;

/**
 * Example class which extends the {@link GWCSettingsPluginPanel} class.
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class DefaultCachingPluginPanel extends GWCSettingsPluginPanel {

    public DefaultCachingPluginPanel(String id, IModel<GWCConfig> model) {
        super(id, model);
    }

    @Override
    public void doSave() {
        // Do nothing
    }

}
