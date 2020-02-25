/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.web.SecurityNamedServicePanel;
import org.geoserver.security.web.SecurityNamedServiceTabbedPanel;

/**
 * Base class for role service panels.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RoleServicePanel<T extends SecurityRoleServiceConfig>
        extends SecurityNamedServicePanel<T> implements SecurityNamedServiceTabbedPanel<T> {

    public RoleServicePanel(String id, IModel<T> model) {
        super(id, model);

        add(new RoleChoice("adminRoleName", model));
        add(new RoleChoice("groupAdminRoleName", model));
    }

    @Override
    public List<ITab> createTabs(final IModel<T> model) {
        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(
                new AbstractTab(new StringResourceModel("roles", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new RolePanel(panelId, model.getObject().getName());
                    }
                });
        return tabs;
    }

    @Override
    public void doSave(T config) throws Exception {
        getSecurityManager().saveRoleService(config);
    }

    public void doLoad(T config) throws Exception {
        getSecurityManager().loadRoleService(config.getName());
    };
}
