/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig;

import static org.easymock.classextension.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.DbMappings;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;

public class JDBCConfigTestSupport {

    public static File createTempDir() throws IOException {
        File f = File.createTempFile("jdbcconfig", "data", new File("target"));
        f.delete();
        f.mkdirs();
        return f;
    }

//     String driver = "org.postgresql.Driver";
//    
//     String connectionUrl = "jdbc:postgresql://localhost:5432/geoserver";
//    
//     String initScriptName = "initdb.postgres.sql";
//    
//     String dropScriptName = "dropdb.postgres.sql";

    String driver = "org.h2.Driver";

    String connectionUrl = "jdbc:h2:file:${DATA_DIR}/geoserver";

    String initScriptName = "initdb.h2.sql";

    String dropScriptName = "dropdb.h2.sql";

     String dbUser = System.getProperty("user.name");
     String dbPasswd = "";

    private WebApplicationContext appContext;

    private GeoServerResourceLoader resourceLoader;

    private CatalogImpl catalog;

    private BasicDataSource dataSource;

    private ConfigDatabase configDb;

    public void setUp() throws Exception {
        ConfigDatabase.LOGGER.setLevel(Level.FINER);

        resourceLoader = new GeoServerResourceLoader(createTempDir());
        GeoserverDataDirectory.loader = resourceLoader;

        // just to avoid hundreds of warnings in the logs about extension lookups with no app
        // context set
        appContext = createNiceMock(WebApplicationContext.class);
        new GeoServerExtensions().setApplicationContext(appContext);

        configureAppContext(appContext);
        replay(appContext);

//        final File testDbDir = new File("target", "jdbcconfig");
//        FileUtils.deleteDirectory(testDbDir);
//        testDbDir.mkdirs();

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(connectionUrl.replace("${DATA_DIR}",
            resourceLoader.getBaseDirectory().getAbsolutePath()));
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPasswd);

        dataSource.setMinIdle(3);
        dataSource.setMaxActive(10);
        try {
            Connection connection = dataSource.getConnection();
            connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            dropDb(dataSource);
        } catch (Exception ignored) {
        }
        initDb(dataSource);

        XStreamInfoSerialBinding binding = new XStreamInfoSerialBinding(
                new XStreamPersisterFactory());

        catalog = new CatalogImpl();
        configDb = new ConfigDatabase(dataSource, binding);
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
        runScript(initScriptName);
    }

    private void dropDb(DataSource dataSource) throws Exception {
        runScript(dropScriptName);
    }

    private void runScript(String dbScriptName) throws IOException {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        String[] initScript = readDbScript(dbScriptName);
        JdbcOperations jdbcOperations = template.getJdbcOperations();
        for (String sentence : initScript) {
            try {
                jdbcOperations.update(sentence);
            } catch (DataAccessException e) {
                // e.printStackTrace();
                throw e;
            }
        }
    }

    private String[] readDbScript(final String scriptName) throws IOException {

        URL url = JDBCGeoServerLoader.class.getResource(scriptName);
        if (url == null) {
            throw new IllegalArgumentException("Script not found: " + getClass().getName() + "/"
                    + scriptName);
        }
        List<String> lines = Lists.newArrayList(Resources.readLines(url, Charset.forName("UTF-8")));
        for (Iterator<String> it = lines.iterator(); it.hasNext();) {
            if (it.next().trim().length() == 0) {
                it.remove();
            }
        }
        return lines.toArray(new String[lines.size()]);
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
}
