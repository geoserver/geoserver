/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASSNAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASS_NOT_FOUND_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_DRIVER_MISMATCH_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_DRIVER_UNSUPPORTED_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_HOST_PROPERTIES_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_PARAMETER_EMPTY;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_PARAMETER_UNKNOWN_$2;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JNDINAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.USERNAME_REQUIRED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidatorTest;
import org.geotools.util.logging.Logging;
import org.junit.Test;

public class JdbcSecurityConfigValidatorTest extends SecurityConfigValidatorTest {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    @Override
    protected SecurityUserGroupServiceConfig createUGConfig(
            String name, Class<?> aClass, String encoder, String policyName) {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setPasswordEncoderName(encoder);
        config.setPasswordPolicyName(policyName);
        config.setCreatingTables(false);
        return config;
    }

    @Override
    protected SecurityRoleServiceConfig createRoleConfig(String name, Class<?> aClass, String adminRole) {
        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setAdminRoleName(adminRole);
        config.setCreatingTables(false);
        return config;
    }

    @Override
    protected SecurityAuthProviderConfig createAuthConfig(String name, Class<?> aClass, String userGroupServiceName) {
        JDBCConnectAuthProviderConfig config = new JDBCConnectAuthProviderConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setUserGroupServiceName(userGroupServiceName);
        return config;
    }

