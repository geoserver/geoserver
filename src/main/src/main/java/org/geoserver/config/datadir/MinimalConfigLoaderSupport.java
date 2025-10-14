/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServicePersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Support class for ensuring configuration consistency in GeoServer across all deployment scenarios.
 *
 * <p>This class addresses a specific issue that affects both standalone and clustered GeoServer deployments: when
 * starting with an empty or incomplete data directory, essential configuration elements (global settings, logging
 * configuration, and root services) may only exist in memory but not be persisted to the data directory. This leads to
 * different GeoServer instances generating different object IDs for the same logical configuration elements.
 *
 * <p>Example scenario using a file-based data directory:
 *
 * <ol>
 *   <li>A new GeoServer instance starts with an empty data directory
 *   <li>Default services are created in memory but not persisted (historically)
 *   <li>If the instance restarts, or another instance starts using the same data directory, new service objects with
 *       different IDs are created
 *   <li>This class ensures these essential configuration elements are consistently persisted and shared
 * </ol>
 *
 * <p>This consistency is particularly important for clustered deployments (using any configuration synchronization
 * mechanism), where all nodes need to share the same object IDs to properly synchronize configuration changes. However,
 * it also benefits standalone GeoServer by ensuring configuration stability across restarts.
 *
 * <p>For typical vanilla GeoServer installations that start with a pre-populated data directory, this class effectively
 * becomes a no-op, as all essential configuration elements already exist.
 *
 * <p>Note: This class focuses specifically on global, logging, and service configurations. Catalog objects (like
 * workspaces, stores, layers) already have more robust persistence handling during their creation. This approach is
 * similar to how default styles are initialized when loading the catalog, where consistent IDs are also essential
 * across GeoServer instances and/or restarts.
 */
class MinimalConfigLoaderSupport {

    private ConfigLoader loader;
    private final GeoServer geoServer;
    private final DataDirectoryWalker fileWalk;

    /**
     * Constructs a new InitialConfigLoaderSupport instance.
     *
     * @param loader The ConfigLoader instance that provides access to configuration loading methods and resources
     */
    public MinimalConfigLoaderSupport(ConfigLoader loader) {
        this.loader = loader;
        geoServer = loader.geoServer;
        fileWalk = loader.fileWalk;
    }

    /**
     * Ensures a minimum configuration when GeoServer starts with an empty or incomplete data directory.
     *
     * <p>This method ensures essential configuration components (global, logging, root services) are present. If
     * they're missing, it creates default versions with appropriate settings.
     *
     * <p>The implementation is thread-safe and cluster-aware, using file locks to coordinate when multiple GeoServer
     * instances might be starting concurrently with the same data directory. Cluster-awarness is delegated to
     * {@link GeoServerConfigurationLock} as per {@link DataDirectoryWalker#lock()}.
     */
    public void initializeEmptyConfig() {

        fileWalk.lock();
        try {
            XStreamPersister xp =
                    fileWalk.getXStreamLoader().getPersisterFactory().createXMLPersister();
            RootConfigPersister persister = RootConfigPersister.valueOf(geoServer, fileWalk.getServiceLoaders(), xp);
            if (isGlobalMissing()) {
                addMissingGlobalConfig(persister);
            }
            if (isLoggingMissing()) {
                addMissingLoggingConfig(persister);
            }
            synchronizeRootServices(persister);
        } finally {
            fileWalk.unlock();
        }
    }

    private boolean isLoggingMissing() {
        LoggingInfo logging = geoServer.getLogging();
        return logging == null
                || logging.getLevel() == null
                || fileWalk.gsLogging().isEmpty();
    }

    private boolean isGlobalMissing() {
        GeoServerInfo global = geoServer.getGlobal();
        return global == null || fileWalk.gsGlobal().isEmpty();
    }

    /**
     * Adds global configuration if it's missing.
     *
     * <p>First attempts to load from disk in case another instance has already created it. If not found, creates a
     * default global configuration and persists it.
     *
     * @param persister The configuration persister to use when creating new config
     */
    private void addMissingGlobalConfig(RootConfigPersister persister) {
        Optional<GeoServerInfo> global = loader.loadGlobal();
        if (global.isPresent()) { // someone else created it already?
            geoServer.setGlobal(global.orElseThrow());
        } else {
            geoServer.addListener(persister);
            GeoServerInfo config = geoServer.getFactory().createGlobal();
            geoServer.setGlobal(config);
            geoServer.removeListener(persister);
        }
    }

