/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.web.data.layer.LayerProvider.CREATED_TIMESTAMP;
import static org.geoserver.web.data.layer.LayerProvider.ENABLED;
import static org.geoserver.web.data.layer.LayerProvider.MODIFIED_BY;
import static org.geoserver.web.data.layer.LayerProvider.MODIFIED_TIMESTAMP;
import static org.geoserver.web.data.layer.LayerProvider.NAME;
import static org.geoserver.web.data.layer.LayerProvider.SRS;
import static org.geoserver.web.data.layer.LayerProvider.STORE;
import static org.geoserver.web.data.layer.LayerProvider.TITLE;
import static org.geoserver.web.data.layer.LayerProvider.TYPE;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
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
import org.geotools.api.filter.Filter;

/**
 * Page listing all the available layers. Follows the usual filter/sort/page approach, provides ways to bulk delete
 * layers and to add new ones
 */
public class LayerPage extends GeoServerSecuredPage {

    private String targetWorkspaceStr = null;
    private String targetLayerStr = null;
    private String targetGroupStr = null;

    LayerProvider provider = new LayerProvider() {
        @Override
        protected Filter getContextFilter() {

            String targetLayerOrGroup = targetGroupStr != null ? targetGroupStr : targetLayerStr;

            if (targetLayerOrGroup != null) {
                String targetLayer;
                if (targetWorkspaceStr != null) {
                    targetLayer = targetWorkspaceStr + ":" + targetLayerOrGroup;
                } else {
                    targetLayer = targetLayerOrGroup;
                }

                LayerGroupInfo gi = getCatalog().getLayerGroupByName(targetLayer);
                if (gi != null) {
                    LayerGroupHelper helper = new LayerGroupHelper(gi);
                    List<String> ids = new ArrayList<>();
                    for (LayerInfo li : helper.allLayers()) {
                        ids.add(li.getId());
                    }
                    return ids.isEmpty() ? Filter.EXCLUDE : Predicates.in("id", ids);
                }

                LayerInfo li = getCatalog().getLayerByName(targetLayer);
                if (li != null) {
                    return Predicates.equal("id", li.getId());
                }
                return Filter.EXCLUDE;
            }

            if (targetWorkspaceStr != null) {
                return Predicates.equal("resource.store.workspace.name", targetWorkspaceStr);
            }
            return null;
        }
    };
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerPage(PageParameters parameters) {
        StringValue wsParam = parameters.get("workspace");
        StringValue layerParam = parameters.get("layer");
        StringValue groupParam = parameters.get("group");

        if (!wsParam.isEmpty()) {
            this.targetWorkspaceStr = wsParam.toString();
        }
        if (!layerParam.isEmpty()) {
            this.targetLayerStr = layerParam.toString();
        }
        if (!groupParam.isEmpty()) {
            this.targetGroupStr = groupParam.toString();
        }
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table = new GeoServerTablePanel<>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<LayerInfo> itemModel, Property<LayerInfo> property) {
                if (property == TYPE) {
                    Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                    f.add(icons.getIcon("layerIcon", icons.getSpecificLayerIcon(itemModel.getObject())));
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
                    String icon = enabled ? icons.getEnabledIcon() : icons.getDisabledIcon();
                    Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                    f.add(icons.getIcon("layerIcon", icon));
                    return f;
                } else if (property == SRS) {
                    return new Label(id, SRS.getModel(itemModel));
                } else if (property == TITLE) {
                    return titleLink(id, itemModel);
                } else if (property == MODIFIED_TIMESTAMP) {
                    return new DateTimeLabel(id, MODIFIED_TIMESTAMP.getModel(itemModel));
                } else if (property == CREATED_TIMESTAMP) {
                    return new DateTimeLabel(id, CREATED_TIMESTAMP.getModel(itemModel));
                } else if (property == MODIFIED_BY) {
                    return new Label(id, MODIFIED_BY.getModel(itemModel));
                }
                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(!table.getSelection().isEmpty());
                target.add(removal);
            }
        };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    public LayerPage() {
        this(new PageParameters());
    }

    private Component titleLink(String id, IModel<LayerInfo> itemModel) {

        @SuppressWarnings("unchecked")
        IModel<String> layerNameModel = (IModel<String>) NAME.getModel(itemModel);
        @SuppressWarnings("unchecked")
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
                ResourceConfigurationPage.LAYER,
                layerName,
                ResourceConfigurationPage.WORKSPACE,
                wsName);
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<>("addNew", NewLayerPage.class));

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
                ResourceConfigurationPage.LAYER,
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
