/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.geoserver.wcs.response.GdalTestUtil.TEST_RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.ogr.core.Format;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class GdalFormatTest {

    GdalCoverageResponseDelegate gdalCovRespDelegate;

    @Before
    public void setUp() throws Exception {
        // check if we can run the tests
        Assume.assumeTrue(GdalTestUtil.isGdalAvailable());

        // the coverage response delegate
        gdalCovRespDelegate =
                new GdalCoverageResponseDelegate(new GeoServerImpl(), new GdalWrapperFactory());
        // add default formats
        for (Format format : GdalConfigurator.DEFAULT.getFormats()) {
            gdalCovRespDelegate.addFormat(format);
        }

        gdalCovRespDelegate.setExecutable(GdalTestUtil.getGdalTranslate());
        gdalCovRespDelegate.setEnvironment(GdalTestUtil.getGdalData());
    }

    @Test
    public void testCanProduce() {
        assertTrue(gdalCovRespDelegate.canProduce("GDAL-JPEG2000"));
        assertTrue(gdalCovRespDelegate.canProduce("GDAL-XYZ"));
        // not among default formats
        assertFalse(gdalCovRespDelegate.canProduce("GDAL-MrSID"));
    }

    @Test
    public void testContentTypeZip() {
        assertEquals("application/zip", gdalCovRespDelegate.getMimeType("GDAL-ArcInfoGrid"));
        assertEquals("zip", gdalCovRespDelegate.getFileExtension("GDAL-ArcInfoGrid"));
    }

    @Test
    public void testContentTypeJP2K() {
        assertEquals("image/jp2", gdalCovRespDelegate.getMimeType("GDAL-JPEG2000"));
        assertEquals("jp2", gdalCovRespDelegate.getFileExtension("GDAL-JPEG2000"));
    }

    @Test
    public void testContentTypePDF() {
        assertEquals("application/pdf", gdalCovRespDelegate.getMimeType("GDAL-PDF"));
        assertEquals("pdf", gdalCovRespDelegate.getFileExtension("GDAL-PDF"));
    }

    @Test
    public void testContentTypeText() {
        assertEquals("text/plain", gdalCovRespDelegate.getMimeType("GDAL-XYZ"));
        assertEquals("txt", gdalCovRespDelegate.getFileExtension("GDAL-XYZ"));
    }

    @Test
    public void testXYZ() throws Exception {
        // prepare input
        File tempFile = prepareInput();
        try {
            GridCoverage2DReader covReader = new GeoTiffReader(tempFile);
            GridCoverage2D cov = covReader.read(null);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                // write out
                gdalCovRespDelegate.encode(cov, "GDAL-XYZ", null, bos);

                // parse the text output to check it's really XYZ data
                try (InputStream is = new ByteArrayInputStream(bos.toByteArray())) {
                    GdalTestUtil.checkXyzData(is);
                }
            }
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @Test
    public void testZippedGrid() throws Exception {
        // prepare input
        File tempFile = prepareInput();
        try {
            GridCoverage2DReader covReader = new GeoTiffReader(tempFile);
            GridCoverage2D cov = covReader.read(null);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                // write out
                gdalCovRespDelegate.encode(cov, "GDAL-ArcInfoGrid", null, bos);

                GdalTestUtil.checkZippedGridData(new ByteArrayInputStream(bos.toByteArray()));
            }
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private File prepareInput() throws IOException {
        File tempFile = File.createTempFile("gdal_wcs_", "_test_data");
        IOUtils.copy(getClass().getResourceAsStream(TEST_RESOURCE), new FileOutputStream(tempFile));

        return tempFile;
    }
}
