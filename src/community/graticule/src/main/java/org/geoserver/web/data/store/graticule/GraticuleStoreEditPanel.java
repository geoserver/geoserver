/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.graticule;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.geometry.jts.ReferencedEnvelope;

public final class GraticuleStoreEditPanel extends StoreEditPanel {

    private ReferencedEnvelope bounds;

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

        IModel<Boolean> enabledModel = new Model<Boolean>(false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private FormComponent addTextPanel(final IModel paramsModel, final String paramName) {

        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final boolean required = true;

        final TextParamPanel textParamPanel =
                new TextParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramName),
                        new ResourceModel(resourceKey, paramName),
                        required);
        textParamPanel.getFormComponent().setType(String.class);

        String defaultTitle = paramName;

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }
}
