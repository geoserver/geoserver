/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.directory.DirContext;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;

/** @author Niels Charlier */
public abstract class LDAPBaseSecurityService extends AbstractGeoServerSecurityService {

    /** regex to find membership attribute in expression */
    protected static final Pattern lookForMembershipAttribute =
            Pattern.compile("^\\(*([a-z]+)=(.*?)\\{([01])\\}(.*?)\\)*$", Pattern.CASE_INSENSITIVE);

    /** regex to extract the username from the user info */
    protected Pattern userNamePattern = Pattern.compile("^(.*)$");

    /** regex to extract username from membership info */
    protected Pattern userMembershipPattern = Pattern.compile("^(.*)$");

    /** LDAP context */
    protected LdapContextSource ldapContext;

    /** LDAP template */
    protected SpringSecurityLdapTemplate template;

    /** User (if authenticating) */
    protected String user;

    /** Pasdword (if authenticating) */
    protected String password;

    /** Search base for ldap groups that are to be mapped to GeoServer groups/roles */
    protected String groupSearchBase = "ou=groups";

    /** Standard filter for getting all roles bounded to a user */
    protected String groupNameFilter = "cn={0}";

    /** Standard filter for getting all roles */
    protected String allGroupsSearchFilter = "cn=*";

    /** The ID of the attribute which contains the role name for a group */
    protected String groupNameAttribute = "cn";

    /** Standard filter for getting all roles bounded to a user */
    protected String groupMembershipFilter = "member={0}";

    /** attribute of a group containing the membership info */
    protected String groupMembershipAttribute = "member";

    /** Search base for ldap users that are to be mapped to GeoServer roles */
    protected String userSearchBase = "ou=people";

    /** Standard filter for getting all groups bounded to a user */
    protected String userNameFilter = "uid={0}";

    /** Standard filter for getting all groups bounded to a user */
    protected String allUsersSearchFilter = "uid=*";

    /** attribute of a user containing the username (used if userFilter is defined) */
    protected String userNameAttribute = "uid";

    /** lookup user for dn */
    protected boolean lookupUserForDn = false;

    /** Activates nested groups searching */
    protected boolean useNestedGroups = true;

    /** The max recursion level for search Hierarchical groups */
    protected int maxGroupSearchLevel = 10;

    /** Pattern used for nested group filtering */
    protected String nestedGroupSearchFilter = "member={0}";

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        LDAPBaseSecurityServiceConfig ldapConfig = (LDAPBaseSecurityServiceConfig) config;

        ldapContext = LDAPUtils.createLdapContext(ldapConfig);

        if (ldapConfig.isBindBeforeGroupSearch()) {
            // authenticate before LDAP searches
            user = ldapConfig.getUser();
            password = ldapConfig.getPassword();
            template = new BindingLdapTemplate(ldapContext);
        } else {
            template = new SpringSecurityLdapTemplate(ldapContext);
        }

        if (!isEmpty(ldapConfig.getGroupSearchBase())) {
            groupSearchBase = ldapConfig.getGroupSearchBase();
        }
        if (!isEmpty(ldapConfig.getUserSearchBase())) {
            userSearchBase = ldapConfig.getUserSearchBase();
        }

        if (!isEmpty(ldapConfig.getGroupSearchFilter())) {
            groupMembershipFilter = ldapConfig.getGroupSearchFilter();
            Matcher m = lookForMembershipAttribute.matcher(groupMembershipFilter);
            if (m.matches()) {
                if (isEmpty(ldapConfig.getGroupMembershipAttribute())) {
                    groupMembershipAttribute = m.group(1);
                }
                lookupUserForDn = m.group(3).equals("1");
                userMembershipPattern =
                        Pattern.compile(
                                "^"
                                        + Pattern.quote(m.group(2))
                                        + "(.*)"
                                        + Pattern.quote(m.group(4))
                                        + "$");
            }
        }
        if (!isEmpty(ldapConfig.getGroupMembershipAttribute())) {
            groupMembershipAttribute = ldapConfig.getGroupMembershipAttribute();
            if (isEmpty(ldapConfig.getGroupSearchFilter())) {
                groupMembershipFilter = groupMembershipAttribute + "={0}";
            }
        }

        if (!isEmpty(ldapConfig.getGroupFilter())) {
            groupNameFilter = ldapConfig.getGroupFilter();
            if (isEmpty(ldapConfig.getGroupNameAttribute())) {
                Matcher m = lookForMembershipAttribute.matcher(groupNameFilter);
                if (m.matches()) {
                    groupNameAttribute = m.group(1);
                }
            }
        }
        if (!isEmpty(ldapConfig.getGroupNameAttribute())) {
            groupNameAttribute = ldapConfig.getGroupNameAttribute();
            if (isEmpty(ldapConfig.getGroupFilter())) {
                groupNameFilter = groupNameAttribute + "={0}";
            }
        }
        if (!isEmpty(ldapConfig.getAllGroupsSearchFilter())) {
            allGroupsSearchFilter = ldapConfig.getAllGroupsSearchFilter();
        } else {
            allGroupsSearchFilter = groupNameAttribute + "=*";
        }

