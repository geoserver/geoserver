/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceUtil;
import org.geoserver.web.GeoServerApplication;

public class ServiceLayerConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {
    private static final long serialVersionUID = 1L;

    private WebMarkupContainer panel1;
    private Palette<String> servicesMultiSelector;

    public ServiceLayerConfigurationPanel(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);
        final AjaxCheckBox configEnabledCheck =
                new AjaxCheckBox(
                        "configEnabled",
                        new PropertyModel<Boolean>(layerModel, "resource.serviceConfiguration")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        ServiceLayerConfigurationPanel.this
                                .getServicesMultiSelector()
                                .setVisible(getModelObject());
                        target.add(ServiceLayerConfigurationPanel.this.getPanel1());
                    }
                };
        add(configEnabledCheck);
        PropertyModel<List<String>> dsModel =
                new PropertyModel<>(layerModel, "resource.disabledServices");
        final IChoiceRenderer<String> renderer =
                new ChoiceRenderer<String>() {
                    @Override
                    public String getObject(
                            String id, IModel<? extends List<? extends String>> choices) {
                        return id;
                    }

                    @Override
                    public Object getDisplayValue(String object) {
                        if (object == null) return null;
                        return super.getDisplayValue(object);
                    }

                    @Override
                    public String getIdValue(String object, int index) {
                        if (object == null) return null;
                        return object;
                    }
                };

        servicesMultiSelector =
                new Palette<String>(
                        "servicesSelection",
                        dsModel,
                        servicesVotedModel(layerModel.getObject().getResource()),
                        renderer,
                        10,
                        false) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("DisabledServicesPalette.selectedHeader"));
                    }

                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("DisabledServicesPalette.availableHeader"));
                    }
                };
        servicesMultiSelector.add(new DefaultTheme());
        servicesMultiSelector.setVisible(
                layerModel.getObject().getResource().isServiceConfiguration());
        panel1 = new WebMarkupContainer("panel1");
        panel1.setOutputMarkupPlaceholderTag(true);
        add(panel1);
        panel1.add(servicesMultiSelector);
    }

    protected ServiceResourceUtil getServiceResourceUtil() {
        return (ServiceResourceUtil)
                GeoServerApplication.get().getBean(ServiceResourceUtil.BEAN_ID);
    }

    private LoadableDetachableModel<List<String>> servicesVotedModel(ResourceInfo resource) {
        return new LoadableDetachableModel<List<String>>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                return getServiceResourceUtil().getLayerVotedServices(resource);
            }
        };
    }

    public Palette<String> getServicesMultiSelector() {
        return servicesMultiSelector;
    }

    public WebMarkupContainer getPanel1() {
        return panel1;
    }
}
