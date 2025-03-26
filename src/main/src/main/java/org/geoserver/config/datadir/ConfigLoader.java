/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.datadir.DataDirectoryWalker.WorkspaceDirectory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.decorate.AbstractDecorator;
import org.geotools.util.logging.Logging;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Specialized loader for GeoServer configuration objects.
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
 * <p>This class works closely with the service loaders defined in the GeoServer extension system to load and configure
 * service objects appropriately.
 */
class ConfigLoader {

    private static final Logger LOGGER =
            Logging.getLogger(ConfigLoader.class.getPackage().getName());

    /**
     * Counter for the number of files successfully depersisted (through {@link #depersist(Path)} or
     * {@link #loadService(XStreamServiceLoader, Resource)}
     */
    private final AtomicLong readFileCount = new AtomicLong();

    /** Successfully added workspace-specific {@link ServiceInfo}s */
    private final AtomicLong services = new AtomicLong();
    /** Successfully added workspace-specific {@link SettingsInfo}s */
    private final AtomicLong workspaceSettings = new AtomicLong();

    final GeoServer geoServer;
    final DataDirectoryWalker fileWalk;

    public ConfigLoader(GeoServer geoServer, DataDirectoryWalker fileWalk) {

        this.geoServer = geoServer;
        this.fileWalk = fileWalk;
    }

    /**
     * Loads the GeoServer configuration from the data directory into the supplied {@link GeoServer} instance, using a
     * shared {@link DataDirectoryWalker} with {@link CatalogLoader}
     */
    public void loadGeoServer() {
        readFileCount.set(0);
        services.set(0);
        workspaceSettings.set(0);

        // temporarily set the raw catalog to avoid decorators forcing spring to resolve
        // beans and deadlock on the main thread
        final Catalog realCatalog = geoServer.getCatalog();
        Catalog rawCatalog = rawCatalog(realCatalog);
        geoServer.setCatalog(rawCatalog);

        Optional<GeoServerInfo> global = loadGlobal();
        global.ifPresent(geoServer::setGlobal);

        Optional<LoggingInfo> logging = loadLogging();
        logging.ifPresent(geoServer::setLogging);

        loadRootServices().forEach(this::addService);

        // admin auth set by GeoServerLoader and propagated to the ForkJoinPool threads
        Authentication admin = SecurityContextHolder.getContext().getAuthentication();
        ForkJoinPool executor = ExecutorFactory.createExecutor(admin);
        try {
            executor.submit(this::loadWorkspaceServicesAndSettings).get();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Thread interrupted while loading the catalog", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            geoServer.setCatalog(realCatalog);
            executor.shutdownNow();
        }
        MinimalConfigLoaderSupport minimalConfigSupport = new MinimalConfigLoaderSupport(this);
        minimalConfigSupport.initializeEmptyConfig();
    }

    Optional<GeoServerInfo> loadGlobal() {
        return fileWalk.gsGlobal().flatMap(this::depersist);
    }

    Optional<LoggingInfo> loadLogging() {
        return fileWalk.gsLogging().flatMap(this::depersist);
    }