        if (!isEmpty(ldapConfig.getUserFilter())) {
            this.userNameFilter = ldapConfig.getUserFilter();
            Matcher m = lookForMembershipAttribute.matcher(userNameFilter);
            if (m.matches()) {
                if (isEmpty(ldapConfig.getUserNameAttribute())) {
                    userNameAttribute = m.group(1);
                }
                userNamePattern =
                        Pattern.compile(
                                "^"
                                        + Pattern.quote(m.group(2))
                                        + "(.*)"
                                        + Pattern.quote(m.group(4))
                                        + "$");
            }
        }
        if (!isEmpty(ldapConfig.getUserNameAttribute())) {
            userNameAttribute = ldapConfig.getUserNameAttribute();
            if (isEmpty(ldapConfig.getUserFilter())) {
                userNameFilter = userNameAttribute + "={0}";
            }
        }
        if (!isEmpty(ldapConfig.getAllUsersSearchFilter())) {
            allUsersSearchFilter = ldapConfig.getAllUsersSearchFilter();
        } else {
            allUsersSearchFilter = userNameAttribute + "=*";
        }

        // Hierarchical groups options
        this.useNestedGroups = ldapConfig.isUseNestedParentGroups();
        if (!isEmpty(ldapConfig.getNestedGroupSearchFilter()))
            this.nestedGroupSearchFilter = ldapConfig.getNestedGroupSearchFilter();
        else this.nestedGroupSearchFilter = "member= {0}";
        if (ldapConfig.getMaxGroupSearchLevel() >= 0)
            this.maxGroupSearchLevel = ldapConfig.getMaxGroupSearchLevel();
        else this.maxGroupSearchLevel = 10;
    }

    /**
     * Execute authentication, if configured to do so, and then call the given callback on
     * authenticated context, or simply call the given callback if no authentication is needed.
     */
    protected void authenticateIfNeeded(AuthenticatedLdapEntryContextCallback callback) {
        if (user != null && password != null) {
            template.authenticate(LdapUtils.emptyLdapName(), user, password, callback);
        } else {
            callback.executeWithContext(null, null);
        }
    }

    protected static boolean isEmpty(String property) {
        return property == null || property.isEmpty();
    }

    protected String getUserNameFromMembership(final String user) {
        final AtomicReference<String> userName = new AtomicReference<String>(user);

        if (lookupUserForDn) {
            authenticateIfNeeded(
                    new AuthenticatedLdapEntryContextCallback() {

                        @Override
                        public void executeWithContext(
                                DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                            DirContextOperations obj =
                                    (DirContextOperations)
                                            LDAPUtils.getLdapTemplateInContext(ctx, template)
                                                    .lookup(user);
                            Object attribute = obj.getObjectAttribute(userNameAttribute);
                            if (attribute != null) {
                                String name = attribute.toString();
                                Matcher m = userNamePattern.matcher(name);
                                if (m.matches()) {
                                    name = m.group(1);
                                }
                                userName.set(name);
                            }
                        }
                    });
        }
        return userName.get();
    }

    protected String lookupDn(String username) {
        final AtomicReference<String> dn = new AtomicReference<String>(username);
        if (lookupUserForDn) {
            authenticateIfNeeded(
                    new AuthenticatedLdapEntryContextCallback() {

                        @Override
                        public void executeWithContext(
                                DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                            try {
                                dn.set(
                                        LDAPUtils.getLdapTemplateInContext(ctx, template)
                                                .searchForSingleEntry(
                                                        "", userNameFilter, new String[] {username})
                                                .getDn()
                                                .toString());
                            } catch (Exception e) {
                                // not found, let's use username instead
                            }
                        }
                    });
        }

        return dn.get();
    }

    protected ContextMapper counter(AtomicInteger count) {
        return ctx -> {
            count.set(count.get() + 1);
            return null;
        };
    }

    protected String extractGroupCnFromDn(String dn) {
        if (StringUtils.isBlank(dn)) return null;
        String[] parts = dn.split(Pattern.quote(","));
        for (String part : parts) {
            if (part.startsWith(groupNameAttribute + "=")) {
                int equalsIndex = part.indexOf("=");
                return part.substring(equalsIndex + 1);
            }
        }
        return null;
    }

    /** Checks if current depth int value is out of depth limit. */
    protected boolean isOutOfDepthBounds(int depth) {
        // if maxGroupSearchLevel == -1 then no limit
        if (maxGroupSearchLevel == -1) return false;
        return depth >= maxGroupSearchLevel;
    }
}
