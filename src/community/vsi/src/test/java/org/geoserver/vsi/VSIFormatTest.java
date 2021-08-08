/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;

/**
 * Tests for VSIFormat class
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSIFormatTest extends GeoServerTestSupport {

    private final VSITestHelper helper = new VSITestHelper();
    private final VSIFormat format = new VSIFormat();

    @Test
    public void testSetInfo() {
        assertEquals(VSIFormat.DESCRIPTION, format.getDescription());
        assertEquals(VSIFormat.NAME, format.getName());
    }

    @Test
    public void testAcceptsNullPath() {
        assertFalse(format.acceptsPath(null));
    }

    @Test
    public void testAcceptsInvalidPath() {
        assertFalse(format.acceptsPath("this is not a valid path!"));
    }

    @Test
    public void testAcceptsValidPath() {
        assertTrue(format.acceptsPath(helper.TIFF_LOCATION));
    }

    @Test
    public void testAcceptsNestedValidPath() {
        assertTrue(format.acceptsPath(helper.ZIP_LOCATION));
    }

    @Test
    public void testAcceptsInvalidType() {
        assertFalse(format.accepts(1337));
    }

    @Test
    public void testAcceptsInvalidString() {
        assertFalse(format.accepts("A valid string but not a valid path!"));
    }

    @Test
    public void testAcceptsValidPathString() {
        assertTrue(format.accepts(helper.TIFF_LOCATION));
    }

    @Test
    public void testAcceptsNestedValidPathString() {
        assertTrue(format.accepts(helper.ZIP_LOCATION));
    }

    @Test
    public void testAcceptsInvalidFile() {
        final File file = new File("/not/a/valid/path");

        assertFalse(format.accepts(file));
    }

    @Test
    public void testAcceptsValidFile() {
        final File file = new File(helper.TIFF_LOCATION);

        assertTrue(format.accepts(file));
    }

    @Test
    public void testAcceptsValidFileWithNestedPath() {
        final File file = new File(helper.ZIP_LOCATION);

        assertTrue(format.accepts(file));
    }

    @Test
    public void testFixPathSingle() {
        final String inputPath = helper.TIFF_LOCATION;
        final String outputPath = format.fixPath(inputPath);

        assertEquals(inputPath, outputPath);
    }

    @Test
    public void testFixPathNested() {
        final String inputPath = "/vsizip/vsiswift/container/file.zip";
        final String outputPath = format.fixPath(inputPath);

        assertEquals(outputPath, "/vsizip//vsiswift/container/file.zip");
    }
}
