/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.DisabledServiceResourceFilter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;

/**
 * Configuration Panel for Service enable/disable in a layer basis
 *
 * @author Fernando Miño - Geosolutions
 */
public class ServiceLayerConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {
    private static final long serialVersionUID = 1L;

    protected GeoServerDialog dialog;

    private WebMarkupContainer serviceSelectionContainer;
    private Palette<String> servicesMultiSelector;
    private Label defaultDisabledServicesLabel;
    private TextField<String> defaultDisabledServices;

    public ServiceLayerConfigurationPanel(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        final String defaultDisabledServiceList =
                GeoServerExtensions.getProperty(DisabledServiceResourceFilter.PROPERTY);
        IModel<Boolean> serviceConfigurationModel = new PropertyModel<>(layerModel, "resource.serviceConfiguration");
        final AjaxCheckBox configEnabledCheck = new AjaxCheckBox("configEnabled", serviceConfigurationModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ServiceLayerConfigurationPanel.this.servicesMultiSelector.setVisible(getModelObject());

                boolean showDefaults =
                        StringUtils.isNotBlank(defaultDisabledServiceList) && getModelObject() != Boolean.TRUE;
                ServiceLayerConfigurationPanel.this.defaultDisabledServices.setVisible(showDefaults);
                ServiceLayerConfigurationPanel.this.defaultDisabledServicesLabel.setVisible(showDefaults);

                target.add(ServiceLayerConfigurationPanel.this);
            }
        };
        add(configEnabledCheck);
        PropertyModel<List<String>> dsModel = new PropertyModel<>(layerModel, "resource.disabledServices");
        final IChoiceRenderer<String> renderer = new ChoiceRenderer<>() {
            @Override
            public String getObject(String id, IModel<? extends List<? extends String>> choices) {
                return id;
            }

            @Override
            public Object getDisplayValue(String object) {
                if (object == null) return null;
                return super.getDisplayValue(object);
            }

            @Override
            public String getIdValue(String object, int index) {
                return object;
            }
        };
        servicesMultiSelector =
                new Palette<>(
                        "servicesSelection",
                        dsModel,
                        servicesVotedModel(layerModel.getObject().getResource()),
                        renderer,
                        10,
                        false) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("DisabledServicesPalette.selectedHeader"));
                    }

                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("DisabledServicesPalette.availableHeader"));
                    }
                };
        servicesMultiSelector.add(new DefaultTheme());
        servicesMultiSelector.setVisible(layerModel.getObject().getResource().isServiceConfiguration());
        serviceSelectionContainer = new WebMarkupContainer("serviceSelectionContainer");
        serviceSelectionContainer.setOutputMarkupPlaceholderTag(true);
        add(serviceSelectionContainer);
        serviceSelectionContainer.add(servicesMultiSelector);

        boolean showDefaults = StringUtils.isNotBlank(defaultDisabledServiceList)
                && serviceConfigurationModel.getObject() != Boolean.TRUE;
        defaultDisabledServicesLabel = new Label(
                "defaultDisabledServicesLabel",
                new ResourceModel("DisabledServicesPalette.defaultDisabledServicesLabel"));
        defaultDisabledServicesLabel.setOutputMarkupId(true);
        defaultDisabledServicesLabel.setVisible(showDefaults);
        add(defaultDisabledServicesLabel);

        defaultDisabledServices = new TextField<>(
                "defaultDisabledServices",
                new Model<>(StringUtils.isBlank(defaultDisabledServiceList) ? "" : defaultDisabledServiceList));
        defaultDisabledServices.setEnabled(false);
        defaultDisabledServices.setOutputMarkupId(true);
        defaultDisabledServices.setVisible(showDefaults);
        add(defaultDisabledServices);

        dialog = new GeoServerDialog("serviceDialog");
        add(dialog);

        add(new AjaxLink<String>("layerSettingsHelp") {
            private static final long serialVersionUID = 9222171216768726058L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.showInfo(
                        target,
                        new StringResourceModel("layerSettings", ServiceLayerConfigurationPanel.this, null),
                        new StringResourceModel(
                                "layerSettingsHelp.message", ServiceLayerConfigurationPanel.this, null));
            }
        });
    }

    protected ServiceResourceProvider getServiceResourceUtil() {
        return GeoServerApplication.get().getBeanOfType(ServiceResourceProvider.class);
    }

    private LoadableDetachableModel<List<String>> servicesVotedModel(ResourceInfo resource) {
        return new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                return getServiceResourceUtil().getServicesForResource(resource);
            }
        };
    }
}
