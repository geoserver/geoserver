/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.geoserver.security.validation.UserGroupServiceException.USER_IN_OTHER_GROUP_NOT_MODIFIABLE_$1;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.UserGroupServiceException;

/**
 * User group service wrapper that filters contents based on an authenticated group administrator.
 *
 * <p>This wrapper filters out the administrative roles {@link GeoServerRole#ADMIN_ROLE} and {@link
 * GeoServerRole#GROUP_ADMIN_ROLE}. It also forces read-only access to the role store.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GroupAdminUserGroupService extends AuthorizingUserGroupService {

    List<String> groups;

    GroupAdminUserGroupService(GeoServerUserGroupService delegate, List<String> groups) {
        super(delegate);
        this.groups = groups;
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        return new GroupAdminUserGroupService(delegate.createStore(), groups);
    }

    @Override
    public int getGroupCount() throws IOException {
        return groups.size();
    }

    @Override
    protected GeoServerUser filterUser(GeoServerUser user) {
        return user;
    }

    @Override
    protected GeoServerUserGroup filterGroup(GeoServerUserGroup group) {
        if (groups.contains(group.getGroupname())) {
            return group;
        }
        return null;
    }

    @Override
    public boolean removeUser(GeoServerUser user) throws IOException {
        checkUserNotInOtherGroup(user);
        return super.removeUser(user);
    }

    @Override
    public void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        checkUserNotInOtherGroup(user);
        super.updateUser(user);
    }

    void checkUserNotInOtherGroup(GeoServerUser user) throws IOException {
        SortedSet<GeoServerUserGroup> userGroups = delegate.getGroupsForUser(user);
        if (userGroups.isEmpty()) {
            return;
        }

        for (GeoServerUserGroup userGroup : userGroups) {
            if (!groups.contains(userGroup.getGroupname())) {
                String msg =
                        new UserGroupServiceException(
                                        USER_IN_OTHER_GROUP_NOT_MODIFIABLE_$1, new Object[] {user})
                                .getMessage();
                throw new IOException(msg);
            }
        }
    }
}
