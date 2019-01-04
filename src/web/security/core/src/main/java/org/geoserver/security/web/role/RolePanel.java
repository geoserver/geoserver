/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

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
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** A page listing roles, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class RolePanel extends Panel {

    protected GeoServerTablePanel<GeoServerRole> roles;
    protected GeoServerDialog dialog;
    protected SelectionRoleRemovalLink removal;
    protected Link<?> add;
    protected String roleServiceName;

    public RolePanel(String id, String serviceName) {
        super(id);
        this.roleServiceName = serviceName;

        RoleListProvider provider = new RoleListProvider(this.roleServiceName);
        add(
                roles =
                        new GeoServerTablePanel<GeoServerRole>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<GeoServerRole> itemModel,
                                    Property<GeoServerRole> property) {
                                if (property == RoleListProvider.ROLENAME) {
                                    return editRoleLink(id, itemModel, property);
                                } else if (RoleListProvider.ParentPropertyName.equals(
                                        property.getName())) {
                                    return editParentRoleLink(id, itemModel, property);
                                } else if (property == RoleListProvider.HASROLEPARAMS) {
                                    if ((Boolean) property.getModel(itemModel).getObject())
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                }
                                throw new RuntimeException("Uknown property " + property);
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(roles.getSelection().size() > 0);
                                target.add(removal);
                            }
                        });
        roles.setItemReuseStrategy(new DefaultItemReuseStrategy());
        roles.setOutputMarkupId(true);
        add(dialog = new GeoServerDialog("dialog"));
        headerComponents();
    }

    public RolePanel setHeaderVisible(boolean visible) {
        get("header").setVisible(visible);
        return this;
    }

    public RolePanel setPagersVisible(boolean top, boolean bottom) {
        roles.getTopPager().setVisible(top);
        roles.getBottomPager().setVisible(bottom);
        return this;
    }

    protected void headerComponents() {

        boolean canCreateStore = getService().canCreateStore();

        WebMarkupContainer h = new WebMarkupContainer("header");
        add(h);

        if (!canCreateStore) {
            h.add(
                    new Label("message", new StringResourceModel("noCreateStore", this, null))
                            .add(new AttributeAppender("class", new Model("info-link"), " ")));
        } else {
            h.add(new Label("message", new Model()));
        }

        // the add button
        h.add(
                add =
                        new Link("addNew") {
                            @Override
                            public void onClick() {
                                setResponsePage(
                                        new NewRolePage(roleServiceName).setReturnPage(getPage()));
                            }
                        });
        add.setVisible(canCreateStore);

        // the removal button
        h.add(
                removal =
                        new SelectionRoleRemovalLink(
                                roleServiceName, "removeSelected", roles, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        removal.setVisible(canCreateStore);
    }

    protected GeoServerRoleService getService() {
        try {
            return GeoServerApplication.get().getSecurityManager().loadRoleService(roleServiceName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //    AjaxLink addRoleLink() {
    //        return new AjaxLink("addRole", new Model()) {
    //
    //            @Override
    //            public void onClick(AjaxRequestTarget target) {
    //                setResponsePage(new NewRolePage());
    //            }
    //
    //        };
    //    }

    @SuppressWarnings("unchecked")
    Component editRoleLink(String id, IModel itemModel, Property<GeoServerRole> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditRolePage(roleServiceName, (GeoServerRole) getDefaultModelObject())
                                .setReturnPage(getPage()));
            }
        };
    }

    @SuppressWarnings("unchecked")
    Component editParentRoleLink(String id, IModel itemModel, Property<GeoServerRole> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                GeoServerRole role = (GeoServerRole) getDefaultModelObject();
                GeoServerRole parentRole;
                try {
                    parentRole =
                            GeoServerApplication.get()
                                    .getSecurityManager()
                                    .loadRoleService(roleServiceName)
                                    .getParentRole(role);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                setResponsePage(
                        new EditRolePage(roleServiceName, parentRole).setReturnPage(getPage()));
            }
        };
    }

    @Override
    protected void onBeforeRender() {
        roles.clearSelection();
        removal.setEnabled(false);
        super.onBeforeRender();
    }
}
