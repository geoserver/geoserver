/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.web.role.RoleServicesPanel;
import org.geoserver.security.web.role.RoleServicesTogglePanel;
import org.geoserver.security.web.usergroup.UserGroupServicesPanel;
import org.geoserver.security.web.usergroup.UserGroupServicesTogglePanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.wicket.HelpLink;

/**
 * Main menu page for user, group, and role services.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupRoleServicesPage extends AbstractSecurityPage {

    public UserGroupRoleServicesPage() {
        // add(new ServicesPanel("panel"));
        List<ITab> tabs = new ArrayList();
        tabs.add(
                new AbstractTab(new StringResourceModel("services", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new ServicesPanel(panelId);
                    }
                });
        tabs.add(
                new AbstractTab(new StringResourceModel("usersgroups", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new UsersGroupsPanel(panelId);
                    }
                });
        tabs.add(
                new AbstractTab(new StringResourceModel("roles", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new RolesPanel(panelId);
                    }
                });
        add(new TabbedPanel("panel", tabs));
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return new GroupAdminComponentAuthorizer();
    }

    class ServicesPanel extends Panel {

        public ServicesPanel(String id) {
            super(id);

            add(new UserGroupServicesPanel("userGroupServices"));
            add(new HelpLink("userGroupServicesHelp").setDialog(dialog));

            add(new RoleServicesPanel("roleServices"));
            add(new HelpLink("roleServicesHelp").setDialog(dialog));
        }
    }

    class UsersGroupsPanel extends Panel {

        public UsersGroupsPanel(String id) {
            super(id);

            add(new UserGroupServicesTogglePanel("usersgroups"));
        }
    }

    class RolesPanel extends Panel {

        public RolesPanel(String id) {
            super(id);

            add(new RoleServicesTogglePanel("roles"));
        }
    }
}
