/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.wicket.FileExistsValidator;

/**
 * Provides the URL form component to edit a {@link CoverageStoreInfo}
 *
 * @author Gabriel Roldan
 * @see AbstractCoverageStorePage
 */
public class DefaultCoverageStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 1L;

    public DefaultCoverageStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel formModel = storeEditForm.getModel();
        // url
        TextParamPanel url =
                new TextParamPanel(
                        "urlPanel",
                        new PropertyModel(formModel, "URL"),
                        new ResourceModel("url", "URL"),
                        true);
        url.getFormComponent().add(new FileExistsValidator());
        add(url);
    }
}
