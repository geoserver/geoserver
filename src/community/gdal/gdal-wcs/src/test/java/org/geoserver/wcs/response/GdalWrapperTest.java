/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wcs.response;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class GdalWrapperTest {

    private GdalWrapper gdal;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(GdalTestUtil.isGdalAvailable());
        gdal = new GdalWrapper(GdalTestUtil.getGdalTranslate(), GdalTestUtil.getGdalData());
    }

    @Test
    public void testAvaialable() {
        // kind of a smoke test, since GdalTestUtils uses the same command!
        gdal.isAvailable();
    }

    @Test
    public void testFormats() {
        Set<String> formats = gdal.getSupportedFormats();
        // well, we can't know which formats GDAL was complied with, but at least there will be one,
        // right?
        assertTrue(formats.size() > 0);

        // these work on my machine, with gdal 1.11.2, libgeotiff 1.4.0, libpng 1.6
        // and libjpeg-turbo 1.3.1
        assertTrue(formats.contains("GTiff"));
        assertTrue(formats.contains("PNG"));
        assertTrue(formats.contains("JPEG"));
        assertTrue(formats.contains("PDF"));
        assertTrue(formats.contains("AAIGrid"));
    }
}
