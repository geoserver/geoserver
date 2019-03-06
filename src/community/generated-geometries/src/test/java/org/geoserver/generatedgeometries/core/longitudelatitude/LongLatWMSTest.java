/*  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.core.longitudelatitude;

import static java.util.Collections.emptyMap;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_LAYER;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.LONG_LAT_QNAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.bbox;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.enableGeometryGenerationStrategy;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.filenameOf;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.wholeWorld;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.wmsUrl;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatTestData.wmsUrlStdSize;
import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class LongLatWMSTest extends GeoServerSystemTestSupport {

    @Before
    public void before() throws Exception {
        getGeoServer().reload();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        setupBasicLayer(testData);
        setupComplexLayer(testData);
    }

    private void setupBasicLayer(SystemTestData testData) throws IOException {
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME,
                emptyMap(),
                filenameOf(LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER),
                getClass(),
                getCatalog());
    }

    private void setupComplexLayer(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        testData.addVectorLayer(
                LONG_LAT_QNAME, emptyMap(), filenameOf(LONG_LAT_LAYER), getClass(), catalog);
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(LONG_LAT_QNAME);
        enableGeometryGenerationStrategy(catalog, featureTypeInfo);
    }

    @Test
    public void testThatWMSCallFailsWithoutGeometryInTheLayer() throws Exception {
        // given
        String url = wmsUrlStdSize(LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER, wholeWorld());

        // when
        Document dom = getAsDOM(url, 200);

        // then
        assertXpathEvaluatesTo("internalError", "//ServiceException/@code", dom);
    }

    @Test
    public void testThatLayerWithGeneratedGeometryEnabledReturnsProperlyRenderedPoints()
            throws Exception {
        // given
        int width = 400;
        int height = 400;
        String url = wmsUrl(LONG_LAT_LAYER, bbox(-2, -2, 2, 2), width, height);
        Color bgColor = new Color(255, 255, 255);
        Color pointColor = new Color(128, 128, 128);

        // when
        BufferedImage image = getAsImage(url, "image/png");
        ImageIO.write(image, "png", new File("image.png"));

        // then
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());

        assertPixel(image, 0, 0, bgColor);
        assertPixel(image, 100, 200, pointColor);
    }
}
