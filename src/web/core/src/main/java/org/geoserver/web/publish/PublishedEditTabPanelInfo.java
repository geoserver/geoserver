/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.ComponentInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;

/**
 * Information about panels plugged into additional tabs on layer/layergroup edit page.
 *
 * <p>Layer edit tabs have a self declared order which describes where they end up on the layer edit
 * page. Lower order panels are weighted toward the left hand side, higher order panels are weighted
 * toward the right hand side.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Niels Charlier
 */
public abstract class PublishedEditTabPanelInfo<T extends PublishedInfo>
        extends ComponentInfo<PublishedEditTabPanel<T>> {

    private static final long serialVersionUID = 4849692244366766812L;

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

    /**
     * It may be that a tab contribution to the {@link PublishedConfigurationPage} need to work on a
     * different model object that the page's layer and resource models (for example, because it
     * edits and saves related information not directly attached to the layer/resource); if such is
     * the case, this method shall return the model to be passed to the {@link
     * PublishedEditTabPanel} constructor.
     *
     * <p>This default implementation just returns {@code null} and assumes the {@link
     * PublishedEditTabPanel} described by this tab panel info works against the {@link
     * ResourceConfigurationPage} LayerInfo model. Subclasses may override as appropriate.
     *
     * @return {@code null} if no need for a custom model for the tab, the model to use otherwise
     * @see PublishedEditTabPanel#save()
     */
    public IModel<?> createOwnModel(IModel<? extends T> model, boolean isNew) {
        return null;
    }

    public abstract Class<T> getPublishedInfoClass();

    public boolean supports(PublishedInfo pi) {
        return getPublishedInfoClass().isAssignableFrom(pi.getClass());
    }
}
