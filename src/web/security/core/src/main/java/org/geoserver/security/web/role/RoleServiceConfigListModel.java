/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * Model for list of role services configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RoleServiceConfigListModel
        extends LoadableDetachableModel<List<SecurityRoleServiceConfig>> {

    @Override
    protected List<SecurityRoleServiceConfig> load() {
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        List<SecurityRoleServiceConfig> configs = new ArrayList();
        try {
            for (String roleServiceName : secMgr.listRoleServices()) {
                SecurityRoleServiceConfig config = secMgr.loadRoleServiceConfig(roleServiceName);
                configs.add(config);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return configs;
    }
}
