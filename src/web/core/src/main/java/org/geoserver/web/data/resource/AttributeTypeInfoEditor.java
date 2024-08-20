/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geotools.util.logging.Logging;

/** Component editing a list of {@link AttributeTypeInfo} */
class AttributeTypeInfoEditor extends Panel {

    static final Logger LOGGER = Logging.getLogger(AttributeTypeInfoEditor.class);

    // editor panel constants
    private static final Property<AttributeTypeInfo> NAME =
            new GeoServerDataProvider.BeanProperty<>("name");
    private static final Property<AttributeTypeInfo> BINDING =
            new GeoServerDataProvider.BeanProperty<>("binding");
    private static final Property<AttributeTypeInfo> SOURCE =
            new GeoServerDataProvider.BeanProperty<>("source");
    private static final Property<AttributeTypeInfo> DESCRIPTION =
            new GeoServerDataProvider.BeanProperty<>("description");
    private static final Property<AttributeTypeInfo> NILLABLE =
            new GeoServerDataProvider.BeanProperty<>("nillable");
    private static final GeoServerDataProvider.PropertyPlaceholder<AttributeTypeInfo> REMOVE =
            new GeoServerDataProvider.PropertyPlaceholder<>("remove");

    // make sure we don't end up serializing the list, but get it fresh from the dataProvider,
    // to avoid serialization issues seen in GEOS-8273
    private static final LoadableDetachableModel<List<Property<AttributeTypeInfo>>>
            propertiesModel =
                    new LoadableDetachableModel<List<Property<AttributeTypeInfo>>>() {
                        @Override
                        protected List<Property<AttributeTypeInfo>> load() {
                            return Arrays.asList(
                                    NAME, BINDING, SOURCE, DESCRIPTION, NILLABLE, REMOVE);
                        }
                    };
    private final ReorderableTablePanel<AttributeTypeInfo> table;
    private final Component parent;

    public AttributeTypeInfoEditor(String id, IModel model, Component parent) {
        super(id, model);
        this.parent = parent;

        add(new AddAttributeLink());
        add(new ResetLink());

        List<AttributeTypeInfo> attributes = getAttributes(model, parent);
        table = new EditorTable(attributes, parent);
        table.setItemReuseStrategy(new DefaultItemReuseStrategy());
        table.setPageable(false);
        table.setFilterable(false);
        add(table);
    }

    private static List<AttributeTypeInfo> getAttributes(IModel model, Component parent) {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) model.getObject();
        List<AttributeTypeInfo> attributes = typeInfo.getAttributes();

        // no attributes loaded yet?
        if (attributes == null || attributes.isEmpty()) {
            return loadNativeAttributes(typeInfo, parent);
        }

        // attributes available, but could be old and lack the binding
        if (attributes.stream().anyMatch(a -> a.getBinding() == null)) {
            List<AttributeTypeInfo> nativeAttributes = loadNativeAttributes(typeInfo, parent);
            Map<String, Class> bindings =
                    nativeAttributes.stream()
                            .collect(Collectors.toMap(a -> a.getName(), a -> a.getBinding()));
            for (AttributeTypeInfo at : attributes) {
                if (at.getBinding() == null) {
                    at.setBinding(bindings.get(at.getName()));
                }
            }
        }

