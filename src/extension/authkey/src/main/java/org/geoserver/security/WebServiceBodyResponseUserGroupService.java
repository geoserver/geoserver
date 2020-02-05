/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

/**
 * Extracts Roles from the {@linkplain WebServiceAuthenticationKeyMapper} Response Body.
 *
 * <p>This {@linkplain GeoServerUserGroupService} can also be used to re-map Roles to internal
 * Security Groups.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class WebServiceBodyResponseUserGroupService extends AbstractGeoServerSecurityService
        implements GeoServerUserGroupService {

    static final SortedSet<GeoServerUser> emptyUserSet =
            Collections.unmodifiableSortedSet(new TreeSet<GeoServerUser>());

    private boolean convertToUpperCase = true;

    private static final String rolePrefix = "ROLE_";

    private static final String groupPrefix = "GROUP_";

    protected Set<UserGroupLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<UserGroupLoadedListener>());

    protected String passwordEncoderName, passwordValidatorName;

    // compiled regex for Roles search on webservice body response
    Pattern searchRolesRegex = null;

    // optional static list of available Groups from the webservice response
    protected SortedSet<GeoServerUserGroup> availableGroups =
            Collections.synchronizedSortedSet(new TreeSet<GeoServerUserGroup>());

    private String roleServiceName;

    private GeoServerRoleService defaultSecurityService;

    public WebServiceBodyResponseUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        initializeFromConfig(config);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        WebServiceBodyResponseUserGroupServiceConfig webServiceBodyConfig =
                ((WebServiceBodyResponseUserGroupServiceConfig) config);
        passwordEncoderName = webServiceBodyConfig.getPasswordEncoderName();
        passwordValidatorName = webServiceBodyConfig.getPasswordPolicyName();

        if (StringUtils.hasLength(webServiceBodyConfig.getSearchRoles())) {
            try {
                searchRolesRegex = Pattern.compile(webServiceBodyConfig.getSearchRoles());
            } catch (PatternSyntaxException e) {
                throw new IOException("Search Roles regex is malformed");
            }
        }

        if (StringUtils.hasLength(webServiceBodyConfig.getAvailableGroups())) {
            for (String role : webServiceBodyConfig.getAvailableGroups().split(",")) {
                availableGroups.add(
                        new GeoServerUserGroup(
                                (convertToUpperCase ? role.trim().toUpperCase() : role.trim())));
            }
        }

        if (StringUtils.hasLength(webServiceBodyConfig.getRoleServiceName())) {
            roleServiceName = webServiceBodyConfig.getRoleServiceName();
        } else {
            roleServiceName = null;
        }
        defaultSecurityService = null;
    }

    /** Read only store. */
    @Override
    public boolean canCreateStore() {
        return false;
    }

    /** Read only store. */
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
        listeners.add(listener);
    }

    @Override
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.remove(listener);
    }

    // -------------------------------------------------------------------------------

    public GeoServerUser loadUserByUsername(String username, final String responseBody) {
        GeoServerUser user = null;
        try {
            if (username == null) {
                throw new UsernameNotFoundException(userNotFoundMessage(username));
            }
            user = new GeoServerUser(username);
            user.setAuthorities(extractRoles(responseBody));
        } catch (IOException e) {
            throw new UsernameNotFoundException(userNotFoundMessage(username), e);
        }

        return user;
    }

    private Set<? extends GrantedAuthority> extractRoles(final String responseBody)
            throws IOException {
        final Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();

        Matcher matcher = searchRolesRegex.matcher(responseBody);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                for (String roleName : matcher.group(i).split(",")) {
                    authorities.add(createAuthorityObject(roleName.trim()));
                }
            }
        }
        if (authorities.size() == 0) {
            LOGGER.log(
                    Level.WARNING,
                    "Error in WebServiceAuthenticationKeyMapper, cannot find any Role in response adding anonymous role");
            authorities.add(GeoServerRole.ANONYMOUS_ROLE);
        }

        RoleCalculator calc = new RoleCalculator(this, getDefaultSecurityService());
        if (calc != null) {
            final SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();
            for (GrantedAuthority authority : authorities) {
                groups.add(createGroupObject(authority.getAuthority(), true));
            }

            for (GeoServerUserGroup group : groups) {
                if (group.isEnabled()) {
                    for (GeoServerRole role : calc.calculateRoles(group)) {
                        if (!authorities.contains(role)) {
                            authorities.add(role);
                        }
                    }
                }
            }
        }

        // Check if Role Admin and Anonymous are present other than other roles
        if (authorities.size() > 0) {
            for (GrantedAuthority authority : authorities) {
                if (authority.equals(GeoServerRole.ADMIN_ROLE)
                        || authority.equals(GeoServerRole.GROUP_ADMIN_ROLE)
                        || authority.getAuthority().equals(DEFAULT_LOCAL_ADMIN_ROLE)
                        || authority.getAuthority().equals(DEFAULT_LOCAL_GROUP_ADMIN_ROLE)) {
                    authorities.clear();
                    authorities.add(GeoServerRole.ADMIN_ROLE);
                    break;
                }

                if (authorities.size() > 1 && authority.equals(GeoServerRole.ANONYMOUS_ROLE)) {
                    authorities.remove(authority);
                    break;
                }
            }
        }

        return Collections.unmodifiableSet(authorities);
    }

    protected String userNotFoundMessage(String username) {
        return "User  " + username + " not found in usergroupservice: " + getName();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String response = WebServiceAuthenticationKeyMapper.RECORDED_RESPONSE.get();
        return loadUserByUsername(username, response);
    }

    @Override
    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        for (GeoServerUserGroup group : availableGroups) {
            if (group.getGroupname().equalsIgnoreCase(groupname)) {
                return group;
            }
        }
        return new GeoServerUserGroup(
                (convertToUpperCase ? groupname.trim().toUpperCase() : groupname.trim()));
    }

    @Override
    public GeoServerUser getUserByUsername(String username) throws IOException {
        return null;
    }

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        return null;
    }

    @Override
    public GeoServerUserGroup createGroupObject(final String groupname, boolean isEnabled)
            throws IOException {
        String theGroupName =
                (convertToUpperCase ? groupname.trim().toUpperCase() : groupname.trim());
        if (!theGroupName.contains(groupPrefix)) {
            if (theGroupName.equals(GeoServerRole.ADMIN_ROLE.getAuthority())) {
                theGroupName =
                        GeoServerRole.GROUP_ADMIN_ROLE
                                .getAuthority()
                                .substring(rolePrefix.length());
            } else {
                // remove standard role prefix
                theGroupName = theGroupName.substring(rolePrefix.length());
                theGroupName = groupPrefix + theGroupName;
            }
        }

        GeoServerUserGroup group = new GeoServerUserGroup(theGroupName);
        group.setEnabled(isEnabled);

        return group;
    }

    protected GrantedAuthority createAuthorityObject(String role) throws IOException {
        return new GeoServerRole(rolePrefix + (convertToUpperCase ? role.toUpperCase() : role));
    }

    @Override
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        return emptyUserSet;
    }

    @Override
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        return availableGroups;
    }

    @Override
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        return emptyUserSet;
    }

    @Override
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        final SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();

        if (user.getAuthorities() != null) {
            for (GrantedAuthority authority : user.getAuthorities()) {
                groups.add(createGroupObject(authority.getAuthority(), true));
            }
        }

        return Collections.unmodifiableSortedSet(groups);
    }

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public String getPasswordValidatorName() {
        return passwordValidatorName;
    }

    /** @return the roleServiceName */
    public String getRoleServiceName() {
        return roleServiceName;
    }

    /** @param roleServiceName the roleServiceName to set */
    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    /** @return the defaultSecurityService */
    public GeoServerRoleService getDefaultSecurityService() {
        if (defaultSecurityService == null) {
            if (StringUtils.hasLength(roleServiceName)) {
                try {
                    for (String roleService : securityManager.listRoleServices()) {
                        if (roleService.equals(roleServiceName)) {
                            defaultSecurityService =
                                    securityManager.loadRoleService(roleServiceName);
                            break;
                        }
                    }
                } catch (IOException e) {
                    defaultSecurityService = null;
                }
            }

            if (defaultSecurityService == null) {
                defaultSecurityService = securityManager.getActiveRoleService();
            }
        }
        return defaultSecurityService;
    }

    /** @param defaultSecurityService the defaultSecurityService to set */
    public void setDefaultSecurityService(GeoServerRoleService defaultSecurityService) {
        this.defaultSecurityService = defaultSecurityService;
    }

    @Override
    public int getUserCount() throws IOException {
        return 0;
    }

    @Override
    public int getGroupCount() throws IOException {
        return availableGroups.size();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        return emptyUserSet;
    }

    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        return 0;
    }

    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        return emptyUserSet;
    }

    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        return 0;
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return emptyUserSet;
    }

    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return 0;
    }
}
