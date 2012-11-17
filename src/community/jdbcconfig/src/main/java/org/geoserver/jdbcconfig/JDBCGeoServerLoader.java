/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

public class JDBCGeoServerLoader extends DefaultGeoServerLoader {

    private static final Logger LOGGER = Logging.getLogger(JDBCGeoServerLoader.class);

    private static final String CONFIG_FILE = "jdbcconfig.properties";

    private CatalogFacade catalogFacade;

    private GeoServerFacade geoServerFacade;

    private boolean importCatalog;

    private URL initScript;

    /**
     * DDL scripts copied to <data dir>/jdbcconfig_scripts/ on first startup
     */
    private static final String[] SCRIPTS = { "dropdb.h2.sql", "dropdb.mssql.sql",
            "dropdb.mysql.sql", "dropdb.oracle.sql", "dropdb.postgres.sql", "initdb.h2.sql",
            "initdb.mssql.sql", "initdb.mysql.sql", "initdb.oracle.sql", "initdb.postgres.sql" };

    private static final String[] SAMPLE_CONFIGS = { "jdbcconfig.properties.h2",
            "jdbcconfig.properties.postgres" };

    public JDBCGeoServerLoader(GeoServerResourceLoader resourceLoader) throws Exception {
        super(resourceLoader);
        this.importCatalog = checkPropertiesFileInitialized();
    }

    public void setCatalogFacade(CatalogFacade catalogFacade) throws IOException {
        this.catalogFacade = catalogFacade;
        ConfigDatabase configDatabase = ((JDBCCatalogFacade) catalogFacade).getConfigDatabase();
        configDatabase.initDb(initScript);
    }

    public void setGeoServerFacade(GeoServerFacade geoServerFacade) {
        this.geoServerFacade = geoServerFacade;
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        Stopwatch sw = new Stopwatch().start();
        loadCatalogInternal(catalog, xp);
        sw.stop();
        System.err.println("Loaded catalog in " + sw.toString());
    }

    private void loadCatalogInternal(Catalog catalog, XStreamPersister xp) throws Exception {
        ((CatalogImpl) catalog).setFacade(catalogFacade);

        // if this is the first time loading up with jdbc configuration, migrate from old
        // file based structure
        if (importCatalog) {
            readCatalog(catalog, xp);
        }

    }

    @Override
    protected void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        ((GeoServerImpl) geoServer).setFacade(geoServerFacade);

        // if this is the first time loading up with bdb je configuration, migrate from old
        // file based structure
        if (importCatalog) {
            readConfiguration(geoServer, xp);
        }

        // do a post check to ensure things were loaded, for instance if we are starting from
        // an empty data directory all objects will be empty
        // TODO: this should really be moved elsewhere
        if (geoServer.getGlobal() == null) {
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }
        if (geoServer.getLogging() == null) {
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }
    }

    @Override
    public void reload() throws Exception {
        super.reload();
    }

    private boolean checkPropertiesFileInitialized() throws IOException, SQLException {
        File propsFile = new File(resourceLoader.getBaseDirectory(), CONFIG_FILE);
        final boolean previouslyConfigured = propsFile.exists();

        if (!previouslyConfigured) {
            copyScripts();
            createDefaultConfig(propsFile);
        }

        Properties configProps = new Properties();
        FileInputStream stream = new FileInputStream(propsFile);
        try {
            configProps.load(stream);
        } finally {
            stream.close();
        }

        final boolean importCatalog = Boolean.parseBoolean(configProps.getProperty("importCatalog",
                "false"));
        final boolean runInitScript = Boolean.parseBoolean(configProps.getProperty("runInitScript",
                "false"));

        final String initScript = configProps.getProperty("initScript");
        if (runInitScript) {
            File file = new File(initScript);
            Preconditions.checkState(file.exists(),
                    "Init script does not exist: " + file.getAbsolutePath());
            this.initScript = file.toURI().toURL();
            // success, set runInitScript and importCatalog to false for next startup
            configProps.put("importCatalog", "false");
            configProps.put("runInitScript", "false");
            OutputStream out = new FileOutputStream(propsFile);
            try {
                configProps.store(new OutputStreamWriter(out, "UTF-8"), "");
            } finally {
                out.close();
            }
        }
        return importCatalog;
    }

    private void createDefaultConfig(File propsFile) {
        try {
            propsFile.createNewFile();
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Can't create file "
                            + new File(resourceLoader.getBaseDirectory(), CONFIG_FILE)
                                    .getAbsolutePath(), e);
            return;
        }

        try {
            String classpathResource = "/" + CONFIG_FILE;
            File baseDirectory = resourceLoader.getBaseDirectory();
            Properties configProps = new Properties();
            configProps.load(getClass().getResourceAsStream(classpathResource));

            for (Entry<Object, Object> entry : configProps.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (value.contains("${GEOSERVER_DATA_DIR}")) {
                    value = value.replace("${GEOSERVER_DATA_DIR}", baseDirectory.getAbsolutePath());
                    configProps.put(key, value);
                }
            }

            OutputStream out = new FileOutputStream(propsFile);
            try {
                configProps.store(out,
                        "Default GeoServer JDBC config driver and connection pool options. "
                                + "Edit as appropriate");
            } finally {
                out.close();
            }

            LOGGER.info("Default jdbc config properties copied to data directory at "
                    + propsFile.getAbsolutePath());

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error copying default jdbc config properties to file "
                    + propsFile.getAbsolutePath(), e);
            propsFile.delete();
        }
    }

    /**
     * @throws IOException
     * 
     */
    private void copyScripts() throws IOException {
        final File scriptsDir = resourceLoader.findOrCreateDirectory("jdbcconfig_scripts");
        Class<?> scope = JDBCGeoServerLoader.class;
        for (String scriptName : SCRIPTS) {
            File target = new File(scriptsDir, scriptName);
            resourceLoader.copyFromClassPath(scriptName, target, scope);
        }

        final File baseDirectory = resourceLoader.getBaseDirectory();
        for (String sampleConfig : SAMPLE_CONFIGS) {
            File target = new File(baseDirectory, sampleConfig);
            resourceLoader.copyFromClassPath(sampleConfig, target);
        }
    }
}
