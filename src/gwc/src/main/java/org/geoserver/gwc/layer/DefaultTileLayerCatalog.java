/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Throwables.throwIfInstanceOf;
import static com.google.common.base.Throwables.throwIfUnchecked;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resources.ExtensionFilter;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.storage.blobstore.file.FilePathUtils;

public class DefaultTileLayerCatalog implements TileLayerCatalog {

    private static final Logger LOGGER = Logging.getLogger(DefaultTileLayerCatalog.class);

    private static final String LAYERINFO_DIRECTORY = "gwc-layers";

    private ConcurrentMap<String, GeoServerTileLayerInfo> layersById;

    /** View of layer ids by name */
    private Map<String, String> layersByName;

    private final XStream serializer;

    private final GeoServerResourceLoader resourceLoader;

    private final String baseDirectory;

    private volatile boolean initialized;

    private List<TileLayerCatalogListener> listeners;

    public DefaultTileLayerCatalog(
            GeoServerResourceLoader resourceLoader, XMLConfiguration xmlPersisterFactory)
            throws IOException {
        this(
                resourceLoader,
                xmlPersisterFactory.getConfiguredXStreamWithContext(
                        new SecureXStream(), Context.PERSIST));
    }

    DefaultTileLayerCatalog(GeoServerResourceLoader resourceLoader, XStream configuredXstream)
            throws IOException {

        this.resourceLoader = resourceLoader;
        this.baseDirectory = LAYERINFO_DIRECTORY;

        this.layersByName = new ConcurrentHashMap<>();
        this.layersById = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.initialized = false;

        // setup xstream security for local classes
        this.serializer = configuredXstream;
        this.serializer.allowTypeHierarchy(GeoServerTileLayerInfo.class);
        // have to use a string here because UnmodifiableSet is private
        this.serializer.allowTypes(new String[] {"java.util.Collections$UnmodifiableSet"});
        // automatically reload configuration on change
        resourceLoader
                .get(baseDirectory)
                .addListener(evt -> evt.events().forEach(this::handleBaseDirectoryResourceEvent));
    }

    private void handleBaseDirectoryResourceEvent(Event event) {
        final String path = event.getPath();
        final boolean isLayerFile = !path.contains("/") && path.toLowerCase().endsWith(".xml");
        if (!isLayerFile) {
            return;
        }
        if (event.getKind() == Kind.ENTRY_DELETE) {
            // resource is no longer available, figure out the id the hard(ish) way
            String layerIdName =
                    this.layersById
                            .keySet()
                            .parallelStream()
                            .map(this::layerIdToFileName)
                            .filter(path::equals)
                            .findFirst()
                            .orElse(null);
            if (layerIdName == null) {
                // we don't have it, no need to notify local listeners
                return;
            }
            Preconditions.checkState(layerIdName.endsWith(".xml"));
            final String layerId = layerIdName.substring(0, layerIdName.lastIndexOf(".xml"));
            GeoServerTileLayerInfo removed = this.layersById.remove(layerId);
            if (removed != null) {
                this.layersByName.remove(removed.getName());
            }
            listeners.forEach(l -> l.onEvent(layerId, TileLayerCatalogListener.Type.DELETE));
            return;
        }

        final Resource resource = resourceLoader.get(baseDirectory).get(path);
        GeoServerTileLayerInfoImpl layerInfo;
        try {
            // do not call load(resource) here, the layer would end up in this.layersById
            // even if it wasn't created by this instance
            layerInfo = depersist(resource);
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error depersisting tile layer information from file " + resource.name(),
                    e);
            return;
        }
        final String layerId = layerInfo.getId();
        final GeoServerTileLayerInfo currentInfo = this.layersById.get(layerId);

        final TileLayerCatalogListener.Type tileEventType =
                event.getKind() == Kind.ENTRY_CREATE
                        ? TileLayerCatalogListener.Type.CREATE
                        : TileLayerCatalogListener.Type.MODIFY;

