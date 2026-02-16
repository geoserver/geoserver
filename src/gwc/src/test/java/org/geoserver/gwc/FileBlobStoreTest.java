/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.logging.Logging;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * This class tests the functionalities of the {@link BlobStore} class.
 *
 * @author Nicola Lagomarsini Geosolutions
 */
public class FileBlobStoreTest extends GeoServerSystemTestSupport {

    /** {@link Logger} used for reporting exceptions */
    private static final Logger LOGGER = Logging.getLogger(FileBlobStoreTest.class);

    /** Name of the test directory */
    public static final String TEST_BLOB_DIR_NAME = "gwcTestBlobs";

    public static final String LAYER_NAME = "test:123123 112";

    private BlobStore blobStore;

    /** Directory containing files for the {@link FileBlobStore} */
    private File directory;

    @Before
    public void setup() throws IOException {
        // Setup the fileBlobStore
        File dataDirectoryRoot = getTestData().getDataDirectoryRoot();

        directory = new File(dataDirectoryRoot, "testConfigurableBlobStore");
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
        directory.mkdirs();

        blobStore = Mockito.spy(new FileBlobStore(directory.getAbsolutePath()));
    }

    @After
    public void after() throws IOException {
        // Delete the created directory
        blobStore.destroy();
        if (directory.exists()) {
            // use deleteQuietly, because it could get concurrent with the GWC own cleanup threads,
            // and end up trying to remove a sub-directory that's already gone
            FileUtils.deleteQuietly(directory);
        }
    }

    @Test
    public void testTileDelete() throws Exception {

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {5L, 6L, 7L};
        TileObject to =
                TileObject.createCompleteTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);
        // Try to get the Tile Object
        TileObject to2 = TileObject.createQueryTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.get(to2);

        // Check if the resources are equals
        try (InputStream is = to2.getBlob().getInputStream();
                InputStream is2 = bytes.getInputStream()) {
            checkInputStreams(is, is2);
        }

        // Remove TileObject
        TileObject to3 = TileObject.createQueryTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.delete(to3);

        // Ensure TileObject is no more present
        TileObject to4 = TileObject.createQueryTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to4));
    }

    @Test
    public void testTileDeleteByParameters() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {5L, 6L, 7L};
        TileObject to =
                TileObject.createCompleteTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);

        // Remove tile objects by parameters
        blobStore.deleteByParameters(LAYER_NAME, parameters);

        // Ensure TileObject is no more present
        TileObject to4 = TileObject.createQueryTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to4));
    }

    @Test
    public void testTileDeleteByParametersId() throws Exception {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {5L, 6L, 7L};
        TileObject to =
                TileObject.createCompleteTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);

        // get the parameters is
        String parametersId =
                blobStore.getParametersMapping(LAYER_NAME).keySet().iterator().next();

        // Remove tile objects by parameters
        blobStore.deleteByParametersId(LAYER_NAME, parametersId);

        // Ensure TileObject is no more present
        TileObject to4 = TileObject.createQueryTileObject(LAYER_NAME, xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to4));
    }

    @Test
    public void testListeners() throws Exception {
        BlobStoreListener l1 = Mockito.mock(BlobStoreListener.class);
        BlobStoreListener l2 = Mockito.mock(BlobStoreListener.class);
        blobStore.addListener(l1);
        blobStore.addListener(l2);
        Mockito.verify(blobStore, Mockito.times(2)).addListener(Mockito.any(BlobStoreListener.class));
    }

    /** Checks if the streams are equals, note that the {@link InputStream}s are also closed. */
    private void checkInputStreams(InputStream is, InputStream is2) throws IOException {
        try (is;
                is2) {
            assertTrue(IOUtils.contentEquals(is, is2));
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                fail();
            }
            try {
                is2.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                fail();
            }
        }
    }
}
