/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;

public class JdbcSecurityConfigValidator extends SecurityConfigValidator {

    /**
     * System property or environment variable adding driver parameters to the built-in list. Format is
     * {@code driverClass:p1,p2;driverClass2:p3}.
     */
    static final String EXTRA_PARAMETERS_PROPERTY = "GEOSERVER_JDBC_EXTRA_PARAMETERS";

    /** Classpath resource listing the accepted connection parameters per driver class. */
    private static final String DRIVER_PARAMETERS_RESOURCE = "driver-parameters.properties";

    /** Legacy driver class names mapped to the entry that carries their parameter list. */
    private static final Map<String, String> DRIVER_ALIASES = Map.of(
            "org.hsqldb.jdbcDriver", "org.hsqldb.jdbc.JDBCDriver",
            "com.mysql.jdbc.Driver", "com.mysql.cj.jdbc.Driver",
            "oracle.jdbc.driver.OracleDriver", "oracle.jdbc.OracleDriver");

    /** JDBC URL sub-protocol handled by each known driver class. */
    private static final Map<String, String> DRIVER_SUBPROTOCOLS = Map.of(
            "org.h2.Driver", "h2",
            "org.postgresql.Driver", "postgresql",
            "org.sqlite.JDBC", "sqlite",
            "org.hsqldb.jdbc.JDBCDriver", "hsqldb",
            "com.mysql.cj.jdbc.Driver", "mysql",
            "org.mariadb.jdbc.Driver", "mariadb",
            "oracle.jdbc.OracleDriver", "oracle",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver");

    /**
     * Sub-protocols that accept connection properties inside the host part, a form the validator does not check: MySQL
     * and MariaDB host properties, Oracle TNS descriptors.
     */
    private static final Set<String> HOST_PROPERTY_SUBPROTOCOLS = Set.of("mysql", "mariadb", "oracle");

    /** Built-in connection parameters per canonical driver class, parameter names in lower case. */
    private static final Map<String, Set<String>> BUILT_IN_PARAMS = loadBuiltInParams();

    /**
     * Connection URL parameters accepted per canonical driver class, in lower case. A driver absent from the map, or a
     * parameter absent from its set, makes the connection URL invalid. Read from {@link #EXTRA_PARAMETERS_PROPERTY}
     * when the validator is built.
     */
    private final Map<String, Set<String>> knownParams;

    public JdbcSecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
        this.knownParams = withExtraParams(GeoServerExtensions.getProperty(EXTRA_PARAMETERS_PROPERTY));
    }

    private static Map<String, Set<String>> loadBuiltInParams() {
        Properties props = new Properties();
        try (InputStream in = JdbcSecurityConfigValidator.class.getResourceAsStream(DRIVER_PARAMETERS_RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing resource " + DRIVER_PARAMETERS_RESOURCE);
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + DRIVER_PARAMETERS_RESOURCE, e);
        }

        Map<String, Set<String>> map = new HashMap<>();
        for (String driver : props.stringPropertyNames()) {
            Set<String> params = new HashSet<>();
            addParams(params, props.getProperty(driver));
            map.put(driver, Set.copyOf(params));
        }
        return Map.copyOf(map);
    }

