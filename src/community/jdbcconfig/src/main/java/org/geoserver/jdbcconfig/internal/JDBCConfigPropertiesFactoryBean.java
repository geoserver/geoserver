package org.geoserver.jdbcconfig.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertiesFactoryBean;

public class JDBCConfigPropertiesFactoryBean extends PropertiesFactoryBean {

    private static final Logger LOGGER = Logging.getLogger(JDBCGeoServerLoader.class);
    
    static final String CONFIG_FILE = "jdbcconfig.properties";
    
    static final String CONFIG_SYSPROP = "jdbcconfig.properties";
    
    static final String JDBCURL_SYSPROP = "jdbcconfig.jdbcurl";
    
    static final String INITDB_SYSPROP = "jdbcconfig.initdb";
    
    static final String IMPORT_SYSPROP = "jdbcconfig.import";

    /**
     * DDL scripts copied to <data dir>/jdbcconfig_scripts/ on first startup
     */
    private static final String[] SCRIPTS = { "dropdb.h2.sql", "dropdb.mssql.sql",
            "dropdb.mysql.sql", "dropdb.oracle.sql", "dropdb.postgres.sql", "initdb.h2.sql",
            "initdb.mssql.sql", "initdb.mysql.sql", "initdb.oracle.sql", "initdb.postgres.sql" };

    private static final String[] SAMPLE_CONFIGS = { "jdbcconfig.properties.h2",
            "jdbcconfig.properties.postgres" };

    GeoServerResourceLoader resourceLoader;
    
    public JDBCConfigPropertiesFactoryBean(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    protected Properties createProperties() throws IOException {
        JDBCConfigProperties config = loadConfig();
        if (!config.isEnabled()) {
            LOGGER.info("jdbcconfig is disabled");
            return config;
        }
        return config;
    }

    private JDBCConfigProperties loadDefaultConfig() throws IOException {
        JDBCConfigProperties config = new JDBCConfigProperties(this);
        config.load(getClass().getResourceAsStream("/" + CONFIG_FILE));
        return config;
    }

    private JDBCConfigProperties loadConfig() throws IOException {
        //copy over sample scripts 
        JDBCConfigProperties config = loadDefaultConfig();

        /*
         * Find configuration, lookup heuristic is as follows.
         * 1. check system property "jdbcconfig.properties" for path/url to properties file
         * 2. check system properties jdbconfig.jdbcurl, jdbconfig.initdb, jdbcimport.import
         * 3. look for <GEOSERVER_DATA_DIR>/jdbcconfig/jdbcconfig.properties
         * 4. use built in defaults 
         */
        if (loadConfigFromURL(config)) {
            return config;
        }

        if (loadConfigFromSysProps(config)) {
            return config;
        }

        if (loadConfigFromDataDir(config)) {
            return config;
        }

        LOGGER.info("Configuring jdbcconfig from defaults");

        //copy over default config to data dir
        saveConfig(config, "Default GeoServer JDBC config driver and connection pool options." + 
            " Edit as appropriate.");
        copySampleConfigsToDataDir();
        copyScriptsToDataDir();
        
        return config;
    }

    private boolean loadConfigFromSysProps(JDBCConfigProperties config) throws IOException {
        String jdbcUrl = System.getProperty(JDBCURL_SYSPROP);
        if (jdbcUrl != null) {
            config.setJdbcUrl(jdbcUrl);

            config.setInitDb(Boolean.getBoolean(INITDB_SYSPROP));
            config.setImport(Boolean.getBoolean(IMPORT_SYSPROP));
            
            if (LOGGER.isLoggable(Level.INFO)) {
                StringBuilder msg = 
                    new StringBuilder("Configuring jdbcconfig from system properties:\n");
                msg.append("  ").append(JDBCURL_SYSPROP).append("=").append(jdbcUrl).append("\n");
                msg.append("  ").append(INITDB_SYSPROP).append("=").append(config.isInitDb()).append("\n");
                msg.append("  ").append(IMPORT_SYSPROP).append("=").append(config.isImport()).append("\n");
                LOGGER.info(msg.toString());
            }
            return true;
        }
        return false;
    }

    private boolean loadConfigFromURL(JDBCConfigProperties config) throws IOException {
        String propUrl = System.getProperty(CONFIG_SYSPROP);
        if (propUrl == null) {
            return false;
        }

        URL url = null;
        try {
            //try to parse directly as url
            try {
                url = new URL(propUrl);
            }
            catch(MalformedURLException e) {
                //failed, try as a file path
                File f = new File(propUrl);
                if (f.canRead() && f.exists()) {
                    url = DataUtilities.fileToURL(f);
                }
            }
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to read " + propUrl, e);
        }

        if (url != null) {
            LOGGER.info("Configuring jdbcconfig from " + url.toString());
            InputStream in = url.openStream();
            try {
                config.load(in);
            }
            finally {
                in.close();
            }
            return true;
        }
        
        LOGGER.severe("System property " + CONFIG_SYSPROP + " specified " + propUrl + " but could not be read, ignoring.");
        return false;
    }

    private boolean loadConfigFromDataDir(JDBCConfigProperties config) throws IOException {
        File propFile = new File(getBaseDir(), CONFIG_FILE);
        if (propFile.exists()) {
            LOGGER.info("Loading jdbcconfig properties from " + propFile.getAbsolutePath());
            FileInputStream stream = new FileInputStream(propFile);
            try {
                config.load(stream);
                return true;
            } finally {
                stream.close();
            }
        }
        return false;
    }

    void saveConfig(JDBCConfigProperties config) throws IOException {
        saveConfig(config, "");
    }

    private void saveConfig(JDBCConfigProperties config, String comment) throws IOException {
        File propFile = new File(getBaseDir(), CONFIG_FILE);
        
        try {
            propFile.createNewFile();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Can't create file " + propFile.getAbsolutePath(), e);
            return;
        }

        try {
            OutputStream out = new FileOutputStream(propFile);
            try {
                config.store(out, comment);
            } finally {
                out.close();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving jdbc config properties to file "
                + propFile.getAbsolutePath(), e);
            propFile.delete();
        }
    }

    private void copyScriptsToDataDir() throws IOException {
        final File scriptsDir = getScriptDir();
        Class<?> scope = JDBCGeoServerLoader.class;
        for (String scriptName : SCRIPTS) {
            File target = new File(scriptsDir, scriptName);
            if (!target.exists()) {
                resourceLoader.copyFromClassPath(scriptName, target, scope);
            }
        }
    }
    
    private void copySampleConfigsToDataDir() throws IOException {
        final File baseDirectory = getBaseDir();
        for (String sampleConfig : SAMPLE_CONFIGS) {
            File target = new File(baseDirectory, sampleConfig);
            if (!target.exists()) {
                resourceLoader.copyFromClassPath(sampleConfig, target);
            }
        }
    }

    File getDataDir() {
        return resourceLoader.getBaseDirectory();
    }

    File getBaseDir() throws IOException {
        return resourceLoader.findOrCreateDirectory("jdbcconfig");
    }

    File getScriptDir() throws IOException {
        return resourceLoader.findOrCreateDirectory("jdbcconfig", "scripts");
    }
}
