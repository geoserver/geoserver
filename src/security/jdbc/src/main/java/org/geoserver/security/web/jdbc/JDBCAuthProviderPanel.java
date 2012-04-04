/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.jdbc.JDBCConnectAuthProvider;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link JDBCConnectAuthProvider}.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCAuthProviderPanel extends AuthenticationProviderPanel<JDBCConnectAuthProviderConfig> {

    public JDBCAuthProviderPanel(String id, IModel<JDBCConnectAuthProviderConfig> model) {
        super(id, model);

        add(new UserGroupServiceChoice("userGroupServiceName"));
        add(new JDBCDriverChoice("driverClassName"));
        add(new TextField("connectURL"));
    }

}
