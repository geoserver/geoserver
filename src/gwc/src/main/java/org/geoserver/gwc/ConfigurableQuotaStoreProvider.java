/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.IOException;

import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.QuotaStoreProvider;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStoreFactory;

/**
 * A quota store whose store is a {@link ConfigurableQuotaStore} whose delegate can be reloaded by
 * calling onto {@link #reloadQuotaStore()}
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class ConfigurableQuotaStoreProvider extends QuotaStoreProvider {

    public ConfigurableQuotaStoreProvider(ConfigLoader loader) {
        super(loader);
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

        QuotaStore store = getQuotaStoreByName(quotaStoreName);
        if (this.store == null) {
            this.store = new ConfigurableQuotaStore(store);
        } else {
            ((ConfigurableQuotaStore) this.store).setStore(store);
        }
    }
    
     

}
