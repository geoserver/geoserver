/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.web.data.layer.LayerProvider.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.WMSStoreEditPage;
import org.geoserver.web.data.store.WMTSStoreEditPage;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/**
 * Page listing all the available layers. Follows the usual filter/sort/page approach, provides ways
 * to bulk delete layers and to add new ones
 */
@SuppressWarnings("serial")
public class LayerPage extends GeoServerSecuredPage {
    LayerProvider provider = new LayerProvider();
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerPage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table =
                new GeoServerTablePanel<LayerInfo>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<LayerInfo> itemModel, Property<LayerInfo> property) {
                        if (property == TYPE) {
                            Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                            f.add(
                                    new Image(
                                            "layerIcon",
                                            icons.getSpecificLayerIcon(itemModel.getObject())));
                            return f;
                        } else if (property == STORE) {
                            return storeLink(id, itemModel);
                        } else if (property == NAME) {
                            return layerLink(id, itemModel);
                        } else if (property == ENABLED) {
                            LayerInfo layerInfo = itemModel.getObject();
                            // ask for enabled() instead of isEnabled() to account for disabled
                            // resource/store
                            boolean enabled = layerInfo.enabled();
                            PackageResourceReference icon =
                                    enabled ? icons.getEnabledIcon() : icons.getDisabledIcon();
                            Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                            f.add(new Image("layerIcon", icon));
                            return f;
                        } else if (property == SRS) {
                            return new Label(id, SRS.getModel(itemModel));
                        } else if (property == TITLE) {
                            return titleLink(id, itemModel);
                        } else if (property == MODIFIED_TIMESTAMP) {
                            return new DateTimeLabel(id, MODIFIED_TIMESTAMP.getModel(itemModel));
                        } else if (property == CREATED_TIMESTAMP) {
                            return new DateTimeLabel(id, CREATED_TIMESTAMP.getModel(itemModel));
                        }
                        throw new IllegalArgumentException(
                                "Don't know a property named " + property.getName());
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        removal.setEnabled(table.getSelection().size() > 0);
                        target.add(removal);
                    }
                };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    private Component titleLink(String id, IModel<LayerInfo> itemModel) {

        IModel<String> layerNameModel = (IModel<String>) NAME.getModel(itemModel);
        IModel<String> layerTitleModel = (IModel<String>) TITLE.getModel(itemModel);
        String layerTitle = layerTitleModel.getObject();
        String layerName = layerNameModel.getObject();
        String wsName = getWorkspaceNameFromLayerInfo(itemModel.getObject());

        IModel linkModel = layerTitleModel;
        if (StringUtils.isEmpty(layerTitle)) {
            linkModel = layerNameModel;
        }

        return new SimpleBookmarkableLink(
                id,
                ResourceConfigurationPage.class,
                linkModel,
                ResourceConfigurationPage.NAME,
                layerName,
                ResourceConfigurationPage.WORKSPACE,
                wsName);
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<Void>("addNew", NewLayerPage.class));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    private Component layerLink(String id, final IModel<LayerInfo> model) {
        @SuppressWarnings("unchecked")
        IModel<String> layerNameModel = (IModel<String>) NAME.getModel(model);
        String wsName = getWorkspaceNameFromLayerInfo(model.getObject());
        String layerName = layerNameModel.getObject();
        String linkTitle = wsName + ":" + layerName;
        return new SimpleBookmarkableLink(
                id,
                ResourceConfigurationPage.class,
                new Model<>(linkTitle),
                ResourceConfigurationPage.NAME,
                layerName,
                ResourceConfigurationPage.WORKSPACE,
                wsName);
    }

    private Component storeLink(String id, final IModel<LayerInfo> model) {
        @SuppressWarnings("unchecked")
        IModel<String> storeModel = (IModel<String>) STORE.getModel(model);
        String wsName = getWorkspaceNameFromLayerInfo(model.getObject());
        String storeName = storeModel.getObject();
        LayerInfo layer = model.getObject();
        StoreInfo store = layer.getResource().getStore();
        if (store instanceof DataStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    DataAccessEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof WMTSStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    WMTSStoreEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof WMSStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    WMSStoreEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else {
            return new SimpleBookmarkableLink(
                    id,
                    CoverageStoreEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        }
    }

    /**
     * Helper to grab the workspace name from the layer info
     *
     * @param li the li
     * @return the workspace name of the ws the layer belong
     */
    private String getWorkspaceNameFromLayerInfo(LayerInfo li) {
        return li.getResource().getStore().getWorkspace().getName();
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
