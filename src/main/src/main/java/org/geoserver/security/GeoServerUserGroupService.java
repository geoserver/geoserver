/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.PasswordValidator;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * This interface is an extenstion to {@link UserDetailsService}
 *
 * <p>A class implementing this interface implements a read only backend for user and group
 * management
 *
 * @author christian
 */
public interface GeoServerUserGroupService extends GeoServerSecurityService, UserDetailsService {

    /**
     * Creates the user group store that corresponds to this service, or null if creating a store is
     * not supported.
     *
     * <p>Implementations that do not support a store should ensure that {@link #canCreateStore()}
     * returns <code>false</code>.
     */
    GeoServerUserGroupStore createStore() throws IOException;

    /** Register for notifications on load */
    void registerUserGroupLoadedListener(UserGroupLoadedListener listener);

    /** Unregister for notifications on store/load */
    void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener);

    /**
     * Returns the the group object, null if not found
     *
     * @return null if group not found
     */
    GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException;

    /**
     * Returns the the user object, null if not found
     *
     * @return null if user not found
     */
    GeoServerUser getUserByUsername(String username) throws IOException;

    /** Create a user object. Implementations can use subclasses of {@link GeoServerUser} */
    GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException;

    /**
     * Create a user object. Implementations can use classes implementing {@link GeoServerUserGroup}
     */
    GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled) throws IOException;

    /**
     * Returns the list of users.
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUser> getUsers() throws IOException;

    /**
     * Returns the list of GeoserverUserGroups.
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUserGroup> getUserGroups() throws IOException;

    /**
     * get users for a group
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException;

    /**
     * get the groups for a user, an implementation not supporting user groups returns an empty
     * collection
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException;

    /** load from backendstore. On success, a {@link UserGroupLoadedEvent} should be triggered */
    void load() throws IOException;

    /**
     * @return the Spring name of the {@link GeoServerPasswordEncoder} object. mandatory, default is
     *     {@link GeoServerDigestPasswordEncoder#BeanName}.
     */
    String getPasswordEncoderName();

    /**
     * @return the name of the {@link PasswordValidator} object. mandatory, default is {@link
     *     PasswordValidator#DEFAULT_NAME} Validators can be loaded using {@link
     *     GeoServerSecurityManager#loadPasswordValidator(String)}
     */
    String getPasswordValidatorName();

    /** @return the number of users */
    int getUserCount() throws IOException;

    /** @return the number of groups */
    int getGroupCount() throws IOException;

    /** Returns a set of {@link GeoServerUser} objects having the specified property */
    SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException;

    /** Returns the number of {@link GeoServerUser} objects having the specified property */
    int getUserCountHavingProperty(String propname) throws IOException;

    /** Returns a set of {@link GeoServerUser} objects NOT having the specified property */
    SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException;

    /** Returns the number of {@link GeoServerUser} objects NOT having the specified property */
    int getUserCountNotHavingProperty(String propname) throws IOException;

    /**
     * Returns a set of {@link GeoServerUser} objects having the property with the specified value
     */
    SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException;

    /**
     * Returns the number of {@link GeoServerUser} objects having the property with the specified
     * value
     */
    int getUserCountHavingPropertyValue(String propname, String propvalue) throws IOException;
}