    private static Map<String, Set<String>> withExtraParams(String extra) {
        if (extra == null || extra.isBlank()) return BUILT_IN_PARAMS;
        Map<String, Set<String>> map = new HashMap<>();
        BUILT_IN_PARAMS.forEach((driver, params) -> map.put(driver, new HashSet<>(params)));
        for (String entry : extra.split(";")) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            String driver = entry.substring(0, colon).trim();
            if (driver.isEmpty()) continue;
            addParams(map.computeIfAbsent(canonical(driver), d -> new HashSet<>()), entry.substring(colon + 1));
        }
        return map;
    }

    /** Maps a legacy driver class name to the current one, so both share one parameter list. */
    private static String canonical(String driverClassName) {
        return DRIVER_ALIASES.getOrDefault(driverClassName, driverClassName);
    }

    private static void addParams(Set<String> target, String csv) {
        for (String param : csv.split(",")) {
            String name = param.trim();
            if (!name.isEmpty()) target.add(name.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;

        validateFileNames(jdbcConfig, JDBCRoleService.DEFAULT_DDL_FILE, JDBCRoleService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);

        if (jdbcConfig.isJndi()) validateJNDI(jdbcConfig);
        else validateJDBC(jdbcConfig);
    }

    @Override
    public void validate(SecurityUserGroupServiceConfig config) throws SecurityConfigException {
        super.validate(config);

        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;

        validateFileNames(jdbcConfig, JDBCUserGroupService.DEFAULT_DDL_FILE, JDBCUserGroupService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);

        if (jdbcConfig.isJndi()) validateJNDI(jdbcConfig);
        else validateJDBC(jdbcConfig);
    }

    protected void checkAutomaticTableCreation(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (config.isCreatingTables()) {
            if (isNotEmpty(config.getPropertyFileNameDDL()) == false) throw createSecurityException(DDL_FILE_REQUIRED);
        }
    }

    protected void validateFileNames(JDBCSecurityServiceConfig config, String defaultDDL, String defaultDML)
            throws SecurityConfigException {

        String fileName = config.getPropertyFileNameDDL();
        // ddl may be null
        if (isNotEmpty(fileName)) {
            if (defaultDDL.equals(fileName) == false) {
                // not the default property file
                File file = new File(fileName);
                // the file is looked up in the security directory, so the name must be relative
                if (file.isAbsolute() || checkFile(file) == false) {
                    throw createSecurityException(DDL_FILE_INVALID, fileName);
                }
            }
        }

        fileName = config.getPropertyFileNameDML();
        if (isNotEmpty(fileName) == false) {
            // dml file is required
            throw createSecurityException(DML_FILE_REQUIRED);
        }

        if (defaultDML.equals(fileName) == false) {
            // not the default property file
            File file = new File(fileName);
            // the file is looked up in the security directory, so the name must be relative
            if (file.isAbsolute() || checkFile(file) == false) {
                throw createSecurityException(DML_FILE_INVALID, fileName);
            }
        }
    }

    /** Creates {@link JDBCSecurityConfigException}, so the messages are looked up under the JDBC keys. */
    @Override
    protected SecurityConfigException createSecurityException(String errorid, Object... args) {
        return new JDBCSecurityConfigException(errorid, args);
    }

    protected void validateJNDI(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getJndiName()) == false) throw createSecurityException(JNDINAME_REQUIRED);
    }

    /**
     * Checks a JDBC connection URL against its driver class and the connection parameters known for that driver. Throws
     * for a missing driver or URL, for an unlisted driver, for a URL handled by a different driver, and for any
     * parameter not listed for the driver. Parameter names are compared ignoring case.
     */
    public void validateJdbcConnectionUrl(String driverClassName, String url) throws SecurityConfigException {
        if (!isNotEmpty(driverClassName)) throw createSecurityException(DRIVER_CLASSNAME_REQUIRED);
        if (!isNotEmpty(url)) throw createSecurityException(JDBCURL_REQUIRED);
        Set<String> known = knownParams.get(canonical(driverClassName));
        if (known == null) throw createSecurityException(JDBCURL_DRIVER_UNSUPPORTED_$1, driverClassName);

        // DriverManager picks the driver from the URL, so the URL must belong to the configured driver
        if (!url.startsWith("jdbc:")) throw createSecurityException(JDBCURL_DRIVER_MISMATCH_$1, driverClassName);
        int colon = url.indexOf(':', 5);
        String subprotocol = colon < 0 ? "" : url.substring(5, colon);
        String expected = DRIVER_SUBPROTOCOLS.get(canonical(driverClassName));
        if (expected != null ? !expected.equals(subprotocol) : DRIVER_SUBPROTOCOLS.containsValue(subprotocol)) {
            throw createSecurityException(JDBCURL_DRIVER_MISMATCH_$1, driverClassName);
        }

        // parameters follow '?' for postgresql, sqlite and mysql, ';' for h2, hsqldb and sqlserver
        int start = firstIndexOf(url, '?', ';');
        String hostPart = url.substring(0, start < 0 ? url.length() : start);
        if (HOST_PROPERTY_SUBPROTOCOLS.contains(subprotocol) && hostPart.indexOf('(') >= 0
                || "oracle".equals(subprotocol) && !isOracleAddress(hostPart)) {
            throw createSecurityException(JDBCURL_HOST_PROPERTIES_$1, driverClassName);
        }
        // a lone trailing separator carries no parameter
        if (start < 0 || start == url.length() - 1) return;

        for (String param : url.substring(start + 1).split("[&;]")) {
            String key = param.split("=", 2)[0];
            if (key.isEmpty()) throw createSecurityException(JDBCURL_PARAMETER_EMPTY);
            if (!known.contains(key.toLowerCase(Locale.ROOT))) {
                throw createSecurityException(JDBCURL_PARAMETER_UNKNOWN_$2, key, driverClassName);
            }
        }
    }

    /**
     * Accepts an Oracle address in host or {@code tcp(s)://} form. Other schemes, such as {@code ldap://} or
     * {@code config-file://}, make the driver load connection properties from a source the validator cannot read.
     */
    private static boolean isOracleAddress(String hostPart) {
        String address = hostPart.substring(hostPart.indexOf('@') + 1);
        return !address.contains("://") || address.startsWith("tcp://") || address.startsWith("tcps://");
    }

    private static int firstIndexOf(String s, char a, char b) {
        int ia = s.indexOf(a);
        int ib = s.indexOf(b);
        if (ia < 0) return ib;
        if (ib < 0) return ia;
        return Math.min(ia, ib);
    }

    protected void validateJDBC(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getUserName()) == false) throw createSecurityException(USERNAME_REQUIRED);
        validateJdbcConnectionUrl(config.getDriverClassName(), config.getConnectURL());
        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(DRIVER_CLASS_NOT_FOUND_$1, config.getDriverClassName());
        }
    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCConnectAuthProviderConfig jdbcConfig = (JDBCConnectAuthProviderConfig) config;
        validateJdbcConnectionUrl(jdbcConfig.getDriverClassName(), jdbcConfig.getConnectURL());
        try {
            Class.forName(jdbcConfig.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(DRIVER_CLASS_NOT_FOUND_$1, jdbcConfig.getDriverClassName());
        }
    }
}
