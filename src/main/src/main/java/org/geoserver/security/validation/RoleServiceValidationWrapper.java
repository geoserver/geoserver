/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import static org.geoserver.security.validation.RoleServiceException.ADMIN_ROLE_NOT_REMOVABLE_$1;
import static org.geoserver.security.validation.RoleServiceException.ALREADY_EXISTS;
import static org.geoserver.security.validation.RoleServiceException.ALREADY_EXISTS_IN;
import static org.geoserver.security.validation.RoleServiceException.CANNOT_CHECK_ROLE_IN_SERVICE;
import static org.geoserver.security.validation.RoleServiceException.GROUPNAME_NOT_FOUND_$1;
import static org.geoserver.security.validation.RoleServiceException.GROUPNAME_REQUIRED;
import static org.geoserver.security.validation.RoleServiceException.GROUP_ADMIN_ROLE_NOT_REMOVABLE_$1;
import static org.geoserver.security.validation.RoleServiceException.NAME_REQUIRED;
import static org.geoserver.security.validation.RoleServiceException.NOT_FOUND;
import static org.geoserver.security.validation.RoleServiceException.RESERVED_NAME;
import static org.geoserver.security.validation.RoleServiceException.ROLE_IN_USE_$2;
import static org.geoserver.security.validation.RoleServiceException.USERNAME_NOT_FOUND_$1;
import static org.geoserver.security.validation.RoleServiceException.USERNAME_REQUIRED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * This class is a validation wrapper for {@link GeoServerRoleService}
 *
 * <p>Usage: <code>
 * GeoserverRoleService valService = new RoleServiceValidationWrapper(service);
 * valService.getRoles();
 * </code> Since the {@link GeoServerRoleService} interface does not allow to throw {@link
 * RoleServiceException} objects directly, these objects a wrapped into an IOException. Use {@link
 * IOException#getCause()} to get the proper exception.
 *
 * @author christian
 */
public class RoleServiceValidationWrapper extends AbstractSecurityValidator
        implements GeoServerRoleService {

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    protected GeoServerRoleService service;
    protected GeoServerUserGroupService[] services;
    protected boolean checkAgainstRules;

    /**
     * Creates a wrapper object. If checkAgainstRules is true, no roles used in rules can be removed
     *
     * <p>Optionally, {@link GeoServerUserGroupService} objects can be passed if validation of user
     * names and group names is required
     */
    public RoleServiceValidationWrapper(
            GeoServerRoleService service,
            boolean checkAgainstRules,
            GeoServerUserGroupService... services) {
        super(service.getSecurityManager());
        this.service = service;
        this.services = services;
        this.checkAgainstRules = checkAgainstRules;
    }

    /** Construct a wrapper without checking againset rules */
    public RoleServiceValidationWrapper(
            GeoServerRoleService service, GeoServerUserGroupService... services) {
        this(service, false, services);
    }

    public GeoServerRoleService getWrappedService() {
        return service;
    }

    /**
     * Checks if a user name is valid if this validator was constructed with {@link
     * GeoServerUserGroupService} objects, a cross check is done
     */
    protected void checkValidUserName(String userName) throws IOException {
        if (isNotEmpty(userName) == false) throw createSecurityException(USERNAME_REQUIRED);

        if (services.length == 0) return;
        for (GeoServerUserGroupService service : services) {
            if (service.getUserByUsername(userName) != null) return;
        }
        throw createSecurityException(USERNAME_NOT_FOUND_$1, userName);
    }

    /**
     * Prevents removal of a role used by access rules Only checks if {@link #checkAgainstRules} is
     * <code>true</code>
     */
    public void checkRoleIsUsed(GeoServerRole role) throws IOException {

        if (checkAgainstRules == false) return;

        GeoServerSecurityManager secMgr = getSecurityManager();

        List<String> keys = new ArrayList<String>();
        for (ServiceAccessRule rule :
                secMgr.getServiceAccessRuleDAO().getRulesAssociatedWithRole(role.getAuthority()))
            keys.add(rule.getKey());
        for (DataAccessRule rule :
                secMgr.getDataAccessRuleDAO().getRulesAssociatedWithRole(role.getAuthority()))
            keys.add(rule.getKey());

        if (keys.size() > 0) {
            String ruleString = StringUtils.collectionToCommaDelimitedString(keys);
            throw createSecurityException(ROLE_IN_USE_$2, role.getAuthority(), ruleString);
        }
    }

    /**
     * Checks if the roles is mapped to a system role, see
     *
     * <p>{@link SecurityRoleServiceConfig#getAdminRoleName()} {@link
     * SecurityRoleServiceConfig#getGroupAdminRoleName()}
     */
    public void checkRoleIsMapped(GeoServerRole role) throws IOException {
        GeoServerRole mappedRole = service.getAdminRole();
        if (mappedRole != null && mappedRole.equals(role))
            throw createSecurityException(ADMIN_ROLE_NOT_REMOVABLE_$1, role.getAuthority());
        mappedRole = service.getGroupAdminRole();
        if (mappedRole != null && mappedRole.equals(role))
            throw createSecurityException(GROUP_ADMIN_ROLE_NOT_REMOVABLE_$1, role.getAuthority());
    }

    /**
     * Checks if a group name is valid if this validator was constructed with {@link
     * GeoServerUserGroupService} objects, a cross check is done
     */
    protected void checkValidGroupName(String groupName) throws IOException {
        if (isNotEmpty(groupName) == false) throw createSecurityException(GROUPNAME_REQUIRED);

        if (services.length == 0) return;
        for (GeoServerUserGroupService service : services) {
            if (service.getGroupByGroupname(groupName) != null) return;
        }
        throw createSecurityException(GROUPNAME_NOT_FOUND_$1, groupName);
    }

    protected void checkRoleName(String roleName) throws IOException {
        if (isNotEmpty(roleName) == false) throw createSecurityException(NAME_REQUIRED);
    }

    protected void checkExistingRoleName(String roleName) throws IOException {
        checkRoleName(roleName);
        if (service.getRoleByName(roleName) == null)
            throw createSecurityException(NOT_FOUND, roleName);
    }

    protected void checkReservedNames(String roleName) throws IOException {
        for (GeoServerRole systemRole : GeoServerRole.SystemRoles) {
            if (systemRole.getAuthority().equals(roleName))
                throw createSecurityException(RESERVED_NAME, roleName);
        }
    }

    protected void checkNotExistingInOtherServices(String roleName) throws IOException {
        checkRoleName(roleName);
        for (String serviceName : service.getSecurityManager().listRoleServices()) {
            // dont check myself
            if (service.getName().equals(serviceName)) continue;
            GeoServerRole role = null;
            try {
                role =
                        service.getSecurityManager()
                                .loadRoleService(serviceName)
                                .getRoleByName(roleName);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                throw createSecurityException(CANNOT_CHECK_ROLE_IN_SERVICE, roleName, serviceName);
            }
            if (role != null) {
                throw createSecurityException(ALREADY_EXISTS_IN, roleName, serviceName);
            }
        }
    }

    protected void checkNotExistingRoleName(String roleName) throws IOException {
        checkRoleName(roleName);
        if (service.getRoleByName(roleName) != null)
            throw createSecurityException(ALREADY_EXISTS, roleName);
    }

    // start wrapper methods

    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        service.initializeFromConfig(config);
    }

    public boolean canCreateStore() {
        return service.canCreateStore();
    }

    public GeoServerRoleStore createStore() throws IOException {
        return service.createStore();
    }

    public String getName() {
        return service.getName();
    }

    public void setName(String name) {
        service.setName(name);
    }

    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        service.setSecurityManager(securityManager);
    }

    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        service.registerRoleLoadedListener(listener);
    }

    public GeoServerSecurityManager getSecurityManager() {
        return service.getSecurityManager();
    }

    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        service.unregisterRoleLoadedListener(listener);
    }

    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        checkExistingRoleName(role.getAuthority());
        return service.getGroupNamesForRole(role);
    }

    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        checkExistingRoleName(role.getAuthority());
        return service.getUserNamesForRole(role);
    }

    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        checkValidUserName(username);
        return service.getRolesForUser(username);
    }

    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        checkValidGroupName(groupname);
        return service.getRolesForGroup(groupname);
    }

    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return service.getRoles();
    }

    public Map<String, String> getParentMappings() throws IOException {
        return service.getParentMappings();
    }

    public GeoServerRole createRoleObject(String role) throws IOException {
        checkRoleName(role);
        return service.createRoleObject(role);
    }

    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        checkExistingRoleName(role.getAuthority());
        return service.getParentRole(role);
    }

    public GeoServerRole getRoleByName(String role) throws IOException {
        return service.getRoleByName(role);
    }

    public void load() throws IOException {
        service.load();
    }

    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return service.personalizeRoleParams(roleName, roleParams, userName, userProps);
    }

    public GeoServerRole getAdminRole() {
        return service.getAdminRole();
    }

    public GeoServerRole getGroupAdminRole() {
        return service.getGroupAdminRole();
    }

    public int getRoleCount() throws IOException {
        return service.getRoleCount();
    }

    /** Helper method for creating a proper {@link SecurityConfigException} object */
    protected IOException createSecurityException(String errorid, Object... args) {
        RoleServiceException ex = new RoleServiceException(errorid, args);
        return new IOException("Details are in the nested exception", ex);
    }
}
