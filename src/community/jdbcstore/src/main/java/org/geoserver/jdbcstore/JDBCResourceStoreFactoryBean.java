/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import com.google.common.base.Preconditions;
import javax.sql.DataSource;
import org.geoserver.jdbcstore.cache.ResourceCache;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Loads JDBCResourceStore if enabled or falls back to regular store if disabled.
 *
 * @author Niels Charlier
 */
public class JDBCResourceStoreFactoryBean implements FactoryBean<ResourceStore>, InitializingBean {

    private ResourceStore resourceStore;

    public JDBCResourceStoreFactoryBean(
            ResourceStore fallbackStore, DataSource ds, JDBCResourceStoreProperties config) {
        if (config.isEnabled()) {
            resourceStore = new JDBCResourceStore(ds, config, fallbackStore);
        } else {
            resourceStore = fallbackStore;
        }
    }

    public void setCache(ResourceCache cache) {
        if (resourceStore instanceof JDBCResourceStore) {
            ((JDBCResourceStore) resourceStore).setCache(cache);
        }
    }

    /**
     * Configure LockProvider used during {@link Resource#out()}.
     *
     * @param lockProvider LockProvider used for Resource#out()
     */
    public void setLockProvider(LockProvider lockProvider) {
        if (resourceStore instanceof JDBCResourceStore) {
            ((JDBCResourceStore) resourceStore).setLockProvider(lockProvider);
        }
    }

    /** Configure ResourceWatcher */
    public void setResourceNotificationDispatcher(ResourceNotificationDispatcher resourceWatcher) {
        if (resourceStore instanceof JDBCResourceStore) {
            ((JDBCResourceStore) resourceStore).setResourceNotificationDispatcher(resourceWatcher);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (resourceStore instanceof JDBCResourceStore) {
            JDBCResourceStore store = ((JDBCResourceStore) resourceStore);
            LockProvider lockProvider = store.getLockProvider();
            Preconditions.checkState(
                    lockProvider != null,
                    "LockProvider has not been set. Check your applicationContext.xml configuration file for JDBCResourceStoreFactoryBean");
        }
    }

    @Override
    public ResourceStore getObject() throws Exception {
        return resourceStore;
    }

    @Override
    public Class<?> getObjectType() {
        return ResourceStore.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
