/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStoreFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Loads/save and tests the JDBC configuration in the GeoServer environment, adding support for the
 * GUI and password encryption
 *
 * @author Andrea Aime - GeoSolutions
 */
class JDBCConfigurationStorage implements ApplicationContextAware, SecurityManagerListener {

    static final Logger LOGGER = Logging.getLogger(JDBCConfigurationStorage.class);

    private JDBCPasswordEncryptionHelper passwordHelper;

    private ApplicationContext applicationContext;

    private Resource configDir;

    public JDBCConfigurationStorage(ResourceStore store, GeoServerSecurityManager securityManager) {
        GeoserverXMLResourceProvider configProvider =
                (GeoserverXMLResourceProvider)
                        GeoServerExtensions.bean("jdbcDiskQuotaConfigResourceProvider");
        this.configDir = configProvider.getConfigDirectory();
        this.passwordHelper = new JDBCPasswordEncryptionHelper(securityManager);
        securityManager.addListener(this);
    }

    public synchronized void saveDiskQuotaConfig(
            DiskQuotaConfig config, JDBCConfiguration jdbcConfig)
            throws ConfigurationException, IOException, InterruptedException {
        Resource configFile = configDir.get("geowebcache-diskquota-jdbc.xml");
        if ("JDBC".equals(config.getQuotaStore())) {
            JDBCConfiguration encrypted = passwordHelper.encryptPassword(jdbcConfig);
            try (OutputStream os = configFile.out()) {
                JDBCConfiguration.store(encrypted, os);
            }
        } else {
            if (Resources.exists(configFile) && !configFile.delete()) {
                LOGGER.log(
                        Level.SEVERE,
                        "Failed to delete "
                                + configFile
                                + ", this might cause misbehavior on GeoServer restart");
            }
        }
    }

    public synchronized JDBCConfiguration getJDBCDiskQuotaConfig()
            throws IOException, org.geowebcache.config.ConfigurationException {
        Resource configFile = configDir.get("geowebcache-diskquota-jdbc.xml");
        if (!Resources.exists(configFile)) {
            return null;
        }
        try {
            JDBCConfiguration configuration;
            try (InputStream is = configFile.in()) {
                configuration = JDBCConfiguration.load(is);
            }
            return passwordHelper.unencryptPassword(configuration);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load geowebcache-diskquota-jdbc.xml", e);
            return null;
        }
    }

    /**
     * Checks the JDBC quota store can be instantiated
     *
     * @param jdbcConfiguration the GWC diskquota JDBC configuration
     */
    public void testQuotaConfiguration(JDBCConfiguration jdbcConfiguration)
            throws ConfigurationException, IOException {
        JDBCQuotaStoreFactory factory = GeoServerExtensions.bean(JDBCQuotaStoreFactory.class);
        QuotaStore qs = null;
        try {
            qs = factory.getJDBCStore(applicationContext, jdbcConfiguration);
        } finally {
            if (qs != null) {
                try {
                    qs.close();
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Failed to dispose test quota store", e);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        // we can't know if the password encoder changed, so we check if the encrypted pwd changed
        // (unfortunately some password encoders change the encrypted password every time they are
        // called...)
        try {
            JDBCConfiguration config = getJDBCDiskQuotaConfig();
            if (config != null) {
                Resource configFile = configDir.get("geowebcache-diskquota-jdbc.xml");
                if (!Resources.exists(configFile)) {
                    return;
                }
                JDBCConfiguration c1;
                try (InputStream is = configFile.in()) {
                    c1 = JDBCConfiguration.load(is);
                }
                if (c1 == null || c1.getConnectionPool() == null) {
                    return;
                }
                String originalEncrypted = c1.getConnectionPool().getPassword();
                if (originalEncrypted == null) {
                    return;
                }
                JDBCConfiguration c2 = passwordHelper.unencryptPassword(c1);
                JDBCConfiguration c3 = passwordHelper.encryptPassword(c2);
                String newEncrypted = c3.getConnectionPool().getPassword();
                if (!originalEncrypted.equals(newEncrypted)) {
                    try (OutputStream os = configFile.out()) {
                        JDBCConfiguration.store(c3, os);
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
