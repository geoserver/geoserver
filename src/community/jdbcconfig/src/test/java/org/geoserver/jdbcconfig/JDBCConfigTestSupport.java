/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig;

import static org.easymock.classextension.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.DbMappings;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;

@SuppressWarnings("unused")
public class JDBCConfigTestSupport {

    public static File createTempDir() throws IOException {
        File f = File.createTempFile("jdbcconfig", "data", new File("target"));
        f.delete();
        f.mkdirs();
        return f;
    }

    public static class DBConfig {
        String name;
        String driver;
        String connectionUrl;
        String dbUser;
        String dbPasswd;
        BasicDataSource dataSource;

        public DBConfig(String name, String driver, String connectionUrl, String dbUser, String dbPasswd) {
            this.name = name;
            this.driver = driver;
            this.connectionUrl = connectionUrl;
            this.dbUser = dbUser;
            this.dbPasswd = dbPasswd;
        }

        DBConfig() {
        }

        BasicDataSource dataSource() throws Exception {
            if (dataSource != null) return dataSource;

            dataSource = new BasicDataSource() {

                @Override
                public synchronized void close() throws SQLException {
                    // do nothing
                }

            };
            dataSource.setDriverClassName(driver);
            dataSource.setUrl(connectionUrl.replace("${DATA_DIR}", createTempDir().getAbsolutePath()));
            dataSource.setUsername(dbUser);
            dataSource.setPassword(dbPasswd);

            dataSource.setMinIdle(3);
            dataSource.setMaxActive(10);
            Connection connection = dataSource.getConnection();
            connection.close();
            return dataSource;
        }

        @Override
        public String toString() {
            return name;
        }

        public String detailString() {
            return "DBConfig{" + "name=" + name + ", driver=" + driver + ", connectionUrl=" + connectionUrl + ", dbUser=" + dbUser + ", dbPasswd=" + dbPasswd + '}';
        }

    }

    private static List<Object[]> parameterizedDBConfigs;
    public static final List<Object[]> parameterizedDBConfigs() {
        if (parameterizedDBConfigs == null) {
            parameterizedDBConfigs = new ArrayList<Object[]>();
            for (DBConfig conf: getDBConfigurations()) {
                parameterizedDBConfigs.add(new Object[] {conf});
            }
        }
        return parameterizedDBConfigs;
    }

    static List<DBConfig> getDBConfigurations() {
        ArrayList<DBConfig> configs = new ArrayList<DBConfig>();

        dbConfig(configs, "h2", "org.h2.Driver", "jdbc:h2:file:${DATA_DIR}/geoserver");
        dbConfig(configs, "postgres", "org.postgresql.Driver", "jdbc:postgresql://localhost:5432/geoserver");
        dbConfig(configs, "oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@//localhost:49161/xe");

        return configs;
    }

    static String getProperty(String dbName, String property) {
        return System.getProperty("jdbcconfig." + dbName + "." + property);
    }

