/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
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
import org.apache.wicket.markup.html.image.ContextImage;
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
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geoserver.web.wicket.GsIcon;
import org.springframework.security.core.Authentication;

public class BreadcrumbNavigationPanel extends Panel {

    private final LoadableDetachableModel<List<BreadcrumbContextMenuItemInfo>> allMenuBeansModel;

    private static final CssResourceReference CSS =
            new CssResourceReference(BreadcrumbNavigationPanel.class, "BreadcrumbNavigationPanel.css");
    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(BreadcrumbNavigationPanel.class, "BreadcrumbNavigationPanel.js");

    public BreadcrumbNavigationPanel(String id) {
        super(id);

        allMenuBeansModel = new LoadableDetachableModel<>() {
            @Override
            protected List<BreadcrumbContextMenuItemInfo> load() {
                return ((GeoServerApplication) getApplication()).getBeansOfType(BreadcrumbContextMenuItemInfo.class);
            }
        };

        LoadableDetachableModel<List<BreadcrumbItem>> breadcrumbModel = new LoadableDetachableModel<>() {
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

                items.add(new BreadcrumbItem(
                        getString("BreadcrumbNavigationPanel.global", null, "Global"),
                        GeoServerHomePage.class,
                        new PageParameters(),
                        "GLOBAL"));

                if (wsName != null && !wsName.isEmpty()) {
                    PageParameters wsParams = new PageParameters();
                    wsParams.add("workspace", wsName);
                    items.add(new BreadcrumbItem(wsName, GeoServerHomePage.class, wsParams, "WORKSPACE"));
                }

                if (resourceName != null && !resourceName.isEmpty()) {
                    String level = "LAYER";
                    LayerGroupInfo group = (wsName != null)
                            ? getCatalog().getLayerGroupByName(wsName, resourceName)
                            : getCatalog().getLayerGroupByName(resourceName);

                    if (group != null) {
                        level = "LAYER_GROUP";
                    }

                    items.add(new BreadcrumbItem(resourceName, null, null, level));
                }

                return items;
            }
        };

