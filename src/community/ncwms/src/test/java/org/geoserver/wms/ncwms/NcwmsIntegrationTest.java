/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import static org.junit.Assert.assertEquals;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.style.PaletteStyleHandler;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class NcwmsIntegrationTest extends WMSTestSupport {

    QName RAIN = new QName(MockTestData.CITE_URI, "rain", MockTestData.CITE_PREFIX);

    String GRAY_BLUE_STYLE = "grayToBlue";

    XpathEngine xpath;

    // @Override
    // protected String getLogConfiguration() {
    // return "/GEOTOOLS_DEVELOPER_LOGGING.properties";
    // }

    @Before
    public void setupXpath() {
        this.xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(
                RAIN, "rain.tif", ".tif", null, NcwmsIntegrationTest.class, getCatalog());
        testData.addStyle(
                getCatalog().getDefaultWorkspace(),
                GRAY_BLUE_STYLE,
                "grayToBlue.palette",
                NcwmsIntegrationTest.class,
                getCatalog(),
                Collections.singletonMap(StyleProperty.FORMAT, PaletteStyleHandler.FORMAT));

        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(RAIN));
        ci.getDimensions().get(0).setRange(NumberRange.create(0d, 7000d));
        getCatalog().save(ci);
    }

    @Test
    public void testPlain() throws Exception {
        BufferedImage image = getAsImage(requestBase(), "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // heavy rain here
        assertPixel(image, 32, 74, new Color(37, 37, 236));
        // mid value here
        assertPixel(image, 120, 74, new Color(129, 129, 191));
        // dry here
        assertPixel(image, 160, 60, new Color(170, 170, 170));
    }

    @Test
    public void testOpacity() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&OPACITY=50", "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // heavy rain here
        assertPixel(image, 32, 74, new Color(37, 37, 236, 128));
        // mid value here
        assertPixel(image, 120, 74, new Color(129, 129, 191, 128));
        // dry here
        assertPixel(image, 160, 60, new Color(170, 170, 170, 128));
    }

    @Test
    public void testLogarithmic() throws Exception {
        // the log application skews the map a lot towards the blues
        BufferedImage image =
                getAsImage(requestBase() + "&COLORSCALERANGE=1,6000&LOGSCALE=true", "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // heavy rain here
        assertPixel(image, 32, 74, new Color(3, 3, 253));
        // mid value here
        assertPixel(image, 120, 74, new Color(25, 25, 243));
        // dry here
        assertPixel(image, 160, 60, new Color(91, 91, 209));
    }

    @Test
    public void testRange() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&COLORSCALERANGE=100,5000", "image/png");
        // RenderedImageBrowser.showChain(image);
        // cut away both low and high
        // heavy rain here
        assertPixelIsTransparent(image, 32, 74);
        // almost dry here
        assertPixelIsTransparent(image, 160, 60);
    }

    @Test
    public void testBeforeAfterColor() throws Exception {
        BufferedImage image =
                getAsImage(
                        requestBase()
                                + "&COLORSCALERANGE=100,4000&BELOWMINCOLOR=0xFF0000&ABOVEMAXCOLOR=0x00FF00",
                        "image/png");
        // RenderedImageBrowser.showChain(image);
        // System.in.read();
        // cut away both low and high
        // heavy rain here
        assertPixel(image, 32, 74, Color.GREEN);
        // dry here
        assertPixel(image, 160, 60, Color.RED);
    }

    @Test
    public void testNumberColors() throws Exception {
        BufferedImage image = getAsImage(requestBase() + "&NUMCOLORBANDS=3", "image/png");
        assertUniqueColorCount(image, 3);
    }

    private String requestBase() {
        return "wms?service=WMS&version=1.1.1&request=GetMap&layers="
                + getLayerId(RAIN)
                + "&styles="
                + GRAY_BLUE_STYLE
                + "&format=image/png&srs=EPSG:4326&bbox=-180,-90,180,90&transparent=true&width=320&height=160";
    }

    private void assertUniqueColorCount(BufferedImage image, int count) {
        int[] rgb =
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        Set<Integer> colors = new HashSet<>();
        for (int i = 0; i < rgb.length; i++) {
            int curr = rgb[i];
            colors.add(curr);
        }

        assertEquals("Expected amount of colors not found", count, colors.size());
    }

    @Test
    public void testDatasetFiltering() throws Exception {
        // filter by workspace
        Document dom =
                getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities&dataset=cite");
        assertDatasetWithCapabilities(dom);
    }

    @Test
    public void testDatasetFilteringPOST() throws Exception {
        // filter by workspace
        Document dom =
                postAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities&dataset=cite");
        assertDatasetWithCapabilities(dom);
    }

    private void assertDatasetWithCapabilities(Document dom) throws XpathException, Exception {
        // check only the layers in that workspace are there, and are not qualified
        for (LayerInfo layer : getCatalog().getLayers()) {
            if ("cite".equals(layer.getResource().getStore().getWorkspace().getName())
                    && !"Geometryless".equals(layer.getName())) {
                assertEquals(
                        "Did not find " + layer.getName(),
                        1,
                        xpath.getMatchingNodes(
                                        "//wms:Layer[wms:Name = '" + layer.getName() + "']", dom)
                                .getLength());
            } else {
                assertEquals(
                        "Found unexpected " + layer.getName(),
                        0,
                        xpath.getMatchingNodes(
                                        "//wms:Layer[wms:Name = '" + layer.getName() + "']", dom)
                                .getLength());
            }
        }

        // filter by layer
        dom = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities&dataset=cite:rain");
        assertEquals(1, xpath.getMatchingNodes("//wms:Layer[wms:Name = 'rain']", dom).getLength());
        // root container and the layer itself
        assertEquals(2, xpath.getMatchingNodes("//wms:Layer", dom).getLength());
    }

    @Test
    public void testPostRequest() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<ogc:GetMap xmlns:ogc=\"http://www.opengis.net/ows\"\n"
                        + "            xmlns:gml=\"http://www.opengis.net/gml\"\n"
                        + "   version=\"1.1.1\" service=\"WMS\">\n"
                        + "   <StyledLayerDescriptor version=\"1.0.0\">\n"
                        + "      <NamedLayer>\n"
                        + "        <Name>"
                        + getLayerId(RAIN)
                        + "</Name>\n"
                        + "        <NamedStyle><Name>"
                        + GRAY_BLUE_STYLE
                        + "</Name></NamedStyle> \n"
                        + "      </NamedLayer> \n"
                        + "   </StyledLayerDescriptor>\n"
                        + "   <BoundingBox srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                        + "      <gml:coord><gml:X>-180</gml:X><gml:Y>-90</gml:Y></gml:coord>\n"
                        + "      <gml:coord><gml:X>180</gml:X><gml:Y>90</gml:Y></gml:coord>\n"
                        + "   </BoundingBox>\n"
                        + "   <Output>\n"
                        + "      <Format>image/png</Format>\n"
                        + "      <Size><Width>320</Width><Height>160</Height></Size>\n"
                        + "   </Output>\n"
                        + "</ogc:GetMap>";

        MockHttpServletResponse resp = postAsServletResponse("wms", xml);
        assertEquals("image/png", resp.getContentType());
        InputStream is = getBinaryInputStream(resp);
        BufferedImage image = ImageIO.read(is);

        // heavy rain here
        assertPixel(image, 32, 74, new Color(37, 37, 236));
        // mid value here
        assertPixel(image, 120, 74, new Color(129, 129, 191));
        // dry here
        assertPixel(image, 160, 60, new Color(170, 170, 170));
    }

    @Test
    public void testWfsCapabilitiesPostRequest() throws Exception {
        // run a WFS 2.0 capabilities request, used to NPE in the NcWmsDatasetCallback
        String xml =
                "<GetCapabilities xmlns=\"http://www.opengis.net/wfs/2.0\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "service=\"WFS\" "
                        + "xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>";
        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertEquals("wfs:WFS_Capabilities", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetLegendGraphic() throws Exception {
        // get legend graphic
        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.1.1&layer="
                                + getLayerId(RAIN)
                                + "&style="
                                + GRAY_BLUE_STYLE
                                + "&request=GetLegendGraphic&format=image/png&width=20&height=20",
                        "image/png");
        // compare the obtained image with the expect result
        try (InputStream inputStream =
                this.getClass().getResourceAsStream("gray_blue_legend.png")) {
            ImageAssert.assertEquals(ImageIO.read(inputStream), image, 1000);
        }
    }
}
