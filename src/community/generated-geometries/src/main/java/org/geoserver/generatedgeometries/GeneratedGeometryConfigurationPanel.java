/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * Resource configuration section for generated geometry on-the-fly.
 *
 * <p>Activates only when working on {@link SimpleFeatureType}.
 */
public class GeneratedGeometryConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 1L;

    private Fragment content;
    private final Map<String, Component> componentMap = new HashMap<>();
    private ChoiceRenderer<GeometryGenerationMethodology> choiceRenderer =
            new ChoiceRenderer<GeometryGenerationMethodology>() {
                @Override
                public Object getDisplayValue(GeometryGenerationMethodology ggm) {
                    return new StringResourceModel(
                                    format("geometryGenerationMethodology.%s", ggm.getName()))
                            .getString();
                }
            };
    private GeometryGenerationMethodology selectedMethodology;
    private WebMarkupContainer methodologyConfiguration;

    public GeneratedGeometryConfigurationPanel(String id, final IModel model) {
        super(id, model);
        init(model);
    }

    private void init(IModel model) {
        if (isSimpleFeatureType(model)) {
            initMainPanel();
            List<GeometryGenerationMethodology> methodologies =
                    GeoServerExtensions.extensions(GeometryGenerationMethodology.class);
            initMethodologyDropdown(methodologies, model);
            initActionLink(model);
        } else {
            showWarningMessage();
        }
    }

    private void initMainPanel() {
        add(content = new Fragment("content", "main", this));
    }

    private void initMethodologyDropdown(
            List<GeometryGenerationMethodology> methodologies, IModel model) {
        DropDownChoice<GeometryGenerationMethodology> methodologyDropDown =
                new DropDownChoice<>(
                        "methodologyDropDown",
                        new PropertyModel<>(this, "selectedMethodology"),
                        methodologies,
                        choiceRenderer);
        content.add(methodologyDropDown);
        methodologyConfiguration = new WebMarkupContainer("methodologyConfiguration");
        methodologyConfiguration.setOutputMarkupId(true);
        content.add(methodologyConfiguration);

        for (GeometryGenerationMethodology ggm : methodologies) {
            Component configuration = ggm.createUI("configuration", model);
            componentMap.put(ggm.getName(), configuration);
            configuration.setVisible(false);
        }

        methodologyConfiguration.add(
                new ListView<Component>("configurations", new ArrayList<>(componentMap.values())) {
                    @Override
                    protected void populateItem(ListItem<Component> item) {
                        item.add(item.getModelObject());
                    }
                });

        methodologyDropDown.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        componentMap.values().forEach(component -> component.setVisible(false));
                        getCurrentUIComponent().ifPresent(c -> c.setVisible(true));
                        target.add(methodologyConfiguration);
                    }
                });
    }

    private void initActionLink(IModel model) {
        GeoServerAjaxFormLink createGeometryLink =
                new GeoServerAjaxFormLink("createGeometryLink") {
                    @Override
                    protected void onClick(AjaxRequestTarget target, Form form) {
                        if (selectedMethodology != null) {
                            try {
                                selectedMethodology.defineGeometryAttributeFor(
                                        getSimpleFeatureType(model));
                            } catch (IOException | ConfigurationException e) {
                                e.printStackTrace();
                                getCurrentUIComponent().ifPresent(c -> error(i18n("invalidConfiguration")));
                            }
                        } else {
                            error(i18n("configurationNotSelected"));
                        }
                    }
                };
        content.add(createGeometryLink);
    }

    private void showWarningMessage() {
        add(content = new Fragment("content", "incorrectFeatureType", this));
    }

    private boolean isSimpleFeatureType(IModel model) {
        try {
            return SimpleFeatureType.class.isAssignableFrom(getFeatureType(model).getClass());
        } catch (IOException e) {
            return false;
        }
    }

    private SimpleFeatureType getSimpleFeatureType(IModel model) throws IOException {
        FeatureType featureType = getFeatureType(model);
        return (SimpleFeatureType) featureType;
    }

    private FeatureType getFeatureType(IModel model) throws IOException {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();
        return resourcePool.getFeatureType((FeatureTypeInfo) model.getObject());
    }

    private Optional<Component> getCurrentUIComponent() {
        if (selectedMethodology == null) {
            return empty();
        }
        return ofNullable(componentMap.get(selectedMethodology.getName()));
    }

    private String i18n(String messageKey) {
        return new StringResourceModel(messageKey, GeneratedGeometryConfigurationPanel.this, null)
                .getString();
    }
}
