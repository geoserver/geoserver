/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleHierarchyHelper;

/**
 * JDBC Implementation of {@link GeoServerRoleStore}
 *
 * @author christian
 */
public class JDBCRoleStore extends JDBCRoleService implements GeoServerRoleStore {

    protected boolean modified;
    protected Connection connection;

    /**
     * The identical connection is used until {@link #store()} or {@link #load()} is called. Within
     * a transaction it is not possible to use different connections.
     *
     * @see org.geoserver.security.jdbc.AbstractJDBCService#getConnection()
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null) connection = super.getConnection();
        return connection;
    }

    @Override
    protected void closeConnection(Connection con) throws SQLException {
        // do nothing
    }

    /** To be called at the the end of a transaction, frees the current {@link Connection} */
    protected void releaseConnection() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    /**
     * Executes {@link Connection#rollback()} and frees the connection object
     *
     * @see org.geoserver.security.jdbc.JDBCRoleService#load()
     */
    public void load() throws IOException {
        // Simply roll back the transaction
        try {
            getConnection().rollback();
            releaseConnection();

        } catch (SQLException ex) {
            throw new IOException(ex);
        }
        setModified(false);
    }

    protected void addRoleProperties(GeoServerRole role, Connection con)
            throws SQLException, IOException {
        if (role.getProperties().size() == 0) return; // nothing to do

        PreparedStatement ps = getDMLStatement("roleprops.insert", con);
        try {
            for (Object key : role.getProperties().keySet()) {
                Object propertyVal = role.getProperties().get(key);
                ps.setString(1, role.getAuthority());
                ps.setString(2, key.toString());
                ps.setObject(3, propertyVal);
                ps.execute();
            }
        } finally {
            closeFinally(null, ps, null);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#addRole(org.geoserver.security.impl.GeoserverRole)
     */
    public void addRole(GeoServerRole role) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("roles.insert", con);
            ps.setString(1, role.getAuthority());
            // ps.setNull(2, Types.VARCHAR);
            ps.execute();

            addRoleProperties(role, con);

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#updateRole(org.geoserver.security.impl.GeoserverRole)
     */
    public void updateRole(GeoServerRole role) throws IOException {

        // No attributes for update
        Connection con = null;
        PreparedStatement ps = null;
        try {

            con = getConnection();
            ps = getDMLStatement("roles.update", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("roleprops.deleteForRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            addRoleProperties(role, con);

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true); // we do as if there was an update
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#removeRole(org.geoserver.security.impl.GeoserverRole)
     */
    public boolean removeRole(GeoServerRole role) throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        boolean retval = false;
        try {
            con = getConnection();
            ps = getDMLStatement("roles.delete", con);
            ps.setString(1, role.getAuthority());
            ps.execute();
            retval = ps.getUpdateCount() > 0;

            ps.close();
            ps = getDMLStatement("userroles.deleteRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("grouproles.deleteRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("roleprops.deleteForRole", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

            ps.close();
            ps = getDMLStatement("roles.deleteParent", con);
            ps.setString(1, role.getAuthority());
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
        return retval;
    }

    /**
     * Executes {@link Connection#commit()} and frees the connection
     *
     * @see org.geoserver.security.GeoServerRoleStore#store()
     */
    public void store() throws IOException {
        // Simply commit the transaction
        try {
            getConnection().commit();
            releaseConnection();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
        setModified(false);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#associateRoleToUser(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("userroles.insert", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, username);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#disAssociateRoleFromUser(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("userroles.delete", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, username);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#associateRoleToGroup(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("grouproles.insert", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, groupname);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#disAssociateRoleFromGroup(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("grouproles.delete", con);
            ps.setString(1, role.getAuthority());
            ps.setString(2, groupname);
            ps.execute();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#setParentRole(org.geoserver.security.impl.GeoserverRole, org.geoserver.security.impl.GeoserverRole)
     */
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {

        RoleHierarchyHelper helper = new RoleHierarchyHelper(getParentMappings());
        if (helper.isValidParent(
                        role.getAuthority(), parentRole == null ? null : parentRole.getAuthority())
                == false)
            throw new IOException(
                    parentRole.getAuthority()
                            + " is not a valid parent for "
                            + role.getAuthority());

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("roles.parentUpdate", con);
            if (parentRole == null) ps.setNull(1, Types.VARCHAR);
            else ps.setString(1, parentRole.getAuthority());
            ps.setString(2, role.getAuthority());
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#clear()
     */
    public void clear() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            ps = getDMLStatement("grouproles.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("userroles.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("roleprops.deleteAll", con);
            ps.execute();
            ps.close();

            ps = getDMLStatement("roles.deleteAll", con);
            ps.execute();

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#initializeFromService(org.geoserver.security.GeoserverRoleService)
     */
    public void initializeFromService(GeoServerRoleService service) throws IOException {
        JDBCRoleService jdbcService = (JDBCRoleService) service;
        this.name = service.getName();
        this.adminRoleName = jdbcService.adminRoleName;
        this.groupAdminRoleName = jdbcService.groupAdminRoleName;
        this.datasource = jdbcService.datasource;
        this.ddlProps = jdbcService.ddlProps;
        this.dmlProps = jdbcService.dmlProps;
        this.securityManager = service.getSecurityManager();
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
