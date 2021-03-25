/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import static org.geoserver.security.validation.UserGroupServiceException.GROUPNAME_REQUIRED;
import static org.geoserver.security.validation.UserGroupServiceException.GROUP_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.UserGroupServiceException.GROUP_NOT_FOUND_$1;
import static org.geoserver.security.validation.UserGroupServiceException.USERNAME_REQUIRED;
import static org.geoserver.security.validation.UserGroupServiceException.USER_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.UserGroupServiceException.USER_NOT_FOUND_$1;

import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * This class is a validation wrapper for {@link GeoServerUserGroupService}
 *
 * <p>Usage: <code>
 * GeoserverUserGroupService valService = new UserGroupServiceValidationWrapper(service);
 * valService.getUsers()
 *
 * </code> Since the {@link GeoServerUserGroupService} interface does not allow to throw {@link
 * UserGroupServiceException} objects directly, these objects a wrapped into an IOException. Use
 * {@link IOException#getCause()} to get the proper exception.
 *
 * @author christian
 */
public class UserGroupServiceValidationWrapper extends AbstractSecurityValidator
        implements GeoServerUserGroupService {

    protected GeoServerUserGroupService service;

    /** Creates a wrapper object. */
    public UserGroupServiceValidationWrapper(GeoServerUserGroupService service) {
        super(service.getSecurityManager());
        this.service = service;
    }

    public GeoServerUserGroupService getWrappedService() {
        return service;
    }

    protected void checkUserName(String userName) throws IOException {
        if (isNotEmpty(userName) == false) throw createSecurityException(USERNAME_REQUIRED);
    }

    protected void checkGroupName(String groupName) throws IOException {
        if (isNotEmpty(groupName) == false) throw createSecurityException(GROUPNAME_REQUIRED);
    }

    protected void checkExistingUserName(String userName) throws IOException {
        checkUserName(userName);
        if (service.getUserByUsername(userName) == null)
            throw createSecurityException(USER_NOT_FOUND_$1, userName);
    }

    protected void checkExistingGroupName(String groupName) throws IOException {
        checkGroupName(groupName);
        if (service.getGroupByGroupname(groupName) == null)
            throw createSecurityException(GROUP_NOT_FOUND_$1, groupName);
    }

    protected void checkNotExistingUserName(String userName) throws IOException {
        checkUserName(userName);
        if (service.getUserByUsername(userName) != null)
            throw createSecurityException(USER_ALREADY_EXISTS_$1, userName);
    }

    protected void checkNotExistingGroupName(String groupName) throws IOException {
        checkGroupName(groupName);
        if (service.getGroupByGroupname(groupName) != null)
            throw createSecurityException(GROUP_ALREADY_EXISTS_$1, groupName);
    }

    // start wrapper methods

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        service.initializeFromConfig(config);
    }

    @Override
    public boolean canCreateStore() {
        return service.canCreateStore();
    }

    @Override
    public String getName() {
        return service.getName();
    }

    @Override
    public void setName(String name) {
        service.setName(name);
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        service.setSecurityManager(securityManager);
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        return service.createStore();
    }

    @Override
    public GeoServerSecurityManager getSecurityManager() {
        return service.getSecurityManager();
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        return service.loadUserByUsername(username);
    }

    @Override
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        service.registerUserGroupLoadedListener(listener);
    }

    @Override
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        service.unregisterUserGroupLoadedListener(listener);
    }

    @Override
    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        return service.getGroupByGroupname(groupname);
    }

    @Override
    public GeoServerUser getUserByUsername(String username) throws IOException {
        return service.getUserByUsername(username);
    }

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        return service.createUserObject(username, password, isEnabled);
    }

    @Override
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        return service.createGroupObject(groupname, isEnabled);
    }

    @Override
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        return service.getUsers();
    }

    @Override
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        return service.getUserGroups();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        checkExistingGroupName(group.getGroupname());
        return service.getUsersForGroup(group);
    }

    @Override
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        checkExistingUserName(user.getUsername());
        return service.getGroupsForUser(user);
    }

    @Override
    public void load() throws IOException {
        service.load();
    }

    @Override
    public String getPasswordEncoderName() {
        return service.getPasswordEncoderName();
    }

    @Override
    public String getPasswordValidatorName() {
        return service.getPasswordValidatorName();
    }

    @Override
    public int getUserCount() throws IOException {
        return service.getUserCount();
    }

    @Override
    public int getGroupCount() throws IOException {
        return service.getGroupCount();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        return service.getUsersHavingProperty(propname);
    }

    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        return service.getUserCountHavingProperty(propname);
    }

    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        return service.getUsersNotHavingProperty(propname);
    }

    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        return service.getUserCountNotHavingProperty(propname);
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return service.getUsersHavingPropertyValue(propname, propvalue);
    }

    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return service.getUserCountHavingPropertyValue(propname, propvalue);
    }

    /** Helper method for creating a proper {@link SecurityConfigException} object */
    protected IOException createSecurityException(String errorid, Object... args) {
        UserGroupServiceException ex = new UserGroupServiceException(errorid, args);
        return new IOException("Details are in the nested excetpion", ex);
    }
}
