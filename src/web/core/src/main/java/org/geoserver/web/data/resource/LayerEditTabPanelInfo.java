/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

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
}
