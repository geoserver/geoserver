/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.io.IOException;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;

/**
 * Extension point for panels which appear in separate tabs on the layer edit page.
 *
 * <p>Subclasses <b>must</b> override the {@link #LayerEditTabPanel(String, IModel)} constructor and
 * <b>not</b> change its signature.
 *
 * <p>Instances of this class are described in a spring context with a {@link
 * PublishedEditTabPanelInfo} bean.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Niels Charlier
 */
public class PublishedEditTabPanel<T extends PublishedInfo> extends Panel {

    private static final long serialVersionUID = 8044055895040826418L;

    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    public PublishedEditTabPanel(String id, IModel<? extends T> model) {
        super(id, model);
    }

    /** Returns the layer currently being edited by the panel. */
    @SuppressWarnings("unchecked")
    public T getPublishedInfo() {
        return ((IModel<? extends T>) getDefaultModel()).getObject();
    }

    /**
     * Called by {@link ResourceConfigurationPage} to save the state of this tab's model.
     *
     * <p>
     */
    public void save() throws IOException {
        // do nothing by default
    }

    public PublishedEditTabPanel<T> setInputEnabled(final boolean enabled) {
        visitChildren(
                (component, visit) -> {
                    component.setEnabled(enabled);
                });
        return this;
    }
}
