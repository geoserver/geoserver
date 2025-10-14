/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.servlet.ServletContext;
import org.easymock.EasyMock;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link GeoServerResourceLoader}. */
public class GeoServerResourceLoaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /** Test {@link GeoServerResourceLoader#requireFile(String, String)} for a single file that exists. */
    @Test
    public void testRequireSingleExistingFile() {
        GeoServerResourceLoader.requireFile("pom.xml", "Test fixture");
    }

    /** Test {@link GeoServerResourceLoader#requireFile(String, String)} for two files that exist. */
    @Test
    public void testRequireTwoExistingFiles() {
        GeoServerResourceLoader.requireFile("pom.xml" + File.pathSeparator + "src", "Test fixture");
    }

    /** Test {@link GeoServerResourceLoader#requireFile(String, String)} for a single file that does not exist. */
    @Test
    public void testRequireSingleMissingFile() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GeoServerResourceLoader.requireFile("does-not-exist", "Test fixture"));
    }

    /** Test {@link GeoServerResourceLoader#requireFile(String, String)} for two files where one does not exist. */
    @Test
    public void testRequireSingleMissingFileOfTwo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GeoServerResourceLoader.requireFile(
                        "pom.xml" + File.pathSeparator + "does-not-exist", "Test fixture"));
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a single required file
     * that exists specified in the Java environment.
     */
    @Test
    public void testLookupRequireExistingFileJava() {
        Assume.assumeThat(System.getenv("GEOSERVER_DATA_DIR"), CoreMatchers.nullValue());
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        System.setProperty("GEOSERVER_REQUIRE_FILE", "pom.xml");
        System.clearProperty("GEOSERVER_DATA_DIR");
        try {
            Assert.assertEquals("data", GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a single required file
     * that does not exist specified in the Java environment.
     */
    @Test
    public void testLookupRequireMissingFileJava() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        System.setProperty("GEOSERVER_REQUIRE_FILE", "does-not-exist");
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a single required file
     * that does not exist specified in the servlet context.
     */
    @Test
    public void testLookupRequireMissingFileServlet() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn("does-not-exist");
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        assertThrows(
                IllegalArgumentException.class, () -> GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
    }

    @Test
    public void testSetBaseDirectory() throws IOException {
        GeoServerResourceLoader loader = new GeoServerResourceLoader();
        assertNull(loader.getBaseDirectory());
        assertEquals(ResourceStore.EMPTY, loader.getResourceStore());

        tempFolder.create();
        File tempDir = tempFolder.getRoot();
        loader.setBaseDirectory(tempDir);
        assertEquals(tempDir, loader.getBaseDirectory());
        assertTrue(loader.getResourceStore() instanceof FileSystemResourceStore);

        ResourceStore mockStore = EasyMock.createMock(ResourceStore.class);
        loader = new GeoServerResourceLoader(mockStore);
        assertNull(loader.getBaseDirectory());
        assertEquals(mockStore, loader.getResourceStore());

        loader.setBaseDirectory(tempDir);
        assertEquals(tempDir, loader.getBaseDirectory());
        assertEquals(mockStore, loader.getResourceStore());
    }

    @Test
    public void fromRelativeURLTest() throws IOException {
        GeoServerResourceLoader loader = new GeoServerResourceLoader();
        tempFolder.create();
        File tempDir = tempFolder.getRoot();
        loader.setBaseDirectory(tempDir);

        Resource res = loader.fromURL("file:relative/with/special+characters%23%C3%BC");
        assertEquals("relative/with/special characters#ü", res.path());

        // test it is writeable
        try (OutputStream out = res.out()) {
            out.write("someText".getBytes());
        }

        assertEquals(
                "someText",
                Files.asCharSource(res.file(), Charset.defaultCharset()).read());
    }
}
