/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.importer;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.importer.LayerResource.LayerStatus;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class WMTSLayerImporterPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -3413451886777414860L;
    String storeId;
    private GeoServerTablePanel<LayerResource> layers;
    private WMTSLayerProvider provider;
    int importCount;
    int errorCount;
    int updateCount;
    private Form<WMTSLayerImporterPage> form;

    public WMTSLayerImporterPage(PageParameters params) {

        storeId = params.get("storeId").toString();

        WMTSStoreInfo store = (WMTSStoreInfo) getCatalog().getStore(storeId, WMTSStoreInfo.class);

        // check if we have anything to import
        provider = new WMTSLayerProvider();
        provider.setStoreId(storeId);

        if (provider.size() <= 0) {
            error(
                    new ParamResourceModel(
                                    "storeEmpty",
                                    this,
                                    store.getName(),
                                    store.getWorkspace().getName())
                            .getString());
        }

        // build the GUI
        form =
                new Form<WMTSLayerImporterPage>(
                        "form", new CompoundPropertyModel<WMTSLayerImporterPage>(this));
        form.setOutputMarkupId(true);
        add(form);
        layers =
                new GeoServerTablePanel<LayerResource>("layerChooser", provider, true) {

                    private static final long serialVersionUID = -5817898784100419973L;

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<LayerResource> itemModel,
                            Property<LayerResource> property) {
                        if (property == WMTSLayerProvider.NAME) {
                            return new Label(id, property.getModel(itemModel));
                        } else if (property == WMTSLayerProvider.STATUS) {
                            Fragment f = new Fragment(id, "labelIcon", WMTSLayerImporterPage.this);
                            f.add(new Image("icon", new IconModel(itemModel)));
                            f.add(new Label("label", new StatusModel(itemModel)));
                            return f;
                        } else if (property == WMTSLayerProvider.ACTION) {
                            final LayerResource resource = (LayerResource) itemModel.getObject();
                            final LayerStatus status = resource.getStatus();
                            if (status == LayerStatus.PUBLISHED
                                    || status == LayerStatus.NEWLY_PUBLISHED
                                    || status == LayerStatus.UPDATED) {
                                return resourceChooserLink(
                                        id,
                                        itemModel,
                                        new ParamResourceModel("NewLayerPage.publishAgain", this));
                            } else {
                                return resourceChooserLink(
                                        id,
                                        itemModel,
                                        new ParamResourceModel("NewLayerPage.publish", this));
                            }
                        }

                        return null;
                    }
                };
        layers.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
        layers.setFilterable(true);

        form.add(layers);

        AjaxSubmitLink submitLink = submitLink();
        form.add(submitLink);
        form.add(importAllLink());
    }

    SimpleAjaxLink<LayerResource> resourceChooserLink(
            String id, IModel<LayerResource> itemModel, IModel<?> label) {
        return new SimpleAjaxLink<LayerResource>(id, itemModel, label) {

            private static final long serialVersionUID = 163167608296661157L;

            @Override
            protected void onClick(AjaxRequestTarget target) {
                LayerResource resource = (LayerResource) getDefaultModelObject();
                setResponsePage(new ResourceConfigurationPage(buildLayerInfo(resource), true));
            }
        };
    }

    LayerInfo buildLayerInfo(LayerResource resource) {
        Catalog catalog = getCatalog();
        StoreInfo store = catalog.getStore(storeId, StoreInfo.class);

        try {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(store);
            WMTSLayerInfo wli = builder.buildWMTSLayer(resource.getLocalName());
            return builder.buildLayer(wli);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error occurred while building the resources for the configuration page", e);
        }
    }

    AjaxSubmitLink submitLink() {
        return new AjaxSubmitLink("import") {

            private static final long serialVersionUID = -7161320029912723242L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    // grab the selection
                    List<LayerResource> selection = layers.getSelection();

                    // if nothing was selected we need to go back
                    if (selection.size() == 0) {
                        error(
                                new ParamResourceModel("selectionEmpty", WMTSLayerImporterPage.this)
                                        .getString());
                    } else {
                        publishLayers(selection);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                }
                target.add(form);
                addFeedbackPanels(target);
            }
        };
    }

    void publishLayers(List<LayerResource> selection) {
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);

        WMTSStoreInfo store = getCatalog().getStore(storeId, WMTSStoreInfo.class);
        builder.setStore(store);

        importCount = 0;
        errorCount = 0;
        updateCount = 0;
        for (LayerResource resource : selection) {
            publishLayer(resource, builder, store, catalog);
        }

        createImportReport();
        layers.reset();
        provider.updateLayerOrder();
    }

    AjaxSubmitLink importAllLink() {
        return new AjaxSubmitLink("importAll") {

            private static final long serialVersionUID = 7089389540839181808L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {

                    publishLayers(provider.getItems());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                }
                target.add(form);
                addFeedbackPanels(target);
            }
        };
    }

    private void publishLayer(
            LayerResource layer, CatalogBuilder builder, WMTSStoreInfo store, Catalog catalog) {

        WMTSLayerInfo wli;
        LayerInfo li;
        try {
            wli = builder.buildWMTSLayer(layer.getLocalName());
            li = builder.buildLayer(wli);
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error building WMTS cascading layer " + layer.getLocalName(),
                    e);
            layer.setStatus(LayerStatus.ERROR);
            layer.setError(e.getMessage());
            errorCount++;
            return;
        }

        WMTSLayerInfo exists = catalog.getResourceByStore(store, li.getName(), WMTSLayerInfo.class);

        if (exists != null) {
            // TODO what to do if layer already exists?
            builder.updateWMTSLayer(exists, wli);
            layer.setStatus(LayerStatus.UPDATED);
            updateCount++;
        } else {
            try {
                catalog.add(wli);
                catalog.add(li);
                layer.setStatus(LayerStatus.NEWLY_PUBLISHED);
                importCount++;
            } catch (Exception e) {
                catalog.remove(wli);
                LOGGER.log(
                        Level.WARNING,
                        "Error auto configuring WMTS cascading layer " + li.getName(),
                        e);
                layer.setStatus(LayerStatus.ERROR);
                layer.setError(e.getMessage());
                errorCount++;
            }
        }
    }

    private void createImportReport() {
        if (importCount > 0) {
            info("Succesfully imported " + importCount + " layers");
        } else {
            info("No new layers were imported");
        }

        if (updateCount > 0) {
            info("Updated " + updateCount + " layers");
        }

        if (errorCount > 0) {
            error(
                    "Unable to import "
                            + errorCount
                            + " layers, you may want to import them manually");
        }
    }

    final class StatusModel implements IModel<String> {

        private static final long serialVersionUID = 7754149365712750847L;
        IModel<LayerResource> layerResource;

        public StatusModel(IModel<LayerResource> layerResource) {
            super();
            this.layerResource = layerResource;
        }

        public String getObject() {
            LayerResource resource = (LayerResource) layerResource.getObject();
            return new ParamResourceModel(
                            "WMTSLayerImporterPage.status." + resource.getStatus(),
                            WMTSLayerImporterPage.this,
                            resource.getError())
                    .getString();
        }

        public void setObject(String object) {
            throw new UnsupportedOperationException();
        }

        public void detach() {
            // nothing to do
        }
    }

    final class IconModel implements IModel<PackageResourceReference> {

        private static final long serialVersionUID = 5762710251083186192L;
        IModel<LayerResource> layerResource;

        public IconModel(IModel<LayerResource> layerResource) {
            this.layerResource = layerResource;
        }

        public PackageResourceReference getObject() {
            LayerResource resource = (LayerResource) layerResource.getObject();
            if (resource.getStatus() == LayerStatus.ERROR) {
                return new PackageResourceReference(
                        GeoServerBasePage.class, "img/icons/silk/error.png");
            } else if (resource.getStatus() == LayerStatus.NEW) {
                return new PackageResourceReference(
                        GeoServerBasePage.class, "img/icons/silk/add.png");
            } else if (resource.getStatus() == LayerStatus.NEWLY_PUBLISHED) {
                return CatalogIconFactory.ENABLED_ICON;
            } else if (resource.getStatus() == LayerStatus.UPDATED) {
                return new PackageResourceReference(
                        GeoServerBasePage.class, "img/icons/silk/pencil.png");
            } else if (resource.getStatus() == LayerStatus.PUBLISHED) {
                return CatalogIconFactory.MAP_ICON;
            } else {
                return null;
            }
        }

        public void setObject(PackageResourceReference object) {
            throw new UnsupportedOperationException();
        }

        public void detach() {
            // nothing to do
        }
    }
}
