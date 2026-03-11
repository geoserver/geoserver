/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.io.Serial;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.ComponentInfo;

/**
 * Extension point for sections of the configuration pages for layers/layergroups.
 *
 * @author David Winslow dwinslow@openplans.org
 * @author Niels Charlier
 */
public abstract class PublishedConfigurationPanelInfo<T extends PublishedInfo>
        extends ComponentInfo<PublishedConfigurationPanel<T>> {

    @Serial
    private static final long serialVersionUID = 6115999990499640707L;

    /** order of the panel with respect to other panels. */
    int order = -1;

    /** Returns the order of the panel. */
    public int getOrder() {
        return order;
    }

    /** Sets the order of the panel. */
    public void setOrder(int order) {
        this.order = order;
    }

    public abstract Class<T> getPublishedInfoClass();

    public boolean canHandle(PublishedInfo pi) {
        return getPublishedInfoClass().isAssignableFrom(pi.getClass());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append("id='").append(getId()).append('\'');
        sb.append(", componentClass=").append(getComponentClass().getSimpleName());
        sb.append(", order=").append(order);
        sb.append(", title='").append(getTitleKey()).append('\'');
        sb.append(", description='").append(getDescriptionKey()).append('\'');
        sb.append(", authorizer=").append(getAuthorizer());
        sb.append('}');
        return sb.toString();
    }
}
