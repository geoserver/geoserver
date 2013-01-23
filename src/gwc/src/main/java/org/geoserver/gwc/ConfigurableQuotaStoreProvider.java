/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.QuotaStoreProvider;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStoreFactory;
import org.geowebcache.diskquota.storage.TilePageCalculator;

/**
 * A quota store whose store is a {@link ConfigurableQuotaStore} whose delegate can be reloaded by
 * calling onto {@link #reloadQuotaStore()}
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ConfigurableQuotaStoreProvider extends QuotaStoreProvider {
    
    static final Logger LOGGER = Logging.getLogger(ConfigurableQuotaStoreProvider.class);
    
    Exception exception;
    TilePageCalculator calculator;
    
    public ConfigurableQuotaStoreProvider(ConfigLoader loader, TilePageCalculator calculator) {
        super(loader);
        this.calculator = calculator;
    }
    
    @Override
    public void reloadQuotaStore() throws ConfigurationException, IOException {
        // get the quota store name
        DiskQuotaConfig config = loader.loadConfig();
        String quotaStoreName = config.getQuotaStore();
        // in case it's null GeoServer defaults to H2 store, we don't have the
        // BDB store in the classpath
        if(quotaStoreName == null) {
            quotaStoreName = JDBCQuotaStoreFactory.H2_STORE;
        }

        QuotaStore store  = null;
        try {
            store = getQuotaStoreByName(quotaStoreName);
            exception = null;
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get a quota store, " +
            		"the GeoWebCache disk quota subsystem will stop working now", e);
            this.exception = e;
            store = new DummyQuotaStore(calculator);
        }
        
        if (this.store == null) {
            this.store = new ConfigurableQuotaStore(store);
        } else {
            ((ConfigurableQuotaStore) this.store).setStore(store);
        }

    }

    /**
     * The exception occurred during the last attempt to load the quota store, if any
     * @return
     */
    public Exception getException() {
        return exception;
    }

}
