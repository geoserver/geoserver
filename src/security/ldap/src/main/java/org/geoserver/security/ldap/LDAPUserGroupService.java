/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.util.Assert;

/**
 * LDAP implementation of {@link GeoServerUserGroupService}
 *
 * @author Niels Charlier
 */
public class LDAPUserGroupService extends LDAPBaseSecurityService
        implements GeoServerUserGroupService {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.ldap");

    private String passwordEncoderName;

    private String passwordValidatorName;

    private String[] populatedAttributes = new String[] {};

    public LDAPUserGroupService(SecurityNamedServiceConfig config) throws IOException {
        initializeFromConfig(config);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        LDAPUserGroupServiceConfig ldapConfig = ((LDAPUserGroupServiceConfig) config);
        passwordEncoderName = ldapConfig.getPasswordEncoderName();
        passwordValidatorName = ldapConfig.getPasswordPolicyName();
        if (!isEmpty(ldapConfig.getPopulatedAttributes())) {
            populatedAttributes = ldapConfig.getPopulatedAttributes().trim().split("[\\s]*,[\\s]*");
        }
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        return null; // read-only!
    }

    @Override
    public void load() throws IOException {
        // do nothing
    }

    @Override
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        // ignore, there are no events
    }

    @Override
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        // ignore, there are no events
    }

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public String getPasswordValidatorName() {
        return passwordValidatorName;
    }

    // ----------------------------------------------------------------------------------

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        GeoServerUser user = new GeoServerUser(username);
        user.setEnabled(isEnabled);
        user.setPassword(password);
        return user;
    }

    @Override
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        GeoServerUserGroup group = new GeoServerUserGroup(groupname);
        group.setEnabled(isEnabled);
        return group;
    }

    @Override
    public SortedSet<GeoServerUserGroup> getUserGroups() {
        final SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        Set<String> groupNames =
                                LDAPUtils.getLdapTemplateInContext(ctx, template)
                                        .searchForSingleAttributeValues(
                                                groupSearchBase,
                                                allGroupsSearchFilter,
                                                new String[] {},
                                                groupNameAttribute);

                        for (String groupName : groupNames) {
                            groups.add(new GeoServerUserGroup(groupName));
                        }
                    }
                });

        return Collections.unmodifiableSortedSet(groups);
    }

    protected GeoServerUser createUser(DirContextOperations dco) {
        GeoServerUser gsUser = new GeoServerUser(dco.getStringAttribute(userNameAttribute));
        for (String attName : populatedAttributes) {
            try {
                Attribute att = dco.getAttributes().get(attName.toLowerCase());
                if (att != null) {
                    Object value = att.get();
                    if (value instanceof String) {
                        gsUser.getProperties().put(attName, value);
                    }
                }
            } catch (NamingException e) {
                LOGGER.log(
                        Level.WARNING, "Could not populate value for user attribute " + attName, e);
            }
        }
        return gsUser;
    }

    protected ContextMapper addToUsers(SortedSet<GeoServerUser> users) {
        return ctx -> {
            users.add(createUser((DirContextAdapter) ctx));
            return null;
        };
    }

    @Override
    public SortedSet<GeoServerUser> getUsers() {
        final SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(userSearchBase, allUsersSearchFilter, addToUsers(users));
                    }
                });

        return Collections.unmodifiableSortedSet(users);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        GeoServerUser user = null;
        try {
            user = getUserByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException(userNotFoundMessage(username));
            }
            RoleCalculator calculator =
                    new RoleCalculator(this, getSecurityManager().getActiveRoleService());
            user.setAuthorities(calculator.calculateRoles(user));
        } catch (IOException e) {
            throw new UsernameNotFoundException(userNotFoundMessage(username), e);
        }

        return user;
    }

    protected String userNotFoundMessage(String username) {
        return "User  " + username + " not found in usergroupservice: " + getName();
    }

    @Override
    public GeoServerUserGroup getGroupByGroupname(String groupname) {
        final AtomicReference<GeoServerUserGroup> group = new AtomicReference<GeoServerUserGroup>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {

                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        try {
                            DirContextOperations dco =
                                    LDAPUtils.getLdapTemplateInContext(ctx, template)
                                            .searchForSingleEntry(
                                                    groupSearchBase,
                                                    groupNameFilter,
                                                    new String[] {groupname});

                            if (dco != null) {
                                group.set(
                                        new GeoServerUserGroup(
                                                dco.getStringAttribute(groupNameAttribute)));
                            }
                        } catch (IncorrectResultSizeDataAccessException e) {
                        }
                    }
                });

        return group.get();
    }

    @Override
    public GeoServerUser getUserByUsername(String username) {
        final AtomicReference<GeoServerUser> user = new AtomicReference<GeoServerUser>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        try {
                            DirContextOperations dco =
                                    LDAPUtils.getLdapTemplateInContext(ctx, template)
                                            .searchForSingleEntry(
                                                    userSearchBase,
                                                    userNameFilter,
                                                    new String[] {username});

                            if (dco != null) {
                                user.set(createUser(dco));
                            }
                        } catch (IncorrectResultSizeDataAccessException e) {
                        }
                    }
                });

        return user.get();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersForGroup(final GeoServerUserGroup group) {
        final SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();

        if (!useNestedGroups) {
            // no nested groups, load users from root group only
            addUsersFromGroup(group, users);
        } else {
            // nested groups search activated
            // search for all hierarchical child groups
            Set<GeoServerUserGroup> groups = new HashSet<>();
            groups.add(group);
            searchAllNestedChildGroups(group, groups, 1);
            // load users from all child groups
            for (GeoServerUserGroup egroup : groups) {
                addUsersFromGroup(egroup, users);
            }
        }

        return Collections.unmodifiableSortedSet(users);
    }

    private void addUsersFromGroup(final GeoServerUserGroup group, final Set<GeoServerUser> users) {
        final String groupDn = getGroupDn(group);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        try {
                            DirContextOperations roleObj =
                                    LDAPUtils.getLdapTemplateInContext(ctx, template)
                                            .searchForSingleEntry(
                                                    groupSearchBase,
                                                    groupNameFilter,
                                                    new String[] {group.getGroupname(), groupDn});
                            if (roleObj != null) {
                                Object[] usernames =
                                        roleObj.getObjectAttributes(groupMembershipAttribute);
                                if (usernames != null) {
                                    for (Object username : usernames) {
                                        String user = username.toString();
                                        Matcher m = userMembershipPattern.matcher(user);
                                        if (m.matches()) {
                                            user = m.group(1);
                                        }
                                        String userNameFromMembership =
                                                getUserNameFromMembership(user);
                                        if (StringUtils.isNotBlank(userNameFromMembership)) {
                                            GeoServerUser userByUsername =
                                                    getUserByUsername(userNameFromMembership);
                                            if (userByUsername != null) users.add(userByUsername);
                                        }
                                    }
                                }
                            }
                        } catch (IncorrectResultSizeDataAccessException e) {
                        }
                    }
                });
    }

    private void searchAllNestedChildGroups(
            GeoServerUserGroup group, Set<GeoServerUserGroup> visitedGroups, int depth) {
        if (isOutOfDepthBounds(depth)) return;
        for (GeoServerUserGroup echild : getChildrenGroups(group)) {
            // check if it was already visited
            if (!visitedGroups.contains(echild)) {
                visitedGroups.add(echild);
                searchAllNestedChildGroups(echild, visitedGroups, depth + 1);
            }
        }
    }

    private Set<GeoServerUserGroup> getChildrenGroups(GeoServerUserGroup parent) {
        Assert.notNull(parent, "Geoserver group shouldn't be null.");
        final String groupName = parent.getGroupname();
        final Set<String> memberGroupDns = new HashSet<>();
        final Set<GeoServerUserGroup> childGroups = new HashSet<>();
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        SpringSecurityLdapTemplate authTemplate =
                                LDAPUtils.getLdapTemplateInContext(ctx, template);
                        Set<String> membersDns =
                                authTemplate
                                        .searchForSingleAttributeValues(
                                                groupSearchBase,
                                                groupNameFilter,
                                                new String[] {groupName},
                                                groupMembershipAttribute)
                                        .stream()
                                        .filter(
                                                x ->
                                                        !useNestedGroups
                                                                || StringUtils.containsIgnoreCase(
                                                                        x, groupSearchBase))
                                        .collect(Collectors.toSet());
                        memberGroupDns.addAll(membersDns);
                    }
                });

        for (String dn : memberGroupDns) {
            String memberGroupName = extractGroupCnFromDn(dn);
            if (StringUtils.isNotBlank(memberGroupName))
                childGroups.add(new GeoServerUserGroup(memberGroupName));
        }
        return childGroups;
    }

    @Override
    public SortedSet<GeoServerUserGroup> getGroupsForUser(final GeoServerUser user) {
        final SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        Set<String> groupNames =
                                LDAPUtils.getLdapTemplateInContext(ctx, template)
                                        .searchForSingleAttributeValues(
                                                groupSearchBase,
                                                groupMembershipFilter,
                                                new String[] {
                                                    user.getUsername(), lookupDn(user.getUsername())
                                                },
                                                groupNameAttribute);

                        for (String groupName : groupNames) {
                            groups.add(new GeoServerUserGroup(groupName));
                        }
                    }
                });
        // if nested groups search is enabled, add hierarchical parent groups
        if (useNestedGroups) {
            for (GeoServerUserGroup egroup : groups) {
                addNestedParentGroups(egroup, groups, 1);
            }
        }
        return Collections.unmodifiableSortedSet(groups);
    }

    private void addNestedParentGroups(
            GeoServerUserGroup group, Set<GeoServerUserGroup> visitedGroups, int depth) {
        if (isOutOfDepthBounds(depth)) return;
        final String groupDn = getGroupDn(group);
        final Set<GeoServerUserGroup> parents = new HashSet<>();
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
                                        groupMembershipFilter,
                                        new String[] {group.getGroupname(), groupDn},
                                        groupNameAttribute);
                        for (String ename : parentGroupsNames) {
                            parents.add(new GeoServerUserGroup(ename));
                        }
                    }
                });
        for (GeoServerUserGroup eparent : parents) {
            if (!visitedGroups.contains(eparent)) {
                visitedGroups.add(eparent);
                addNestedParentGroups(eparent, visitedGroups, depth + 1);
            }
        }
    }

    private String getGroupDn(GeoServerUserGroup group) {
        final String groupName = group.getGroupname();
        final MutableObject<String> groupDnReference = new MutableObject<String>(null);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        final String dn =
                                LDAPUtils.getLdapTemplateInContext(ctx, template)
                                        .searchForSingleEntry(
                                                groupSearchBase,
                                                groupNameFilter,
                                                new String[] {groupName})
                                        .getDn()
                                        .toString();
                        groupDnReference.setValue(dn);
                    }
                });
        return groupDnReference.getValue();
    }

    @Override
    public int getUserCount() {
        AtomicInteger size = new AtomicInteger(0);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(userSearchBase, allUsersSearchFilter, counter(size));
                    }
                });

        return size.get();
    }

    @Override
    public int getGroupCount() {
        AtomicInteger size = new AtomicInteger(0);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(groupSearchBase, allGroupsSearchFilter, counter(size));
                    }
                });
        return size.get();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) {
        final SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(userSearchBase, propname + "=*", addToUsers(users));
                    }
                });

        return users;
    }

    @Override
    public int getUserCountHavingProperty(String propname) {
        AtomicInteger size = new AtomicInteger(0);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(userSearchBase, propname + "=*", counter(size));
                    }
                });
        return size.get();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) {
        final SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(
                                        userSearchBase,
                                        "(&(!(" + propname + "=*))(" + allUsersSearchFilter + "))",
                                        addToUsers(users));
                    }
                });

        return users;
    }

    @Override
    public int getUserCountNotHavingProperty(String propname) {
        AtomicInteger size = new AtomicInteger(0);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(
                                        userSearchBase,
                                        "(&(!(" + propname + "=*))(" + allUsersSearchFilter + "))",
                                        counter(size));
                    }
                });
        return size.get();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        final SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();

        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(
                                        userSearchBase,
                                        propname + "=" + propvalue,
                                        addToUsers(users));
                    }
                });

        return users;
    }

    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        AtomicInteger size = new AtomicInteger(0);
        authenticateIfNeeded(
                new AuthenticatedLdapEntryContextCallback() {
                    @Override
                    public void executeWithContext(
                            DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                .search(userSearchBase, propname + "=" + propvalue, counter(size));
                    }
                });
        return size.get();
    }
}
