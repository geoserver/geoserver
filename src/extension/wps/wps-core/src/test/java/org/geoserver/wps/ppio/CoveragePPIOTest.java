/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.ppio.CoveragePPIO.JPEGPPIO;
import org.geoserver.wps.ppio.CoveragePPIO.PNGPPIO;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.util.ImageUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Testing CoveragePPIOs is able to encode PNG and JPEG formats. */
public class CoveragePPIOTest {

    File geotiff = new File("./target/testInput.tiff");

    File targetPng = new File("./target/output.png");

    File targetJpeg = new File("./target/output.jpeg");

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
        return new GridCoverageFactory()
                .create(
                        coverage.getName(),
                        coverage.getRenderedImage(),
                        coverage.getEnvelope(),
                        coverage.getSampleDimensions(),
                        null,
                        null);
    }

    @Test
    public void testPNGEncode() throws Exception {
        GridCoverage2D coverage = getCoverage();
        PNGPPIO ppio = new PNGPPIO();
        testIsFormat(coverage, ppio, targetPng, "PNG");
    }

    @Test
    public void testJPEGEncode() throws Exception {
        GridCoverage2D coverage = getCoverage();
        JPEGPPIO ppio = new JPEGPPIO();
        testIsFormat(coverage, ppio, targetJpeg, "JPEG");
    }

    @Test
    public void testEncodeQuality() throws Exception {
        GridCoverage2D coverage = getCoverage();
        JPEGPPIO ppio = new JPEGPPIO();
        Map<String, Object> encodingParams = new HashMap<String, Object>();

        File highQualityFile = new File("./target/outputHiQ.jpg");
        encodingParams.put(CoveragePPIO.QUALITY_KEY, "0.99");
        try (FileOutputStream fos = new FileOutputStream(highQualityFile)) {
            ppio.encode(coverage, encodingParams, fos);
        }
        final long highQualityFileSize = highQualityFile.length();

        File lowQualityFile = new File("./target/outputLoQ.jpg");
        encodingParams.put(CoveragePPIO.QUALITY_KEY, "0.01");
        try (FileOutputStream fos = new FileOutputStream(lowQualityFile)) {
            ppio.encode(coverage, encodingParams, fos);
        }
        final long lowQualityFileSize = lowQualityFile.length();
        assertTrue(highQualityFileSize > lowQualityFileSize);
    }

    private void testIsFormat(
            GridCoverage2D coverage, CoveragePPIO ppio, File encodedFile, String formatName)
            throws Exception {
        try (FileOutputStream fos = new FileOutputStream(encodedFile)) {
            ppio.encode(coverage, fos);
        }
        try (FileImageInputStream fis = new FileImageInputStream(encodedFile)) {
            ImageReader imageReader = null;
            try {
                imageReader = ImageIO.getImageReaders(fis).next();
                imageReader.setInput(fis);
                assertTrue(formatName.equalsIgnoreCase(imageReader.getFormatName()));
                assertNotNull(imageReader.read(0));
            } finally {
                if (imageReader != null) {
                    try {
                        imageReader.dispose();
                    } catch (Throwable t) {
                        // Ignore it.
                    }
                }
            }
        }
    }
}
