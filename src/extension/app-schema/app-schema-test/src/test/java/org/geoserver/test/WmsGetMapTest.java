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
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier */
public class WmsGetMapTest extends AbstractAppSchemaTestSupport {

    public WmsGetMapTest() throws Exception {
        super();
    }

    @Override
    protected WmsSupportMockData createTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("outcropcharacter", "styles/outcropcharacter.sld");
        mockData.addStyle("positionalaccuracy", "styles/positionalaccuracy.sld");
        mockData.addStyle("occurrencecount", "styles/attributeCountTest.sld");
        return mockData;
    }

    @Test
    public void testGetMapOutcropCharacter() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/test-data/img/outcrop.png")),
                imageBuffer,
                10);
    }

    @Test
    public void testGetMapOutcropCharacterReprojection() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:3857&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-222638.981586547,6800125.45439731,0,7170156.29399995&X=0&Y=0&width=20&height=20&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/test-data/img/outcrop_3857.png")),
                imageBuffer,
                10);
    }

    @Test
    public void testGetMapPositionalAccuracy() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=positionalaccuracy&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap positional accuracy", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/test-data/img/posacc.png")),
                imageBuffer,
                10);
    }

    @Test
    public void testGetMapAfterWFS() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?version=1.1.0&request=getFeature&typeName=gsml:MappedFeature&maxFeatures=1");
        LOGGER.info(prettyString(doc));
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=outcropcharacter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/test-data/img/outcrop.png")),
                imageBuffer,
                10);
    }

    @Test
    public void testGetMapWithCount() throws Exception {
        Document doc = getAsDOM("wfs?version=1.1.0&request=getFeature&typeName=gsml:MappedFeature");
        LOGGER.info(prettyString(doc));
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=occurrencecount&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/test-data/img/occurrence.png")),
                imageBuffer,
                10);
    }
}
