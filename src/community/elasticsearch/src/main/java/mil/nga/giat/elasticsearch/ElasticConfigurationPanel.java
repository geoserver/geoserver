/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package mil.nga.giat.elasticsearch;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

import mil.nga.giat.data.elasticsearch.ElasticAttribute;
import mil.nga.giat.data.elasticsearch.ElasticDataStore;
import mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
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
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * Resource configuration panel to show a link to open Elasticsearch attribute 
 * modal dialog <br> If the Elasticsearch attribute are not configured for 
 * current layer, the modal dialog will be open at first resource configuration 
 * window opening <br> After modal dialog is closed the resource page is 
 * reloaded and feature configuration table updated
 * 
 */
public class ElasticConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 3382530429105288433L;

    private LayerInfo _layerInfo;

    private ElasticLayerConfiguration _layerConfig;

    /**
     * Adds Elasticsearch configuration panel link, configure modal dialog and 
     * implements modal callback.
     * 
     * @see {@link ElasticConfigurationPage#done}
     */

    public ElasticConfigurationPanel(final String panelId, final IModel model) {
        super(panelId, model);
        final FeatureTypeInfo fti = (FeatureTypeInfo) model.getObject();

        final ModalWindow modal = new ModalWindow("modal");
        modal.setInitialWidth(800);
        modal.setTitle(new ParamResourceModel("modalTitle", ElasticConfigurationPanel.this));
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            @Override
            public void onClose(AjaxRequestTarget target) {
                if (_layerInfo != null) {
                    GeoServerApplication app = (GeoServerApplication) getApplication();
                    final FeatureTypeInfo ft = (FeatureTypeInfo) getResourceInfo();

                    app.getCatalog().getResourcePool().clear(ft);
                    app.getCatalog().getResourcePool().clear(ft.getStore());
                    setResponsePage(new ElasticResourceConfigurationPage(ft));
                }
            }
        });

        if (fti.getMetadata().get(ElasticLayerConfiguration.KEY) == null) {
            modal.add(new OpenWindowOnLoadBehavior());
        }

        modal.setContent(new ElasticConfigurationPage(panelId, model) {
            @Override
            void done(AjaxRequestTarget target, LayerInfo layerInfo, 
                    ElasticLayerConfiguration layerConfig) {
                _layerInfo = layerInfo;
                _layerConfig = layerConfig;
                modal.close(target);
            }
        });
        add(modal);

        AjaxLink findLink = new AjaxLink("edit") {
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
    
    private class ElasticResourceConfigurationPage extends ResourceConfigurationPage {
        
        private FeatureTypeInfo ft;
        
        public ElasticResourceConfigurationPage(FeatureTypeInfo ft) {
            super(_layerInfo, false);
            this.ft = ft;
        }
        
        @Override
        protected void doSave() {
            _layerInfo = (LayerInfo)getPublishedInfo();
            GeoServerApplication app = (GeoServerApplication) getApplication();
            Catalog catalog = app.getCatalog();
            
            //Override _isNew state, based on resource informations into catalog
            final boolean isNew;
            if (ft.getId() != null && catalog.getResource(ft.getId(),ResourceInfo.class) != null) {
                isNew = false;
            } else{
                isNew = true;
            }

            try {
                String namespace = ft.getNamespace().getURI();
                Name qualifiedName = new NameImpl(namespace, _layerInfo.getName());
                LayerInfo layerInfo = catalog.getLayerByName(qualifiedName);

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
                    //builder.updateFeatureType(typeInfo, (FeatureTypeInfo) _layerInfo.getResource());
                    typeInfo.setName(_layerInfo.getName());
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
                    typeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, _layerConfig);
                }
                
                this.updatePublishedInfo(layerInfo);
                this.updateResource(typeInfo);
                
                Field isNewfield = ResourceConfigurationPage.class.getDeclaredField("isNew");
                isNewfield.setAccessible(true);
                isNewfield.set(this, isNew);
                
                super.doSave();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error saving layer", e);
                e.printStackTrace();
                error(e.getMessage());
            }
        }
    }

}
