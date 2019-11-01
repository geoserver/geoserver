/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.logging.Logging;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.MemoryBlobStore;
import org.geowebcache.storage.blobstore.memory.NullBlobStore;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * This class tests the functionalities of the {@link ConfigurableBlobStore} class.
 *
 * @author Nicola Lagomarsini Geosolutions
 */
public class ConfigurableBlobStoreTest extends GeoServerSystemTestSupport {

    /** {@link Logger} used for reporting exceptions */
    private static final Logger LOGGER = Logging.getLogger(ConfigurableBlobStoreTest.class);

    /** Name of the test directory */
    public static final String TEST_BLOB_DIR_NAME = "gwcTestBlobs";

    public static final String LAYER_NAME = "test:123123 112";

    /** {@link CacheProvider} object used for testing purposes */
    private static CacheProvider cache;

    private BlobStore defaultStore;

    /** {@link ConfigurableBlobStore} object to test */
    private ConfigurableBlobStore blobStore;

    /** Directory containing files for the {@link FileBlobStore} */
    private File directory;

    @BeforeClass
    public static void initialSetup() {
        cache = new GuavaCacheProvider(new CacheConfiguration());
    }

    @Before
    public void setup() throws IOException {
        // Setup the fileBlobStore
        File dataDirectoryRoot = getTestData().getDataDirectoryRoot();

        MemoryBlobStore mbs = new MemoryBlobStore();

        NullBlobStore nbs = new NullBlobStore();

        directory = new File(dataDirectoryRoot, "testConfigurableBlobStore");
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
        directory.mkdirs();

        defaultStore = Mockito.spy(new FileBlobStore(directory.getAbsolutePath()));
        blobStore = new ConfigurableBlobStore(defaultStore, mbs, nbs);
        blobStore.setCache(cache);
    }

    @After
    public void after() throws IOException {
        // Delete the created directory
        blobStore.destroy();
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    @Test
    public void testNullStore() throws Exception {
        // Configure the blobstore
        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(true);
        gwcConfig.setEnabledPersistence(false);
        blobStore.setChanged(gwcConfig, false);

        BlobStore delegate = blobStore.getDelegate();
        assertTrue(delegate instanceof MemoryBlobStore);

        assertTrue(((MemoryBlobStore) delegate).getStore() instanceof NullBlobStore);

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {1L, 2L, 3L};
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to =
                TileObject.createCompleteTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);
        // Try to get the Tile Object
        TileObject to2 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.get(to2);

        // Check formats
        assertEquals(to.getBlobFormat(), to2.getBlobFormat());

        // Check if the resources are equals
        InputStream is = to.getBlob().getInputStream();
        InputStream is2 = to2.getBlob().getInputStream();
        checkInputStreams(is, is2);

        // Ensure Cache contains the result
        TileObject to3 = cache.getTileObj(to);
        assertNotNull(to3);
        assertEquals(to.getBlobFormat(), to3.getBlobFormat());

        // Check if the resources are equals
        is = to.getBlob().getInputStream();
        InputStream is3 = to3.getBlob().getInputStream();
        checkInputStreams(is, is3);

        // Ensure that NullBlobStore does not contain anything
        assertFalse(((MemoryBlobStore) delegate).getStore().get(to));
    }

    @Test
    public void testTilePut() throws Exception {
        // Configure the blobstore
        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(true);
        gwcConfig.setEnabledPersistence(true);
        blobStore.setChanged(gwcConfig, false);

        assertTrue(blobStore.getDelegate() instanceof MemoryBlobStore);

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {1L, 2L, 3L};
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to =
                TileObject.createCompleteTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);
        // Try to get the Tile Object
        TileObject to2 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.get(to2);

        // Check formats
        assertEquals(to.getBlobFormat(), to2.getBlobFormat());

        // Check if the resources are equals
        InputStream is = to.getBlob().getInputStream();
        InputStream is2 = to2.getBlob().getInputStream();
        checkInputStreams(is, is2);

        // Ensure Cache contains the result
        TileObject to3 = cache.getTileObj(to);
        assertNotNull(to3);
        assertEquals(to.getBlobFormat(), to3.getBlobFormat());

