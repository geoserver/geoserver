/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.vividsolutions.jts.geom.Envelope;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletResponse;

public class OpenLayersMapOutputFormatTest extends WMSTestSupport {

    private OpenLayersMapOutputFormat mapProducer;

    Pattern lookForEscapedParam =
            Pattern.compile(
                    Pattern.quote(
                            "\"</script><script>alert('x-scripted');</script><script>\": 'foo'"));

    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Before
    public void setMapProducer() throws Exception {
        Logging.getLogger("org.geotools.rendering").setLevel(Level.OFF);
        this.mapProducer = getProducerInstance();
    }

    protected OpenLayersMapOutputFormat getProducerInstance() {
        return new OpenLayersMapOutputFormat(getWMS());
    }

    @After
    public void unsetMapProducer() throws Exception {
        this.mapProducer = null;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // get default workspace info
        WorkspaceInfo workspaceInfo = getCatalog().getWorkspaceByName(MockData.DEFAULT_PREFIX);
        // create static raster store
        StoreInfo store = createStaticRasterStore(workspaceInfo);
        // create static raster layer
        NamespaceInfo nameSpace = getCatalog().getNamespaceByPrefix(MockData.DEFAULT_PREFIX);
        createStaticRasterLayer(nameSpace, store, "staticRaster");
    }

