/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;

/**
 * BindingLdapAuthoritiesPopulator: modified DefaultLdapAuthoritiesPopulator that binds the user
 * before extracting roles.
 *
 * <p>Needed for Windows ActiveDirectory support and maybe other LDAP servers requiring binding
 * before searches.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class BindingLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    // ~ Static fields/initializers
    // =====================================================================================

    private static final Log logger = LogFactory.getLog(BindingLdapAuthoritiesPopulator.class);

    // ~ Instance fields
    // ================================================================================================

    /** A default role which will be assigned to all authenticated users if set */
    private GrantedAuthority defaultRole;

    private final SpringSecurityLdapTemplate ldapTemplate;

    /**
     * Controls used to determine whether group searches should be performed over the full sub-tree
     * from the base DN. Modified by searchSubTree property
     */
    private final SearchControls searchControls = new SearchControls();

    /** The ID of the attribute which contains the role name for a group */
    private String groupRoleAttribute = "cn";

    /** The base DN from which the search for group membership should be performed */
    private String groupSearchBase;

    /** The pattern to be used for the user search. {0} is the user's DN */
    private String groupSearchFilter = "(member={0})";

    private String rolePrefix = "ROLE_";
    private boolean convertToUpperCase = true;

    /** Activates hierarchical nested parent groups search */
    private boolean useNestedParentGroups = false;

    /** The max recursion level for search Hierarchical groups */
    private int maxGroupSearchLevel = 10;

    /** Pattern used for nested group filtering */
    private String nestedGroupSearchFilter = "(member={0})";

    // ~ Constructors
    // ===================================================================================================

    /**
     * Constructor for group search scenarios. <tt>userRoleAttributes</tt> may still be set as a
     * property.
     *
     * @param contextSource supplies the contexts used to search for user roles.
     * @param groupSearchBase if this is an empty string the search will be performed from the root
     *     DN of the context factory. If null, no search will be performed.
     */
    public BindingLdapAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase) {
        Assert.notNull(contextSource, "contextSource must not be null");

        // use a binding LdapTemplate, that doesn't make searches without
        // authentication
        ldapTemplate = new BindingLdapTemplate(contextSource);
        ldapTemplate.setSearchControls(searchControls);
        this.groupSearchBase = groupSearchBase;

        if (groupSearchBase == null) {
            logger.info("groupSearchBase is null. No group search will be performed.");
        } else if (groupSearchBase.length() == 0) {
            logger.info(
                    "groupSearchBase is empty. Searches will be performed from the context source base");
        }
    }

    // ~ Methods
    // ========================================================================================================

    /**
     * This method should be overridden if required to obtain any additional roles for the given
     * user (on top of those obtained from the standard search implemented by this class).
     *
     * @param user the context representing the user who's roles are required
     * @return the extra roles which will be merged with those returned by the group search
     */
    protected Set<GrantedAuthority> getAdditionalRoles(
            DirContext ctx, DirContextOperations user, String username) {
        return null;
    }

    /**
     * Obtains the authorities for the user who's directory entry is represented by the supplied
     * LdapUserDetails object.
     *
     * @param user the user who's authorities are required (or user:password to be used to bind to
     *     ldap server prior to the search operations).
     * @return the set of roles granted to the user.
     */
    @Override
    public final Collection<GrantedAuthority> getGrantedAuthorities(
            final DirContextOperations user, final String username) {
        return getGrantedAuthorities(user, username, null);
    }

    /**
     * Obtains the authorities for the user who's directory entry is represented by the supplied
     * LdapUserDetails object.
     *
     * @param user the user who's authorities are required
     * @param password be used to bind to ldap server prior to the search operations, null otherwise
     * @return the set of roles granted to the user.
     */
    public final Collection<GrantedAuthority> getGrantedAuthorities(
            final DirContextOperations user, final String username, final String password) {
        final String userDn = user.getNameInNamespace();

        if (logger.isDebugEnabled()) {
            logger.debug("Getting authorities for user " + userDn);
        }

        final List<GrantedAuthority> result = new ArrayList<GrantedAuthority>();

        // password included -> authenticate before search
        if (password != null) {
            // authenticate and execute role extraction in the authenticated
            // context
            Consumer<Consumer<DirContext>> ctxConsumer =
                    (ctxFunc) -> {
                        ldapTemplate.authenticate(
                                LdapUtils.emptyLdapName(),
                                userDn,
                                password,
                                new AuthenticatedLdapEntryContextCallback() {

                                    @Override
                                    public void executeWithContext(
                                            DirContext ctx,
                                            LdapEntryIdentification ldapEntryIdentification) {
                                        ctxFunc.accept(ctx);
                                    }
                                });
                    };
            ldapTemplate.authenticate(
                    LdapUtils.emptyLdapName(),
                    userDn,
                    password,
                    new AuthenticatedLdapEntryContextCallback() {

                        @Override
                        public void executeWithContext(
                                DirContext ctx, LdapEntryIdentification ldapEntryIdentification) {
                            getAllRoles(user, userDn, result, username, ctxConsumer);
                        }
                    });
        } else {
            getAllRoles(user, userDn, result, username, (func) -> func.accept(null));
        }

        return result;
    }

    public Set<GrantedAuthority> getGroupMembershipRoles(
            Consumer<Consumer<DirContext>> ctxConsumer, String userDn, String username) {
        if (getGroupSearchBase() == null) {
            return new HashSet<GrantedAuthority>();
        }

        Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Searching for roles for user '"
                            + username
                            + "', DN = "
                            + "'"
                            + userDn
                            + "', with filter "
                            + groupSearchFilter
                            + " in search base '"
                            + getGroupSearchBase()
                            + "'");
        }
        final List<Pair<String, String>> userRolesNameDn = new ArrayList<>();
        ctxConsumer.accept(
                (ctx) -> {
                    SpringSecurityLdapTemplate authTemplate =
                            (SpringSecurityLdapTemplate)
                                    LDAPUtils.getLdapTemplateInContext(ctx, ldapTemplate);

                    // Get ldap groups in form of Pair<String,String> -> Pair<name,dn>
                    final String formattedFilter =
                            MessageFormat.format(groupSearchFilter, userDn, username);
                    userRolesNameDn.addAll(
                            authTemplate.search(
                                    getGroupSearchBase(),
                                    formattedFilter,
                                    new AbstractContextMapper<Pair<String, String>>() {
                                        @Override
                                        protected Pair<String, String> doMapFromContext(
                                                DirContextOperations ctx) {
                                            String name =
                                                    ctx.getStringAttribute(groupRoleAttribute);
                                            String dn = ctx.getNameInNamespace();
                                            return Pair.of(name, dn);
                                        }
                                    }));
                });

        if (logger.isDebugEnabled()) {
            logger.debug("Roles from search: " + userRolesNameDn);
        }

        for (Pair<String, String> roleNameDn : userRolesNameDn) {
            String role = roleNameDn.getLeft();
            String dn = roleNameDn.getRight();
            if (convertToUpperCase) {
                role = role.toUpperCase();
            }
            authorities.add(new SimpleGrantedAuthority(rolePrefix + role));
            // search nested LDAP groups if nested parents is enabled
            if (useNestedParentGroups)
                searchNestedGroupMembershipRoles(
                        ctxConsumer,
                        dn,
                        roleNameDn.getLeft(),
                        authorities,
                        maxGroupSearchLevel - 1);
        }
        return authorities;
    }

    /** Recursively collect all hierarchical related roles */
    private void searchNestedGroupMembershipRoles(
            Consumer<Consumer<DirContext>> ctxConsumer,
            String groupDn,
            String groupName,
            Set<GrantedAuthority> authorities,
            int depth) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Searching for roles for nested group '"
                            + groupName
                            + "', DN = "
                            + "'"
                            + groupDn
                            + "', with filter "
                            + nestedGroupSearchFilter
                            + " in search base '"
                            + getGroupSearchBase()
                            + "'");
        }

        final List<Pair<String, String>> groupRolesNameDn = new ArrayList<>();
        ctxConsumer.accept(
                (ctx) -> {
                    SpringSecurityLdapTemplate authTemplate =
                            (SpringSecurityLdapTemplate)
                                    LDAPUtils.getLdapTemplateInContext(ctx, ldapTemplate);
                    // Get ldap groups in form of Pair<String,String> -> Pair<name,dn>
                    final String formattedFilter =
                            MessageFormat.format(nestedGroupSearchFilter, groupDn, groupName);
                    groupRolesNameDn.addAll(
                            authTemplate.search(
                                    getGroupSearchBase(),
                                    formattedFilter,
                                    new AbstractContextMapper<Pair<String, String>>() {
                                        @Override
                                        protected Pair<String, String> doMapFromContext(
                                                DirContextOperations ctx) {
                                            String name =
                                                    ctx.getStringAttribute(groupRoleAttribute);
                                            String dn = ctx.getNameInNamespace();
                                            return Pair.of(name, dn);
                                        }
                                    }));
                });

        if (logger.isDebugEnabled()) {
            logger.debug("Roles from search: " + groupRolesNameDn);
        }

        for (Pair<String, String> roleNameDn : groupRolesNameDn) {
            String role = roleNameDn.getLeft();
            String dn = roleNameDn.getRight();
            if (convertToUpperCase) {
                role = role.toUpperCase();
            }
            boolean addedSuccesfuly =
                    authorities.add(new SimpleGrantedAuthority(rolePrefix + role));
            // search nested Ldap groups,
            // only if role was added successfully for avoiding circular references
            // if maxGroupSearchLevel == -1 -> no depth limit
            if ((maxGroupSearchLevel == -1 || depth > 0) && addedSuccesfuly)
                searchNestedGroupMembershipRoles(
                        ctxConsumer, dn, roleNameDn.getLeft(), authorities, depth - 1);
        }
    }

    protected ContextSource getContextSource() {
        return ldapTemplate.getContextSource();
    }

    protected String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupRoleAttribute(String groupRoleAttribute) {
        Assert.notNull(groupRoleAttribute, "groupRoleAttribute must not be null");
        this.groupRoleAttribute = groupRoleAttribute;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        Assert.notNull(groupSearchFilter, "groupSearchFilter must not be null");
        this.groupSearchFilter = groupSearchFilter;
    }

    /**
     * If set to true, a subtree scope search will be performed. If false a single-level search is
     * used.
     *
     * @param searchSubtree set to true to enable searching of the entire tree below the
     *     <tt>groupSearchBase</tt>.
     */
    public void setSearchSubtree(boolean searchSubtree) {
        int searchScope =
                searchSubtree ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE;
        searchControls.setSearchScope(searchScope);
    }

    /**
     * Sets the corresponding property on the underlying template, avoiding specific issues with
     * Active Directory.
     *
     * @see LdapTemplate#setIgnoreNameNotFoundException(boolean)
     */
    public void setIgnorePartialResultException(boolean ignore) {
        ldapTemplate.setIgnorePartialResultException(ignore);
    }

    private void getAllRoles(
            final DirContextOperations user,
            final String userDn,
            final List<GrantedAuthority> result,
            final String userName,
            Consumer<Consumer<DirContext>> ctxConsumer) {
        Set<GrantedAuthority> roles = getGroupMembershipRoles(ctxConsumer, userDn, userName);

        final Set<GrantedAuthority> extraRoles = new HashSet<>();
        ctxConsumer.accept(
                (ctx) -> {
                    extraRoles.addAll(getAdditionalRoles(ctx, user, userName));
                });

        if (extraRoles != null) {
            roles.addAll(extraRoles);
        }

        if (defaultRole != null) {
            roles.add(defaultRole);
        }

        result.addAll(roles);
    }

    public void setUseNestedParentGroups(boolean useNestedParentGroups) {
        this.useNestedParentGroups = useNestedParentGroups;
    }

    public void setMaxGroupSearchLevel(int maxGroupSearchLevel) {
        this.maxGroupSearchLevel = maxGroupSearchLevel;
    }

    public void setNestedGroupSearchFilter(String nestedGroupSearchFilter) {
        this.nestedGroupSearchFilter = nestedGroupSearchFilter;
    }
}
