/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.elasticsearch;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.feature.type.Name;
import org.geotools.data.elasticsearch.ElasticAttribute;
import org.geotools.data.elasticsearch.ElasticDataStore;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;

/**
 * Resource configuration panel to show a link to open Elasticsearch attribute modal dialog <br>
 * If the Elasticsearch attribute are not configured for current layer, the modal dialog will be
 * open at first resource configuration window opening <br>
 * After modal dialog is closed the resource page is reloaded and feature configuration table
 * updated
 */
@SuppressWarnings("WeakerAccess")
public class ElasticConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 3382530429105288433L;

    private LayerInfo _layerInfo;

    private ElasticLayerConfiguration _layerConfig;

    protected ModalWindow modal;

    /**
     * Adds Elasticsearch configuration panel link, configure modal dialog and implements modal
     * callback.
     *
     * @see ElasticConfigurationPage#done
     */
    public ElasticConfigurationPanel(final String panelId, final IModel<?> model) {
        super(panelId, model);
        final FeatureTypeInfo fti = (FeatureTypeInfo) model.getObject();

        modal = new ModalWindow("modal");
        modal.setInitialWidth(800);
        modal.setTitle(new ParamResourceModel("modalTitle", this));

        if (fti.getMetadata().get(ElasticLayerConfiguration.KEY) == null) {
            modal.add(new OpenWindowOnLoadBehavior());
        }

        modal.setContent(getElasticConfigurationPage(modal.getContentId(), model, modal, false));
        add(modal);

        AjaxLink<?> findLink =
                new AjaxLink("edit") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        modal.show(target);
                    }
                };
        final Fragment attributePanel = new Fragment("esPanel", "esPanelFragment", this);
        attributePanel.setOutputMarkupId(true);
        add(attributePanel);
        attributePanel.add(findLink);
    }

    protected ElasticConfigurationPage getElasticConfigurationPage(
            final String panelId,
            final IModel<?> model,
            final ModalWindow modal,
            boolean isRefresh) {
        modal.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        Optional<ListView> listView = getEditTabPanel(modal);
                        if (listView.isPresent()) {
                            Optional<WebMarkupContainer> attributePanel2 =
                                    getAttributePanel(listView.get());
                            if (attributePanel2.isPresent()) {
                                WebMarkupContainer attributePanel = attributePanel2.get();
                                GeoServerApplication app = (GeoServerApplication) getApplication();
                                FeatureTypeInfo ft = (FeatureTypeInfo) getResourceInfo();
                                app.getCatalog().getResourcePool().clear(ft);
                                app.getCatalog().getResourcePool().clear(ft.getStore());
                                attributePanel.modelChanged();
                                target.add(attributePanel);
                            } else {
                                LOGGER.log(
                                        Level.INFO,
                                        "Cannot refresh the attribute panel, cannot find the attributePanel component");
                            }
                        } else {
                            LOGGER.log(
                                    Level.INFO,
                                    "DataLayerEditTabPanel is not present, cannot refresh the attribute panel");
                        }

                        modal.close(target);
                    }
                });
        return new ElasticConfigurationPage(panelId, model, isRefresh) {
            @Override
            void done(
                    AjaxRequestTarget target,
                    LayerInfo layerInfo,
                    ElasticLayerConfiguration layerConfig) {
                _layerInfo = layerInfo;
                _layerConfig = layerConfig;
                modal.close(target);
            }

            @Override
            void refresh(AjaxRequestTarget target) {
                Component elasticConfigurationPage =
                        getElasticConfigurationPage(panelId, this.getDefaultModel(), modal, true)
                                .setOutputMarkupId(true);
                elasticConfigurationPage.setEnabled(true);
                modal.addOrReplace(elasticConfigurationPage);
                target.add(elasticConfigurationPage);
            }
        };
    }

    private Optional<ListView> getEditTabPanel(ModalWindow modal) {
        Component c = modal;
        for (int i = 0; i < 5; i++) { // The parent should be within 5 levels
            c = c.getParent();
            if (ListView.class.isAssignableFrom(c.getClass())) {
                return Optional.of((ListView) c);
            }
        }
        return Optional.empty();
    }

    private Optional<WebMarkupContainer> getAttributePanel(ListView listView) {
        for (int i = 0; i < listView.size(); i++) {
            Component c = listView.get(String.valueOf(i));
            if (c.get("content") != null) {
                Component content = c.get("content");
                if (content.get("attributePanel") != null) {
                    return Optional.of((WebMarkupContainer) content.get("attributePanel"));
                }
            }
        }
        return Optional.empty();
    }

    /*
     * Open modal dialog on window load
     */
    private class OpenWindowOnLoadBehavior extends AbstractDefaultAjaxBehavior {
        @Override
        protected void respond(AjaxRequestTarget target) {
            ModalWindow window = (ModalWindow) getComponent();
            window.show(target);
        }

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            response.render(OnLoadHeaderItem.forScript(getCallbackScript().toString()));
        }
    }

    private void saveLayer(FeatureTypeInfo ft) throws IOException {
        GeoServerApplication app = (GeoServerApplication) getApplication();
        Catalog catalog = app.getCatalog();

        // String namespace = ft.getNamespace().getURI();
        // Name qualifiedName = new NameImpl(namespace, _layerInfo.getName());
        Name qualifiedName = ft.getQualifiedName();
        LayerInfo layerInfo = catalog.getLayerByName(qualifiedName);

        boolean isNew =
                ft.getId() == null
                        || app.getCatalog().getResource(ft.getId(), ResourceInfo.class) == null;

        FeatureTypeInfo typeInfo;
        if (layerInfo == null || isNew) {
            // New
            DataStoreInfo dsInfo;
            dsInfo = catalog.getStore(ft.getStore().getId(), DataStoreInfo.class);
            ElasticDataStore ds = (ElasticDataStore) dsInfo.getDataStore(null);
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(dsInfo);
            ElasticLayerConfiguration layerConfig;
            layerConfig = new ElasticLayerConfiguration(_layerConfig);
            layerConfig.setLayerName(_layerInfo.getName());
            layerConfig.getAttributes().clear();
            List<ElasticAttribute> attributes = _layerConfig.getAttributes();
            layerConfig.getAttributes().addAll(attributes);
            ds.setLayerConfiguration(layerConfig);

            FeatureTypeInfo _typeInfo = (FeatureTypeInfo) _layerInfo.getResource();
            typeInfo = builder.buildFeatureType(ds.getFeatureSource(qualifiedName));
            typeInfo.setName(_layerInfo.getName());
            typeInfo.setNativeName(_layerInfo.getName());
            typeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, layerConfig);
            typeInfo.setEnabled(_typeInfo.isEnabled());
            typeInfo.setAdvertised(_typeInfo.isAdvertised());
            typeInfo.setTitle(_typeInfo.getTitle());
            typeInfo.setDescription(_typeInfo.getDescription());
            typeInfo.setAbstract(_typeInfo.getAbstract());
            typeInfo.getKeywords().addAll(_typeInfo.getKeywords());
            typeInfo.getMetadataLinks().addAll(_typeInfo.getMetadataLinks());
            typeInfo.getDataLinks().addAll(_typeInfo.getDataLinks());
            typeInfo.setSRS(_typeInfo.getSRS());
            typeInfo.setProjectionPolicy(_typeInfo.getProjectionPolicy());
            typeInfo.setNativeBoundingBox(_typeInfo.getNativeBoundingBox());
            typeInfo.setLatLonBoundingBox(_typeInfo.getLatLonBoundingBox());
            typeInfo.setCircularArcPresent(_typeInfo.isCircularArcPresent());
            typeInfo.setLinearizationTolerance(_typeInfo.getLinearizationTolerance());

            layerInfo = builder.buildLayer(typeInfo);
            builder.updateLayer(layerInfo, _layerInfo);
            layerInfo.setName(_layerInfo.getName());
            layerInfo.setResource(typeInfo);
        } else {
            // Update
            typeInfo = (FeatureTypeInfo) layerInfo.getResource();
            if (_layerConfig != null) {
                typeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, _layerConfig);
            }
        }
    }

    @Override
    public void onSave() {
        FeatureTypeInfo fti = (FeatureTypeInfo) getDefaultModelObject();
        fti.setNativeName(fti.getName());
        MarkupContainer parent = this.getParent();
        while (!(parent == null || parent instanceof ResourceConfigurationPage)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            try {
                if (_layerConfig != null && _layerInfo != null) {
                    _layerConfig.setLayerName(fti.getName());
                    _layerInfo.setName(fti.getName());
                } else {
                    _layerConfig =
                            (ElasticLayerConfiguration)
                                    fti.getMetadata().get(ElasticLayerConfiguration.KEY);
                    _layerConfig.setLayerName(fti.getName());
                }
                saveLayer(fti);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                error(new ParamResourceModel("creationFailure", this, e).getString());
            }
            ((ResourceConfigurationPage) parent).updateResource(fti);
        }
    }
}
