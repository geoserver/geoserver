/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir;

import com.google.common.base.Stopwatch;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.datadir.internal.DataDirectoryLoader;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.IncludeFilter;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * The loading process is multi-threaded, and will take place in an {@link Executor} whose
 * parallelism is determined by an heuristic resolving to the minimum between {@code 16} and the
 * number of available processors as reported by {@link Runtime#availableProcessors()}, or
 * overridden by the value passed through the environment variable or system property {@literal
 * DATADIR_LOAD_PARALLELISM}.
 *
 * @since 2.25
 */
public class DataDirectoryGeoServerLoader extends DefaultGeoServerLoader {
    static final String SYSPROP_KEY = "datadir.loader.enabled";
    static final String ENVVAR_KEY = "DATADIR_LOADER_ENABLED";

    static final Logger LOGGER =
            Logging.getLogger(DataDirectoryGeoServerLoader.class.getPackage().getName());

    private DataDirectoryLoader loader;

    private GeoServerSecurityManager securityManager;

    private boolean catalogLoaded;
    private boolean geoserverLoaded;

    /**
     * @param resourceLoader
     * @param securityManager
     * @throws IllegalArgumentException if the resource loader's {@link ResourceStore} is not a
     *     {@link FileSystemResourceStore}
     */
    public DataDirectoryGeoServerLoader(
            GeoServerResourceLoader resourceLoader, GeoServerSecurityManager securityManager) {
        super(resourceLoader);
        this.securityManager = securityManager;
    }

    @Override
    public void destroy() {
        if (loader != null) {
            loader.dispose();
        }
        super.destroy();
    }

    private DataDirectoryLoader loader() {
        if (this.loader == null) {
            this.loader = createLoader();
        }
        return this.loader;
    }

    /**
     * Loads a new {@link CatalogImpl} through {@link DataDirectoryLoader}.
     *
     * @param xp not used by this {@link GeoServerLoader} implementation , the {@link
     *     DataDirectoryLoader} collaborator creates one per loading thread.
     * @implNote note while {@link DefaultGeoServerLoader} decrypts {@link DataStoreInfo} password
     *     connection parameters on the fly as stores are loaded, this implementation does it after
     *     the catalog is completely loaded, to avoid deadlocks on the main thread produced by
     *     loading with multiple threads while {@link GeoServerExtensions} is called and forces
     *     loading beans which only works on the main thread for spring.
     */
    @Override
    protected Catalog readCatalog(XStreamPersister xp) throws Exception {
        Stopwatch startedStopWatch = logStart();

        CatalogImpl catalog = newTemporaryCatalog();
        catalog.setResourceLoader(resourceLoader);
        List.copyOf(catalog.getListeners()).forEach(catalog::removeListener);

        catalog = loader().loadCatalog(catalog);
        logStop(startedStopWatch.stop(), catalog);

        catalogLoaded = true;
        disposeIfBothLoaded();
        catalog.resolve();
        decryptDataStorePasswords(catalog);
        return catalog;
    }

    @Override
    protected void readConfiguration(GeoServer target, XStreamPersister xp) throws Exception {
        if (isLegacyConfig()) {
            super.readConfiguration(target, xp);
            return;
        }
        LOGGER.config("Loading GeoServer config...");
        Stopwatch stopWatch = Stopwatch.createStarted();

        loader().loadGeoServer(target);
        geoserverLoaded = true;
        disposeIfBothLoaded();

        LOGGER.log(
                Level.CONFIG,
                "GeoServer config (settings and services) loaded in {0}",
                stopWatch.stop());

        stopWatch.reset().start();
        LOGGER.log(
                Level.CONFIG,
                "Transferred GeoServer config to actual service bean in {0}",
                stopWatch.stop());
    }

    private void disposeIfBothLoaded() {
        if (catalogLoaded && geoserverLoaded) {
            loader().dispose();
        }
    }

    /** Override to use an alternative {@link CatalogImpl} subclass */
    protected CatalogImpl newTemporaryCatalog() {
        return new CatalogImpl();
    }

    private void decryptDataStorePasswords(CatalogImpl catalog) {
        ConfigurationPasswordEncryptionHelper helper;
        helper = securityManager.getConfigPasswordEncryptionHelper();

        catalog.getDataStores().stream()
                .filter(this::shouldTryDecrypt)
                .map(ModificationProxy::unwrap)
                .forEach(helper::decode);
    }

    /**
     * Determines whether to even attempt to decrypt password connection parameters, returns {@code
     * false} for known DataStore types that won't have such parameters, in order to avoid the
     * overhead from {@link ConfigurationPasswordEncryptionHelper} which would end up loading the
     * {@link DataStoreFactorySpi} and forcing thousands of calls to {@link
     * GeoTools#getInitialContext()}.
     */
    private boolean shouldTryDecrypt(DataStoreInfo ds) {
        return null != ds.getType()
                && !"Shapefile".equals(ds.getType())
                && !"OGR".equals(ds.getType());
    }

    protected DataDirectoryLoader createLoader() {
        FileSystemResourceStore resourceStore = resolveResourceStore(resourceLoader);
        List<XStreamServiceLoader<ServiceInfo>> serviceLoaders = findServiceLoaders();
        XStreamPersisterFactory persisterFactory = super.xpf;
        return new DataDirectoryLoader(resourceStore, serviceLoaders, persisterFactory);
    }

    public static List<XStreamServiceLoader<ServiceInfo>> findServiceLoaders() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<XStreamServiceLoader<ServiceInfo>> loaders =
                (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);
        return loaders;
    }

    private FileSystemResourceStore resolveResourceStore(GeoServerResourceLoader resourceLoader) {
        ResourceStore resourceStore = resourceLoader.getResourceStore();
        if (!(resourceStore instanceof FileSystemResourceStore)) {
            throw new IllegalArgumentException(
                    "Expected ResourceStore to be FileSystemResourceStore, got "
                            + resourceStore.getClass().getName());
        }
        return (FileSystemResourceStore) resourceStore;
    }

    private Stopwatch logStart() {
        LOGGER.log(Level.INFO, "Loading catalog from {0}", resourceLoader.getBaseDirectory());
        return Stopwatch.createStarted();
    }

    private void logStop(Stopwatch stoppedSw, final Catalog catalog) {
        LOGGER.log(Level.INFO, "Read catalog {0}", stoppedSw);
        LOGGER.config(
                () -> {
                    String msg =
                            "Loaded Catalog contents: "
                                    + "workspaces: %,d, "
                                    + "namespaces: %,d, "
                                    + "styles: %,d, "
                                    + "stores: %,d, "
                                    + "resources: %,d, "
                                    + "layers: %,d, "
                                    + "layer groups: %,d.";

                    final IncludeFilter all = Filter.INCLUDE;
                    return String.format(
                            msg,
                            catalog.count(WorkspaceInfo.class, all),
                            catalog.count(NamespaceInfo.class, all),
                            catalog.count(StyleInfo.class, all),
                            catalog.count(StoreInfo.class, all),
                            catalog.count(ResourceInfo.class, all),
                            catalog.count(LayerInfo.class, all),
                            catalog.count(LayerGroupInfo.class, all));
                });
    }

    /**
     * Determines whether the {@link DataDirectoryGeoServerLoader} shall be used, works like a
     * spring-boot's {@code @ConditionalOnProperty(value="datadir.loader.enabled",
     * havingValue="true", matchIfMissing=true)}, in which the both {@code datadir.loader.enabled}
     * and {@code DATADIR_LOADER_ENABLED} can be used as System property, environment variable, or
     * ApplicationContex {@link Environment} property.
     *
     * @return {@code true} by default, false if the enabled config property resolves to anything
     *     but {@code true}
     * @param context if provided, used to fall back resolving the {@code datadir.loader.enabled} or
     *     {@code DATADIR_LOADER_ENABLED} config property if not provided as System property or
     *     environment variable.
     */
    public static boolean isEnabled(@Nullable ApplicationContext context) {
        String value =
                getProperty(context, SYSPROP_KEY)
                        .or(() -> getProperty(context, ENVVAR_KEY))
                        .orElse("true");
        return Boolean.parseBoolean(value);
    }

    private static Optional<String> getProperty(@Nullable ApplicationContext context, String prop) {
        String value = GeoServerExtensions.getProperty(prop);
        if (!StringUtils.hasText(value) && null != context) {
            // GeoServerExtensions.getProperty() doesn't check the Environment property
            // doing it here, with lower priority than env variables and system properties,
            // so it also works with GeoServer Cloud's externalized configuration
            value = context.getEnvironment().getProperty(prop);
        }
        return Optional.ofNullable(value);
    }
}
