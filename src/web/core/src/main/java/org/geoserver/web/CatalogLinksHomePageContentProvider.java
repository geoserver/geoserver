/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.store.WMSStoreEditPage;
import org.geoserver.web.data.store.WMTSStoreEditPage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;

/** Provides feedback on layer enabled, visible status when home page displaying layer. */
public class CatalogLinksHomePageContentProvider implements GeoServerHomePageContentProvider {
    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return isAdmin;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new LinksPanel(id);
    }

    /** LinksPanel */
    static class LinksPanel extends Panel {
        private static final boolean isCssEmpty =
                IsWicketCssFileEmpty(CatalogLinksHomePageContentProvider.LinksPanel.class);

        int layerCount, groupCount, storesCount, wsCount;

        public LinksPanel(String id) {
            super(id);
            WebMarkupContainer catalogLinks = new WebMarkupContainer("catalogLinks");
            catalogLinks.add(catalogLinks("catalogLink"));

            add(catalogLinks);
        }

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), toResourceName(getClass(), "css"))));
            }
        }

        ListView<BookmarkablePageLink> catalogLinks(String id) {

            LoadableDetachableModel<List<BookmarkablePageLink>> links = new LoadableDetachableModel<>() {
                @Override
                protected List<BookmarkablePageLink> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) LinksPanel.this.getPage();
                    return generateCatalogLinks(homePage);
                }
            };
            return new ListView<>(id, links) {
                @Override
                protected void populateItem(ListItem<BookmarkablePageLink> item) {
                    BookmarkablePageLink link = item.getModelObject();
                    item.add(link);
                }
            };
        }

        /**
         * Generate catalog links for administrator.
         *
         * @param homePage
         * @return list of catalog links for administrator.
         */
        List<BookmarkablePageLink> generateCatalogLinks(GeoServerHomePage homePage) {
            catalogCounts(homePage);
            PublishedInfo publishedInfo = homePage.getPublishedInfo();
            WorkspaceInfo workspaceInfo = homePage.getWorkspaceInfo();

            List<BookmarkablePageLink> pageLinks = new ArrayList<>();

            Localizer localizer = getLocalizer();

            // LAYERS
            if (layerCount == 0 && storesCount > 0) {
                BookmarkablePageLink addLayer = new BookmarkablePageLink<>("link", NewLayerPage.class);
                addLayer.add(new Label("title", localizer.getString("addLayers", this)));
                pageLinks.add(addLayer);
            } else if (layerCount == 1 && publishedInfo instanceof LayerInfo) {
                PageParameters editLayersParams = new PageParameters()
                        .add(ResourceConfigurationPage.LAYER, publishedInfo.getName())
                        .add(
                                ResourceConfigurationPage.WORKSPACE,
                                ((LayerInfo) publishedInfo)
                                        .getResource()
                                        .getStore()
                                        .getWorkspace()
                                        .getName());
                BookmarkablePageLink layerEdit =
                        new BookmarkablePageLink<>("link", ResourceConfigurationPage.class, editLayersParams);
                layerEdit.add(new Label("title", localizer.getString("layersEdit", this)));
                pageLinks.add(layerEdit);
            }
            if (layerCount > 1
                    || (layerCount == 1 && (publishedInfo == null || publishedInfo instanceof LayerGroupInfo))) {
                BookmarkablePageLink layersLink =
                        new BookmarkablePageLink<>("link", LayerPage.class, homePage.getPageParameters());
                layersLink.add(
                        new Label("title", new StringResourceModel("layersCount", this).setParameters(layerCount)));
                pageLinks.add(layersLink);
            }

            // GROUPS
            if (groupCount == 0 && layerCount > 0 && publishedInfo == null) {
                BookmarkablePageLink addGroup = new BookmarkablePageLink<>("link", LayerGroupEditPage.class);
                addGroup.add(new Label("title", localizer.getString("addGroups", this)));
                addGroup.setEnabled(publishedInfo == null);
                pageLinks.add(addGroup);
            } else if (publishedInfo instanceof LayerGroupInfo) {
                PageParameters editGroupParams = new PageParameters();

                editGroupParams.add(LayerGroupEditPage.GROUP, publishedInfo.getName());
                if (workspaceInfo != null) {
                    editGroupParams.add(LayerGroupEditPage.WORKSPACE, workspaceInfo.getName());
                }
                BookmarkablePageLink groupEdit =
                        new BookmarkablePageLink<>("link", LayerGroupEditPage.class, editGroupParams);
                groupEdit.add(new Label("title", localizer.getString("groupsEdit", this)));

                pageLinks.add(groupEdit);
            }

            if (groupCount > 1 || (groupCount == 1 && publishedInfo == null)) {
                BookmarkablePageLink groupsLink =
                        new BookmarkablePageLink<>("link", LayerGroupPage.class, homePage.getPageParameters());
                groupsLink.add(
                        new Label("title", new StringResourceModel("groupsCount", this).setParameters(groupCount)));
                pageLinks.add(groupsLink);
            }

            // STORES
            if (!(publishedInfo instanceof LayerGroupInfo)) {
                // we are not interested in adding / editing a store for a layer group
                if (storesCount == 0 && wsCount > 0) {
                    PageParameters storeParams = new PageParameters();
                    if (workspaceInfo != null) storeParams.add("workspace", workspaceInfo.getName());
                    BookmarkablePageLink addStore = new BookmarkablePageLink<>("link", NewDataPage.class, storeParams);
                    addStore.add(new Label("title", localizer.getString("addStores", homePage)));
                    pageLinks.add(addStore);
                } else if (publishedInfo != null && publishedInfo instanceof LayerInfo) {
                    LayerInfo layerInfo = (LayerInfo) publishedInfo;
                    StoreInfo store = layerInfo.getResource().getStore();

                    PageParameters storeParams = new PageParameters();
                    storeParams.add(DataAccessEditPage.STORE_NAME, store.getName());
                    storeParams.add(
                            DataAccessEditPage.WS_NAME, store.getWorkspace().getName());

                    BookmarkablePageLink editStore;
                    // edit layer if we are showing a layer info
                    if (store instanceof DataStoreInfo) {
                        editStore = new BookmarkablePageLink<>("link", DataAccessEditPage.class, storeParams);
                    } else if (store instanceof CoverageStoreInfo) {
                        editStore = new BookmarkablePageLink<>("link", CoverageStoreEditPage.class, storeParams);
                    } else if (store instanceof WMSStoreInfo) {
                        editStore = new BookmarkablePageLink<>("link", WMSStoreEditPage.class, storeParams);
                    } else if (store instanceof WMTSStoreInfo) {
                        editStore = new BookmarkablePageLink<>("link", WMTSStoreEditPage.class, storeParams);
                    } else {
                        editStore = null; // skip unknown store type
                    }
                    if (editStore != null) {
                        editStore.add(new Label("title", localizer.getString("storeEdit", homePage)));
                        pageLinks.add(editStore);
                    }
                }
            }

            if (storesCount > 1
                    || (storesCount == 1 && (publishedInfo == null || publishedInfo instanceof LayerGroupInfo))) {
                BookmarkablePageLink storesLink =
                        new BookmarkablePageLink<>("link", StorePage.class, homePage.getPageParameters());
                storesLink.add(new Label(
                        "title", new StringResourceModel("storesCount", homePage).setParameters(storesCount)));
                pageLinks.add(storesLink);
            }

            // WORKSPACE
            if (wsCount == 0) {
                BookmarkablePageLink addWorkspace = new BookmarkablePageLink<>("link", WorkspaceNewPage.class);
                addWorkspace.add(new Label("title", new StringResourceModel("addWorkspaces", homePage)));
                pageLinks.add(addWorkspace);
            } else if (workspaceInfo != null) {
                PageParameters params = new PageParameters();
                params.add("workspace", workspaceInfo.getName());

                BookmarkablePageLink editWorkspace =
                        new BookmarkablePageLink<>("link", WorkspaceEditPage.class, params);
                editWorkspace.add(new Label("title", localizer.getString("workspaceEdit", homePage)));
                pageLinks.add(editWorkspace);
            } else if (wsCount > 1 || (wsCount == 1 && workspaceInfo == null)) {
                BookmarkablePageLink workspacesLink =
                        new BookmarkablePageLink<>("link", WorkspacePage.class, homePage.getPageParameters());
                workspacesLink.add(
                        new Label("title", new StringResourceModel("workspaceCount", homePage).setParameters(wsCount)));
                pageLinks.add(workspacesLink);
            }

            return pageLinks;
        }

        private void catalogCounts(GeoServerHomePage homePage) {
            Stopwatch sw = Stopwatch.createStarted();
            try {
                Catalog catalog = homePage.getCatalog();
                PublishedInfo publishedInfo = homePage.getPublishedInfo();
                WorkspaceInfo workspaceInfo = homePage.getWorkspaceInfo();

                layerCount = -1;
                groupCount = -1;
                storesCount = -1;
                wsCount = -1;

                if (publishedInfo != null) {
                    if (publishedInfo instanceof LayerInfo) {
                        layerCount = 1;
                        groupCount = 0;
                        storesCount = 1;
                        wsCount = 1;
                    } else if (publishedInfo instanceof LayerGroupInfo) {
                        LayerGroupInfo groupInfo = (LayerGroupInfo) publishedInfo;

                        LayerGroupHelper helper = new LayerGroupHelper(groupInfo);
                        Set<String> layerIds = new HashSet<>();
                        Set<String> groupIds = new HashSet<>();
                        Set<String> workspaceIds = new HashSet<>();
                        Set<String> storeIds = new HashSet<>();
                        for (LayerInfo li : helper.allLayers()) {
                            if (li.getId() != null) {
                                layerIds.add(li.getId());
                            }
                            if (li.getResource() != null && li.getResource().getStore() != null) {
                                StoreInfo store = li.getResource().getStore();
                                if (store.getId() != null) {
                                    storeIds.add(store.getId());
                                }
                                if (store.getWorkspace() != null
                                        && store.getWorkspace().getId() != null) {
                                    workspaceIds.add(store.getWorkspace().getId());
                                }
                            }
                        }
                        for (LayerGroupInfo gi : helper.allGroups()) {
                            if (gi.getId() != null) {
                                groupIds.add(gi.getId());
                            }
                        }
                        layerCount = layerIds.size();
                        storesCount = storeIds.size();
                        groupCount = groupIds.size();
                        wsCount = workspaceIds.size();
                    } else {
                        layerCount = 0;
                        groupCount = 0;
                        storesCount = 0;
                        wsCount = 0;
                    }
                } else if (workspaceInfo != null) {
                    layerCount = catalog.count(
                            LayerInfo.class, Predicates.equal("resource.namespace.prefix", workspaceInfo.getName()));
                    groupCount = catalog.count(
                            LayerGroupInfo.class, Predicates.equal("workspace.name", workspaceInfo.getName()));
                    storesCount =
                            catalog.count(StoreInfo.class, Predicates.equal("workspace.name", workspaceInfo.getName()));
                    wsCount = 1;
                } else {
                    layerCount = catalog.count(LayerInfo.class, acceptAll());
                    groupCount = catalog.count(LayerGroupInfo.class, acceptAll());
                    storesCount = catalog.count(StoreInfo.class, acceptAll());
                    wsCount = catalog.count(WorkspaceInfo.class, acceptAll());
                }
            } finally {
                sw.stop();
                if (GeoServerHomePage.LOGGER.isLoggable(Level.FINE)) {
                    GeoServerHomePage.LOGGER.fine("Admin summary of catalog contents took "
                            + sw.elapsed().toMillis() + " ms");
                }
            }
        }
    }
}
