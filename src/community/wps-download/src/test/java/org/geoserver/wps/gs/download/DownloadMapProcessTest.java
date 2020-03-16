/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
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
import org.geotools.data.ows.HTTPClient;
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
        // print(d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='image/png']", d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='image/jpeg']", d);
        assertXpathExists(
                "//ComplexOutput/Supported/Format[MimeType='" + KMZMapOutputFormat.MIME_TYPE + "']",
                d);
    }

    @Test
    public void testExecuteSingleLayer() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimple.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 100);
    }

    @Test
    public void testExecuteSingleLayerFilter() throws Exception {
        String xml =
                IOUtils.toString(getClass().getResourceAsStream("mapSimpleFilter.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimpleFilter.png"), image, 100);
    }

    @Test
    public void testExecuteSingleDecorated() throws Exception {
        String xml =
                IOUtils.toString(getClass().getResourceAsStream("mapSimpleDecorated.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "watermarked.png"), image, 100);
    }

    @Test
    public void testExecuteMultiName() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapMultiName.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=result.png", response.getHeader("Content-disposition"));
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
    }

    @Test
    public void testExecuteMultiLayer() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapMultiLayer.xml"), "UTF-8");
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
        String request =
                IOUtils.toString(getClass().getResourceAsStream("mapMultiLayer.xml"), "UTF-8");
        request = request.replaceAll("image/png", "image/geotiff");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("image/geotiff", response.getContentType());
        assertEquals("attachment; filename=result.tif", response.getHeader("Content-disposition"));
    }

    public void testExecutMultiLayerKmz(String mime) throws Exception {
        String request =
                IOUtils.toString(getClass().getResourceAsStream("mapMultiLayer.xml"), "UTF-8");
        request = request.replaceAll("image/png", mime);
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals(KMZMapOutputFormat.MIME_TYPE, response.getContentType());
        assertEquals("inline; filename=result.kmz", response.getHeader("Content-disposition"));

        ZipInputStream zis =
                new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()));
        try {
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
        } finally {
            zis.close();
        }
    }

    @Test
    public void testTimeFilter() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapTimeFilter.xml"), "UTF-8");
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
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("mapTimeFilterTimestamped.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilterTimestamped.png"), image, 200);
    }

    @Test
    public void testTimeFilterFormattedTimestamp() throws Exception {
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("mapTimeFilterFormattedTimestamp.xml"),
                        "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(
                new File(SAMPLES + "mapTimeFilterFormattedTimestamp.png"), image, 200);
    }

    @Test
    public void downloadMapGif() throws Exception {
        String request = IOUtils.toString(getClass().getResourceAsStream("mapSimple.xml"), "UTF-8");
        request = request.replaceAll("image/png", "image/gif");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("image/gif", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 200);
    }

    @Test
    public void downloadRemoteSimple11() throws Exception {
        String request =
                IOUtils.toString(getClass().getResourceAsStream("mapRemoteSimple11.xml"), "UTF-8");
        String caps111 = IOUtils.toString(getClass().getResourceAsStream("caps111.xml"), "UTF-8");
        byte[] getMapBytes = FileUtils.readFileToByteArray(new File(SAMPLES + "mapSimple.png"));
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
            assertEquals("image/png", response.getContentType());
            BufferedImage image =
                    ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
            ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 100);
        } finally {
            process.setHttpClientSupplier(oldSupplier);
        }
    }

    @Test
    public void downloadRemoteSimple13() throws Exception {
        String request =
                IOUtils.toString(getClass().getResourceAsStream("mapRemoteSimple13.xml"), "UTF-8");
        String caps130 = IOUtils.toString(getClass().getResourceAsStream("caps130.xml"), "UTF-8");
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
        String request =
                IOUtils.toString(getClass().getResourceAsStream("mapLocalRemote.xml"), "UTF-8");
        String caps111 = IOUtils.toString(getClass().getResourceAsStream("caps111.xml"), "UTF-8");
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
        String xml =
                IOUtils.toString(
                        getClass().getResourceAsStream("mapSingleLayerWithLegendDecoration.xml"),
                        "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image =
                ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "withLegend.png"), image, 1500);
    }
}
