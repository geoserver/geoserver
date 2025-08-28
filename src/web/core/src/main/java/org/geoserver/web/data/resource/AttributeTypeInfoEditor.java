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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
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
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;

/** Component editing a list of {@link AttributeTypeInfo} */
class AttributeTypeInfoEditor extends Panel {

    static final Logger LOGGER = Logging.getLogger(AttributeTypeInfoEditor.class);

    // editor panel constants
    private static final Property<AttributeTypeInfo> NAME = new GeoServerDataProvider.BeanProperty<>("name");
    private static final Property<AttributeTypeInfo> BINDING = new GeoServerDataProvider.BeanProperty<>("binding");
    private static final Property<AttributeTypeInfo> DESCRIPTION =
            new GeoServerDataProvider.BeanProperty<>("description");
    private static final GeoServerDataProvider.PropertyPlaceholder<AttributeTypeInfo> DETAILS =
            new GeoServerDataProvider.PropertyPlaceholder<>("details");
    private static final GeoServerDataProvider.PropertyPlaceholder<AttributeTypeInfo> REMOVE =
            new GeoServerDataProvider.PropertyPlaceholder<>("remove");
    private static final GeoServerDataProvider.PropertyPlaceholder<AttributeTypeInfo> EDIT =
            new GeoServerDataProvider.PropertyPlaceholder<>("edit");

    // make sure we don't end up serializing the list, but get it fresh from the dataProvider,
    // to avoid serialization issues seen in GEOS-8273
    private static final LoadableDetachableModel<List<Property<AttributeTypeInfo>>> propertiesModel =
            new LoadableDetachableModel<>() {
                @Override
                protected List<Property<AttributeTypeInfo>> load() {
                    return Arrays.asList(NAME, BINDING, DESCRIPTION, DETAILS, EDIT, REMOVE);
                }
            };
    private final ReorderableTablePanel<AttributeTypeInfo> table;
    private final GeoServerDialog editDialog;
    private final Component parent;

    public AttributeTypeInfoEditor(String id, IModel model, Component parent) {
        super(id, model);
        this.parent = parent;

        add(new AddAttributeLink());
        add(new ResetLink());

        List<AttributeTypeInfo> attributes = getAttributes(model, parent);
        table = new EditorTable(attributes, parent);
        table.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());
        table.setPageable(false);
        table.setFilterable(false);
        add(table);

