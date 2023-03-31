/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config;

import static org.geoserver.platform.resource.Resource.Type.DIRECTORY;
import static org.geoserver.platform.resource.Resource.Type.RESOURCE;
import static org.geoserver.platform.resource.Resource.Type.UNDEFINED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpringResourceAdaptorTest {

    Resource directoryResource;
    Resource missingResource;
    Resource existingResource;

    static final String CONTENT = "content";

    @Before
    public void setUp() throws Exception {

        FileSystemResourceStore target = new FileSystemResourceStore(new File("target"));

        missingResource = target.get("missingFile");
        assertFalse(Resources.exists(missingResource));

        existingResource = target.get("existingFile");
        Resources.createNewFile(existingResource);
        existingResource.setContents(CONTENT.getBytes(StandardCharsets.UTF_8));
        assertTrue(Resources.exists(existingResource));

        directoryResource = existingResource.parent();
        assertTrue(Resources.exists(directoryResource));
    }

    @After
    public void tearDown() throws Exception {
        existingResource.delete();
        missingResource.delete();
    }

    @Test
    public void testExistingResource() throws IOException {

        assertEquals(RESOURCE, existingResource.getType());
        assertTrue(Resources.exists(existingResource));

        SpringResourceAdaptor springResource = new SpringResourceAdaptor(existingResource);

        assertTrue(springResource.isReadable());
        assertEquals(springResource.contentLength(), CONTENT.length());

        assertNotNull(springResource.getFile());

        try (InputStream is = springResource.getInputStream()) {
            assertNotNull(is);
            String content = org.apache.commons.io.IOUtils.toString(is, StandardCharsets.UTF_8);
            assertEquals(content, CONTENT);
        }
    }

    @Test
    public void testDirectoryResource() throws IOException {

        assertEquals(DIRECTORY, directoryResource.getType());
        assertTrue(Resources.exists(directoryResource));

        SpringResourceAdaptor springResource = new SpringResourceAdaptor(directoryResource);

        assertFalse(springResource.isReadable());
        assertEquals(springResource.contentLength(), 0);

        assertNotNull(springResource.getFile());
        assertThrows(FileNotFoundException.class, () -> springResource.getInputStream().close());
    }

    @Test
    public void testMissingResource() {

        assertEquals(UNDEFINED, missingResource.getType());
        assertFalse(Resources.exists(missingResource));

        SpringResourceAdaptor springResource = new SpringResourceAdaptor(missingResource);

        assertFalse(springResource.isReadable());
        assertEquals(springResource.contentLength(), 0);

        assertThrows(FileNotFoundException.class, springResource::getFile);
        assertThrows(FileNotFoundException.class, () -> springResource.getInputStream().close());

        // must not be created unintentionally.
        assertFalse(Resources.exists(missingResource));
    }
}
