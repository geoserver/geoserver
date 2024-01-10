/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.graticule;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.data.store.StoreEditPanel;

public final class GraticuleStoreEditPanel extends StoreEditPanel {

    public GraticuleStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        final WebMarkupContainer configsContainer = new WebMarkupContainer("configsContainer");
        configsContainer.setOutputMarkupId(true);
        add(configsContainer);

        final GraticulePanel advancedConfigPanel =
                new GraticulePanel("gratpanel", paramsModel, storeEditForm);
        advancedConfigPanel.setOutputMarkupId(true);
        advancedConfigPanel.setVisible(true);
        configsContainer.add(advancedConfigPanel);
    }
}
