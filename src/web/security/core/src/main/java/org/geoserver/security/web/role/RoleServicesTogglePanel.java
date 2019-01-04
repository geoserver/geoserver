/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.web.SecurityNamedServicesTogglePanel;

public class RoleServicesTogglePanel
        extends SecurityNamedServicesTogglePanel<SecurityRoleServiceConfig> {

    public RoleServicesTogglePanel(String id) {
        super(id, new RoleServiceConfigListModel());
    }

    @Override
    protected ContentPanel createPanel(String id, IModel<SecurityRoleServiceConfig> config) {
        return new RolesPanel(id, config);
    }

    static class RolesPanel extends ContentPanel<SecurityRoleServiceConfig> {

        public RolesPanel(String id, IModel<SecurityRoleServiceConfig> model) {
            super(id, model);

            add(
                    new RolePanel("roles", model.getObject().getName())
                            .setHeaderVisible(true)
                            .setPagersVisible(false, true));
        }
    }
}
