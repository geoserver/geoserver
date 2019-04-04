/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.pgraster;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;

/**
 * Provides more components for PGRaster store automatic configuration
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public final class PGRasterCoverageStoreEditPanel extends StoreEditPanel {

    private CheckBox enabled;

    public PGRasterCoverageStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        // double container dance to get stuff to show up and hide on demand (grrr)
        final WebMarkupContainer configsContainer = new WebMarkupContainer("configsContainer");
        configsContainer.setOutputMarkupId(true);
        add(configsContainer);

        final PGRasterPanel advancedConfigPanel =
                new PGRasterPanel("pgpanel", paramsModel, storeEditForm);
        advancedConfigPanel.setOutputMarkupId(true);
        advancedConfigPanel.setVisible(false);
        configsContainer.add(advancedConfigPanel);

        // TODO: Check whether this constructor is properly setup
        final TextParamPanel url =
                new TextParamPanel(
                        "urlPanel",
                        new PropertyModel(paramsModel, "URL"),
                        new ResourceModel("url", "URL"),
                        true);
        final FormComponent urlFormComponent = url.getFormComponent();
        urlFormComponent.add(new FileExistsValidator());
        add(url);

        // enabled flag, and show the rest only if enabled is true
        IModel<Boolean> enabledModel = new Model<Boolean>(false);
        enabled = new CheckBox("enabled", enabledModel);
        add(enabled);
        enabled.add(
                new AjaxFormComponentUpdatingBehavior("click") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean visible = enabled.getModelObject();

                        advancedConfigPanel.setVisible(visible);
                        target.add(configsContainer);
                    }
                });

        /*
         * Listen to form submission and update the model's URL
         */
        storeEditForm.add(
                new IFormValidator() {
                    private static final long serialVersionUID = 1L;

                    public FormComponent[] getDependentFormComponents() {
                        if (enabled.getModelObject()) {
                            return advancedConfigPanel.getDependentFormComponents();
                        } else {
                            return new FormComponent[] {urlFormComponent};
                        }
                    }

                    public void validate(final Form form) {
                        CoverageStoreInfo storeInfo = (CoverageStoreInfo) form.getModelObject();
                        String coverageUrl = urlFormComponent.getValue();
                        if (enabled.getModelObject()) {
                            coverageUrl = advancedConfigPanel.buildURL() + coverageUrl;
                        }

                        storeInfo.setURL(coverageUrl);
                    }
                });
    }

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
