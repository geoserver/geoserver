/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.publish.LayerGroupConfigurationPanel;

/**
 * Configures {@link LayerGroupInfo} WMS specific attributes
 */
@SuppressWarnings("serial")
public class WMSLayerGroupConfig extends LayerGroupConfigurationPanel {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public WMSLayerGroupConfig(String id, IModel layerGroupModel) {
        super(id, layerGroupModel);

        // authority URLs and identifiers for this layer
        LayerAuthoritiesAndIdentifiersPanel authAndIds;
        authAndIds = new LayerAuthoritiesAndIdentifiersPanel("authoritiesAndIds", false,
                layerGroupModel);
        add(authAndIds);
    }
}
