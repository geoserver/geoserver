package org.geoserver.web;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.wicket.markup.html.image.Image;
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
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geotools.api.filter.Filter;
import org.springframework.security.core.Authentication;

public class NavigationTreePanel extends Panel {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean globalExpanded = false;
    private boolean workspacesExpanded = true;

    private WebMarkupContainer workspacesSectionBody;
    private WebMarkupContainer workspacesScroll;
    private ListView<Workspace> workspacesList;
    private WebMarkupContainer globalSectionBody;
    private WebMarkupContainer globalToggle;
    private WebMarkupContainer workspacesToggle;
    private WebMarkupContainer globalSectionContainer;
    private WebMarkupContainer workspacesSectionContainer;
    private WebMarkupContainer noDataMessage;
    private ListView<WorkspaceChild> globalChildrenList;
    private WebMarkupContainer globalPagination;

    private AbstractDefaultAjaxBehavior loadWorkspacesBehavior;

    private static final int PAGE_SIZE = 50;
    private int globalPage = 1;
    private int globalPageSize = PAGE_SIZE;
    private int workspacesPage = 1;
    private int workspacesPageSize = PAGE_SIZE;
    private WebMarkupContainer workspacesPagination;

    // Page-based pagination for layers. State is unique per workspace.
    private final Map<String, Integer> layerPageByWorkspace = new HashMap<>();
    private final Map<String, Integer> layerPageSizeByWorkspace = new HashMap<>();

    private final Catalog catalog = GeoServerApplication.get().getCatalog();
    private int totalWorkspaces;
    private final Map<String, WorkspaceState> workspaceStates = new HashMap<>();
    private String selectedWorkspaceName;
    private String selectedLayerName;
    private String treeFilterQuery = "";
    private boolean selectionInitialized;

    private static final CssResourceReference CSS =
            new CssResourceReference(NavigationTreePanel.class, "NavigationTreePanel.css");
    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(NavigationTreePanel.class, "NavigationTreePanel.js");

