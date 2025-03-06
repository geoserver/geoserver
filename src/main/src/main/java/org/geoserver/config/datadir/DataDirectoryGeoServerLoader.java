/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import com.google.common.base.Stopwatch;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.datadir.internal.DataDirectoryLoader;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.filter.Filter;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

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
 *   <li>Parallelizing file reading and XML parsing operations
 *   <li>Optimizing the directory traversal to make a single pass
 *   <li>Decrypting passwords after catalog load to avoid threading issues
 *   <li>Using thread-local XStreamPersisters to avoid contention
 * </ul>
 *
 * <p>This is the default data directory loader since GeoServer 2.27, and can be disabled (falling back to
 * {@link DefaultGeoServerLoader}), through the {@code GEOSERVER_DATA_DIR_LOADER_ENABLED=false} System Property or
 * environment variable.
 *
 * <p>The loading process is multi-threaded, and will take place in an {@link Executor} whose parallelism is determined
 * by an heuristic resolving to the minimum between {@code 16} and the number of available processors as reported by
 * {@link Runtime#availableProcessors()}.
 *
 * <p>The parallelism level can also be overridden through the environment variable or system property
 * {@literal GEOSERVER_DATA_DIR_LOADER_THREADS}. A value of zero or less will produce a warning and fall back to the
 * default value heuristic mentioned above.
 *
 * @implNote This class shares the loading workflow of default {@link GeoServerLoader} and
 *     {@link DefaultGeoServerLoader}, tapping into {@link #readCatalog(XStreamPersister)} and
 *     {@link #readConfiguration(GeoServer, XStreamPersister)} to load a catalog, and populate the {@link GeoServer}
 *     config, respectively, using the parallelism and single-pass directory walk provided by the helper class
 *     {@link DataDirectoryLoader}.
 */
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryGeoServerLoader.class.getPackage().getName());

    /** Environment variable or System property to disable this GeoServerLoader as the default one */
    public static final String GEOSERVER_DATA_DIR_LOADER_ENABLED = "GEOSERVER_DATA_DIR_LOADER_ENABLED";

    /**
     * Environment variable or System property to bypass the parallelism heuristics and set a fixed number of threads to
     * use
     */
    public static final String GEOSERVER_DATA_DIR_LOADER_THREADS = "GEOSERVER_DATA_DIR_LOADER_THREADS";

    private final GeoServerDataDirectory dataDirectory;
    private GeoServerSecurityManager securityManager;

    /** Lazily created by {@link #loader()} */
    DataDirectoryLoader loader;

    /**
     * @param dataDirectory
     * @param securityManager
     * @param xpf
     * @throws IllegalArgumentException if the resource loader's {@link ResourceStore} is not a
     *     {@link FileSystemResourceStore}
     */
    public DataDirectoryGeoServerLoader(
            GeoServerDataDirectory dataDirectory,
            GeoServerSecurityManager securityManager,
            XStreamPersisterFactory xpf) {
        super(dataDirectory.getResourceLoader());
        this.dataDirectory = dataDirectory;
        this.securityManager = securityManager;
        setXStreamPeristerFactory(xpf);
    }

    /**
     * Utility method to check if usage of this geoserver loader is enabled. Defaults to {@code true}, unless disabled
     * by the {@literal GEOSERVER_DATA_DIR_LOADER_ENABLED} environment variable or System property, as returned by
     * {@link GeoServerExtensions#getProperty(String, ApplicationContext)}
     */
    public static boolean isEnabled(@Nullable ApplicationContext context) {
        String value = GeoServerExtensions.getProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, context);
        if (null == value) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public void destroy() {
        try {
            super.destroy(); // calls geoserver.dispose()
        } finally {
            DataDirectoryLoader l = this.loader;
            this.loader = null;
            if (l != null) {
                l.dispose();
            }
        }
    }

    /**
     * Called by {@link GeoServerLoader#readCatalog(Catalog, XStreamPersister)} for reading GeoServer 2.x data
     * directories into a fresh catalog instance
     *
     * <p>This method implements the parallel loading strategy by:
     *
     * <ol>
     *   <li>Creating a temporary catalog instance to load into
     *   <li>Using {@link DataDirectoryLoader} to load the catalog in parallel
     *   <li>Resolving all catalog references
     *   <li>Decrypting store passwords on the main thread (avoiding threading issues)
     *   <li>Transferring all loaded objects to the target catalog
     * </ol>
     *
     * <p>Called by {@link GeoServerLoader#postProcessBeforeInitialization(Object, String)}
     *
     * @param xp not used by this {@link GeoServerLoader} implementation, as the {@link DataDirectoryLoader}
     *     collaborator creates one per loading thread.
     * @implNote While {@link DefaultGeoServerLoader} decrypts {@link DataStoreInfo} password connection parameters on
     *     the fly as stores are loaded, this implementation decrypts passwords after the catalog is completely loaded,
     *     to avoid deadlocks on the main thread produced by loading with multiple threads. This is necessary because
     *     {@link GeoServerExtensions} is called during decryption and forces loading Spring beans which only works
     *     correctly on the main thread.
     * @return a new catalog instance populated with the data directory contents from the supplied
     *     {@link #dataDirectory}
     */
    @Override
    protected Catalog readCatalog(XStreamPersister xp) throws Exception {
        Stopwatch startedStopWatch = logStart();
        CatalogImpl catalog = newTemporaryCatalog();
        catalog.setResourceLoader(resourceLoader);

        loader().loadCatalog(catalog);
        catalog.resolve();

        logStop(startedStopWatch.stop(), catalog);

        decryptStorePasswords(catalog);

        catalog.resolve();

        return catalog;
    }

    /**
     * Loads the GeoServer configuration (global settings, logging settings, services) using parallel processing.
     *
     * <p>Called by {@link DefaultGeoServerLoader#loadGeoServer(GeoServer, XStreamPersister)} after removing persistence
     * listeners
     *
     * <p>This method implements the parallel loading strategy for GeoServer configuration by:
     *
     * <ol>
     *   <li>Unwrapping the raw catalog from any decorators
     *   <li>Using {@link DataDirectoryLoader} to load the GeoServer configuration in parallel
     *   <li>Transferring all loaded configuration to the target GeoServer instance
     *   <li>Notifying any registered loader listeners
     * </ol>
     *
     * @param geoServer the target GeoServer instance to load configuration into
     * @param xp the XStream persister (not directly used by this implementation)
     */
    @Override
    protected void readConfiguration(GeoServer geoServer, XStreamPersister xp) throws Exception {
        LOGGER.config("Loading GeoServer config...");
        Stopwatch stopWatch = Stopwatch.createStarted();

        loader().loadGeoServer(geoServer);

        LOGGER.log(Level.CONFIG, "GeoServer config (settings and services) loaded in {0}", stopWatch.stop());
    }

    protected CatalogImpl newTemporaryCatalog() {
        return new CatalogImpl();
    }

    private DataDirectoryLoader loader() {
        if (loader == null) {
            loader = createLoader();
        }
        return loader;
    }

    private void decryptStorePasswords(CatalogImpl catalog) {
        catalog.getStores(HTTPStoreInfo.class).stream()
                .filter(store -> store.getPassword() != null)
                .map(ModificationProxy::unwrap)
                .forEach(this::decodePasswords);

        catalog.getDataStores().stream()
                .filter(this::shouldTryDecrypt)
                .map(ModificationProxy::unwrap)
                .forEach(this::decodePassword);
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

    /**
     * Determines whether to even attempt to decrypt password connection parameters, returns {@code false} for known
     * DataStore types that won't have such parameters, in order to avoid the overhead from
     * {@link ConfigurationPasswordEncryptionHelper} which would end up loading the {@link DataStoreFactorySpi} and
     * forcing thousands of calls to {@link GeoTools#getInitialContext()}.
     */
    private boolean shouldTryDecrypt(DataStoreInfo ds) {
        return null != ds.getType() && !"Shapefile".equals(ds.getType()) && !"OGR".equals(ds.getType());
    }

    protected DataDirectoryLoader createLoader() {
        List<XStreamServiceLoader<ServiceInfo>> serviceLoaders = findServiceLoaders();
        XStreamPersisterFactory xpfac = Objects.requireNonNull(super.xpf);
        return new DataDirectoryLoader(dataDirectory, serviceLoaders, xpfac);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static List<XStreamServiceLoader<ServiceInfo>> findServiceLoaders() {

        return (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);
    }

    private Stopwatch logStart() {
        LOGGER.log(Level.INFO, "Loading catalog from {0}", resourceLoader.getBaseDirectory());
        return Stopwatch.createStarted();
    }

    private void logStop(Stopwatch stoppedSw, final Catalog catalog) {
        final Filter all = Predicates.acceptAll();
        String msg = String.format(
                "Loaded Catalog in %s: workspaces: %,d, namespaces: %,d, styles: %,d, stores: %,d, resources: %,d, layers: %,d, layer groups: %,d.",
                stoppedSw,
                catalog.count(WorkspaceInfo.class, all),
                catalog.count(NamespaceInfo.class, all),
                catalog.count(StyleInfo.class, all),
                catalog.count(StoreInfo.class, all),
                catalog.count(ResourceInfo.class, all),
                catalog.count(LayerInfo.class, all),
                catalog.count(LayerGroupInfo.class, all));
        LOGGER.config(msg);
    }
}
