/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.directory.DirContext;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;

/**
 * LDAP implementation of {@link GeoServerRoleService}
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class LDAPRoleService extends AbstractGeoServerSecurityService implements GeoServerRoleService {

    private static final SortedSet<GeoServerRole> emptySet = Collections
            .unmodifiableSortedSet(new TreeSet<GeoServerRole>());
    
    private static final SortedSet<String> emptyStringSet = Collections
            .unmodifiableSortedSet(new TreeSet<String>());
    
    private static final Map<String, String> emptyMap = Collections.emptyMap();
    
    static Logger LOGGER = Logging.getLogger("org.geoserver.security.ldap");
    protected Set<RoleLoadedListener> listeners = 
        Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    LdapContextSource ldapContext;
    SpringSecurityLdapTemplate template;
    
    // search base for ldap groups that are to be mapped to GeoServer roles
    String groupSearchBase;
    String user, password;
    /**
     * Standard filter for getting all roles bounded to a user
     */
    String groupSearchFilter = "member={0}";
    // attribute of a group containing the membership info
    String groupMembershipAttribute = "member";
    // regex to extract username from membership info
    Pattern userMembershipPattern = Pattern.compile("^(.*)$");
    
    // attribute of a user containing the username (used if userFilter is defined)
    String userNameAttribute = "uid";
    // regex to extract the username from the user info
    Pattern userNamePattern = Pattern.compile("^(.*)$");
    
    String userFilter = null;
    boolean lookupUserForDn = false;
    /**
     * Standard filter for getting all roles
     */
    String allGroupsSearchFilter = "cn=*";
    /**
     * The ID of the attribute which contains the role name for a group
     */
    private String groupRoleAttribute = "cn";
    
    private String rolePrefix = "ROLE_";
    private boolean convertToUpperCase = true;
    
    private String adminGroup;
    private String groupAdminGroup;
    
    Pattern lookForMembershipAttribute = Pattern.compile(
            "^\\(*([a-z]+)=(.*?)\\{([01])\\}(.*?)\\)*$", Pattern.CASE_INSENSITIVE);

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config)
            throws IOException {
        super.initializeFromConfig(config);
        LDAPRoleServiceConfig ldapConfig = (LDAPRoleServiceConfig) config;
    
        ldapContext = LDAPUtils.createLdapContext(ldapConfig);
    
        if (ldapConfig.isBindBeforeGroupSearch()) {
            // authenticate before LDAP searches
            user = ldapConfig.getUser();
            password = ldapConfig.getPassword();
            template = new BindingLdapTemplate(ldapContext);
        } else {
            template = new SpringSecurityLdapTemplate(ldapContext);
        }
    
        this.groupSearchBase = ldapConfig.getGroupSearchBase();
        if (isNotEmpty(ldapConfig.getGroupSearchFilter())) {
            this.groupSearchFilter = ldapConfig.getGroupSearchFilter();
            Matcher m = lookForMembershipAttribute.matcher(groupSearchFilter);
            if (m.matches()) {
                groupMembershipAttribute = m.group(1);
                lookupUserForDn = m.group(3).equals("1");
                userMembershipPattern = Pattern.compile("^"
                        + Pattern.quote(m.group(2)) + "(.*)"
                        + Pattern.quote(m.group(4)) + "$");
            }
        }
        if (isNotEmpty(ldapConfig.getAllGroupsSearchFilter())) {
            this.allGroupsSearchFilter = ldapConfig.getAllGroupsSearchFilter();
        }
        if (isNotEmpty(ldapConfig.getAdminGroup())) {
            this.adminGroup = ldapConfig.getAdminGroup();
        }
        if (isNotEmpty(ldapConfig.getGroupAdminGroup())) {
            this.groupAdminGroup = ldapConfig.getGroupAdminGroup();
        }
        if (isNotEmpty(ldapConfig.getUserFilter())) {
            this.userFilter = ldapConfig.getUserFilter();
            Matcher m = lookForMembershipAttribute.matcher(userFilter);
            if (m.matches()) {
                userNameAttribute = m.group(1);
                userNamePattern = Pattern.compile("^"
                        + Pattern.quote(m.group(2)) + "(.*)"
                        + Pattern.quote(m.group(4)) + "$");
            }
        }
    }

    private boolean isNotEmpty(String property) {
        return property != null
                && !property.isEmpty();
    }

    /**
     * Read only store.
     */
    @Override
    public boolean canCreateStore() {
        return false;
    }

    /**
     * Read only store.
     */
    @Override
    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    /** 
     * @see org.geoserver.security.GeoServerRoleService#registerRoleLoadedListener(RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }


    /** 
     * @see org.geoserver.security.GeoServerRoleService#unregisterRoleLoadedListener(RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Roles to group association is not supported
     */
    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role)
            throws IOException {
        return emptyStringSet;
    }

    @Override
    public SortedSet<String> getUserNamesForRole(final GeoServerRole role)
            throws IOException {
        final SortedSet<String> users = new TreeSet<String>();
        
        authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
            
            @Override
            public void executeWithContext(DirContext ctx,
                    LdapEntryIdentification ldapEntryIdentification) {
                fillUsersForRole(ctx, users, role);
            }
        });

        return Collections.unmodifiableSortedSet(users);
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(final String username)
            throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();
        final Set<String> userDn = new HashSet<String>();
        userDn.add(username);
        if (lookupUserForDn && isNotEmpty(userFilter)) {
            authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
    
                @Override
                public void executeWithContext(DirContext ctx,
                        LdapEntryIdentification ldapEntryIdentification) {
                    try {
                        String dn = LDAPUtils
                                .getLdapTemplateInContext(ctx, template)
                                .searchForSingleEntry("", userFilter,
                                        new String[] { username }).getDn()
                                .toString();
                        userDn.clear();
                        userDn.add(dn);
                    } catch (Exception e) {
                        // not found, let's use username instead
                    }
                }
            });
        }
        authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
    
            @Override
            public void executeWithContext(DirContext ctx,
                    LdapEntryIdentification ldapEntryIdentification) {
                fillRolesForUser(ctx, username, userDn.iterator().next(), roles);
            }
        });
    
        return Collections.unmodifiableSortedSet(roles);
    }

    /**
     * Roles to group association is not supported
     */
    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname)
            throws IOException {
        
        return emptySet;
    }

    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();
        
        authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
            
            @Override
            public void executeWithContext(DirContext ctx,
                    LdapEntryIdentification ldapEntryIdentification) {
                fillAllRoles(ctx, roles);
            }
        });

        return Collections.unmodifiableSortedSet(roles);
    }

    /**
     * Execute authentication, if configured to do so, and then
     * call the given callback on authenticated context, or simply
     * call the given callback if no authentication is needed.
     * 
     * @param callback
     */
    private void authenticateIfNeeded(AuthenticatedLdapEntryContextCallback callback) {
        if (user != null && password != null) {
            template.authenticate(DistinguishedName.EMPTY_PATH, user, password,
                    callback);
        } else {
            callback.executeWithContext(null, null);
        }
    
    }

    private void fillAllRoles(DirContext ctx, SortedSet<GeoServerRole> roles) {
        Set<String> roleNames = LDAPUtils.getLdapTemplateInContext(ctx, template)
                .searchForSingleAttributeValues(groupSearchBase,
                        allGroupsSearchFilter, new String[] {}, groupRoleAttribute);
        addRolesToSet(roles, roleNames);
    }
    
    private void fillUsersForRole(DirContext ctx, SortedSet<String> users,
            GeoServerRole role) {
        DirContextOperations roleObj = LDAPUtils.getLdapTemplateInContext(ctx,
                template).searchForSingleEntry(groupSearchBase, "cn={0}",
                new String[] { role.toString() });
        if (roleObj != null) {
            Object[] usernames = roleObj
                    .getObjectAttributes(groupMembershipAttribute);
            if (usernames != null) {
                for (Object username : usernames) {
                    String user = username.toString();
                    Matcher m = userMembershipPattern.matcher(user);
                    if (m.matches()) {
                        user = m.group(1);
                    }
                    if(lookupUserForDn) {
                        user = getUserNameFromMembership(user);
                    }
                    users.add(user);
                }
            }
        }
    }

    private String getUserNameFromMembership(final String user) {
        final Set<String> userName = new HashSet<String>();
        userName.add(user);
        authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
            
            @Override
            public void executeWithContext(DirContext ctx,
                    LdapEntryIdentification ldapEntryIdentification) {
                DirContextOperations obj = (DirContextOperations)LDAPUtils
                        .getLdapTemplateInContext(ctx, template)
                        .lookup(user);
                String name = obj.getObjectAttribute(userNameAttribute).toString();
                Matcher m = userNamePattern.matcher(name);
                if(m.matches()) {
                    name = m.group(1);
                }
                userName.clear();
                userName.add(name);
            }
        });
        return userName.iterator().next();
    }

    private void addRolesToSet(SortedSet<GeoServerRole> roles,
            Set<String> roleNames) {
        for (String roleName : roleNames) {
            try {
                roles.add(createRoleObject(roleName));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error adding a new role from LDAP", e);
            }
        }
    }
    
    private void fillRolesForUser(DirContext ctx, String username, String userDn,
            SortedSet<GeoServerRole> roles) {
        Set<String> roleNames = LDAPUtils.getLdapTemplateInContext(ctx, template)
                .searchForSingleAttributeValues(groupSearchBase, groupSearchFilter,
                        new String[] { username, userDn }, groupRoleAttribute);
    
        addRolesToSet(roles, roleNames);
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return emptyMap;
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(rolePrefix
                + (convertToUpperCase ? role.toUpperCase() : role)); 
    }

    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        if (role.startsWith("ROLE_")) {
            // remove standard role prefix
            role = role.substring(5);
        }
        final String roleName = role;
        final SortedSet<String> roles = new TreeSet<String>();
        authenticateIfNeeded(new AuthenticatedLdapEntryContextCallback() {
    
            @Override
            public void executeWithContext(DirContext ctx,
                    LdapEntryIdentification ldapEntryIdentification) {
                roles.addAll(LDAPUtils.getLdapTemplateInContext(ctx, template)
                        .searchForSingleAttributeValues(groupSearchBase,
                                "cn=" + roleName, new String[] { roleName },
                                groupRoleAttribute));
            }
        });
        if (roles.size() == 1) {
            return createRoleObject(role);
        }
        return null;
    }

    @Override
    public void load() throws IOException {
        
    }

    @Override
    public Properties personalizeRoleParams(String roleName,
            Properties roleParams, String userName, Properties userProps)
            throws IOException {        
        return null;
    }

    @Override
    public GeoServerRole getAdminRole() {
        if(adminGroup == null) {
            return null;
        }
        try {
            return getRoleByName(adminGroup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        if(groupAdminGroup == null) {
            return null;
        }
        try {
            return getRoleByName(groupAdminGroup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRoleCount() throws IOException {
        return getRoles().size();
    }

}
