/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.kml.KMZMapOutputFormat;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.http.HTTPClient;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DownloadMapProcessTest extends BaseDownloadImageProcessTest {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        super.registerNamespaces(namespaces);
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
    }

    @Test
    public void testDescribeProcess() throws Exception {
        Document d =
                getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:DownloadMap");
        print(d);
        assertXpathEvaluatesTo("0", "//Input[ows:Identifier='transparent']/@minOccurs", d);
        assertXpathEvaluatesTo(
                "xs:boolean", "//Input[ows:Identifier='transparent']/LiteralData/ows:DataType", d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='image/png']", d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='image/jpeg']", d);
        assertXpathExists(
                "//ComplexOutput/Supported/Format[MimeType='" + KMZMapOutputFormat.MIME_TYPE + "']",
                d);
    }

    @Test
    public void testExecuteSingleLayer() throws Exception {
        String xml = getTestRequest("mapSimple.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 100);
    }

    @Test
    public void testExecuteSingleLayerFilter() throws Exception {
        String xml = getTestRequest("mapSimpleFilter.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertEquals(ColorModel.OPAQUE, image.getColorModel().getTransparency());
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimpleFilter.png"), image, 100);
    }

    @Test
    public void testExecuteTransparent() throws Exception {
        String xml = getTestRequest("mapSimpleFilterTransparent.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertEquals(ColorModel.TRANSLUCENT, image.getColorModel().getTransparency());
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimpleFilterTransparent.png"), image, 100);
    }

    @Test
    public void testExecuteSingleDecorated() throws Exception {
        String xml = getTestRequest("mapSimpleDecorated.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "watermarked.png"), image, 100);
    }

    @Test
    public void testExecuteSingleDecoratedTransparent() throws Exception {
        String xml = getTestRequest("mapSimpleDecoratedTransparent.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "watermarkedTx.png"), image, 100);
    }

    private String getTestRequest(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name), UTF_8);
    }

    @Test
    public void testExecuteMultiName() throws Exception {
        String xml = getTestRequest("mapMultiName.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=result.png", response.getHeader("Content-disposition"));
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
    }

    @Test
    public void testExecuteMultiLayer() throws Exception {
        String xml = getTestRequest("mapMultiLayer.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        // not a typo, the output should indeed be the same as testExecuteMultiName
        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
    }

    @Test
    public void testExecuteMultiLayerKmz() throws Exception {
        testExecutMultiLayerKmz(KMZMapOutputFormat.MIME_TYPE);
    }

    @Test
    public void testExecuteMultiLayerKmzShort() throws Exception {
        testExecutMultiLayerKmz("kmz");
    }

    @Test
    public void testExecuteGeotiff() throws Exception {
        String request = getTestRequest("mapMultiLayer.xml");
        request = request.replaceAll("image/png", "image/geotiff");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("image/geotiff", response.getContentType());
        assertEquals("attachment; filename=result.tif", response.getHeader("Content-disposition"));
    }

    protected void testExecutMultiLayerKmz(String mime) throws Exception {
        String request = getTestRequest("mapMultiLayer.xml");
        request = request.replaceAll("image/png", mime);
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals(KMZMapOutputFormat.MIME_TYPE, response.getContentType());
        assertEquals("inline; filename=result.kmz", response.getHeader("Content-disposition"));

        try (ZipInputStream zis =
                new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            // first entry, the kml document itself
            ZipEntry entry = zis.getNextEntry();
            assertEquals("wms.kml", entry.getName());
            byte[] data = IOUtils.toByteArray(zis);
            Document dom = dom(new ByteArrayInputStream(data));
            // print(dom);
            assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:GroundOverlay)", dom);
            String href =
                    XMLUnit.newXpathEngine()
                            .evaluate("//kml:Folder/kml:GroundOverlay/kml:Icon/kml:href", dom);
            assertEquals("image.png", href);
            zis.closeEntry();

            // the ground overlay for the raster layer
            entry = zis.getNextEntry();
            assertEquals("image.png", entry.getName());
            BufferedImage image = ImageIO.read(zis);
            zis.closeEntry();
            assertNull(zis.getNextEntry());

            // check the output, same as mapMultiName
            ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
        }
    }

    @Test
    public void testTimeFilter() throws Exception {
        String xml = getTestRequest("mapTimeFilter.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

        // same test as DimensionRasterGetMapTest#testTime
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));

        // making extra sure
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilter.png"), image, 100);
    }

    @Test
    public void testTimeFilterTimestamped() throws Exception {
        String xml = getTestRequest("mapTimeFilterTimestamped.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilterTimestamped.png"), image, 200);
    }

    @Test
    public void testTimeFilterFormattedTimestamp() throws Exception {
        String xml = getTestRequest("mapTimeFilterFormattedTimestamp.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(
                new File(SAMPLES + "mapTimeFilterFormattedTimestamp.png"), image, 200);
    }

    @Test
    public void downloadMapGif() throws Exception {
        String request = getTestRequest("mapSimple.xml");
        request = request.replaceAll("image/png", "image/gif");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("image/gif", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 200);
    }

    @Test
    public void downloadMapMars() throws Exception {
        String request = getTestRequest("mapMars.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapMars.png"), image, 200);
    }

    @Test
    public void downloadRemoteSimple11() throws Exception {
        byte[] bytes =
                testRemoteSimple(
                        "mapSimple.png", "mapRemoteSimple11.xml", "caps111.xml", "image/png");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 100);
    }

    @Test
    public void downloadRemoteSimple11Opacity() throws Exception {
        byte[] bytes =
                testRemoteSimple(
                        "mapSimpleWithAlpha.png",
                        "mapRemoteSimple11Opacity.xml",
                        "caps111.xml",
                        "image/png");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        // images match despite differences in alpha channel due to ImageAssert tolerances
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimpleWithAlpha.png"), image, 100);
        Color c = new Color(image.getRGB(128, 128), true);
        // alpha is 50% of 128 in mapSimpleWithAlpha alpha channel
        assertEquals(64, c.getAlpha());
    }

    @Test
    public void downloadRemoteSimpleOpacityOutOfRange() throws Exception {
        byte[] bytes =
                testRemoteSimple(
                        "mapSimpleWithAlpha.png",
                        "mapRemoteSimple11OpacityOutOfRange.xml",
                        "caps111.xml",
                        "text/xml");
        String response = new String(bytes, UTF_8);
        assertTrue(response.contains("has opacity set to an invalid value (only 0-100 allowed)"));
    }

    private byte[] testRemoteSimple(
            String imageName,
            String requestFileName,
            String capabilitiesFileName,
            String expectedType)
            throws Exception {
        byte[] bytes = null;
        String request = getTestRequest(requestFileName);
        String caps111 = getTestRequest(capabilitiesFileName);
        byte[] getMapBytes = FileUtils.readFileToByteArray(new File(SAMPLES + imageName));
        DownloadMapProcess process = applicationContext.getBean(DownloadMapProcess.class);
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(
                        "http://geoserver"
                                + ".org/geoserver/wms?service=WMS&request=GetCapabilities&version=1.1.0"),
                new MockHttpResponse(caps111, "text/xml"));
        // check it follows the links in the caps document
        client.expectGet(
                new URL(
                        "http://mock.test.geoserver"
                                + ".org/wms11?SERVICE=WMS&LAYERS=cite:BasicPolygons&FORMAT=image%2Fpng&HEIGHT=256&TRANSPARENT=false"
                                + "&REQUEST=GetMap&WIDTH=256&BBOX=-2.4,1.4,0.4,4.2&SRS=EPSG:4326&VERSION=1.1.1"),
                new MockHttpResponse(getMapBytes, "image/png"));
        // switch from the standard supplier to one using the mock client prepared above
        Supplier<HTTPClient> oldSupplier = process.getHttpClientSupplier();
        try {
            process.setHttpClientSupplier(() -> client);

            MockHttpServletResponse response = postAsServletResponse("wps", request);
            assertEquals(expectedType, response.getContentType());
            bytes = response.getContentAsByteArray();
        } finally {
            process.setHttpClientSupplier(oldSupplier);
        }
        return bytes;
    }

    @Test
    public void downloadRemoteSimple13() throws Exception {
        String request = getTestRequest("mapRemoteSimple13.xml");
        String caps130 = getTestRequest("caps130.xml");
        byte[] getMapBytes = FileUtils.readFileToByteArray(new File(SAMPLES + "mapSimple.png"));
        DownloadMapProcess process = applicationContext.getBean(DownloadMapProcess.class);
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(
                        "http://geoserver.org/geoserver/wms?service=WMS&request=GetCapabilities&version=1.3.0"),
                new MockHttpResponse(caps130, "text/xml"));
        // check it follows the links in the caps document and does axis flipping as required
        client.expectGet(
                new URL(
                        "http://mock.test.geoserver"
                                + ".org/wms13?SERVICE=WMS&LAYERS=cite:BasicPolygons&FORMAT=image%2Fpng&HEIGHT=256&TRANSPARENT=false"
                                + "&REQUEST=GetMap&WIDTH=256&BBOX=1.4,-2.4,4.2,0.4&CRS=EPSG:4326&VERSION=1.3.0"),
                new MockHttpResponse(getMapBytes, "image/png"));
        // switch from the standard supplier to one using the mock client prepared above
        Supplier<HTTPClient> oldSupplier = process.getHttpClientSupplier();
        try {
            process.setHttpClientSupplier(() -> client);

            MockHttpServletResponse response = postAsServletResponse("wps", request);
            assertEquals("image/png", response.getContentType());
            BufferedImage image =
                    ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
            ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 100);
        } finally {
            process.setHttpClientSupplier(oldSupplier);
        }
    }

    @Test
    public void downloadLocalRemote() throws Exception {
        String request = getTestRequest("mapLocalRemote.xml");
        String caps111 = getTestRequest("caps111.xml");
        byte[] getMapBytes = FileUtils.readFileToByteArray(new File(SAMPLES + "lakes.png"));
        DownloadMapProcess process = applicationContext.getBean(DownloadMapProcess.class);
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(
                        "http://geoserver"
                                + ".org/geoserver/wms?service=WMS&request=GetCapabilities&version=1.1.0"),
                new MockHttpResponse(caps111, "text/xml"));
        // check it follows the links in the caps document
        client.expectGet(
                new URL(
                        "http://mock.test.geoserver"
                                + ".org/wms11?SERVICE=WMS&LAYERS=cite:Lakes&FORMAT=image%2Fpng&HEIGHT=256&TRANSPARENT=true"
                                + "&REQUEST=GetMap&WIDTH=256&BBOX=0.0,-0.003,0.004,0.001&SRS=EPSG:4326&VERSION=1.1.1"),
                new MockHttpResponse(getMapBytes, "image/png"));
        // switch from the standard supplier to one using the mock client prepared above
        Supplier<HTTPClient> oldSupplier = process.getHttpClientSupplier();
        try {
            process.setHttpClientSupplier(() -> client);

            MockHttpServletResponse response = postAsServletResponse("wps", request);
            assertEquals("image/png", response.getContentType());
            BufferedImage image =
                    ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
            ImageAssert.assertEquals(new File(SAMPLES + "localRemote.png"), image, 100);
        } finally {
            process.setHttpClientSupplier(oldSupplier);
        }
    }

    @Test
    public void testExecuteSingleDecoratedWithLegend() throws Exception {
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = getClass().getResource("legend_decoration.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "legend_decoration.xml"));
        String xml = getTestRequest("mapSingleLayerWithLegendDecoration.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "withLegend.png"), image, 1500);
    }

    @Test
    public void testExecuteSingleDecoratedWithLegendAndHeader() throws Exception {
        File layouts = getDataDirectory().findOrCreateDir("layouts");

        URL leftLayout = getClass().getResource("legend_left_decoration.xml");
        FileUtils.copyURLToFile(leftLayout, new File(layouts, "legend_left_decoration.xml"));

        URL headerLayout = getClass().getResource("header_decoration.xml");
        FileUtils.copyURLToFile(headerLayout, new File(layouts, "header_decoration.xml"));

        URL headerBackground = getClass().getResource("header.jpg");
        FileUtils.copyURLToFile(headerBackground, new File(layouts, "header.jpg"));

        String xml = getTestRequest("mapSingleLayerWithLegendAndHeaderDecoration.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

        ImageAssert.assertEquals(
                new File(SAMPLES + "singleLayerWithLegendAndHeader.png"), image, 1500);
    }

    @Test
    public void testExecuteMultiDecoratedWithLegendAndHeader() throws Exception {
        File layouts = getDataDirectory().findOrCreateDir("layouts");

        URL leftLayout = getClass().getResource("legend_left_decoration.xml");
        FileUtils.copyURLToFile(leftLayout, new File(layouts, "legend_left_decoration.xml"));
        URL rightLayout = getClass().getResource("legend_right_decoration.xml");
        FileUtils.copyURLToFile(rightLayout, new File(layouts, "legend_right_decoration.xml"));
        URL headerLayout = getClass().getResource("header_decoration.xml");
        FileUtils.copyURLToFile(headerLayout, new File(layouts, "header_decoration.xml"));
        URL headerBackground = getClass().getResource("header.jpg");
        FileUtils.copyURLToFile(headerBackground, new File(layouts, "header.jpg"));

        String xml = getTestRequest("mapMultiLayerWithLegendAndHeaderDecoration.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

        ImageAssert.assertEquals(
                new File(SAMPLES + "multiLayerWithLegendAndHeader.png"), image, 1500);
    }

    @Test
    public void testExecuteMultiDecoratedWithLegend() throws Exception {
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL leftLayout = getClass().getResource("legend_left_decoration.xml");
        FileUtils.copyURLToFile(leftLayout, new File(layouts, "legend_left_decoration.xml"));
        URL rightLayout = getClass().getResource("legend_right_decoration.xml");
        FileUtils.copyURLToFile(rightLayout, new File(layouts, "legend_right_decoration.xml"));
        String xml = getTestRequest("mapMultiLayerWithLegendDecoration.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiLayerWithLegend.png"), image, 1500);
    }

    @Test
    public void testMapBmTimeMetadata() throws Exception {
        String xml = getTestRequest("mapBlueMarbleMetadata.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("application/xml", response.getContentType());
        Document dom = dom(response, true);
        print(dom);

        // check the animation is produced as normal, de-referencing the link
        String fullLocation =
                XMLUnit.newXpathEngine()
                        .evaluate("//wps:Output[ows:Identifier='result']/wps:Reference/@href", dom);
        String testLocation = getTestReference(fullLocation);
        response = getAsServletResponse(testLocation);
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());

        // the metadata is in-line and should be
        assertXpathExists(
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/Warnings",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "count(//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/Warnings/*)",
                dom);
        assertXpathEvaluatesTo(
                "false",
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/WarningsFound",
                dom);
    }

    @Test
    public void testMapBmTimeMetadataWarnings() throws Exception {
        String xml = getTestRequest("mapBlueMarbleMetadataWarnings.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("application/xml", response.getContentType());
        Document dom = dom(response, true);
        print(dom);

        // check the animation is produced as normal, de-referencing the link
        String fullLocation =
                XMLUnit.newXpathEngine()
                        .evaluate("//wps:Output[ows:Identifier='result']/wps:Reference/@href", dom);
        String testLocation = getTestReference(fullLocation);
        response = getAsServletResponse(testLocation);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());

        // the metadata is in-line and should be
        assertXpathExists(
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/Warnings",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/Warnings/*)",
                dom);
        String prefix =
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/Warnings/DimensionWarning";
        // first warning, nearest
        assertXpathEvaluatesTo("sf:bmtime", prefix + "[1]/LayerName", dom);
        assertXpathEvaluatesTo("time", prefix + "[1]/DimensionName", dom);
        assertXpathEvaluatesTo("2004-02-01T00:00:00.000Z", prefix + "[1]/Value", dom);
        assertXpathEvaluatesTo("Nearest", prefix + "[1]/WarningType", dom);
        // flag about warnings
        assertXpathEvaluatesTo(
                "true",
                "//wps:Output[ows:Identifier='metadata']/wps:Data/wps:ComplexData/DownloadMetadata/WarningsFound",
                dom);
    }
}
