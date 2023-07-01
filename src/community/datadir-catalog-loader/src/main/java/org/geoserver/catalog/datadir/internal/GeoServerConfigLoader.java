/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir.internal;

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
import org.geoserver.catalog.datadir.internal.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

class GeoServerConfigLoader {

    private static final Logger LOGGER =
            Logging.getLogger(GeoServerConfigLoader.class.getPackage().getName());

    private final AtomicLong readFileCount = new AtomicLong();

    private final DataDirectoryWalker fileWalk;
    private final XStreamLoader xstreamLoader;
    private final ExecutorService executor;
    private final GeoServerImpl geoServer;

    private FileSystemResourceStore resourceStore;
    private List<XStreamServiceLoader<ServiceInfo>> serviceLoaders;

    public GeoServerConfigLoader(
            DataDirectoryWalker fileWalk,
            XStreamLoader xstreamLoader,
            ExecutorService executor,
            GeoServerImpl gs,
            FileSystemResourceStore resourceStore,
            List<XStreamServiceLoader<ServiceInfo>> serviceLoaders) {

        requireNonNull(fileWalk);
        requireNonNull(xstreamLoader);
        requireNonNull(executor);
        requireNonNull(gs);
        requireNonNull(resourceStore);
        requireNonNull(serviceLoaders);

        this.fileWalk = fileWalk;
        this.geoServer = gs;
        this.resourceStore = resourceStore;
        this.xstreamLoader = xstreamLoader;
        this.executor = executor;
        this.serviceLoaders = serviceLoaders;
    }

    public GeoServerImpl loadGeoServer() throws Exception {
        Future<GeoServerImpl> task = executor.submit(this::readGeoServer);
        try {
            return task.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw ((Error) cause);
            throw e;
        }
    }

    private GeoServerImpl readGeoServer() {
        readFileCount.set(0);

        Optional<GeoServerInfo> global = fileWalk.gsGlobal().flatMap(this::depersist);
        global.ifPresent(geoServer::setGlobal);

        Optional<LoggingInfo> logging = fileWalk.gsLogging().flatMap(this::depersist);
        logging.ifPresent(geoServer::setLogging);

        loadSettings();
        loadServices();

        config("Depersisted {0} Config files", readFileCount.get());
        return geoServer;
    }

    private void loadServices() {
        loadRootServices();

        LOGGER.config("Loading workspace services...");

        Long count =
                fileWalk.workspaces().stream()
                        .parallel()
                        .map(this::loadWorkspaceServices)
                        .reduce((c1, c2) -> c1 + c2)
                        .orElse(0L);
        config("Loaded {0} workspace-specific services.", count);
    }

    private void loadRootServices() {
        Resource baseDirectory = resourceStore.get("");
        for (XStreamServiceLoader<ServiceInfo> loader : serviceLoaders) {
            loadService(loader, baseDirectory).ifPresent(this::save);
        }
    }

    private long loadWorkspaceServices(WorkspaceDirectory ws) {
        final Set<String> serviceFiles = ws.serviceInfoFileNames;
        if (serviceFiles.isEmpty()) return 0L;

        final String wsName = ws.workspaceFile.getParent().getFileName().toString();
        final Resource wsdir = resourceStore.get("workspaces").get(wsName);

        return serviceLoaders.stream()
                // filter loaders on available service files for the workspace
                .filter(loader -> serviceFiles.contains(loader.getFilename()))
                .map(loader -> loadService(loader, wsdir))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(s -> warnIfWorkspaceIsNull(s, wsName))
                .filter(service -> null != service.getWorkspace())
                .map(this::save)
                .count();
    }

    private void warnIfWorkspaceIsNull(ServiceInfo service, String workspaceDirectoryName) {
        if (null == service.getWorkspace()) {
            warn(
                    "Service {0} on workspace directory '{1}' has no workspace attached, service not loaded",
                    service.getName(), workspaceDirectoryName);
        }
    }

    Optional<ServiceInfo> loadService(
            XStreamServiceLoader<ServiceInfo> serviceLoader, Resource directory) {
        ServiceInfo s = null;
        try {
            s = serviceLoader.load(geoServer, directory);
            this.readFileCount.incrementAndGet();
        } catch (Throwable t) {
            if (Resources.exists(directory)) {
                severe(
                        "Failed to load the service configuration in directory: {0} with loader for {1}",
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
        log(Level.SEVERE, fmt, args);
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
        long count =
                fileWalk.workspaces().stream()
                        .parallel()
                        .map(wd -> wd.settings)
                        .map(this::depersist)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(SettingsInfo.class::cast)
                        .map(this::save)
                        .filter(Optional::isPresent)
                        .count();
        config("Loaded {0} workspace-specific settings.", count);
    }

    private Optional<SettingsInfo> save(SettingsInfo settings) {
        return save(settings, geoServer::add);
    }

    private Optional<ServiceInfo> save(ServiceInfo service) {
        return save(service, geoServer::add);
    }

    private <I extends Info> Optional<I> save(I info, Consumer<I> saver) {
        try {
            saver.accept(info);
            return Optional.of(info);
        } catch (Exception e) {
            final String name = (String) OwsUtils.get(info, "id");
            String err = e.getMessage();
            LOGGER.log(
                    Level.WARNING,
                    "Error saving {0} {1}: {2}",
                    new Object[] {info.getClass().getSimpleName(), name, err});
        }
        return Optional.empty();
    }

    private <C extends Info> Optional<C> depersist(Optional<Path> file) {
        return file.flatMap(this::depersist);
    }

    private <C extends Info> Optional<C> depersist(Path file) {
        Catalog catalog = geoServer.getCatalog();
        Optional<C> info = xstreamLoader.depersist(file, catalog);
        if (info.isPresent()) readFileCount.incrementAndGet();
        return info;
    }
}
