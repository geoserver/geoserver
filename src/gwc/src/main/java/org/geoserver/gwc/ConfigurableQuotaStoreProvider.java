/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geowebcache.diskquota.DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.QuotaStoreProvider;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStoreFactory;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.layer.TileLayer;
import org.springframework.context.ApplicationContext;

/**
 * A quota store whose store is a {@link ConfigurableQuotaStore} whose delegate can be reloaded by
 * calling onto {@link #reloadQuotaStore()}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ConfigurableQuotaStoreProvider extends QuotaStoreProvider {

    static final Logger LOGGER = Logging.getLogger(ConfigurableQuotaStoreProvider.class);

    Exception exception;
    TilePageCalculator calculator;

    boolean diskQuotaEnabled;

    private JDBCConfigurationStorage jdbcConfigManager;

    public ConfigurableQuotaStoreProvider(
            ConfigLoader loader,
            TilePageCalculator calculator,
            JDBCConfigurationStorage jdbcConfigManager) {
        super(loader);
        this.calculator = calculator;
        this.jdbcConfigManager = jdbcConfigManager;

        boolean disabled =
                Boolean.valueOf(GeoServerExtensions.getProperty(GWC_DISKQUOTA_DISABLED))
                        .booleanValue();
        if (disabled) {
            LOGGER.warning(
                    " -- Found environment variable "
                            + GWC_DISKQUOTA_DISABLED
                            + " set to true. DiskQuotaMonitor is disabled.");
        }
        this.diskQuotaEnabled = !disabled;
    }

    @Override
    public void reloadQuotaStore() throws ConfigurationException, IOException {
        if (!diskQuotaEnabled) {
            store = null;
            return;
        }

        // get the quota store name
        DiskQuotaConfig config = loader.loadConfig();
        QuotaStore store = null;
        if (!config.isEnabled()) {
            // it would be nice to just return null, but the other portions of the
            // disk quota system will throw exceptions if we did while the quota store
            // is not disable via system variable. Let's just give it a dummy quota store instead.
            store = new DummyQuotaStore(calculator);
        } else {
            String quotaStoreName = config.getQuotaStore();
            // in case it's null GeoServer defaults to H2 store, we don't have the
            // BDB store in the classpath
            if (quotaStoreName == null) {
                quotaStoreName = JDBCQuotaStoreFactory.H2_STORE;
            }

            try {
                store = getQuotaStoreByName(quotaStoreName);
                exception = null;
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Failed to get a quota store, "
                                + "the GeoWebCache disk quota subsystem will stop working now",
                        e);
                this.exception = e;
                store = new DummyQuotaStore(calculator);
            }
        }

        if (this.store == null) {
            this.store = new ConfigurableQuotaStore(store);
        } else {
            ConfigurableQuotaStore configurable = (ConfigurableQuotaStore) this.store;
            QuotaStore oldStore = configurable.getStore();
            configurable.setStore(store);
            // clean up the quota information gathered so far, otherwise when re-enabling
            // we'll have in the db stale information
            if (!(oldStore instanceof DummyQuotaStore)) {
                try {
                    for (TileLayer tl : GWC.get().getTileLayers()) {
                        oldStore.deleteLayer(tl.getName());
                    }
                } finally {
                    try {
                        oldStore.close();
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.SEVERE,
                                "An error occurred while closing up the previous quota store",
                                e);
                    }
                }
            }
        }
    }

    /** The exception occurred during the last attempt to load the quota store, if any */
    public Exception getException() {
        return exception;
    }

    @Override
    protected QuotaStore getQuotaStoreByName(String quotaStoreName)
            throws ConfigurationException, IOException {
        if ("JDBC".equals(quotaStoreName)) {
            return loadJDBCQuotaStore(applicationContext, quotaStoreName);
        } else {
            return super.getQuotaStoreByName(quotaStoreName);
        }
    }

    private QuotaStore loadJDBCQuotaStore(
            ApplicationContext applicationContext, String quotaStoreName)
            throws ConfigurationException, IOException {
        // special case for the JDBC quota store, allows us to unencrypt passwords before
        // creating the GUI
        JDBCConfiguration config = jdbcConfigManager.getJDBCDiskQuotaConfig();
        JDBCQuotaStoreFactory factory = new JDBCQuotaStoreFactory();
        factory.setApplicationContext(applicationContext);
        return factory.getJDBCStore(applicationContext, config);
    }
}