        // make a copy to avoid touching the original live list until save
        return new ArrayList<>(attributes);
    }

    static List<AttributeTypeInfo> loadNativeAttributes(
            FeatureTypeInfo typeInfo, Component component) {
        try {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            final ResourcePool resourcePool = catalog.getResourcePool();
            // using loadAttributes to dodge the ResourcePool caches, the
            // feature type structure might have been modified (e.g., SQL view editing)
            return resourcePool.loadAttributes(typeInfo);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Grabbing the attribute list failed", e);
            String errorMessage = e.getMessage();
            if (e.getCause() != null && e.getCause() instanceof SQLException) {
                errorMessage = e.getCause().getMessage();
            }
            String error =
                    new ParamResourceModel("attributeListingFailed", component, errorMessage)
                            .getString();
            try {
                component.getPage().error(error);
            } catch (Exception e1) {
                LOGGER.log(Level.SEVERE, "Grabbing the attribute list failed", e1.getMessage());
            }
            return Collections.emptyList();
        }
    }

    public Collection<? extends AttributeTypeInfo> getItems() {
        table.processInputs();
        return table.getItems();
    }

    private class EditorTable extends ReorderableTablePanel<AttributeTypeInfo> {
        private final Component targetComponent;

        public EditorTable(List<AttributeTypeInfo> attributes, Component targetComponent) {
            super(
                    "table",
                    AttributeTypeInfo.class,
                    attributes,
                    AttributeTypeInfoEditor.propertiesModel);
            this.targetComponent = targetComponent;
        }

        /**
         * Note, all editors returned are without validation and with a behavior that updates the
         * server side whenever the editor loses focus, to make sure drag/up/down don't end up
         * resetting the client side content (as they happen server side, with a subsequent AJAX
         * redraw on the client).
         */
        @Override
        @SuppressWarnings("unchecked")
        protected Component getComponentForProperty(
                String id,
                IModel<AttributeTypeInfo> itemModel,
                Property<AttributeTypeInfo> property) {
            IModel model = property.getModel(itemModel);
            if (property == NAME) {
                Fragment f = new Fragment(id, "text", getParent());
                TextField<String> nameField = new TextField<>("text", model);
                nameField.add(new SourceUpdateBehavior(itemModel));
                f.add(nameField);
                return f;
            } else if (property == BINDING) {
                Fragment f = new Fragment(id, "type", getParent());
                TextField<Class> type = new ClassTextField(id, model);
                f.add(type);
                return f;
            } else if (property == SOURCE) {
                Fragment f = new Fragment(id, "area", getParent());
                TextArea<String> source = new TextArea<>("area", model);
                source.add(new UpdateModelBehavior());
                f.add(source);
                return f;
            } else if (property == DESCRIPTION) {
                Fragment f = new Fragment(id, "description", getParent());
                TextArea<String> source =
                        new TextArea<>("description", model) {
                            @SuppressWarnings("unchecked")
                            @Override
                            public <C> IConverter<C> getConverter(Class<C> type) {
                                return (IConverter<C>) new InternationalStringConverter();
                            }
                        };
                source.add(new UpdateModelBehavior());
                f.add(source);
                return f;
            } else if (property == NILLABLE) {
                Fragment f = new Fragment(id, "check", getParent());
                CheckBox check = new CheckBox("check", model);
                f.add(check);
                return f;
            } else if (property == REMOVE) {
                final AttributeTypeInfo entry = (AttributeTypeInfo) itemModel.getObject();
                PackageResourceReference icon =
                        new PackageResourceReference(getClass(), "../../img/icons/silk/delete.png");
                ImageAjaxLink<Object> link =
                        new ImageAjaxLink<Object>(id, icon) {

                            @Override
                            protected void onClick(AjaxRequestTarget target) {
                                getItems().remove(entry);
                                target.add(targetComponent);
                            }
                        };
                return link;
            }

            return null;
        }
    }

    private static class UpdateModelBehavior extends AjaxFormComponentUpdatingBehavior {

        public UpdateModelBehavior() {
            super("blur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
            // nothing to do, the mere presence is enough to update the server side model
            // before up/down/drag actions are performed
        }
    }

    /**
     * Behavior that updates the source of an {@link AttributeTypeInfo} when the associated form
     * component loses focus. This behavior ensures that if the raw source is null and the name of
     * the attribute has changed, the source is set to the previous name (the original name of the
     * attribute).
     */
    private class SourceUpdateBehavior extends AjaxFormComponentUpdatingBehavior {

        private final IModel<AttributeTypeInfo> itemModel;
        private final String previousName;

        /**
         * Constructs a new SourceUpdateBehavior.
         *
         * @param itemModel the model of the {@link AttributeTypeInfo} being edited
         */
        public SourceUpdateBehavior(IModel<AttributeTypeInfo> itemModel) {
            super("blur");
            this.itemModel = itemModel;
            this.previousName = itemModel.getObject().getName();
        }

        /**
         * Called when the form component loses focus and updates the source of the attribute if
         * necessary.
         *
         * @param ajaxRequestTarget the AJAX request target
         */
        @Override
        protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
            AttributeTypeInfo attribute = itemModel.getObject();
            if (attribute.getRawSource() == null
                    && !Objects.equals(attribute.getName(), previousName)) {
                attribute.setSource(previousName);
            }
        }
    }

    private class AddAttributeLink extends AjaxLink<LayerInfo> {

        public AddAttributeLink() {
            super("addAttribute");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            AttributeTypeInfo att =
                    GeoServerApplication.get().getCatalog().getFactory().createAttribute();
            table.getItems().add(att);
            target.add(parent);
        }
    }

    private class ResetLink extends AjaxLink<LayerInfo> {

        public ResetLink() {
            super("resetAttributes");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            table.getItems().clear();
            FeatureTypeInfo typeInfo =
                    (FeatureTypeInfo) AttributeTypeInfoEditor.this.getDefaultModelObject();
            table.getItems().addAll(loadNativeAttributes(typeInfo, parent));
            target.add(parent);
        }

        @Override
        protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
            super.updateAjaxAttributes(attributes);

            // confirm dialog
            AjaxCallListener ajaxCallListener = new AjaxCallListener();
            String message = new ParamResourceModel("confirmReset", this).getString();
            ajaxCallListener.onPrecondition("return confirm('" + message + "');");
            attributes.getAjaxCallListeners().add(ajaxCallListener);
        }
    }
}
