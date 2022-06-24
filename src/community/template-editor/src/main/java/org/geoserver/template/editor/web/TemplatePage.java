/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template.editor.web;

import static org.geoserver.template.editor.web.LayerProvider.NAME;
import static org.geoserver.template.editor.web.LayerProvider.STORE;
import static org.geoserver.template.editor.web.LayerProvider.TYPE;
import static org.geoserver.template.editor.web.LayerProvider.WORKSPACE;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/**
 * Page listing layers, datastores and workspaces. Allows to view/set the associated templates
 * (FreeMarker templates for getFeatureInfo HTML rendering)
 */
public class TemplatePage extends GeoServerSecuredPage {
    LayerProvider provider = new LayerProvider();
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public TemplatePage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table =
                new GeoServerTablePanel<LayerInfo>("table", provider, false) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<LayerInfo> itemModel, Property<LayerInfo> property) {
                        if (property == TYPE) {
                            Fragment f = new Fragment(id, "iconFragment", TemplatePage.this);
                            f.add(
                                    new Image(
                                            "layerIcon",
                                            icons.getSpecificLayerIcon(itemModel.getObject())));
                            return f;
                        } else if (property == WORKSPACE) {
                            return workspaceLink(id, itemModel);
                        } else if (property == STORE) {
                            return storeLink(id, itemModel);
                        } else if (property == NAME) {
                            return layerLink(id, itemModel);
                        }
                        throw new IllegalArgumentException(
                                "Don't know a property named "
                                        + property.getName()
                                        + ". Expected property to be either '"
                                        + TYPE
                                        + "', '"
                                        + WORKSPACE
                                        + "', '"
                                        + STORE
                                        + "' or '"
                                        + NAME
                                        + "'.");
                    }
                };
        table.setOutputMarkupId(true);
        add(table);
    }

    private Component layerLink(String id, final IModel<LayerInfo> model) {
        @SuppressWarnings("unchecked")
        IModel<String> layerNameModel = (IModel<String>) NAME.getModel(model);
        String wsName = model.getObject().getResource().getStore().getWorkspace().getName();
        String layerName = layerNameModel.getObject();
        @SuppressWarnings("unchecked")
        IModel<String> storeModel = (IModel<String>) STORE.getModel(model);
        String storeName = storeModel.getObject();
        return new SimpleBookmarkableLink(
                id,
                LayerTemplateEditorPage.class,
                layerNameModel,
                ResourceConfigurationPage.NAME,
                layerName,
                DataAccessEditPage.STORE_NAME,
                storeName,
                ResourceConfigurationPage.WORKSPACE,
                wsName);
    }

    private Component storeLink(String id, final IModel<LayerInfo> model) {
        String wsName = model.getObject().getResource().getStore().getWorkspace().getName();
        @SuppressWarnings("unchecked")
        IModel<String> storeModel = (IModel<String>) STORE.getModel(model);
        String storeName = storeModel.getObject();
        // LayerInfo layer = model.getObject();
        // StoreInfo store = layer.getResource().getStore();
        return new SimpleBookmarkableLink(
                id,
                StoreTemplateEditorPage.class,
                storeModel,
                DataAccessEditPage.STORE_NAME,
                storeName,
                DataAccessEditPage.WS_NAME,
                wsName);
    }

    private Component workspaceLink(String id, final IModel<LayerInfo> model) {
        @SuppressWarnings("unchecked")
        IModel<String> nameModel = (IModel<String>) WORKSPACE.getModel(model);
        return new SimpleBookmarkableLink(
                id,
                WorkspaceTemplateEditorPage.class,
                nameModel,
                DataAccessEditPage.WS_NAME,
                nameModel.getObject());
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
