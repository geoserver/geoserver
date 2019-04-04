/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.naming.directory.DirContext;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapEntryIdentification;

/**
 * LDAP implementation of {@link GeoServerRoleService}
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class LDAPRoleService extends LDAPBaseSecurityService implements GeoServerRoleService {

    private static final SortedSet<String> emptyStringSet =
            Collections.unmodifiableSortedSet(new TreeSet<String>());

    private static final Map<String, String> emptyMap = Collections.emptyMap();

    static Logger LOGGER = Logging.getLogger("org.geoserver.security.ldap");
    protected Set<RoleLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    private String rolePrefix = "ROLE_";
    private boolean convertToUpperCase = true;

    private String adminGroup;
    private String groupAdminGroup;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        LDAPRoleServiceConfig ldapConfig = (LDAPRoleServiceConfig) config;
        if (!isEmpty(ldapConfig.getAdminGroup())) {
            this.adminGroup = ldapConfig.getAdminGroup();
        }
        if (!isEmpty(ldapConfig.getGroupAdminGroup())) {
            this.groupAdminGroup = ldapConfig.getGroupAdminGroup();
        }
    }
    /** Read only store. */
    @Override
    public boolean canCreateStore() {
        return false;
    }

    /** Read only store. */
    @Override
    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerRoleService#registerRoleLoadedListener(RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerRoleService#unregisterRoleLoadedListener(RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /** Roles to group association is not supported */
    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return emptyStringSet;
    }

    @Override
    public SortedSet<String> getUserNamesForRole(final GeoServerRole role) throws IOException {
        final SortedSet<String> users = new TreeSet<String>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        fillUsersForRole(ctx, users, role);
                    }
                });

        return Collections.unmodifiableSortedSet(users);
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(final String username) throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        fillRolesForUser(ctx, username, lookupDn(username), roles);
                    }
                });

        return Collections.unmodifiableSortedSet(roles);
    }

    /** Assume role name = group name */
    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        SortedSet<GeoServerRole> set = new TreeSet<GeoServerRole>();
        GeoServerRole role = getRoleByName(groupname);
        if (role != null) {
            set.add(role);
        }

        return Collections.unmodifiableSortedSet(set);
    }

    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        fillAllRoles(ctx, roles);
                    }
                });

        return Collections.unmodifiableSortedSet(roles);
    }

    private void fillAllRoles(DirContext ctx, SortedSet<GeoServerRole> roles) {
        Set<String> roleNames =
                LDAPUtils.getLdapTemplateInContext(ctx, template)
                        .searchForSingleAttributeValues(
                                groupSearchBase,
                                allGroupsSearchFilter,
                                new String[] {},
                                groupNameAttribute);
        addRolesToSet(roles, roleNames);
    }

    private void fillUsersForRole(DirContext ctx, SortedSet<String> users, GeoServerRole role) {
        String roleStr = role.toString();
        if (roleStr.startsWith("ROLE_")) {
            // remove standard role prefix
            roleStr = roleStr.substring(5);
        }
        DirContextOperations roleObj =
                LDAPUtils.getLdapTemplateInContext(ctx, template)
                        .searchForSingleEntry(
                                groupSearchBase, groupNameFilter, new String[] {roleStr});
        if (roleObj != null) {
            Object[] usernames = roleObj.getObjectAttributes(groupMembershipAttribute);
            if (usernames != null) {
                for (Object username : usernames) {
                    String user = username.toString();
                    Matcher m = userMembershipPattern.matcher(user);
                    if (m.matches()) {
                        user = m.group(1);
                    }
                    users.add(getUserNameFromMembership(user));
                }
            }
        }
    }

    private void addRolesToSet(SortedSet<GeoServerRole> roles, Set<String> roleNames) {
        for (String roleName : roleNames) {
            try {
                roles.add(createRoleObject(roleName));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error adding a new role from LDAP", e);
            }
        }
    }

    private void fillRolesForUser(
            DirContext ctx, String username, String userDn, SortedSet<GeoServerRole> roles) {
        Set<String> roleNames =
                LDAPUtils.getLdapTemplateInContext(ctx, template)
                        .searchForSingleAttributeValues(
                                groupSearchBase,
                                groupMembershipFilter,
                                new String[] {username, userDn},
                                groupNameAttribute);

        addRolesToSet(roles, roleNames);
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return emptyMap;
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(rolePrefix + (convertToUpperCase ? role.toUpperCase() : role));
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
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        roles.addAll(
                                LDAPUtils.getLdapTemplateInContext(ctx, template)
                                        .searchForSingleAttributeValues(
                                                groupSearchBase,
                                                groupNameFilter,
                                                new String[] {roleName},
                                                groupNameAttribute));
                    }
                });
        if (roles.size() == 1) {
            return createRoleObject(role);
        }
        return null;
    }

    @Override
    public void load() throws IOException {}

    @Override
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getAdminRole() {
        if (adminGroup == null) {
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
        if (groupAdminGroup == null) {
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
        AtomicInteger count = new AtomicInteger(0);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(groupSearchBase, allGroupsSearchFilter, counter(count));
                    }
                });
        return count.get();
    }
}
