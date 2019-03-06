/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.web;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.BasicResourceConfig;
import org.geoserver.web.data.resource.FeatureResourceConfigurationPanel;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * Resource configuration section for generated geometry on-the-fly.
 *
 * <p>Activates only when working on {@link SimpleFeatureType}.
 */
public class GeneratedGeometryConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 1L;

    private final transient Supplier<GeoServerApplication> geoServerApplicationSupplier;
    private final transient Function<Class, List> extensionFinder;

    private Fragment content;
    private final Map<String, Component> componentMap = new HashMap<>();
    private ChoiceRenderer<GeometryGenerationStrategyUIGenerator> choiceRenderer =
            new ChoiceRenderer<GeometryGenerationStrategyUIGenerator>() {
                @Override
                public Object getDisplayValue(
                        GeometryGenerationStrategyUIGenerator geometryGenerationStrategyPanel) {
                    return new StringResourceModel(
                                    format(
                                            "geometryGenerationMethodology.%s",
                                            geometryGenerationStrategyPanel.getName()))
                            .getString();
                }
            };
    private GeometryGenerationStrategyUIGenerator selectedStrategy;
    private WebMarkupContainer methodologyConfiguration;
    private String listNullValue =
            new ResourceModel("GeneratedGeometryConfigurationPanel.listNullValue").getObject();

    public GeneratedGeometryConfigurationPanel(String id, final IModel model) {
        this(id, model, GeoServerExtensions::extensions, GeoServerApplication::get);
    }

    public GeneratedGeometryConfigurationPanel(
            String id,
            final IModel model,
            Function<Class, List> extensionFinder,
            Supplier<GeoServerApplication> geoServerApplicationSupplier) {
        super(id, model);
        this.extensionFinder = extensionFinder;
        this.geoServerApplicationSupplier = geoServerApplicationSupplier;
        init(model);
    }

    private void init(IModel model) {
        if (isSimpleFeatureType(model)) {
            initMainPanel();
            List<GeometryGenerationStrategyUIGenerator> strategies =
                    extensionFinder.apply(GeometryGenerationStrategyUIGenerator.class);
            initMethodologyDropdown(strategies, model);
            initActionLink(model);
        } else {
            showWarningMessage();
        }
    }

    private void initMainPanel() {
        add(content = new Fragment("content", "main", this));
    }

    private void initMethodologyDropdown(
            List<GeometryGenerationStrategyUIGenerator> strategies, IModel model) {
        DropDownChoice<GeometryGenerationStrategyUIGenerator> methodologyDropDown =
                new DropDownChoice<GeometryGenerationStrategyUIGenerator>(
                        "methodologyDropDown",
                        new PropertyModel<>(this, "selectedStrategy"),
                        strategies,
                        choiceRenderer) {
                    @Override
                    protected String getNullKeyDisplayValue() {
                        return listNullValue;
                    }
                };
        content.add(methodologyDropDown);
        methodologyConfiguration = new WebMarkupContainer("methodologyConfiguration");
        methodologyConfiguration.setOutputMarkupId(true);
        content.add(methodologyConfiguration);

        for (GeometryGenerationStrategyUIGenerator ggsp : strategies) {
            Component configuration = ggsp.createUI("configuration", model);
            componentMap.put(ggsp.getName(), configuration);
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
        GeoServerAjaxFormLink createGeometryLink = createAjaxLink("createGeometryLink", model);
        GeoServerAjaxFormLink updateGeometryLink = createAjaxLink("updateGeometryLink", model);
        boolean isGeometryCreated = false;
        try {
            isGeometryCreated =
                    ((FeatureTypeInfo) model.getObject()).getFeatureType().getGeometryDescriptor()
                            != null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateGeometryLink.setVisible(isGeometryCreated);
        createGeometryLink.setVisible(!isGeometryCreated);

        content.add(createGeometryLink);
        content.add(updateGeometryLink);
    }

    private GeoServerAjaxFormLink createAjaxLink(String key, IModel model) {
        return new GeoServerAjaxFormLink(key) {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                try {
                    if (selectedStrategy != null) {
                        FeatureTypeInfo info = (FeatureTypeInfo) model.getObject();
                        selectedStrategy.configure(info);

                        ListView listView =
                                (ListView) getPage().get("publishedinfo:tabs:panel:theList");
                        findChildPanel(listView, BasicResourceConfig.class).ifPresent(target::add);
                        findChildPanel(listView, FeatureResourceConfigurationPanel.class)
                                .ifPresent(target::add);
                    } else {
                        error(i18n("configurationNotSelected"));
                    }
                } catch (Exception e) {
                    error(i18n("geometryCreationError"));
                }
            }
        };
    }

    private <C extends Component> Optional<C> findChildPanel(ListView listView, Class<C> clazz) {
        for (int i = 0; i < listView.getList().size(); i++) {
            Component component = listView.get(format("%d:content", i));
            if (clazz.isAssignableFrom(component.getClass())) {
                return (Optional<C>) Optional.of(component);
            }
        }
        return empty();
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
        Catalog catalog = geoServerApplicationSupplier.get().getCatalog();
        return catalog.getResourcePool();
    }

    private Optional<Component> getCurrentUIComponent() {
        if (selectedStrategy == null) {
            return empty();
        }
        return ofNullable(componentMap.get(selectedStrategy.getName()));
    }

    private String i18n(String messageKey) {
        return new StringResourceModel(messageKey, this, null).getString();
    }
}
