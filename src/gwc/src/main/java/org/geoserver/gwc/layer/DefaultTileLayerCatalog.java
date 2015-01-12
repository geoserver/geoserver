/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.storage.blobstore.file.FilePathGenerator;
import org.geowebcache.storage.blobstore.file.FilePathUtils;
import org.geowebcache.util.FileUtils;

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
            XMLConfiguration xmlPersisterFactory, String baseDirectory) throws IOException {
        this(resourceLoader, xmlPersisterFactory.getConfiguredXStreamWithContext(new XStream(), 
                Context.PERSIST), baseDirectory);
    }
    
    public DefaultTileLayerCatalog(GeoServerResourceLoader resourceLoader,
            XMLConfiguration xmlPersisterFactory) throws IOException {
        this(resourceLoader, xmlPersisterFactory.getConfiguredXStreamWithContext(new XStream(), 
                Context.PERSIST), LAYERINFO_DIRECTORY);
    }
    
    DefaultTileLayerCatalog(GeoServerResourceLoader resourceLoader, XStream configuredXstream)
            throws IOException {
        this(resourceLoader, configuredXstream, LAYERINFO_DIRECTORY);
    }
    

    DefaultTileLayerCatalog(GeoServerResourceLoader resourceLoader, XStream configuredXstream, String baseDirectory)
            throws IOException {

        this.resourceLoader = resourceLoader;
        this.serializer = configuredXstream;
        this.baseDirectory = baseDirectory;

        BiMap<String, String> baseBiMap = HashBiMap.create();
        this.layersById = Maps.synchronizedBiMap(baseBiMap);
        this.layersByName = layersById.inverse();
        this.initialized = false;
    }

    @Override
    public void reset() {
        layersById.clear();
        this.initialized = false;
    }

    @Override
    public void initialize() {

        layersById.clear();

        File baseDir;
        try {
            baseDir = resourceLoader.findOrCreateDirectory(baseDirectory);
        } catch (IOException e) {
            throw propagate(e);
        }

        LOGGER.info("GeoServer TileLayer store base directory is: " + baseDir.getAbsolutePath());

        final String[] tileLayerFiles = baseDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        LOGGER.info("Loading tile layers from " + baseDir.getAbsolutePath());
        for (String fileName : tileLayerFiles) {
            GeoServerTileLayerInfoImpl info;
            try {
                File file = new File(baseDir, fileName);
                info = depersist(file);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error depersisting tile layer information from file "
                        + fileName, e);
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
                File file = getFile(tileLayerId, false);
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
        File file = getFile(tileLayerId, false);
        boolean cleanup = false;
        if (file == null) {
            cleanup = true;
            file = getFile(tileLayerId, true);
        }
        final File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            final Writer writer = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
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
        final File file = getFile(tileLayerId, false);
        if (null == file) {
            throw new FileNotFoundException(tileLayerId);
        }
        return depersist(file);
    }

    private File getFile(final String tileLayerId, final boolean create) throws IOException {
        final String fileName = FilePathUtils.filteredLayerName(tileLayerId) + ".xml";

        final File base = resourceLoader.findOrCreateDirectory(baseDirectory);

        File file = resourceLoader.find(base, fileName);
        if (null == file && create) {
            return resourceLoader.createFile(base, fileName);
        }

        return file;
    }

    private GeoServerTileLayerInfoImpl depersist(final File file) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Depersisting GeoServerTileLayerInfo from " + file.getAbsolutePath());
        }
        GeoServerTileLayerInfoImpl info;
        Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        try {
            info = (GeoServerTileLayerInfoImpl) serializer.fromXML(reader);
        } finally {
            reader.close();
        }

        return info;
    }

    private void rename(File source, File dest) throws IOException {
        // same path? Do nothing
        if (source.getCanonicalPath().equalsIgnoreCase(dest.getCanonicalPath()))
            return;

        // different path
        boolean win = System.getProperty("os.name").startsWith("Windows");
        if (win && dest.exists()) {
            // windows does not do atomic renames, and can not rename a file if the dest file
            // exists
            if (!dest.delete()) {
                throw new IOException("Could not delete: " + dest.getCanonicalPath());
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
