/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.PublishedInfo;

/**
 * A panel created to configure one aspect of a {@link PublishedInfo} object.
 *
 * <p>Typically there will be one panel dealing generically with {@link PublishedInfo} and one extra
 * panel to deal with the specifics of each service publishing the data (WMS, WCS, WFS, ...).
 *
 * <p>All the components in the panel must be contained in a {@link Form} to make sure the whole tab
 * switch and page submit workflow function properly
 *
 * @author Niels Charlier
 */
public class PublishedConfigurationPanel<T extends PublishedInfo> extends Panel {
    private static final long serialVersionUID = 4881474189619124359L;

    public PublishedConfigurationPanel(String id, IModel<? extends T> model) {
        super(id, model);
    }

    @SuppressWarnings("unchecked")
    public T getPublishedInfo() {
        return (T) getDefaultModelObject();
    }

    /**
     * Allows subclasses to override in case they need to save any other state than the {@link
     * PublishedInfo} itself
     */
    public void save() {
        // do nothing by default
    }
}
