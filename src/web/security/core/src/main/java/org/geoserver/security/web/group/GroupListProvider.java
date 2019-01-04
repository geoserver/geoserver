/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Page listing for {@link GeoServerUserGroup} objects
 *
 * @author christian
 */
@SuppressWarnings("serial")
public class GroupListProvider extends GeoServerDataProvider<GeoServerUserGroup> {

    public static final Property<GeoServerUserGroup> GROUPNAME =
            new BeanProperty<GeoServerUserGroup>("groupname", "groupname");
    public static final Property<GeoServerUserGroup> ENABLED =
            new BeanProperty<GeoServerUserGroup>("enabled", "enabled");
    protected String userGroupServiceName;

    public GroupListProvider(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    @Override
    protected List<GeoServerUserGroup> getItems() {
        SortedSet<GeoServerUserGroup> groups = null;
        try {
            GeoServerUserGroupService service = null;
            if (userGroupServiceName != null)
                service =
                        getApplication()
                                .getSecurityManager()
                                .loadUserGroupService(userGroupServiceName);

            if (service == null) groups = new TreeSet<GeoServerUserGroup>();
            else groups = service.getUserGroups();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<GeoServerUserGroup> groupList = new ArrayList<GeoServerUserGroup>();
        groupList.addAll(groups);
        return groupList;
    }

    @Override
    protected List<Property<GeoServerUserGroup>> getProperties() {
        List<Property<GeoServerUserGroup>> result =
                new ArrayList<GeoServerDataProvider.Property<GeoServerUserGroup>>();
        result.add(GROUPNAME);
        result.add(ENABLED);
        return result;
    }
}