        is = to.getBlob().getInputStream();
        InputStream is3 = to3.getBlob().getInputStream();
        checkInputStreams(is, is3);

        // check the layer is known
        assertThat(blobStore.layerExists(LAYER_NAME), equalTo(true));

        // check the parameters can be listed
        Map<String, Optional<Map<String, String>>> parametersMapping =
                blobStore.getParametersMapping(LAYER_NAME);
        assertThat(parametersMapping.size(), equalTo(1));
        Optional<Map<String, String>> value = parametersMapping.values().iterator().next();
        assertThat(value.isPresent(), equalTo(true));
        assertThat(value.get(), hasEntry("a", "x"));
        assertThat(value.get(), hasEntry("b", "ø"));
    }

    @Test
    public void testTileDelete() throws Exception {

        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(false);
        blobStore.setChanged(gwcConfig, false);

        assertTrue(blobStore.getDelegate() instanceof FileBlobStore);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {5L, 6L, 7L};
        TileObject to =
                TileObject.createCompleteTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);
        // Try to get the Tile Object
        TileObject to2 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.get(to2);

        // Check if the resources are equals
        InputStream is = to2.getBlob().getInputStream();
        InputStream is2 = bytes.getInputStream();
        checkInputStreams(is, is2);

        // Remove TileObject
        TileObject to3 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.delete(to3);

        // Ensure TileObject is no more present
        TileObject to4 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to4));
    }

    @Test
    public void testTileDeleteByParameters() throws Exception {

        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(false);
        blobStore.setChanged(gwcConfig, false);

        assertTrue(blobStore.getDelegate() instanceof FileBlobStore);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {5L, 6L, 7L};
        TileObject to =
                TileObject.createCompleteTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);

        // Remove tile objects by parameters
        blobStore.deleteByParameters(LAYER_NAME, parameters);

        // Ensure TileObject is no more present
        TileObject to4 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to4));
    }

    @Test
    public void testTileDeleteByParametersId() throws Exception {

        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(false);
        blobStore.setChanged(gwcConfig, false);

        assertTrue(blobStore.getDelegate() instanceof FileBlobStore);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {5L, 6L, 7L};
        TileObject to =
                TileObject.createCompleteTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);

        // get the parameters is
        String parametersId = blobStore.getParametersMapping(LAYER_NAME).keySet().iterator().next();

        // Remove tile objects by parameters
        blobStore.deleteByParametersId(LAYER_NAME, parametersId);

        // Ensure TileObject is no more present
        TileObject to4 =
                TileObject.createQueryTileObject(
                        LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to4));
    }

    @Test
    public void testListeners() throws Exception {
        // Configure the blobstore
        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(true);
        gwcConfig.setEnabledPersistence(true);
        blobStore.setChanged(gwcConfig, false);

        BlobStoreListener l1 = Mockito.mock(BlobStoreListener.class);
        BlobStoreListener l2 = Mockito.mock(BlobStoreListener.class);

        assertTrue(blobStore.getDelegate() instanceof MemoryBlobStore);

        blobStore.addListener(l1);
        blobStore.addListener(l2);

        Mockito.verify(defaultStore, Mockito.times(2))
                .addListener(Mockito.any(BlobStoreListener.class));
        Mockito.reset(defaultStore);

        // change the configuration
        GWCConfig newConfig = new GWCConfig();
        newConfig.setInnerCachingEnabled(false);
        newConfig.setEnabledPersistence(true);
        blobStore.setChanged(newConfig, false);

        assertFalse(blobStore.getDelegate() instanceof MemoryBlobStore);

        Mockito.verify(defaultStore, Mockito.times(2))
                .removeListener(Mockito.any(BlobStoreListener.class));
        Mockito.verify(defaultStore, Mockito.times(2))
                .addListener(Mockito.any(BlobStoreListener.class));
    }

    /** Checks if the streams are equals, note that the {@link InputStream}s are also closed. */
    private void checkInputStreams(InputStream is, InputStream is2) throws IOException {
        try {
            assertTrue(IOUtils.contentEquals(is, is2));
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                assertTrue(false);
            }
            try {
                is2.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                assertTrue(false);
            }
        }
    }
}
