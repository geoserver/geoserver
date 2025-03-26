/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import com.google.common.base.Stopwatch;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeCallback;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.RetypeFeatureTypeCallback;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.api.referencing.FactoryException;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Faster alternative to {@link DefaultGeoServerLoader}, especially over network drives like NFS shares.
 *
 * <p>This loader parallelizes both I/O calls and parsing of Catalog and Config info objects, minimizing I/O calls as
 * much as possible, trying to make a single pass over the `workspaces` directory tree, and loading both catalog and
 * config files in one pass.
 *
 * <p>Large Catalogs contain several thousand small XML files that need to be read and parsed, and network shares (NFS
 * in particular) are really bad at serving lots of small files. This implementation addresses this performance
 * bottleneck by:
 *
 * <ul>
 *   <li>Parallelizing file reading and XML parsing operations to the extent possible
 *   <li>Optimizing the directory traversal to make a single pass
 *   <li>Decrypting passwords after catalog load to avoid threading issues and crashing. Failure to decode a password
 *       field will disable the store instead.
 *   <li>Using thread-local XStreamPersisters to avoid contention
 * </ul>
 *
 * <p>This can be disabled (falling back to {@link DefaultGeoServerLoader}), through the
 * {@code GEOSERVER_DATA_DIR_LOADER_ENABLED=false} System Property or environment variable.
 *
 * <p>The loading process is multi-threaded, and will take place in an {@link Executor} whose parallelism is determined
 * by an heuristic resolving to the minimum between {@code 16} and the number of available processors as reported by
 * {@link Runtime#availableProcessors()}.
 *
 * <p>The parallelism level can also be overridden through the environment variable or system property
 * {@literal GEOSERVER_DATA_DIR_LOADER_THREADS}. A value of zero or less will produce a warning and fall back to the
 * default value heuristic mentioned above.
 *
 * @implNote This class shares the loading workflow of {@link GeoServerLoader} and {@link DefaultGeoServerLoader},
 *     tapping into {@link #readCatalog(XStreamPersister)} and {@link #readConfiguration(GeoServer, XStreamPersister)}
 *     to load a catalog, and populate the {@link GeoServer} config, respectively, using the parallelism and single-pass
 *     directory walk provided by the helper classes {@link CatalogLoader} and {@link ConfigLoader}.
 */
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryGeoServerLoader.class.getPackage().getName());

    /** Environment variable or System property to disable this GeoServerLoader as the default one */
    static final String GEOSERVER_DATA_DIR_LOADER_ENABLED = "GEOSERVER_DATA_DIR_LOADER_ENABLED";

    /**
     * Environment variable or System property to bypass the parallelism heuristics and set a fixed number of threads to
     * use
     */
    static final String GEOSERVER_DATA_DIR_LOADER_THREADS = "GEOSERVER_DATA_DIR_LOADER_THREADS";

    private final GeoServerDataDirectory dataDirectory;
    private final GeoServerSecurityManager securityManager;

    /** Lazily created by {@link #fileWalker()} */
    DataDirectoryWalker fileWalk;

    private GeoServerConfigurationLock configLock;

    public DataDirectoryGeoServerLoader(
            GeoServerDataDirectory dataDirectory,
            GeoServerSecurityManager securityManager,
            GeoServerConfigurationLock configLock) {
        super(dataDirectory.getResourceLoader());
        this.dataDirectory = dataDirectory;
        this.securityManager = securityManager;
        this.configLock = configLock;
    }

    /**
     * Utility method to check if usage of this geoserver loader is enabled. Defaults to {@code true}, unless disabled
     * by the {@literal GEOSERVER_DATA_DIR_LOADER_ENABLED} environment variable or System property, as returned by
     * {@link GeoServerExtensions#getProperty(String, ApplicationContext)}
     */
    public static boolean isEnabled(@Nullable ApplicationContext context) {
        String value = GeoServerExtensions.getProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, context);
        return value == null || Boolean.parseBoolean(value);
    }

    @Override
    public void destroy() {
        this.fileWalk = null;
        super.destroy(); // calls geoserver.dispose()
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        initializeDependencies();
        super.loadCatalog(catalog, xp);
    }

    /**
     * Called by {@link GeoServerLoader#readCatalog(Catalog, XStreamPersister)} for reading GeoServer 2.x data
     * directories into a fresh catalog instance
     *
     * <p>This method implements the parallel loading strategy by:
     *
     * <ol>
     *   <li>Creating a temporary catalog instance to load into
     *   <li>Using {@link CatalogLoader} to load the catalog in parallel
     *   <li>Decrypting store passwords on the main thread (avoiding threading issues and startup failures)
     * </ol>
     *
     * @param xp not used by this {@link GeoServerLoader} implementation, as the {@link XStreamLoader} collaborator
     *     creates one per loading thread.
     * @return a new catalog instance populated with the data directory contents from the supplied
     *     {@link #dataDirectory}
     */
    @Override
    protected Catalog readCatalog(XStreamPersister xp) throws Exception {
        CatalogImpl catalog = newTemporaryCatalog();
        xp.setCatalog(catalog);
        xp.setUnwrapNulls(false);

        // see if we really need to verify stores on startup
        boolean checkStores = checkStoresOnStartup(xp);
        catalog.setExtendedValidation(checkStores);

        catalog.setResourceLoader(resourceLoader);

        CatalogLoader catalogLoader = new CatalogLoader(catalog, fileWalker());
        catalogLoader.loadCatalog();

        decryptStorePasswords(catalog);

        return catalog;
    }

    /**
     * Loads the GeoServer configuration (global settings, logging settings, services).
     *
     * <p>Called by {@link DefaultGeoServerLoader#loadGeoServer(GeoServer, XStreamPersister)} after removing persistence
     * listeners
     *
     * <p>This method implements the loading strategy for GeoServer configuration by:
     *
     * <ol>
     *   <li>Unwrapping the raw catalog from any decorators
     *   <li>Using {@link ConfigLoader} to load the GeoServer configuration from the same {@link DataDirectoryWalker}
     *       used by {@link #readCatalog(XStreamPersister)}
     * </ol>
     *
     * @param geoServer the target GeoServer instance to load configuration into
     * @param xp the XStream persister (not directly used by this implementation)
     */
    @Override
    protected void readConfiguration(GeoServer geoServer, XStreamPersister xp) throws Exception {
        LOGGER.config("Loading GeoServer config...");

        if (isLegacyDatadir()) {
            super.readConfiguration(geoServer, xp);
        } else {
            Stopwatch stopWatch = Stopwatch.createStarted();

            ConfigLoader configLoader = new ConfigLoader(geoServer, fileWalker());
            configLoader.loadGeoServer();

            LOGGER.log(Level.CONFIG, "GeoServer config (settings and services) loaded in {0}", stopWatch.stop());
        }
    }

    /** Looks for services.xml, if it exists assume we are dealing with an old data directory */
    private boolean isLegacyDatadir() {
        Resource legacyGlobalConfig = resourceLoader.get("services.xml");
        return Resources.exists(legacyGlobalConfig);
    }

    /**
     * Overrides as a no-op, {@link CatalogLoader} will initialize the default styles right after loading the global
     * styles, so it can safely assign default styles to broken links (e.g. to a {@link LayerInfo} pointing to a
     * dangling style, or a style in a different workspace), hence making the startup process more resilient to common
     * misconfigurations due to manual manipulation of the data directory or misbehaving tooling.
     */
    @Override
    protected void initializeDefaultStyles(Catalog catalog) {
        // no-op, default styles are initialized by CatalogLoader
    }

    /** Give subclasses a chance to provide an alternative catalog implementation */
    protected CatalogImpl newTemporaryCatalog() {
        return new CatalogImpl();
    }

    private void decryptStorePasswords(CatalogImpl catalog) {
        catalog.getStores(HTTPStoreInfo.class).stream()
                .filter(store -> store.getPassword() != null)
                .map(ModificationProxy::unwrap)
                .forEach(this::decodePasswords);

        catalog.getDataStores().stream().map(ModificationProxy::unwrap).forEach(this::decodePassword);
    }

    private void decodePasswords(HTTPStoreInfo store) {
        ConfigurationPasswordEncryptionHelper helper = passwordHelper();
        try {
            String decoded = helper.decode(store.getPassword());
            store.setPassword(decoded);
        } catch (RuntimeException e) {
            store.setEnabled(false);
            String msg = String.format(
                    "Error decrypting password for store %s:%s",
                    store.getWorkspace().getName(), store.getName());
            LOGGER.log(Level.SEVERE, msg, e);
        }
    }

    private void decodePassword(DataStoreInfo store) {
        ConfigurationPasswordEncryptionHelper helper = passwordHelper();
        try {
            helper.decode(store);
        } catch (RuntimeException e) {
            store.setEnabled(false);
            String msg = String.format(
                    "Error decrypting password for store '%s:%s'. Store disabled.",
                    store.getWorkspace().getName(), store.getName());
            LOGGER.log(Level.SEVERE, msg, e);
        }
    }

    private ConfigurationPasswordEncryptionHelper passwordHelper() {
        return securityManager.getConfigPasswordEncryptionHelper();
    }

    private DataDirectoryWalker fileWalker() {
        if (fileWalk == null) {
            fileWalk = new DataDirectoryWalker(dataDirectory, xpf, configLock);
        }
        return fileWalk;
    }

    /** Warm up the extensions cache with all the extensions used during the loading process to avoid race conditions */
    private void initializeDependencies() {
        try {
            // preemptively initialize the CRS subsystem to avoid factory lookups deadlocking while being hit
            // concurrently at #readCatalog
            org.geotools.referencing.CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            LOGGER.log(Level.WARNING, "Error initializing CRS factories", e);
        }

        // warm up GeoServerExtensions with extensions probably called during catalog loading
        preLoadExtensions(EntityResolverProvider.class);
        // CatalogImpl
        preLoadExtensions(CatalogValidator.class);
        // Styles.handlers()
        preLoadExtensions(StyleHandler.class);
        // ResourcePool
        preLoadExtensions(FeatureTypeCallback.class);
        preLoadExtensions(RetypeFeatureTypeCallback.class);
        // XStreamPersisterFactory
        preLoadExtensions(XStreamPersisterInitializer.class);
        preLoadExtensions(XStreamServiceLoader.class);

        // misconfigured layers may end up calling FeatureTypeInfo.getFeatureType(), which in turn
        // will trigger GeoServerExtensions and deadlock on the main thread's while spring is
        // building up beans
        DataStoreUtils.getAvailableDataStoreFactories().forEach(f -> {
            try {
                DataStoreUtils.aquireFactory(f.getDisplayName());
            } catch (Exception ignore) {
                //
            }
        });
    }

    private void preLoadExtensions(Class<?> extensionType) {
        try {
            GeoServerExtensions.extensions(extensionType);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error preloading " + extensionType.getCanonicalName(), e);
        }
    }
}
