/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.cog.panel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.cog.CogSettingsStore;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;

/** Store specific CogSettings panel, containing therefore eventual authentication info */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CogSettingsStorePanel<T extends CogSettingsStore> extends CogSettingsPanel {

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

    FormComponent user;

    FormComponent password;

    public CogSettingsStorePanel(String id, IModel settingsModel, Form storeEditForm) {
        super(id, settingsModel);

        final IModel model = storeEditForm.getModel();
        final IModel paramsModel = new PropertyModel<>(model, "connectionParameters");

        user = addTextPanel(paramsModel);
        password = addPasswordPanel(paramsModel);

        user.setOutputMarkupId(true);
        password.setOutputMarkupId(true);
    }

    private FormComponent addTextPanel(final IModel paramsModel) {

        final TextParamPanel textParamPanel = new TextParamPanel<>(
                "user", new MapModel<>(paramsModel, "user"), new ResourceModel("CogSettings.userName", "user"), false);
        textParamPanel.getFormComponent().setType(String.class);

        ResourceModel titleModel = new ResourceModel("CogSettings.userName", "user");
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        container.add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    private FormComponent addPasswordPanel(final IModel paramsModel) {
        final PasswordParamPanel pwdPanel = new PasswordParamPanel(
                "password",
                new MapModel(paramsModel, "password"),
                new ResourceModel("CogSettings.password", "password"),
                false);
        String defaultTitle = "password";

        ResourceModel titleModel = new ResourceModel("CogSettings.password", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        pwdPanel.add(AttributeModifier.replace("title", title));
        container.add(pwdPanel);
        return pwdPanel.getFormComponent();
    }
}
