/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.Test;

public class WmsGetLegendGraphicTest extends AbstractAppSchemaTestSupport {

    public WmsGetLegendGraphicTest() throws Exception {
        super();
    }

    @Override
    protected WmsSupportMockData createTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("outcropcharacter", "styles/outcropcharacter.sld");
        return mockData;
    }

    @Test
    public void testGetLegendGraphicAll() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetLegendGraphic&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/png");

        BufferedImage imageBuffer = ImageIO.read(is);
        // ImageIO.write(imageBuffer, "PNG", new File("/tmp/image.png"));
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(0, 0, 255));
        assertPixel(imageBuffer, 10, 30, new Color(255, 0, 0));
        assertPixel(imageBuffer, 10, 50, new Color(0, 255, 0));
    }

    @Test
    public void testGetLegendGraphicBlueRule() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetLegendGraphic&rule=xrule&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/png");

        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(0, 0, 255));
    }

    @Test
    public void testGetLegendGraphicRedRule() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetLegendGraphic&rule=yrule&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/png");

        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(255, 0, 0));
    }

    @Test
    public void testGetLegendGraphicGreenRule() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetLegendGraphic&rule=zrule&SRS=EPSG:4326&layer=gsml:MappedFeature&style=outcropcharacter&X=0&Y=0&width=20&height=20&FORMAT=image/png");

        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        assertPixel(imageBuffer, 10, 10, new Color(0, 255, 0));
    }
}
