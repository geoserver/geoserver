/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.springframework.security.ldap.LdapUtils.getRelativeName;

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
import java.util.stream.Collectors;
import javax.naming.directory.DirContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.util.Assert;

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

        // if nested groups search is activated, retrieve all children role's userNames
        if (useNestedGroups) {
            Set<GeoServerRole> childrenRoles = getChildrenRoles(role);
            Set<GeoServerRole> navigatedRoles = new HashSet<>();
            navigatedRoles.add(role);
            for (GeoServerRole erole : childrenRoles) {
                SortedSet<String> userNamesForRole =
                        getUserNamesForRoleNested(erole, navigatedRoles, 1);
                users.addAll(userNamesForRole);
            }
        }

        return Collections.unmodifiableSortedSet(users);
    }

    private SortedSet<String> getUserNamesForRoleNested(
            GeoServerRole role, final Set<GeoServerRole> navigatedRoles, int depth)
            throws IOException {
        final SortedSet<String> users = new TreeSet<String>();
        if (isOutOfDepthBounds(depth)) return users;

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        fillUsersForRole(ctx, users, role);
                    }
                });

        // if nested groups search is activated, retrieve all children role's userNames
        if (useNestedGroups) {
            Set<GeoServerRole> childrenRoles = getChildrenRoles(role);
            for (GeoServerRole erole : childrenRoles) {
                if (!navigatedRoles.contains(erole)) {
                    navigatedRoles.add(role);
                    SortedSet<String> userNamesForRole =
                            getUserNamesForRoleNested(erole, navigatedRoles, depth + 1);
                    users.addAll(userNamesForRole);
                }
            }
        }
        return users;
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(final String username) throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();
        final String userDn = lookupDn(username);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        fillRolesForUser(ctx, username, userDn, roles);
                    }
                });

        if (useNestedGroups) {
            for (GeoServerRole erole : roles) {
                searchNestedParentRoles(erole, roles, 1);
            }
        }

        return Collections.unmodifiableSortedSet(roles);
    }

    private void searchNestedParentRoles(GeoServerRole role, Set<GeoServerRole> roles, int depth) {
        if (isOutOfDepthBounds(depth)) return;
        for (GeoServerRole erole : getParentRolesbyMember(role)) {
            if (!roles.contains(erole)) {
                roles.add(erole);
                searchNestedParentRoles(erole, roles, depth + 1);
            }
        }
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
        try {
            authenticateIfNeeded(
                    new AuthenticatedLdapEntryContextCallback() {

                        @Override
                        public void executeWithContext(
                                DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                            fillAllRoles(ctx, roles);
                        }
                    });

            return Collections.unmodifiableSortedSet(roles);
        } catch (CommunicationException ex) {
            throw new IOException(ex);
        }
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
        String roleStr = normalizeGroupName(role.toString());
        String roleDn = getRoleDn(role);

        DirContextOperations roleObj =
                LDAPUtils.getLdapTemplateInContext(ctx, template)
                        .searchForSingleEntry(
                                groupSearchBase, groupNameFilter, new String[] {roleStr, roleDn});
        if (roleObj != null) {
            Object[] usernames = roleObj.getObjectAttributes(groupMembershipAttribute);
            if (usernames != null) {
                for (Object username : usernames) {
                    String user = username.toString();
                    Matcher m = userMembershipPattern.matcher(user);
                    if (m.matches()) {
                        user = m.group(1);
                    }
                    // only if hierarchical groups are activated, filter for full user dn group
                    if (!(useNestedGroups
                            && !StringUtils.containsIgnoreCase(
                                    username.toString(), userSearchBase))) {
                        user = removeBaseDN(user);
                        users.add(getUserNameFromMembership(user));
                    }
                }
            }
        }
    }

    private String removeBaseDN(String user) {
        DirContext baseCtx = template.getContextSource().getReadOnlyContext();
        try {
            user = getRelativeName(user, baseCtx);
        } catch (Exception e) {
            // continue, rename only if works for the case
        }
        return user;
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

    private String normalizeGroupName(String role) {
        if (role.startsWith("ROLE_")) {
            // remove standard role prefix
            role = role.substring(5);
        }
        return role;
    }

    private Set<GeoServerRole> getChildrenRoles(final GeoServerRole role) {
        Assert.notNull(role, "Geoserver role shouldn't be null.");
        String roleName = normalizeGroupName(role.getAuthority());
        String roleDn = getRoleDn(role);
        final Set<String> membersDns = new HashSet<>();
        final Set<GeoServerRole> childs = new HashSet<>();
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        SpringSecurityLdapTemplate authTemplate =
                                LDAPUtils.getLdapTemplateInContext(ctx, template);
                        membersDns.addAll(
                                authTemplate
                                        .searchForSingleAttributeValues(
                                                groupSearchBase,
                                                groupNameFilter,
                                                new String[] {roleName, roleDn},
                                                groupMembershipAttribute)
                                        .stream()
                                        .filter(x -> x.contains(groupSearchBase))
                                        .collect(Collectors.toSet()));
                    }
                });
        for (String dn : membersDns) {
            String cnFromDn = extractGroupCnFromDn(dn);
            if (StringUtils.isNotBlank(cnFromDn))
                try {
                    childs.add(createRoleObject(cnFromDn));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }
        return childs;
    }

    private Set<GeoServerRole> getParentRolesbyMember(final GeoServerRole role) {
        if (role == null) return Collections.emptySet();
        final Set<GeoServerRole> parents = new HashSet<>();
        String roleDn = getRoleDn(role);
        String roleName = normalizeGroupName(role.getAuthority());
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        SpringSecurityLdapTemplate authTemplate =
                                LDAPUtils.getLdapTemplateInContext(ctx, template);
                        Set<String> parentGroupsNames =
                                authTemplate.searchForSingleAttributeValues(
                                        groupSearchBase,
                                        nestedGroupSearchFilter,
                                        new String[] {roleName, roleDn},
                                        groupNameAttribute);
                        for (String eparent : parentGroupsNames) {
                            try {
                                parents.add(createRoleObject(eparent));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
        return parents;
    }

    private String getRoleDn(GeoServerRole role) {
        String roleName = normalizeGroupName(role.getAuthority());
        final MutableObject<String> roleDnReference = new MutableObject<String>(null);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        String dn =
                                LDAPUtils.getLdapTemplateInContext(ctx, template)
                                        .searchForSingleEntry(
                                                groupSearchBase,
                                                groupNameFilter,
                                                new String[] {roleName})
                                        .getNameInNamespace();
                        roleDnReference.setValue(dn);
                    }
                });
        return roleDnReference.getValue();
    }
}
