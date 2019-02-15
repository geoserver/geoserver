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
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.geoserver.platform.GeoServerExtensions.extensions;

/**
 * Resource configuration section for generated geometry on-the-fly.
 *
 * <p>Activates only when working on {@link SimpleFeatureType}.
 */
public class GeneratedGeometryConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 1L;

    private final GeneratedGeometryResourcePoolCallback resourcePoolCallback;

    private Fragment content;
    private final Map<String, Component> componentMap = new HashMap<>();
    private ChoiceRenderer<GeometryGenerationStrategy> choiceRenderer =
            new ChoiceRenderer<GeometryGenerationStrategy>() {
                @Override
                public Object getDisplayValue(GeometryGenerationStrategy ggm) {
                    return new StringResourceModel(
                                    format("geometryGenerationMethodology.%s", ggm.getName()))
                            .getString();
                }
            };
    private GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> selectedStrategy;
    private WebMarkupContainer methodologyConfiguration;

    public GeneratedGeometryConfigurationPanel(String id, final IModel model) {
        super(id, model);
        resourcePoolCallback = findResourcePoolCallback();
        init(model);
    }

    private GeneratedGeometryResourcePoolCallback findResourcePoolCallback() {
        return extensions(GeneratedGeometryResourcePoolCallback.class).stream().findFirst().orElse(null);
    }

    private void init(IModel model) {
        if (isSimpleFeatureType(model)) {
            initMainPanel();
            List<GeometryGenerationStrategy> methodologies =
                    extensions(GeometryGenerationStrategy.class);
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
            List<GeometryGenerationStrategy> strategies, IModel model) {
        DropDownChoice<GeometryGenerationStrategy> methodologyDropDown =
                new DropDownChoice<>(
                        "methodologyDropDown",
                        new PropertyModel<>(this, "selectedStrategy"),
                        strategies,
                        choiceRenderer);
        content.add(methodologyDropDown);
        methodologyConfiguration = new WebMarkupContainer("methodologyConfiguration");
        methodologyConfiguration.setOutputMarkupId(true);
        content.add(methodologyConfiguration);

        for (GeometryGenerationStrategy ggm : strategies) {
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
                        if (selectedStrategy != null) {
                            resourcePoolCallback.setStrategy(selectedStrategy);
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

    private FeatureType getFeatureType(IModel model) throws IOException {
        final ResourcePool resourcePool = getResourcePool();
        return resourcePool.getFeatureType((FeatureTypeInfo) model.getObject());
    }

    private ResourcePool getResourcePool() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        return catalog.getResourcePool();
    }

    private Optional<Component> getCurrentUIComponent() {
        if (selectedStrategy == null) {
            return empty();
        }
        return ofNullable(componentMap.get(selectedStrategy.getName()));
    }

    private String i18n(String messageKey) {
        return new StringResourceModel(messageKey, GeneratedGeometryConfigurationPanel.this, null)
                .getString();
    }
}
