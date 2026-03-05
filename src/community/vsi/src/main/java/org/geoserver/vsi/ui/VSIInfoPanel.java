/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vsi.ui;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

/**
 * Basic data store panel for VSI sources.
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSIInfoPanel extends StoreEditPanel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(VSIInfoPanel.class);

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

    public VSIInfoPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);

        IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        add(new TextParamPanel<>(
                "location", new PropertyModel<>(model, "url"), new ResourceModel("location", "Location"), true));
    }
}
