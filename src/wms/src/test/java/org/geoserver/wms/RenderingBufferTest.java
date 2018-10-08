/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests that the admin specified per layer buffer parameter is taken into account
 *
 * @author Andrea Aime - OpenGeo
 */
public class RenderingBufferTest extends WMSTestSupport {

    static final QName LINE_WIDTH_LAYER =
            new QName(MockData.CITE_URI, "LineWidth", MockData.CITE_PREFIX);

    static final String LINE_WIDTH_STYLE = "linewidth";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle(LINE_WIDTH_STYLE, "linewidth.sld", getClass(), getCatalog());
        Map properties = new HashMap();
        properties.put(MockData.KEY_STYLE, LINE_WIDTH_STYLE);
        testData.addVectorLayer(
                LINE_WIDTH_LAYER, properties, "LineWidth.properties", getClass(), getCatalog());
    }

    @Before
    public void resetBuffer() {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().remove(LayerInfo.BUFFER);
        catalog.save(layer);
    }

    @Test
    public void testGetMapNoBuffer() throws Exception {
        String request =
                "cite/wms?request=getmap&service=wms"
                        + "&layers="
                        + getLayerId(LINE_WIDTH_LAYER)
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertEquals(0, countNonBlankPixels("testGetMap", image, BG_COLOR));
    }

    @Test
    public void testGetFeatureInfoNoBuffer() throws Exception {
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request =
                "cite/wms?request=getfeatureinfo&service=wms"
                        + "&layers="
                        + layerName
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers="
                        + layerName
                        + "&info_format=application/vnd.ogc.gml";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("0", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testGetMapExplicitBuffer() throws Exception {
        String request =
                "cite/wms?request=getmap&service=wms"
                        + "&layers="
                        + getLayerId(LINE_WIDTH_LAYER)
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&buffer=30";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        int nonBlankPixels = countNonBlankPixels("testGetMap", image, BG_COLOR);
        assertTrue(nonBlankPixels > 0);
    }

    @Test
    public void testGetFeatureInfoExplicitBuffer() throws Exception {
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request =
                "cite/wms?version=1.1.1&request=getfeatureinfo&service=wms"
                        + "&layers="
                        + layerName
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers="
                        + layerName
                        + "&info_format=application/vnd.ogc.gml&buffer=30";
        Document dom = getAsDOM(request);
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testGetMapConfiguredBuffer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().put(LayerInfo.BUFFER, 30);
        catalog.save(layer);

        String request =
                "cite/wms?request=getmap&service=wms"
                        + "&layers="
                        + getLayerId(LINE_WIDTH_LAYER)
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertTrue(countNonBlankPixels("testGetMap", image, BG_COLOR) > 0);
    }

    @Test
    public void testGetFeatureInfoConfiguredBuffer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().put(LayerInfo.BUFFER, 30);
        catalog.save(layer);

        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request =
                "cite/wms?version=1.1.1&request=getfeatureinfo&service=wms"
                        + "&layers="
                        + layerName
                        + "&styles="
                        + LINE_WIDTH_STYLE
                        + "&width=50&height=50&format=image/png"
                        + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers="
                        + layerName
                        + "&info_format=application/vnd.ogc.gml";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }
}