    @Override
    @Test
    public void testRoleConfig() throws IOException {

        super.testRoleConfig();

        JDBCRoleServiceConfig config = (JDBCRoleServiceConfig)
                createRoleConfig("jdbc", JDBCRoleService.class, AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);

        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        JDBCRoleServiceConfig configJNDI = (JDBCRoleServiceConfig)
                createRoleConfig("jndi", JDBCRoleService.class, AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        configJNDI.setJndi(true);
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());
        try {
            configJNDI.setJndiName("");
            validator.validateAddRoleService(configJNDI);
            // getSecurityManager().saveRoleService(configJNDI);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JNDINAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        try {
            config.setDriverClassName("");
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASSNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setDriverClassName("a.b.c");
        try {
            config.setUserName("");
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(USERNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserName("user");
        try {
            config.setConnectURL(null);
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        // driver is not on the list of known drivers, so the connection is rejected before the
        // driver class is ever looked up
        config.setDriverClassName("a.b.c");
        config.setConnectURL("jdbc:connect");
        try {
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_DRIVER_UNSUPPORTED_$1, ex.getId());
            assertArrayEquals(new Object[] {"a.b.c"}, ex.getArgs());
        }

        // driver is both known and resolvable on the test classpath
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectURL("jdbc:postgresql://localhost/db");
        config.setPropertyFileNameDDL(null);
        try {
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }

        GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);
        expect(secMgr.listRoleServices())
                .andReturn(new TreeSet<>(Arrays.asList("default", "jdbc")))
                .anyTimes();
        replay(secMgr);
        validator = new JdbcSecurityConfigValidator(secMgr);

        JDBCRoleServiceConfig oldConfig = new JDBCRoleServiceConfig(config);

        config.setPropertyFileNameDML(null);
        try {
            // getSecurityManager().saveRoleService(config);
            validator.validateModifiedRoleService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DML_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCRoleServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            try {
                // getSecurityManager().saveRoleService(config);
                validator.validateModifiedRoleService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DDL_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCRoleServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDML(invalidPath);
            try {
                // getSecurityManager().saveRoleService(config);
                validator.validateModifiedRoleService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DML_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        oldConfig = new JDBCRoleServiceConfig(config);

        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        try {
            // getSecurityManager().saveRoleService(config);
            validator.validateModifiedRoleService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DDL_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
    }

    @Override
    @Test
    public void testUserGroupConfig() throws IOException {

        super.testUserGroupConfig();

        JDBCUserGroupServiceConfig config = (JDBCUserGroupServiceConfig) createUGConfig(
                "jdbc",
                JDBCUserGroupService.class,
                getPlainTextPasswordEncoder().getName(),
                PasswordValidator.DEFAULT_NAME);

        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        JDBCUserGroupServiceConfig configJNDI = (JDBCUserGroupServiceConfig) createUGConfig(
                "jdbc",
                JDBCUserGroupService.class,
                getPlainTextPasswordEncoder().getName(),
                PasswordValidator.DEFAULT_NAME);
        configJNDI.setJndi(true);
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());
        try {
            configJNDI.setJndiName("");
            // getSecurityManager().saveUserGroupService(configJNDI);
            validator.validateAddUserGroupService(configJNDI);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JNDINAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        try {
            config.setDriverClassName("");
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASSNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setDriverClassName("a.b.c");
        try {
            config.setUserName("");
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(USERNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserName("user");
        try {
            config.setConnectURL(null);
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        // driver is not on the list of known drivers, so the connection is rejected before the
        // driver class is ever looked up
        config.setDriverClassName("a.b.c");
        config.setConnectURL("jdbc:connect");
        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_DRIVER_UNSUPPORTED_$1, ex.getId());
            assertArrayEquals(new Object[] {"a.b.c"}, ex.getArgs());
        }

        // driver is both known and resolvable on the test classpath
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectURL("jdbc:postgresql://localhost/db");
        config.setPropertyFileNameDDL(null);
        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }

        GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);
        expect(secMgr.listUserGroupServices())
                .andReturn(new TreeSet<>(Arrays.asList("default", "jdbc")))
                .anyTimes();

        GeoServerPlainTextPasswordEncoder pwEncoder = getPlainTextPasswordEncoder();
        expect(secMgr.loadPasswordEncoder(pwEncoder.getName()))
                .andReturn(pwEncoder)
                .anyTimes();
        expect(secMgr.listPasswordValidators())
                .andReturn(new TreeSet<>(Arrays.asList(PasswordValidator.DEFAULT_NAME)))
                .anyTimes();
        replay(secMgr);

        validator = new JdbcSecurityConfigValidator(secMgr);

        JDBCUserGroupServiceConfig oldConfig = new JDBCUserGroupServiceConfig(config);

        config.setPropertyFileNameDML(null);
        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateModifiedUserGroupService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DML_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCUserGroupServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            try {
                // getSecurityManager().saveUserGroupService(config);
                validator.validateModifiedUserGroupService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DDL_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCUserGroupServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDML(invalidPath);
            try {
                // getSecurityManager().saveUserGroupService(config);
                validator.validateModifiedUserGroupService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DML_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateModifiedUserGroupService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DDL_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
    }

    @Override
    @Test
    public void testAuthenticationProvider() throws IOException {
        super.testAuthenticationProvider();
        JDBCConnectAuthProviderConfig config =
                (JDBCConnectAuthProviderConfig) createAuthConfig("jdbcprov", JDBCConnectAuthProvider.class, "default");

        config.setConnectURL("jdbc:connect");

        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        try {
            config.setDriverClassName("");
            // getSecurityManager().saveAuthenticationProvider(config);
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASSNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        // driver is not on the list of known drivers, so the connection is rejected before the
        // driver class is ever looked up
        config.setDriverClassName("a.b.c");
        try {
            // getSecurityManager().saveAuthenticationProvider(config);
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_DRIVER_UNSUPPORTED_$1, ex.getId());
            assertArrayEquals(new Object[] {"a.b.c"}, ex.getArgs());
        }

        try {
            config.setConnectURL(null);
            // getSecurityManager().saveAuthenticationProvider(config);
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
    }

    @Test
    public void testValidateJdbcConnectionUrl() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        // a driver with no known parameters is rejected outright
        assertRejected(
                validator,
                "com.example.UnknownDriver",
                "jdbc:example://host/db",
                JDBCURL_DRIVER_UNSUPPORTED_$1,
                "com.example.UnknownDriver");

        // a URL with no query string is always allowed for a known driver
        validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://localhost/db");

        // a single known parameter is accepted
        validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://localhost/db?ssl=true");

        // several known parameters together are accepted
        validator.validateJdbcConnectionUrl(
                "org.postgresql.Driver", "jdbc:postgresql://localhost/db?ssl=true&sslmode=require&loginTimeout=10");

        // a parameter unknown to the driver is rejected on its own
        assertRejected(
                validator,
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost/db?extraOption=true",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "extraOption",
                "org.postgresql.Driver");

        // mixing one known and one unknown parameter still rejects the whole URL
        assertRejected(
                validator,
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost/db?ssl=true&extraOption=true",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "extraOption",
                "org.postgresql.Driver");

        // parameter names are compared ignoring case
        validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://localhost/db?SSLMODE=require");

        // a misspelled parameter name is rejected
        assertRejected(
                validator,
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost/db?sslmdoe=require",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "sslmdoe",
                "org.postgresql.Driver");

        // malformed/empty parameter segments are not silently skipped (fails closed)
        assertRejected(
                validator,
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost/db?ssl=true&&sslmode=require",
                JDBCURL_PARAMETER_EMPTY);

        // a bare flag-style parameter (no '=') is matched by name alone
        validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://localhost/db?ssl");

        // each driver has its own parameters
        validator.validateJdbcConnectionUrl("org.sqlite.JDBC", "jdbc:sqlite:file.db?mode=ro&cache=shared");
        assertRejected(
                validator,
                "org.sqlite.JDBC",
                "jdbc:sqlite:file.db?ssl=true",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "ssl",
                "org.sqlite.JDBC");
    }

    @Test
    public void testDriverMatchesUrl() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        validator.validateJdbcConnectionUrl("org.h2.Driver", "jdbc:h2:file:security/db");
        assertRejected(
                validator,
                "org.postgresql.Driver",
                "jdbc:h2:file:security/db",
                JDBCURL_DRIVER_MISMATCH_$1,
                "org.postgresql.Driver");
        assertRejected(
                validator,
                "org.h2.Driver",
                "jdbc:postgresql://localhost/db",
                JDBCURL_DRIVER_MISMATCH_$1,
                "org.h2.Driver");
        assertRejected(
                validator,
                "com.mysql.cj.jdbc.Driver",
                "jdbc:mariadb://host/db",
                JDBCURL_DRIVER_MISMATCH_$1,
                "com.mysql.cj.jdbc.Driver");
        assertRejected(
                validator,
                "org.postgresql.Driver",
                "postgresql://localhost/db",
                JDBCURL_DRIVER_MISMATCH_$1,
                "org.postgresql.Driver");
        // legacy class names share the sub-protocol of the current ones
        validator.validateJdbcConnectionUrl("com.mysql.jdbc.Driver", "jdbc:mysql://host/db");
        validator.validateJdbcConnectionUrl(
                "com.mysql.cj.jdbc.Driver", "jdbc:mysql:replication://host1,host2/db?connectTimeout=5");
    }

    @Test
    public void testMysqlHostPropertiesUnsupported() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        assertRejected(
                validator,
                "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://(host=host,connectTimeout=5)/db",
                JDBCURL_HOST_PROPERTIES_$1,
                "com.mysql.cj.jdbc.Driver");
        assertRejected(
                validator,
                "org.mariadb.jdbc.Driver",
                "jdbc:mariadb://address=(host=host)(port=3306)/db",
                JDBCURL_HOST_PROPERTIES_$1,
                "org.mariadb.jdbc.Driver");
        validator.validateJdbcConnectionUrl("com.mysql.cj.jdbc.Driver", "jdbc:mysql://host:3306/db?connectTimeout=5");
    }

    @Test
    public void testOracleDescriptorUnsupported() throws SecurityConfigException {
        assertRejected(
                validator(),
                "oracle.jdbc.OracleDriver",
                "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(HOST=host)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=db)))",
                JDBCURL_HOST_PROPERTIES_$1,
                "oracle.jdbc.OracleDriver");
    }

    /** Address schemes that load connection properties from elsewhere are rejected, plain and TCP addresses pass. */
    @Test
    public void testOracleAddressScheme() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = validator();
        for (String address :
                List.of("ldap://ldap/db,cn=OracleContext", "config-file:///tmp/db.json", "config-https://host/db")) {
            assertRejected(
                    validator,
                    "oracle.jdbc.OracleDriver",
                    "jdbc:oracle:thin:@" + address,
                    JDBCURL_HOST_PROPERTIES_$1,
                    "oracle.jdbc.OracleDriver");
        }
        for (String address : List.of("host:1521:db", "//host:1521/db", "tcp://host:1521/db", "tcps://host:2484/db")) {
            validator.validateJdbcConnectionUrl("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@" + address);
        }
    }

    /** The URL check is used alone by the connection test in the user interface, so it checks for missing values. */
    @Test
    public void testConnectionUrlRequired() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = validator();
        assertRejected(validator, "org.h2.Driver", "", JDBCURL_REQUIRED);
        assertRejected(validator, "org.h2.Driver", null, JDBCURL_REQUIRED);
        assertRejected(validator, "", "jdbc:h2:mem:db", DRIVER_CLASSNAME_REQUIRED);
        assertRejected(validator, null, "jdbc:h2:mem:db", DRIVER_CLASSNAME_REQUIRED);
    }

    /** Extra parameters given for a current driver class name apply to its legacy name, and the reverse. */
    @Test
    public void testExtraParametersAlias() throws SecurityConfigException {
        withExtraParameters("org.hsqldb.jdbc.JDBCDriver:extraA;org.hsqldb.jdbcDriver:extraB", () -> {
            JdbcSecurityConfigValidator validator = validator();
            for (String driver : List.of("org.hsqldb.jdbc.JDBCDriver", "org.hsqldb.jdbcDriver")) {
                validator.validateJdbcConnectionUrl(driver, "jdbc:hsqldb:mem:db;extraA=1;extraB=2");
            }
        });
    }

    @Test
    public void testH2ConnectionUrlParameters() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        validator.validateJdbcConnectionUrl(
                "org.h2.Driver", "jdbc:h2:file:security/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        validator.validateJdbcConnectionUrl("org.h2.Driver", "jdbc:h2:file:security/db;mode=PostgreSQL");
        assertRejected(
                validator,
                "org.h2.Driver",
                "jdbc:h2:file:security/db;AUTO_SERVR=TRUE",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "AUTO_SERVR",
                "org.h2.Driver");
    }

    @Test
    public void testJdbcConnectionUrlEnforcedOnSave() throws IOException {
        JDBCRoleServiceConfig config = (JDBCRoleServiceConfig) createRoleConfig(
                "jdbcUrlValidation", JDBCRoleService.class, AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        config.setDriverClassName("org.postgresql.Driver");
        config.setUserName("user");
        config.setConnectURL("jdbc:postgresql://localhost/db?extraOption=true");
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        try {
            validator.validateAddRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_PARAMETER_UNKNOWN_$2, ex.getId());
            assertArrayEquals(new Object[] {"extraOption", "org.postgresql.Driver"}, ex.getArgs());
        }

        // the same config with only known parameters must pass validation
        config.setConnectURL("jdbc:postgresql://localhost/db?ssl=true");
        try {
            validator.validateAddRoleService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }
    }

    @Test
    public void testJdbcConnectionUrlEnforcedOnSaveForAuthProvider() throws IOException {
        JDBCConnectAuthProviderConfig config = (JDBCConnectAuthProviderConfig)
                createAuthConfig("jdbcProvUrlValidation", JDBCConnectAuthProvider.class, "default");
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectURL("jdbc:postgresql://localhost/db?extraOption=true");

        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        try {
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_PARAMETER_UNKNOWN_$2, ex.getId());
            assertArrayEquals(new Object[] {"extraOption", "org.postgresql.Driver"}, ex.getArgs());
        }

        // the same config with only known parameters must pass validation
        config.setConnectURL("jdbc:postgresql://localhost/db?ssl=true");
        try {
            validator.validateAddAuthProvider(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }
    }

    @Test
    public void testHsqldbConnectionUrlParameters() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = new JdbcSecurityConfigValidator(getSecurityManager());

        // hsqldb separates parameters with ';', not '?'
        validator.validateJdbcConnectionUrl(
                "org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:file:mydb;ifexists=true;shutdown=true");
        assertRejected(
                validator,
                "org.hsqldb.jdbc.JDBCDriver",
                "jdbc:hsqldb:file:mydb;ifexists=true;other=x",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "other",
                "org.hsqldb.jdbc.JDBCDriver");
    }

    @Test
    public void testExtraParameters() throws SecurityConfigException {
        withExtraParameters("org.example.Driver:foo,bar", () -> {
            JdbcSecurityConfigValidator validator = validator();
            validator.validateJdbcConnectionUrl("org.example.Driver", "jdbc:example:db?foo=1&bar=2");
            assertRejected(
                    validator,
                    "org.example.Driver",
                    "jdbc:example:db?baz=3",
                    JDBCURL_PARAMETER_UNKNOWN_$2,
                    "baz",
                    "org.example.Driver");
            // a driver still absent from the extended list stays rejected
            assertRejected(
                    validator,
                    "org.other.Driver",
                    "jdbc:other:db?foo=1",
                    JDBCURL_DRIVER_UNSUPPORTED_$1,
                    "org.other.Driver");
        });
    }

    /** Malformed entries are skipped, and a built-in driver keeps its own parameters when extended. */
    @Test
    public void testExtraParametersMalformed() throws SecurityConfigException {
        withExtraParameters(" nocolon ; :orphan ; org.postgresql.Driver: myParam , ; org.example.Driver: ;", () -> {
            JdbcSecurityConfigValidator validator = validator();
            validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://h/db?ssl=true&MYPARAM=1");
            assertRejected(
                    validator,
                    "org.postgresql.Driver",
                    "jdbc:postgresql://h/db?orphan=1",
                    JDBCURL_PARAMETER_UNKNOWN_$2,
                    "orphan",
                    "org.postgresql.Driver");
            assertRejected(
                    validator,
                    "org.example.Driver",
                    "jdbc:example:db?x=1",
                    JDBCURL_PARAMETER_UNKNOWN_$2,
                    "x",
                    "org.example.Driver");
        });
    }

    /** A custom driver cannot claim the URLs of a known driver, which would skip that driver's parameter list. */
    @Test
    public void testExtraDriverKnownSubprotocol() throws SecurityConfigException {
        withExtraParameters(
                "org.example.Driver:socketFactory",
                () -> assertRejected(
                        validator(),
                        "org.example.Driver",
                        "jdbc:postgresql://h/db?socketFactory=x",
                        JDBCURL_DRIVER_MISMATCH_$1,
                        "org.example.Driver"));
    }

    @Test
    public void testDdlFileAbsolutePath() throws IOException {
        String ddl = new File("dummy.xml").getAbsolutePath();
        JDBCRoleServiceConfig config = postgresRoleConfig("jdbcDdlPath");
        config.setCreatingTables(true);
        config.setPropertyFileNameDDL(ddl);

        SecurityConfigException e =
                assertThrows(SecurityConfigException.class, () -> validator().validateAddRoleService(config));
        assertEquals(DDL_FILE_INVALID, e.getId());
        assertArrayEquals(new Object[] {ddl}, e.getArgs());
    }

    @Test
    public void testDmlFileAbsolutePath() throws IOException {
        String dml = new File("dummy.xml").getAbsolutePath();
        JDBCRoleServiceConfig config = postgresRoleConfig("jdbcDmlPath");
        config.setPropertyFileNameDML(dml);

        SecurityConfigException e =
                assertThrows(SecurityConfigException.class, () -> validator().validateAddRoleService(config));
        assertEquals(DML_FILE_INVALID, e.getId());
        assertArrayEquals(new Object[] {dml}, e.getArgs());
    }

    /** The Oracle driver is listed but not on the test classpath. */
    @Test
    public void testDriverClassNotFound() throws IOException {
        JDBCRoleServiceConfig config = postgresRoleConfig("jdbcNoDriver");
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setConnectURL("jdbc:oracle:thin:@localhost:1521:db");

        SecurityConfigException e =
                assertThrows(SecurityConfigException.class, () -> validator().validateAddRoleService(config));
        assertEquals(DRIVER_CLASS_NOT_FOUND_$1, e.getId());
        assertArrayEquals(new Object[] {"oracle.jdbc.OracleDriver"}, e.getArgs());
    }

    /** MySQL percent-decodes parameter names, so an encoded name must not match a listed one. */
    @Test
    public void testEncodedParameterName() {
        String mysql = "com.mysql.cj.jdbc.Driver";
        assertRejected(
                validator(),
                mysql,
                "jdbc:mysql://host/db?allowLoadLocal%49nfile=true",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "allowLoadLocal%49nfile",
                mysql);
        assertRejected(
                validator(),
                mysql,
                "jdbc:mysql://host/db?ssl%4Dode=REQUIRED",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "ssl%4Dode",
                mysql);
    }

    @Test
    public void testTrailingSeparator() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = validator();
        validator.validateJdbcConnectionUrl("org.h2.Driver", "jdbc:h2:mem:db;");
        validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://localhost/db?");
        validator.validateJdbcConnectionUrl("org.postgresql.Driver", "jdbc:postgresql://localhost/db?ssl=true&");
    }

    /** Legacy driver class names use the parameter list of the current class name. */
    @Test
    public void testAliasParameters() throws SecurityConfigException {
        JdbcSecurityConfigValidator validator = validator();
        validator.validateJdbcConnectionUrl("com.mysql.jdbc.Driver", "jdbc:mysql://host/db?sslMode=REQUIRED");
        validator.validateJdbcConnectionUrl("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:db;shutdown=true");
        validator.validateJdbcConnectionUrl(
                "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@host:1521:db?defaultRowPrefetch=50");
        assertRejected(
                validator,
                "com.mysql.jdbc.Driver",
                "jdbc:mysql://host/db?allowLoadLocalInfile=true",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "allowLoadLocalInfile",
                "com.mysql.jdbc.Driver");
    }

    @Test
    public void testDriverParameters() throws SecurityConfigException {
        JdbcSecurityConfigValidator v = new JdbcSecurityConfigValidator(getSecurityManager());

        v.validateJdbcConnectionUrl("com.mysql.cj.jdbc.Driver", "jdbc:mysql://host/db?sslMode=REQUIRED");
        assertRejected(
                v,
                "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://host/db?sslMod=REQUIRED",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "sslMod",
                "com.mysql.cj.jdbc.Driver");

        v.validateJdbcConnectionUrl("org.mariadb.jdbc.Driver", "jdbc:mariadb://host/db?sslMode=verify-full");
        assertRejected(
                v,
                "org.mariadb.jdbc.Driver",
                "jdbc:mariadb://host/db?ssl_mode=verify-full",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "ssl_mode",
                "org.mariadb.jdbc.Driver");

        v.validateJdbcConnectionUrl(
                "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://host;applicationName=gs");
        assertRejected(
                v,
                "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "jdbc:sqlserver://host;appName=gs",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "appName",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver");

        v.validateJdbcConnectionUrl("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@host:1521:db?defaultRowPrefetch=50");
        assertRejected(
                v,
                "oracle.jdbc.OracleDriver",
                "jdbc:oracle:thin:@host:1521:db?rowPrefetch=50",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "rowPrefetch",
                "oracle.jdbc.OracleDriver");

        v.validateJdbcConnectionUrl("org.sqlite.JDBC", "jdbc:sqlite:file.db?journal_mode=wal");
        assertRejected(
                v,
                "org.sqlite.JDBC",
                "jdbc:sqlite:file.db?journalmode=wal",
                JDBCURL_PARAMETER_UNKNOWN_$2,
                "journalmode",
                "org.sqlite.JDBC");
    }

    private static void assertRejected(
            JdbcSecurityConfigValidator validator, String driver, String url, String id, Object... args) {
        SecurityConfigException e =
                assertThrows(SecurityConfigException.class, () -> validator.validateJdbcConnectionUrl(driver, url));
        assertEquals(id, e.getId());
        assertArrayEquals(args, e.getArgs());
    }

    private JdbcSecurityConfigValidator validator() {
        return new JdbcSecurityConfigValidator(getSecurityManager());
    }

    private JDBCRoleServiceConfig postgresRoleConfig(String name) throws IOException {
        JDBCRoleServiceConfig config = (JDBCRoleServiceConfig)
                createRoleConfig(name, JDBCRoleService.class, AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        config.setDriverClassName("org.postgresql.Driver");
        config.setUserName("user");
        config.setConnectURL("jdbc:postgresql://localhost/db");
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        return config;
    }

    /** Runs the checks with {@link JdbcSecurityConfigValidator#EXTRA_PARAMETERS_PROPERTY} set, restoring it after. */
    private static void withExtraParameters(String value, ThrowingRunnable checks) throws SecurityConfigException {
        String previous = System.getProperty(JdbcSecurityConfigValidator.EXTRA_PARAMETERS_PROPERTY);
        System.setProperty(JdbcSecurityConfigValidator.EXTRA_PARAMETERS_PROPERTY, value);
        try {
            checks.run();
        } finally {
            if (previous == null) {
                System.clearProperty(JdbcSecurityConfigValidator.EXTRA_PARAMETERS_PROPERTY);
            } else {
                System.setProperty(JdbcSecurityConfigValidator.EXTRA_PARAMETERS_PROPERTY, previous);
            }
        }
    }

    private interface ThrowingRunnable {
        void run() throws SecurityConfigException;
    }
}
