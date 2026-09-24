/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.jspecify.annotations.Nullable;

/** Table to manage conformance settings for a service. */
public class ConformanceTable extends GeoServerTablePanel<APIConformance> {

    private static final CssResourceReference CSS =
            new CssResourceReference(ConformanceTable.class, "ConformanceTable.css");

    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(ConformanceTable.class, "ConformanceTable.js");

    private final IModel<?> serviceModel;

    private final IModel<ConformanceInfo<?>> conformanceModel;

    /**
     * Table to manage conformance settings for service.
     *
     * @param serviceModel the {@code ServiceInfo} being edited, shows conformance classes in effect
     * @param conformanceModel {@code ConformanceInfo} from service
     */
    public ConformanceTable(
            String id, IModel<?> serviceModel, IModel<ConformanceInfo<?>> conformanceModel, Component parent) {
        super(id, new ConformanceDataProvider(conformanceModel, parent));
        this.serviceModel = serviceModel;
        this.conformanceModel = conformanceModel;

        // set up for editing
        setPageable(false);
        setOutputMarkupId(true);
        setSortable(false);
        setItemReuseStrategy(new DefaultItemReuseStrategy());
        setSelectable(false); // no selection, the editable checkboxes are a different case
        setFilterable(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(
            String id, IModel<APIConformance> itemModel, GeoServerDataProvider.Property<APIConformance> property) {
        if ("enabled".equals(property.getName())) {
            Fragment fragment = new Fragment(id, "checkboxFragment", this);
            fragment.add(new ThreeStateCheckBox("checkbox", (IModel<Boolean>) property.getModel(itemModel)));
            // what each checkbox state would mean for the service, so the page can show it as the user clicks
            fragment.add(AttributeModifier.replace(
                    "data-in-effect-unset", IModel.of(() -> isInEffect(itemModel.getObject(), null))));
            fragment.add(AttributeModifier.replace(
                    "data-in-effect-true", IModel.of(() -> isInEffect(itemModel.getObject(), Boolean.TRUE))));
            fragment.add(AttributeModifier.replace(
                    "data-in-effect-false", IModel.of(() -> isInEffect(itemModel.getObject(), Boolean.FALSE))));
            return fragment;
        }
        if (ConformanceDataProvider.ID.equals(property)) {
            Label label = new Label(id, property.getModel(itemModel));
            label.add(AttributeModifier.append(
                    "class",
                    IModel.of(() -> isInEffect(itemModel.getObject())
                            ? "gs-conformance-id"
                            : "gs-conformance-id gs-conformance-disabled")));
            return label;
        }
        return null; // default to label
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));
        response.render(OnDomReadyHeaderItem.forScript("gsConformanceTableInit('" + getMarkupId() + "')"));
    }

    /**
     * Checks if the conformance class is in effect for the service, following its default when not set explicitly.
     *
     * @param conformance conformance class listed in the table
     * @return {@code true} if the service currently declares the conformance class
     */
    @SuppressWarnings("unchecked")
    private boolean isInEffect(APIConformance conformance) {
        ServiceInfo service = (ServiceInfo) serviceModel.getObject();
        return ((ConformanceInfo<ServiceInfo>) conformanceModel.getObject())
                .conformances(service)
                .contains(conformance);
    }

    /**
     * Checks if the conformance class would be in effect with its checkbox in the given state.
     *
     * <p>Defaults and dependencies between classes are only known to the {@link ConformanceInfo}, so the state is
     * applied to it briefly and then restored.
     *
     * @param conformance conformance class listed in the table
     * @param enabled checkbox state, {@code null} to follow the default
     * @return {@code true} if the service would declare the conformance class
     */
    @SuppressWarnings("unchecked")
    private boolean isInEffect(APIConformance conformance, @Nullable Boolean enabled) {
        ServiceInfo service = (ServiceInfo) serviceModel.getObject();
        ConformanceInfo<ServiceInfo> info = (ConformanceInfo<ServiceInfo>) conformanceModel.getObject();
        Boolean current = info.isEnabled(conformance);
        try {
            info.setEnabled(conformance, enabled);
            return info.conformances(service).contains(conformance);
        } finally {
            info.setEnabled(conformance, current);
        }
    }

    private static class ConformanceDataProvider extends GeoServerDataProvider<APIConformance> {

        static final Property<APIConformance> ID = new BeanProperty<>("id");
        static final Property<APIConformance> LEVEL = new BeanProperty<>("level");
        static final Property<APIConformance> TYPE = new BeanProperty<>("type");

        private final IModel<ConformanceInfo<?>> conformanceModel;
        private final Component parent;

        public ConformanceDataProvider(IModel<ConformanceInfo<?>> conformanceModel, Component parent) {
            this.conformanceModel = conformanceModel;
            this.parent = parent;
        }

        @Override
        protected List<Property<APIConformance>> getProperties() {
            Property<APIConformance> enabled = new AbstractProperty<>("enabled") {
                @Override
                public Object getPropertyValue(APIConformance item) {
                    return new IModel<Boolean>() {

                        @Override
                        public Boolean getObject() {
                            return conformanceModel.getObject().isEnabled(item);
                        }

                        @Override
                        public void setObject(Boolean object) {
                            conformanceModel.getObject().setEnabled(item, object);
                        }
                    };
                }
            };
            Property<APIConformance> name = new AbstractProperty<>("name") {
                @Override
                public Object getPropertyValue(APIConformance item) {
                    return new ParamResourceModel(
                                    conformanceModel.getObject().getId() + "." + item.getProperty(), parent)
                            .getString();
                }

                @Override
                public IModel<?> getModel(IModel<APIConformance> itemModel) {
                    return super.getModel(itemModel);
                }
            };
            return List.of(enabled, name, ID, LEVEL, TYPE);
        }

        @Override
        protected List<APIConformance> getItems() {
            return conformanceModel.getObject().configurableConformances();
        }
    }
}
