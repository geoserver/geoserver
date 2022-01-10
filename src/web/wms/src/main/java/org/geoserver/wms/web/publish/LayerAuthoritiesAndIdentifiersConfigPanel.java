/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;

/** Configures {@link LayerGroupInfo} WMS specific attributes */
public class LayerAuthoritiesAndIdentifiersConfigPanel
        extends PublishedConfigurationPanel<PublishedInfo> {

    private static final long serialVersionUID = 8652096571563162644L;

    public LayerAuthoritiesAndIdentifiersConfigPanel(
            String id, IModel<? extends PublishedInfo> layerGroupModel) {
        super(id, layerGroupModel);

        // authority URLs and identifiers for this layer
        LayerAuthoritiesAndIdentifiersPanel authAndIds;
        authAndIds =
                new LayerAuthoritiesAndIdentifiersPanel(
                        "authoritiesAndIds", false, layerGroupModel);
        add(authAndIds);
    }
}
