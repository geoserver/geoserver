/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo.PngEncoderType;
import org.geoserver.data.test.SystemTestData;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.image.test.ImageAssert;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CollectionLayerTest extends OSEORestTestSupport {

    private String resourceBase;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
        namespaces.put("wms", "http://www.opengis.net/wms");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // PNGJ has bugs with band selected data, fall back on JDK
        super.onSetUp(testData);

        GeoServer gs = getGeoServer();
        GeoServerInfo gsInfo = gs.getGlobal();
        gsInfo.getJAI().setPngEncoderType(PngEncoderType.JDK);
        gs.save(gsInfo);
    }

    @Before
    public void setupTestWorkspace() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName("test");
        if (ws == null) {
            ws = catalog.getFactory().createWorkspace();
            ws.setName("test");
            catalog.add(ws);
            NamespaceInfo ns = catalog.getFactory().createNamespace();
            ns.setPrefix("test");
            ns.setURI("http://geoserver.org/test");
            catalog.add(ns);
        }
    }

    @After
    public void cleanupTestWorkspace() throws Exception {
        Catalog catalog = getCatalog();
        CascadeDeleteVisitor remover = new CascadeDeleteVisitor(catalog);
        WorkspaceInfo ws = catalog.getWorkspaceByName("test");
        if (ws != null) {
            remover.visit(ws);
        }
    }

    @Before
    public void setupTestCollectionAndProduct() throws IOException, Exception {
        // create the collection
        createTest123Collection();

        // create the product
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections/TEST123/products",
                        getTestData("/test123-product.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/TEST123/products/TEST123_P1",
                response.getHeader("location"));

        // setup the base granule location
        File file = new File("./src/test/resources");
        resourceBase = file.getCanonicalFile().getAbsolutePath().replace("\\", "/");
    }

    protected String getTestStringData(String location) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(location), "UTF-8");
    }

    @Test
    public void testCreateCollectionSimpleLayer() throws Exception {
        // setup the granules
        setupDefaultLayer(
                "/test123-product-granules-rgb.json",
                "/test123-layer-simple.json",
                "gs",
                Boolean.FALSE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs", "test123", new String[] {"RED_BAND", "GREEN_BAND", "BLUE_BAND"});
        // ... its style is the default one
        assertThat(layer.getDefaultStyle().getName(), equalTo("raster"));

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-simple-rgb.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testCreateCollectionSimpleLayerTestWorkspace() throws Exception {
        // setup the granules
        setupDefaultLayer(
                "/test123-product-granules-rgb.json",
                "/test123-layer-simple-testws.json",
                "test",
                Boolean.FALSE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "test", "test123", new String[] {"RED_BAND", "GREEN_BAND", "BLUE_BAND"});
        // ... its style is the default one
        assertThat(layer.getDefaultStyle().getName(), equalTo("raster"));

        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers=test:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-simple-rgb.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testCreateCollectionSimpleLayerWithCustomStyle() throws Exception {
        // setup the granules
        setupDefaultLayer(
                "/test123-product-granules-rgb.json",
                "/test123-layer-simple-graystyle.json",
                "gs",
                Boolean.FALSE);
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs", "test123", new String[] {"RED_BAND", "GREEN_BAND", "BLUE_BAND"});

        // ... its style is a gray one based on the RED band
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        ChannelSelection cs = getChannelSelection(layer);
        assertNull(cs.getRGBChannels()[0]);
        assertNull(cs.getRGBChannels()[1]);
        assertNull(cs.getRGBChannels()[2]);
        assertEquals("1", cs.getGrayChannel().getChannelName().evaluate(null, String.class));

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-simple-gray.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    private ChannelSelection getChannelSelection(LayerInfo layer) throws IOException {
        Style style = layer.getDefaultStyle().getStyle();
        List<FeatureTypeStyle> fts = style.featureTypeStyles();
        assertEquals(1, fts.size());
        List<Rule> rules = fts.get(0).rules();
        assertEquals(1, rules.size());
        List<Symbolizer> symbolizers = rules.get(0).symbolizers();
        assertEquals(1, symbolizers.size());
        RasterSymbolizer rs = (RasterSymbolizer) symbolizers.get(0);
        return rs.getChannelSelection();
    }

    private LayerInfo validateBasicLayerStructure(
            String workspace, String layerName, String[] expectedNames) {
        // check the configuration elements are there too
        Catalog catalog = getCatalog();
        // ... the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName(workspace, layerName);
        assertNotNull(store);
        assertThat(store.getFormat(), instanceOf(ImageMosaicFormat.class));
        // ... the layer
        LayerInfo layer = catalog.getLayerByName(workspace + ":" + layerName);
        assertNotNull(layer);
        final CoverageInfo coverageInfo = (CoverageInfo) layer.getResource();
        assertThat(coverageInfo.getStore(), equalTo(store));
        // ... the resource is time enabled
        DimensionInfo dimension =
                coverageInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        assertThat(dimension.getAttribute(), equalTo("timeStart"));
        assertThat(dimension.getDefaultValue().getStrategyType(), equalTo(Strategy.MAXIMUM));
        // ... has the expected bands
        String[] names =
                coverageInfo
                        .getDimensions()
                        .stream()
                        .map(cd -> cd.getName())
                        .toArray(String[]::new);
        assertThat(expectedNames, equalTo(names));

        return layer;
    }

    @Test
    public void testCreateCollectionMultiband() throws Exception {
        // setup the granules
        setupDefaultLayer(
                "/test123-product-granules-multiband.json",
                "/test123-layer-multiband.json",
                "gs",
                Boolean.TRUE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs", "test123", new String[] {"B02", "B03", "B04", "B08"});

        // ... its style is a RGB one based on the B2, B3, B4
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        ChannelSelection cs = getChannelSelection(layer);
        assertEquals("4", cs.getRGBChannels()[0].getChannelName().evaluate(null, String.class));
        assertEquals("2", cs.getRGBChannels()[1].getChannelName().evaluate(null, String.class));
        assertEquals("1", cs.getRGBChannels()[2].getChannelName().evaluate(null, String.class));
        assertNull(cs.getGrayChannel());

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-multiband.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    /** This test checks it's possible to change an existing configuration and stuff still works */
    @Test
    public void testModifyConfigurationSingleBand() throws Exception {
        // setup and check one collection
        testCreateCollectionSimpleLayer();

        // now go and setup another single collection
        testCreateCollectionMultiband();

        // switch to multiband
        testCreateCollectionMultiband();

        // and back to single
        testCreateCollectionSimpleLayer();
    }

    @Test
    public void testBandsFlagsAll() throws Exception {
        setupDefaultLayer(
                "/test123-product-granules-bands-flags.json",
                "/test123-layer-bands-flags-all.json",
                "gs",
                Boolean.TRUE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs",
                        "test123",
                        new String[] {
                            "VNIR_0", "VNIR_1", "VNIR_2", "QUALITY", "CLOUDSHADOW", "HAZE", "SNOW"
                        });
        // ... its style has been generated
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        // ... and it uses all VNIR bands
        ChannelSelection cs = getChannelSelection(layer);
        assertEquals("1", cs.getRGBChannels()[0].getChannelName().evaluate(null, String.class));
        assertEquals("2", cs.getRGBChannels()[1].getChannelName().evaluate(null, String.class));
        assertEquals("3", cs.getRGBChannels()[2].getChannelName().evaluate(null, String.class));
        assertNull(cs.getGrayChannel());

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-vnir.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testBandsFlagsMix() throws Exception {
        setupDefaultLayer(
                "/test123-product-granules-bands-flags.json",
                "/test123-layer-bands-flags-browseMix.json",
                "gs",
                Boolean.TRUE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs",
                        "test123",
                        new String[] {
                            "VNIR_0", "VNIR_1", "VNIR_2", "QUALITY", "CLOUDSHADOW", "HAZE", "SNOW"
                        });
        // ... its style has been generated
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        // ... and it uses all two vnir bands and a flag
        ChannelSelection cs = getChannelSelection(layer);
        assertEquals("1", cs.getRGBChannels()[0].getChannelName().evaluate(null, String.class));
        assertEquals("2", cs.getRGBChannels()[1].getChannelName().evaluate(null, String.class));
        assertEquals("7", cs.getRGBChannels()[2].getChannelName().evaluate(null, String.class));
        assertNull(cs.getGrayChannel());

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-vnir-snow.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testBandsFlagsGrayFlag() throws Exception {
        setupDefaultLayer(
                "/test123-product-granules-bands-flags.json",
                "/test123-layer-bands-flags-grayFlag.json",
                "gs",
                Boolean.TRUE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs",
                        "test123",
                        new String[] {
                            "VNIR_0", "VNIR_1", "VNIR_2", "QUALITY", "CLOUDSHADOW", "HAZE", "SNOW"
                        });
        // ... its style has been generated
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        // ... and it uses only a gray band, the snow flag
        ChannelSelection cs = getChannelSelection(layer);
        assertNull(cs.getRGBChannels()[0]);
        assertNull(cs.getRGBChannels()[1]);
        assertNull(cs.getRGBChannels()[2]);
        assertEquals("7", cs.getGrayChannel().getChannelName().evaluate(null, String.class));

        // the image is almost black, but not fully
        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-gray-snow.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    private void setupDefaultLayer(
            String granuleLocations,
            String layerDefinition,
            String workspace,
            Boolean expectSeparateBands)
            throws Exception {
        // setup the granules
        String granulesTemplate = getTestStringData(granuleLocations);
        String granules = granulesTemplate.replace("$resources", resourceBase);
        MockHttpServletResponse response =
                putAsServletResponse(
                        "/rest/oseo/collections/TEST123/products/TEST123_P1/granules",
                        granules,
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        // create the layer
        boolean previousConfiguration =
                getAsServletResponse("rest/oseo/collections/TEST123/layer").getStatus()
                        == HttpStatus.OK.value();
        response =
                putAsServletResponse(
                        "rest/oseo/collections/TEST123/layer",
                        getTestData(layerDefinition),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(previousConfiguration ? 200 : 201, response.getStatus());

        // check it has been created from REST
        DocumentContext json = getAsJSONPath("rest/oseo/collections/TEST123/layer", 200);
        assertEquals(workspace, json.read("$.workspace"));
        assertEquals("test123", json.read("$.layer"));
        assertEquals(expectSeparateBands, json.read("$.separateBands"));
        assertEquals(Boolean.TRUE, json.read("$.heterogeneousCRS"));
    }

    @Test
    public void testGetCollectionDefaultLayer() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2/layer", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("sentinel2", json.read("$.layer"));
        assertEquals(Integer.valueOf(12), json.read("$.bands.length()"));
        assertEquals(Boolean.TRUE, json.read("$.separateBands"));
        assertEquals("B01", json.read("$.bands[0]"));
        assertEquals(Integer.valueOf(3), json.read("$.browseBands.length()"));
        assertEquals("B04", json.read("$.browseBands[0]"));
        assertEquals(Boolean.TRUE, json.read("$.heterogeneousCRS"));
        assertEquals("EPSG:4326", json.read("$.mosaicCRS"));
    }

    @Test
    public void testGetCollectionLayers() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/LANDSAT8/layers", 200);
        assertEquals(Integer.valueOf(2), json.read("$.layers.length()"));
        assertEquals(
                Arrays.asList(
                        "http://localhost:8080/geoserver/rest/oseo/collections/LANDSAT8/layers/landsat8-SINGLE"),
                json.read("$.layers[?(@.name == 'landsat8-SINGLE')].href"));
        assertEquals(
                Arrays.asList(
                        "http://localhost:8080/geoserver/rest/oseo/collections/LANDSAT8/layers/landsat8-SEPARATE"),
                json.read("$.layers[?(@.name == 'landsat8-SEPARATE')].href"));
    }

    @Test
    public void testGetCollectionLayerByName() throws Exception {
        DocumentContext json =
                getAsJSONPath("/rest/oseo/collections/LANDSAT8/layers/landsat8-SINGLE", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("landsat8-SINGLE", json.read("$.layer"));
        assertEquals(Boolean.FALSE, json.read("$.separateBands"));
        assertEquals(Boolean.TRUE, json.read("$.heterogeneousCRS"));
        assertEquals("EPSG:4326", json.read("$.mosaicCRS"));
    }

    @Test
    public void testDeleteCollectionLayer() throws Exception {
        // create something to delete
        setupDefaultLayer(
                "/test123-product-granules-rgb.json",
                "/test123-layer-simple.json",
                "gs",
                Boolean.FALSE);

        // check the GeoServer layer is there
        assertNotNull(getCatalog().getLayerByName("test123"));

        // remove
        MockHttpServletResponse response =
                deleteAsServletResponse("rest/oseo/collections/TEST123/layer");
        assertEquals(200, response.getStatus());

        // no more there on REST API and on catalog
        response = getAsServletResponse("rest/oseo/collections/TEST123/layer");
        assertEquals(404, response.getStatus());
        assertNull(getCatalog().getLayerByName("test123"));
    }

    @Test
    public void testAddSecondLayerByPut() throws Exception {
        // add the first layer and granules
        setupDefaultLayer(
                "/test123-product-granules-bands-flags.json",
                "/test123-layer-bands-flags-grayFlag.json",
                "gs",
                Boolean.TRUE);

        // confirm on layer on the list
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123/layers", 200);
        assertEquals(Integer.valueOf(1), json.read("$.layers.length()"));

        // now add another layer
        MockHttpServletResponse response =
                putAsServletResponse(
                        "rest/oseo/collections/TEST123/layers/test123-secondary",
                        getTestData("/test123-layer-bands-flags-browseMix-secondary.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());

        // confirm there are two layers on list now
        json = getAsJSONPath("/rest/oseo/collections/TEST123/layers", 200);
        assertEquals(Integer.valueOf(2), json.read("$.layers.length()"));
        checkTest123SecondaryLayer();
        return;
    }

    private void checkTest123SecondaryLayer() throws Exception {
        DocumentContext json; // check it has been created from REST
        json = getAsJSONPath("rest/oseo/collections/TEST123/layers/test123-secondary", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("test123-secondary", json.read("$.layer"));
        assertEquals(Boolean.TRUE, json.read("$.separateBands"));
        assertEquals(Boolean.TRUE, json.read("$.heterogeneousCRS"));

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs",
                        "test123-secondary",
                        new String[] {
                            "VNIR_0", "VNIR_1", "VNIR_2", "QUALITY", "CLOUDSHADOW", "HAZE", "SNOW"
                        });
        // ... its style has been generated
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123-secondary"));
        // ... and it uses only a gray band, the snow flag
        ChannelSelection cs = getChannelSelection(layer);
        assertEquals("1", cs.getRGBChannels()[0].getChannelName().evaluate(null, String.class));
        assertEquals("2", cs.getRGBChannels()[1].getChannelName().evaluate(null, String.class));
        assertEquals("7", cs.getRGBChannels()[2].getChannelName().evaluate(null, String.class));
        assertNull(cs.getGrayChannel());

        // the image is almost black, but not fully
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers=gs:test123-secondary&format=image/png&width=200",
                        "image/png");
        File expected = new File("src/test/resources/test123-vnir-snow.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testAddSecondLayerByPost() throws Exception {
        // add the first layer and granules
        setupDefaultLayer(
                "/test123-product-granules-bands-flags.json",
                "/test123-layer-bands-flags-grayFlag.json",
                "gs",
                Boolean.TRUE);

        // confirm on layer on the list
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123/layers", 200);
        assertEquals(Integer.valueOf(1), json.read("$.layers.length()"));

        // now add another layer
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections/TEST123/layers",
                        getTestData("/test123-layer-bands-flags-browseMix-secondary.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());

        // confirm there are two layers on list now
        json = getAsJSONPath("/rest/oseo/collections/TEST123/layers", 200);
        assertEquals(Integer.valueOf(2), json.read("$.layers.length()"));

        // check it has been created from REST
        checkTest123SecondaryLayer();
    }

    @Test
    public void testRemoveLayer() throws Exception {
        // add two layers
        testAddSecondLayerByPost();

        getAsJSONPath("/rest/oseo/collections/TEST123/layers", 200);

        // now go and remove the first, which was the default one
        MockHttpServletResponse response =
                deleteAsServletResponse("rest/oseo/collections/TEST123/layers/test123");
        assertEquals(200, response.getStatus());

        // check it got removed from list
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123/layers", 200);
        assertEquals(Integer.valueOf(1), json.read("$.layers.length()"));
        assertEquals(
                Arrays.asList(
                        "http://localhost:8080/geoserver/rest/oseo/collections/TEST123/layers/test123-secondary"),
                json.read("$.layers[?(@.name == 'test123-secondary')].href"));

        // check it's a 404 on direct request
        response = getAsServletResponse("rest/oseo/collections/TEST123/layers/test123");
        assertEquals(404, response.getStatus());

        // check the other layer is now the default
        json = getAsJSONPath("rest/oseo/collections/TEST123/layer", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("test123-secondary", json.read("$.layer"));
        assertEquals(Boolean.TRUE, json.read("$.separateBands"));
        assertEquals(Boolean.TRUE, json.read("$.heterogeneousCRS"));

        // the image is almost black, but not fully
        BufferedImage image =
                getAsImage(
                        "wms/reflect?layers=gs:test123-secondary&format=image/png&width=200",
                        "image/png");
        File expected = new File("src/test/resources/test123-vnir-snow.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testCreateTimeRangesSimpleLayer() throws Exception {
        // setup the granules
        setupDefaultLayer(
                "/test123-product-granules-rgb.json",
                "/test123-layer-timerange.json",
                "gs",
                Boolean.FALSE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs", "test123", new String[] {"RED_BAND", "GREEN_BAND", "BLUE_BAND"});
        // ... its style is the default one
        assertThat(layer.getDefaultStyle().getName(), equalTo("raster"));

        // get the capabilites and check the times are indeed ranges
        Document dom = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("2018-01-01T02:00:00Z", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo(
                "2018-01-01T00:00:00.000Z/2018-01-01T02:00:00.000Z/PT1S",
                "//wms:Layer/wms:Dimension",
                dom);
    }

    @Test
    public void testCreateTimeRangesMultiband() throws Exception {
        // setup the granules
        setupDefaultLayer(
                "/test123-product-granules-multiband.json",
                "/test123-layer-multiband-timerange.json",
                "gs",
                Boolean.TRUE);

        // check the configuration elements are there too
        LayerInfo layer =
                validateBasicLayerStructure(
                        "gs", "test123", new String[] {"B02", "B03", "B04", "B08"});

        // get the capabilites and check the times are indeed ranges
        Document dom = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        // print(dom);
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("2018-01-01T02:00:00Z", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo(
                "2018-01-01T00:00:00.000Z/2018-01-01T02:00:00.000Z/PT1S",
                "//wms:Layer/wms:Dimension",
                dom);
    }
}
