/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.api.filter.Filter;

/**
 * Page listing all the available stores. Follows the usual filter/sort/page approach, provides ways to bulk delete
 * stores and to add new ones
 *
 * @see StorePanel
 */
@SuppressWarnings("serial")
public class StorePage extends GeoServerSecuredPage {

    private String targetWorkspaceStr = null;
    private String targetLayerStr = null;

    StoreProvider provider = new StoreProvider() {
        @Override
        protected Filter getContextFilter() {
            // Optional layer constraint: Stores which back the given LayerInfo/LayerGroupInfo.
            if (targetLayerStr != null && !targetLayerStr.isEmpty()) {
                Set<String> layerStoreIds = new LinkedHashSet<>();

                // First try a named layer, then a layer group (the UI parameter is called "layer" in both cases).
                LayerInfo layer = getCatalog().getLayerByName(targetLayerStr);
                if (layer != null) {
                    if (layer.getResource() != null) {
                        StoreInfo store = layer.getResource().getStore();
                        if (store != null && store.getId() != null) {
                            layerStoreIds.add(store.getId());
                        }
                    }
                } else {
                    LayerGroupInfo layerGroup = getCatalog().getLayerGroupByName(targetLayerStr);
                    if (layerGroup != null) {
                        LayerGroupHelper helper = new LayerGroupHelper(layerGroup);
                        for (LayerInfo li : helper.allLayers()) {
                            if (li.getResource() != null && li.getResource().getStore() != null) {
                                StoreInfo store = li.getResource().getStore();
                                if (store.getId() != null) {
                                    layerStoreIds.add(store.getId());
                                }
                            }
                        }
                    }
                }

                if (layerStoreIds.isEmpty()) {
                    // No stores back the layer/group: yield an empty result set.
                    return Filter.EXCLUDE;
                }

                // Filter StoreInfo by the ids of the backing stores.
                List<String> storeIdList = new ArrayList<>(layerStoreIds);
                Filter layerStoreFilter = Predicates.in("id", storeIdList);
                if (targetWorkspaceStr != null && !targetWorkspaceStr.isEmpty()) {
                    return Predicates.and(Predicates.equal("workspace.name", targetWorkspaceStr), layerStoreFilter);
                }
                return layerStoreFilter;
            }

            // Optional workspace constraint only.
            if (targetWorkspaceStr != null && !targetWorkspaceStr.isEmpty()) {
                return Predicates.equal("workspace.name", targetWorkspaceStr);
            }

            return null;
        }
    };

    StorePanel table;

    SelectionRemovalLink removal;

    GeoServerDialog dialog;

    public StorePage(PageParameters parameters) {
        StringValue wsParam = parameters.get("workspace");
        if (!wsParam.isEmpty()) {
            this.targetWorkspaceStr = wsParam.toString();
        }
        StringValue layerParam = parameters.get("layer");
        if (!layerParam.isEmpty()) {
            this.targetLayerStr = layerParam.toString();
        }
        // the table, and wire up selection change
        table = new StorePanel("table", provider, true) {
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

    public StorePage() {
        this(new PageParameters());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button - forward workspace param so NewDataPage can propagate it to sub-pages
        String ws = getPageParameters().get("workspace").toOptionalString();
        PageParameters newDataParams = new PageParameters();
        if (ws != null && !ws.isEmpty()) newDataParams.add("workspace", ws);
        header.add(new BookmarkablePageLink<>("addNew", NewDataPage.class, newDataParams));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