        if (event.getKind() == Kind.ENTRY_MODIFY
                && currentInfo != null
                && !currentInfo.getName().contentEquals(layerInfo.getName())) {
            layersByName.remove(currentInfo.getName());
        }
        saveInternal(layerInfo);
        listeners.forEach(l -> l.onEvent(layerId, tileEventType));
    }

    @Override
    public void reset() {
        layersById.clear();
        layersByName.clear();
        this.initialized = false;
    }

    @Override
    public void initialize() {
        reset();

        Resource baseDir = resourceLoader.get(baseDirectory);

        LOGGER.info("GeoServer TileLayer store base directory is: " + baseDir.path());
        LOGGER.info("Loading tile layers from " + baseDir.path());
        Stopwatch sw = Stopwatch.createStarted();
        ExtensionFilter xmlFilter = new Resources.ExtensionFilter("XML");
        baseDir.list().parallelStream().filter(r -> xmlFilter.accept(r)).forEach(this::load);
        LOGGER.info(String.format("Loaded %,d tile layers in %s", layersById.size(), sw.stop()));
        this.initialized = true;
    }

    @Override
    public GeoServerTileLayerInfo getLayerById(final String id) {
        checkInitialized();
        GeoServerTileLayerInfo layer = layersById.get(id);
        return layer == null ? null : layer.clone();
    }

    private synchronized void checkInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    @Override
    public GeoServerTileLayerInfo getLayerByName(String layerName) {
        checkInitialized();
        String id = getLayerId(layerName);
        if (id == null) {
            return null;
        }
        return getLayerById(id);
    }

    @Override
    public Set<String> getLayerIds() {
        checkInitialized();
        return ImmutableSet.copyOf(layersById.keySet());
    }

    @Override
    public boolean exists(String layerId) {
        checkInitialized();
        return layersById.containsKey(layerId);
    }

    @Override
    public Set<String> getLayerNames() {
        checkInitialized();
        return ImmutableSet.copyOf(layersByName.keySet());
    }

    @Override
    public GeoServerTileLayerInfo delete(final String tileLayerId) {
        checkInitialized();
        try {
            GeoServerTileLayerInfo currValue = layersById.remove(tileLayerId);
            if (currValue != null) {
                Resource file = getFile(tileLayerId);
                layersByName.remove(currValue.getName());
                file.delete();
                listeners.forEach(
                        l -> l.onEvent(tileLayerId, TileLayerCatalogListener.Type.DELETE));
                return currValue;
            }
        } catch (IOException notFound) {
            LOGGER.log(Level.FINEST, "Deleting " + tileLayerId, notFound);
        }
        return null;
    }

    /**
     * Called both when a new tile layer is created or when an existing one is modified on this
     * service instance
     *
     * @return the previous value, or {@code null} if the tile layer didn't previously exist on this
     *     tile layer catalog
     */
    @Override
    public GeoServerTileLayerInfo save(final GeoServerTileLayerInfo newValue) {
        checkInitialized();
        GeoServerTileLayerInfoImpl oldValue = null;

        final String tileLayerId = newValue.getId();
        Preconditions.checkNotNull(tileLayerId);

        try {
            try {
                oldValue = loadInternal(tileLayerId);
            } catch (FileNotFoundException ignore) {
                // ok
            } catch (Exception other) {
                throwIfUnchecked(other);
            }

            if (oldValue == null) {
                final String duplicateNameId = layersByName.get(newValue.getName());
                if (null != duplicateNameId) {
                    throw new IllegalArgumentException(
                            "TileLayer with same name already exists: "
                                    + newValue.getName()
                                    + ": <"
                                    + duplicateNameId
                                    + ">");
                }
            } else {
                layersByName.remove(oldValue.getName());
            }
            persist(newValue);
            layersByName.put(newValue.getName(), newValue.getId());
            layersById.put(newValue.getId(), newValue.clone());
        } catch (Exception e) {
            if (e instanceof ExecutionException) {
                throwIfUnchecked(((ExecutionException) e).getCause());
            }
            throwIfUnchecked(e);
        }
        return oldValue;
    }

    private GeoServerTileLayerInfoImpl load(Resource res) {
        GeoServerTileLayerInfoImpl info;
        try {
            info = depersist(res);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error depersisting tile layer information from file " + res.name(),
                    e);
            return null;
        }
        saveInternal(info);
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Loaded tile layer '" + info.getName() + "'");
        }
        return info;
    }

    private void saveInternal(GeoServerTileLayerInfoImpl info) {
        layersByName.put(info.getName(), info.getId());
        layersById.put(info.getId(), info);
    }

    private void persist(GeoServerTileLayerInfo real) throws IOException {
        final String tileLayerId = real.getId();
        Resource file = getFile(tileLayerId);

        boolean cleanup = false;
        if (file.getType() == Type.UNDEFINED) {
            cleanup = true;
        }
        final Resource tmp = file.parent().get(file.name() + ".tmp");
        try {
            final Writer writer = new OutputStreamWriter(tmp.out(), "UTF-8");
            try {
                serializer.toXML(real, writer);
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            tmp.delete();
            if (cleanup) {
                file.delete();
            }
            throwIfInstanceOf(e, IOException.class);
            throwIfUnchecked(e);
        }
        // sanity check
        try {
            depersist(tmp);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Persisted version of tile layer " + real.getName() + " can't be loaded back",
                    e);
            throwIfInstanceOf(e, IOException.class);
            throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
        rename(tmp, file);
    }

    private GeoServerTileLayerInfoImpl loadInternal(final String tileLayerId)
            throws FileNotFoundException, IOException {
        final Resource file = getFile(tileLayerId);
        if (file.getType() == Type.UNDEFINED) {
            throw new FileNotFoundException(tileLayerId);
        }
        return depersist(file);
    }

    private Resource getFile(final String tileLayerId) throws IOException {
        final String fileName = layerIdToFileName(tileLayerId);

        final Resource base = resourceLoader.get(baseDirectory);

        return base.get(fileName);
    }

    private String layerIdToFileName(final String tileLayerId) {
        return FilePathUtils.filteredLayerName(tileLayerId) + ".xml";
    }

    private GeoServerTileLayerInfoImpl depersist(final Resource res) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Depersisting GeoServerTileLayerInfo from " + res.path());
        }
        GeoServerTileLayerInfoImpl info;
        try (Reader reader =
                new InputStreamReader(new ByteArrayInputStream(res.getContents()), "UTF-8")) {
            info = (GeoServerTileLayerInfoImpl) serializer.fromXML(reader);
        }

        return info;
    }

    private GeoServerTileLayerInfoImpl depersist(final byte[] contents) throws IOException {
        GeoServerTileLayerInfoImpl info;
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(contents), "UTF-8")) {
            info = (GeoServerTileLayerInfoImpl) serializer.fromXML(reader);
        }

        return info;
    }

    private void rename(Resource source, Resource dest) throws IOException {
        // same resource? Do nothing
        if (source.equals(dest)) return;

        // different resource
        boolean win = System.getProperty("os.name").startsWith("Windows");
        if (win && Resources.exists(dest)) {
            // windows does not do atomic renames, and can not rename a file if the dest
            // file
            // exists
            if (!dest.delete()) {
                throw new IOException("Could not delete: " + dest.path());
            }
            source.renameTo(dest);
        } else {
            source.renameTo(dest);
        }
    }

    @Override
    public String getLayerId(String layerName) {
        checkInitialized();
        final WorkspaceInfo ws = LocalWorkspace.get();
        if (ws != null && !layerName.startsWith(ws.getName() + ":")) {
            layerName = ws.getName() + ":" + layerName;
        }
        return layersByName.get(layerName);
    }

    @Override
    public String getLayerName(String layerId) {
        checkInitialized();
        return layersById.get(layerId).getName();
    }

    @Override
    public String getPersistenceLocation() {
        return resourceLoader.get(baseDirectory).path();
    }

    @Override
    public void addListener(TileLayerCatalogListener listener) {
        listeners.add(listener);
    }
}
