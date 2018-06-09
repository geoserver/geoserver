/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.geoserver.catalog.LayerGroupInfo;

/**
 * Information about panels plugged into additional tabs on layergroup edit page.
 *
 * @author Niels Charlier
 */
public class LayerGroupEditTabPanelInfo extends PublishedEditTabPanelInfo<LayerGroupInfo> {

    private static final long serialVersionUID = -388475157541960108L;

    @Override
    public Class<LayerGroupInfo> getPublishedInfoClass() {
        return LayerGroupInfo.class;
    }
}
