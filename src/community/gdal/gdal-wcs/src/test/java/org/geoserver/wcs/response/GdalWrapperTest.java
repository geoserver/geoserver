/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wcs.response;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.Assume;
import org.junit.Test;

public class GdalWrapperTest {

    private GdalWrapper gdal() {
        Assume.assumeTrue(GdalTestUtil.isGdalAvailable());
        return new GdalWrapper(GdalTestUtil.getGdalTranslate(), GdalTestUtil.getGdalData());
    }

    @Test
    public void testAvaialable() {
        assertTrue(gdal().isAvailable());
    }

    @Test
    public void testFormats() {
        Set<String> formats = gdal().getSupportedFormats();
        assertTrue(formats.contains("GTiff"));
        assertTrue(formats.contains("PNG"));
        assertTrue(formats.contains("JPEG"));
        assertTrue(formats.contains("PDF"));
        assertTrue(formats.contains("AAIGrid"));
    }

    @Test
    public void testMissingExecutable() {
        GdalWrapper missing = new GdalWrapper("gdal_translate_missing", null);
        assertFalse(missing.isAvailable());
        assertTrue(missing.getSupportedFormats().isEmpty());
    }

    @Test
    public void testParseFormatsGdal34() throws IOException {
        assertFormats(GdalWrapper.parseFormats(read("gdal-3.4-formats.txt")));
    }

    @Test
    public void testParseFormatsGdal312() throws IOException {
        assertFormats(GdalWrapper.parseFormats(read("gdal-3.12-formats.txt")));
    }

    @Test
    public void testParseFormatsGdal313() throws IOException {
        assertFormats(GdalWrapper.parseFormats(read("gdal-3.13-formats.txt")));
    }

    @Test
    public void testParseFormatsEmpty() {
        assertTrue(GdalWrapper.parseFormats("").isEmpty());
        assertTrue(GdalWrapper.parseFormats("Usage: gdal_translate [--help]").isEmpty());
    }

    private void assertFormats(Set<String> formats) {
        assertTrue(formats.contains("GTiff"));
        assertTrue(formats.contains("COG"));
        assertTrue(formats.contains("VRT"));
        assertTrue(formats.contains("PNG"));
        assertTrue(formats.contains("JPEG"));
        assertTrue(formats.contains("PDF"));
        assertTrue(formats.contains("AAIGrid"));
        assertTrue(formats.contains("XYZ"));
        assertTrue(formats.contains("JP2OpenJPEG"));
        assertTrue(formats.contains("netCDF"));
        assertFalse(formats.contains("DERIVED"));
    }

    private String read(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
