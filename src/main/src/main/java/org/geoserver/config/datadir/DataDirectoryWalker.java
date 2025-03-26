/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.lang.NonNull;

/**
 * Provides efficient traversal of the GeoServer data directory structure.
 *
 * <p>This class implements a specialized walker that understands the GeoServer data directory structure and provides
 * hierarchical access to workspaces, stores, layers, and other configuration entities with minimal filesystem
 * operations. Key features include:
 *
 * <ul>
 *   <li>Single-pass traversal of the data directory structure
 *   <li>Caching of workspace directories for reuse between catalog and config loading
 *   <li>Specialized inner classes for working with different directory types
 *   <li>Lazy loading of directory contents to minimize filesystem operations
 * </ul>
 *
 * <p>The inner classes ({@link WorkspaceDirectory}, {@link StoreDirectory}, {@link LayerDirectory}) represent the
 * different directory types in the GeoServer data directory hierarchy and provide type-safe access to their contents.
 */
class DataDirectoryWalker {

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryWalker.class.getPackage().getName());

    private final GeoServerDataDirectory dataDirectory;

    private final XStreamLoader xstreamLoader;

    private final List<XStreamServiceLoader<ServiceInfo>> serviceLoaders;

    /**
     * List of possible service info file names as reported by all available {@link XStreamServiceLoader#getFilename()}
     */
    private final List<String> serviceFileNames;

    private GeoServerConfigurationLock configLock;

    public DataDirectoryWalker(
            GeoServerDataDirectory dataDirectory, XStreamPersisterFactory xpf, GeoServerConfigurationLock configLock) {
        this(dataDirectory, xpf, configLock, findServiceLoaders());
    }

    @SuppressWarnings("unchecked")
    DataDirectoryWalker(
            GeoServerDataDirectory dataDirectory,
            XStreamPersisterFactory xpf,
            GeoServerConfigurationLock configLock,
            List<?> serviceLoaders) {
        this.dataDirectory = dataDirectory;
        this.configLock = configLock;
        this.xstreamLoader = new XStreamLoader(xpf);
        // cache all possible service loaders and their file names, and force #findServiceLoaders()
        this.serviceLoaders = serviceLoaders.stream()
                .map(l -> (XStreamServiceLoader<ServiceInfo>) l)
                .collect(Collectors.toList());
        this.serviceFileNames = this.serviceLoaders.stream()
                .map(XStreamServiceLoader::getFilename)
                .collect(Collectors.toList());
    }

    public List<XStreamServiceLoader<ServiceInfo>> getServiceLoaders() {
        return serviceLoaders;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<XStreamServiceLoader<ServiceInfo>> findServiceLoaders() {
        return (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);
    }

    public GeoServerDataDirectory getDataDirectory() {
        return dataDirectory;
    }

    public XStreamLoader getXStreamLoader() {
        return xstreamLoader;
    }

    /**
     * Grab a config lock, to be used when a sanitization task needs to change the data directory during startup.
     *
     * <p>GeoServerConfigurationLock implementations (such as in GeoServer Cloud) may grab a cluster-wide lock to
     * support concurrent start ups on a shared directory.
     */
    public void lock() {
        getConfigurationLock().lock(LockType.WRITE);
    }

    /** Release the config {@link #lock()} */
    public void unlock() {
        getConfigurationLock().unlock();
    }

    public GeoServerConfigurationLock getConfigurationLock() {
        return configLock;
    }

    public Path getRoot() {
        return dataDirectory.root().toPath();
    }

    private Stream<LayerDirectory> layers(Path storeDirectory) {
        return subdirectories(storeDirectory).stream()
                .map(this::newLayer)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<LayerDirectory> newLayer(Path layerDirectory) {
        Path layerFile = layerDirectory.resolve("layer.xml");
        if (Files.isRegularFile(layerFile)) {
            return Stream.of("featuretype.xml", "coverage.xml", "wmslayer.xml", "wmtslayer.xml")
                    .map(layerDirectory::resolve)
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .map(resourceFile -> new LayerDirectory(resourceFile, layerFile));
        }
        return Optional.empty();
    }

    private static final Predicate<Path> STORE_DIRNAME_FILTER = dir -> {
        String name = dir.getFileName().toString();
        return !"styles".equals(name) && !"layergroups".equals(name);
    };

    private Stream<StoreDirectory> stores(Path workspaceDir) {
        return subdirectories(workspaceDir).stream()
                .filter(STORE_DIRNAME_FILTER)
                .map(this::newStore)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<StoreDirectory> newStore(Path directory) {
        return Stream.of("datastore.xml", "coveragestore.xml", "wmsstore.xml", "wmtsstore.xml")
                .map(directory::resolve)
                .filter(Files::isRegularFile)
                .findFirst()
                .map(storeFile -> new StoreDirectory(storeFile, this));
    }

    public List<Path> globalStyles() {
        return childXmlFiles(getRoot().resolve("styles"));
    }

    public List<Path> globalLayerGroups() {
        return childXmlFiles(getRoot().resolve("layergroups"));
    }

    // cached to make a single pass when loading both the catalog and geoserver
    private List<WorkspaceDirectory> workspaces;

    public List<WorkspaceDirectory> workspaces() {
        if (workspaces == null) {
            Path workspacesRoot = workspacesRoot();
            List<Path> workspaceDirectories = subdirectories(workspacesRoot);
            workspaces = workspaceDirectories.stream()
                    .parallel()
                    .map(this::newWorkspace)
                    .filter(Optional::isPresent)
                    .map(Optional::orElseThrow)
                    .collect(Collectors.toList());
        }
        return workspaces;
    }

    private Path workspacesRoot() {
        return getRoot().resolve("workspaces");
    }

    public Optional<Path> gsGlobal() {
        return optionalFile(getRoot().resolve("global.xml"));
    }

    public Optional<Path> gsLogging() {
        return optionalFile(getRoot().resolve("logging.xml"));
    }

    public Optional<Path> defaultWorkspace() {
        return optionalFile(getDefaultWorkspaceFile());
    }

    public Path getDefaultWorkspaceFile() {
        return workspacesRoot().resolve("default.xml");
    }

    private Optional<Path> optionalFile(Path file) {
        boolean exists = Files.isRegularFile(file);
        return Optional.ofNullable(exists ? file : null);
    }

    private Optional<WorkspaceDirectory> newWorkspace(Path wsdir) {
        return WorkspaceDirectory.newInstance(wsdir, this);
    }

    private List<Path> subdirectories(Path parent) {
        return children(parent, Files::isDirectory);
    }

    private List<Path> childXmlFiles(Path parent) {
        return children(parent, "*.xml");
    }

    private List<Path> children(Path parent, String glob) {
        if (Files.isDirectory(parent)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent, glob)) {
                return toList(directoryStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Collections.emptyList();
    }

    private List<Path> children(Path parent, DirectoryStream.Filter<Path> filter) {
        if (Files.isDirectory(parent)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent, filter)) {
                return toList(directoryStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Collections.emptyList();
    }

    private List<Path> toList(Iterable<Path> children) {
        return Lists.newArrayList(children);
    }

    /**
     * Represents a workspace directory in the GeoServer data directory.
     *
     * <p>Provides access to workspace-related files including:
     *
     * <ul>
     *   <li>workspace.xml - Workspace configuration
     *   <li>namespace.xml - Associated namespace configuration
     *   <li>settings.xml - Workspace-specific settings (if present)
     *   <li>Service configuration files (optional)
     *   <li>Style files in the styles/ subdirectory
     *   <li>Layer group files in the layergroups/ subdirectory
     *   <li>Store directories (for datastore, coveragestore, etc.)
     * </ul>
     *
     * <p>This class caches the list of service configuration files present in the workspace to avoid repeated
     * filesystem operations.
     */
    static class WorkspaceDirectory {
        private final @NonNull Path directory;
        private final @NonNull DataDirectoryWalker walker;

        /** Cached set of service configuration file names present in this workspace directory */
        private Set<String> serviceInfoFileNames = Set.of();

        private WorkspaceDirectory(Path directory, DataDirectoryWalker walker) {
            requireNonNull(directory);
            requireNonNull(walker);
            this.directory = directory;
            this.walker = walker;
        }

        public static Optional<WorkspaceDirectory> newInstance(Path workspaceDirectory, DataDirectoryWalker fileWalk) {

            WorkspaceDirectory wd = new WorkspaceDirectory(workspaceDirectory, fileWalk);
            Path ws = wd.workspaceFile();
            Path ns = wd.namespaceFile();

            final boolean wsExists = Files.isRegularFile(ws);
            final boolean nsExists = Files.isRegularFile(ns);

            if (wsExists && nsExists) {
                // cache which ServiceInfo xml files are in the workspace directory to make a
                // single pass
                List<String> availableServiceFileNames = fileWalk.serviceFileNames;
                wd.serviceInfoFileNames = availableServiceFileNames.stream()
                        .filter(f -> Files.isRegularFile(workspaceDirectory.resolve(f)))
                        .collect(Collectors.toSet());

                return Optional.of(wd);
            }
            if (!wsExists) LOGGER.severe("workspace.xml missing at " + workspaceDirectory);
            if (!nsExists) LOGGER.severe("namespace.xml missing at " + workspaceDirectory + " ignoring workspace");
            return Optional.empty();
        }

        public Path directory() {
            return directory;
        }

        public Path workspaceFile() {
            return directory.resolve("workspace.xml");
        }

        public Path namespaceFile() {
            return directory.resolve("namespace.xml");
        }

        public Optional<Path> settingsFile() {
            return Optional.of(directory.resolve("settings.xml")).filter(Files::isRegularFile);
        }

        public Set<String> serviceInfoFileNames() {
            return serviceInfoFileNames;
        }

        public Stream<StoreDirectory> stores() {
            return walker.stores(directory);
        }

        public List<Path> styles() {
            return walker.childXmlFiles(directory.resolve("styles"));
        }

        public List<Path> layerGroups() {
            return walker.childXmlFiles(directory.resolve("layergroups"));
        }
    }

    /**
     * Represents a store directory in the GeoServer data directory.
     *
     * <p>A store directory contains:
     *
     * <ul>
     *   <li>A store configuration file (datastore.xml, coveragestore.xml, wmsstore.xml, etc.)
     *   <li>Subdirectories for each layer associated with the store
     * </ul>
     *
     * <p>This class provides access to the store configuration file and methods to traverse the contained layer
     * directories.
     */
    static class StoreDirectory {
        /** The path to the store configuration file (datastore.xml, coveragestore.xml, etc.) */
        public final @NonNull Path storeFile;

        private @NonNull DataDirectoryWalker walker;

        public StoreDirectory(Path storeFile, DataDirectoryWalker walker) {
            requireNonNull(storeFile);
            requireNonNull(walker);
            this.storeFile = storeFile;
            this.walker = walker;
        }

        public Stream<LayerDirectory> layers() {
            Path parent = Optional.ofNullable(storeFile.getParent()).orElseThrow(NullPointerException::new);
            return walker.layers(parent);
        }
    }

    /**
     * Represents a layer directory in the GeoServer data directory.
     *
     * <p>A layer directory contains:
     *
     * <ul>
     *   <li>A layer.xml file with layer configuration
     *   <li>A resource configuration file (featuretype.xml, coverage.xml, wmslayer.xml, etc.)
     * </ul>
     *
     * <p>This class provides access to both the layer configuration file and the resource configuration file.
     */
    static class LayerDirectory {
        /** The path to the resource configuration file (featuretype.xml, coverage.xml, etc.) */
        public final @NonNull Path resourceFile;

        /** The path to the layer.xml configuration file */
        public final @NonNull Path layerFile;

        public LayerDirectory(Path resourceFile, Path layerFile) {
            requireNonNull(resourceFile);
            requireNonNull(layerFile);
            this.resourceFile = resourceFile;
            this.layerFile = layerFile;
        }
    }
}
