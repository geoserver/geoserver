/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.*;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.importer.WMSLayerImporterPage;
import org.geoserver.web.data.importer.WMTSLayerImporterPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.util.decorate.Wrapper;

/**
 * A page listing the resources contained in a store, and whose links will bring the user to a new
 * resource configuration page
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class NewLayerPage extends GeoServerSecuredPage {

    String storeId;
    private NewLayerPageProvider provider;
    private GeoServerTablePanel<Resource> layers;
    private WebMarkupContainer selectLayersContainer;
    private WebMarkupContainer selectLayers;
    private Label storeName;
    private WebMarkupContainer createTypeContainer;
    private WebMarkupContainer createSQLViewContainer;
    private WebMarkupContainer createCoverageViewContainer;
    private WebMarkupContainer createCascadedWFSStoredQueryContainer;
    private WebMarkupContainer createWMSLayerImportContainer;
    private WebMarkupContainer createWMTSLayerImportContainer;

    public NewLayerPage() {
        this(null);
    }

    public NewLayerPage(String storeId) {
        this.storeId = storeId;

        // the store selector, used when no store is initially known
        Form<?> selector = new Form<Void>("selector");
        selector.add(storesDropDown());
        selector.setVisible(storeId == null);
        add(selector);

        // the layer choosing block
        // visible when in any  way a store has been chosen
        selectLayersContainer = new WebMarkupContainer("selectLayersContainer");
        selectLayersContainer.setOutputMarkupId(true);
        add(selectLayersContainer);
        selectLayers = new WebMarkupContainer("selectLayers");
        selectLayers.setVisible(storeId != null);
        selectLayersContainer.add(selectLayers);

        selectLayers.add(storeName = new Label("storeName", new Model<String>()));
        if (storeId != null) {
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);
            storeName.setDefaultModelObject(store.getName());
        }

        provider = new NewLayerPageProvider();
        provider.setStoreId(storeId);
        provider.setShowPublished(true);
        layers =
                new GeoServerTablePanel<Resource>("layers", provider) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<Resource> itemModel, Property<Resource> property) {
                        if (property == NewLayerPageProvider.NAME) {
                            return new Label(id, property.getModel(itemModel));
                        } else if (property == NewLayerPageProvider.PUBLISHED) {
                            final Resource resource = itemModel.getObject();
                            final CatalogIconFactory icons = CatalogIconFactory.get();
                            if (resource.isPublished()) {
                                PackageResourceReference icon = icons.getEnabledIcon();
                                Fragment f = new Fragment(id, "iconFragment", selectLayers);
                                f.add(new Image("layerIcon", icon));
                                return f;
                            } else {
                                return new Label(id);
                            }
                        } else if (property == NewLayerPageProvider.ACTION) {
                            final Resource resource = itemModel.getObject();
                            if (resource.isPublished()) {
                                return resourceChooserLink(
                                        id,
                                        itemModel,
                                        new ParamResourceModel("publishAgain", this));
                            } else {
                                return resourceChooserLink(
                                        id, itemModel, new ParamResourceModel("publish", this));
                            }
                        } else {
                            throw new IllegalArgumentException(
                                    "Don't know of property " + property.getName());
                        }
                    }
                };
        layers.setFilterVisible(true);

        selectLayers.add(layers);

        createTypeContainer = new WebMarkupContainer("createTypeContainer");
        createTypeContainer.setVisible(false);
        createTypeContainer.add(newFeatureTypeLink());
        selectLayersContainer.add(createTypeContainer);

        createSQLViewContainer = new WebMarkupContainer("createSQLViewContainer");
        createSQLViewContainer.setVisible(false);
        createSQLViewContainer.add(newSQLViewLink());
        selectLayersContainer.add(createSQLViewContainer);

        createCoverageViewContainer = new WebMarkupContainer("createCoverageViewContainer");
        createCoverageViewContainer.setVisible(false);
        createCoverageViewContainer.add(newCoverageViewLink());
        selectLayersContainer.add(createCoverageViewContainer);

        createCascadedWFSStoredQueryContainer =
                new WebMarkupContainer("createCascadedWFSStoredQueryContainer");
        createCascadedWFSStoredQueryContainer.setVisible(false);
        createCascadedWFSStoredQueryContainer.add(newCascadedWFSStoredQueryLink());
        selectLayersContainer.add(createCascadedWFSStoredQueryContainer);

        createWMSLayerImportContainer = new WebMarkupContainer("createWMSLayerImportContainer");
        createWMSLayerImportContainer.setVisible(false);
        createWMSLayerImportContainer.add(newWMSImportLink());
        selectLayersContainer.add(createWMSLayerImportContainer);

        createWMTSLayerImportContainer = new WebMarkupContainer("createWMTSLayerImportContainer");
        createWMTSLayerImportContainer.setVisible(false);
        createWMTSLayerImportContainer.add(newWMTSImportLink());
        selectLayersContainer.add(createWMTSLayerImportContainer);

        // case where the store is selected, or we have just created new one
        if (storeId != null) {
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);
            updateSpecialFunctionPanels(store);
        }
    }

    Component newFeatureTypeLink() {
        return new AjaxLink<Void>("createFeatureType") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                DataStoreInfo ds = getCatalog().getStore(storeId, DataStoreInfo.class);
                PageParameters pp =
                        new PageParameters()
                                .add("wsName", ds.getWorkspace().getName())
                                .add("storeName", ds.getName());
                setResponsePage(NewFeatureTypePage.class, pp);
            }
        };
    }

    Component newSQLViewLink() {
        return new AjaxLink<Void>("createSQLView") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                DataStoreInfo ds = getCatalog().getStore(storeId, DataStoreInfo.class);
                PageParameters pp =
                        new PageParameters()
                                .add("wsName", ds.getWorkspace().getName())
                                .add("storeName", ds.getName());
                setResponsePage(SQLViewNewPage.class, pp);
            }
        };
    }

    Component newCoverageViewLink() {
        return new AjaxLink<Void>("createCoverageView") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                CoverageStoreInfo cs = getCatalog().getStore(storeId, CoverageStoreInfo.class);
                PageParameters pp =
                        new PageParameters()
                                .add("wsName", cs.getWorkspace().getName())
                                .add("storeName", cs.getName());
                setResponsePage(CoverageViewNewPage.class, pp);
            }
        };
    }

    Component newCascadedWFSStoredQueryLink() {
        return new AjaxLink<Void>("createCascadedWFSStoredQuery") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                DataStoreInfo ds = getCatalog().getStore(storeId, DataStoreInfo.class);
                PageParameters pp =
                        new PageParameters()
                                .add("wsName", ds.getWorkspace().getName())
                                .add("storeName", ds.getName());
                setResponsePage(CascadedWFSStoredQueryNewPage.class, pp);
            }
        };
    }

    Component newWMSImportLink() {
        return new AjaxLink<Void>("createWMSImport") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters pp = new PageParameters().add("storeId", storeId);
                setResponsePage(WMSLayerImporterPage.class, pp);
            }
        };
    }

    Component newWMTSImportLink() {
        return new AjaxLink<Void>("createWMTSImport") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters pp = new PageParameters().add("storeId", storeId);
                setResponsePage(WMTSLayerImporterPage.class, pp);
            }
        };
    }

    private Select2DropDownChoice<StoreInfo> storesDropDown() {
        final Select2DropDownChoice<StoreInfo> stores =
                new Select2DropDownChoice<>(
                        "storesDropDown",
                        new Model<>(),
                        new StoreListModel(),
                        new StoreListChoiceRenderer());
        stores.setOutputMarkupId(true);
        stores.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (stores.getModelObject() != null) {
                            StoreInfo store = (StoreInfo) stores.getModelObject();
                            NewLayerPage.this.storeId = store.getId();
                            provider.setStoreId(store.getId());
                            storeName.setDefaultModelObject(store.getName());
                            selectLayers.setVisible(true);

                            // make sure we can actually list the contents, it may happen
                            // the store is actually unreachable, in that case we
                            // want to display an error message
                            try {
                                provider.getItems();
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.SEVERE,
                                        "Error retrieving layers for the specified store",
                                        e);
                                error(e.getMessage());
                                selectLayers.setVisible(false);
                            }

                            updateSpecialFunctionPanels(store);

                        } else {
                            selectLayers.setVisible(false);
                            createTypeContainer.setVisible(false);
                        }
                        target.add(selectLayersContainer);
                        addFeedbackPanels(target);
                    }
                });
        return stores;
    }

    SimpleAjaxLink<Resource> resourceChooserLink(
            String id, IModel<Resource> itemModel, IModel<String> label) {
        return new SimpleAjaxLink<Resource>(id, itemModel, label) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                Resource resource = (Resource) getDefaultModelObject();
                setResponsePage(new ResourceConfigurationPage(buildLayerInfo(resource), true));
            }
        };
    }

    void updateSpecialFunctionPanels(StoreInfo store) {
        // at the moment just assume every store can create types
        try {
            createTypeContainer.setVisible(
                    store instanceof DataStoreInfo
                            && ((DataStoreInfo) store).getDataStore(null) instanceof DataStore);
        } catch (IOException e) {
            LOGGER.log(Level.FINEST, e.getMessage());
        }

        // reset to default first, to avoid the container being displayed if store is not a
        // DataStoreInfo
        createSQLViewContainer.setVisible(false);
        createCascadedWFSStoredQueryContainer.setVisible(false);
        if (store instanceof DataStoreInfo) {
            try {
                DataAccess<?, ?> da = ((DataStoreInfo) store).getDataStore(null);

                if (da instanceof Wrapper) {
                    try {
                        da = ((Wrapper) da).unwrap(DataAccess.class);
                    } catch (IllegalArgumentException e) {
                        throw new IOException(e);
                    }
                }

                createSQLViewContainer.setVisible(da instanceof JDBCDataStore);

                if (da instanceof WFSDataStore) {
                    createCascadedWFSStoredQueryContainer.setVisible(
                            ((WFSDataStore) da).supportsStoredQueries());
                }
            } catch (IOException e) {
                LOGGER.log(Level.FINEST, e.getMessage());
            }
        }
        createCoverageViewContainer.setVisible(false);
        if (store instanceof CoverageStoreInfo) {
            createCoverageViewContainer.setVisible(true);
        }

        // reset to default first, to avoid the container being displayed if store is not a
        // reset to default first, to avoid the container being displayed if store is not a
        // WMSStoreInfo
        createWMSLayerImportContainer.setVisible(false);
        // WMSStoreInfo
        createWMTSLayerImportContainer.setVisible(false);
        if (store instanceof WMTSStoreInfo) {
            try {
                WebMapTileServer wmts = ((WMTSStoreInfo) store).getWebMapTileServer(null);
                createWMTSLayerImportContainer.setVisible(wmts != null);
            } catch (IOException e) {
                LOGGER.log(Level.FINEST, e.getMessage());
            }
        } else if (store instanceof WMSStoreInfo) {
            try {
                WebMapServer wms = ((WMSStoreInfo) store).getWebMapServer(null);
                createWMSLayerImportContainer.setVisible(wms != null);
            } catch (IOException e) {
                LOGGER.log(Level.FINEST, e.getMessage());
            }
        }
    }

    /** Turns a resource name into a full {@link ResourceInfo} */
    LayerInfo buildLayerInfo(Resource resource) {
        Catalog catalog = getCatalog();
        StoreInfo store = catalog.getStore(getSelectedStoreId(), StoreInfo.class);
        StoreInfo expandedStore = null;

        if (store instanceof DataStoreInfo) {
            DataStoreInfo dstore = (DataStoreInfo) store;
            expandedStore = getCatalog().getResourcePool().clone(dstore, true);
        } else if (store instanceof CoverageStoreInfo) {
            CoverageStoreInfo cstore = (CoverageStoreInfo) store;
            expandedStore = getCatalog().getResourcePool().clone(cstore, true);
        } else if (store instanceof WMSStoreInfo) {
            WMSStoreInfo wmsInfo = (WMSStoreInfo) store;
            expandedStore = getCatalog().getResourcePool().clone(wmsInfo, true);
        } else if (store instanceof WMTSStoreInfo) {
            WMTSStoreInfo wmsInfo = (WMTSStoreInfo) store;
            expandedStore = getCatalog().getResourcePool().clone(wmsInfo, true);
        }

        // try to build from coverage store or data store
        try {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(expandedStore);
            if (expandedStore instanceof CoverageStoreInfo) {
                CoverageInfo ci = builder.buildCoverage(resource.getName().getLocalPart());
                return builder.buildLayer(ci);
            } else if (expandedStore instanceof DataStoreInfo) {
                FeatureTypeInfo fti = builder.buildFeatureType(resource.getName());
                return builder.buildLayer(fti);
            } else if (expandedStore instanceof WMTSStoreInfo) {
                WMTSLayerInfo wli = builder.buildWMTSLayer(resource.getLocalName());
                return builder.buildLayer(wli);
            } else if (expandedStore instanceof WMSStoreInfo) {
                WMSLayerInfo wli = builder.buildWMSLayer(resource.getLocalName());
                return builder.buildLayer(wli);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error occurred while building the resources for the configuration page", e);
        }

        // handle the case in which the store was not found anymore, or was not
        // of the expected type
        if (expandedStore == null)
            throw new IllegalArgumentException("Store is missing from configuration!");
        else
            throw new IllegalArgumentException(
                    "Don't know how to deal with this store " + expandedStore);
    }

    /**
     * Returns the storeId provided during construction, or the one pointed by the drop down if none
     * was provided during construction
     */
    String getSelectedStoreId() {
        // the provider is always up to date
        return provider.getStoreId();
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
