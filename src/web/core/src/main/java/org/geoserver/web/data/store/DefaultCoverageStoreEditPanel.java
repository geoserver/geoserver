/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.Serial;
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

    private static final boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(
            java.lang.invoke.MethodHandles.lookup().lookupClass());

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

    @Serial
    private static final long serialVersionUID = 1L;

    public DefaultCoverageStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel formModel = storeEditForm.getModel();
        // url
        TextParamPanel<String> url = new TextParamPanel<>(
                "urlPanel", new PropertyModel<>(formModel, "URL"), new ResourceModel("url", "URL"), true);
        url.getFormComponent().add(new FileExistsValidator());
        add(url);
    }
}
