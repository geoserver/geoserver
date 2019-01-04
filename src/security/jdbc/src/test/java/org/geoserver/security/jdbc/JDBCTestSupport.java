/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.Util;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;

public class JDBCTestSupport {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");

    public static void dropExistingTables(AbstractJDBCService service) throws IOException {
        Connection con = null;
        try {
            con = service.getDataSource().getConnection();
            dropExistingTables(service, con);
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException ex) {
            }
            ;;
        }
    }

    public static void dropExistingTables(AbstractJDBCService service, Connection con)
            throws IOException {
        PreparedStatement ps = null;
        try {
            for (String stmt : service.getOrderedNamesForDrop()) {
                try {
                    ps = service.getDDLStatement(stmt, con);
                    ps.execute();
                    ps.close();
                } catch (SQLException ex) {
                    // ex.printStackTrace();
                }
            }
            con.commit();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException ex) {
            }
            ;
        }
    }

    public static boolean isFixtureDisabled(String fixtureId) {
        final String property = System.getProperty("gs." + fixtureId);
        return property != null && "false".equals(property.toLowerCase());
    }

    protected static JDBCUserGroupServiceConfig createConfigObjectH2(
            String serviceName, GeoServerSecurityManager securityManager) {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();
        config.setName(serviceName);
        config.setConnectURL("jdbc:h2:target/h2/security");
        config.setDriverClassName("org.h2.Driver");
        config.setUserName("sa");
        config.setPassword("");
        config.setClassName(JDBCUserGroupService.class.getName());
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        config.setCreatingTables(false);
        config.setPasswordEncoderName(
                securityManager
                        .loadPasswordEncoder(GeoServerDigestPasswordEncoder.class)
                        .getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        return config;
    }

    protected static GeoServerUserGroupService createH2UserGroupService(
            String serviceName, GeoServerSecurityManager securityManager) throws Exception {

        if (securityManager.listUserGroupServices().contains(serviceName)) {
            GeoServerUserGroupService service = securityManager.loadUserGroupService(serviceName);
            if (service.canCreateStore()) {
                GeoServerUserGroupStore store = service.createStore();
                store.clear();
                store.store();
            }
            SecurityUserGroupServiceConfig old =
                    securityManager.loadUserGroupServiceConfig(serviceName);
            securityManager.removeUserGroupService(old);
        }

        securityManager.saveUserGroupService(createConfigObjectH2(serviceName, securityManager));
        return securityManager.loadUserGroupService(serviceName);
    }

    protected static JDBCUserGroupServiceConfig createConfigObjectH2Jndi(
            String serviceName, GeoServerSecurityManager securityManager) {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();
        config.setName(serviceName);
        config.setJndi(true);
        config.setJndiName("ds.h2");
        config.setClassName(JDBCUserGroupService.class.getName());
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        config.setCreatingTables(false);
        config.setPasswordEncoderName(
                securityManager
                        .loadPasswordEncoder(GeoServerDigestPasswordEncoder.class)
                        .getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        return config;
    }

    protected static GeoServerUserGroupService createH2UserGroupServiceFromJNDI(
            String serviceName, GeoServerSecurityManager securityManager) throws Exception {
        securityManager.saveUserGroupService(
                createConfigObjectH2Jndi(serviceName, securityManager));
        return securityManager.loadUserGroupService(serviceName);
    }

    protected static GeoServerRoleService createH2RoleService(
            String serviceName, GeoServerSecurityManager securityManager) throws Exception {

        if (securityManager.listRoleServices().contains(serviceName)) {
            if (securityManager.getActiveRoleService().getName().equals(serviceName)) {
                GeoServerRoleService roleService = securityManager.loadRoleService("default");
                securityManager.setActiveRoleService(roleService);
            }

            GeoServerRoleService service = securityManager.loadRoleService(serviceName);
            if (service.canCreateStore()) {
                GeoServerRoleStore store = service.createStore();
                store.clear();
                store.store();
            }

            SecurityRoleServiceConfig old = securityManager.loadRoleServiceConfig(serviceName);
            securityManager.removeRoleService(old);
        }
        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();

        config.setName(serviceName);
        config.setConnectURL("jdbc:h2:target/h2/security");
        config.setDriverClassName("org.h2.Driver");
        config.setUserName("sa");
        config.setPassword("");
        config.setClassName(JDBCRoleService.class.getName());
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        config.setCreatingTables(false);
        securityManager.saveRoleService(config);
        return securityManager.loadRoleService(serviceName);
    }

    protected static GeoServerRoleService createH2RoleServiceFromJNDI(
            String serviceName, GeoServerSecurityManager securityManager) throws Exception {

        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();

        config.setName(serviceName);
        config.setJndi(true);
        config.setJndiName("ds.h2");
        config.setClassName(JDBCRoleService.class.getName());
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        config.setCreatingTables(false);
        securityManager.saveRoleService(config);
        return securityManager.loadRoleService(serviceName);
    }

    protected static GeoServerRoleService createRoleService(
            String fixtureId, LiveDbmsDataSecurity data, GeoServerSecurityManager securityManager)
            throws Exception {

        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();

        Properties props = Util.loadUniversal(new FileInputStream(data.getFixture()));

        config.setName(fixtureId);
        config.setConnectURL(props.getProperty("url"));
        config.setDriverClassName(props.getProperty("driver"));
        config.setUserName(
                props.getProperty("user") == null
                        ? props.getProperty("username")
                        : props.getProperty("user"));
        config.setPassword(props.getProperty("password"));
        config.setClassName(JDBCRoleService.class.getName());
        config.setCreatingTables(false);
        if ("h2".equals(fixtureId)) {
            config.setPropertyFileNameDDL("rolesddl.h2.xml");
        } else if ("postgis".equals(fixtureId)) {
            config.setPropertyFileNameDDL("rolesddl.postgis.xml");
        } else if ("mysql".equals(fixtureId)) {
            config.setPropertyFileNameDDL("rolesddl.mysql.xml");
        } else {
            config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        }
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        securityManager.saveRoleService(config);
        return securityManager.loadRoleService(fixtureId);
    }

    protected static JDBCUserGroupServiceConfig createConfigObject(
            String fixtureId, LiveDbmsDataSecurity data, GeoServerSecurityManager securityManager)
            throws Exception {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();

        Properties props = Util.loadUniversal(new FileInputStream(data.getFixture()));

        config.setName(fixtureId);
        config.setConnectURL(props.getProperty("url"));
        config.setDriverClassName(props.getProperty("driver"));
        config.setUserName(
                props.getProperty("user") == null
                        ? props.getProperty("username")
                        : props.getProperty("user"));
        config.setPassword(props.getProperty("password"));
        config.setClassName(JDBCUserGroupService.class.getName());
        config.setCreatingTables(false);
        config.setPasswordEncoderName(
                securityManager
                        .loadPasswordEncoder(GeoServerDigestPasswordEncoder.class)
                        .getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        if ("mysql".equals(fixtureId)) {
            config.setPropertyFileNameDDL("usersddl.mysql.xml");
        } else {
            config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        }
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        return config;
    }

    protected static GeoServerUserGroupService createUserGroupService(
            String fixtureId, LiveDbmsDataSecurity data, GeoServerSecurityManager securityManager)
            throws Exception {
        securityManager.saveUserGroupService(createConfigObject(fixtureId, data, securityManager));
        return securityManager.loadUserGroupService(fixtureId);
    }
}
