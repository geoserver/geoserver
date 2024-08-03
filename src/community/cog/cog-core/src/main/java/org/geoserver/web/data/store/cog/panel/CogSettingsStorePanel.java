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
public class CogSettingsStorePanel<T extends CogSettingsStore> extends CogSettingsPanel {

    FormComponent user;

    FormComponent password;

    public CogSettingsStorePanel(String id, IModel settingsModel, Form storeEditForm) {
        super(id, settingsModel);

        final IModel model = storeEditForm.getModel();
        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        user = addTextPanel(paramsModel, "user", "user", "CogSettings.userName", false);
        password = addPasswordPanel(paramsModel, "password", "CogSettings.password", false);

        user.setOutputMarkupId(true);
        password.setOutputMarkupId(true);
    }

    private FormComponent addTextPanel(
            final IModel paramsModel,
            final String paramName,
            final String paramTitle,
            final String resourceKey,
            final boolean required) {

        final TextParamPanel textParamPanel =
                new TextParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramTitle),
                        new ResourceModel(resourceKey, paramName),
                        required);
        textParamPanel.getFormComponent().setType(String.class);

        String defaultTitle = paramTitle;

        ResourceModel titleModel = new ResourceModel(resourceKey, defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        container.add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    private FormComponent addPasswordPanel(
            final IModel paramsModel,
            final String paramName,
            final String resourceKey,
            final boolean required) {
        final PasswordParamPanel pwdPanel =
                new PasswordParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramName),
                        new ResourceModel(resourceKey, paramName),
                        required);
        String defaultTitle = paramName;

        ResourceModel titleModel = new ResourceModel(resourceKey, defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        pwdPanel.add(AttributeModifier.replace("title", title));
        container.add(pwdPanel);
        return pwdPanel.getFormComponent();
    }
}
