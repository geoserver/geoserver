/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.process.raster.CropCoverage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GeotiffPPIOTest {

    File geotiff = new File("./target/test.tiff");
    File target = new File("./target/target.tiff");
    GeoTiffReader reader;
    GridCoverage2D coverage;

    @Before
    public void prepareGeoTiff() throws IOException {
        try (InputStream is = SystemTestData.class.getResourceAsStream("tazbm.tiff")) {
            FileUtils.copyInputStreamToFile(is, geotiff);
        }
        reader = new GeoTiffReader(geotiff);
    }

    @After
    public void cleanup() {
        if (coverage != null) {
            ImageUtilities.disposeImage(coverage.getRenderedImage());
        }
        if (reader != null) {
            reader.dispose();
        }
    }

    private GridCoverage2D getCoverage() throws IOException {
        coverage = reader.read(null);
        Map properties = new HashMap<>(coverage.getProperties());
        properties.put(
                AbstractGridCoverage2DReader.FILE_SOURCE_PROPERTY, geotiff.getCanonicalPath());
        return new GridCoverageFactory()
                .create(
                        coverage.getName(),
                        coverage.getRenderedImage(),
                        coverage.getEnvelope(),
                        coverage.getSampleDimensions(),
                        null,
                        properties);
    }

    @Test
    public void testRawCopy() throws Exception {
        GridCoverage2D coverage = getCoverage();
        GeoTiffPPIO ppio = new GeoTiffPPIO();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            ppio.encode(coverage, fos);
        }
        // was a straight copy (a re-encoding would change the size as the input
        // is compressed, the output is not)
        assertEquals(geotiff.length(), target.length());
    }

    @Test
    public void testCropped() throws Exception {
        GridCoverage2D cov = getCoverage();
        ReferencedEnvelope re = ReferencedEnvelope.reference(coverage.getEnvelope2D());
        re.expandBy(-0.1);
        this.coverage = new CropCoverage().execute(coverage, JTS.toGeometry(re), null);
        GeoTiffPPIO ppio = new GeoTiffPPIO();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            ppio.encode(coverage, fos);
        }
        // not a straight copy, size is different
        assertNotEquals(geotiff.length(), target.length());
    }
}
