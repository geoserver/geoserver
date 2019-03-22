/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.impl.Util;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

/**
 * JDBC implementation of {@link GeoServerUserGroupService}
 *
 * @author christian
 */
public class JDBCUserGroupService extends AbstractJDBCService implements GeoServerUserGroupService {

    static final String DEFAULT_DML_FILE = "usersdml.xml";
    static final String DEFAULT_DDL_FILE = "usersddl.xml";

    protected SortedSet<GeoServerUser> emptyUsers;
    protected SortedSet<GeoServerUserGroup> emptyGroups;

    /** logger */
    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");

    protected Set<UserGroupLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<UserGroupLoadedListener>());

    protected String passwordEncoderName, passwordValidatorName;

    public JDBCUserGroupService() throws IOException {
        emptyUsers = Collections.unmodifiableSortedSet(new TreeSet<GeoServerUser>());
        emptyGroups = Collections.unmodifiableSortedSet(new TreeSet<GeoServerUserGroup>());
    }

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public String getPasswordValidatorName() {
        return passwordValidatorName;
    }

    @Override
    public boolean canCreateStore() {
        return true;
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        JDBCUserGroupStore store = new JDBCUserGroupStore();
        store.initializeFromService(this);
        return store;
    }

    /**
     * Uses {@link #initializeDSFromConfig(SecurityNamedServiceConfig)} and {@link
     * #checkORCreateJDBCPropertyFile(String, File, String)} for initializing
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        this.name = config.getName();
        passwordEncoderName = ((SecurityUserGroupServiceConfig) config).getPasswordEncoderName();
        passwordValidatorName = ((SecurityUserGroupServiceConfig) config).getPasswordPolicyName();
        initializeDSFromConfig(config);

        if (config instanceof JDBCUserGroupServiceConfig) {
            JDBCUserGroupServiceConfig jdbcConfig = (JDBCUserGroupServiceConfig) config;

            String fileNameDML = jdbcConfig.getPropertyFileNameDML();
            Resource file =
                    checkORCreateJDBCPropertyFile(fileNameDML, getConfigRoot(), DEFAULT_DML_FILE);
            dmlProps = Util.loadUniversal(file.in());

            String fileNameDDL = jdbcConfig.getPropertyFileNameDDL();
            if (fileNameDDL != null && fileNameDDL.length() > 0) {
                file =
                        checkORCreateJDBCPropertyFile(
                                fileNameDDL, getConfigRoot(), DEFAULT_DDL_FILE);
                ddlProps = Util.loadUniversal(file.in());
                createTablesIfRequired((JDBCSecurityServiceConfig) config);
            }

            GeoServerPasswordEncoder enc =
                    getSecurityManager().loadPasswordEncoder(passwordEncoderName);
            if (enc.getEncodingType() == PasswordEncodingType.ENCRYPT) {
                KeyStoreProvider prov = getSecurityManager().getKeyStoreProvider();
                String alias = prov.aliasForGroupService(name);
                if (prov.containsAlias(alias) == false) {
                    prov.setUserGroupKey(
                            name,
                            getSecurityManager()
                                    .getRandomPassworddProvider()
                                    .getRandomPasswordWithDefaultLength());
                    prov.storeKeyStore();
                }
            }
            enc.initializeFor(this);
            passwordValidatorName = jdbcConfig.getPasswordPolicyName();
        }
    }

    /** @see org.geoserver.security.jdbc.AbstractJDBCService#getOrderedNamesForCreate() */
    protected String[] getOrderedNamesForCreate() {
        return new String[] {
            "users.create",
            "userprops.create",
            "groups.create",
            "groupmembers.create",
            "groupmembers.indexcreate",
            "userprops.indexcreate1",
            "userprops.indexcreate2"
        };
    }
    /** @see org.geoserver.security.jdbc.AbstractJDBCService#getOrderedNamesForDrop() */
    protected String[] getOrderedNamesForDrop() {
        return new String[] {"groupmembers.drop", "groups.drop", "userprops.drop", "users.drop"};
    }

    /** @see org.geoserver.security.GeoServerUserGroupService#getUserByUsername(java.lang.String) */
    public GeoServerUser getUserByUsername(String username) throws IOException {

        Connection con = null;
        PreparedStatement ps = null, ps2 = null;
        ResultSet rs = null, rs2 = null;
        GeoServerUser u = null;
        try {
            con = getConnection();
            ps = getDMLStatement("users.keyed", con);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                String password = rs.getString(1);
                String enabledString = rs.getString(2);
                boolean isEnabled = convertFromString(enabledString);
                u = createUserObject(username, password, isEnabled);
                ps2 = getDMLStatement("userprops.selectForUser", con);
                ps2.setString(1, username);
                rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    String propName = rs2.getString(1);
                    Object propValue = rs2.getObject(2);
                    u.getProperties().put(propName, propValue == null ? "" : propValue);
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(null, ps2, rs2);
            closeFinally(con, ps, rs);
        }

        return u;
    }

    /**
     * @see org.geoserver.security.GeoServerUserGroupService#getGroupByGroupname(java.lang.String)
     */
    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        GeoServerUserGroup g = null;
        try {
            con = getConnection();
            ps = getDMLStatement("groups.keyed", con);
            ps.setString(1, groupname);
            rs = ps.executeQuery();
            if (rs.next()) {
                String enabledString = rs.getString(1);
                boolean isEnabled = convertFromString(enabledString);
                g = createGroupObject(groupname, isEnabled);
            }

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        return g;
    }

    /** @see org.geoserver.security.GeoServerUserGroupService#getUsers() */
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Map<String, GeoServerUser> map = new HashMap<String, GeoServerUser>();
        try {
            con = getConnection();
            ps = getDMLStatement("users.all", con);
            rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString(1);
                String password = rs.getString(2);
                String enabledString = rs.getString(3);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUser u = createUserObject(username, password, isEnabled);
                map.put(username, u);
            }

            ps.close();
            rs.close();

            ps = getDMLStatement("userprops.all", con);
            rs = ps.executeQuery();
            while (rs.next()) {
                String useName = rs.getString(1);
                String propName = rs.getString(2);
                Object propValue = rs.getString(3);
                GeoServerUser u = map.get(useName);
                if (u != null) {
                    u.getProperties().put(propName, propValue == null ? "" : propValue);
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(map.values());
        return Collections.unmodifiableSortedSet(users);
    }

    /** @see org.geoserver.security.GeoServerUserGroupService#getUserGroups() */
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Collection<GeoServerUserGroup> tmp = new ArrayList<GeoServerUserGroup>();
        try {
            con = getConnection();
            ps = getDMLStatement("groups.all", con);
            rs = ps.executeQuery();
            while (rs.next()) {
                String groupname = rs.getString(1);
                String enabledString = rs.getString(2);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUserGroup g = createGroupObject(groupname, isEnabled);
                tmp.add(g);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();
        groups.addAll(tmp);
        return Collections.unmodifiableSortedSet(groups);
    }

    /**
     * @see org.geoserver.security.GeoServerUserGroupService#createUserObject(java.lang.String,
     *     java.lang.String, boolean)
     */
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        GeoServerUser user = new GeoServerUser(username);
        user.setEnabled(isEnabled);
        user.setPassword(password);
        return user;
    }

    /**
     * @see org.geoserver.security.GeoServerUserGroupService#createGroupObject(java.lang.String,
     *     boolean)
     */
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        GeoServerUserGroup group = new GeoServerUserGroup(groupname);
        group.setEnabled(isEnabled);
        return group;
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#getGroupsForUser(org.geoserver.security.impl.GeoServerUser)
     */
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Collection<GeoServerUserGroup> tmp = new ArrayList<GeoServerUserGroup>();
        try {
            con = getConnection();
            ps = getDMLStatement("groupmembers.groupsForUser", con);
            ps.setString(1, user.getUsername());
            rs = ps.executeQuery();
            while (rs.next()) {
                String groupname = rs.getString(1);
                String enabledString = rs.getString(2);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUserGroup g = createGroupObject(groupname, isEnabled);
                tmp.add(g);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        TreeSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();
        groups.addAll(tmp);
        return Collections.unmodifiableSortedSet(groups);
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#getUsersForGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        Connection con = null;
        PreparedStatement ps = null, ps2 = null;
        ResultSet rs = null, rs2 = null;
        Map<String, GeoServerUser> map = new HashMap<String, GeoServerUser>();
        try {
            con = getConnection();
            ps = getDMLStatement("groupmembers.usersForGroup", con);

            ps.setString(1, group.getGroupname());
            rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString(1);
                String password = rs.getString(2);
                String enabledString = rs.getString(3);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUser u = createUserObject(username, password, isEnabled);
                map.put(username, u);
            }
            rs.close();
            ps.close();

            ps = getDMLStatement("userprops.userPropsForGroup", con);
            ps.setString(1, group.getGroupname());
            rs = ps.executeQuery();
            while (rs.next()) {
                String userName = rs.getString(1);
                String propName = rs.getString(2);
                Object propValue = rs.getObject(3);
                GeoServerUser u = map.get(userName);
                if (u != null) {
                    u.getProperties().put(propName, propValue == null ? "" : propValue);
                }
            }

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(null, ps2, rs2);
            closeFinally(con, ps, rs);
        }

        TreeSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(map.values());
        return Collections.unmodifiableSortedSet(users);
    }

    public int getUserCount() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count;
        try {
            con = getConnection();
            ps = getDMLStatement("users.count", con);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IOException("SQL query did not return a count");
            }
            count = rs.getInt(1);
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }
        return count;
    }

    public int getGroupCount() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count;
        try {
            con = getConnection();
            ps = getDMLStatement("groups.count", con);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IOException("SQL query did not return a count");
            }
            count = rs.getInt(1);
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }
        return count;
    }

    /** @see org.geoserver.security.GeoServerUserGroupService#load() */
    public void load() throws IOException {
        // do nothing
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#registerUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#unregisterUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.remove(listener);
    }

    /** Fire {@link UserGroupLoadedEvent} for all listeners */
    protected void fireUserGroupLoadedEvent() {
        UserGroupLoadedEvent event = new UserGroupLoadedEvent(this);
        for (UserGroupLoadedListener listener : listeners) {
            listener.usersAndGroupsChanged(event);
        }
    }

    /** The root configuration for the user group service. */
    public Resource getConfigRoot() throws IOException {
        return getSecurityManager().get("security/usergroup").get(getName());
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        GeoServerUser user = null;
        try {
            user = getUserByUsername(username);
            if (user == null) throw new UsernameNotFoundException(userNotFoundMessage(username));
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
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Map<String, GeoServerUser> map = new HashMap<String, GeoServerUser>();
        try {
            con = getConnection();
            ps = getDMLStatement("user.usersHavingProperty", con);
            ps.setString(1, propname);
            rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString(1);
                String password = rs.getString(2);
                String enabledString = rs.getString(3);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUser u = createUserObject(username, password, isEnabled);
                map.put(username, u);
            }

            ps.close();
            rs.close();

            ps = getDMLStatement("userprops.usersHavingProperty", con);
            ps.setString(1, propname);
            rs = ps.executeQuery();
            while (rs.next()) {
                String useName = rs.getString(1);
                String propName = rs.getString(2);
                Object propValue = rs.getString(3);
                GeoServerUser u = map.get(useName);
                if (u != null) {
                    u.getProperties().put(propName, propValue == null ? "" : propValue);
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(map.values());
        return Collections.unmodifiableSortedSet(users);
    }

    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return 0;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count;
        try {
            con = getConnection();
            ps = getDMLStatement("userprops.userCountHavingProperty", con);
            ps.setString(1, propname);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IOException("SQL query did not return a count");
            }
            count = rs.getInt(1);
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }
        return count;
    }

    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Map<String, GeoServerUser> map = new HashMap<String, GeoServerUser>();
        try {
            con = getConnection();
            ps = getDMLStatement("user.usersNotHavingProperty", con);
            ps.setString(1, propname);
            rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString(1);
                String password = rs.getString(2);
                String enabledString = rs.getString(3);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUser u = createUserObject(username, password, isEnabled);
                map.put(username, u);
            }

            ps.close();
            rs.close();

            ps = getDMLStatement("userprops.usersNotHavingProperty", con);
            ps.setString(1, propname);
            rs = ps.executeQuery();
            while (rs.next()) {
                String useName = rs.getString(1);
                String propName = rs.getString(2);
                Object propValue = rs.getString(3);
                GeoServerUser u = map.get(useName);
                if (u != null) {
                    u.getProperties().put(propName, propValue == null ? "" : propValue);
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(map.values());
        return Collections.unmodifiableSortedSet(users);
    }

    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return getUserCount();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count;
        try {
            con = getConnection();
            ps = getDMLStatement("userprops.userCountNotHavingProperty", con);
            ps.setString(1, propname);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IOException("SQL query did not return a count");
            }
            count = rs.getInt(1);
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }
        return count;
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {

        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        if (StringUtils.hasLength(propvalue) == false) return emptyUsers;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Map<String, GeoServerUser> map = new HashMap<String, GeoServerUser>();
        try {
            con = getConnection();
            ps = getDMLStatement("user.usersHavingPropertyValue", con);
            ps.setString(1, propname);
            ps.setString(2, propvalue);
            rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString(1);
                String password = rs.getString(2);
                String enabledString = rs.getString(3);
                boolean isEnabled = convertFromString(enabledString);
                GeoServerUser u = createUserObject(username, password, isEnabled);
                map.put(username, u);
            }

            ps.close();
            rs.close();

            ps = getDMLStatement("userprops.usersHavingPropertyValue", con);
            ps.setString(1, propname);
            ps.setString(2, propvalue);
            rs = ps.executeQuery();
            while (rs.next()) {
                String useName = rs.getString(1);
                String propName = rs.getString(2);
                Object propValue = rs.getString(3);
                GeoServerUser u = map.get(useName);
                if (u != null) {
                    u.getProperties().put(propName, propValue == null ? "" : propValue);
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }

        SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(map.values());
        return Collections.unmodifiableSortedSet(users);
    }

    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {

        if (StringUtils.hasLength(propname) == false) return 0;

        if (StringUtils.hasLength(propvalue) == false) return 0;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int count;
        try {
            con = getConnection();
            ps = getDMLStatement("userprops.userCountHavingPropertyValue", con);
            ps.setString(1, propname);
            ps.setString(2, propvalue);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new IOException("SQL query did not return a count");
            }
            count = rs.getInt(1);
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, rs);
        }
        return count;
    }
}
