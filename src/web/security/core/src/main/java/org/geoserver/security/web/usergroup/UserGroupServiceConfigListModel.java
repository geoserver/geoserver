/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * Model for list of user group service configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupServiceConfigListModel
        extends LoadableDetachableModel<List<SecurityUserGroupServiceConfig>> {

    @Override
    protected List<SecurityUserGroupServiceConfig> load() {
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        List<SecurityUserGroupServiceConfig> configs = new ArrayList();
        try {
            for (String ugServiceName : secMgr.listUserGroupServices()) {
                SecurityUserGroupServiceConfig config =
                        secMgr.loadUserGroupServiceConfig(ugServiceName);
                configs.add(config);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return configs;
    }
}
