/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir.internal;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.datadir.internal.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.decorate.AbstractDecorator;
import org.geotools.util.logging.Logging;

/**
 * Specialized loader for GeoServer configuration objects that supports parallel loading.
 *
 * <p>This class is responsible for loading the GeoServer configuration structure, including:
 *
 * <ul>
 *   <li>Global configuration (global.xml)
 *   <li>Logging configuration (logging.xml)
 *   <li>Service configurations (wms.xml, wfs.xml, etc.) both global and workspace-specific
 *   <li>Workspace-specific settings (settings.xml in workspace directories)
 * </ul>
 *
 * <p>The loading process uses parallel streams for workspace-specific settings and services to maximize throughput. The
 * loader maintains proper relationships between loaded objects and the existing catalog.
 *
 * <p>This class works closely with the service loaders defined in the GeoServer extension system to load and configure
 * service objects appropriately.
 */
class ConfigLoader {

    private static final Logger LOGGER =
            Logging.getLogger(ConfigLoader.class.getPackage().getName());

    /** Counter for the number of files successfully read and parsed */
    private final AtomicLong readFileCount = new AtomicLong();

    private final DataDirectoryWalker fileWalk;
    private final XStreamLoader xstreamLoader;
    private final ExecutorService executor;
    private final GeoServer geoServer;

    /** Data directory for resolving resource paths */
    private GeoServerDataDirectory dataDirectory;

    /** Service loaders for all registered service types */
    private List<XStreamServiceLoader<ServiceInfo>> serviceLoaders;

    public ConfigLoader(
            DataDirectoryWalker fileWalk,
            XStreamLoader xstreamLoader,
            ExecutorService executor,
            GeoServer gs,
            GeoServerDataDirectory dataDirectory,
            List<XStreamServiceLoader<ServiceInfo>> serviceLoaders) {

        requireNonNull(fileWalk);
        requireNonNull(xstreamLoader);
        requireNonNull(executor);
        requireNonNull(gs);
        requireNonNull(dataDirectory);
        requireNonNull(serviceLoaders);

        this.fileWalk = fileWalk;
        this.geoServer = gs;
        this.dataDirectory = dataDirectory;
        this.xstreamLoader = xstreamLoader;
        this.executor = executor;
        this.serviceLoaders = serviceLoaders;
    }

    public void loadGeoServer() throws InterruptedException, ExecutionException {
        Future<?> task = executor.submit(this::readGeoServer);
        task.get();
    }

    private void readGeoServer() {
        readFileCount.set(0);

        // temporarily set the raw catalog to avoid decorators forcing spring to resolve beans and deadlock on the main
        // thread
        final Catalog realCatalog = geoServer.getCatalog();
        geoServer.setCatalog(rawCatalog(realCatalog));
        try {
            Optional<GeoServerInfo> global = fileWalk.gsGlobal().flatMap(this::depersist);
            global.ifPresent(geoServer::setGlobal);

            Optional<LoggingInfo> logging = fileWalk.gsLogging().flatMap(this::depersist);
            logging.ifPresent(geoServer::setLogging);

            loadSettings();
            loadServices();

            config("Depersisted {0} Config files", readFileCount.get());
        } finally {
            geoServer.setCatalog(realCatalog);
        }
    }

    private void loadServices() {
        loadRootServices();

        LOGGER.config("Loading workspace services...");

        Long count = fileWalk.workspaces().stream()
                .parallel()
                .map(this::loadWorkspaceServices)
                .reduce((c1, c2) -> c1 + c2)
                .orElse(0L);
        config("Loaded {0} workspace-specific services.", count);
    }

    private void loadRootServices() {
        Resource baseDirectory = dataDirectory.getRoot();
        for (XStreamServiceLoader<ServiceInfo> loader : serviceLoaders) {
            loadService(loader, baseDirectory).ifPresent(this::add);
        }
    }

    private long loadWorkspaceServices(WorkspaceDirectory ws) {
        final Set<String> serviceFiles = ws.serviceInfoFileNames();
        if (serviceFiles.isEmpty()) return 0L;

        final String wsName = ws.directory().getFileName().toString();
        final Resource wsdir = dataDirectory.get("workspaces").get(wsName);

        return serviceLoaders.stream()
                // filter loaders on available service files for the workspace
                .filter(loader -> serviceFiles.contains(loader.getFilename()))
                .map(loader -> loadService(loader, wsdir))
                .map(sOpt -> filterNullWorkspace(sOpt, ws))
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(this::add)
                .count();
    }

    private Optional<ServiceInfo> filterNullWorkspace(Optional<ServiceInfo> service, WorkspaceDirectory wd) {
        return service.filter(s -> {
            boolean hasWorkspace = s.getWorkspace() != null;
            if (!hasWorkspace) {
                warn(
                        "Service {0} on workspace directory {1} has no workspace attached, service not loaded. Check the workspace id.",
                        s.getName(), wd.directory().getFileName());
            }
            return hasWorkspace;
        });
    }

