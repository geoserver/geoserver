/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.geoserver.catalog.LayerInfo;

/**
 * Information about panels plugged into additional tabs on layer edit page.
 *
 * @author Niels Charlier
 */
public class LayerEditTabPanelInfo extends PublishedEditTabPanelInfo<LayerInfo> {

    private static final long serialVersionUID = -388475157541960108L;

    @Override
    public Class<LayerInfo> getPublishedInfoClass() {
        return LayerInfo.class;
    }
}
