/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geoserver.web.wicket.GsIcon;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.factory.CommonFactoryFinder;
import org.springframework.security.core.Authentication;

public class NavigationTreePanel extends Panel {

    @Serial
    private static final long serialVersionUID = 1L;

    private WebMarkupContainer workspacesScroll;
    private ListView<Workspace> workspacesList;
    private WebMarkupContainer globalSectionBody;
    private WebMarkupContainer globalSectionContainer;
    private WebMarkupContainer noDataMessage;
    private WebMarkupContainer globalChildrenContainer;
    private ListView<WorkspaceChild> globalChildrenList;
    private WebMarkupContainer globalPagination;

    private AbstractDefaultAjaxBehavior loadWorkspacesBehavior;

    private static final int PAGE_SIZE = 50;
    private static final int SEARCH_LIMIT = 250;
    private static final int MAX_QUERY_LENGTH = 100;
    private int globalPage = 1;
    private int globalPageSize = PAGE_SIZE;

    private final Map<String, Integer> layerPageByWorkspace = new HashMap<>();
    private final Map<String, Integer> layerPageSizeByWorkspace = new HashMap<>();

    private final Map<String, WorkspaceState> workspaceStates = new HashMap<>();
    private String selectedWorkspaceName;
    private String selectedLayerName;
    private String treeFilterQuery = "";
    private boolean selectionInitialized;
    private transient Set<String> cachedMatchingWorkspaces;
    private transient Integer cachedTotalGlobalItems;
    private transient Integer cachedTotalWorkspaceItems;

    private static final CssResourceReference CSS =
            new CssResourceReference(NavigationTreePanel.class, "NavigationTreePanel.css");
    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(NavigationTreePanel.class, "NavigationTreePanel.js");
    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    private static final SortBy ALPHABETICAL = FF.sort("name", SortOrder.ASCENDING);