    /**
     * Used to load both root services and workspace services
     * @param serviceLoader the service loader to use
     * @param directory the directory where {@link XStreamServiceLoader#load(org.geoserver.config.GeoServer, Resource) loads) the service from
     * @return an optional with the service loaded, or empty if there was an error
     */
    Optional<ServiceInfo> loadService(XStreamServiceLoader<ServiceInfo> serviceLoader, Resource directory) {
        ServiceInfo s = null;
        try {
            s = serviceLoader.load(geoServer, directory);
            this.readFileCount.incrementAndGet();
        } catch (Exception t) {
            if (Resources.exists(directory)) {
                severe(
                        "Failed to load the service configuration in directory: {0} with XStreamServiceLoader for {1}",
                        directory, serviceLoader.getServiceClass());
            } else {
                severe(
                        "Failed to load the root service configuration with loader for {0}",
                        serviceLoader.getServiceClass(), t);
            }
        }
        return Optional.ofNullable(s);
    }

    private void config(String fmt, Object... args) {
        log(Level.CONFIG, fmt, args);
    }

    private void warn(String fmt, Object... args) {
        log(Level.WARNING, fmt, args);
    }

    private void severe(String fmt, Object... args) {
        log(Level.SEVERE, fmt, args);
    }

    private void log(Level level, String fmt, Object... args) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, fmt, args);
        }
    }

    private void loadSettings() {
        LOGGER.config("Loading workspace settings...");

        long count = fileWalk.workspaces().stream()
                .parallel()
                .map(this::loadSettings)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .map(this::add)
                .filter(Optional::isPresent)
                .count();
        config("Loaded {0} workspace-specific settings.", count);
    }

    private Optional<SettingsInfo> loadSettings(WorkspaceDirectory wd) {
        return wd.settingsFile()
                .flatMap(this::depersist)
                .filter(SettingsInfo.class::isInstance)
                .map(SettingsInfo.class::cast)
                .filter(settings -> fixWorkspace(settings, wd));
    }

    /**
     * {@code settings.xml} stores the full {@code <workspace>}, not a reference for a {@code ResolvingProxy}. This
     * method assigns the correct reference from the catalog and returns {@code true}, or returns {@code false} if the
     * workspace does not match the one from {@code workspaceDirectory}
     *
     * @param settings the SettingsInfo to assign the correct workspace reference from the Catalog to
     * @param workspaceDirectory the workspace directory where settings was depersisted from
     */
    private boolean fixWorkspace(SettingsInfo settings, WorkspaceDirectory workspaceDirectory) {
        WorkspaceInfo workspace = settings.getWorkspace();
        if (workspace == null) {
            severe(
                    "settings.xml on workspace directory {0} has no workspace attached," + " . Settings ignored.",
                    workspaceDirectory.directory().getFileName());
            return false;
        }

        if (null == workspace.getId()) {
            severe(
                    "settings.xml on workspace directory {0} has a no workspace id. Settings ignored.",
                    workspaceDirectory.directory().getFileName(), workspace.getName(), workspace.getId());
            return false;
        }

        WorkspaceInfo actualWorkspace = geoServer.getCatalog().getWorkspace(workspace.getId());
        if (actualWorkspace == null) {
            severe(
                    "settings.xml on workspace directory {0} points to non existing workspace {1}, id {2}. Settings ignored.",
                    workspaceDirectory.directory().getFileName(), workspace.getName(), workspace.getId());
            return false;
        }
        Path settingsDir = dataDirectory.config(settings).file().toPath().getParent();
        Path expectedDir = workspaceDirectory.directory();
        if (!expectedDir.equals(settingsDir)) {
            String msg =
                    "settings.xml in workspace directory {0} points to a different workspace: {1}. Settings ignored.";
            severe(msg, workspaceDirectory.directory().getFileName(), actualWorkspace.getName());
            return false;
        }
        return true;
    }

    private Optional<SettingsInfo> add(SettingsInfo settings) {
        return add(settings, geoServer::add);
    }

    private Optional<ServiceInfo> add(ServiceInfo service) {
        return add(service, geoServer::add);
    }

    private <I extends Info> Optional<I> add(I info, Consumer<I> saver) {
        try {
            saver.accept(info);
            return Optional.of(info);
        } catch (Exception e) {
            final String name = (String) OwsUtils.get(info, "id");
            String err = e.getMessage();
            LOGGER.log(Level.WARNING, "Error saving {0} {1}: {2}", new Object[] {
                info.getClass().getSimpleName(), name, err
            });
        }
        return Optional.empty();
    }

    private <C extends Info> Optional<C> depersist(Path file) {
        Catalog catalog = geoServer.getCatalog();
        Optional<C> info = xstreamLoader.depersist(file, catalog);
        if (info.isPresent()) readFileCount.incrementAndGet();
        return info;
    }

    /** We don't want catalog decorators calling in {@link GeoServerExtensions} and trigger spring resolving beans */
    private Catalog rawCatalog(Catalog catalog) {
        Catalog rawCatalog = catalog;
        while (rawCatalog instanceof AbstractDecorator) {
            rawCatalog = ((AbstractDecorator<?>) rawCatalog).unwrap(Catalog.class);
        }
        return rawCatalog;
    }
}
