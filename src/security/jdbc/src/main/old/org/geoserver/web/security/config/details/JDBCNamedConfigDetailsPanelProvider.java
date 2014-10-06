/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.security.config.details;

import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.security.jdbc.JDBCConnectAuthProvider;
import org.geoserver.security.jdbc.JDBCRoleService;
import org.geoserver.security.jdbc.JDBCUserGroupService;
import org.geoserver.web.security.config.SecurityNamedConfigModelHelper;

/**
 * 
 * JDBC implementation of {@link NamedConfigDetailsPanelProvider}
 * 
 * @author christian
 *
 */
public class JDBCNamedConfigDetailsPanelProvider implements NamedConfigDetailsPanelProvider {

    @Override
    public AbstractNamedConfigDetailsPanel getDetailsPanel(String className, String id,
            CompoundPropertyModel<SecurityNamedConfigModelHelper> model) {
        
        if (JDBCUserGroupService.class.getName().equals(className))
            return new JDBCUserGroupConfigDetailsPanel(id,model);
        if (JDBCRoleService.class.getName().equals(className))
            return new JDBCRoleConfigDetailsPanel(id, model);
        if (JDBCConnectAuthProvider.class.getName().equals(className))
            return new JDBCAuthProviderConfigDetailsPanel(id, model);
             
        return null;
    }

}
