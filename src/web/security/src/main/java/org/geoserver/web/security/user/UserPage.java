/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.SelectionUserRemovalLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * A page listing users, allowing for removal, addition and linking to an edit page
 */
@SuppressWarnings("serial")
public class UserPage extends GeoServerSecuredPage {

    private GeoServerTablePanel<User> users;
    GeoServerDialog dialog;
    private SelectionUserRemovalLink removal;

    public UserPage() {
        UserListProvider provider = new UserListProvider();
        add(users = new GeoServerTablePanel<User>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<User> property) {
                if (property == UserListProvider.USERNAME) {
                    return editUserLink(id, itemModel, property);
                } else if (property == UserListProvider.ROLES) {
                    return new Label(id, property.getModel(itemModel));
                } else if (property == UserListProvider.ADMIN) {
                    return new Label(id, property.getModel(itemModel));
                }
                throw new RuntimeException("Uknown property " + property);
            }
            
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(users.getSelection().size() > 0);               
                target.addComponent(removal);
            }

        });
        users.setOutputMarkupId(true);
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());

    }
    
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewUserPage.class));

        // the removal button
        header.add(removal = new SelectionUserRemovalLink("removeSelected", users, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    AjaxLink addUserLink() {
        return new AjaxLink("addUser", new Model()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new NewUserPage());
            }

        };
    }

    Component editUserLink(String id, IModel itemModel, Property<User> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(new EditUserPage((UserDetails) getDefaultModelObject()));
            }

        };
    }

}
