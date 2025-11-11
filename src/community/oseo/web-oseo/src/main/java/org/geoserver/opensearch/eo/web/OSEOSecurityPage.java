/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.security.EOAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfoImpl;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfoImpl;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;

/**
 * Page for editing OSEO security settings, namely collection and product access limits
 *
 * <p>The page allows adding, editing, removing and reordering both collection and product access limits
 */
public class OSEOSecurityPage extends AbstractSecurityPage {

    private final List<EOCollectionAccessLimitInfo> collectionLimits;
    private final List<EOProductAccessLimitInfo> productLimits;

    public OSEOSecurityPage() {
        GeoServer gs = ((GeoServerApplication) getApplication()).getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        this.collectionLimits = service.getCollectionLimits();
        this.productLimits = service.getProductLimits();

        Form<Void> form = new Form<>("form");
        add(form);
        setupCollectionLimitTable(form);
        setupProductionLimitTable(form);

        // page buttons
        SubmitLink submit = new SubmitLink("submit", new StringResourceModel("save", null, null)) {
            @Override
            public void onSubmit() {
                saveLimits(true);
            }
        };
        form.add(submit);
        Button apply = new Button("apply") {
            @Override
            public void onSubmit() {
                saveLimits(false);
            }
        };
        form.add(apply);
        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    private void setupCollectionLimitTable(Form<Void> form) {
        WebMarkupContainer collectionLimitsContainer = new WebMarkupContainer("collectionLimitsContainer");
        collectionLimitsContainer.setOutputMarkupId(true);
        form.add(collectionLimitsContainer);
        LimitTable<EOCollectionAccessLimitInfo> collectionTable =
                new LimitTable<>("collectionLimits", collectionLimits, EOCollectionAccessLimitInfo.class) {

                    @Override
                    protected void openEditDialog(
                            AjaxRequestTarget target, IModel<EOCollectionAccessLimitInfo> itemModel) {
                        EOCollectionAccessLimitInfo limit = itemModel.getObject();
                        showEditor(
                                target,
                                collectionLimitsContainer,
                                new ParamResourceModel("editCollectionLimit", OSEOSecurityPage.this),
                                limit,
                                false,
                                true);
                    }
                };
        collectionLimitsContainer.add(collectionTable);
        form.add(createAddCollectionLimitLink(collectionLimitsContainer));
    }

    private AjaxLink<Void> createAddCollectionLimitLink(Component limitsContainer) {
        return new AjaxLink<>("addCollectionRule") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showEditor(
                        target,
                        limitsContainer,
                        new ParamResourceModel("addCollectionLimit", OSEOSecurityPage.this),
                        new EOCollectionAccessLimitInfoImpl(),
                        false,
                        false);
            }
        };
    }

    private void showEditor(
            AjaxRequestTarget target,
            Component limitsContainer,
            ParamResourceModel title,
            EOAccessLimitInfo limitToEdit,
            boolean isProduct,
            boolean edit) {
        dialog.setTitle(title);
        dialog.setInitialHeight(500);
        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

            @Override
            protected Component getContents(String id) {
                return new OSEOLimitPanel(id, new Model<>(limitToEdit));
            }

            @Override
            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                if (!edit) {
                    if (isProduct) {
                        EOProductAccessLimitInfo limit = (EOProductAccessLimitInfo) contents.getDefaultModelObject();
                        productLimits.add(limit);
                    } else {
                        EOCollectionAccessLimitInfo limit =
                                (EOCollectionAccessLimitInfo) contents.getDefaultModelObject();
                        collectionLimits.add(limit);
                    }
                }
                target.add(limitsContainer);
                return true;
            }
        });
    }

    private void setupProductionLimitTable(Form<Void> form) {
        WebMarkupContainer productLimitsContainer = new WebMarkupContainer("productLimitsContainer");
        productLimitsContainer.setOutputMarkupId(true);
        form.add(productLimitsContainer);
        LimitTable<EOProductAccessLimitInfo> productTable =
                new LimitTable<>("productLimits", productLimits, EOProductAccessLimitInfo.class) {

                    @Override
                    protected void openEditDialog(
                            AjaxRequestTarget target, IModel<EOProductAccessLimitInfo> itemModel) {
                        EOProductAccessLimitInfo limit = itemModel.getObject();
                        showEditor(
                                target,
                                productLimitsContainer,
                                new ParamResourceModel("editProductLimit", OSEOSecurityPage.this),
                                limit,
                                true,
                                true);
                    }
                };
        productLimitsContainer.add(productTable);
        form.add(createAddProductLimitLink(productLimitsContainer));
    }

    private AjaxLink<Void> createAddProductLimitLink(Component limitsContainer) {
        return new AjaxLink<>("addProductRule") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showEditor(
                        target,
                        limitsContainer,
                        new ParamResourceModel("addProductLimit", OSEOSecurityPage.this),
                        new EOProductAccessLimitInfoImpl(),
                        true,
                        false);
            }
        };
    }

    private void saveLimits(boolean doReturn) {
        // save updates
        GeoServer gs = ((GeoServerApplication) getApplication()).getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.getCollectionLimits().clear();
        service.getCollectionLimits().addAll(collectionLimits);
        service.getProductLimits().clear();
        service.getProductLimits().addAll(productLimits);
        gs.save(service);

        // return if requested
        if (doReturn) doReturn();
    }

    /**
     * Table for editing EO access limits
     *
     * @param <T> the limit type
     */
    private abstract static class LimitTable<T extends EOAccessLimitInfo> extends ReorderableTablePanel<T> {
        private static <T> IModel<List<Property<T>>> createPropertiesModel(Class<?> clazz) {
            List<String> names = (clazz == EOProductAccessLimitInfo.class)
                    ? List.of("collection", "cqlFilter", "roles")
                    : List.of("cqlFilter", "roles");

            // Cannot have a simple Model because List is not serializable while Model requires it
            return new LoadableDetachableModel<>() {
                @Override
                protected List<Property<T>> load() {
                    List<Property<T>> properties = names.stream()
                            .map(n -> (Property<T>) new BeanProperty<T>(n))
                            .collect(Collectors.toCollection(ArrayList::new));
                    properties.add(new GeoServerDataProvider.PropertyPlaceholder<>("edit"));
                    properties.add(new GeoServerDataProvider.PropertyPlaceholder<>("remove"));
                    return properties;
                }
            };
        }

        public LimitTable(String id, List<T> limits, Class<T> clazz) {
            super(id, clazz, limits, createPropertiesModel(clazz), false);
            setPageable(false);
            setFilterable(false);
            setSortable(false);
            setItemReuseStrategy(new DefaultItemReuseStrategy());
        }

        @Override
        protected Component getComponentForProperty(String id, IModel<T> itemModel, Property<T> property) {
            if ("roles".equals(property.getName())) {
                List<String> roles = itemModel.getObject().getRoles();
                String rolesLabel = String.join(", ", roles);
                return new Label(id, rolesLabel);
            } else if ("edit".equals(property.getName())) {
                PackageResourceReference icon =
                        new PackageResourceReference(GeoServerHomePage.class, "/img/icons/silk/pencil.png");
                return new ImageAjaxLink<>(id, icon) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        openEditDialog(target, itemModel);
                    }
                };
            } else if ("remove".equals(property.getName())) {
                final T entry = itemModel.getObject();
                PackageResourceReference icon =
                        new PackageResourceReference(GeoServerHomePage.class, "/img/icons/silk/delete.png");
                return new ImageAjaxLink<>(id, icon) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        getItems().remove(entry);
                        target.add(LimitTable.this.getParent());
                    }
                };
            }
            return null;
        }

        protected abstract void openEditDialog(AjaxRequestTarget target, IModel<T> itemModel);
    }
}