        editDialog = new GeoServerDialog("dialog");
        add(editDialog);
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
            Map<String, Class> bindings = nativeAttributes.stream()
                    .collect(Collectors.toMap(AttributeTypeInfo::getName, AttributeTypeInfo::getBinding));
            for (AttributeTypeInfo at : attributes) {
                if (at.getBinding() == null) {
                    at.setBinding(bindings.get(at.getName()));
                }
            }
        }

        // make a copy to avoid touching the original live list until save
        return new ArrayList<>(attributes);
    }

    static List<AttributeTypeInfo> loadNativeAttributes(FeatureTypeInfo typeInfo, Component component) {
        try {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            final ResourcePool resourcePool = catalog.getResourcePool();
            // using loadAttributes to dodge the ResourcePool caches, the
            // feature type structure might have been modified (e.g., SQL view editing)
            return resourcePool.loadAttributes(typeInfo);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Grabbing the attribute list failed", e);
            String errorMessage = e.getMessage();
            if (e.getCause() instanceof SQLException) {
                errorMessage = e.getCause().getMessage();
            }
            String error = new ParamResourceModel("attributeListingFailed", component, errorMessage).getString();
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
            super("table", AttributeTypeInfo.class, attributes, AttributeTypeInfoEditor.propertiesModel);
            this.targetComponent = targetComponent;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Component getComponentForProperty(
                String id, IModel<AttributeTypeInfo> itemModel, Property<AttributeTypeInfo> property) {
            IModel model = property.getModel(itemModel);
            if (property == NAME) {
                Fragment f = new Fragment(id, "name", getParent());
                Label name = new Label("name", model);
                f.add(name);
                return f;
            } else if (property == BINDING) {
                Fragment f = new Fragment(id, "type", getParent());
                ClassLabel type = new ClassLabel(model);
                f.add(type);
                return f;
            } else if (property == DESCRIPTION) {
                Fragment f = new Fragment(id, "description", getParent());
                Label description = new Label("description", model) {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return (IConverter<C>) new InternationalStringConverter();
                    }
                };
                f.add(description);
                return f;
            } else if (property == DETAILS) {
                return new MultiLineLabel(id, new LoadableDetachableModel<String>() {
                    @Override
                    public String load() {
                        return composeDetailsText(itemModel.getObject());
                    }
                });
            } else if (property == EDIT) {
                PackageResourceReference icon =
                        new PackageResourceReference(getClass(), "../../img/icons/silk/pencil.png");
                return new ImageAjaxLink<>(id, icon) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        openEditAttributeDialog(target, itemModel);
                    }
                };
            } else if (property == REMOVE) {
                final AttributeTypeInfo entry = itemModel.getObject();
                PackageResourceReference icon =
                        new PackageResourceReference(getClass(), "../../img/icons/silk/delete.png");
                return new ImageAjaxLink<>(id, icon) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        getItems().remove(entry);
                        target.add(targetComponent);
                    }
                };
            }
            return null;
        }
    }

    private String composeDetailsText(AttributeTypeInfo attributeTypeInfo) {
        StringBuilder labelText = new StringBuilder();

        if (!attributeTypeInfo.getName().equals(attributeTypeInfo.getSource())) {
            labelText
                    .append(new StringResourceModel("AttributeTypeInfoEditPanel.source").getString())
                    .append(": ")
                    .append(attributeTypeInfo.getSource())
                    .append("\n");
        }

        if (attributeTypeInfo.isNillable()) {
            labelText
                    .append(new StringResourceModel("AttributeTypeInfoEditPanel.nillable").getString())
                    .append(": true")
                    .append("\n");
        }

        if (attributeTypeInfo.getOptions() != null
                && !attributeTypeInfo.getOptions().isEmpty()) {
            labelText
                    .append(new StringResourceModel("AttributeTypeInfoEditPanel.restrictionType.options").getString())
                    .append(": ");

            String collect =
                    attributeTypeInfo.getOptions().stream().map(String::valueOf).collect(Collectors.joining(", "));

            if (collect.length() > 75) {
                collect = collect.substring(0, 75) + "...";
            }

            labelText.append(collect).append("\n");
        }

        if (attributeTypeInfo.getRange() != null) {
            NumberRange<? extends Number> range =
                    attributeTypeInfo.getRange().castTo((Class) attributeTypeInfo.getBinding());
            labelText
                    .append(new StringResourceModel("AttributeTypeInfoEditPanel.restrictionType.range").getString())
                    .append(": ")
                    .append(range)
                    .append("\n");
        }

        return labelText.toString();
    }

    private void openEditAttributeDialog(AjaxRequestTarget target, IModel<AttributeTypeInfo> itemModel) {
        editDialog.setTitle(new StringResourceModel("editAttribute"));
        editDialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
            @Override
            protected Component getContents(String id) {
                return new AttributeTypeInfoEditPanel(id, itemModel);
            }

            @Override
            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                AttributeTypeInfoEditPanel panel = (AttributeTypeInfoEditPanel) contents;
                panel.finalizeAttributeValues();
                return true;
            }

            @Override
            public void onClose(AjaxRequestTarget target) {
                target.add(table);
            }
        });
    }

    private class AddAttributeLink extends AjaxLink<LayerInfo> {

        public AddAttributeLink() {
            super("addAttribute");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            AttributeTypeInfo newAttribute =
                    GeoServerApplication.get().getCatalog().getFactory().createAttribute();
            editDialog.setTitle(new StringResourceModel("addAttribute"));
            editDialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                @Override
                protected Component getContents(String id) {
                    return new AttributeTypeInfoEditPanel(id, new Model<>(newAttribute));
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    AttributeTypeInfoEditPanel panel = (AttributeTypeInfoEditPanel) contents;
                    panel.finalizeAttributeValues();
                    table.getItems().add(newAttribute);
                    return true;
                }

                @Override
                public void onClose(AjaxRequestTarget target) {
                    target.add(table);
                }
            });
        }
    }

    private class ResetLink extends AjaxLink<LayerInfo> {

        public ResetLink() {
            super("resetAttributes");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            table.getItems().clear();
            FeatureTypeInfo typeInfo = (FeatureTypeInfo) AttributeTypeInfoEditor.this.getDefaultModelObject();
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
