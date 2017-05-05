/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.turbojpeg;

import com.sun.media.jai.operator.ImageReadDescriptor;
import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import it.geosolutions.jaiext.JAIExt;
import org.geotools.image.ImageWorker;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import org.geotools.resources.image.ImageUtilities;


public class TurboImageWorkerWithJaiExtTest extends Assert {

    static final String ERROR_LIB_MESSAGE = "The TurboJpeg native library or native mlib hasn't been loaded: Skipping test";

    static boolean SKIP_TESTS = false;

    static final Logger LOGGER = Logger.getLogger(TurboImageWorkerTest.class.toString());

    @BeforeClass
    public static void setup() {
        SKIP_TESTS = !TurboJpegUtilities.isTurboJpegAvailable() || !ImageUtilities.isMediaLibAvailable();
    }

    @BeforeClass
    public static void setupJaiExt() {
        JAIExt.initJAIEXT(true);
    }

    @AfterClass
    public static void teardownJaiExt() {
        JAIExt.initJAIEXT(false);
    }

    private static final String BAND_SEQUENTIAL_DATA_TIF = "band_sequential_data.tif";

    // rasterRenderingBug, seems there is a trouble when removing alpha with jai-ext band select operation.
    // in order the test to work (i.e. to fail), "testWithNativeJaiExt" profile has to be activated.
    // this profile redefine maven surefire plugin, adding:
    //   native jai in classpath:
    //     /usr/share/java/mlibwrapper_jai.jar
    //     /usr/share/java/jai_codec.jar
    //     /usr/share/java/jai_core.jar
    //     /usr/share/java/clibwrapper_jiio.jar
    //     /usr/share/java/jai_imageio.jar
    //   path to turbo jpeg and native jai shared libs as command line options:
    //     -Djava.library.path=/opt/libjpeg-turbo/lib64:/usr/lib/jni
    //

    @Test
    public void writeFromTiffWorksWithJaiExt() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        File tempTurboJpegJaiExt = fileFromName("turbo_jpeg_jaiExt.jpg");

        File inputTiff =  TestData.file(this, BAND_SEQUENTIAL_DATA_TIF);
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(0, 0, 10000, 10000));
        TIFFImageReader readerTiff = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        RenderedImage imageTiff = ImageReadDescriptor.create(new FileImageInputStream(inputTiff),
                Integer.valueOf(0), false, false, false, null, null, null,
                readerTiff, null);

        writeTurboJpeg(imageTiff, tempTurboJpegJaiExt);

        assertTrue((isRightBorderBlackInTheMiddle(tempTurboJpegJaiExt)));
    }

    private void writeTurboJpeg(RenderedImage image, File file) throws IOException {
        new TurboJpegImageWorker(image).writeTurboJPEG(new FileOutputStream(file),.75f);
        new ImageWorker(file).getBufferedImage().flush();
    }

    private static int NO_DATA = -16777216;

    /*  Did notice that with both jai ext (with native jai) and turbo jpgeg, band_sequential_data.tif rendering
        fails: a kind of dilation along x axis occur, cf. turbo_jpeg_jaiExt.jpg. Looking for NO_DATA in the middle of
        right border may help detect trouble.
    */
    public boolean isRightBorderBlackInTheMiddle(File fileA) throws IOException {
        BufferedImage biA = ImageIO.read(fileA);
        int x = 2025;
        for (int y = 1000; y <= 1200; y = y + 50) {
            if (biA.getRGB(x,y) != NO_DATA) {
                return false;
            }
        }
        return true;
    }

    private File fileFromName(String name) throws IOException {
        //return (new File ("/tmp/" + name));
        return TestData.temp(this, name);
    }
}
