/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.ComponentInfo;

/**
 * Information about panels plugged into additional tabs on layer edit page.
 * <p>
 * Layer edit tabs have a self declared order which describes where they end up on the layer edit 
 * page. Lower order panels are weighted toward the left hand side, higher order panels are weighted
 * toward the right hand side. 
 * </p>
 *
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LayerEditTabPanelInfo extends ComponentInfo<LayerEditTabPanel> {

    private static final long serialVersionUID = 4849692244366766812L;
    
    /**
     * order of the panel with respect to other panels.
     */
    int order = -1;

    /**
     * Returns the order of the panel.
     */
    public int getOrder() {
        return order;
    }

    /**
     * Sets the order of the panel.
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /**
     * It may be that a tab contribution to the {@link ResourceConfigurationPage} need to work on a
     * different model object that the page's layer and resource models (for example, because it
     * edits and saves related information not directly attached to the layer/resource); if such is
     * the case, this method shall return the model to be passed to the {@link LayerEditTabPanel}
     * constructor.
     * <p>
     * This default implementation just returns {@code null} and assumes the
     * {@link LayerEditTabPanel} described by this tab panel info works against the
     * {@link ResourceConfigurationPage} LayerInfo model. Subclasses may override as appropriate.
     * 
     * @param resourceModel
     * @param layerModel
     * @param isNew
     * @return {@code null} if no need for a custom model for the tab, the model to use otherwise
     * @see LayerEditTabPanel#save()
     */
    public IModel<?> createOwnModel(IModel<? extends ResourceInfo> resourceModel,
            IModel<LayerInfo> layerModel, boolean isNew) {
        return null;
    }
}
