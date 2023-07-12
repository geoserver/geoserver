/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir.internal;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geotools.util.logging.Logging;
import org.springframework.lang.NonNull;

class DataDirectoryWalker {

    private static final Logger LOGGER =
            Logging.getLogger(DataDirectoryWalker.class.getPackage().getName());

    private Path dataDirRoot;

    /**
     * List of possible service info file names as reported by all available {@link
     * XStreamServiceLoader#getFilename()}
     */
    private List<String> serviceFileNames;

    static class WorkspaceDirectory {
        public final @NonNull Path workspaceFile;
        public final @NonNull Path namespaceFile;
        public final @NonNull Optional<Path> settings;
        public final @NonNull Set<String> serviceInfoFileNames;

        private @NonNull DataDirectoryWalker walker;

        WorkspaceDirectory(
                Path ws,
                Path ns,
                Optional<Path> settings,
                Set<String> serviceInfoFileNames,
                DataDirectoryWalker walker) {
            requireNonNull(ws);
            requireNonNull(ns);
            requireNonNull(settings);
            requireNonNull(serviceInfoFileNames);
            requireNonNull(walker);
            this.workspaceFile = ws;
            this.namespaceFile = ns;
            this.settings = settings;
            this.serviceInfoFileNames = serviceInfoFileNames;
            this.walker = walker;
        }

        public Stream<StoreDirectory> stores() {
            return walker.stores(workspaceFile.getParent());
        }

        public List<Path> styles() {
            return walker.childXmlFiles(workspaceFile.resolveSibling("styles"));
        }

        public List<Path> layerGroups() {
            return walker.childXmlFiles(workspaceFile.resolveSibling("layergroups"));
        }
    }

    static class StoreDirectory {
        public final @NonNull Path storeFile;
        private @NonNull DataDirectoryWalker walker;

        public StoreDirectory(Path storeFile, DataDirectoryWalker walker) {
            requireNonNull(storeFile);
            requireNonNull(walker);
            this.storeFile = storeFile;
            this.walker = walker;
        }

        public Stream<LayerDirectory> layers() {
            Path parent =
                    Optional.ofNullable(storeFile.getParent())
                            .orElseThrow(NullPointerException::new);
            return walker.layers(parent);
        }
    }

    static class LayerDirectory {
        public final @NonNull Path resourceFile;
        public final @NonNull Path layerFile;

        public LayerDirectory(Path resourceFile, Path layerFile) {
            requireNonNull(resourceFile);
            requireNonNull(layerFile);
            this.resourceFile = resourceFile;
            this.layerFile = layerFile;
        }
    }

    public DataDirectoryWalker(@NonNull Path dataDirRoot, @NonNull List<String> serviceFileNames) {
        requireNonNull(dataDirRoot);
        requireNonNull(serviceFileNames);
        this.dataDirRoot = dataDirRoot;
        this.serviceFileNames = serviceFileNames;
    }

    public Path getRoot() {
        return this.dataDirRoot;
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

    private static final Predicate<Path> STORE_DIRNAME_FILTER =
            dir -> {
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
        return childXmlFiles(dataDirRoot.resolve("styles"));
    }

    public List<Path> globalLayerGroups() {
        return childXmlFiles(dataDirRoot.resolve("layergroups"));
    }

    // cached to make a single pass when loading both the catalog and geoserver
    private List<WorkspaceDirectory> workspaces;

    public List<WorkspaceDirectory> workspaces() {
        if (workspaces == null) {
            Path workspacesRoot = dataDirRoot.resolve("workspaces");
            List<Path> workspaceDirectories = subdirectories(workspacesRoot);
            workspaces =
                    workspaceDirectories.stream()
                            .map(this::newWorkspace)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
        }
        return workspaces;
    }

    public Optional<Path> gsGlobal() {
        return optionalFile(dataDirRoot.resolve("global.xml"));
    }

    public Optional<Path> gsLogging() {
        return optionalFile(dataDirRoot.resolve("logging.xml"));
    }

    private Optional<Path> optionalFile(Path file) {
        boolean exists = Files.isRegularFile(file);
        return Optional.ofNullable(exists ? file : null);
    }

    public void dispose() {
        workspaces = null;
    }

    private WorkspaceDirectory newWorkspace(Path wsdir) {
        Path ws = wsdir.resolve("workspace.xml");
        Path ns = wsdir.resolve("namespace.xml");

        final boolean wsExists = Files.isRegularFile(ws);
        final boolean nsExists = Files.isRegularFile(ns);
        if (wsExists && nsExists) {

            Optional<Path> settings = optionalFile(wsdir.resolve("settings.xml"));
            // cache which ServiceInfo xml files are in the workspace directory to make a single
            // pass
            Set<String> serviceInfoFiles =
                    serviceFileNames.stream()
                            .filter(f -> Files.isRegularFile(wsdir.resolve(f)))
                            .collect(Collectors.toSet());

            return new WorkspaceDirectory(ws, ns, settings, serviceInfoFiles, this);
        }
        if (!wsExists) LOGGER.warning("workspace.xml missing at " + wsdir);
        if (!nsExists) LOGGER.warning("namespace.xml missing at " + wsdir + " ignoring workspace");

        return null;
    }

    private List<Path> subdirectories(Path parent) {
        return children(parent, Files::isDirectory);
    }

    private List<Path> childXmlFiles(Path parent) {
        return children(parent, "*.xml");
    }

    private List<Path> children(Path parent, String glob) {
        if (Files.isDirectory(parent)) {
            try {
                return toStream(parent, Files.newDirectoryStream(parent, glob));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Collections.emptyList();
    }

    private List<Path> children(Path parent, DirectoryStream.Filter<Path> filter) {
        if (Files.isDirectory(parent)) {
            try {
                return toStream(parent, Files.newDirectoryStream(parent, filter));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Collections.emptyList();
    }

    private List<Path> toStream(Path parent, DirectoryStream<Path> children) throws IOException {
        try {
            return Lists.newArrayList(children);
        } finally {
            children.close();
        }
    }
}
