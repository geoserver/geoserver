/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.GeoServerRestRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanel;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleServicePanel
        extends RoleServicePanel<GeoServerRestRoleServiceConfig> {

    /** serialVersionUID */
    private static final long serialVersionUID = -6288298408680657130L;

    public GeoServerRestRoleServicePanel(String id, IModel<GeoServerRestRoleServiceConfig> model) {
        super(id, model);

        add(new TextField<String>("baseUrl").setRequired(true));
        add(new TextField<String>("rolesRESTEndpoint").setRequired(true));
        add(new TextField<String>("adminRoleRESTEndpoint").setRequired(true));
        add(new TextField<String>("usersRESTEndpoint").setRequired(true));
        add(new TextField<String>("rolesJSONPath").setRequired(true));
        add(new TextField<String>("adminRoleJSONPath").setRequired(true));
        add(new TextField<String>("usersJSONPath").setRequired(true));
        add(new TextField<Integer>("cacheConcurrencyLevel").setRequired(true));
        add(new TextField<Long>("cacheMaximumSize").setRequired(true));
        add(new TextField<Long>("cacheExpirationTime").setRequired(true));
        add(new TextField<String>("authApiKey").setRequired(false));
    }
}