    /**
     * Adds logging configuration if it's missing.
     *
     * <p>First attempts to load from disk in case another instance has already created it. If not found, creates a
     * default logging configuration with level set to "DEFAULT_LOGGING" and persists it.
     *
     * @param persister The configuration persister to use when creating new config
     */
    private void addMissingLoggingConfig(RootConfigPersister persister) {
        Optional<LoggingInfo> logging = loader.loadLogging();
        if (logging.isPresent()) { // someone else created it already?
            geoServer.setLogging(logging.orElseThrow());
        } else {
            LoggingInfo config = geoServer.getFactory().createLogging();
            config.setLevel("DEFAULT_LOGGING");
            config.setLocation("logs/geoserver.log");
            config.setStdOutLogging(true);

            geoServer.addListener(persister);
            geoServer.setLogging(config);
            geoServer.removeListener(persister);
        }
    }

    /**
     * Synchronizes root services. When starting multiple instances and there are missing service xml files,
     * {@link XStreamServiceLoader} will create a default one, and each instance will have different copies (with
     * different ids) of the same, non-persisted, service.
     */
    private void synchronizeRootServices(RootConfigPersister persister) {

        List<XStreamServiceLoader<ServiceInfo>> loaders = fileWalk.getServiceLoaders();
        // ensure we have a service configuration for every service we know about
        for (XStreamServiceLoader<ServiceInfo> sloader : loaders) {
            synchronizeRootService(sloader, persister);
        }
    }

    /**
     * If the service already exists, makes sure the local copy is the same, otherwise persists the local copy. This
     * method is called within a {@link GeoServerConfigurationLock}.
     */
    private void synchronizeRootService(XStreamServiceLoader<ServiceInfo> sloader, RootConfigPersister persister) {
        final Path root = fileWalk.getRoot();
        final boolean exists = Files.exists(root.resolve(sloader.getFilename()));

        // should never be null in a real world situation, since XStreamServiceLoader creates a new instance, but we'll
        // check for null-ness for the sake of ServicePersisterTest
        final ServiceInfo inMemory = geoServer.getService(sloader.getServiceClass());
        if (null != inMemory) requireNonNull(inMemory.getId());

        if (exists) {
            // someone else created it already, just replace it
            ServiceInfo persisted = loader.loadRootService(sloader);
            // replace with the persisted one
            if (inMemory != null) {
                geoServer.remove(inMemory);
            }
            if (persisted != null) {
                geoServer.remove(persisted);
                geoServer.add(persisted);
            }
        } else {
            // we hold the lock and the file doesn't exist, persist it
            // remove it to add it back with a persisting listener
            if (inMemory != null) {
                geoServer.remove(inMemory);
                geoServer.addListener(persister);
                geoServer.add(inMemory);
                geoServer.removeListener(persister);
            }
        }
    }

    /**
     * Helper class that combines service persistence and configuration persistence into a single listener that can be
     * temporarily attached when making configuration changes.
     */
    private static class RootConfigPersister extends ConfigurationListenerAdapter {

        private ServicePersister servicePeristerListener;
        private GeoServerConfigPersister configPersisterListener;

        /**
         * Factory method to create a RootConfigPersister instance.
         *
         * @param geoServer The GeoServer instance
         * @param serviceLoaders List of service loaders to be used for service persistence
         * @param xp The XStream persister to use for configuration persistence
         * @return A new RootConfigPersister instance
         */
        public static RootConfigPersister valueOf(
                GeoServer geoServer, List<XStreamServiceLoader<ServiceInfo>> serviceLoaders, XStreamPersister xp) {

            RootConfigPersister rp = new RootConfigPersister();
            rp.servicePeristerListener = new ServicePersister(serviceLoaders, geoServer);

            GeoServerResourceLoader resourceLoader = geoServer.getCatalog().getResourceLoader();
            xp.setCatalog(geoServer.getCatalog());
            rp.configPersisterListener = new GeoServerConfigPersister(resourceLoader, xp);
            return rp;
        }

        /**
         * Overrides equals to ensure a listener is only added once. Identity comparison is used since we only care
         * about this exact instance.
         */
        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        /** Unused, added to avoid errorprone failures */
        @Override
        public int hashCode() {
            return Objects.hash(getClass());
        }

        /** Handles changes to global configuration by delegating to the config persister. */
        @Override
        public void handlePostGlobalChange(GeoServerInfo global) {
            configPersisterListener.handlePostGlobalChange(global);
        }

        /** Handles changes to logging configuration by delegating to the config persister. */
        @Override
        public void handlePostLoggingChange(LoggingInfo logging) {
            configPersisterListener.handlePostLoggingChange(logging);
        }

        /** Handles changes to service configuration by delegating to the service persister. */
        @Override
        public void handlePostServiceChange(ServiceInfo service) {
            servicePeristerListener.handlePostServiceChange(service);
        }
    }
}
