/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.storage.blobstore.file.FilePathUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.XStream;

public class DefaultTileLayerCatalog implements TileLayerCatalog {

    private static final Logger LOGGER = Logging.getLogger(DefaultTileLayerCatalog.class);

    private static final String LAYERINFO_DIRECTORY = "gwc-layers";

    private BiMap<String, String> layersById;

    /**
     * View of layer ids by name
     */
    private BiMap<String, String> layersByName;

    private final XStream serializer;

    private final GeoServerResourceLoader resourceLoader;

    private final String baseDirectory;

    private volatile boolean initialized;

    public DefaultTileLayerCatalog(GeoServerResourceLoader resourceLoader,
            XMLConfiguration xmlPersisterFactory) throws IOException {
        this(resourceLoader,
                // this configures security back in GWC, no point in using SecureXStream here
                xmlPersisterFactory.getConfiguredXStreamWithContext(new XStream(),
                Context.PERSIST));
    }

    DefaultTileLayerCatalog(GeoServerResourceLoader resourceLoader, XStream configuredXstream)
            throws IOException {

        this.resourceLoader = resourceLoader;
        this.baseDirectory = LAYERINFO_DIRECTORY;

        BiMap<String, String> baseBiMap = HashBiMap.create();
        this.layersById = Maps.synchronizedBiMap(baseBiMap);
        this.layersByName = layersById.inverse();
        this.initialized = false;

        // setup xstream security for local classes
        this.serializer = configuredXstream;
        this.serializer.allowTypeHierarchy(GeoServerTileLayerInfo.class);
        this.serializer.allowTypeHierarchy(SortedSet.class);

    }

    @Override
    public void reset() {
        layersById.clear();
        this.initialized = false;
    }

    @Override
    public void initialize() {

        layersById.clear();

        Resource baseDir = resourceLoader.get(baseDirectory);

        LOGGER.info("GeoServer TileLayer store base directory is: " + baseDir.path());

        final List<Resource> tileLayerFiles = Resources.list(baseDir, new Filter<Resource>() {
            @Override
            public boolean accept(Resource res) {
                return res.name().endsWith(".xml");
            }
        });

        LOGGER.info("Loading tile layers from " + baseDir.path());
        for (Resource res : tileLayerFiles) {
            GeoServerTileLayerInfoImpl info;
            try {
                info = depersist(res);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error depersisting tile layer information from file "
                        + res.name(), e);
                continue;
            }

            layersById.put(info.getId(), info.getName());

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Loaded tile layer '" + info.getName() + "'");
            }
        }
        this.initialized = true;
    }

    @Override
    public GeoServerTileLayerInfo getLayerById(final String id) {
        checkInitialized();
        if (!layersById.containsKey(id)) {
            return null;
        }

        try {
            GeoServerTileLayerInfo real = loadInternal(id);
            return real;
        } catch (IOException e) {
            LOGGER.finer("GeoServer tile layer does not exist or can't be loaded: " + id);
            LOGGER.log(Level.FINEST, "Trying to load tile layer " + id, e);
        }

        return null;
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
                file.delete();
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
                    throw new IllegalArgumentException("TileLayer with same name already exists: "
                            + newValue.getName() + ": <" + duplicateNameId + ">");
                }
            } else {
                layersByName.remove(oldValue.getName());
            }

            persist(newValue);
            layersById.put(newValue.getId(), newValue.getName());

        } catch (Exception e) {
            if (e instanceof ExecutionException) {
                propagate(((ExecutionException) e).getCause());
            }
            propagate(e);
        }
        return oldValue;
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
            propagateIfInstanceOf(e, IOException.class);
            throw propagate(e);
        }
        // sanity check
        try {
            depersist(tmp);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Persisted version of tile layer " + real.getName()
                    + " can't be loaded back", e);
            propagateIfInstanceOf(e, IOException.class);
            throw propagate(e);
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
        final String fileName = FilePathUtils.filteredLayerName(tileLayerId) + ".xml";

        final Resource base = resourceLoader.get(baseDirectory);

        return base.get(fileName);
    }

    private GeoServerTileLayerInfoImpl depersist(final Resource res) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Depersisting GeoServerTileLayerInfo from " + res.path());
        }
        GeoServerTileLayerInfoImpl info;
        try(Reader reader = new InputStreamReader(res.in(), "UTF-8")) {
            info = (GeoServerTileLayerInfoImpl) serializer.fromXML(reader);
        }

        return info;
    }

    private void rename(Resource source, Resource dest) throws IOException {
        // same resource? Do nothing
        if (source.equals(dest))
            return;

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
        return layersById.get(layerId);
    }

}
