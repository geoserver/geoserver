/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.springframework.util.StringUtils;

/** */
public class UserPropertyAuthenticationKeyMapper extends AbstractAuthenticationKeyMapper {

    private String userPropertyName;

    public String getUserPropertyName() {
        return userPropertyName;
    }

    public void setUserPropertyName(String userPropertyName) {
        this.userPropertyName = userPropertyName;
    }

    @Override
    protected void checkProperties() throws IOException {
        super.checkProperties();
        if (StringUtils.hasLength(getUserPropertyName()) == false) {
            throw new IOException("User property name is unset");
        }
    }

    public boolean supportsReadOnlyUserGroupService() {
        return false;
    }

    @Override
    public GeoServerUser getUser(String key) throws IOException {
        checkProperties();

        SortedSet<GeoServerUser> set =
                getUserGroupService().getUsersHavingPropertyValue(getUserPropertyName(), key);
        if (set.isEmpty()) return null;

        if (set.size() > 1) {
            StringBuffer buff = new StringBuffer();
            for (GeoServerUser user : set) {
                buff.append(user.getUsername()).append(",");
            }
            buff.setLength(buff.length() - 1);
            throw new IOException(
                    "More than one user have auth key: "
                            + key
                            + ". Problematic users :"
                            + buff.toString());
        }

        GeoServerUser user = set.first();
        if (user.isEnabled() == false) {
            LOGGER.info(
                    "Found user "
                            + user.getUsername()
                            + " for key "
                            + key
                            + ", but this user is disabled");
            return null;
        }
        return (GeoServerUser) getUserGroupService().loadUserByUsername(user.getUsername());
    }

    @Override
    public synchronized int synchronize() throws IOException {
        checkProperties();
        GeoServerUserGroupService service = getUserGroupService();
        if (service.canCreateStore() == false)
            throw new IOException("Cannot synchronize a read only user group service");

        int counter = 0;
        GeoServerUserGroupStore store = service.createStore();
        store.load();
        for (GeoServerUser user : store.getUsers()) {
            String value = user.getProperties().getProperty(getUserPropertyName());
            if (StringUtils.hasLength(value) == false) {
                user.getProperties().put(getUserPropertyName(), createAuthKey());
                try {
                    store.updateUser(user);
                    counter++;
                } catch (PasswordPolicyException e) {
                    throw new IOException("Never should reach this point", e);
                }
            }
        }
        store.store();
        return counter;
    }
}