    public static void dbConfig(List<DBConfig> configs, String name, String driver, String connectionUrl) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("skipping " + name + " tests, enable via maven profile");
            return;
        }
        if ("true".equals(System.getProperty("jdbcconfig." + name + ".skip"))) {
            System.err.println("skipping " + name + " tests, enable via maven profile");
            return;
        }
        DBConfig conf = new DBConfig();
        conf.name = name;
        conf.driver = driver;
        conf.connectionUrl = connectionUrl;
        conf.dbUser = System.getProperty("user.name");
        conf.dbPasswd = "";

        connectionUrl = getProperty(name, "connectionUrl");
        if (connectionUrl != null) {
            conf.connectionUrl = connectionUrl;
        }
        String dbUser = getProperty(name, "dbUser");
        if (dbUser != null) {
            conf.dbUser = dbUser;
        }
        String dbPass = getProperty(name, "dbPasswd");
        if (dbPass != null) {
            conf.dbPasswd = dbPass;
        }
        try {
            conf.dataSource();
        } catch (Exception ex) {
            System.err.println("Unable to connect to datastore, either disable test or specify correct configuration:");
            System.out.println(ex.getMessage());
            System.out.println("Current configuration : " + conf.detailString());
            return;
        }
        configs.add(conf);
    }

    private final DBConfig dbConfig;

    private WebApplicationContext appContext;

    private GeoServerResourceLoader resourceLoader;

    private CatalogImpl catalog;

    private BasicDataSource dataSource;

    private ConfigDatabase configDb;

    public JDBCConfigTestSupport(DBConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void setUp() throws Exception {
        ConfigDatabase.LOGGER.setLevel(Level.FINER);

        resourceLoader = new GeoServerResourceLoader(createTempDir());

        // just to avoid hundreds of warnings in the logs about extension lookups with no app
        // context set
        appContext = createNiceMock(WebApplicationContext.class);
        GeoServerExtensionsHelper.init(appContext);

        configureAppContext(appContext);
        replay(appContext);

//        final File testDbDir = new File("target", "jdbcconfig");
//        FileUtils.deleteDirectory(testDbDir);
//        testDbDir.mkdirs();

        dataSource = dbConfig.dataSource();

        try {
            dropDb(dataSource);
        } catch (Exception ignored) {
        }
        initDb(dataSource);

        // use a context to initialize the ConfigDatabase as this will enable
        // transaction management making the tests much faster (and correcter)
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        // use the dataSource we just created
        context.getBean(Config.class).real = dataSource;
        configDb = context.getBean(ConfigDatabase.class);

        catalog = new CatalogImpl();
        configDb.setCatalog(catalog);
        configDb.initDb(null);
    }

    protected void configureAppContext(WebApplicationContext appContext) {
        expect(appContext.getBeansOfType((Class) anyObject()))
            .andReturn(Collections.EMPTY_MAP).anyTimes();
        expect(appContext.getBeanNamesForType((Class) anyObject()))
            .andReturn(new String[] {}).anyTimes();

        ServletContext servletContext = createNiceMock(ServletContext.class);
        replay(servletContext);

        expect(appContext.getServletContext()).andReturn(servletContext);
    }

    public void tearDown() throws Exception {
        if (dataSource != null) {
            dropDb(dataSource);
        }
        try {
            if (configDb != null) {
                configDb.dispose();
            }
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }

    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public WebApplicationContext getApplicationContext() {
        return appContext;
    }

    public ConfigDatabase getDatabase() {
        return configDb;
    }

    private void initDb(DataSource dataSource) throws Exception {
        String initScriptName = "initdb." + dbConfig.name + ".sql";
        runScript(initScriptName);
    }

    private void dropDb(DataSource dataSource) throws Exception {
        String dropScriptName = "dropdb." + dbConfig.name + ".sql";
        runScript(dropScriptName);
    }

    private void runScript(String dbScriptName) throws IOException {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        URL url = JDBCGeoServerLoader.class.getResource(dbScriptName);
        if (url == null) {
            throw new IllegalArgumentException("Script not found: " + getClass().getName() + "/"
                    + dbScriptName);
        }

        Util.runScript(url, template.getJdbcOperations(), null);
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public DbMappings getDbMappings() {
        return this.configDb.getDbMappings();
    }

    public CatalogImpl getCatalog() {
        return catalog;
    }

    @Configuration
    @EnableTransactionManagement
    public static class Config {
        DataSource real;
        // we need a datasource immediately, but don't have one so use this as
        // a delegate that uses the 'real' DataSource
        DataSource lazy = new BasicDataSource() {

            @Override
            protected synchronized DataSource createDataSource() throws SQLException {
                return real;
            }

        };

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        public ConfigDatabase configDatabase() {
            return new ConfigDatabase(dataSource(), new XStreamInfoSerialBinding(
                new XStreamPersisterFactory()));
        }

        @Bean
        public DataSource dataSource() {
            return lazy;
        }
    }
}