    /**
     * Test for GEOS-5318: xss vulnerability when a weird parameter is added to the request
     * (something like: %3C%2Fscript%
     * 3E%3Cscript%3Ealert%28%27x-scripted%27%29%3C%2Fscript%3E%3Cscript%3E=foo) the causes js code
     * execution.
     *
     * @throws IOException
     */
    @Test
    public void testXssFix() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                MockData.BASIC_POLYGONS.getPrefix(),
                                MockData.BASIC_POLYGONS.getLocalPart())
                        .getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        request.getRawKvp().put("</script><script>alert('x-scripted');</script><script>", "foo");
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        FeatureLayer layer = new FeatureLayer(fs, basicStyle);
        layer.setTitle("Title");
        map.addLayer(layer);
        request.setFormat("application/openlayers");
        RawMap rawMap = this.mapProducer.produceMap(map);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rawMap.writeTo(bos);
        String htmlDoc = new String(bos.toByteArray(), "UTF-8");
        // check that weird param is correctly encoded to avoid js code execution
        int index =
                htmlDoc.replace("\\n", "")
                        .replace("\\r", "")
                        .indexOf(
                                "\"</script\\><script\\>alert(\\'x-scripted\\');</script\\><script\\>\": 'foo'");
        assertTrue(index > -1);
    }

    @Test
    public void testRastersFilteringCapabilities() throws Exception {
        // static raster layer supports filtering
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=gs:staticRaster"
                                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                                + "14.592757149389236,44.55808294568743&width=768&height=330"
                                + "&srs=EPSG:4326&format=application/openlayers");
        String content = response.getContentAsString();
        assertThat(content.contains("var supportsFiltering = true;"), is(true));
        // world raster layer doesn't support filtering
        response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=wcs:World"
                                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                                + "14.592757149389236,44.55808294568743&width=768&height=330"
                                + "&srs=EPSG:4326&format=application/openlayers");
        content = response.getContentAsString();
        assertThat(content.contains("var supportsFiltering = false;"), is(true));

        // if at least one layer supports filtering, overall filtering should be supported
        response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=wcs:World,gs:staticRaster"
                                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                                + "14.592757149389236,44.55808294568743&width=768&height=330"
                                + "&srs=EPSG:4326&format=application/openlayers");
        content = response.getContentAsString();
        assertThat(content.contains("var supportsFiltering = true;"), is(true));
    }

    @Test
    public void testWMTSFilteringCapabilities() throws Exception {

        // Create a cascading layer
        createWMTSCatalogStuff();

        // wmts by itself should not support filtering
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=gs:wmtslayername"
                                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                                + "14.592757149389236,44.55808294568743&width=768&height=330"
                                + "&srs=EPSG:4326&format=application/openlayers");
        String content = response.getContentAsString();
        assertThat(content.contains("var supportsFiltering = false;"), is(true));

        // wmts along with filterable layer should support filtering
        response =
                getAsServletResponse(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=gs:wmtslayername,gs:staticRaster"
                                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                                + "14.592757149389236,44.55808294568743&width=768&height=330"
                                + "&srs=EPSG:4326&format=application/openlayers");
        content = response.getContentAsString();
        assertThat(content.contains("var supportsFiltering = true;"), is(true));
    }

    /** Helper method that creates a static raster store and adds it to the catalog. */
    private StoreInfo createStaticRasterStore(WorkspaceInfo workspace) {
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        store.setWorkspace(workspace);
        store.setType("StaticRaster");
        store.setEnabled(true);
        store.setName("StaticRaster");
        // some fictive URL
        store.setURL("http://127.0.0.1:geoserver");
        // add the store to the catalog
        catalog.add(store);
        return store;
    }

    /**
     * Helper method that creates a static WMTS store and related layer and adds it to the catalog.
     */
    private StoreInfo createWMTSCatalogStuff() throws MalformedURLException, IOException {
        // use a local mock capabilities
        String capabilities =
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities),
                new MockHttpResponse(getClass().getResource("/nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);

        Catalog catalog = getCatalog();

        WorkspaceInfo workspace = getCatalog().getWorkspaceByName(MockData.DEFAULT_PREFIX);
        NamespaceInfo nameSpace = getCatalog().getNamespaceByPrefix(MockData.DEFAULT_PREFIX);

        // Store
        WMTSStoreInfo store = catalog.getFactory().createWebMapTileServer();
        store.setWorkspace(workspace);
        store.setType("WMTS");
        store.setEnabled(true);
        store.setName("wmts");
        store.setCapabilitiesURL(capabilities);
        catalog.add(store);

        // Resource
        WMTSLayerInfo resource = catalog.getFactory().createWMTSLayer();
        resource.setNamespace(nameSpace);
        resource.setName("wmtslayername");
        resource.setNativeName("AMSR2_Snow_Water_Equivalent");
        resource.setEnabled(true);
        resource.setStore(store);
        catalog.add(resource);

        // Layer
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setResource(resource);
        layer.setEnabled(true);
        layer.setName("wmtslayername");
        catalog.add(layer);

        return store;
    }

    /** Helper method that creates a static raster layer and adds it to the catalog. */
    private void createStaticRasterLayer(
            NamespaceInfo namespace, StoreInfo store, String layerName) {
        Catalog catalog = getCatalog();
        // creating the coverage info
        CoverageInfoImpl coverageInfo = new CoverageInfoImpl(catalog);
        coverageInfo.setNamespace(namespace);
        coverageInfo.setName(layerName);
        coverageInfo.setNativeCoverageName(layerName);
        coverageInfo.setStore(store);
        // creating the layer
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setResource(coverageInfo);
        layer.setEnabled(true);
        layer.setName(layerName);
        // set the layers styles
        layer.setDefaultStyle(catalog.getStyleByName("raster"));
        // set layer CRS and native CRS
        coverageInfo.setNativeCRS(DefaultGeographicCRS.WGS84);
        coverageInfo.setSRS("EPSG:4326");
        // saving everything
        catalog.add(coverageInfo);
        catalog.add(layer);
    }

    /**
     * Test for GEOS-8178: OpenLayersOutputFormat NoSuchAuthorityCodeExceptions being thrown due to
     * malformed URN codes.
     *
     * <p>Exception is thrown when decoding CRS in isWms13FlippedCRS which is called by produceMap,
     * test uses produceMap and reads the resulting output steam to ensure "yx: true" is returned
     * for EPSG:4326, output is false before fix
     *
     * @throws Exception
     */
    @Test
    public void testUrnCodeFix() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                MockData.BASIC_POLYGONS.getPrefix(),
                                MockData.BASIC_POLYGONS.getLocalPart())
                        .getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");

        request.setCrs(crs);

        final WMSMapContent map = new WMSMapContent();
        map.setRequest(request);
        request.setFormat("application/openlayers");

        RawMap rawMap = this.mapProducer.produceMap(map);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rawMap.writeTo(bos);
        String htmlDoc = new String(bos.toByteArray(), "UTF-8");
        int index = htmlDoc.indexOf("yx : {'EPSG:4326' : true}\n");

        assertTrue(index > -1);
    }
}
