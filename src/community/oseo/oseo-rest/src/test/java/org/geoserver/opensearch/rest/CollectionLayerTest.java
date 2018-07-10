/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

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
import java.util.List;
import org.apache.commons.io.IOUtils;
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

public class CollectionLayerTest extends OSEORestTestSupport {

    private String resourceBase;

    @Override
    protected String getLogConfiguration() {
        // return "/GEOTOOLS_DEVELOPER_LOGGING.properties";
        return super.getLogConfiguration();
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
        resourceBase = file.getCanonicalFile().getAbsolutePath();
    }

    protected String getTestStringData(String location) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(location));
    }

    @Test
    public void testCreateCollectionSimpleLayer() throws Exception {
        // setup the granules
        String granulesTemplate = getTestStringData("/test123-product-granules-rgb.json");
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
                        getTestData("/test123-layer-simple.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(previousConfiguration ? 200 : 201, response.getStatus());

        // check it has been created from REST
        DocumentContext json = getAsJSONPath("rest/oseo/collections/TEST123/layer", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("test123", json.read("$.layer"));
        assertEquals(Boolean.FALSE, json.read("$.separateBands"));
        assertEquals(Boolean.FALSE, json.read("$.heterogeneousCRS"));

        // check the configuration elements are there too
        Catalog catalog = getCatalog();
        // ... the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName("gs", "test123");
        assertNotNull(store);
        assertThat(store.getFormat(), instanceOf(ImageMosaicFormat.class));
        // ... the layer
        LayerInfo layer = catalog.getLayerByName("gs:test123");
        assertNotNull(layer);
        final CoverageInfo coverageInfo = (CoverageInfo) layer.getResource();
        assertThat(coverageInfo.getStore(), equalTo(store));
        // ... the resource is time enabled
        DimensionInfo dimension =
                coverageInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        assertThat(dimension.getAttribute(), equalTo("timeStart"));
        assertThat(dimension.getDefaultValue().getStrategyType(), equalTo(Strategy.MAXIMUM));
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
        String granulesTemplate = getTestStringData("/test123-product-granules-rgb.json");
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
                        getTestData("/test123-layer-simple-testws.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(previousConfiguration ? 200 : 201, response.getStatus());

        // check it has been created from REST
        DocumentContext json = getAsJSONPath("rest/oseo/collections/TEST123/layer", 200);
        assertEquals("test", json.read("$.workspace"));
        assertEquals("test123", json.read("$.layer"));
        assertEquals(Boolean.FALSE, json.read("$.separateBands"));
        assertEquals(Boolean.FALSE, json.read("$.heterogeneousCRS"));

        // check the configuration elements are there too
        Catalog catalog = getCatalog();
        // ... the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName("test", "test123");
        assertNotNull(store);
        assertThat(store.getFormat(), instanceOf(ImageMosaicFormat.class));
        // ... the layer
        LayerInfo layer = catalog.getLayerByName("test:test123");
        assertNotNull(layer);
        final CoverageInfo coverageInfo = (CoverageInfo) layer.getResource();
        assertThat(coverageInfo.getStore(), equalTo(store));
        // ... the resource is time enabled
        DimensionInfo dimension =
                coverageInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        assertThat(dimension.getAttribute(), equalTo("timeStart"));
        assertThat(dimension.getDefaultValue().getStrategyType(), equalTo(Strategy.MAXIMUM));
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
        String granulesTemplate = getTestStringData("/test123-product-granules-rgb.json");
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
                        getTestData("/test123-layer-simple-graystyle.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(previousConfiguration ? 200 : 201, response.getStatus());

        // check it has been created from REST
        DocumentContext json = getAsJSONPath("rest/oseo/collections/TEST123/layer", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("test123", json.read("$.layer"));
        assertEquals(Boolean.FALSE, json.read("$.separateBands"));
        assertEquals(Boolean.FALSE, json.read("$.heterogeneousCRS"));

        // check the configuration elements are there too
        Catalog catalog = getCatalog();
        // ... the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName("gs", "test123");
        assertNotNull(store);
        assertThat(store.getFormat(), instanceOf(ImageMosaicFormat.class));
        // ... the layer
        LayerInfo layer = catalog.getLayerByName("gs:test123");
        assertNotNull(layer);
        final CoverageInfo coverageInfo = (CoverageInfo) layer.getResource();
        assertThat(coverageInfo.getStore(), equalTo(store));
        // ... the resource is time enabled
        DimensionInfo dimension =
                coverageInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        assertThat(dimension.getAttribute(), equalTo("timeStart"));
        assertThat(dimension.getDefaultValue().getStrategyType(), equalTo(Strategy.MAXIMUM));
        // ... its style is a gray one based on the RED band
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        Style style = layer.getDefaultStyle().getStyle();
        List<FeatureTypeStyle> fts = style.featureTypeStyles();
        assertEquals(1, fts.size());
        List<Rule> rules = fts.get(0).rules();
        assertEquals(1, rules.size());
        List<Symbolizer> symbolizers = rules.get(0).symbolizers();
        assertEquals(1, symbolizers.size());
        RasterSymbolizer rs = (RasterSymbolizer) symbolizers.get(0);
        ChannelSelection cs = rs.getChannelSelection();
        assertNull(cs.getRGBChannels()[0]);
        assertNull(cs.getRGBChannels()[1]);
        assertNull(cs.getRGBChannels()[2]);
        assertEquals("1", cs.getGrayChannel().getChannelName());

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-simple-gray.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    @Test
    public void testCreateCollectionMultiband() throws Exception {
        // setup the granules
        String granulesTemplate = getTestStringData("/test123-product-granules-multiband.json");
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
                        getTestData("/test123-layer-multiband.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(previousConfiguration ? 200 : 201, response.getStatus());

        // check it has been created from REST
        DocumentContext json = getAsJSONPath("rest/oseo/collections/TEST123/layer", 200);
        assertEquals("gs", json.read("$.workspace"));
        assertEquals("test123", json.read("$.layer"));
        assertEquals(Boolean.TRUE, json.read("$.separateBands"));
        assertEquals(Boolean.TRUE, json.read("$.heterogeneousCRS"));

        // check the configuration elements are there too
        Catalog catalog = getCatalog();
        // ... the store
        CoverageStoreInfo store = catalog.getCoverageStoreByName("gs", "test123");
        assertNotNull(store);
        assertThat(store.getFormat(), instanceOf(ImageMosaicFormat.class));
        // ... the layer
        LayerInfo layer = catalog.getLayerByName("gs:test123");
        assertNotNull(layer);
        final CoverageInfo coverageInfo = (CoverageInfo) layer.getResource();
        assertThat(coverageInfo.getStore(), equalTo(store));
        // ... the resource is time enabled
        DimensionInfo dimension =
                coverageInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        assertThat(dimension.getAttribute(), equalTo("timeStart"));
        assertThat(dimension.getDefaultValue().getStrategyType(), equalTo(Strategy.MAXIMUM));

        // ... its style is a RGB one based on the B2, B3, B4
        assertThat(layer.getDefaultStyle().prefixedName(), equalTo("gs:test123"));
        Style style = layer.getDefaultStyle().getStyle();
        List<FeatureTypeStyle> fts = style.featureTypeStyles();
        assertEquals(1, fts.size());
        List<Rule> rules = fts.get(0).rules();
        assertEquals(1, rules.size());
        List<Symbolizer> symbolizers = rules.get(0).symbolizers();
        assertEquals(1, symbolizers.size());
        RasterSymbolizer rs = (RasterSymbolizer) symbolizers.get(0);
        ChannelSelection cs = rs.getChannelSelection();
        assertEquals("4", cs.getRGBChannels()[0].getChannelName());
        assertEquals("2", cs.getRGBChannels()[1].getChannelName());
        assertEquals("1", cs.getRGBChannels()[2].getChannelName());
        assertNull(cs.getGrayChannel());

        BufferedImage image =
                getAsImage("wms/reflect?layers=gs:test123&format=image/png&width=200", "image/png");
        File expected = new File("src/test/resources/test123-multiband.png");
        ImageAssert.assertEquals(expected, image, 1000);
    }

    /**
     * This test checks it's possible to change an existing configuration and stuff still works
     *
     * @throws Exception
     */
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
}
