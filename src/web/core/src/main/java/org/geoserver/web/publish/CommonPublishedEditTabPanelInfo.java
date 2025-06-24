/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.io.Serial;
import org.geoserver.catalog.PublishedInfo;

/**
 * Information about panels plugged into additional tabs on both layer and layergroup edit page.
 *
 * @author Niels Charlier
 */
public class CommonPublishedEditTabPanelInfo extends PublishedEditTabPanelInfo<PublishedInfo> {

    @Serial
    private static final long serialVersionUID = -388475157541960108L;

    @Override
    public Class<PublishedInfo> getPublishedInfoClass() {
        return PublishedInfo.class;
    }
}
