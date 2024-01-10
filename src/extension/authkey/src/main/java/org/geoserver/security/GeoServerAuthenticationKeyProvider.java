/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Security provider for auth-key authentication
 *
 * @author mcr
 */
public class GeoServerAuthenticationKeyProvider extends AbstractFilterProvider
        implements DisposableBean {

    // Use a counter to ensure a unique prefix for each pool.
    private static final AtomicInteger poolCounter = new AtomicInteger();
    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    private final GeoServerSecurityManager securityManager;

    private final ScheduledExecutorService scheduler;
    private final int autoSyncDelaySeconds;

    /**
     * @param securityManager the security manager
     * @param autoSyncDelaySeconds the delay in seconds between each synchronization
     */
    public GeoServerAuthenticationKeyProvider(
            GeoServerSecurityManager securityManager, int autoSyncDelaySeconds) {
        this.securityManager = securityManager;
        this.autoSyncDelaySeconds = autoSyncDelaySeconds;
        this.scheduler = Executors.newScheduledThreadPool(1, getThreadFactory());

        // schedule auto-sync thread
        Runnable authKeyMapperSyncTask = new AuthKeyMapperSyncRunnable();
        scheduler.scheduleAtFixedRate(
                authKeyMapperSyncTask,
                autoSyncDelaySeconds,
                autoSyncDelaySeconds,
                TimeUnit.SECONDS);
    }

    /** @return a thread factory for the eviction thread */
    private ThreadFactory getThreadFactory() {
        CustomizableThreadFactory tFactory =
                new CustomizableThreadFactory(
                        String.format(
                                "GeoServerAuthenticationKey-%d-", poolCounter.getAndIncrement()));
        tFactory.setDaemon(true);
        return tFactory;
    }

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("authKeyAuthentication", AuthenticationKeyFilterConfig.class);
        xp.getXStream().alias("authKeyRESTRoleService", GeoServerRestRoleServiceConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerAuthenticationKeyFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerAuthenticationKeyFilter();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new AuthenticationKeyFilterConfigValidator(securityManager);
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new GeoServerRestRoleService(config);
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return super.createUserGroupService(config);
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return GeoServerRestRoleService.class;
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return super.getUserGroupServiceClass();
    }

    @Override
    public boolean roleServiceNeedsLockProtection() {
        return false;
    }

    @Override
    public boolean userGroupServiceNeedsLockProtection() {
        return super.userGroupServiceNeedsLockProtection();
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public int getAutoSyncDelaySeconds() {
        return autoSyncDelaySeconds;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }

    /** Runnable task to synchronize the AuthenticationKeyMapper */
    class AuthKeyMapperSyncRunnable implements Runnable {
        @Override
        public void run() {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("AuthenticationKey Mapper Sync task running");
            }
            try {
                securityManager
                        .listFilters(GeoServerAuthenticationKeyFilter.class)
                        .forEach(this::doSync);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("AuthenticationKey Mapper Sync task completed");
            }
        }

        private void doSync(String filter) {
            AuthenticationKeyFilterConfig config = null;
            try {
                config =
                        (AuthenticationKeyFilterConfig)
                                securityManager.loadFilterConfig(filter, true);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Authentication key error ", e);
                throw new RuntimeException(e);
            }
            if (config != null && config.isAllowMapperKeysAutoSync()) {
                AuthenticationKeyMapper mapper =
                        (AuthenticationKeyMapper)
                                GeoServerExtensions.bean(config.getAuthKeyMapperName());
                mapper.setAuthenticationFilterName(filter);
                mapper.setSecurityManager(securityManager);
                mapper.setUserGroupServiceName(config.getUserGroupServiceName());
                int numberOfNewKeys = 0;
                try {
                    numberOfNewKeys = mapper.synchronize();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                "AuthenticationKey Mapper Sync task completed with "
                                        + numberOfNewKeys
                                        + " new keys");
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Authentication key error ", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
