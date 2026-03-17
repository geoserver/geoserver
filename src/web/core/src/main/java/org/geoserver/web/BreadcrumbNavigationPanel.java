package org.geoserver.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Page;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.web.spring.security.GeoServerSession;
import org.springframework.security.core.Authentication;

public class BreadcrumbNavigationPanel extends Panel {

    private static final CssResourceReference CSS =
            new CssResourceReference(BreadcrumbNavigationPanel.class, "BreadcrumbNavigationPanel.css");
    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(BreadcrumbNavigationPanel.class, "BreadcrumbNavigationPanel.js");

    public BreadcrumbNavigationPanel(String id) {
        super(id);

        LoadableDetachableModel<List<BreadcrumbItem>> breadcrumbModel =
                new LoadableDetachableModel<List<BreadcrumbItem>>() {
                    @Override
                    protected List<BreadcrumbItem> load() {
                        List<BreadcrumbItem> items = new ArrayList<>();
                        PageParameters currentParams = getPage().getPageParameters();

                        String wsName = currentParams.get("workspace").toString(null);
                        String resourceName = currentParams.get("layer").toString(null);
                        if (resourceName == null)
                            resourceName = currentParams.get("name").toString(null);
                        if (resourceName == null)
                            resourceName = currentParams.get("group").toString(null);

                        items.add(
                                new BreadcrumbItem("Global", GeoServerHomePage.class, new PageParameters(), "GLOBAL"));

                        if (wsName != null && !wsName.isEmpty()) {
                            PageParameters wsParams = new PageParameters();
                            wsParams.add("workspace", wsName);
                            items.add(new BreadcrumbItem(wsName, GeoServerHomePage.class, wsParams, "WORKSPACE"));
                        }

                        if (resourceName != null && !resourceName.isEmpty()) {
                            String level = "LAYER";
                            Catalog catalog = ((GeoServerApplication) getApplication()).getCatalog();
                            if (wsName != null && catalog.getLayerGroupByName(wsName, resourceName) != null) {
                                level = "LAYER_GROUP";
                            } else if (wsName == null && catalog.getLayerGroupByName(resourceName) != null) {
                                level = "LAYER_GROUP";
                            }
                            items.add(new BreadcrumbItem(resourceName, null, null, level));
                        }

                        return items;
                    }
                };

        ListView<BreadcrumbItem> breadcrumbList = new ListView<BreadcrumbItem>("breadcrumbs", breadcrumbModel) {
            @Override
            protected void populateItem(ListItem<BreadcrumbItem> item) {
                BreadcrumbItem bc = item.getModelObject();
                boolean isLast = (item.getIndex() == getList().size() - 1);

                LoadableDetachableModel<DropdownMenuData> menuDataModel =
                        new LoadableDetachableModel<DropdownMenuData>() {
                            @Override
                            protected DropdownMenuData load() {
                                GeoServerApplication app = (GeoServerApplication) getApplication();
                                List<BreadcrumbContextMenuItemInfo> allBeans =
                                        app.getBeansOfType(BreadcrumbContextMenuItemInfo.class);
                                Authentication user = ((GeoServerSession) getSession()).getAuthentication();

                                List<BreadcrumbContextMenuItemInfo> standalone = new ArrayList<>();
                                Map<Category, List<BreadcrumbContextMenuItemInfo>> grouped = new HashMap<>();

                                for (BreadcrumbContextMenuItemInfo bean : allBeans) {
                                    if (bean.getTargetLevel().equalsIgnoreCase(bc.getLevel())) {
                                        if (bean.getAuthorizer() == null
                                                || bean.getAuthorizer()
                                                        .isAccessAllowed(bean.getComponentClass(), user)) {
                                            String target =
                                                    bean.getTargetLevel() != null ? bean.getTargetLevel() : "LAYER";
                                            if (target.equalsIgnoreCase(bc.getLevel())) {
                                                if (bean.getAuthorizer() == null
                                                        || bean.getAuthorizer()
                                                                .isAccessAllowed(bean.getComponentClass(), user)) {
                                                    Category cat = bean.getCategory();
                                                    if (cat == null) {
                                                        standalone.add(bean);
                                                    } else {
                                                        if (!grouped.containsKey(cat))
                                                            grouped.put(cat, new ArrayList<>());
                                                        grouped.get(cat).add(bean);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Collections.sort(standalone);
                                List<Category> categories = new ArrayList<>(grouped.keySet());
                                Collections.sort(categories);

                                List<CategoryGroup> categoryGroups = new ArrayList<>();
                                for (Category cat : categories) {
                                    List<BreadcrumbContextMenuItemInfo> groupItems = grouped.get(cat);
                                    Collections.sort(groupItems);
                                    categoryGroups.add(new CategoryGroup(cat, groupItems));
                                }

                                return new DropdownMenuData(standalone, categoryGroups);
                            }
                        };

                boolean hasMenuItems = !menuDataModel.getObject().isEmpty();

                PackageResourceReference iconRef;
                switch (bc.getLevel()) {
                    case "WORKSPACE":
                        iconRef = new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/folder.png");
                        break;
                    case "LAYER_GROUP":
                        iconRef = new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/layers.png");
                        break;
                    case "LAYER":
                        iconRef = new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/picture_empty.png");
                        break;
                    case "GLOBAL":
                    default:
                        iconRef = new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/server.png");
                        break;
                }

                WebMarkupContainer normalLinkContainer = new WebMarkupContainer("normalLinkContainer");
                normalLinkContainer.setVisible(!isLast);
                item.add(normalLinkContainer);

                Class<? extends Page> targetPageClass =
                        (bc.getPageClass() != null) ? bc.getPageClass() : GeoServerHomePage.class;
                BookmarkablePageLink<Void> link =
                        new BookmarkablePageLink<Void>("link", targetPageClass, bc.getParameters());
                link.add(new Image("linkIcon", iconRef));
                link.add(new Label("label", bc.getLabel()));
                normalLinkContainer.add(link);

                WebMarkupContainer plainLabelContainer = new WebMarkupContainer("plainLabelContainer");
                plainLabelContainer.setVisible(isLast && !hasMenuItems);
                item.add(plainLabelContainer);
                plainLabelContainer.add(new Image("plainIcon", iconRef));
                plainLabelContainer.add(new Label("plainLabel", bc.getLabel()));

                WebMarkupContainer contextMenuContainer = new WebMarkupContainer("contextMenuContainer");
                contextMenuContainer.setVisible(isLast && hasMenuItems);
                item.add(contextMenuContainer);

                contextMenuContainer.add(new Image("menuIcon", iconRef));
                contextMenuContainer.add(new Label("currentLabel", bc.getLabel() + " ▾"));

                LoadableDetachableModel<List<BreadcrumbContextMenuItemInfo>> standaloneModel =
                        new LoadableDetachableModel<List<BreadcrumbContextMenuItemInfo>>() {
                            @Override
                            protected List<BreadcrumbContextMenuItemInfo> load() {
                                return menuDataModel.getObject().standalone;
                            }
                        };
                ListView<BreadcrumbContextMenuItemInfo> standaloneList =
                        new ListView<BreadcrumbContextMenuItemInfo>("standaloneItems", standaloneModel) {
                            @Override
                            protected void populateItem(ListItem<BreadcrumbContextMenuItemInfo> menuItemListItem) {
                                populateMenuLink(menuItemListItem, getPage().getPageParameters());
                            }
                        };
                contextMenuContainer.add(standaloneList);

                LoadableDetachableModel<List<CategoryGroup>> groupsModel =
                        new LoadableDetachableModel<List<CategoryGroup>>() {
                            @Override
                            protected List<CategoryGroup> load() {
                                return menuDataModel.getObject().groups;
                            }
                        };
                ListView<CategoryGroup> categoryGroupsList =
                        new ListView<CategoryGroup>("categoryGroups", groupsModel) {
                            @Override
                            protected void populateItem(ListItem<CategoryGroup> groupItem) {
                                CategoryGroup group = groupItem.getModelObject();

                                groupItem.add(new Label(
                                        "categoryHeader",
                                        new ResourceModel(group.category.getNameKey(), group.category.getNameKey())));

                                ListView<BreadcrumbContextMenuItemInfo> categoryItemsList =
                                        new ListView<BreadcrumbContextMenuItemInfo>("categoryItems", group.items) {
                                            @Override
                                            protected void populateItem(
                                                    ListItem<BreadcrumbContextMenuItemInfo> menuItemListItem) {
                                                populateMenuLink(
                                                        menuItemListItem,
                                                        getPage().getPageParameters());
                                            }
                                        };
                                groupItem.add(categoryItemsList);
                            }
                        };
                contextMenuContainer.add(categoryGroupsList);
            }
        };

        add(breadcrumbList);
    }

    private void populateMenuLink(
            ListItem<BreadcrumbContextMenuItemInfo> menuItemListItem, PageParameters currentParams) {
        BreadcrumbContextMenuItemInfo ctxMenu = menuItemListItem.getModelObject();
        String wName = currentParams.get("workspace").toString(null);
        String lName = currentParams.get("layer").toString(null);
        if (lName == null) lName = currentParams.get("name").toString(null);
        if (lName == null) lName = currentParams.get("group").toString(null);

        PageParameters targetParams = ctxMenu.getPageParameters(wName, lName);
        BookmarkablePageLink<Void> menuLink =
                new BookmarkablePageLink<Void>("menuLink", ctxMenu.getComponentClass(), targetParams);

        menuLink.add(org.apache.wicket.AttributeModifier.append("class", ctxMenu.getId()));

        org.apache.wicket.Component iconComponent;

        if (ctxMenu.getIcon() != null && !ctxMenu.getIcon().isEmpty()) {
            if (ctxMenu.getIcon().startsWith("/")) {
                String contextPath = ctxMenu.getIcon().substring(1);
                iconComponent = new org.apache.wicket.markup.html.image.ContextImage("menuItemIcon", contextPath);
            } else {
                iconComponent = new org.apache.wicket.markup.html.image.Image(
                        "menuItemIcon",
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                ctxMenu.getComponentClass(), ctxMenu.getIcon()));
            }
        } else {
            iconComponent = new WebMarkupContainer("menuItemIcon");
            iconComponent.setVisible(false);
        }

        menuLink.add(iconComponent);

        menuLink.add(new Label("menuLabel", new ResourceModel(ctxMenu.getTitleKey(), ctxMenu.getTitleKey())));
        menuItemListItem.add(menuLink);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));
    }

    private static class BreadcrumbItem implements Serializable {
        private final String label;
        private final Class<? extends Page> pageClass;
        private final PageParameters parameters;
        private final String level;

        public BreadcrumbItem(String label, Class<? extends Page> pageClass, PageParameters parameters, String level) {
            this.label = label;
            this.pageClass = pageClass;
            this.parameters = parameters;
            this.level = level;
        }

        public String getLabel() {
            return label;
        }

        public Class<? extends Page> getPageClass() {
            return pageClass;
        }

        public PageParameters getParameters() {
            return parameters;
        }

        public String getLevel() {
            return level;
        }
    }

    private static class DropdownMenuData implements Serializable {
        List<BreadcrumbContextMenuItemInfo> standalone;
        List<CategoryGroup> groups;

        DropdownMenuData(List<BreadcrumbContextMenuItemInfo> standalone, List<CategoryGroup> groups) {
            this.standalone = standalone;
            this.groups = groups;
        }

        boolean isEmpty() {
            return standalone.isEmpty() && groups.isEmpty();
        }
    }

    private static class CategoryGroup implements Serializable {
        Category category;
        List<BreadcrumbContextMenuItemInfo> items;

        CategoryGroup(Category category, List<BreadcrumbContextMenuItemInfo> items) {
            this.category = category;
            this.items = items;
        }
    }
}
