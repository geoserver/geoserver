/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
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
                                                    new String[] {group.getGroupname()});
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
                                        users.add(
                                                getUserByUsername(getUserNameFromMembership(user)));
                                    }
                                }
                            }
                        } catch (IncorrectResultSizeDataAccessException e) {
                        }
                    }
                });

        return Collections.unmodifiableSortedSet(users);
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
        return Collections.unmodifiableSortedSet(groups);
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
