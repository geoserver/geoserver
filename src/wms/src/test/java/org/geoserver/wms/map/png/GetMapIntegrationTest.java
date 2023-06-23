/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetMapIntegrationTest extends WMSTestSupport {

    String bbox = "-2,0,2,6";

    String layers = getLayerId(MockData.BASIC_POLYGONS);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs11RasterLayers();
    }

    @Test
    public void testPngOpaque() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        assertEquals("image/png", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            ColorModel cm = bi.getColorModel();
            assertFalse(cm.hasAlpha());
            assertEquals(3, cm.getNumColorComponents());
        }
    }

    @Test
    public void testPngTransparent() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true");
        assertEquals("image/png", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            ColorModel cm = bi.getColorModel();
            assertTrue(cm.hasAlpha());
            assertEquals(3, cm.getNumColorComponents());
        }
    }

    @Test
    public void testPng8Opaque() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            IndexColorModel cm = (IndexColorModel) bi.getColorModel();
            assertEquals(Transparency.OPAQUE, cm.getTransparency());
            assertEquals(-1, cm.getTransparentPixel());
        }
    }

    @Test
    public void testPng8ForceBitmask() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true&format_options=quantizer:octree");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            IndexColorModel cm = (IndexColorModel) bi.getColorModel();
            assertEquals(Transparency.BITMASK, cm.getTransparency());
            assertTrue(cm.getTransparentPixel() >= 0);
        }
    }

    @Test
    public void testPng8Translucent() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            IndexColorModel cm = (IndexColorModel) bi.getColorModel();
            assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
        }
    }

    @Test
    public void testwms2_0_0Filter() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            IndexColorModel cm = (IndexColorModel) bi.getColorModel();
            assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
            assertEquals(cm.getMapSize(), 256);
        }

        MockHttpServletResponse response1 =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&SERVICE=WMS&VERSION=1.1.1"
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&FILTER=%3Cfes%3AFilter%20xmlns%3Axsi%3D%22http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema-instance%22%20xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%2F3.2%22%20xmlns%3Awfs%3D%22http%3A%2F%2Fwww.opengis.net%2Fwfs%22%20xmlns%3D%22http%3A%2F%2Fwww.opengis.net%2Ffes%2F2.0%22%20xmlns%3Afes%3D%22http%3A%2F%2Fwww.opengis.net%2Ffes%2F2.0%22%3E%3Cfes%3APropertyIsLike%20wildCard%3D%22*%22%20singleChar%3D%22.%22%20escapeChar%3D%22!%22%3E%3Cfes%3AValueReference%3EID%3C%2Ffes%3AValueReference%3E%3Cfes%3ALiteral%3E*0*%3C%2Ffes%3ALiteral%3E%3C%2Ffes%3APropertyIsLike%3E%3C%2Ffes%3AFilter%3E");
        assertEquals("image/png; mode=8bit", response1.getContentType());

        try (InputStream is1 = getBinaryInputStream(response1)) {
            BufferedImage bi1 = ImageIO.read(is1);
            IndexColorModel cm1 = (IndexColorModel) bi1.getColorModel();
            assertEquals(Transparency.BITMASK, cm1.getTransparency());
            assertTrue(cm1.getMapSize() < 256);
        }
    }

    @Test
    public void testwms1_1_0Filter() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            IndexColorModel cm = (IndexColorModel) bi.getColorModel();
            assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
            assertEquals(cm.getMapSize(), 256);
        }

        MockHttpServletResponse response1 =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&SERVICE=WMS&VERSION=1.1.1"
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&FILTER=%3Cogc%3AFilter%0A%09xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22%0A%09xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%0A%09%3Cogc%3APropertyIsLike%20wildCard%3D%22*%22%20singleChar%3D%22.%22%20escapeChar%3D%22!%22%20matchCase%3D%22false%22%3E%0A%09%09%3Cogc%3APropertyName%3EID%3C%2Fogc%3APropertyName%3E%0A%09%09%3Cogc%3ALiteral%3E*1*%3C%2Fogc%3ALiteral%3E%0A%09%3C%2Fogc%3APropertyIsLike%3E%0A%09%3Cogc%3ASortBy%3E%0A%20%20%20%20%3Cogc%3APropertyName%3EID%3C%2Fogc%3APropertyName%3E%0A%20%20%20%20%3Cogc%3ASortOrderType%3EASCENDING%3C%2Fogc%3ASortOrderType%3E%0A%20%20%3C%2Fogc%3ASortBy%3E%0A%3C%2Fogc%3AFilter%3E");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is1 = getBinaryInputStream(response1)) {
            BufferedImage bi1 = ImageIO.read(is1);
            IndexColorModel cm1 = (IndexColorModel) bi1.getColorModel();
            assertEquals(Transparency.BITMASK, cm1.getTransparency());
            assertTrue(cm1.getMapSize() < 256);
        }
    }

    @Test
    public void testwms1_0_0Filter() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true");
        assertEquals("image/png; mode=8bit", response.getContentType());

        try (InputStream is = getBinaryInputStream(response)) {
            BufferedImage bi = ImageIO.read(is);
            IndexColorModel cm = (IndexColorModel) bi.getColorModel();
            assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
            assertEquals(cm.getMapSize(), 256);
        }

        MockHttpServletResponse response1 =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&SERVICE=WMS&VERSION=1.1.1"
                                + "&Format=image/png8"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&transparent=true"
                                + "&FILTER=%3Cogc%3AFilter%0A%09xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22%0A%09xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%0A%09%3Cogc%3APropertyIsLike%20wildCard%3D%22*%22%20singleChar%3D%22.%22%20escapeChar%3D%22!%22%20%3E%0A%09%09%3Cogc%3APropertyName%3EID%3C%2Fogc%3APropertyName%3E%0A%09%09%3Cogc%3ALiteral%3E*1*%3C%2Fogc%3ALiteral%3E%0A%09%3C%2Fogc%3APropertyIsLike%3E%0A%3C%2Fogc%3AFilter%3E");
        assertEquals("image/png; mode=8bit", response1.getContentType());

        try (InputStream is1 = getBinaryInputStream(response1)) {
            BufferedImage bi1 = ImageIO.read(is1);
            IndexColorModel cm1 = (IndexColorModel) bi1.getColorModel();
            assertEquals(Transparency.BITMASK, cm1.getTransparency());
            assertTrue(cm1.getMapSize() < 256);
        }
    }
}