    private List<ServiceInfo> loadRootServices() {
        return fileWalk.getServiceLoaders().stream()
                .map(this::loadRootService)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Load all workspace services and settings files doing a single (though parallel) path traversal of the workspaces/
     * directory.
     */
    private void loadWorkspaceServicesAndSettings() {
        LOGGER.config("Loading workspace services and settings...");

        fileWalk.workspaces().stream().parallel().forEach(this::loadWorkspaceConfig);
    }

    private void loadWorkspaceConfig(WorkspaceDirectory wd) {
        loadSettings(wd);
        loadServices(wd);
    }

    private void loadSettings(WorkspaceDirectory wd) {
        wd.settingsFile()
                .flatMap(this::depersist)
                .filter(SettingsInfo.class::isInstance)
                .map(SettingsInfo.class::cast)
                .filter(settings -> fixWorkspace(settings, wd))
                .ifPresent(this::addSettings);
    }

    private void loadServices(WorkspaceDirectory wd) {
        final Set<String> serviceFiles = wd.serviceInfoFileNames();
        if (serviceFiles.isEmpty()) {
            return;
        }

        Path fileName = wd.directory().getFileName();
        if (null == fileName) return;

        final String wsName = fileName.toString();
        final Resource wsdir = fileWalk.getDataDirectory().get("workspaces").get(wsName);

        fileWalk.getServiceLoaders().stream()
                // filter loaders on available service files for the workspace
                .filter(loader -> serviceFiles.contains(loader.getFilename()))
                .map(loader -> loadService(loader, wsdir))
                .filter(Objects::nonNull)
                .filter(service -> filterNullWorkspace(service, wd))
                .forEach(this::addService);
    }

    private boolean filterNullWorkspace(ServiceInfo service, WorkspaceDirectory wd) {
        boolean hasWorkspace = service.getWorkspace() != null;
        if (!hasWorkspace) {
            log(
                    Level.SEVERE,
                    "Service {0} on workspace directory {1} has no workspace attached, service not loaded. Check the workspace id.",
                    service.getName(),
                    wd.directory().getFileName());
        }
        return hasWorkspace;
    }

    @Nullable
    ServiceInfo loadRootService(XStreamServiceLoader<ServiceInfo> loader) {
        Resource baseDirectory = fileWalk.getDataDirectory().getRoot();
        return loadService(loader, baseDirectory);
    }

    /**
     * Used to load both root services and workspace services
     *
     * @param serviceLoader the service loader to use
     * @param directory     the directory where
     *                      {@link XStreamServiceLoader#load(org.geoserver.config.GeoServer, Resource)
     * loads) the service from
     * @return the service loaded, or {@code null} if there was an error
     */
    @Nullable
    ServiceInfo loadService(XStreamServiceLoader<ServiceInfo> serviceLoader, Resource directory) {
        ServiceInfo s = null;
        try {
            s = serviceLoader.load(geoServer, directory);
            this.readFileCount.incrementAndGet();
        } catch (Exception t) {
            if (Resources.exists(directory)) {
                log(
                        Level.SEVERE,
                        "Failed to load the service configuration in directory: {0} with XStreamServiceLoader for {1}",
                        directory,
                        serviceLoader.getServiceClass());
            } else {
                log(
                        Level.SEVERE,
                        "Failed to load the root service configuration with loader for {0}",
                        serviceLoader.getServiceClass(),
                        t);
            }
        }
        return s;
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
            log(
                    Level.SEVERE,
                    "settings.xml on workspace directory {0} has no workspace attached," + " . Settings ignored.",
                    workspaceDirectory.directory().getFileName());
            return false;
        }

        if (null == workspace.getId()) {
            log(
                    Level.SEVERE,
                    "settings.xml on workspace directory {0} has a no workspace id. Settings ignored.",
                    workspaceDirectory.directory().getFileName(),
                    workspace.getName(),
                    workspace.getId());
            return false;
        }

        WorkspaceInfo actualWorkspace = geoServer.getCatalog().getWorkspace(workspace.getId());
        if (actualWorkspace == null) {
            log(
                    Level.SEVERE,
                    "settings.xml on workspace directory {0} points to non existing workspace {1}, id {2}. Settings ignored.",
                    workspaceDirectory.directory().getFileName(),
                    workspace.getName(),
                    workspace.getId());
            return false;
        }
        Path settingsDir =
                fileWalk.getDataDirectory().config(settings).file().toPath().getParent();
        Path expectedDir = workspaceDirectory.directory();
        if (!expectedDir.equals(settingsDir)) {
            String msg =
                    "settings.xml in workspace directory {0} points to a different workspace: {1}. Settings ignored.";
            log(Level.SEVERE, msg, workspaceDirectory.directory().getFileName(), actualWorkspace.getName());
            return false;
        }
        return true;
    }

    private void addSettings(SettingsInfo settings) {
        add(settings, geoServer::add).ifPresent(s -> this.workspaceSettings.incrementAndGet());
    }

    private void addService(ServiceInfo service) {
        add(service, geoServer::add).ifPresent(s -> {
            this.services.incrementAndGet();
            LOGGER.config(() -> "Loaded service '" + s.getId() + "', " + (s.isEnabled() ? "enabled" : "disabled"));
        });
    }

    private <I extends Info> Optional<I> add(I info, Consumer<I> saver) {
        try {
            saver.accept(info);
            return Optional.of(info);
        } catch (Exception e) {
            final String name = (String) OwsUtils.get(info, "id");
            String err = e.getMessage();
            LOGGER.log(Level.SEVERE, "Error saving {0} {1}: {2}", new Object[] {
                info.getClass().getSimpleName(), name, err
            });
        }
        return Optional.empty();
    }

    private <C extends Info> Optional<C> depersist(Path file) {
        Optional<C> info = fileWalk.getXStreamLoader().depersist(file);
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

    private void log(Level level, String fmt, Object... args) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, fmt, args);
        }
    }
}