    public NavigationTreePanel(String id) {
        super(id);

        add(new SearchInputPanel("myCustomSearch", false));

        WebMarkupContainer newMenu = new WebMarkupContainer("newMenu") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                Authentication auth = GeoServerSession.get().getAuthentication();
                setVisible(auth != null && auth.isAuthenticated());
            }
        };
        add(newMenu);
        List<SidebarNewMenuItemInfo> sidebarNewItems =
                new ArrayList<>(GeoServerApplication.get().getBeansOfType(SidebarNewMenuItemInfo.class));
        sidebarNewItems.removeIf(info -> info.getId() == null || !info.getId().startsWith("sidebarNew"));
        sidebarNewItems.sort(Comparator.comparingInt(SidebarNewMenuItemInfo::getOrder));
        newMenu.add(new ListView<>("newMenuItems", sidebarNewItems) {
            @Override
            protected void populateItem(ListItem<SidebarNewMenuItemInfo> item) {
                SidebarNewMenuItemInfo info = item.getModelObject();
                String titleKey = info.getTitleKey();

                PageParameters pageParams = getPage().getPageParameters();
                String workspaceParam = pageParams.get("workspace").toOptionalString();
                String layerParam = pageParams.get("layer").toOptionalString();
                String groupParam = pageParams.get("group").toOptionalString();
                String nameParam = pageParams.get("name").toOptionalString();

                PageParameters linkParams = null;
                if (info.includesContextParam("workspace") && !Strings.isEmpty(workspaceParam)) {
                    linkParams = new PageParameters();
                    linkParams.add("workspace", workspaceParam);
                }
                if (info.includesContextParam("layer") && !Strings.isEmpty(layerParam)) {
                    if (linkParams == null) linkParams = new PageParameters();
                    linkParams.add("layer", layerParam);
                }
                if (info.includesContextParam("group") && !Strings.isEmpty(groupParam)) {
                    if (linkParams == null) linkParams = new PageParameters();
                    linkParams.add("group", groupParam);
                }
                if (info.includesContextParam("name") && !Strings.isEmpty(nameParam)) {
                    if (linkParams == null) linkParams = new PageParameters();
                    linkParams.add("name", nameParam);
                }

                BookmarkablePageLink<Void> link = linkParams != null
                        ? new BookmarkablePageLink<>("link", info.getComponentClass(), linkParams)
                        : new BookmarkablePageLink<>("link", info.getComponentClass());
                Label label;
                if (titleKey != null && !titleKey.isEmpty()) {
                    label = new Label("label", new ResourceModel(titleKey, titleKey));
                } else {
                    label = new Label("label", "");
                }
                link.add(label);
                WebMarkupContainer ctxIndicator = new WebMarkupContainer("ctxIndicator");
                List<String> ctxParts = new ArrayList<>();
                if (!Strings.isEmpty(workspaceParam) && info.includesContextParam("workspace"))
                    ctxParts.add(workspaceParam);
                if (!Strings.isEmpty(layerParam) && info.includesContextParam("layer")) ctxParts.add(layerParam);
                if (!Strings.isEmpty(groupParam) && info.includesContextParam("group")) ctxParts.add(groupParam);
                if (!ctxParts.isEmpty()) {
                    ctxIndicator.add(AttributeModifier.replace("title", String.join(" > ", ctxParts)));
                    String ctxClass;
                    if (!Strings.isEmpty(groupParam) && info.includesContextParam("group")) {
                        ctxClass = "gs-ctx-indicator--group";
                    } else if (!Strings.isEmpty(layerParam) && info.includesContextParam("layer")) {
                        ctxClass = "gs-ctx-indicator--layer";
                    } else {
                        ctxClass = "gs-ctx-indicator--workspace";
                    }
                    ctxIndicator.add(AttributeModifier.append("class", ctxClass));
                } else {
                    ctxIndicator.setVisible(false);
                }
                link.add(ctxIndicator);
                item.add(link);
            }
        });

        globalSectionContainer = new WebMarkupContainer("globalSectionContainer") {
            @Override
            public boolean isVisible() {
                return getTotalWorkspaceItems() + getTotalGlobalItems() > 0;
            }
        };
        globalSectionContainer.setOutputMarkupPlaceholderTag(true);
        add(globalSectionContainer);

        globalSectionBody = new WebMarkupContainer("globalSectionBody");
        globalSectionBody.setOutputMarkupPlaceholderTag(true);
        globalSectionBody.setOutputMarkupId(true);
        globalSectionContainer.add(globalSectionBody);

        globalSectionContainer.add(new GsIcon("globalIcon", "gs-icon-server"));

        AjaxLink<Void> globalSectionSelect = new AjaxLink<>("globalSectionSelect") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToHome(null, null, null);
            }
        };
        globalSectionContainer.add(globalSectionSelect);
        globalSectionContainer.add(new Label("globalCount", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return NavigationTreePanel.this.formatCount(getTotalWorkspaceItems() + getTotalGlobalItems());
            }
        }));

        LoadableDetachableModel<List<WorkspaceChild>> globalChildrenModel = new LoadableDetachableModel<>() {
            @Override
            protected List<WorkspaceChild> load() {
                int totalWs = getTotalWorkspaceItems();
                int combinedOffset = (globalPage - 1) * globalPageSize;
                int combinedEnd = combinedOffset + globalPageSize;
                int globalOffset = Math.max(0, combinedOffset - totalWs);
                int globalCount = Math.max(0, combinedEnd - totalWs) - globalOffset;
                if (globalCount <= 0) return new ArrayList<>();
                return getGlobalChildrenPage(globalOffset, globalCount);
            }
        };
        globalChildrenList = new ListView<>("globalChildren", globalChildrenModel) {
            @Override
            protected void populateItem(ListItem<WorkspaceChild> item) {
                WorkspaceChild childInfo = item.getModelObject();
                item.add(childIcon("layerIcon", childInfo.type));
                AjaxLink<Void> layerSelect = new AjaxLink<>("layerSelect") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        navigateToHome(null, childInfo.name, childInfo.type);
                    }
                };
                layerSelect.add(AttributeModifier.replace("title", childInfo.name));
                if (selectedWorkspaceName == null && childInfo.name.equals(selectedLayerName)) {
                    item.add(AttributeAppender.append("class", "is-active"));
                }
                Label layerName = new Label("layerName", highlightTreeText(childInfo.name));
                layerName.setEscapeModelStrings(false);
                layerSelect.add(layerName);
                item.add(layerSelect);
            }
        };
        globalChildrenContainer = new WebMarkupContainer("globalChildrenContainer") {
            @Override
            public boolean isVisible() {
                return selectedWorkspaceName == null;
            }
        };
        globalChildrenContainer.setOutputMarkupPlaceholderTag(true);
        globalChildrenContainer.setOutputMarkupId(true);
        globalChildrenContainer.add(globalChildrenList);
        globalSectionBody.add(globalChildrenContainer);

        workspacesScroll = new WebMarkupContainer("workspacesScroll");
        workspacesScroll.setOutputMarkupId(true);
        globalSectionBody.add(workspacesScroll);

        globalPagination = new WebMarkupContainer("globalPagination") {
            @Override
            public boolean isVisible() {
                return selectedWorkspaceName == null && (getTotalWorkspaceItems() + getTotalGlobalItems()) > PAGE_SIZE;
            }
        };
        globalPagination.setOutputMarkupId(true);
        globalPagination.add(
                new org.apache.wicket.AttributeModifier("data-current-page", () -> String.valueOf(globalPage)));
        globalPagination.add(
                new org.apache.wicket.AttributeModifier("data-page-size", () -> String.valueOf(globalPageSize)));
        globalPagination.add(new org.apache.wicket.AttributeModifier(
                "data-total-items", () -> String.valueOf(getTotalWorkspaceItems() + getTotalGlobalItems())));
        globalPagination.add(new org.apache.wicket.AttributeModifier("data-total-pages", () -> {
            int total = getTotalWorkspaceItems() + getTotalGlobalItems();
            return String.valueOf((int) Math.ceil(total / (double) globalPageSize));
        }));
        globalSectionBody.add(globalPagination);

        noDataMessage = new WebMarkupContainer("noDataMessage") {
            @Override
            public boolean isVisible() {
                return getTotalGlobalItems() == 0 && getTotalWorkspaceItems() == 0;
            }
        };
        noDataMessage.setOutputMarkupPlaceholderTag(true);
        add(noDataMessage);

        LoadableDetachableModel<List<Workspace>> workspacesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<Workspace> load() {
                return loadWorkspaces();
            }
        };
        workspacesList = new ListView<>("workspaces", workspacesModel) {
            @Override
            protected void populateItem(ListItem<Workspace> item) {
                Workspace ws = item.getModelObject();
                final int wsLayerCount = getLayerCount(ws.name);

                WebMarkupContainer toggle = new WebMarkupContainer("workspaceToggle");
                toggle.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        WorkspaceState state = workspaceStates.computeIfAbsent(ws.name, n -> new WorkspaceState());
                        state.expanded = !state.expanded;
                        ws.expanded = state.expanded;
                        target.add(workspacesScroll);
                        target.appendJavaScript(initCall());
                    }
                });
                toggle.add(new ToggleCaretIcon("workspaceToggleIcon", () -> ws.expanded));
                item.add(toggle);

                item.add(workspaceIcon("workspaceIcon"));
                AjaxLink<Void> select = new AjaxLink<>("workspaceSelect") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        navigateToHome(ws.name, null, null);
                    }
                };
                if (ws.name.equals(selectedWorkspaceName)) {
                    item.add(AttributeAppender.append("class", "is-active"));
                }
                select.add(AttributeModifier.replace("title", ws.name));
                Label workspaceName = new Label("workspaceName", highlightTreeText(ws.name));
                workspaceName.setEscapeModelStrings(false);
                select.add(workspaceName);
                item.add(select);
                item.add(new Label("workspaceLayerCount", formatCount(wsLayerCount)));

                WebMarkupContainer layersScroll = new WebMarkupContainer("layersScroll") {
                    @Override
                    public boolean isVisible() {
                        return ws.expanded;
                    }
                };
                layersScroll.setOutputMarkupId(true);
                toggle.add(
                        new org.apache.wicket.AttributeModifier("aria-expanded", () -> ws.expanded ? "true" : "false"));
                toggle.add(new org.apache.wicket.AttributeModifier("aria-controls", () -> layersScroll.getMarkupId()));
                layersScroll.add(new org.apache.wicket.AttributeModifier("data-kind", "layers"));
                layersScroll.add(new org.apache.wicket.AttributeModifier("data-workspace", ws.name));
                item.add(layersScroll);

                WebMarkupContainer layersPagination = new WebMarkupContainer("layersPagination") {
                    @Override
                    public boolean isVisible() {
                        return ws.expanded && wsLayerCount > PAGE_SIZE;
                    }
                };
                layersPagination.setOutputMarkupId(true);
                layersPagination.add(new org.apache.wicket.AttributeModifier("data-workspace", () -> ws.name));
                layersPagination.add(new org.apache.wicket.AttributeModifier(
                        "data-current-page", () -> String.valueOf(layerPageByWorkspace.getOrDefault(ws.name, 1))));
                layersPagination.add(new org.apache.wicket.AttributeModifier(
                        "data-page-size",
                        () -> String.valueOf(layerPageSizeByWorkspace.getOrDefault(ws.name, PAGE_SIZE))));
                layersPagination.add(new org.apache.wicket.AttributeModifier(
                        "data-total-items", () -> String.valueOf(wsLayerCount)));
                layersPagination.add(new org.apache.wicket.AttributeModifier("data-total-pages", () -> {
                    int total = wsLayerCount;
                    int pageSize = layerPageSizeByWorkspace.getOrDefault(ws.name, PAGE_SIZE);
                    return String.valueOf((int) Math.ceil(total / (double) pageSize));
                }));
                item.add(layersPagination);

                layersScroll.add(
                        new ListView<>("workspaceChildren", new LoadableDetachableModel<List<WorkspaceChild>>() {
                            @Override
                            protected List<WorkspaceChild> load() {
                                int page = layerPageByWorkspace.getOrDefault(ws.name, 1);
                                int pageSize = layerPageSizeByWorkspace.getOrDefault(ws.name, PAGE_SIZE);
                                int offset = (page - 1) * pageSize;
                                return getWorkspaceChildrenPage(ws.name, offset, pageSize);
                            }
                        }) {
                            @Override
                            protected void populateItem(ListItem<WorkspaceChild> child) {
                                WorkspaceChild childInfo = child.getModelObject();
                                child.add(childIcon("layerIcon", childInfo.type));
                                AjaxLink<Void> layerSelect = new AjaxLink<>("layerSelect") {
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        navigateToHome(ws.name, childInfo.name, childInfo.type);
                                    }
                                };
                                layerSelect.add(AttributeModifier.replace("title", childInfo.name));
                                if (ws.name.equals(selectedWorkspaceName) && childInfo.name.equals(selectedLayerName)) {
                                    child.add(AttributeAppender.append("class", "is-active"));
                                }
                                Label layerName = new Label("layerName", highlightTreeText(childInfo.name));
                                layerName.setEscapeModelStrings(false);
                                layerSelect.add(layerName);
                                child.add(layerSelect);
                            }
                        });
            }
        };
        workspacesList.setOutputMarkupId(true);
        workspacesScroll.add(workspacesList);

        loadWorkspacesBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                IRequestParameters p = RequestCycle.get().getRequest().getRequestParameters();
                String kind = p.getParameterValue("kind").toOptionalString();

                if ("filter".equals(kind)) {
                    String query = p.getParameterValue("q").toOptionalString();
                    String trimmed = query == null ? "" : query.trim();
                    treeFilterQuery =
                            trimmed.length() > MAX_QUERY_LENGTH ? trimmed.substring(0, MAX_QUERY_LENGTH) : trimmed;

                    globalPage = 1;
                    layerPageByWorkspace.clear();

                    target.add(globalSectionContainer);
                    target.add(noDataMessage);
                    target.appendJavaScript(initCall());
                    return;
                }

                if ("global".equals(kind)) {
                    int page = parsePositiveInt(p.getParameterValue("page").toOptionalString(), 1);
                    int pageSize = parsePageSize(p.getParameterValue("pageSize").toOptionalString(), PAGE_SIZE);
                    globalPage = page;
                    globalPageSize = pageSize;

                    target.add(workspacesScroll);
                    target.add(globalChildrenContainer);
                    target.add(globalPagination);
                    target.appendJavaScript(initCall());
                    return;
                }

                if ("layers".equals(kind)) {
                    String wsName = p.getParameterValue("workspace").toOptionalString();
                    if (wsName == null) return;

                    int page = parsePositiveInt(p.getParameterValue("page").toOptionalString(), 1);
                    int pageSize = parsePageSize(p.getParameterValue("pageSize").toOptionalString(), PAGE_SIZE);
                    layerPageByWorkspace.put(wsName, page);
                    layerPageSizeByWorkspace.put(wsName, pageSize);

                    target.add(workspacesScroll);
                    target.appendJavaScript(initCall());
                }
            }
        };
        add(loadWorkspacesBehavior);
    }

    private static Catalog getCatalog() {
        return GeoServerApplication.get().getCatalog();
    }

    private Filter buildSearchFilter(String propertyName) {
        if (Strings.isEmpty(treeFilterQuery)) return Filter.INCLUDE;
        String escaped =
                treeFilterQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return FF.like(FF.property(propertyName), "%" + escaped + "%", "%", "_", "\\", false);
    }

    private Filter buildGlobalLayerFilter() {
        Filter noWorkspace = FF.isNull(FF.property("resource.store.workspace.name"));
        return FF.and(noWorkspace, buildSearchFilter("name"));
    }

    private Filter buildGlobalGroupFilter() {
        Filter noWorkspace = FF.isNull(FF.property("workspace.name"));
        return FF.and(noWorkspace, buildSearchFilter("name"));
    }

    private Filter buildWorkspaceLayerFilter(String wsName) {
        Filter wsMatch = FF.equal(FF.property("resource.store.workspace.name"), FF.literal(wsName), true);
        return FF.and(wsMatch, buildSearchFilter("name"));
    }

    private Filter buildWorkspaceGroupFilter(String wsName) {
        Filter wsMatch = FF.equal(FF.property("workspace.name"), FF.literal(wsName), true);
        return FF.and(wsMatch, buildSearchFilter("name"));
    }

    private int getTotalGlobalItems() {
        if (cachedTotalGlobalItems == null) {
            int layerCount = getCatalog().count(LayerInfo.class, buildGlobalLayerFilter());
            int groupCount = getCatalog().count(LayerGroupInfo.class, buildGlobalGroupFilter());
            cachedTotalGlobalItems = layerCount + groupCount;
        }
        return cachedTotalGlobalItems;
    }

    private int getLayerCount(String workspaceName) {
        int layerCount = getCatalog().count(LayerInfo.class, buildWorkspaceLayerFilter(workspaceName));
        int groupCount = getCatalog().count(LayerGroupInfo.class, buildWorkspaceGroupFilter(workspaceName));
        return layerCount + groupCount;
    }

    private int getTotalWorkspaceItems() {
        if (cachedTotalWorkspaceItems == null) {
            if (hasActiveTreeFilter()) {
                cachedTotalWorkspaceItems = matchingWorkspaces().size();
            } else {
                cachedTotalWorkspaceItems = getCatalog().count(WorkspaceInfo.class, Filter.INCLUDE);
            }
        }
        return cachedTotalWorkspaceItems;
    }

    private Set<String> getWorkspacesMatchingFilter() {
        Set<String> matchingWs = new HashSet<>();
        Filter nameMatch = buildSearchFilter("name");

        try (CloseableIterator<WorkspaceInfo> it =
                getCatalog().list(WorkspaceInfo.class, nameMatch, 0, SEARCH_LIMIT, null)) {
            while (it.hasNext()) {
                matchingWs.add(it.next().getName());
            }
        }
        Filter layerMatch = FF.and(FF.not(FF.isNull(FF.property("resource.store.workspace.name"))), nameMatch);
        try (CloseableIterator<LayerInfo> it = getCatalog().list(LayerInfo.class, layerMatch, 0, SEARCH_LIMIT, null)) {
            while (it.hasNext()) {
                LayerInfo layer = it.next();
                if (layer.getResource() != null && layer.getResource().getStore() != null) {
                    WorkspaceInfo ws = layer.getResource().getStore().getWorkspace();
                    if (ws != null) matchingWs.add(ws.getName());
                }
            }
        }

        Filter groupMatch = FF.and(FF.not(FF.isNull(FF.property("workspace.name"))), nameMatch);
        try (CloseableIterator<LayerGroupInfo> it =
                getCatalog().list(LayerGroupInfo.class, groupMatch, 0, SEARCH_LIMIT, null)) {
            while (it.hasNext()) {
                WorkspaceInfo ws = it.next().getWorkspace();
                if (ws != null) matchingWs.add(ws.getName());
            }
        }

        return matchingWs;
    }

    private Set<String> matchingWorkspaces() {
        if (cachedMatchingWorkspaces == null) {
            cachedMatchingWorkspaces = getWorkspacesMatchingFilter();
        }
        return cachedMatchingWorkspaces;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));
        response.render(OnDomReadyHeaderItem.forScript(initCall()));
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        cachedMatchingWorkspaces = null;
        cachedTotalGlobalItems = null;
        cachedTotalWorkspaceItems = null;
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        PageParameters pageParams = getPage().getPageParameters();
        String paramWorkspace = pageParams.get("workspace").toOptionalString();
        // Support both "layer" (layers) and "group" (layer groups); also accept "layer" for
        // legacy bookmarks that may contain a group name under the old "layer" key.
        String paramLayer = pageParams.get("layer").toOptionalString();
        if (paramLayer == null) paramLayer = pageParams.get("group").toOptionalString();

        // Re-sync selection state from page parameters on every render
        boolean selectionChanged = !java.util.Objects.equals(paramWorkspace, selectedWorkspaceName)
                || !java.util.Objects.equals(paramLayer, selectedLayerName);

        this.selectedWorkspaceName = paramWorkspace;
        this.selectedLayerName = paramLayer;

        if (!selectionInitialized || selectionChanged) {
            if (paramWorkspace != null) {
                WorkspaceState state = workspaceStates.computeIfAbsent(paramWorkspace, n -> new WorkspaceState());
                state.expanded = true;
                if (paramLayer != null) {
                    int idx = findWorkspaceChildIndex(paramWorkspace, paramLayer);
                    if (idx >= 0) {
                        int pageSize = layerPageSizeByWorkspace.getOrDefault(paramWorkspace, PAGE_SIZE);
                        int page = (idx / pageSize) + 1;
                        layerPageByWorkspace.put(paramWorkspace, page);
                    }
                }
            } else if (paramLayer != null) {
                int idx = findGlobalChildIndex(paramLayer);
                if (idx >= 0) {
                    int combinedIdx = getTotalWorkspaceItems() + idx;
                    globalPage = (combinedIdx / globalPageSize) + 1;
                }
            }
            this.selectionInitialized = true;
        }
    }

    private int findGlobalChildIndex(String childName) {
        if (childName == null) return -1;

        Filter comesBefore = FF.less(FF.property("name"), FF.literal(childName));

        LayerInfo layer = getCatalog().getLayerByName(childName);
        if (layer != null
                && (layer.getResource() == null
                        || layer.getResource().getStore() == null
                        || layer.getResource().getStore().getWorkspace() == null)) {
            Filter precedingFilter = FF.and(buildGlobalLayerFilter(), comesBefore);
            return getCatalog().count(LayerInfo.class, precedingFilter);
        }

        LayerGroupInfo group = getCatalog().getLayerGroupByName(childName);
        if (group != null && group.getWorkspace() == null) {
            int totalGlobalLayers = getCatalog().count(LayerInfo.class, buildGlobalLayerFilter());
            Filter precedingFilter = FF.and(buildGlobalGroupFilter(), comesBefore);
            int precedingGroups = getCatalog().count(LayerGroupInfo.class, precedingFilter);
            return totalGlobalLayers + precedingGroups;
        }

        return -1;
    }

    private int findWorkspaceChildIndex(String workspaceName, String childName) {
        if (childName == null) return -1;

        Filter comesBefore = FF.less(FF.property("name"), FF.literal(childName));

        LayerInfo layer = getCatalog().getLayerByName(childName);
        if (layer != null
                && layer.getResource() != null
                && layer.getResource().getStore() != null
                && workspaceName.equals(
                        layer.getResource().getStore().getWorkspace().getName())) {
            Filter precedingFilter = FF.and(buildWorkspaceLayerFilter(workspaceName), comesBefore);
            return getCatalog().count(LayerInfo.class, precedingFilter);
        }

        LayerGroupInfo group = getCatalog().getLayerGroupByName(childName);
        if (group != null
                && group.getWorkspace() != null
                && workspaceName.equals(group.getWorkspace().getName())) {
            int totalWorkspaceLayers = getCatalog().count(LayerInfo.class, buildWorkspaceLayerFilter(workspaceName));
            Filter precedingFilter = FF.and(buildWorkspaceGroupFilter(workspaceName), comesBefore);
            int precedingGroups = getCatalog().count(LayerGroupInfo.class, precedingFilter);
            return totalWorkspaceLayers + precedingGroups;
        }

        return -1;
    }

    private String initCall() {
        return String.format(
                "initNavigationTreePanel('%s','%s');",
                SearchInputPanel.escapeForJsString(workspacesScroll.getMarkupId()),
                SearchInputPanel.escapeForJsString(
                        loadWorkspacesBehavior.getCallbackUrl().toString()));
    }

    private boolean isWorkspaceExpanded(WorkspaceState state) {
        return hasActiveTreeFilter() || state.expanded;
    }

    private List<Workspace> loadWorkspaces() {
        if (selectedWorkspaceName != null) {
            List<Workspace> single = new ArrayList<>();
            WorkspaceInfo wsInfo = getCatalog().getWorkspaceByName(selectedWorkspaceName);
            if (wsInfo != null) {
                WorkspaceState state = workspaceStates.computeIfAbsent(wsInfo.getName(), n -> new WorkspaceState());
                single.add(new Workspace(wsInfo.getName(), isWorkspaceExpanded(state)));
            }
            return single;
        }

        int combinedOffset = (globalPage - 1) * globalPageSize;
        int wsCount = Math.max(0, Math.min(getTotalWorkspaceItems(), combinedOffset + globalPageSize) - combinedOffset);
        if (wsCount <= 0) return new ArrayList<>();

        if (hasActiveTreeFilter()) {
            List<String> sortedNames = new ArrayList<>(matchingWorkspaces());
            sortedNames.sort(String::compareTo);

            List<Workspace> result = new ArrayList<>();
            int start = Math.max(0, combinedOffset);
            int end = Math.min(start + wsCount, sortedNames.size());

            for (int i = start; i < end; i++) {
                String wsName = sortedNames.get(i);
                WorkspaceState state = workspaceStates.computeIfAbsent(wsName, n -> new WorkspaceState());
                result.add(new Workspace(wsName, isWorkspaceExpanded(state)));
            }
            return result;
        }

        List<Workspace> result = new ArrayList<>();
        try (CloseableIterator<WorkspaceInfo> it =
                getCatalog().list(WorkspaceInfo.class, Filter.INCLUDE, combinedOffset, wsCount, ALPHABETICAL)) {
            while (it.hasNext()) {
                WorkspaceInfo wi = it.next();
                WorkspaceState state = workspaceStates.computeIfAbsent(wi.getName(), n -> new WorkspaceState());
                result.add(new Workspace(wi.getName(), isWorkspaceExpanded(state)));
            }
        }
        return result;
    }

    private static final class Workspace implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        String name;
        boolean expanded = false;

        Workspace(String name, boolean expanded) {
            this.name = name;
            this.expanded = expanded;
        }
    }

    private static final class WorkspaceState implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        boolean expanded = false;
    }

    private void navigateToHome(String workspaceName, String childName, ChildType childType) {
        PageParameters params = new PageParameters();
        if (workspaceName != null) {
            params.add("workspace", workspaceName);
        }
        if (childName != null) {
            if (childType == ChildType.LAYER_GROUP) {
                params.add("group", childName);
            } else {
                params.add("layer", childName);
            }
        }
        getPage().setResponsePage(GeoServerHomePage.class, params);
    }

    private interface BooleanSupplier extends Serializable {
        boolean getAsBoolean();
    }

    private static final class ToggleCaretIcon extends WebMarkupContainer {
        @Serial
        private static final long serialVersionUID = 1L;

        private final BooleanSupplier expanded;

        private ToggleCaretIcon(String id, BooleanSupplier expanded) {
            super(id);
            this.expanded = expanded;
        }

        @Override
        protected void onComponentTag(org.apache.wicket.markup.ComponentTag tag) {
            super.onComponentTag(tag);
            String stateClass = expanded.getAsBoolean() ? "is-expanded" : "is-collapsed";
            String existingClass = tag.getAttribute("class");
            if (Strings.isEmpty(existingClass)) {
                tag.put("class", stateClass);
            } else {
                tag.put("class", existingClass + " " + stateClass);
            }
        }
    }

    private static WebComponent workspaceIcon(String id) {
        return new GsIcon(id, "gs-icon-folder");
    }

    private enum ChildType {
        LAYER,
        LAYER_GROUP
    }

    private static final class WorkspaceChild implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        final String name;
        final ChildType type;

        private WorkspaceChild(String name, ChildType type) {
            this.name = name;
            this.type = type;
        }
    }

    private String formatCount(int count) {
        NumberFormat fmt = NumberFormat.getCompactNumberInstance(getLocale(), NumberFormat.Style.SHORT);
        fmt.setMaximumFractionDigits(1);
        return fmt.format(count);
    }

    private static WebComponent childIcon(String id, ChildType type) {
        String cssClass =
                switch (type) {
                    case LAYER -> "gs-icon-picture-empty";
                    case LAYER_GROUP -> "gs-icon-layers";
                };
        return new GsIcon(id, cssClass);
    }

    private List<WorkspaceChild> getWorkspaceChildrenPage(String workspaceName, int offset, int limit) {
        List<WorkspaceChild> children = new ArrayList<>();
        if (limit <= 0) return children;

        int layerCount = getCatalog().count(LayerInfo.class, buildWorkspaceLayerFilter(workspaceName));
        int groupCount = getCatalog().count(LayerGroupInfo.class, buildWorkspaceGroupFilter(workspaceName));

        if (offset >= layerCount + groupCount) return children;

        int remaining = limit;

        if (offset < layerCount) {
            int layersToTake = Math.min(remaining, layerCount - offset);
            try (CloseableIterator<LayerInfo> it = getCatalog()
                    .list(
                            LayerInfo.class,
                            buildWorkspaceLayerFilter(workspaceName),
                            offset,
                            layersToTake,
                            ALPHABETICAL)) {
                while (it.hasNext()) {
                    children.add(new WorkspaceChild(it.next().getName(), ChildType.LAYER));
                }
            }
            remaining = limit - children.size();
        }

        if (remaining > 0) {
            int offsetInGroups = Math.max(0, offset - layerCount);
            try (CloseableIterator<LayerGroupInfo> it = getCatalog()
                    .list(
                            LayerGroupInfo.class,
                            buildWorkspaceGroupFilter(workspaceName),
                            offsetInGroups,
                            remaining,
                            ALPHABETICAL)) {
                while (it.hasNext()) {
                    children.add(new WorkspaceChild(it.next().getName(), ChildType.LAYER_GROUP));
                }
            }
        }
        return children;
    }

    private List<WorkspaceChild> getGlobalChildrenPage(int offset, int limit) {
        List<WorkspaceChild> children = new ArrayList<>();
        if (limit <= 0) return children;

        int layerCount = getCatalog().count(LayerInfo.class, buildGlobalLayerFilter());
        int groupCount = getCatalog().count(LayerGroupInfo.class, buildGlobalGroupFilter());

        if (offset >= layerCount + groupCount) return children;

        int remaining = limit;

        if (offset < layerCount) {
            int layersToTake = Math.min(remaining, layerCount - offset);
            try (CloseableIterator<LayerInfo> it =
                    getCatalog().list(LayerInfo.class, buildGlobalLayerFilter(), offset, layersToTake, ALPHABETICAL)) {
                while (it.hasNext()) {
                    children.add(new WorkspaceChild(it.next().getName(), ChildType.LAYER));
                }
            }
            remaining = limit - children.size();
        }

        if (remaining > 0) {
            int offsetInGroups = Math.max(0, offset - layerCount);
            try (CloseableIterator<LayerGroupInfo> it = getCatalog()
                    .list(LayerGroupInfo.class, buildGlobalGroupFilter(), offsetInGroups, remaining, ALPHABETICAL)) {
                while (it.hasNext()) {
                    children.add(new WorkspaceChild(it.next().getName(), ChildType.LAYER_GROUP));
                }
            }
        }
        return children;
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            int v = Integer.parseInt(value);
            return v > 0 ? v : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int parsePageSize(String value, int defaultValue) {
        int ps = parsePositiveInt(value, defaultValue);
        return switch (ps) {
            case 10, 20, 50, 100, 1000 -> ps;
            default -> defaultValue;
        };
    }

    private boolean hasActiveTreeFilter() {
        return !Strings.isEmpty(treeFilterQuery);
    }

    private String highlightTreeText(String text) {
        if (text == null) return "";
        String escaped = Strings.escapeMarkup(text).toString();
        if (!hasActiveTreeFilter()) return escaped;
        String q = treeFilterQuery;
        if (Strings.isEmpty(q)) return escaped;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        String lowerQ = q.toLowerCase(java.util.Locale.ROOT);
        int idx = lower.indexOf(lowerQ);
        if (idx < 0) return escaped;

        StringBuilder out = new StringBuilder();
        int start = 0;
        while (idx >= 0) {
            if (idx > start) {
                out.append(Strings.escapeMarkup(text.substring(start, idx)));
            }
            int end = idx + q.length();
            out.append("<mark>")
                    .append(Strings.escapeMarkup(text.substring(idx, end)))
                    .append("</mark>");
            start = end;
            idx = lower.indexOf(lowerQ, start);
        }
        if (start < text.length()) {
            out.append(Strings.escapeMarkup(text.substring(start)));
        }
        return out.toString();
    }
}
