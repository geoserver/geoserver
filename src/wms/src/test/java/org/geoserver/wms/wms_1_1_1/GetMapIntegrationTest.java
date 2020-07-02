/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.data.test.TestData;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.*;
import org.geoserver.wms.map.OpenLayersMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.image.ImageWorker;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetMapIntegrationTest extends WMSTestSupport {

    private static final QName ONE_BIT = new QName(MockData.SF_URI, "onebit", MockData.SF_PREFIX);

    private static final QName MOSAIC_HOLES =
            new QName(MockData.SF_URI, "mosaic_holes", MockData.SF_PREFIX);

    private static final QName MOSAIC = new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX);

    private static final QName MASKED = new QName(MockData.SF_URI, "masked", MockData.SF_PREFIX);

    public static QName GIANT_POLYGON =
            new QName(MockData.CITE_URI, "giantPolygon", MockData.CITE_PREFIX);

    public static QName LARGE_POLYGON =
            new QName(MockData.CITE_URI, "slightlyLessGiantPolygon", MockData.CITE_PREFIX);

    String bbox = "-130,24,-66,50";

    String styles = "states";

    String layers = "sf:states";

    public static final String STATES_SLD =
            "<StyledLayerDescriptor version=\"1.0.0\">"
                    + "<UserLayer><Name>sf:states</Name><UserStyle><Name>UserSelection</Name>"
                    + "<FeatureTypeStyle><Rule><Filter xmlns:gml=\"http://www.opengis.net/gml\">"
                    + "<PropertyIsEqualTo><PropertyName>STATE_ABBR</PropertyName><Literal>IL</Literal></PropertyIsEqualTo>"
                    + "</Filter><PolygonSymbolizer><Fill><CssParameter name=\"fill\">#FF0000</CssParameter></Fill>"
                    + "</PolygonSymbolizer></Rule><Rule><LineSymbolizer><Stroke/></LineSymbolizer></Rule>"
                    + "</FeatureTypeStyle></UserStyle></UserLayer></StyledLayerDescriptor>";

    public static final String STATES_SLD11 =
            "<StyledLayerDescriptor version=\"1.1.0\"> "
                    + " <UserLayer> "
                    + "  <Name>sf:states</Name> "
                    + "  <UserStyle> "
                    + "   <Name>UserSelection</Name> "
                    + "   <se:FeatureTypeStyle xmlns:se=\"http://www.opengis.net/se\"> "
                    + "    <se:Rule> "
                    + "     <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "
                    + "      <ogc:PropertyIsEqualTo> "
                    + "       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName> "
                    + "       <ogc:Literal>IL</ogc:Literal> "
                    + "      </ogc:PropertyIsEqualTo> "
                    + "     </ogc:Filter> "
                    + "     <se:PolygonSymbolizer> "
                    + "      <se:Fill> "
                    + "       <se:SvgParameter name=\"fill\">#FF0000</se:SvgParameter> "
                    + "      </se:Fill> "
                    + "     </se:PolygonSymbolizer> "
                    + "    </se:Rule> "
                    + "    <se:Rule> "
                    + "     <se:LineSymbolizer> "
                    + "      <se:Stroke/> "
                    + "     </se:LineSymbolizer> "
                    + "    </se:Rule> "
                    + "   </se:FeatureTypeStyle> "
                    + "  </UserStyle> "
                    + " </UserLayer> "
                    + "</StyledLayerDescriptor>";

    public static final String STATES_GETMAP = //
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n "
                    + "<ogc:GetMap service=\"WMS\"  version=\"1.1.1\" \n "
                    + "        xmlns:gml=\"http://www.opengis.net/gml\"\n "
                    + "        xmlns:ogc=\"http://www.opengis.net/ows\"\n "
                    + "        xmlns:sld=\"http://www.opengis.net/sld\"\n "
                    + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n "
                    + "        xsi:schemaLocation=\"http://www.opengis.net/ows GetMap.xsd http://www.opengis.net/gml geometry.xsd http://www.opengis.net/sld StyledLayerDescriptor.xsd \">\n "
                    + "        <sld:StyledLayerDescriptor>\n "
                    + "                <sld:NamedLayer>\n "
                    + "                        <sld:Name>sf:states</sld:Name>\n "
                    + "                        <sld:NamedStyle>\n "
                    + "                                <sld:Name>Default</sld:Name>\n "
                    + "                        </sld:NamedStyle>\n "
                    + "                </sld:NamedLayer>\n "
                    + "        </sld:StyledLayerDescriptor>\n "
                    + "        <ogc:BoundingBox srsName=\"4326\">\n "
                    + "                <gml:coord>\n "
                    + "                        <gml:X>-130</gml:X>\n "
                    + "                        <gml:Y>24</gml:Y>\n "
                    + "                </gml:coord>\n "
                    + "                <gml:coord>\n "
                    + "                        <gml:X>-66</gml:X>\n "
                    + "                        <gml:Y>50</gml:Y>\n "
                    + "                </gml:coord>\n "
                    + "        </ogc:BoundingBox>\n "
                    + "        <ogc:Output>\n "
                    + "                <ogc:Format>image/png</ogc:Format>\n "
                    + "                <ogc:Size>\n "
                    + "                        <ogc:Width>550</ogc:Width>\n "
                    + "                        <ogc:Height>250</ogc:Height>\n "
                    + "                </ogc:Size>\n "
                    + "        </ogc:Output>\n "
                    + "</ogc:GetMap>\n ";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs11RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("Population", "Population.sld", GetMapIntegrationTest.class, catalog);
        testData.addStyle(
                "jiffleBandSelect", "jiffleBandSelect.sld", GetMapIntegrationTest.class, catalog);
        testData.addVectorLayer(
                new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                Collections.EMPTY_MAP,
                "states.properties",
                getClass(),
                catalog);
        // add a parametric style to the mix
        testData.addStyle(
                "parametric",
                "parametric.sld",
                org.geoserver.wms.map.GetMapIntegrationTest.class,
                catalog);

        // add a translucent style to the mix
        testData.addStyle("translucent", "translucent.sld", GetMapIntegrationTest.class, catalog);

        testData.addStyle("raster", "raster.sld", SystemTestData.class, catalog);
        testData.addStyle(
                "demTranslucent", "demTranslucent.sld", GetMapIntegrationTest.class, catalog);

        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(
                MOSAIC_HOLES,
                "mosaic_holes.zip",
                null,
                properties,
                GetMapIntegrationTest.class,
                catalog);

        testData.addRasterLayer(
                ONE_BIT, "onebit.zip", null, properties, GetMapIntegrationTest.class, catalog);

        testData.addRasterLayer(
                MOSAIC, "mosaic.zip", null, properties, GetMapIntegrationTest.class, catalog);

        testData.addRasterLayer(
                MASKED, "masked.tif", null, properties, GetMapIntegrationTest.class, catalog);

        testData.addVectorLayer(
                GIANT_POLYGON,
                Collections.EMPTY_MAP,
                "giantPolygon.properties",
                SystemTestData.class,
                getCatalog());

        testData.addVectorLayer(
                LARGE_POLYGON,
                Collections.EMPTY_MAP,
                "slightlyLessGiantPolygon.properties",
                GetMapTest.class,
                getCatalog());

        addCoverageViewLayer();

        setupOpaqueGroup(catalog);
    }

    private void addCoverageViewLayer() throws Exception {
        final InputCoverageBand ib0 = new InputCoverageBand("mosaic", "2");
        final CoverageBand b0 =
                new CoverageBand(
                        Collections.singletonList(ib0), "mosaic@2", 0, CompositionType.BAND_SELECT);

        final InputCoverageBand ib1 = new InputCoverageBand("mosaic", "1");
        final CoverageBand b1 =
                new CoverageBand(
                        Collections.singletonList(ib1), "mosaic@1", 1, CompositionType.BAND_SELECT);

        final InputCoverageBand ib2 = new InputCoverageBand("mosaic", "0");
        final CoverageBand b2 =
                new CoverageBand(
                        Collections.singletonList(ib2), "mosaic@0", 2, CompositionType.BAND_SELECT);

        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(3);
        coverageBands.add(b0);
        coverageBands.add(b1);
        coverageBands.add(b2);
        CoverageView coverageView = new CoverageView("mosaic_shuffle", coverageBands);
        Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("mosaic");

        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        // Reordered bands coverage
        final CoverageInfo coverageInfoView =
                coverageView.createCoverageInfo("mosaic_shuffle", storeInfo, builder);
        coverageInfoView.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfoView);
        final LayerInfo layerInfoView = builder.buildLayer(coverageInfoView);
        cat.add(layerInfoView);
    }

    protected String getDefaultLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }

    @Test
    public void testImage() throws Exception {
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
        checkImage(response);
    }

    @Test
    public void testAllowedMimeTypes() throws Exception {

        WMSInfo wms = getWMS().getServiceInfo();
        GetMapOutputFormat format = new RenderedImageMapOutputFormat(getWMS());
        wms.getGetMapMimeTypes().add(format.getMimeType());
        wms.setGetMapMimeTypeCheckingEnabled(true);

        getGeoServer().save(wms);

        // check mime type allowed
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
        checkImage(response);

        // check mime type not allowed
        String result =
                getAsString(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format="
                                + OpenLayersMapOutputFormat.MIME_TYPE
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        assertTrue(result.indexOf("ForbiddenFormat") > 0);

        wms.setGetMapMimeTypeCheckingEnabled(false);
        wms.getGetMapMimeTypes().clear();
        getGeoServer().save(wms);

        result =
                getAsString(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format="
                                + OpenLayersMapOutputFormat.MIME_TYPE
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");

        assertTrue(result.indexOf("OpenLayers") > 0);
    }

    @Test
    public void testLayoutLegendNPE() throws Exception {
        // set the title to null
        FeatureTypeInfo states = getCatalog().getFeatureTypeByName("states");
        states.setTitle(null);
        getCatalog().save(states);

        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout.xml"));

        // get a map with the layout, it used to NPE
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=Population&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&format_options=layout:test-layout",
                        "image/png");
        // RenderedImageBrowser.showChain(image);

        // check the pixels that should be in the legend
        assertPixel(image, 12, 16, Color.RED);
        assertPixel(image, 12, 32, Color.GREEN);
        assertPixel(image, 12, 52, Color.BLUE);
    }

    @Test
    public void testLayoutLegendStyleTitle() throws Exception {
        // set the title to null
        FeatureTypeInfo states = getCatalog().getFeatureTypeByName("states");
        states.setTitle(null);
        getCatalog().save(states);

        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout-sldtitle.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-sldtitle.xml"));

        // get a map with the layout, it used to NPE
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=Population&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&format_options=layout:test-layout-sldtitle",
                        "image/png");
        // RenderedImageBrowser.showChain(image);

        // check the pixels that should be in the legend
        assertPixel(image, 12, 36, Color.RED);
        assertPixel(image, 12, 52, Color.GREEN);
        assertPixel(image, 12, 72, Color.BLUE);
    }

    @Test
    public void testLayoutTranslucent() throws Exception {
        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout.xml"));

        // get a map with the layout after using a translucent style
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=translucent&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&format_options=layout:test-layout&transparent=true",
                        "image/png");
        // RenderedImageBrowser.showChain(image);

        // check the pixels that should be in the scale bar
        assertPixel(image, 56, 211, Color.WHITE);
        // see GEOS-6482
        assertTrue(
                getPixelColor(image, 52, 221).equals(Color.BLACK)
                        || getPixelColor(image, 52, 222).equals(Color.BLACK));
    }

    @Test
    public void testGeotiffMime() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/geotiff"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        assertEquals("image/geotiff", response.getContentType());
        assertEquals("inline; filename=sf-states.tif", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testLargerThanWorld() throws Exception {
        // setup a logging "bomb" rigged to explode when the warning message we
        // want to eliminate
        org.apache.log4j.Logger l4jLogger = getLog4JLogger(GetMap.class, "LOGGER");
        l4jLogger.addAppender(
                new AppenderSkeleton() {

                    @Override
                    public boolean requiresLayout() {
                        return false;
                    }

                    @Override
                    public void close() {}

                    @Override
                    protected void append(LoggingEvent event) {
                        if (event.getMessage() != null
                                && event.getMessage()
                                        .toString()
                                        .startsWith("Failed to compute the scale denominator")) {
                            // ka-blam!
                            fail("The error message is still there!");
                        }
                    }
                });

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-9.6450076761637E7,-3.9566251818225E7,9.6450076761637E7,3.9566251818225E7"
                                + "&styles=&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:900913");
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));
    }

    private org.apache.log4j.Logger getLog4JLogger(Class targetClass, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = targetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        Logger jlogger = (Logger) field.get(null);
        Field l4jField = jlogger.getClass().getDeclaredField("logger");
        l4jField.setAccessible(true);
        org.apache.log4j.Logger l4jLogger = (org.apache.log4j.Logger) l4jField.get(jlogger);
        return l4jLogger;
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
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));

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
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));

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
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));

        InputStream is = getBinaryInputStream(response);
        BufferedImage bi = ImageIO.read(is);
        IndexColorModel cm = (IndexColorModel) bi.getColorModel();
        assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());
    }

    @Test
    public void testDefaultContentDisposition() throws Exception {
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
        assertEquals("inline; filename=sf-states.png", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testForcedContentDisposition() throws Exception {
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
                                + "&srs=EPSG:4326&content-disposition=attachment");
        assertEquals("image/png", response.getContentType());
        assertEquals(
                "attachment; filename=sf-states.png", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testForcedFilename() throws Exception {
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
                                + "&srs=EPSG:4326&filename=dude.png");
        assertEquals("image/png", response.getContentType());
        assertEquals("inline; filename=dude.png", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testForcedContentDispositionFilename() throws Exception {
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
                                + "&srs=EPSG:4326&content-disposition=attachment&filename=dude.png");
        assertEquals("image/png", response.getContentType());
        assertEquals("attachment; filename=dude.png", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testSldBody() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&SLD_BODY="
                                + STATES_SLD.replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test
    public void testStyleBody() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&STYLE_BODY="
                                + STATES_SLD.replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test
    public void testSldBody11() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&SLD_BODY="
                                + STATES_SLD11.replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test
    public void testStyleBody11() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&STYLE_BODY="
                                + STATES_SLD11.replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test
    public void testSldBodyNoVersion() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&SLD_BODY="
                                + STATES_SLD
                                        .replace(" version=\"1.1.0\"", "")
                                        .replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test
    public void testStyleBodyNoVersion() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles="
                                + "&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&STYLE_BODY="
                                + STATES_SLD
                                        .replace(" version=\"1.1.0\"", "")
                                        .replaceAll("=", "%3D"));
        checkImage(response);
    }

    @Test
    public void testSldBodyPost() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&format=image/png&request=GetMap&width=550&height=250"
                                + "&srs=EPSG:4326",
                        STATES_SLD);

        checkImage(response);
    }

    @Test
    public void testSldBodyPost11() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&format=image/png&request=GetMap&width=550&height=250"
                                + "&srs=EPSG:4326",
                        STATES_SLD11);

        checkImage(response);
    }

    @Test
    public void testXmlPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?", STATES_GETMAP);
        checkImage(response);
    }

    @Test
    public void testRemoteOWSGet() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        ServletResponse response =
                getAsServletResponse(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format=image/png"
                                + "&layers="
                                + RemoteOWSTestSupport.TOPP_STATES
                                + ","
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles=Population,"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&remote_ows_type=WFS"
                                + "&remote_ows_url="
                                + RemoteOWSTestSupport.WFS_SERVER_URL
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testRemoteOWSUserStyleGet() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) {
            return;
        }

        URL url = GetMapIntegrationTest.class.getResource("remoteOws.sld");

        ServletResponse response =
                getAsServletResponse(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format=image/png"
                                + "&sld="
                                + url.toString()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testWorkspaceQualified() throws Exception {

        Document doc =
                getAsDOM(
                        "cite/wms?request=getmap&service=wms"
                                + "&layers=PrimitiveGeoFeature&width=100&height=100&format=image/png"
                                + "&srs=epsg:4326&bbox=-180,-90,180,90",
                        true);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());

        ServletResponse response =
                getAsServletResponse(
                        "cite/wms?request=getmap&service=wms"
                                + "&layers=Lakes&width=100&height=100&format=image/png"
                                + "&srs=epsg:4326&bbox=-180,-90,180,90");
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testLayerQualified() throws Exception {
        Document doc =
                getAsDOM(
                        "cite/Ponds/wms?request=getmap&service=wms"
                                + "&layers=Forests&width=100&height=100&format=image/png"
                                + "&srs=epsg:4326&bbox=-180,-90,180,90",
                        true);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());

        ServletResponse response =
                getAsServletResponse(
                        "cite/Ponds/wms?request=getmap&service=wms"
                                + "&layers=Ponds&width=100&height=100&format=image/png"
                                + "&srs=epsg:4326&bbox=-180,-90,180,90");
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testGroupWorkspaceQualified() throws Exception {
        // check the group works without workspace qualification
        String url =
                "wms?request=getmap&service=wms"
                        + "&layers=nature&width=100&height=100&format=image/png"
                        + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
        ServletResponse response = getAsServletResponse(url);
        assertEquals("image/png", response.getContentType());

        // see that it works also with workspace qualification
        response = getAsServletResponse("cite/" + url);
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testEnvDefault() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=parametric&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));

        int[] rgba = new int[3];
        // fully black pixel in the middle of the map
        image.getData().getPixel(250, 125, rgba);
        // assertEquals(0, rgba[0]);
        // assertEquals(0, rgba[1]);
        // assertEquals(0, rgba[2]);
    }

    @Test
    public void testEnvRed() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox="
                                + bbox
                                + "&styles=parametric&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326&env=color:0xFF0000");
        assertEquals("image/png", response.getContentType());

        RenderedImage image = ImageIO.read(getBinaryInputStream(response));

        int[] rgba = new int[3];
        // fully red pixel in the middle of the map
        image.getData().getPixel(250, 125, rgba);
        // assertEquals(255, rgba[0]);
        // assertEquals(0, rgba[1]);
        // assertEquals(0, rgba[2]);
    }

    @Test
    public void testMosaicHoles() throws Exception {
        String url =
                "wms?LAYERS=sf%3Amosaic_holes&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326"
                        + "&BBOX=6.40284375,36.385494140625,12.189662109375,42.444494140625"
                        + "&WIDTH=489&HEIGHT=512&transparent=true";
        BufferedImage bi = getAsImage(url, "image/png");
        int[] pixel = new int[4];
        bi.getRaster().getPixel(0, 250, pixel);
        assertTrue(Arrays.equals(new int[] {0, 0, 0, 255}, pixel));

        // now reconfigure the mosaic for transparency
        CoverageInfo ci = getCatalog().getCoverageByName("sf:mosaic_holes");
        Map<String, Serializable> params = ci.getParameters();
        params.put(ImageMosaicFormat.INPUT_TRANSPARENT_COLOR.getName().getCode(), "#000000");
        params.put(ImageMosaicFormat.OUTPUT_TRANSPARENT_COLOR.getName().getCode(), "#000000");
        getCatalog().save(ci);

        // this time that pixel should be transparent
        bi = getAsImage(url, "image/png");
        bi.getRaster().getPixel(0, 250, pixel);
        assertTrue(Arrays.equals(new int[] {255, 255, 255, 0}, pixel));
    }

    @Test
    public void testTransparentPaletteOpaqueOutput() throws Exception {
        String url =
                "wms?LAYERS="
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "&styles=demTranslucent&"
                        + "FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200&bgcolor=0xFF0000";
        BufferedImage bi = getAsImage(url, "image/png");

        ColorModel cm = bi.getColorModel();
        assertTrue(cm instanceof IndexColorModel);
        assertEquals(Transparency.OPAQUE, cm.getTransparency());

        // grab a pixel in the low left corner, should be red (BG color)
        int[] pixel = new int[1];
        bi.getRaster().getPixel(4, 196, pixel);
        int[] color = new int[3];
        cm.getComponents(pixel[0], color, 0);
        assertEquals(255, color[0]);
        assertEquals(0, color[1]);
        assertEquals(0, color[2]);

        // a pixel high enough to be solid, should be fully green
        bi.getRaster().getPixel(56, 49, pixel);
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[0]);
        assertEquals(255, color[1]);
        assertEquals(0, color[2]);
    }

    @Test
    public void testCoverageViewMap() throws Exception {
        String url =
                "wms?LAYERS=mosaic&"
                        + "&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=7,37,11,41&WIDTH=100&HEIGHT=200&bgcolor=0xFF0000";
        BufferedImage bi = getAsImage(url, "image/png");
        int[] pixel = new int[3];
        bi.getRaster().getPixel(50, 100, pixel);

        final int R_PIXEL = 45;
        final int G_PIXEL = 46;
        final int B_PIXEL = 69;

        assertEquals(R_PIXEL, pixel[0]);
        assertEquals(G_PIXEL, pixel[1]);
        assertEquals(B_PIXEL, pixel[2]);

        // The shuffled view revert RGB bands to BGR
        url =
                "wms?LAYERS=mosaic_shuffle&"
                        + "&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=7,37,11,41&WIDTH=100&HEIGHT=200&bgcolor=0xFF0000";

        bi = getAsImage(url, "image/png");
        bi.getRaster().getPixel(50, 100, pixel);

        assertEquals(B_PIXEL, pixel[0]);
        assertEquals(G_PIXEL, pixel[1]);
        assertEquals(R_PIXEL, pixel[2]);
    }

    @Test
    public void testTransparentPaletteTransparentOutput() throws Exception {
        String url =
                "wms?LAYERS="
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "&styles=demTranslucent&"
                        + "FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200&transparent=true";
        BufferedImage bi = getAsImage(url, "image/png");

        ColorModel cm = bi.getColorModel();
        assertTrue(cm instanceof IndexColorModel);
        assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());

        // grab a pixel in the low left corner, should be transparent
        int[] pixel = new int[1];
        bi.getRaster().getPixel(4, 196, pixel);
        int[] color = new int[4];
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[3]);

        // a pixel high enough to be solid, should be solid green
        bi.getRaster().getPixel(56, 49, pixel);
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[0]);
        assertEquals(255, color[1]);
        assertEquals(0, color[2]);
        assertEquals(255, color[3]);
    }

    @Test
    public void testTransparentPaletteTransparentOutputPng8() throws Exception {
        String url =
                "wms?LAYERS="
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "&styles=demTranslucent&"
                        + "FORMAT=image%2Fpng8&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200&transparent=true";
        BufferedImage bi = getAsImage(url, "image/png; mode=8bit");

        ColorModel cm = bi.getColorModel();
        assertTrue(cm instanceof IndexColorModel);
        assertEquals(Transparency.TRANSLUCENT, cm.getTransparency());

        // grab a pixel in the low left corner, should be transparent
        int[] pixel = new int[1];
        bi.getRaster().getPixel(4, 196, pixel);
        int[] color = new int[4];
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[3]);

        // a pixel high enough to be solid, should be solid green
        bi.getRaster().getPixel(56, 49, pixel);
        cm.getComponents(pixel[0], color, 0);
        assertEquals(0, color[0]);
        assertEquals(255, color[1]);
        assertEquals(0, color[2]);
        assertEquals(255, color[3]);
    }

    @Test
    public void testLayoutLegendStyleTitleDPI() throws Exception {
        // set the title to null
        FeatureTypeInfo states = getCatalog().getFeatureTypeByName("states");
        states.setTitle(null);
        getCatalog().save(states);

        // add the layout to the data dir
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("test-layout-sldtitle.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-sldtitle.xml"));

        int dpi = 90 * 2;
        int width = 550 * 2;
        int height = 250 * 2;

        // get a map with the layout, it used to NPE
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=Population&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width="
                                + width
                                + "&height="
                                + height
                                + "&srs=EPSG:4326&format_options=layout:test-layout-sldtitle;dpi:"
                                + dpi,
                        "image/png");
        // RenderedImageBrowser.showChain(image);
        // check the pixels that should be in the legend
        assertPixel(image, 15, 67, Color.RED);
        assertPixel(image, 15, 107, Color.GREEN);
        assertPixel(image, 15, 147, Color.BLUE);
    }

    @Test
    public void testLayerGroupSingle() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group =
                createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.SINGLE, null);
        try {
            String url =
                    "wms?LAYERS="
                            + group.getName()
                            + "&STYLES=&FORMAT=image%2Fpng"
                            + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            BufferedImage image = getAsImage(url, "image/png");

            assertPixel(image, 150, 160, Color.WHITE);
            // places
            assertPixel(image, 180, 16, COLOR_PLACES_GRAY);
            // lakes
            assertPixel(image, 90, 200, COLOR_LAKES_BLUE);
        } finally {
            catalog.remove(group);
        }
    }

    @Test
    public void testLayerGroupNamed() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group =
                createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
        try {
            String url =
                    "wms?LAYERS="
                            + group.getName()
                            + "&STYLES=&FORMAT=image%2Fpng"
                            + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            BufferedImage image = getAsImage(url, "image/png");

            assertPixel(image, 150, 160, Color.WHITE);
            // places
            assertPixel(image, 180, 16, COLOR_PLACES_GRAY);
            // lakes
            assertPixel(image, 90, 200, COLOR_LAKES_BLUE);
        } finally {
            catalog.remove(group);
        }
    }

    @Test
    public void testLayerGroupContainer() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group =
                createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.CONTAINER, null);
        try {
            String url =
                    "wms?LAYERS="
                            + group.getName()
                            + "&STYLES=&FORMAT=image%2Fpng"
                            + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            // this group is not meant to be called directly so we should get an exception
            MockHttpServletResponse resp = getAsServletResponse(url);
            assertEquals("application/vnd.ogc.se_xml", resp.getContentType());

            Document dom = getAsDOM(url);
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());

            Element serviceException =
                    (Element)
                            dom.getDocumentElement()
                                    .getElementsByTagName("ServiceException")
                                    .item(0);
            assertEquals("LayerNotDefined", serviceException.getAttribute("code"));
            assertEquals("layers", serviceException.getAttribute("locator"));
            assertEquals(
                    "Could not find layer " + group.getName(),
                    serviceException.getTextContent().trim());
        } finally {
            catalog.remove(group);
        }
    }

    @Test
    public void testLayerGroupModeEo() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group =
                createLakesPlacesLayerGroup(
                        catalog,
                        LayerGroupInfo.Mode.EO,
                        catalog.getLayerByName(getLayerId(MockData.LAKES)));
        try {
            String url =
                    "wms?LAYERS="
                            + group.getName()
                            + "&STYLES=&FORMAT=image%2Fpng"
                            + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            BufferedImage image = getAsImage(url, "image/png");

            assertPixel(image, 150, 160, Color.WHITE);
            // no places
            assertPixel(image, 180, 16, Color.WHITE);
            // lakes
            assertPixel(image, 90, 200, COLOR_LAKES_BLUE);
        } finally {
            catalog.remove(group);
        }
    }

    @Test
    public void testOneBit() throws Exception {
        String url =
                "wms?LAYERS="
                        + getLayerId(ONE_BIT)
                        + "&STYLES=&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=10&HEIGHT=10&BBOX=0,0,10,10";
        // used to crash, should give us back a empty image instead
        getAsImage(url, "image/png");
    }

    @Test
    public void testSldExternalEntities() throws Exception {
        URL sldUrl = TestData.class.getResource("externalEntities.sld");
        String url =
                "wms?bbox="
                        + bbox
                        + "&styles="
                        + "&layers="
                        + layers
                        + "&Format=image/png"
                        + "&request=GetMap"
                        + "&width=550"
                        + "&height=250"
                        + "&srs=EPSG:4326"
                        + "&sld="
                        + sldUrl.toString();

        WMS wms = new WMS(getGeoServer());
        GeoServerInfo geoserverInfo = wms.getGeoServer().getGlobal();
        try {
            // enable entities in external SLD files
            geoserverInfo.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(geoserverInfo);

            // if entities evaluation is enabled
            // the parser will try to read a file on the local file system
            // if the file is found, its content will be used to replace the entity
            // if the file is not found the parser will throw a FileNotFoundException
            String response = getAsString(url);
            assertTrue(response.indexOf("Error while getting SLD.") > -1);

            // disable entities
            geoserverInfo.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(geoserverInfo);

            // if entities evaluation is disabled
            // the parser will throw a MalformedURLException when it finds an entity
            response = getAsString(url);
            assertTrue(response.indexOf("Entity resolution disallowed") > -1);

            // try default value: disabled entities
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);

            // if entities evaluation is disabled
            // the parser will throw a MalformedURLException when it finds an entity
            response = getAsString(url);
            assertTrue(response.indexOf("Entity resolution disallowed") > -1);
        } finally {
            // default
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);
        }
    }

    @Test
    public void testRssMime() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?request=reflect&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&format=rss");
        assertEquals("application/rss+xml", response.getContentType());
    }

    /** Basic sanity tests on a polar stereographic projection (EPSG:5041) WMS response. */
    @Test
    public void testPolarStereographic() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?"
                                + "service=WMS"
                                + "&version=1.1.1"
                                + "&request=GetMap"
                                + "&layers=sf:states"
                                + "&bbox=-10700000,-10700000,14700000,14700000,EPSG:5041"
                                + "&width=200"
                                + "&height=200"
                                + "&srs=EPSG:5041"
                                + "&format=image%2Fpng");
        checkImage(response, "image/png", 200, 200);
        String testName = "testPolarStereographic";
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertNotBlank(testName, image);
        // top-left quadrant should not be blank
        assertNotBlank(testName, image.getSubimage(0, 0, 100, 100));
        // top 25% should be blank
        assertEquals(0, countNonBlankPixels(testName, image.getSubimage(0, 0, 200, 50), BG_COLOR));
        // right-hand side should be blank
        assertEquals(
                0, countNonBlankPixels(testName, image.getSubimage(100, 0, 100, 200), BG_COLOR));
        // bottom 35% should be blank
        assertEquals(
                0, countNonBlankPixels(testName, image.getSubimage(0, 130, 200, 70), BG_COLOR));
    }

    @Test
    public void testMapWrapping() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        Boolean original = wms.getMetadata().get(WMS.MAP_WRAPPING_KEY, Boolean.class);
        try {
            wms.getMetadata().put(WMS.MAP_WRAPPING_KEY, Boolean.TRUE);
            gs.save(wms);

            String layer = getLayerId(GIANT_POLYGON);
            String request =
                    "wms?version=1.1.1&bbox=170,-10,190,10&format=image/png"
                            + "&request=GetMap&layers="
                            + layer
                            + "&styles=polygon"
                            + "&width=100&height=100&srs=EPSG:4326";

            String wrapDisabledOptionRequest = request + "&format_options=mapWrapping:false";
            String wrapEnabledOptionRequest = request + "&format_options=mapWrapping:true";

            BufferedImage image = getAsImage(request, "image/png");
            // with wrapping enabled we should get a gray pixel
            assertPixel(image, 75, 0, new Color(170, 170, 170));

            image = getAsImage(wrapDisabledOptionRequest, "image/png");
            // This should disable wrapping, so we get white pixel (nothing)
            assertPixel(image, 75, 0, Color.WHITE);

            image = getAsImage(wrapEnabledOptionRequest, "image/png");
            // with wrapping explictly enabled we should get a gray pixel
            assertPixel(image, 75, 0, new Color(170, 170, 170));

            wms.getMetadata().put(WMS.MAP_WRAPPING_KEY, Boolean.FALSE);
            gs.save(wms);
            image = getAsImage(request, "image/png");
            // with wrapping disabled we should get a white one (nothing)
            assertPixel(image, 75, 0, Color.WHITE);

            image = getAsImage(wrapDisabledOptionRequest, "image/png");
            // With explicit config disable, our option should be disabled
            assertPixel(image, 75, 0, Color.WHITE);
            image = getAsImage(wrapEnabledOptionRequest, "image/png");
            assertPixel(image, 75, 0, Color.WHITE);
        } finally {
            wms.getMetadata().put(WMS.MAP_WRAPPING_KEY, original);
            gs.save(wms);
        }
    }

    @Test
    public void testAdvancedProjectionHandling() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        Boolean original = wms.getMetadata().get(WMS.ADVANCED_PROJECTION_KEY, Boolean.class);
        try {
            wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, Boolean.TRUE);
            gs.save(wms);

            String layer = getLayerId(LARGE_POLYGON);

            String request =
                    "wms?version=1.1.1&bbox=-18643898.1832,0,18084728.7111,20029262&format=image/png"
                            + "&request=GetMap&layers="
                            + layer
                            + "&styles=polygon"
                            + "&width=400&height=400&srs=EPSG:3832";

            String disabledRequest = request + "&format_options=advancedProjectionHandling:false";
            String enabledRequest = request + "&format_options=advancedProjectionHandling:true";

            BufferedImage image = getAsImage(request, "image/png");
            // with APH, we should get a gap
            assertPixel(image, 200, 200, Color.WHITE);

            // APH enabled in the GUI, disabled in the request
            image = getAsImage(disabledRequest, "image/png");
            // expect it to cross the image
            assertPixel(image, 200, 200, new Color(170, 170, 170));

            // APH enabled in the GUI, explictly enabled in the request
            image = getAsImage(enabledRequest, "image/png");
            assertPixel(image, 200, 200, Color.WHITE);

            wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, Boolean.FALSE);
            gs.save(wms);
            image = getAsImage(request, "image/png");
            assertPixel(image, 200, 200, new Color(170, 170, 170));

            // APH disabled in the GUI, disabled in the request
            image = getAsImage(disabledRequest, "image/png");
            // expect it to cross the image
            assertPixel(image, 200, 200, new Color(170, 170, 170));

            // APH disabled in the GUI, explictly enabled in the request
            image = getAsImage(enabledRequest, "image/png");
            // does not override admin disabled.
            assertPixel(image, 200, 200, new Color(170, 170, 170));
        } finally {
            wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, original);
            gs.save(wms);
        }
    }

    @Test
    public void testJpegPngTransparent() throws Exception {
        String request =
                "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fvnd.jpeg-png&TRANSPARENT=true&STYLES"
                        + "&LAYERS=cite%3ABasicPolygons&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-2.4%2C1.4%2C0.4%2C4.2";
        // checks it's a PNG
        BufferedImage image = getAsImage(request, "image/png");
        assertNotBlank("testJpegPngTransparent", image);
    }

    @Test
    public void testJpegPng8Transparent() throws Exception {
        String request =
                "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fvnd.jpeg-png8&TRANSPARENT=true&STYLES"
                        + "&LAYERS=cite%3ABasicPolygons&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-2.4%2C1.4%2C0.4%2C4.2";
        // checks it's a PNG
        MockHttpServletResponse resp = getAsServletResponse(request);
        assertEquals("image/png", resp.getContentType());
        assertEquals(
                "inline; filename=cite-BasicPolygons.png",
                resp.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        BufferedImage image = ImageIO.read(getBinaryInputStream(resp));
        assertNotBlank("testJpegPngTransparent", image);
        // check it's paletted
        assertThat(image.getColorModel(), instanceOf(IndexColorModel.class));
    }

    @Test
    public void testJpegPngOpaque() throws Exception {
        String request =
                "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fvnd.jpeg-png&TRANSPARENT=true&STYLES"
                        + "&LAYERS=cite%3ABasicPolygons&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-0.4%2C3.6%2C1%2C5";
        // checks it's a JPEG, since it's opaque
        MockHttpServletResponse resp = getAsServletResponse(request);
        assertEquals("image/jpeg", resp.getContentType());
        assertEquals(
                "inline; filename=cite-BasicPolygons.jpg",
                resp.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        InputStream is = getBinaryInputStream(resp);
        BufferedImage image = ImageIO.read(is);
        assertNotBlank("testJpegPngOpaque", image);
    }

    @Test
    public void testJpegPng8Opaque() throws Exception {
        String request =
                "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fvnd.jpeg-png8&TRANSPARENT=true&STYLES"
                        + "&LAYERS=cite%3ABasicPolygons&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-0.4%2C3.6%2C1%2C5";
        // checks it's a JPEG, since it's opaque
        BufferedImage image = getAsImage(request, "image/jpeg");
        assertNotBlank("testJpegPngOpaque", image);
    }

    @Test
    public void testJpegPngEmpty() throws Exception {
        String request =
                "wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fvnd.jpeg-png&TRANSPARENT=true&STYLES"
                        + "&LAYERS=cite%3ABasicPolygons&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-1.9%2C1.8%2C-1.3%2C2.5";
        // checks it's a PNG
        BufferedImage image = getAsImage(request, "image/png");
        assertBlank("testJpegPngEmpty", image, new Color(255, 255, 255, 0));
    }

    @Test
    public void testFeatureIdMultipleLayers() throws Exception {
        String lakes = getLayerId(MockData.LAKES);
        String places = getLayerId(MockData.NAMED_PLACES);

        String urlSingle =
                "wms?LAYERS="
                        + lakes
                        + "&STYLES=&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
        BufferedImage imageLakes = getAsImage(urlSingle, "image/png");

        // ask with featureid filter against two layers... used to fail
        String url =
                "wms?LAYERS="
                        + lakes
                        + ","
                        + places
                        + "&STYLES=&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010"
                        + "&featureId=Lakes.1107531835962";
        BufferedImage imageLakesPlaces = getAsImage(url, "image/png");

        // should be the same image, the second request filters out anything in "places"
        ImageAssert.assertEquals(imageLakes, imageLakesPlaces, 0);
    }

    @Test
    public void testGetMapOpaqueGroup() throws Exception {
        String url =
                "wms?LAYERS="
                        + OPAQUE_GROUP
                        + "&STYLES=&FORMAT=image%2Fpng"
                        + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-0.0043,-0.0025,0.0043,0.0025";
        BufferedImage imageGroup = getAsImage(url, "image/png");

        ImageAssert.assertEquals(
                new File("./src/test/resources/org/geoserver/wms/wms_1_1_1/opaqueGroup.png"),
                imageGroup,
                300);
    }

    @Test
    public void testGetMapLayersInOpaqueGroup() throws Exception {
        LayerGroupInfo group = getCatalog().getLayerGroupByName(OPAQUE_GROUP);
        for (PublishedInfo pi : group.layers()) {
            String url =
                    "wms?LAYERS="
                            + pi.prefixedName()
                            + "&STYLES=&FORMAT=image%2Fpng"
                            + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=-0.0043,-0.0025,0.0043,0.0025";
            Document dom = getAsDOM(url);
            // print(dom);

            // should not be found
            XMLAssert.assertXpathEvaluatesTo("1", "count(/ServiceExceptionReport)", dom);
            XMLAssert.assertXpathEvaluatesTo("layers", "//ServiceException/@locator", dom);
            XMLAssert.assertXpathEvaluatesTo("LayerNotDefined", "//ServiceException/@code", dom);
        }
    }

    @Test
    public void testReprojectRGBTransparent() throws Exception {
        // UTM53N, close enough to tasmania but sure to add rotation
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers="
                                + getLayerId(MockData.TASMANIA_BM)
                                + "&SRS=EPSG:32753&format=image/png&transparent=true",
                        "image/png");

        // it's transparent
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(4, image.getSampleModel().getNumBands());
        // assert pixels in the 4 corners, the rotation should have made them all transparent
        assertPixelIsTransparent(image, 0, 0);
        assertPixelIsTransparent(image, image.getWidth() - 1, 0);
        assertPixelIsTransparent(image, image.getWidth() - 1, image.getHeight() - 1);
        assertPixelIsTransparent(image, 0, image.getHeight() - 1);
    }

    @Test
    public void testReprojectRGBWithBgColor() throws Exception {
        // UTM53N, close enough to tasmania but sure to add rotation
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers="
                                + getLayerId(MockData.TASMANIA_BM)
                                + "&SRS=EPSG:32753&format=image/png&bgcolor=#FF0000",
                        "image/png");

        // it's not transparent
        assertFalse(image.getColorModel().hasAlpha());
        assertEquals(3, image.getSampleModel().getNumBands());
        // assert pixels in the 4 corners, the rotation should have made them all red
        assertPixel(image, 0, 0, Color.RED);
        assertPixel(image, image.getWidth() - 1, 0, Color.RED);
        assertPixel(image, image.getWidth() - 1, image.getHeight() - 1, Color.RED);
        assertPixel(image, 0, image.getHeight() - 1, Color.RED);
    }

    @Test
    public void testReprojectedDemWithTransparency() throws Exception {
        // UTM53N, close enough to tasmania but sure to add rotation
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers="
                                + getLayerId(MockData.TASMANIA_DEM)
                                + "&styles=demTranslucent&SRS=EPSG:32753&format=image/png&transparent=true",
                        "image/png");

        // RenderedImageBrowser.showChain(image);

        // it's transparent
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(1, image.getSampleModel().getNumBands());
        // assert pixels in the 4 corners, the rotation should have made them all dark gray
        assertPixelIsTransparent(image, 0, 0);
        assertPixelIsTransparent(image, image.getWidth() - 1, 0);
        assertPixelIsTransparent(image, image.getWidth() - 1, image.getHeight() - 1);
        assertPixelIsTransparent(image, 0, image.getHeight() - 1);
    }

    @Test
    public void testDemWithBgColor() throws Exception {
        // UTM53N, close enough to tasmania but sure to add rotation
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers="
                                + getLayerId(MockData.TASMANIA_DEM)
                                + "&styles=demTranslucent&SRS=EPSG:32753&format=image/png&bgcolor=#404040",
                        "image/png");

        // RenderedImageBrowser.showChain(image);

        // it's transparent
        assertFalse(image.getColorModel().hasAlpha());
        assertEquals(1, image.getSampleModel().getNumBands());
        // assert pixels in the 4 corners, the rotation should have made them all dark gray
        assertPixel(image, 0, 0, Color.DARK_GRAY);
        assertPixel(image, image.getWidth() - 1, 0, Color.DARK_GRAY);
        assertPixel(image, image.getWidth() - 1, image.getHeight() - 1, Color.DARK_GRAY);
        assertPixel(image, 0, image.getHeight() - 1, Color.DARK_GRAY);
    }

    @Test
    public void testMaskedNoAPH() throws Exception {
        // used to fail when hitting a tiff with ROI with reprojection (thus buffer) in an area
        // close to, but not hitting, the ROI
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        Serializable oldValue = wms.getMetadata().get(WMS.ADVANCED_PROJECTION_KEY);
        try {
            wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, false);
            gs.save(wms);

            BufferedImage image =
                    getAsImage(
                            "wms/reflect?layers="
                                    + getLayerId(MASKED)
                                    + "&SRS=AUTO%3A97002%2C9001%2C-1%2C40&BBOX=694182%2C-4631295%2C695092%2C-4630379&format=image/png&transparent=true",
                            "image/png");
            // transparent model
            assertTrue(image.getColorModel().hasAlpha());
            assertThat(image.getColorModel(), instanceOf(ComponentColorModel.class));
            double[] maximums = new ImageWorker(image).getMaximums();
            // last band, alpha, is fully at zero, so transparent
            assertEquals(0, maximums[maximums.length - 1], 0d);

        } finally {
            wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, oldValue);
            gs.save(wms);
        }
    }

    @Test
    public void testRTAndBandSelection() throws Exception {
        String url =
                "wms?LAYERS=mosaic_shuffle&styles=jiffleBandSelect"
                        + "&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=7,37,11,41&WIDTH=100&HEIGHT=200&bgcolor=0xFF0000";
        // used to go NPE
        BufferedImage jiffleBandSelected = getAsImage(url, "image/png");
        ImageAssert.assertEquals(
                new File("./src/test/resources/org/geoserver/wms/wms_1_1_1/jiffleBandSelected.png"),
                jiffleBandSelected,
                300);
    }

    @Test
    public void testWFSNGReprojection() throws Exception {
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();

        URL descURL =
                new URL(baseURL + "/wfs?REQUEST=DescribeFeatureType&VERSION=1.1.0&SERVICE=WFS");
        client.expectGet(
                descURL,
                new MockHttpResponse(
                        getClass().getResource("/geoserver/wfs-ng/desc_110.xml"), "text/xml"));

        URL descFeatureURL =
                new URL(
                        baseURL
                                + "/wfs?NAMESPACE=xmlns%28topp%3Dhttp%3A%2F%2Fwww.topp.com%29&TYPENAME=topp%3Aroads22&REQUEST=DescribeFeatureType&VERSION=1.1.0&SERVICE=WFS");

        client.expectGet(
                descFeatureURL,
                new MockHttpResponse(
                        getClass().getResource("/geoserver/wfs-ng/desc_feature.xml"), "text/xml"));

        URL remoteRequestURL =
                new URL(
                        baseURL
                                + "/wfs?PROPERTYNAME=the_geom&FILTER=%3Cogc%3AFilter+xmlns%3Axs%3D%22http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%22+xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22+xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%3Cogc%3ABBOX%3E%3Cogc%3APropertyName%3Ethe_geom%3C%2Fogc%3APropertyName%3E%3Cgml%3AEnvelope+srsDimension%3D%222%22+srsName%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%2Fsrs%2Fepsg.xml%234326%22%3E%3Cgml%3AlowerCorner%3E-103.882897+44.370304%3C%2Fgml%3AlowerCorner%3E%3Cgml%3AupperCorner%3E-103.617584+44.50476%3C%2Fgml%3AupperCorner%3E%3C%2Fgml%3AEnvelope%3E%3C%2Fogc%3ABBOX%3E%3C%2Fogc%3AFilter%3E&TYPENAME=topp%3Aroads22&REQUEST=GetFeature&RESULTTYPE=RESULTS&OUTPUTFORMAT=text%2Fxml%3B+subtype%3Dgml%2F3.1.1&SRSNAME=EPSG%3A4326&VERSION=1.1.0&SERVICE=WFS");

        client.expectGet(
                remoteRequestURL,
                new MockHttpResponse(
                        getClass().getResource("/geoserver/wfs-ng/wfs_response_4326.xml"),
                        "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        TestHttpClientProvider.bind(client, descFeatureURL);
        TestHttpClientProvider.bind(client, remoteRequestURL);

        // MOCKING Catalog
        URL url = getClass().getResource("/geoserver/wfs-ng/wfs_cap_110.xml");

        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        DataStoreInfo storeInfo = cb.buildDataStore("MockWFSDataStore");
        ((DataStoreInfoImpl) storeInfo).setId("1");
        ((DataStoreInfoImpl) storeInfo).setType("Web Feature Server (NG)");
        ((DataStoreInfoImpl) storeInfo)
                .getConnectionParameters()
                .put(WFSDataStoreFactory.URL.key, url);
        ((DataStoreInfoImpl) storeInfo)
                .getConnectionParameters()
                .put("usedefaultsrs", Boolean.FALSE);
        ((DataStoreInfoImpl) storeInfo)
                .getConnectionParameters()
                .put(WFSDataStoreFactory.PROTOCOL.key, Boolean.FALSE);
        ((DataStoreInfoImpl) storeInfo).getConnectionParameters().put("TESTING", Boolean.TRUE);
        getCatalog().add(storeInfo);

        // MOCKING Feature Type with native CRS EPSG:26713
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        FeatureTypeInfo ftInfo =
                xp.load(
                        getClass().getResourceAsStream("/geoserver/wfs-ng/featuretype.xml"),
                        FeatureTypeInfoImpl.class);
        ((FeatureTypeInfoImpl) ftInfo).setStore(storeInfo);
        ((FeatureTypeInfoImpl) ftInfo).setMetadata(new MetadataMap());
        ftInfo.setSRS("EPSG:26713");
        ftInfo.getMetadata().put(FeatureTypeInfo.OTHER_SRS, "EPSG:4326,EPSG:3857");
        getCatalog().add(ftInfo);

        // setting mock feature type as resource of Layer from Test Data
        LayerInfo layerInfo = getCatalog().getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart());
        layerInfo.setResource(ftInfo);
        layerInfo.setDefaultStyle(getCatalog().getStyleByName("line"));
        // Injecting Mock Http client in WFS Data Store to read mock respones from XML
        DataAccess dac = ftInfo.getStore().getDataStore(null);
        RetypingDataStore retypingDS = (RetypingDataStore) dac;
        WFSDataStore wfsDS = (WFSDataStore) retypingDS.getWrapped();
        wfsDS.getWfsClient().setHttpClient(client);

        getCatalog().save(layerInfo);

        // test starts now
        // a WMS request with EPSG:4326 should result in a remote WFS call with EPSG:4326 filter and
        // response
        // the expected URL can seen in remoteRequestURL
        String wmsUrl =
                "wms?LAYERS=topp_roads22&styles=line"
                        + "&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=-103.87779468316292,44.37288961726252,-103.62268570651278,44.50217396380937"
                        + "&WIDTH=100&HEIGHT=100";

        BufferedImage wfsNGImage = getAsImage(wmsUrl, "image/png");
        // ImageIO.write(wfsNGImage, "png", new File("D://cascaded_wfs_layer_response.png"));
        ImageAssert.assertEquals(
                new File("./src/test/resources/geoserver/wfs-ng/cascaded_wfs_layer_response.png"),
                wfsNGImage,
                300);
    }

    @Test
    public void testVendorOptionClipVector() throws Exception {
        URL exptectedResponse = this.getClass().getResource("../wms_clip_vector.png");
        BufferedImage expectedImage = ImageIO.read(exptectedResponse);

        String polygonWkt =
                "POLYGON((-103.81153231351766%2038.73789567417218,-105.74512606351766%2031.78525172547746,-95.28614168851766%2028.053665204466157,-91.33106356351766%2031.260810654461146,-96.42871981351766%2038.66930662128952,-103.81153231351766%2038.73789567417218))";

        BufferedImage response =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=polygon&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&version=1.1.1"
                                + "&clip="
                                + polygonWkt,
                        "image/png");

        ImageAssert.assertEquals(expectedImage, response, 100);

        String polygonWkt900913 =
                "srid=900913;POLYGON ((-11556246.91561025 4684196.6150700655, -11771493.587261306 3735154.4718813156, -10607204.772421502 3255741.4304766906, -10166927.489498887 3666666.8945377995, -10734395.987488035 4674412.675449564, -11556246.91561025 4684196.6150700655))";
        response =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&styles=polygon&layers="
                                + layers
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&version=1.1.1"
                                + "&clip="
                                + polygonWkt900913,
                        "image/png");
        ImageAssert.assertEquals(expectedImage, response, 100);
    }

    @Test
    public void testVendorOptionClipRaster() throws Exception {

        URL exptectedResponse = this.getClass().getResource("../wms_clip_raster.png");
        BufferedImage expectedImage = ImageIO.read(exptectedResponse);

        // EU south of Schengen
        String rasterMask =
                "POLYGON((-0.4455465239619838 49.03915485780325,27.679453476038034 48.692256255310134,34.53492222603802 32.400173313532584,5.355234726038036 37.161881019039605,-0.4455465239619838 49.03915485780325))";
        String worldBbox = "-53.384768,4.769752,80.121092,57.719733";

        BufferedImage response =
                getAsImage(
                        "wms?bbox="
                                + worldBbox
                                + "&styles=&layers="
                                + "wcs:World"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&version=1.1.1"
                                + "&clip="
                                + rasterMask,
                        "image/png");
        ImageAssert.assertEquals(expectedImage, response, 100);

        String rasterMask900913 =
                "srid=900913;POLYGON ((-49598.01217216109 6281507.767506711, 3081262.66638866 6222804.1297836965, 3844409.956787858 3815954.983140064, 596142.0027810101 4461694.998093233, -49598.01217216109 6281507.767506711))";

        response =
                getAsImage(
                        "wms?bbox="
                                + worldBbox
                                + "&styles=&layers="
                                + "wcs:World"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326"
                                + "&version=1.1.1"
                                + "&clip="
                                + rasterMask900913,
                        "image/png");

        ImageAssert.assertEquals(expectedImage, response, 100);
    }

    @Test
    public void testVendorOptionClipMosaic() throws Exception {
        String clipPolygon = "POLYGON ((8 38, 10 38, 10 40, 8 40, 8 38))";
        String url =
                "wms?LAYERS=mosaic&"
                        + "&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=7,37,11,41&WIDTH=200&HEIGHT=200&bgcolor=0xFF0000"
                        + "&CLIP="
                        + clipPolygon;
        BufferedImage response = getAsImage(url, "image/png");

        File expected = new File("./src/test/resources/org/geoserver/wms/wms_clip_mosaic.png");
        ImageAssert.assertEquals(expected, response, 100);
    }

    @Test
    public void testLayoutLegendStyleOnlineResource() throws Exception {
        Catalog catalog = getCatalog();
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("../test-layout-legend-image.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-legend-image.xml"));
        File styles = getDataDirectory().findOrCreateDir("styles");
        URL grassPng = GetMapIntegrationTest.class.getResource("../red_fill.png");
        FileUtils.copyURLToFile(grassPng, new File(styles, "org/geoserver/wms/red_fill.png"));
        FeatureTypeInfo giantPolygon = catalog.getFeatureTypeByName("giantPolygon");

        StyleInfo sInfo = catalog.getLayerByName(giantPolygon.getName()).getDefaultStyle();
        LegendInfoImpl legend = new LegendInfoImpl();
        legend.setOnlineResource("org/geoserver/wms/red_fill.png");
        legend.setFormat("image/png;charset=utf-8");
        legend.setHeight(32);
        legend.setWidth(32);
        sInfo.setLegend(legend);
        catalog.save(sInfo);
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:giantPolygon"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=150"
                                + "&legend_options=fontName:Bitstream Vera Sans"
                                + "&srs=EPSG:4326&format_options=layout:test-layout-legend-image",
                        "image/png");

        URL expectedResponse = getClass().getResource("giant_poly_legend_static_res.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 2000);
        sInfo.setLegend(null);
        catalog.save(sInfo);
    }

    @Test
    public void testLayoutLegendStyleTextFitBox() throws Exception {
        Catalog catalog = getCatalog();
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("../test-layout-legend-image.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-legend-image.xml"));
        FeatureTypeInfo giantPolygon = catalog.getFeatureTypeByName("giantPolygon");

        StyleInfo sInfo = catalog.getLayerByName(giantPolygon.getName()).getDefaultStyle();
        LegendInfoImpl legend = new LegendInfoImpl();
        legend.setFormat("image/png;charset=utf-8");
        legend.setHeight(32);
        legend.setWidth(32);
        sInfo.setLegend(legend);
        catalog.save(sInfo);
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:giantPolygon"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=150"
                                + "&legend_options=fontName:Bitstream Vera Sans"
                                + "&srs=EPSG:4326&format_options=layout:test-layout-legend-image",
                        "image/png");

        URL expectedResponse = getClass().getResource("giant_poly_legend.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 1500);
        sInfo.setLegend(null);
        catalog.save(sInfo);
    }

    @Test
    public void testLayoutLegendWithSpecifiedTargetSize() throws Exception {
        // checking a legend decoration with a custom size bigger than the
        // map size; expecting that the specified legend size is picked up,
        // while resizing it accordingly to map dimension
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("../test-layout-with-size.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-with-size.xml"));
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:giantPolygon"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=150"
                                + "&legend_options=fontName:Bitstream Vera Sans"
                                + "&srs=EPSG:4326&format_options=layout:test-layout-with-size",
                        "image/png");

        URL expectedResponse = getClass().getResource("giant_poly_big_legend.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 1500);
    }

    @Test
    public void testLegendDecoratorWithRaster() throws Exception {

        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("../test-layout-legend-image.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-legend-image.xml"));
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers="
                                + getLayerId(MockData.TASMANIA_DEM)
                                + "&format_options=layout:test-layout-legend-image&styles=demTranslucent&SRS=EPSG:32753&format=image/png&bgcolor=#404040",
                        "image/png");

        URL expectedResponse = getClass().getResource("dem_with_legend.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);

        ImageAssert.assertEquals(image, expectedImage, 3400);
    }

    @Test
    public void testLayoutLegendStyleWithLargeOnlineResource() throws Exception {
        Catalog catalog = getCatalog();
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout = GetMapIntegrationTest.class.getResource("../test-layout-legend-image.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-legend-image.xml"));
        File styles = getDataDirectory().findOrCreateDir("styles");
        URL grassPng = GetMapIntegrationTest.class.getResource("../large_legend_res.png");
        FileUtils.copyURLToFile(
                grassPng, new File(styles, "org/geoserver/wms/large_legend_res.png"));
        FeatureTypeInfo giantPolygon = catalog.getFeatureTypeByName("giantPolygon");

        StyleInfo sInfo = catalog.getLayerByName(giantPolygon.getName()).getDefaultStyle();
        LegendInfoImpl legend = new LegendInfoImpl();
        legend.setOnlineResource("org/geoserver/wms/large_legend_res.png");
        legend.setFormat("image/png;charset=utf-8");
        legend.setHeight(80);
        legend.setWidth(640);
        sInfo.setLegend(legend);
        catalog.save(sInfo);
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:giantPolygon"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=150"
                                + "&legend_options=fontName:Bitstream Vera Sans"
                                + "&srs=EPSG:4326&format_options=layout:test-layout-legend-image",
                        "image/png");
        URL expectedResponse = getClass().getResource("giant_poly_big_res.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 2300);
        sInfo.setLegend(null);
        catalog.save(sInfo);
    }

    @Test
    public void testLayoutLegendStyleWithOnlineResourceAndCustomWidth() throws Exception {
        Catalog catalog = getCatalog();
        File layouts = getDataDirectory().findOrCreateDir("layouts");
        URL layout =
                GetMapIntegrationTest.class.getResource("../test-layout-legend-image-size.xml");
        FileUtils.copyURLToFile(layout, new File(layouts, "test-layout-legend-image-size.xml"));
        File styles = getDataDirectory().findOrCreateDir("styles");
        URL grassPng = GetMapIntegrationTest.class.getResource("../large_legend_res.png");
        FileUtils.copyURLToFile(
                grassPng, new File(styles, "org/geoserver/wms/large_legend_res.png"));
        FeatureTypeInfo giantPolygon = catalog.getFeatureTypeByName("giantPolygon");

        StyleInfo sInfo = catalog.getLayerByName(giantPolygon.getName()).getDefaultStyle();
        LegendInfoImpl legend = new LegendInfoImpl();
        legend.setOnlineResource("org/geoserver/wms/large_legend_res.png");
        legend.setFormat("image/png;charset=utf-8");
        legend.setHeight(80);
        legend.setWidth(640);
        sInfo.setLegend(legend);
        catalog.save(sInfo);
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:giantPolygon"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=150"
                                + "&legend_options=fontName:Bitstream Vera Sans"
                                + "&srs=EPSG:4326&format_options=layout:test-layout-legend-image-size",
                        "image/png");
        URL expectedResponse = getClass().getResource("giant_poly_big_res_size.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 1500);
        sInfo.setLegend(null);
        catalog.save(sInfo);
    }
}
