/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.common.base.Preconditions;
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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resources.ExtensionFilter;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.storage.blobstore.file.FilePathUtils;

public class DefaultTileLayerCatalog implements TileLayerCatalog {

    private static final Logger LOGGER = Logging.getLogger(DefaultTileLayerCatalog.class);

    private static final String LAYERINFO_DIRECTORY = "gwc-layers";

    private Map<String, GeoServerTileLayerInfo> layersById;

    /** View of layer ids by name */
    private Map<String, String> layersByName;

    private final XStream serializer;

    private final GeoServerResourceLoader resourceLoader;

    private final String baseDirectory;

    private volatile boolean initialized;

    private Map<String, ResourceListener> listenersByFileName;

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
        this.listenersByFileName = new ConcurrentHashMap<>();
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
                .addListener(
                        new ResourceListener() {
                            @Override
                            public void changed(ResourceNotification notify) {
                                for (Event event : notify.events()) {
                                    if ((event.getKind() == ResourceNotification.Kind.ENTRY_CREATE
                                                    || event.getKind()
                                                            == ResourceNotification.Kind
                                                                    .ENTRY_MODIFY)
                                            && !event.getPath().contains("/")
                                            && event.getPath().toLowerCase().endsWith(".xml")
                                            && !listenersByFileName.containsKey(event.getPath())) {
                                        GeoServerTileLayerInfoImpl info =
                                                load(
                                                        resourceLoader
                                                                .get(baseDirectory)
                                                                .get(event.getPath()));
                                        if (info != null) {
                                            for (TileLayerCatalogListener listener : listeners) {
                                                listener.onEvent(
                                                        info.getId(),
                                                        TileLayerCatalogListener.Type.CREATE);
                                            }
                                        }
                                    }
                                }
                            }
                        });
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

        ExtensionFilter xmlFilter = new Resources.ExtensionFilter("XML");
        baseDir.list()
                .parallelStream()
                .filter(r -> xmlFilter.accept(r))
                .forEach(
                        res -> {
                            load(res);
                        });
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
        String id = layersByName.get(layerName);
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
            GeoServerTileLayerInfo info = getLayerById(tileLayerId);
            if (info != null) {
                Resource file = getFile(tileLayerId);
                layersById.remove(tileLayerId);
                layersByName.remove(info.getName());
                stopListening(file);
                file.delete();
                listenersByFileName.remove(file.name());
            }
            return info;
        } catch (IOException notFound) {
            LOGGER.log(Level.FINEST, "Deleting " + tileLayerId, notFound);
            return null;
        }
    }

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
                throw propagate(other);
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
                propagate(((ExecutionException) e).getCause());
            }
            propagate(e);
        }
        return oldValue;
    }

    private GeoServerTileLayerInfoImpl load(Resource res) {
        GeoServerTileLayerInfoImpl info;
        try {
            info = depersist(res);
            startListening(res, info.getId());
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error depersisting tile layer information from file " + res.name(),
                    e);
            return null;
        }

        layersByName.put(info.getName(), info.getId());
        layersById.put(info.getId(), info);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Loaded tile layer '" + info.getName() + "'");
        }

        return info;
    }

    private void reload(String id, Resource res) {
        GeoServerTileLayerInfo old = layersById.remove(id);
        if (old != null) {
            layersByName.remove(old.getName());
        }
        load(res);
    }

    private void startListening(Resource file, String tileLayerId) {
        ResourceListener existingLayerListener =
                new ResourceListener() {
                    @Override
                    public void changed(ResourceNotification notify) {
                        if (notify.getKind() == ResourceNotification.Kind.ENTRY_MODIFY) {
                            reload(tileLayerId, resourceLoader.get(notify.getPath()));
                            for (TileLayerCatalogListener listener : listeners) {
                                listener.onEvent(tileLayerId, TileLayerCatalogListener.Type.MODIFY);
                            }
                        } else if (notify.getKind() == ResourceNotification.Kind.ENTRY_DELETE) {
                            delete(tileLayerId);
                            for (TileLayerCatalogListener listener : listeners) {
                                listener.onEvent(tileLayerId, TileLayerCatalogListener.Type.DELETE);
                            }
                        }
                    }
                };
        listenersByFileName.put(file.name(), existingLayerListener);
        file.addListener(existingLayerListener);
    }

    private void stopListening(Resource file) {
        ResourceListener existingLayerListener = listenersByFileName.get(file.name());
        if (existingLayerListener != null) {
            file.removeListener(existingLayerListener);
        }
    }

    private void persist(GeoServerTileLayerInfo real) throws IOException {
        final String tileLayerId = real.getId();
        Resource file = getFile(tileLayerId);

        stopListening(file);

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
            propagateIfInstanceOf(e, IOException.class);
            throw propagate(e);
        }
        // sanity check
        try {
            depersist(tmp);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Persisted version of tile layer " + real.getName() + " can't be loaded back",
                    e);
            propagateIfInstanceOf(e, IOException.class);
            throw propagate(e);
        }
        rename(tmp, file);

        startListening(file, tileLayerId);
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
        final String fileName = FilePathUtils.filteredLayerName(tileLayerId) + ".xml";

        final Resource base = resourceLoader.get(baseDirectory);

        return base.get(fileName);
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
            // windows does not do atomic renames, and can not rename a file if the dest file
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
