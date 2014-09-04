/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;

/**
 * Base class for panels created to configure one aspect of a {@link LayerGroupInfo} object.
 * 
 * @see LayerConfigurationPanel
 */
public abstract class LayerGroupConfigurationPanel extends Panel {
    private static final long serialVersionUID = 4881474189619124359L;

    /**
     * Subclasses MUST have a constructor with the same arguments than this one for the lookup
     * mechanism to correctly instantiate the concrete subclass pointed out by the
     * {@link LayerGroupConfigurationPanelInfo} in the Spring context.
     */
    public LayerGroupConfigurationPanel(final String id, final IModel<LayerGroupInfo> model) {
        super(id, model);
    }

    public LayerGroupInfo getLayerGroupInfo() {
        return (LayerGroupInfo) getDefaultModelObject();
    }

    /**
     * Allows subclasses to override in case they need to save any other state than the
     * {@link LayerGroupInfo} itself
     */
    public void save() {
        // do nothing by default
    }
}
