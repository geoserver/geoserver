/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;

/**
 * Extension point for panels which appear in separate tabs on the layer edit page.
 * <p>
 * Subclasses <b>must</b> override the {@link #LayerEditTabPanel(String, IModel)} constructor 
 * and <b>not</b> change its signature. 
 * </p>
 * <p>
 * Instances of this class are described in a spring context with a {@link LayerEditTabPanelInfo}
 * bean. 
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class LayerEditTabPanel extends Panel {

    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    public LayerEditTabPanel(String id, IModel model) {
        super(id, model);
    }
    
    /**
     * Returns the layer currently being edited by the panel.
     */
    public LayerInfo getLayer() {
        return (LayerInfo) getDefaultModel().getObject();
    }

    /**
     * Called by {@link ResourceConfigurationPage} to save the state of this tab's model.
     * <p>
     * This default implementation does nothing, as by default {@link ResourceConfigurationPage} is
     * in charge of saving its layer and group info models.
     */
    public void save() {
        // do nothing
    }
}
