/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.geoserver.catalog.PublishedInfo;

/**
 * Extension point for sections of the configuration pages that work for both layers and
 * layergroups.
 *
 * @author Niels Charlier
 */
public class CommonPublishedConfigurationPanelInfo
        extends PublishedConfigurationPanelInfo<PublishedInfo> {

    private static final long serialVersionUID = 8382295309912226673L;

    @Override
    public Class<PublishedInfo> getPublishedInfoClass() {
        return PublishedInfo.class;
    }
}
