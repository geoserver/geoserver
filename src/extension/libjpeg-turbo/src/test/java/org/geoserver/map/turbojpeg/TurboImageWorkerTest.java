/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import org.geotools.image.ImageWorker;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testing directly the {@link org.geoserver.map.turbojpeg.TurboJpegImageWorker}.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class TurboImageWorkerTest extends Assert {

    static final String ERROR_LIB_MESSAGE =
            "The TurboJpeg native library hasn't been loaded: Skipping test";

    static boolean SKIP_TESTS = false;

    static final Logger LOGGER = Logger.getLogger(TurboImageWorkerTest.class.toString());

    @BeforeClass
    public static void setup() {
        SKIP_TESTS = !TurboJpegUtilities.isTurboJpegAvailable();
    }

    @Test
    public void errors() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        try {
            new TurboJpegImageWorker(ImageIO.read(input))
                    .writeTurboJPEG(new FileOutputStream(output), 1.5f);
            assertFalse("We should not be allowed to specify compression ratios > 1", true);
        } catch (Exception e) {
            // TODO: handle exception
        }

        try {
            new TurboJpegImageWorker(ImageIO.read(input))
                    .writeTurboJPEG(new FileOutputStream(output), -.5f);
            assertFalse("We should not be allowed to specify compression ratios > 1", true);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Test
    public void writeIndexedWithAlpha() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // Create paletted image
        final byte bb[] = new byte[256];
        for (int i = 0; i < 200; i++) {
            bb[i] = (byte) i;
        }
        int noDataValue = 200;
        NoDataContainer noData = new NoDataContainer(noDataValue);
        final IndexColorModel icm = new IndexColorModel(8, 256, bb, bb, bb);
        final WritableRaster raster =
                RasterFactory.createWritableRaster(icm.createCompatibleSampleModel(512, 512), null);
        for (int i = raster.getMinX(); i < raster.getMinX() + raster.getWidth(); i++) {
            for (int j = raster.getMinY(); j < raster.getMinY() + raster.getHeight(); j++) {
                if (i - raster.getMinX() < raster.getWidth() / 2) {
                    raster.setSample(i, j, 0, (i + j) / 32);
                } else {
                    raster.setSample(i, j, 0, 200);
                }
            }
        }
        // Set no data
        BufferedImage bi = new BufferedImage(icm, raster, false, null);
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(bi);
        planarImage.setProperty(NoDataContainer.GC_NODATA, noData);

        // create output file
        final File output = TestData.temp(this, "outputNoAlpha.jpeg");
        new TurboJpegImageWorker(planarImage).writeTurboJPEG(new FileOutputStream(output), .5f);
        // No exceptions occurred so far.
        assertTrue("Unable to create output file", output.exists() && output.isFile());
    }

    @Test
    public void writer() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input))
                .writeTurboJPEG(new FileOutputStream(output), .5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());

        new ImageWorker(output).getBufferedImage().flush();
    }

    @Test
    public void testWriterBandSelect() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergba.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input))
                .writeTurboJPEG(new FileOutputStream(output), .5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());

        new ImageWorker(output).getBufferedImage().flush();
    }
}
