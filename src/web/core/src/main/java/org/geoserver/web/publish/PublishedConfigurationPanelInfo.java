/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.ComponentInfo;

/**
 * Extension point for sections of the configuration pages for layers/layergroups.
 *
 * @author David Winslow <dwinslow@openplans.org>
 * @author Niels Charlier
 */
public abstract class PublishedConfigurationPanelInfo<T extends PublishedInfo>
        extends ComponentInfo<PublishedConfigurationPanel<T>> {

    private static final long serialVersionUID = 6115999990499640707L;

    public abstract Class<T> getPublishedInfoClass();

    public boolean canHandle(PublishedInfo pi) {
        return getPublishedInfoClass().isAssignableFrom(pi.getClass());
    }
}
