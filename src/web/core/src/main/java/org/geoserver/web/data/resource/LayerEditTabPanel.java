/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;

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

}