    public NavigationTreePanel(String id) {
        super(id);

        totalWorkspaces = catalog.count(WorkspaceInfo.class, Filter.INCLUDE);
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
        newMenu.add(new ListView<SidebarNewMenuItemInfo>("newMenuItems", sidebarNewItems) {
            @Override
            protected void populateItem(ListItem<SidebarNewMenuItemInfo> item) {
                SidebarNewMenuItemInfo info = item.getModelObject();
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", info.getComponentClass());
                String titleKey = info.getTitleKey();
                Label label;
                if (titleKey != null && !titleKey.isEmpty()) {
                    // Use i18n key when available, but fall back to the key itself if missing
                    label = new Label("label", new ResourceModel(titleKey, titleKey));
                } else {
                    label = new Label("label", "");
                }
                link.add(label);
                item.add(link);
            }
        });

        // --- Global Section ---
        globalSectionContainer = new WebMarkupContainer("globalSectionContainer") {
            @Override
            public boolean isVisible() {
                return !hasActiveTreeFilter() || hasFilteredGlobalResults();
            }
        };
        globalSectionContainer.setOutputMarkupPlaceholderTag(true);
        add(globalSectionContainer);

        globalSectionBody = new WebMarkupContainer("globalSectionBody") {
            @Override
            public boolean isVisible() {
                return isGlobalExpanded();
            }
        };
        globalSectionBody.setOutputMarkupPlaceholderTag(true);
        globalSectionContainer.add(globalSectionBody);

        globalToggle = new WebMarkupContainer("globalSectionToggle");
        globalToggle.setOutputMarkupId(true);
        globalToggle.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                globalExpanded = !globalExpanded;
                target.add(globalSectionBody);
                target.add(globalToggle);
                target.appendJavaScript(initCall());
            }
        });
        globalToggle.add(new ToggleCaretLabel("globalSectionToggleIcon", () -> isGlobalExpanded()));
        globalSectionContainer.add(globalToggle);
        globalSectionContainer.add(new Label("globalCount", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return String.valueOf(getGlobalChildren(Integer.MAX_VALUE).size());
            }
        }));

        globalChildrenList =
                new ListView<>("globalChildren", new LoadableDetachableModel<List<WorkspaceChild>>() {
                    @Override
                    protected List<WorkspaceChild> load() {
                        if (hasActiveTreeFilter()) {
                            return getGlobalChildren(Integer.MAX_VALUE);
                        }
                        int offset = (globalPage - 1) * globalPageSize;
                        return getGlobalChildrenPage(offset, globalPageSize);
                    }
                }) {
                    @Override
                    protected void populateItem(ListItem<WorkspaceChild> item) {
                        WorkspaceChild childInfo = item.getModelObject();
                        item.add(childIcon("layerIcon", childInfo.type));
                        AjaxLink<Void> layerSelect = new AjaxLink<>("layerSelect") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                navigateToHome(null, childInfo.name);
                            }
                        };
                        if (selectedWorkspaceName == null && childInfo.name.equals(selectedLayerName)) {
                            item.add(AttributeAppender.append("class", "is-active"));
                        }
                        Label layerName = new Label("layerName", highlightTreeText(childInfo.name));
                        layerName.setEscapeModelStrings(false);
                        layerSelect.add(layerName);
                        item.add(layerSelect);
                    }
                };
        globalChildrenList.setOutputMarkupId(true);
        globalSectionBody.add(globalChildrenList);

        globalPagination = new WebMarkupContainer("globalPagination") {
            @Override
            public boolean isVisible() {
                return !hasActiveTreeFilter()
                        && getGlobalChildren(Integer.MAX_VALUE).size() > PAGE_SIZE;
            }
        };
        globalPagination.setOutputMarkupId(true);
        globalPagination.add(
                new org.apache.wicket.AttributeModifier("data-current-page", () -> String.valueOf(globalPage)));
        globalPagination.add(
                new org.apache.wicket.AttributeModifier("data-page-size", () -> String.valueOf(globalPageSize)));
        globalPagination.add(new org.apache.wicket.AttributeModifier(
                "data-total-items",
                () -> String.valueOf(getGlobalChildren(Integer.MAX_VALUE).size())));
        globalPagination.add(new org.apache.wicket.AttributeModifier("data-total-pages", () -> {
            int total = getGlobalChildren(Integer.MAX_VALUE).size();
            return String.valueOf((int) Math.ceil(total / (double) globalPageSize));
        }));
        globalSectionBody.add(globalPagination);

        // --- Workspaces Section ---
        workspacesSectionContainer = new WebMarkupContainer("workspacesSectionContainer") {
            @Override
            public boolean isVisible() {
                return !hasActiveTreeFilter() || hasFilteredWorkspacesResults();
            }
        };
        workspacesSectionContainer.setOutputMarkupPlaceholderTag(true);
        add(workspacesSectionContainer);

        workspacesSectionBody = new WebMarkupContainer("workspacesSectionBody") {
            @Override
            public boolean isVisible() {
                return workspacesExpanded;
            }
        };
        workspacesSectionBody.setOutputMarkupPlaceholderTag(true);
        workspacesSectionContainer.add(workspacesSectionBody);

        workspacesToggle = new WebMarkupContainer("workspacesSectionToggle");
        workspacesToggle.setOutputMarkupId(true);
        workspacesToggle.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                workspacesExpanded = !workspacesExpanded;
                target.add(workspacesSectionBody);
                target.add(workspacesToggle);
                target.appendJavaScript(initCall());
            }
        });
        workspacesToggle.add(new ToggleCaretLabel("workspacesSectionToggleIcon", () -> workspacesExpanded));
        workspacesSectionContainer.add(workspacesToggle);

        AjaxLink<Void> workspacesSectionSelect = new AjaxLink<>("workspacesSectionSelect") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                // Navigate to global welcome page (no workspace/layer selection)
                navigateToHome(null, null);
            }
        };
        workspacesSectionContainer.add(workspacesSectionSelect);
        workspacesSectionContainer.add(new Label("workspacesCount", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return String.valueOf(totalWorkspaces);
            }
        }));

        workspacesScroll = new WebMarkupContainer("workspacesScroll");
        workspacesScroll.setOutputMarkupId(true);
        workspacesSectionBody.add(workspacesScroll);

        workspacesPagination = new WebMarkupContainer("workspacesPagination") {
            @Override
            public boolean isVisible() {
                return !hasActiveTreeFilter() && selectedWorkspaceName == null && totalWorkspaces > PAGE_SIZE;
            }
        };
        workspacesPagination.setOutputMarkupId(true);
        workspacesPagination.add(
                new org.apache.wicket.AttributeModifier("data-current-page", () -> String.valueOf(workspacesPage)));
        workspacesPagination.add(
                new org.apache.wicket.AttributeModifier("data-page-size", () -> String.valueOf(workspacesPageSize)));
        workspacesPagination.add(
                new org.apache.wicket.AttributeModifier("data-total-items", () -> String.valueOf(totalWorkspaces)));
        workspacesPagination.add(new org.apache.wicket.AttributeModifier(
                "data-total-pages",
                () -> String.valueOf((int) Math.ceil(totalWorkspaces / (double) workspacesPageSize))));
        workspacesSectionBody.add(workspacesPagination);

        noDataMessage = new WebMarkupContainer("noDataMessage") {
            @Override
            public boolean isVisible() {
                return hasActiveTreeFilter() && !hasFilteredGlobalResults() && !hasFilteredWorkspacesResults();
            }
        };
        noDataMessage.setOutputMarkupPlaceholderTag(true);
        add(noDataMessage);

        workspacesList =
                new ListView<>("workspaces", new LoadableDetachableModel<List<Workspace>>() {
                    @Override
                    protected List<Workspace> load() {
                        return loadWorkspaces();
                    }
                }) {
                    @Override
                    protected void populateItem(ListItem<Workspace> item) {
                        Workspace ws = item.getModelObject();

                        WebMarkupContainer toggle = new WebMarkupContainer("workspaceToggle");
                        toggle.add(new AjaxEventBehavior("click") {
                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                WorkspaceState state =
                                        workspaceStates.computeIfAbsent(ws.name, n -> new WorkspaceState());
                                state.expanded = !state.expanded;
                                ws.expanded = state.expanded;
                                target.add(workspacesScroll);
                                target.appendJavaScript(initCall());
                            }
                        });
                        toggle.add(new ToggleCaretLabel("workspaceToggleIcon", () -> ws.expanded));
                        item.add(toggle);

                        item.add(workspaceIcon("workspaceIcon"));
                        AjaxLink<Void> select = new AjaxLink<>("workspaceSelect") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                navigateToHome(ws.name, null);
                            }
                        };
                        if (ws.name.equals(selectedWorkspaceName)) {
                            item.add(AttributeAppender.append("class", "is-active"));
                        }
                        Label workspaceName = new Label("workspaceName", highlightTreeText(ws.name));
                        workspaceName.setEscapeModelStrings(false);
                        select.add(workspaceName);
                        item.add(select);
                        item.add(new Label("workspaceLayerCount", String.valueOf(getLayerCount(ws.name))));

                        WebMarkupContainer layersScroll = new WebMarkupContainer("layersScroll") {
                            @Override
                            public boolean isVisible() {
                                return ws.expanded;
                            }
                        };
                        layersScroll.setOutputMarkupId(true);
                        layersScroll.add(new org.apache.wicket.AttributeModifier("data-kind", "layers"));
                        layersScroll.add(new org.apache.wicket.AttributeModifier("data-workspace", ws.name));
                        item.add(layersScroll);

                        WebMarkupContainer layersPagination = new WebMarkupContainer("layersPagination") {
                            @Override
                            public boolean isVisible() {
                                return ws.expanded && !hasActiveTreeFilter() && getLayerCount(ws.name) > PAGE_SIZE;
                            }
                        };
                        layersPagination.setOutputMarkupId(true);
                        layersPagination.add(new org.apache.wicket.AttributeModifier("data-workspace", () -> ws.name));
                        layersPagination.add(new org.apache.wicket.AttributeModifier(
                                "data-current-page",
                                () -> String.valueOf(layerPageByWorkspace.getOrDefault(ws.name, 1))));
                        layersPagination.add(new org.apache.wicket.AttributeModifier(
                                "data-page-size",
                                () -> String.valueOf(layerPageSizeByWorkspace.getOrDefault(ws.name, PAGE_SIZE))));
                        layersPagination.add(new org.apache.wicket.AttributeModifier(
                                "data-total-items", () -> String.valueOf(getLayerCount(ws.name))));
                        layersPagination.add(new org.apache.wicket.AttributeModifier("data-total-pages", () -> {
                            int total = getLayerCount(ws.name);
                            int pageSize = layerPageSizeByWorkspace.getOrDefault(ws.name, PAGE_SIZE);
                            return String.valueOf((int) Math.ceil(total / (double) pageSize));
                        }));
                        item.add(layersPagination);

                        layersScroll.add(
                                new ListView<WorkspaceChild>(
                                        "workspaceChildren", new LoadableDetachableModel<List<WorkspaceChild>>() {
                                            @Override
                                            protected List<WorkspaceChild> load() {
                                                if (hasActiveTreeFilter()) {
                                                    return getWorkspaceChildren(ws.name, Integer.MAX_VALUE);
                                                }
                                                int page = layerPageByWorkspace.getOrDefault(ws.name, 1);
                                                int pageSize =
                                                        layerPageSizeByWorkspace.getOrDefault(ws.name, PAGE_SIZE);
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
                                                navigateToHome(ws.name, childInfo.name);
                                            }
                                        };
                                        if (ws.name.equals(selectedWorkspaceName)
                                                && childInfo.name.equals(selectedLayerName)) {
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

        // --- Page-based pagination via AJAX ---
        loadWorkspacesBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                IRequestParameters p = RequestCycle.get().getRequest().getRequestParameters();
                String kind = p.getParameterValue("kind").toOptionalString();

                if ("filter".equals(kind)) {
                    String query = p.getParameterValue("q").toOptionalString();
                    treeFilterQuery = query == null ? "" : query.trim();

                    // When filtering, hide pagination and show full results.
                    globalPage = 1;
                    globalPageSize = PAGE_SIZE;
                    workspacesPage = 1;
                    workspacesPageSize = PAGE_SIZE;
                    layerPageByWorkspace.clear();
                    layerPageSizeByWorkspace.clear();
                    workspacesExpanded = true;

                    target.add(globalSectionContainer);
                    target.add(workspacesSectionContainer);
                    target.add(noDataMessage);
                    target.appendJavaScript(initCall());
                    return;
                }

                if ("global".equals(kind)) {
                    if (hasActiveTreeFilter()) return;
                    int page = parsePositiveInt(p.getParameterValue("page").toOptionalString(), 1);
                    int pageSize = parsePageSize(p.getParameterValue("pageSize").toOptionalString(), PAGE_SIZE);
                    globalPage = page;
                    globalPageSize = pageSize;

                    target.add(globalSectionBody);
                    target.add(globalPagination);
                    target.appendJavaScript(initCall());
                    return;
                }

                if ("workspaces".equals(kind)) {
                    if (hasActiveTreeFilter()) return;
                    if (selectedWorkspaceName != null) return;

                    int page = parsePositiveInt(p.getParameterValue("page").toOptionalString(), 1);
                    int pageSize = parsePageSize(p.getParameterValue("pageSize").toOptionalString(), PAGE_SIZE);
                    workspacesPage = page;
                    workspacesPageSize = pageSize;

                    target.add(workspacesScroll);
                    target.add(workspacesPagination);
                    target.appendJavaScript(initCall());
                    return;
                }

                if ("layers".equals(kind)) {
                    if (hasActiveTreeFilter()) return;
                    String wsName = p.getParameterValue("workspace").toOptionalString();
                    if (wsName == null) return;

                    int page = parsePositiveInt(p.getParameterValue("page").toOptionalString(), 1);
                    int pageSize = parsePageSize(p.getParameterValue("pageSize").toOptionalString(), PAGE_SIZE);
                    layerPageByWorkspace.put(wsName, page);
                    layerPageSizeByWorkspace.put(wsName, pageSize);

                    // Re-render the workspace list so its layers + pager update.
                    target.add(workspacesScroll);
                    target.appendJavaScript(initCall());
                }
            }
        };
        add(loadWorkspacesBehavior);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));

        response.render(OnDomReadyHeaderItem.forScript(initCall()));
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (!selectionInitialized) {
            String paramWorkspace =
                    getPage().getPageParameters().get("workspace").toOptionalString();
            String paramLayer = getPage().getPageParameters().get("layer").toOptionalString();
            this.selectedWorkspaceName = paramWorkspace;
            this.selectedLayerName = paramLayer;

            if (paramWorkspace != null) {
                WorkspaceState state = workspaceStates.computeIfAbsent(paramWorkspace, n -> new WorkspaceState());
                state.expanded = true;
                workspacesExpanded = true;
                if (paramLayer != null) {
                    int idx = findWorkspaceChildIndex(paramWorkspace, paramLayer);
                    if (idx >= 0) {
                        int pageSize = layerPageSizeByWorkspace.getOrDefault(paramWorkspace, PAGE_SIZE);
                        int page = (idx / pageSize) + 1;
                        layerPageByWorkspace.put(paramWorkspace, page);
                    }
                }
            } else if (paramLayer != null) {
                // Global layer selection: expand global section and set page so selection is visible.
                globalExpanded = true;
                int idx = findGlobalChildIndex(paramLayer);
                if (idx >= 0) {
                    globalPage = (idx / globalPageSize) + 1;
                }
            }
            this.selectionInitialized = true;
        }
    }

    private int findGlobalChildIndex(String childName) {
        if (childName == null) return -1;

        int idx = 0;
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, Filter.INCLUDE, null, null, null)) {
            while (it.hasNext()) {
                LayerInfo li = it.next();
                if (li.getResource() == null
                        || li.getResource().getStore() == null
                        || li.getResource().getStore().getWorkspace() != null) {
                    continue;
                }
                if (matchesFilterText(li.getName()) && childName.equals(li.getName())) {
                    return idx;
                }
                if (matchesFilterText(li.getName())) {
                    idx++;
                }
            }
        }

        try (CloseableIterator<LayerGroupInfo> it =
                catalog.list(LayerGroupInfo.class, Filter.INCLUDE, null, null, null)) {
            while (it.hasNext()) {
                LayerGroupInfo gi = it.next();
                if (gi.getWorkspace() != null) {
                    continue;
                }
                if (matchesFilterText(gi.getName()) && childName.equals(gi.getName())) {
                    return idx;
                }
                if (matchesFilterText(gi.getName())) {
                    idx++;
                }
            }
        }

        return -1;
    }

    private String initCall() {
        return String.format(
                "initNavigationTreePanel('%s','%s');",
                workspacesScroll.getMarkupId(), loadWorkspacesBehavior.getCallbackUrl());
    }

    private boolean isWorkspaceExpanded(WorkspaceState state) {
        return hasActiveTreeFilter() || state.expanded;
    }

    private boolean isGlobalExpanded() {
        return hasActiveTreeFilter() || globalExpanded;
    }

    private List<Workspace> loadWorkspaces() {
        if (selectedWorkspaceName != null) {
            List<Workspace> single = new ArrayList<>();
            WorkspaceInfo wsInfo = catalog.getWorkspaceByName(selectedWorkspaceName);
            if (wsInfo != null && matchesWorkspaceFilter(wsInfo.getName())) {
                WorkspaceState state = workspaceStates.computeIfAbsent(wsInfo.getName(), n -> new WorkspaceState());
                single.add(new Workspace(wsInfo.getName(), isWorkspaceExpanded(state)));
            }
            return single;
        }

        // When filtering, don't page the workspace list; otherwise we can incorrectly show "no data"
        // just because the matching workspace appears beyond the current window.
        if (hasActiveTreeFilter()) {
            List<Workspace> result = new ArrayList<>();
            try (CloseableIterator<WorkspaceInfo> it =
                    catalog.list(WorkspaceInfo.class, Filter.INCLUDE, null, totalWorkspaces, null)) {
                while (it.hasNext()) {
                    WorkspaceInfo wi = it.next();
                    if (!matchesWorkspaceFilter(wi.getName())) continue;
                    WorkspaceState state = workspaceStates.computeIfAbsent(wi.getName(), n -> new WorkspaceState());
                    result.add(new Workspace(wi.getName(), isWorkspaceExpanded(state)));
                }
            }
            return result;
        }

        int offset = Math.max(0, (workspacesPage - 1) * workspacesPageSize);
        if (offset >= totalWorkspaces) {
            return new ArrayList<>();
        }
        List<Workspace> result = new ArrayList<>();
        try (CloseableIterator<WorkspaceInfo> it =
                catalog.list(WorkspaceInfo.class, Filter.INCLUDE, offset, workspacesPageSize, null)) {
            while (it.hasNext()) {
                WorkspaceInfo wi = it.next();
                if (!matchesWorkspaceFilter(wi.getName())) {
                    continue;
                }
                WorkspaceState state = workspaceStates.computeIfAbsent(wi.getName(), n -> new WorkspaceState());
                result.add(new Workspace(wi.getName(), isWorkspaceExpanded(state)));
            }
        }
        return result;
    }

    private boolean hasFilteredGlobalResults() {
        if (!hasActiveTreeFilter()) {
            return true;
        }
        return !getGlobalChildren(Integer.MAX_VALUE).isEmpty();
    }

    private boolean hasFilteredWorkspacesResults() {
        if (!hasActiveTreeFilter()) {
            return true;
        }
        return !loadWorkspaces().isEmpty();
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

    private void navigateToHome(String workspaceName, String layerName) {
        PageParameters params = new PageParameters();
        if (workspaceName != null) {
            params.add("workspace", workspaceName);
        }
        if (layerName != null) {
            params.add("layer", layerName);
        }
        getPage().setResponsePage(GeoServerHomePage.class, params);
    }

    private interface BooleanSupplier extends Serializable {
        boolean getAsBoolean();
    }

    private static final class ToggleCaretLabel extends Label {
        @Serial
        private static final long serialVersionUID = 1L;

        private final BooleanSupplier expanded;

        private ToggleCaretLabel(String id, BooleanSupplier expanded) {
            super(id, "");
            this.expanded = expanded;
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            // Use simple caret characters instead of image icons.
            // Collapsed: › (right-facing caret)
            // Expanded: ˅ (down-facing caret)
            setDefaultModelObject(expanded.getAsBoolean() ? "▾" : "›");
        }
    }

    private static WebComponent workspaceIcon(String id) {
        return new Image(id, new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/folder.png"));
    }

    private enum ChildType {
        WORKSPACE,
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

    private static WebComponent childIcon(String id, ChildType type) {
        String path =
                switch (type) {
                    case WORKSPACE -> "img/icons/silk/folder.png";
                    case LAYER -> "img/icons/silk/picture_empty.png";
                    case LAYER_GROUP -> "img/icons/silk/layers.png";
                };
        return new Image(id, new PackageResourceReference(GeoServerBasePage.class, path));
    }

    private int getLayerCount(String workspaceName) {
        Filter layerFilter = Predicates.equal("resource.store.workspace.name", workspaceName);
        int layers = catalog.count(LayerInfo.class, layerFilter);
        Filter groupFilter = Predicates.equal("workspace.name", workspaceName);
        int groups = catalog.count(LayerGroupInfo.class, groupFilter);
        return layers + groups;
    }

    private int getWorkspaceLayerCount(String workspaceName) {
        Filter layerFilter = Predicates.equal("resource.store.workspace.name", workspaceName);
        return catalog.count(LayerInfo.class, layerFilter);
    }

    private int getWorkspaceGroupCount(String workspaceName) {
        Filter groupFilter = Predicates.equal("workspace.name", workspaceName);
        return catalog.count(LayerGroupInfo.class, groupFilter);
    }

    private List<WorkspaceChild> getWorkspaceChildrenPage(String workspaceName, int offset, int limit) {
        List<WorkspaceChild> children = new ArrayList<>();
        if (limit <= 0) {
            return children;
        }

        int layerCount = getWorkspaceLayerCount(workspaceName);
        int groupCount = getWorkspaceGroupCount(workspaceName);
        int total = layerCount + groupCount;
        if (offset >= total) {
            return children;
        }

        int remaining = limit;

        // 1) Layers (first)
        if (offset < layerCount) {
            int layersToSkip = Math.max(0, offset);
            int layersToTake = Math.min(remaining, layerCount - layersToSkip);
            Filter layerFilter = Predicates.equal("resource.store.workspace.name", workspaceName);
            try (CloseableIterator<LayerInfo> it =
                    catalog.list(LayerInfo.class, layerFilter, layersToSkip, layersToTake, null)) {
                while (it.hasNext() && children.size() < limit) {
                    String name = it.next().getName();
                    if (!matchesFilterText(name)) continue;
                    children.add(new WorkspaceChild(name, ChildType.LAYER));
                }
            }
            remaining = limit - children.size();
        }

        // 2) Layer groups (second)
        if (remaining > 0) {
            int offsetInGroups = Math.max(0, offset - layerCount);
            int groupsToTake = Math.min(remaining, groupCount - offsetInGroups);
            if (groupsToTake > 0) {
                Filter groupFilter = Predicates.equal("workspace.name", workspaceName);
                try (CloseableIterator<LayerGroupInfo> it =
                        catalog.list(LayerGroupInfo.class, groupFilter, offsetInGroups, groupsToTake, null)) {
                    while (it.hasNext() && children.size() < limit) {
                        String name = it.next().getName();
                        if (!matchesFilterText(name)) continue;
                        children.add(new WorkspaceChild(name, ChildType.LAYER_GROUP));
                    }
                }
            }
        }

        return children;
    }

    private int findWorkspaceChildIndex(String workspaceName, String childName) {
        if (childName == null) return -1;

        Filter layerFilter = Predicates.equal("resource.store.workspace.name", workspaceName);
        int idx = 0;
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, layerFilter, null, null, null)) {
            while (it.hasNext()) {
                LayerInfo li = it.next();
                if (childName.equals(li.getName())) {
                    return idx;
                }
                idx++;
            }
        }

        Filter groupFilter = Predicates.equal("workspace.name", workspaceName);
        try (CloseableIterator<LayerGroupInfo> it = catalog.list(LayerGroupInfo.class, groupFilter, null, null, null)) {
            while (it.hasNext()) {
                LayerGroupInfo gi = it.next();
                if (childName.equals(gi.getName())) {
                    return idx;
                }
                idx++;
            }
        }

        return -1;
    }

    private List<WorkspaceChild> getWorkspaceChildren(String workspaceName, int limit) {
        List<WorkspaceChild> children = new ArrayList<>();

        Filter layerFilter = Predicates.equal("resource.store.workspace.name", workspaceName);
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, layerFilter, null, limit, null)) {
            while (it.hasNext() && children.size() < limit) {
                String name = it.next().getName();
                if (!matchesFilterText(name)) continue;
                children.add(new WorkspaceChild(name, ChildType.LAYER));
            }
        }

        if (children.size() < limit) {
            Filter groupFilter = Predicates.equal("workspace.name", workspaceName);
            try (CloseableIterator<LayerGroupInfo> it =
                    catalog.list(LayerGroupInfo.class, groupFilter, null, limit, null)) {
                while (it.hasNext() && children.size() < limit) {
                    String name = it.next().getName();
                    if (!matchesFilterText(name)) continue;
                    children.add(new WorkspaceChild(name, ChildType.LAYER_GROUP));
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

    private List<WorkspaceChild> getGlobalChildrenPage(int offset, int limit) {
        List<WorkspaceChild> children = new ArrayList<>();
        if (limit <= 0) {
            return children;
        }

        int remainingOffset = Math.max(0, offset);

        // 1) Global layers first
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, Filter.INCLUDE, null, null, null)) {
            while (it.hasNext() && children.size() < limit) {
                LayerInfo layer = it.next();
                if (layer.getResource() == null
                        || layer.getResource().getStore() == null
                        || layer.getResource().getStore().getWorkspace() != null) {
                    continue;
                }

                String name = layer.getName();
                if (!matchesFilterText(name)) continue;

                if (remainingOffset > 0) {
                    remainingOffset--;
                    continue;
                }

                children.add(new WorkspaceChild(name, ChildType.LAYER));
            }
        }

        // 2) Then global layer groups
        if (children.size() < limit) {
            try (CloseableIterator<LayerGroupInfo> it =
                    catalog.list(LayerGroupInfo.class, Filter.INCLUDE, null, null, null)) {
                while (it.hasNext() && children.size() < limit) {
                    LayerGroupInfo group = it.next();
                    if (group.getWorkspace() != null) {
                        continue;
                    }
                    String name = group.getName();
                    if (!matchesFilterText(name)) continue;

                    if (remainingOffset > 0) {
                        remainingOffset--;
                        continue;
                    }

                    children.add(new WorkspaceChild(name, ChildType.LAYER_GROUP));
                }
            }
        }

        return children;
    }

    private List<WorkspaceChild> getGlobalChildren(int limit) {
        List<WorkspaceChild> children = new ArrayList<>();

        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, Filter.INCLUDE, null, null, null)) {
            while (it.hasNext() && children.size() < limit) {
                LayerInfo layer = it.next();
                if (layer.getResource() == null
                        || layer.getResource().getStore() == null
                        || layer.getResource().getStore().getWorkspace() != null) {
                    continue;
                }
                String name = layer.getName();
                if (!matchesFilterText(name)) continue;
                children.add(new WorkspaceChild(name, ChildType.LAYER));
            }
        }

        if (children.size() < limit) {
            try (CloseableIterator<LayerGroupInfo> it =
                    catalog.list(LayerGroupInfo.class, Filter.INCLUDE, null, null, null)) {
                while (it.hasNext() && children.size() < limit) {
                    LayerGroupInfo group = it.next();
                    if (group.getWorkspace() != null) {
                        continue;
                    }
                    String name = group.getName();
                    if (!matchesFilterText(name)) continue;
                    children.add(new WorkspaceChild(name, ChildType.LAYER_GROUP));
                }
            }
        }

        return children;
    }

    private boolean hasActiveTreeFilter() {
        return !Strings.isEmpty(treeFilterQuery);
    }

    private boolean matchesFilterText(String text) {
        if (!hasActiveTreeFilter()) return true;
        return text != null && text.toLowerCase().contains(treeFilterQuery.toLowerCase());
    }

    private boolean matchesWorkspaceFilter(String workspaceName) {
        if (!hasActiveTreeFilter()) return true;
        if (matchesFilterText(workspaceName)) return true;
        return workspaceHasMatchingChildrenByName(workspaceName);
    }

    private boolean workspaceHasMatchingChildrenByName(String workspaceName) {
        Filter layerWs = Predicates.equal("resource.store.workspace.name", workspaceName);
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, layerWs, null, null, null)) {
            while (it.hasNext()) {
                if (matchesFilterText(it.next().getName())) {
                    return true;
                }
            }
        }

        Filter groupWs = Predicates.equal("workspace.name", workspaceName);
        try (CloseableIterator<LayerGroupInfo> it = catalog.list(LayerGroupInfo.class, groupWs, null, null, null)) {
            while (it.hasNext()) {
                if (matchesFilterText(it.next().getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String highlightTreeText(String text) {
        if (text == null) return "";
        String escaped = Strings.escapeMarkup(text).toString();
        if (!hasActiveTreeFilter()) return escaped;
        String q = treeFilterQuery;
        if (Strings.isEmpty(q)) return escaped;
        String lower = text.toLowerCase();
        String lowerQ = q.toLowerCase();
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
