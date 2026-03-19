package org.geoserver.web;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
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

    private AbstractDefaultAjaxBehavior loadWorkspacesBehavior;

    private static final int PAGE_SIZE = 20;
    private int visibleWorkspaces = PAGE_SIZE;
    private final Map<String, Integer> visibleLayersByWorkspace = new HashMap<>();

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
        newMenu.add(new BookmarkablePageLink<>("addLayerLink", NewLayerPage.class));
        newMenu.add(new BookmarkablePageLink<>("addGroupLink", LayerGroupEditPage.class));
        newMenu.add(new BookmarkablePageLink<>("addStoreLink", NewDataPage.class));
        newMenu.add(new BookmarkablePageLink<>("addWorkspaceLink", WorkspaceNewPage.class));

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
        globalToggle.add(new ToggleIconImage("globalSectionToggleIcon", () -> isGlobalExpanded()));
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
                        return getGlobalChildren(Integer.MAX_VALUE);
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
        workspacesToggle.add(new ToggleIconImage("workspacesSectionToggleIcon", () -> workspacesExpanded));
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
        workspacesScroll.add(new org.apache.wicket.AttributeModifier(
                "data-has-more", () -> String.valueOf(!hasActiveTreeFilter() && visibleWorkspaces < totalWorkspaces)));
        workspacesScroll.add(new org.apache.wicket.AttributeModifier(
                "data-selected-workspace", () -> selectedWorkspaceName == null ? "" : selectedWorkspaceName));
        workspacesSectionBody.add(workspacesScroll);

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
                        toggle.add(new ToggleIconImage("workspaceToggleIcon", () -> ws.expanded));
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
                        layersScroll.add(new org.apache.wicket.AttributeModifier(
                                "data-has-more",
                                () -> String.valueOf(!hasActiveTreeFilter()
                                        && visibleLayersByWorkspace.getOrDefault(ws.name, PAGE_SIZE)
                                                < getLayerCount(ws.name))));
                        item.add(layersScroll);

                        layersScroll.add(
                                new ListView<WorkspaceChild>(
                                        "workspaceChildren", new LoadableDetachableModel<List<WorkspaceChild>>() {
                                            @Override
                                            protected List<WorkspaceChild> load() {
                                                int end = hasActiveTreeFilter()
                                                        ? Integer.MAX_VALUE
                                                        : Math.min(
                                                                visibleLayersByWorkspace.getOrDefault(
                                                                        ws.name, PAGE_SIZE),
                                                                getLayerCount(ws.name));
                                                return getWorkspaceChildren(ws.name, end);
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

        // --- Infinite Scroll Behavior for Workspaces + Layers (mock, server driven) ---
        loadWorkspacesBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                IRequestParameters p = RequestCycle.get().getRequest().getRequestParameters();
                String kind = p.getParameterValue("kind").toOptionalString();
                if ("filter".equals(kind)) {
                    String query = p.getParameterValue("q").toOptionalString();
                    treeFilterQuery = query == null ? "" : query.trim();
                    visibleWorkspaces = PAGE_SIZE;
                    visibleLayersByWorkspace.clear();
                    workspacesExpanded = true;
                    target.add(globalSectionContainer);
                    target.add(workspacesSectionContainer);
                    target.add(noDataMessage);
                    target.appendJavaScript(initCall());
                    return;
                }
                if ("workspaces".equals(kind)) {
                    if (hasActiveTreeFilter()) {
                        return;
                    }
                    visibleWorkspaces = Math.min(totalWorkspaces, visibleWorkspaces + PAGE_SIZE);
                    // Render updated workspaces
                    target.add(workspacesScroll);
                    boolean hasMore = visibleWorkspaces < totalWorkspaces;
                    target.appendJavaScript("$('#" + workspacesScroll.getMarkupId() + "')"
                            + ".attr('data-has-more', '"
                            + hasMore
                            + "')"
                            + ".data('hasMore', "
                            + hasMore
                            + ");");
                    target.appendJavaScript(initCall());
                    return;
                }
                if ("layers".equals(kind)) {
                    if (hasActiveTreeFilter()) {
                        return;
                    }
                    String wsName = p.getParameterValue("workspace").toOptionalString();
                    if (wsName != null) {
                        int current = visibleLayersByWorkspace.getOrDefault(wsName, PAGE_SIZE);
                        int totalLayers = getLayerCount(wsName);
                        int newVisible = Math.min(totalLayers, current + PAGE_SIZE);
                        visibleLayersByWorkspace.put(wsName, newVisible);
                        // Render updated layer lists
                        target.add(workspacesScroll);
                        boolean hasMore = newVisible < totalLayers;
                        target.appendJavaScript("$('[data-kind=\"layers\"][data-workspace=\"" + wsName
                                + "\"]')"
                                + ".attr('data-has-more', '"
                                + hasMore
                                + "')"
                                + ".data('hasMore', "
                                + hasMore
                                + ");");
                        target.appendJavaScript(initCall());
                    }
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
            }
            this.selectionInitialized = true;
        }
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
        // just because the matching workspace appears beyond the current PAGE_SIZE window.
        int end = hasActiveTreeFilter() ? totalWorkspaces : Math.min(visibleWorkspaces, totalWorkspaces);
        List<Workspace> result = new ArrayList<>();
        try (CloseableIterator<WorkspaceInfo> it = catalog.list(WorkspaceInfo.class, Filter.INCLUDE, null, end, null)) {
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

    private static final class ToggleIconImage extends Image {
        @Serial
        private static final long serialVersionUID = 1L;

        private final BooleanSupplier expanded;

        private ToggleIconImage(String id, BooleanSupplier expanded) {
            super(id, new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/keyboard-arrow-right.png"));
            this.expanded = expanded;
        }

        @Override
        protected void onConfigure() {
            super.onConfigure();
            String path = expanded.getAsBoolean()
                    ? "img/icons/silk/keyboard-arrow-down.png"
                    : "img/icons/silk/keyboard-arrow-right.png";
            setImageResourceReference(new PackageResourceReference(GeoServerBasePage.class, path));
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
