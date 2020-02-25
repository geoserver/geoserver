/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PropertyStyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.ImageWorker;
import org.geotools.image.test.ImageAssert;
import org.geotools.styling.Style;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetMapIntegrationTest extends WMSTestSupport {

    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs10RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("indexed", "indexed.sld", getClass(), catalog);
        testData.addStyle("crop_raster", "CropTransform.sld", getClass(), catalog);
        testData.addStyle("lakeScale", "lakeScale.sld", getClass(), catalog);
        testData.addStyle("channelSelector", "channelSelector.sld", getClass(), catalog);

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.STYLE, "indexed");

        testData.addRasterLayer(
                new QName(MockData.SF_URI, "indexed", MockData.SF_PREFIX),
                "indexed.tif",
                "tif",
                props,
                getClass(),
                catalog);

        props.put(LayerProperty.STYLE, "raster");

        testData.addRasterLayer(
                new QName(MockData.SF_URI, "paletted", MockData.SF_PREFIX),
                "paletted.tif",
                "tif",
                props,
                getClass(),
                catalog);

        testData.addRasterLayer(
                new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX),
                "raster-filter-test.zip",
                null,
                props,
                SystemTestData.class,
                catalog);

        testData.addRasterLayer(
                new QName(MockData.SF_URI, "fourbits", MockData.SF_PREFIX),
                "fourbits.zip",
                null,
                props,
                SystemTestData.class,
                catalog);

        testData.addRasterLayer(
                new QName(MockData.SF_URI, "multiband", MockData.SF_PREFIX),
                "multiband.tiff",
                null,
                props,
                SystemTestData.class,
                catalog);

        testData.addStyle(
                "BasicStyleGroup",
                "BasicStyleGroup.sld",
                GetMapKvpRequestReaderTest.class,
                getCatalog());
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        StyleInfo s = catalog.getStyleByName("BasicStyleGroup");

        lg.setName("BasicStyleGroup");
        lg.getLayers().add(null);
        lg.getStyles().add(s);
        catalog.add(lg);
    }

    @Test
    public void testIndexed() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?LAYERS=sf:indexed&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=100,78,104,80&WIDTH=300&HEIGHT=150");

        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        image = new ImageWorker(image).forceComponentColorModel().getRenderedImage();
        ImageAssert.assertEquals(
                new File("src/test/resources/org/geoserver/wms/map/indexed-expected.png"),
                image,
                0);
    }

    @Test
    public void testIndexedBlackBG() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bgcolor=0x000000&LAYERS=sf:indexed&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=100,78,104,80&WIDTH=300&HEIGHT=150&transparent=false");

        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        ImageAssert.assertEquals(
                new File("src/test/resources/org/geoserver/wms/map/indexed-bg-expected.png"),
                image,
                0);
    }

    @Test
    public void testRasterFilterRed() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false&CQL_FILTER=location like 'red%25'");

        assertEquals("image/png", response.getContentType());

        // check we got the
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(255, pixel[0]);
        assertEquals(0, pixel[1]);
        assertEquals(0, pixel[2]);
    }

    @Test
    public void testRasterFilterGreen() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false&CQL_FILTER=location like 'green%25'");

        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
    }

    @Test
    public void testMosaicTwice() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false");

        assertEquals("image/png", response.getContentType());

        response =
                getAsServletResponse(
                        "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false");

        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testIndexedTransparency() throws Exception {
        String request =
                "wms?LAYERS=sf:paletted&STYLES=&FORMAT=image%2Fpng&SERVICE=WMS"
                        + "&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A3174"
                        + "&BBOX=-3256153.625,826440.25,-2756153.625,1326440.25"
                        + "&WIDTH=256&HEIGHT=256&transparent=true";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));
        assertTrue(image.getColorModel().hasAlpha());

        int[] rgba = new int[4];
        // transparent pixel in the top left corner
        image.getData().getPixel(0, 0, rgba);
        assertEquals(0, rgba[3]);
        // solid pixel in the lower right corner
        image.getData().getPixel(255, 255, rgba);
        assertEquals(255, rgba[3]);
    }

    @Test
    public void testFourBits() throws Exception {
        String request =
                "wms?LAYERS=sf:fourbits&STYLES=&FORMAT=image/png"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4269"
                        + "&BBOX=-118.58930224611,45.862378906251,-118.33030957033,45.974688476563"
                        + "&WIDTH=761&HEIGHT=330";

        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());
    }

    /**
     * https://osgeo-org.atlassian.net/browse/GEOS-4893, make meta-tiler work with WMS 1.3 as well
     */
    @Test
    public void testMetaWMS13() throws Exception {
        String wms11 =
                "wms?LAYERS=cite%3ALakes&STYLES=&FORMAT=image%2Fpng&TILED=true&TILESORIGIN=0.0006%2C-0.0018"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&BBOX=0.0006,-0.0018,0.0031,0.0007&WIDTH=256&HEIGHT=256";
        String wms13 =
                "wms?LAYERS=cite%3ALakes&STYLES=&FORMAT=image%2Fpng&TILED=true&TILESORIGIN=-0.0018%2C0.0006"
                        + "&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS=EPSG%3A4326&BBOX=-0.0018,0.0006,0.0007,0.0031&WIDTH=256&HEIGHT=256";

        BufferedImage image11 = getAsImage(wms11, "image/png");
        BufferedImage image13 = getAsImage(wms13, "image/png");

        // compare the general structure
        assertEquals(image11.getWidth(), image13.getWidth());
        assertEquals(image11.getHeight(), image13.getHeight());
        assertEquals(image11.getColorModel(), image13.getColorModel());
        assertEquals(image11.getSampleModel(), image13.getSampleModel());
        // compare the actual data
        DataBufferByte db11 = (DataBufferByte) image11.getData().getDataBuffer();
        DataBufferByte db13 = (DataBufferByte) image13.getData().getDataBuffer();
        byte[][] bankData11 = db11.getBankData();
        byte[][] bankData13 = db13.getBankData();
        for (int i = 0; i < bankData11.length; i++) {
            assertTrue(Arrays.equals(bankData11[i], bankData13[i]));
        }
    }

    @Test
    public void testOpenLayersProxy() throws Exception {
        NamespaceContext oldContext = XMLUnit.getXpathNamespaceContext();
        try {
            Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
            registerNamespaces(namespaces);
            XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

            // setup a proxy base url
            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setProxyBaseUrl("http://www.geoserver.org:1234/gs");
            getGeoServer().save(global);

            Document dom =
                    getAsDOM(
                            "wms?LAYERS=sf:indexed&STYLES=&FORMAT=application/openlayers&SERVICE=WMS&VERSION=1.1.1"
                                    + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=100,78,104,80&WIDTH=300&HEIGHT=150");

            assertXpathEvaluatesTo(
                    "//www.geoserver.org:1234/gs/openlayers/OpenLayers.js",
                    "//xhtml:script[contains(@src, 'OpenLayers.js')]/@src",
                    dom);
        } finally {
            XMLUnit.setXpathNamespaceContext(oldContext);
        }
    }

    @Test
    public void testRasterRenderingTx() throws Exception {
        // System.out.println(getCatalog().getCoverages());

        String layer = getLayerId(MockData.USA_WORLDIMG);
        String url =
                "wms?service=WMS&VERSION=1.1.1&request=GetMap&styles="
                        + "&format=image/png&layers="
                        + layer
                        + "&WIDTH=100&HEIGHT=100"
                        + "&srs=epsg:4326&BBOX=-130,49,-125,54";

        BufferedImage image = getAsImage(url, "image/png");
        Color color = getPixelColor(image, 25, 25);
        // the color is not white, nor white-ish
        assertTrue(color.getRed() + color.getGreen() + color.getBlue() < (250 * 3));

        // now crop and check the image is cut
        url =
                "wms?service=WMS&VERSION=1.1.1&request=GetMap&styles=crop_raster"
                        + "&format=image/png&layers="
                        + layer
                        + "&WIDTH=100&HEIGHT=100"
                        + "&srs=epsg:4326&BBOX=-130,49,-125,54";
        image = getAsImage(url, "image/png");
        color = getPixelColor(image, 25, 25);
        // the color is white, or white-ish
        assertTrue(color.getRed() + color.getGreen() + color.getBlue() > (250 * 3));
    }

    @Test
    public void testRasterRenderingTxOutOfBbox() throws Exception {
        String layer = getLayerId(MockData.USA_WORLDIMG);
        String url =
                "wms?service=WMS&VERSION=1.1.1&request=GetMap&styles=crop_raster"
                        + "&format=image/png&layers="
                        + layer
                        + "&WIDTH=100&HEIGHT=100"
                        + "&srs=epsg:4326&BBOX=-120,40,-115,45";
        BufferedImage image = getAsImage(url, "image/png");
        Color color = getPixelColor(image, 25, 25);
        // the color is white, or white-ish
        assertTrue(color.getRed() + color.getGreen() + color.getBlue() > (250 * 3));
    }

    @Test
    public void testGetMapWithPropertyStyle() throws Exception {
        Properties props = new Properties();
        props.put("type", "point");
        props.put("color", "00ffff");
        StringWriter w = new StringWriter();
        props.store(w, null);

        String bbox = "-180,-90,180,90";
        String layer = getLayerId(CiteTestData.POINTS);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&layers="
                                + layer
                                + "&sld_body="
                                + w.toString()
                                + "&style_format="
                                + PropertyStyleHandler.FORMAT
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        checkImage(response);
    }

    @Test
    public void testScaleMethod() throws Exception {
        // first request, no scale method, scale is roughly 1:20k
        String request =
                "wms?&LAYERS="
                        + getLayerId(MockData.LAKES)
                        + "&STYLES=lakeScale&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=0,-0.002,0.00451,0&WIDTH=88&HEIGHT=44";
        BufferedImage bi = getAsImage(request, "image/png");
        assertNotBlank("Image should contain the polygon,  scale denominator 20k", bi);

        bi = getAsImage(request + "&scaleMethod=Accurate", "image/png");
        assertBlank("Image should not contain the polygon, scale is just below 1:20", bi);
    }

    @Test
    public void testStyleGroup() throws Exception {
        WMS wms = new WMS(getGeoServer());
        GetMapKvpRequestReader reader = new GetMapKvpRequestReader(wms);
        // asserts the a layerGroup can be created with null layer and a styleGroup sld
        HashMap kvp = new HashMap();
        kvp.put("layers", "BasicStyleGroup");
        kvp.put("styles", "");

        GetMapRequest request = reader.createRequest();
        request = reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getLayers());
        assertNotNull(request.getStyles());
    }

    @Test
    public void testResolveLayersForStyleGroup() throws Exception {
        WMS wms = new WMS(getGeoServer());
        GetMapXmlReader reader = new GetMapXmlReader(wms);

        GetMapRequest request = reader.createRequest();
        InputStream resourceStream =
                getClass().getResource("WMSPostLayerGroupWithStyleGroup.xml").openStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(resourceStream));

        request = (GetMapRequest) reader.read(request, input, new HashMap());

        String layer = MockData.BASIC_POLYGONS.getLocalPart();
        assertEquals(1, request.getLayers().size());
        assertTrue(request.getLayers().get(0).getName().endsWith(layer));

        assertEquals(1, request.getStyles().size());
        Style expected = getCatalog().getStyleByName("BasicStyleGroup").getStyle();
        Style style = request.getStyles().get(0);
        assertEquals(expected, style);
    }

    @Test
    public void testChannelSelectionEnvVar() throws Exception {
        String request =
                "wms?&LAYERS="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + ",sf:multiband"
                        + "&STYLES=polygon,channelSelector&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A32611"
                        + "&BBOX=508880,3551340,748865,3750000&WIDTH=64&HEIGHT=64";
        final File BASE = new File("src/test/resources/org/geoserver/wms/map");

        // first channel
        BufferedImage bi = getAsImage(request + "&env=band:1", "image/png");
        ImageAssert.assertEquals(new File(BASE, "csFirstChannel.png"), bi, 0);

        // middle channel
        bi = getAsImage(request + "&env=band:5", "image/png");
        ImageAssert.assertEquals(new File(BASE, "csMidChannel.png"), bi, 0);

        // last channel
        bi = getAsImage(request + "&env=band:9", "image/png");
        ImageAssert.assertEquals(new File(BASE, "csLastChannel.png"), bi, 0);
    }

    @Test
    public void testRenderTimeStatisticsVectorsIntegrationTest() throws Exception {

        String bbox = "-180,-90,180,90";
        String layer = getLayerId(CiteTestData.POINTS);
        String layer2 = getLayerId(CiteTestData.POLYGONS);
        MockHttpServletRequest request =
                createRequest(
                        "wms?bbox="
                                + bbox
                                + "&layers="
                                + layer
                                + ","
                                + layer2
                                + "&style_format="
                                + PropertyStyleHandler.FORMAT
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request);
        RenderTimeStatistics statistics =
                (RenderTimeStatistics) request.getAttribute(RenderTimeStatistics.ID);
        assertEquals(statistics.getLayerNames().size(), 2);
        assertNotNull(statistics.getRenderingTime(0));
        assertNotNull(statistics.getRenderingTime(1));
        assertNotNull(statistics.getLabellingTime());
        checkImage(response);
    }

    @Test
    public void testRenderTimeStatisticsRasterIntegrationTest() throws Exception {
        MockHttpServletRequest request =
                createRequest(
                        "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                                + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150&transparent=false&CQL_FILTER=location like 'green%25'");
        request.setMethod("GET");
        MockHttpServletResponse response = dispatch(request);
        RenderTimeStatistics statistics =
                (RenderTimeStatistics) request.getAttribute(RenderTimeStatistics.ID);
        assertEquals(statistics.getLayerNames().size(), 1);
        assertNotNull(statistics.getRenderingTime(0));
        checkImage(response);
    }
}