        ListView<BreadcrumbItem> breadcrumbList = new ListView<>("breadcrumbs", breadcrumbModel) {
            @Override
            protected void populateItem(ListItem<BreadcrumbItem> item) {
                BreadcrumbItem bc = item.getModelObject();
                boolean isLast = item.getIndex() == getList().size() - 1;

                DropdownMenuData menuData;
                if (isLast) {
                    List<BreadcrumbContextMenuItemInfo> allBeans = allMenuBeansModel.getObject();
                    Authentication user = ((GeoServerSession) getSession()).getAuthentication();

                    List<BreadcrumbContextMenuItemInfo> standalone = new ArrayList<>();
                    Map<Category, List<BreadcrumbContextMenuItemInfo>> grouped = new HashMap<>();

                    for (BreadcrumbContextMenuItemInfo bean : allBeans) {
                        if (bean.getTargetLevel().equalsIgnoreCase(bc.getLevel())
                                && (bean.getAuthorizer() == null
                                        || bean.getAuthorizer().isAccessAllowed(bean.getComponentClass(), user))) {
                            Category cat = bean.getCategory();
                            if (cat == null) {
                                standalone.add(bean);
                            } else {
                                grouped.computeIfAbsent(cat, k -> new ArrayList<>())
                                        .add(bean);
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

                    menuData = new DropdownMenuData(standalone, categoryGroups);
                } else {
                    menuData = new DropdownMenuData(new ArrayList<>(), new ArrayList<>());
                }

                boolean hasMenuItems = !menuData.isEmpty();

                String iconClass;
                switch (bc.getLevel()) {
                    case "WORKSPACE":
                        iconClass = "gs-icon-folder";
                        break;
                    case "LAYER_GROUP":
                        iconClass = "gs-icon-layers";
                        break;
                    case "LAYER":
                        iconClass = "gs-icon-picture-empty";
                        break;
                    case "GLOBAL":
                    default:
                        iconClass = "gs-icon-server";
                        break;
                }

                WebMarkupContainer normalLinkContainer = new WebMarkupContainer("normalLinkContainer");
                normalLinkContainer.setVisible(!isLast);
                item.add(normalLinkContainer);

                Class<? extends Page> targetPageClass =
                        (bc.getPageClass() != null) ? bc.getPageClass() : GeoServerHomePage.class;
                BookmarkablePageLink<Void> link =
                        new BookmarkablePageLink<>("link", targetPageClass, bc.getParameters());
                link.add(new GsIcon("linkIcon", iconClass));
                link.add(new Label("label", bc.getLabel()));
                normalLinkContainer.add(link);

                WebMarkupContainer plainLabelContainer = new WebMarkupContainer("plainLabelContainer");
                plainLabelContainer.setVisible(isLast && !hasMenuItems);
                item.add(plainLabelContainer);
                plainLabelContainer.add(new GsIcon("plainIcon", iconClass));
                plainLabelContainer.add(new Label("plainLabel", bc.getLabel()));

                WebMarkupContainer contextMenuContainer = new WebMarkupContainer("contextMenuContainer");

                contextMenuContainer.setVisible(isLast && hasMenuItems);
                item.add(contextMenuContainer);

                contextMenuContainer.add(new GsIcon("menuIcon", iconClass));
                contextMenuContainer.add(new Label("currentLabel", bc.getLabel()));

                final String bcLevel = bc.getLevel();
                ListView<BreadcrumbContextMenuItemInfo> standaloneList =
                        new ListView<>("standaloneItems", menuData.standalone) {
                            @Override
                            protected void populateItem(ListItem<BreadcrumbContextMenuItemInfo> menuItemListItem) {
                                populateMenuLink(menuItemListItem, getPage().getPageParameters(), bcLevel);
                            }
                        };
                contextMenuContainer.add(standaloneList);

                ListView<CategoryGroup> categoryGroupsList = new ListView<>("categoryGroups", menuData.groups) {
                    @Override
                    protected void populateItem(ListItem<CategoryGroup> groupItem) {
                        CategoryGroup group = groupItem.getModelObject();

                        groupItem.add(new Label(
                                "categoryHeader",
                                new ResourceModel(group.category.getNameKey(), group.category.getNameKey())));

                        ListView<BreadcrumbContextMenuItemInfo> categoryItemsList =
                                new ListView<>("categoryItems", group.items) {
                                    @Override
                                    protected void populateItem(
                                            ListItem<BreadcrumbContextMenuItemInfo> menuItemListItem) {
                                        populateMenuLink(
                                                menuItemListItem, getPage().getPageParameters(), bcLevel);
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
            ListItem<BreadcrumbContextMenuItemInfo> menuItemListItem, PageParameters currentParams, String level) {
        BreadcrumbContextMenuItemInfo ctxMenu = menuItemListItem.getModelObject();
        String wName = currentParams.get("workspace").toString(null);
        String lName;
        if ("LAYER_GROUP".equals(level)) {
            lName = currentParams.get("group").toString(null);
            if (lName == null) lName = currentParams.get("layer").toString(null); // retro-compat
        } else {
            lName = currentParams.get("layer").toString(null);
            if (lName == null) lName = currentParams.get("name").toString(null);
        }

        PageParameters targetParams = ctxMenu.getPageParameters(wName, lName, level);
        BookmarkablePageLink<Void> menuLink =
                new BookmarkablePageLink<>("menuLink", ctxMenu.getComponentClass(), targetParams);

        menuLink.add(org.apache.wicket.AttributeModifier.append("class", ctxMenu.getId()));

        org.apache.wicket.Component iconComponent;

        String iconValue = ctxMenu.getIcon();
        if (iconValue != null && !iconValue.isEmpty()) {
            if (iconValue.startsWith("gs-icon")) {
                iconComponent = new GsIcon("menuItemIcon", iconValue);
            } else if (iconValue.startsWith("/")) {
                iconComponent = new ContextImage("menuItemIcon", iconValue.substring(1));
            } else {
                iconComponent =
                        new Image("menuItemIcon", new PackageResourceReference(ctxMenu.getComponentClass(), iconValue));
            }
        } else {
            iconComponent = new WebMarkupContainer("menuItemIcon");
            iconComponent.setVisible(false);
        }

        menuLink.add(iconComponent);
        menuLink.add(new Label("menuLabel", new ResourceModel(ctxMenu.getTitleKey(), ctxMenu.getTitleKey())));
        menuItemListItem.add(menuLink);
    }

    private static Catalog getCatalog() {
        return GeoServerApplication.get().getCatalog();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        allMenuBeansModel.detach();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));
    }

    private static class BreadcrumbItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

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
        @Serial
        private static final long serialVersionUID = 1L;

        final List<BreadcrumbContextMenuItemInfo> standalone;
        final List<CategoryGroup> groups;

        DropdownMenuData(List<BreadcrumbContextMenuItemInfo> standalone, List<CategoryGroup> groups) {
            this.standalone = standalone;
            this.groups = groups;
        }

        boolean isEmpty() {
            return standalone.isEmpty() && groups.isEmpty();
        }
    }

    private static class CategoryGroup implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        final Category category;
        final List<BreadcrumbContextMenuItemInfo> items;

        CategoryGroup(Category category, List<BreadcrumbContextMenuItemInfo> items) {
            this.category = category;
            this.items = items;
        }
    }
}
