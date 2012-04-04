/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.springframework.util.StringUtils;

import static org.geoserver.security.validation.RoleServiceException.*;

/**
 * 
 * This class is a validation wrapper for {@link GeoServerRoleService}
 * 
 * Usage:
 * <code>
 * GeoserverRoleService valService = new RoleServiceValidationWrapper(service);
 * valService.getRoles();
 * </code>
 * 
 * Since the {@link GeoServerRoleService} interface does not allow to 
 * throw {@link RoleServiceException} objects directly, these objects
 * a wrapped into an IOException. Use {@link IOException#getCause()} to
 * get the proper exception.
 * 
 * 
 * @author christian
 *
 */
public class RoleServiceValidationWrapper extends AbstractSecurityValidator implements GeoServerRoleService{

    protected GeoServerRoleService service;
    protected GeoServerUserGroupService[] services;
    protected boolean checkAgainstRules;
    
    /**
     * Creates a wrapper object. If  checkAgainstRules is true, no 
     * roles used in rules can be removed
     * 
     * Optionally, {@link GeoServerUserGroupService} objects
     * can be passed if validation of user names and group names is required
     * 
     * @param service
     * @param checkAgainstRules 
     * @param services
     */    
    public RoleServiceValidationWrapper(GeoServerRoleService service, boolean checkAgainstRules,
            GeoServerUserGroupService ...services) {
        super(service.getSecurityManager());
        this.service=service;
        this.services=services;
        this.checkAgainstRules=checkAgainstRules;
    }
    
    /**
     * Construct a wrapper without checking againset rules
     * @param service
     * @param services
     */
    public RoleServiceValidationWrapper(GeoServerRoleService service,
            GeoServerUserGroupService ...services) {
        this(service, false, services);
    }


    public GeoServerRoleService getWrappedService() {
        return service;
    }
    
    /**
     * Checks if a user name is valid
     * if this validator was constructed with {@link GeoServerUserGroupService}
     * objects, a cross check is done 
     * 
     * @param userName
     * @throws RoleServiceException
     */
    protected void checkValidUserName(String userName) throws IOException{
        if (isNotEmpty(userName)==false)
            throw createSecurityException(ROLE_ERR_04);
        
        if (services.length==0) return;
        for (GeoServerUserGroupService service : services) {
            if (service.getUserByUsername(userName)!=null)
                return;
        }
        throw createSecurityException(ROLE_ERR_06,userName);
    }
    
    /**
     * Prevents the removal of the admin and group admin roles.
     * 
     * @param role
     * @throws IOException
     */
    public void checkRemovalOfAdminRole(GeoServerRole role) throws IOException {
        if (getAdminRole() != null && role.getAuthority().equals(getAdminRole().getAuthority())) {
            throw createSecurityException(ROLE_ERR_08,role.getAuthority());
        }
        if (getGroupAdminRole() != null 
            && role.getAuthority().equals(getGroupAdminRole().getAuthority())) {
            throw createSecurityException(GROUP_ADMIN_ROLE_NOT_REMOVABLE_$1, role.getAuthority());
        }
    }

    /**
     * Prevents removal of a role used by access rules
     * Only checks if {@link #checkAgainstRules} is 
     * <code>true</code>
     * 
     * @param role
     * @throws IOException
     */
    public void checkRoleIsUsed(GeoServerRole role) throws IOException {
        
        if (checkAgainstRules==false)
            return;
        
        List<String> keys = new ArrayList<String>();
        for (ServiceAccessRule rule : ServiceAccessRuleDAO.get().getRulesAssociatedWithRole(role.getAuthority()))
            keys.add(rule.getKey());
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRulesAssociatedWithRole(role.getAuthority()))
            keys.add(rule.getKey());
        
        if (keys.size()>0) {
            String ruleString = StringUtils.collectionToCommaDelimitedString(keys);    
            throw createSecurityException(ROLE_ERR_09, role.getAuthority(),ruleString);
        }
    }
    
    
    /**
     * Checks if a group name is valid
     * if this validator was constructed with {@link GeoServerUserGroupService}
     * objects, a cross check is done 
     * 
     * @param groupName
     * @throws RoleServiceException
     */
    protected void checkValidGroupName(String groupName) throws  IOException{
        if (isNotEmpty(groupName)==false)
            throw createSecurityException(ROLE_ERR_05);
        
        if (services.length==0) return;
        for (GeoServerUserGroupService service : services) {
            if (service.getGroupByGroupname(groupName)!=null)
                return;
        }
        throw createSecurityException(ROLE_ERR_07,groupName);
    }

    protected void checkRoleName(String roleName) throws IOException{
        if (isNotEmpty(roleName)==false)
            throw createSecurityException(ROLE_ERR_01);        
    }
    
    protected void checkExistingRoleName(String roleName) throws IOException{
        checkRoleName(roleName);
        if (service.getRoleByName(roleName)==null)
            throw createSecurityException(ROLE_ERR_02,roleName);
    }
    
    protected void checkNotExistingRoleName(String roleName) throws IOException{
        checkRoleName(roleName);
        if (service.getRoleByName(roleName)!=null)
            throw createSecurityException(ROLE_ERR_03,roleName);
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


    public Properties personalizeRoleParams(String roleName, Properties roleParams,
            String userName, Properties userProps) throws IOException {
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
    
    /**
     * Helper method for creating a proper
     * {@link SecurityConfigException} object
     * 
     * @param errorid
     * @param args
     * @return
     */
    protected IOException createSecurityException (String errorid, Object ...args) {
        RoleServiceException ex =  new RoleServiceException(errorid,args);
        return new IOException("Details are in the nested exception",ex);
    }
        
}
