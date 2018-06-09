/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import java.io.IOException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** A page listing users, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class GroupPanel extends Panel {

    protected GeoServerTablePanel<GeoServerUserGroup> groups;
    protected GeoServerDialog dialog;
    protected SelectionGroupRemovalLink removal, removalWithRoles;
    protected Link<?> add;
    protected String serviceName;

    protected GeoServerUserGroupService getService() {
        try {
            return GeoServerApplication.get()
                    .getSecurityManager()
                    .loadUserGroupService(serviceName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GroupPanel(String id, String serviceName) {
        super(id);

        this.serviceName = serviceName;
        GroupListProvider provider = new GroupListProvider(serviceName);
        add(
                groups =
                        new GeoServerTablePanel<GeoServerUserGroup>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<GeoServerUserGroup> itemModel,
                                    Property<GeoServerUserGroup> property) {
                                if (property == GroupListProvider.GROUPNAME) {
                                    return editGroupLink(id, itemModel, property);
                                } else if (property == GroupListProvider.ENABLED) {
                                    if ((Boolean) property.getModel(itemModel).getObject())
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                }
                                throw new RuntimeException("Uknown property " + property);
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(groups.getSelection().size() > 0);
                                target.add(removal);
                                removalWithRoles.setEnabled(groups.getSelection().size() > 0);
                                target.add(removalWithRoles);
                            }
                        });
        groups.setItemReuseStrategy(new DefaultItemReuseStrategy());
        groups.setOutputMarkupId(true);
        add(dialog = new GeoServerDialog("dialog"));
        headerComponents();
    }

    public GroupPanel setHeaderVisible(boolean visible) {
        get("header").setVisible(visible);
        return this;
    }

    public GroupPanel setPagersVisible(boolean top, boolean bottom) {
        groups.getTopPager().setVisible(top);
        groups.getBottomPager().setVisible(bottom);
        return this;
    }

    protected void headerComponents() {

        boolean canCreateStore = getService().canCreateStore();
        // the add button

        WebMarkupContainer h = new WebMarkupContainer("header");
        add(h);
        if (!canCreateStore) {
            h.add(
                    new Label("message", new StringResourceModel("noCreateStore", this, null))
                            .add(new AttributeAppender("class", new Model("info-link"), " ")));
        } else {
            h.add(new Label("message", new Model()));
        }

        h.add(
                add =
                        new Link("addNew") {
                            @Override
                            public void onClick() {
                                setResponsePage(
                                        new NewGroupPage(serviceName).setReturnPage(getPage()));
                            }
                        });
        add.setVisible(canCreateStore);

        // the removal button
        h.add(
                removal =
                        new SelectionGroupRemovalLink(
                                serviceName, "removeSelected", groups, dialog, false));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        removal.setVisibilityAllowed(canCreateStore);

        // the removal button
        h.add(
                removalWithRoles =
                        new SelectionGroupRemovalLink(
                                serviceName, "removeSelectedWithRoles", groups, dialog, true));
        removalWithRoles.setOutputMarkupId(true);
        removalWithRoles.setEnabled(false);
        removalWithRoles.setVisibilityAllowed(
                canCreateStore
                        && GeoServerApplication.get()
                                .getSecurityManager()
                                .getActiveRoleService()
                                .canCreateStore());

        // enable header only for full admin
        h.setEnabled(getService().getSecurityManager().checkAuthenticationForAdminRole());
    }

    Component editGroupLink(String id, IModel itemModel, Property<GeoServerUserGroup> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditGroupPage(serviceName, (GeoServerUserGroup) getDefaultModelObject())
                                .setReturnPage(getPage()));
            }
        };
    }

    protected void onBeforeRender() {
        groups.clearSelection();
        removal.setEnabled(false);
        removalWithRoles.setEnabled(false);
        super.onBeforeRender();
    }
}
