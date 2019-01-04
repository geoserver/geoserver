/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.web.GeoServerApplication;

public class GroupsModel extends LoadableDetachableModel<List<GeoServerUserGroup>> {

    String userGroupServiceName;

    public GroupsModel(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    @Override
    protected List<GeoServerUserGroup> load() {
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        try {
            return new ArrayList(secMgr.loadUserGroupService(userGroupServiceName).getUserGroups());
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }
    }
}
