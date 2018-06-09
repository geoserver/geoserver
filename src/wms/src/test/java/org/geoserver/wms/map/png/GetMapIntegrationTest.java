/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import static org.junit.Assert.*;

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

        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        ColorModel cm = bi.getColorModel();
        assertFalse(cm.hasAlpha());
        assertEquals(3, cm.getNumColorComponents());
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

        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        ColorModel cm = bi.getColorModel();
        assertTrue(cm.hasAlpha());
        assertEquals(3, cm.getNumColorComponents());
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

        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.OPAQUE, cm.getTransparency());
        assertEquals(-1, cm.getTransparentPixel());
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

        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.BITMASK, cm.getTransparency());
        assertTrue(cm.getTransparentPixel() >= 0);
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

        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
    }
}
