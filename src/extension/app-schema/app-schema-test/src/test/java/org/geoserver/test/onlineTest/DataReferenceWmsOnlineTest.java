/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.geoserver.test.onlineTest.support.AbstractDataReferenceWfsTest;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author Niels Charlier
 *     <p>Base class for wms online tests
 */
public abstract class DataReferenceWmsOnlineTest extends AbstractDataReferenceWfsTest {

    public DataReferenceWmsOnlineTest() throws Exception {
        super();
    }

    @Test
    public void testGetMapSimpleLithology() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=simplelithology&BBOX=140,-38,145,-35&width=500&height=500&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap simple lithology", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(
                        getClass().getResource("/test-data/img/datareference_simplelithology.png")),
                imageBuffer,
                250);
    }

    @Test
    public void testGetMapStratChart() throws Exception {
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=stratchart&BBOX=140,-38,150,-35&width=500&height=500&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap stratchart", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(
                        getClass().getResource("/test-data/img/datareference_stratchart.png")),
                imageBuffer,
                250);
    }

    @Test
    public void testGetMapAfterWFS() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=getFeature&version=1.1.0&typeName=gsml:MappedFeature&maxFeatures=1");
        LOGGER.info(prettyString(doc));
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=stratchart&BBOX=140,-38,150,-35&width=500&height=500&FORMAT=image/png");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap outcrop character", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(
                        getClass().getResource("/test-data/img/datareference_stratchart.png")),
                imageBuffer,
                250);
    }
}
