/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.kml.KMZMapOutputFormat;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DownloadMapProcessTest extends BaseDownloadImageProcessTest {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        super.registerNamespaces(namespaces);
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
    }
    
    @Test
    public void testDescribeProcess() throws Exception {
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=gs:DownloadMap");
        // print(d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='image/png']", d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='image/jpeg']", d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='" + KMZMapOutputFormat.MIME_TYPE + "']", d);
    }

    @Test
    public void testExecuteSingleLayer() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimple.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES +  "mapSimple.png"), image, 100);
    }

    @Test
    public void testExecuteSingleLayerFilter() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimpleFilter.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimpleFilter.png"), image, 100);
    }

    @Test
    public void testExecuteSingleDecorated() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapSimpleDecorated.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES +  "watermarked.png"), image, 100);
    }

    @Test
    public void testExecuteMultiName() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapMultiName.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapMultiName.png"), image, 100);
    }

    @Test
    public void testExecuteMultiLayer() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapMultiLayer.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
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


    public void testExecutMultiLayerKmz(String mime) throws Exception {
        String request = IOUtils.toString(getClass().getResourceAsStream("mapMultiLayer.xml"));
        request = request.replaceAll("image/png", mime);
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals(KMZMapOutputFormat.MIME_TYPE, response.getContentType());

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()));
        try {
            // first entry, the kml document itself
            ZipEntry entry = zis.getNextEntry();
            assertEquals("wms.kml", entry.getName());
            byte[] data = IOUtils.toByteArray(zis);
            Document dom = dom(new ByteArrayInputStream(data));
            // print(dom);
            assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:GroundOverlay)", dom);
            String href = XMLUnit.newXpathEngine().evaluate(
                    "//kml:Folder/kml:GroundOverlay/kml:Icon/kml:href", dom);
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
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapTimeFilter.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

        // same test as DimensionRasterGetMapTest#testTime
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 181, 181));

        // making extra sure
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilter.png"), image, 100);
    }

    @Test
    public void testTimeFilterTimestamped() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapTimeFilterTimestamped.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilterTimestamped.png"), image, 200);
    }

    @Test
    public void testTimeFilterFormattedTimestamp() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("mapTimeFilterFormattedTimestamp.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("image/png", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapTimeFilterFormattedTimestamp.png"), image, 200);
    }
    
    @Test
    public void downloadMapGif() throws Exception {
        String request = IOUtils.toString(getClass().getResourceAsStream("mapSimple.xml"));
        request = request.replaceAll("image/png", "image/gif");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("image/gif", response.getContentType());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        ImageAssert.assertEquals(new File(SAMPLES + "mapSimple.png"), image, 200);
    }

}
