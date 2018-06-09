/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.geoserver.catalog.LayerGroupInfo;

/**
 * Extension point for sections of the configuration pages for layergroups.
 *
 * @author Niels Charlier
 */
public class LayerGroupConfigurationPanelInfo
        extends PublishedConfigurationPanelInfo<LayerGroupInfo> {

    private static final long serialVersionUID = 8382295309912226673L;

    @Override
    public Class<LayerGroupInfo> getPublishedInfoClass() {
        return LayerGroupInfo.class;
    }
}
