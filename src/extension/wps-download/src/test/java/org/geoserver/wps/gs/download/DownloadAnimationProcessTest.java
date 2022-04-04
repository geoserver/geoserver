/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.image.test.ImageAssert;
import org.hamcrest.CoreMatchers;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.scale.AWTUtil;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DownloadAnimationProcessTest extends BaseDownloadImageProcessTest {

    public interface ThrowingBiConsumer<T, U> {

        void accept(T t, U u) throws Exception;
    }

    @Override
    protected String getLogConfiguration() {
        if (isQuietTests()) {
            return "/QUIET_LOGGING.properties";
        }
        return "/DEFAULT_LOGGING.properties";
    }

    @Test
    public void testDescribeProcess() throws Exception {
        Document d =
                getAsDOM(
                        root()
                                + "service=wps&request=describeprocess&identifier=gs:DownloadAnimation");
        // print(d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='video/mp4']", d);
    }

    @Test
    public void testAnimateBmTime() throws Exception {
        String xml =
                IOUtils.toString(getClass().getResourceAsStream("animateBlueMarble.xml"), UTF_8);
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertAnimationMonths2345(response, this::assertDefaultFrames);
    }

    private void assertAnimationMonths2345(
            MockHttpServletResponse response, ThrowingBiConsumer<File, FrameGrab> assertor)
            throws Exception {
        assertEquals(200, response.getStatus());
        assertEquals("video/mp4", response.getContentType());

        // JCodec API works off files only...
        File testFile = new File("target/animateBmTime.mp4");
        FileUtils.writeByteArrayToFile(testFile, response.getContentAsByteArray());

        // check frames and duration
        Format f = JCodecUtil.detectFormat(testFile);
        try (Demuxer d = JCodecUtil.createDemuxer(f, testFile)) {
            DemuxerTrack vt = d.getVideoTracks().get(0);
            DemuxerTrackMeta dtm = vt.getMeta();
            assertEquals(4, dtm.getTotalFrames());
            assertEquals(8, dtm.getTotalDuration(), 0d);

            // grab frames for checking
            File source = new File("src/test/resources/org/geoserver/wps/gs/download/bm_time.zip");
            try (FileChannelWrapper fc = NIOUtils.readableChannel(testFile)) {
                FrameGrab grabber = FrameGrab.createFrameGrab(fc);
                assertor.accept(source, grabber);
            }
        }
    }

    private void assertDefaultFrames(File source, FrameGrab grabber) throws IOException {
        // first
        BufferedImage frame1 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected1 = getImageFromZip(source, "world.200402.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected1, frame1, 100);
        // second
        BufferedImage frame2 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected2 = getImageFromZip(source, "world.200403.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected2, frame2, 100);
        // third
        BufferedImage frame3 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected3 = getImageFromZip(source, "world.200404.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected3, frame3, 100);
        // fourth
        BufferedImage frame4 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected4 = getImageFromZip(source, "world.200405.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected4, frame4, 100);
    }

    @Test
    public void testAnimateBmTimeMetadata() throws Exception {
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateBlueMarbleMetadata.xml"), UTF_8);
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("application/xml", response.getContentType());
        Document dom = dom(response, true);
        print(dom);

        // check the animation is produced as normal, de-referencing the link
        String fullLocation =
                XMLUnit.newXpathEngine()
                        .evaluate("//wps:Output[ows:Identifier='result']/wps:Reference/@href", dom);
        String testLocation = getTestReference(fullLocation);
        assertAnimationMonths2345(getAsServletResponse(testLocation), this::assertDefaultFrames);

        // the metadata is in-line and should be
        assertXpathExists(
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/Warnings",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "count(//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/Warnings/*)",
                dom);
        assertXpathEvaluatesTo(
                "false",
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/WarningsFound",
                dom);
    }

    @Test
    public void testAnimateBmTimeMetadataWarnings() throws Exception {
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateBlueMarbleMetadataWarnings.xml"),
                        UTF_8);
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("application/xml", response.getContentType());
        Document dom = dom(response, true);
        print(dom);

        // check the animation is produced as normal, de-referencing the link
        String fullLocation =
                XMLUnit.newXpathEngine()
                        .evaluate("//wps:Output[ows:Identifier='result']/wps:Reference/@href", dom);
        String testLocation = getTestReference(fullLocation);
        assertAnimationMonths2345(
                getAsServletResponse(testLocation),
                (zip, grabber) -> {
                    // first
                    BufferedImage frame1 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
                    BufferedImage expected1 = getImageFromZip(zip, "world.200402.3x5400x2700.tiff");
                    ImageAssert.assertEquals(expected1, frame1, 100);
                    // second is a missed match, should be empty
                    BufferedImage frame2 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
                    assertAlmostBlank(frame2);
                    // third
                    BufferedImage frame3 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
                    BufferedImage expected3 = getImageFromZip(zip, "world.200404.3x5400x2700.tiff");
                    ImageAssert.assertEquals(expected3, frame3, 100);
                    // fourth
                    BufferedImage frame4 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
                    BufferedImage expected4 = getImageFromZip(zip, "world.200405.3x5400x2700.tiff");
                    ImageAssert.assertEquals(expected4, frame4, 100);
                });

        // the metadata is in-line and should be
        assertXpathExists(
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/Warnings",
                dom);
        assertXpathEvaluatesTo(
                "4",
                "count(//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/Warnings/*)",
                dom);
        String prefix =
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/Warnings/FrameWarning";
        // first warning, nearest
        assertXpathEvaluatesTo("sf:bmtime", prefix + "[1]/LayerName", dom);
        assertXpathEvaluatesTo("time", prefix + "[1]/DimensionName", dom);
        assertXpathEvaluatesTo("2004-02-01T00:00:00.000Z", prefix + "[1]/Value", dom);
        assertXpathEvaluatesTo("Nearest", prefix + "[1]/WarningType", dom);
        assertXpathEvaluatesTo("0", prefix + "[1]/Frame", dom);
        // second warning
        assertXpathEvaluatesTo("sf:bmtime", prefix + "[2]/LayerName", dom);
        assertXpathEvaluatesTo("time", prefix + "[2]/DimensionName", dom);
        assertXpathEvaluatesTo("FailedNearest", prefix + "[2]/WarningType", dom);
        assertXpathEvaluatesTo("1", prefix + "[2]/Frame", dom);
        // flag about warnings
        assertXpathEvaluatesTo(
                "true",
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/AnimationMetadata/WarningsFound",
                dom);
    }

    /**
     * Checks for an empty frame, accounting for lossy compression (so not all colors will be
     * white). Uses a very rough manhattan distance in RGB, the output was also checked manually,
     * there is a number of darker round spots in it)
     *
     * @param image
     */
    private void assertAlmostBlank(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color rgb = new Color(image.getRGB(x, y));
                double d =
                        Math.abs(rgb.getRed() - 255)
                                + Math.abs(rgb.getGreen() - 255)
                                + Math.abs(rgb.getBlue() - 255);
                assertTrue(d < 170);
            }
        }
    }

    @Test
    public void testAnimateFrameLimits() throws Exception {
        // set a limit of 1 frame
        final DownloadServiceConfigurationWatcher watcher =
                GeoServerExtensions.bean(DownloadServiceConfigurationWatcher.class);
        watcher.getConfiguration().setMaxAnimationFrames(1);

        try {
            String xml =
                    IOUtils.toString(
                            getClass().getResourceAsStream("animateBlueMarble.xml"), UTF_8);
            Document dom = postAsDOM("wps", xml);
            // print(dom);
            XMLAssert.assertXpathExists("//wps:ProcessFailed", dom);
            String message = XMLUnit.newXpathEngine().evaluate("//ows:ExceptionText", dom);
            assertThat(
                    message,
                    CoreMatchers.containsString("More than 1 times specified in the request"));
        } finally {
            Resource config = getDataDirectory().get("download.properties");
            assertTrue("Failed to remove download configuration file", config.delete());
            // force reset of default configuration
            watcher.loadConfiguration();
        }
    }

    @Test
    public void testAnimateDecoration() throws Exception {
        String xml =
                IOUtils.toString(getClass().getResourceAsStream("animateDecoration.xml"), UTF_8);
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());

        checkAnimation(response, 2, 2, "animateDecorateFirstFrame.png");
    }

    @Test
    public void testAnimateTimestamped() throws Exception {
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateBlueMarbleTimestamped.xml"), UTF_8);
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());

        // JCodec API works off files only...
        checkAnimation(response, 4, 8, "animateBlueMarbleTimestampedFrame1.png");
    }

    @Test
    public void testDynamicDecorationDefault() throws Exception {
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateDynamicDecoration.xml"), UTF_8);

        // as is, should be the same as testAnimateDecoration
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());
        checkAnimation(response, 2, 2, "animateDecorateFirstFrame.png");
    }

    @Test
    public void testDynamicDecorationLargeLogo() throws Exception {
        String xmlTemplate =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateDynamicDecoration.xml"), UTF_8);

        // change the logo size and run again
        String xml = xmlTemplate.replace("${env}", "size:64,64");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());
        checkAnimation(response, 2, 2, "animateDecorateLargeLogo.png");
    }

    @Test
    public void testDynamicDecorationOsgeoLogo() throws Exception {
        String xmlTemplate =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateDynamicDecoration.xml"), UTF_8);

        // change the logo and its size
        String xml = xmlTemplate.replace("${env}", "size:64,64;logo:osgeo.png");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());
        checkAnimation(response, 2, 2, "animateDecorateOsgeoLogo.png");
    }

    @Test
    public void testDynamicDecorationLayer() throws Exception {
        String xmlTemplate =
                IOUtils.toString(
                        getClass().getResourceAsStream("animateLayerDynamicDecoration.xml"), UTF_8);

        // change the logo and its size
        String xml = xmlTemplate.replace("${env}", "size:64,64;logo:osgeo.png");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());
        checkAnimation(response, 2, 2, "animateDecorateOsgeoLogo.png");
    }

    private void checkAnimation(
            MockHttpServletResponse response, int totalFrames, int totalDuration, String firstFrame)
            throws IOException, JCodecException {
        // JCodec API works off files only...
        File testFile = new File("target/test.mp4");
        FileUtils.writeByteArrayToFile(testFile, response.getContentAsByteArray());

        // check frames and duration
        Format f = JCodecUtil.detectFormat(testFile);
        try (Demuxer d = JCodecUtil.createDemuxer(f, testFile)) {
            DemuxerTrack vt = d.getVideoTracks().get(0);
            DemuxerTrackMeta dtm = vt.getMeta();
            assertEquals(totalFrames, dtm.getTotalFrames());
            assertEquals(totalDuration, dtm.getTotalDuration(), 0d);

            // grab first frame for test
            FrameGrab grabber = FrameGrab.createFrameGrab(NIOUtils.readableChannel(testFile));
            BufferedImage frame1 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
            ImageAssert.assertEquals(new File(SAMPLES + firstFrame), frame1, 100);
        }
    }

    BufferedImage getImageFromZip(File file, String entryName) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase(entryName)) {
                    try (InputStream stream = zipFile.getInputStream(entry)) {
                        return ImageIO.read(stream);
                    }
                }
            }

            return null;
        }
    }
}
