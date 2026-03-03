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

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    public GraticuleStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        final IModel paramsModel = new PropertyModel<>(model, "connectionParameters");

        final WebMarkupContainer configsContainer = new WebMarkupContainer("configsContainer");
        configsContainer.setOutputMarkupId(true);
        add(configsContainer);

        final GraticulePanel advancedConfigPanel = new GraticulePanel("gratpanel", paramsModel, storeEditForm);
        advancedConfigPanel.setOutputMarkupId(true);
        advancedConfigPanel.setVisible(true);
        configsContainer.add(advancedConfigPanel);
    }
}
