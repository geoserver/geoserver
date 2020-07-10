/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.geotools.coverage.grid.io.AbstractGridFormat.BACKGROUND_COLOR;
import static org.geotools.coverage.grid.io.AbstractGridFormat.FOOTPRINT_BEHAVIOR;
import static org.geotools.coverage.grid.io.AbstractGridFormat.INPUT_TRANSPARENT_COLOR;
import static org.geotools.coverage.grid.io.AbstractGridFormat.OVERVIEW_POLICY;
import static org.geotools.coverage.grid.io.AbstractGridFormat.RESCALE_PIXELS;
import static org.geotools.coverage.grid.io.AbstractGridFormat.USE_JAI_IMAGEREAD;
import static org.geotools.gce.imagemosaic.ImageMosaicFormat.ACCURATE_RESOLUTION;
import static org.geotools.gce.imagemosaic.ImageMosaicFormat.ALLOW_MULTITHREADING;
import static org.geotools.gce.imagemosaic.ImageMosaicFormat.EXCESS_GRANULE_REMOVAL;
import static org.geotools.gce.imagemosaic.ImageMosaicFormat.MERGE_BEHAVIOR;
import static org.geotools.gce.imagemosaic.ImageMosaicFormat.OUTPUT_TRANSPARENT_COLOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.namespace.QName;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.ColorPickerPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.ParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class ResourceConfigurationPageTest extends GeoServerWicketTestSupport {

    protected static QName TIMERANGES =
            new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);

    protected static QName LINES = new QName(MockData.SF_URI, "null_srid_line", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(
                TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());

        testData.addVectorLayer(
                LINES,
                Collections.EMPTY_MAP,
                "null_srid_line.properties",
                ResourceConfigurationPageTest.class,
                getCatalog());
    }

    @Test
    public void testBasic() {
        LayerInfo layer =
                getGeoServerApplication()
                        .getCatalog()
                        .getLayerByName(getLayerId(MockData.BASIC_POLYGONS));

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        tester.assertLabel("publishedinfoname", layer.getResource().prefixedName());
        tester.assertComponent(
                "publishedinfo:tabs:panel:theList:0:content", BasicResourceConfig.class);
    }

    @Test
    public void testUpdateResource() {
        LayerInfo layer =
                getGeoServerApplication()
                        .getCatalog()
                        .getLayerByName(getLayerId(MockData.GEOMETRYLESS));

        login();
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);

        tester.startPage(page);
        tester.assertContainsNot("the_geom");

        FeatureTypeInfo info =
                getCatalog()
                        .getResourceByName(MockData.BRIDGES.getLocalPart(), FeatureTypeInfo.class);

        // Apply the new feature to the page
        page.add(
                new AjaxEventBehavior("ondblclick") {
                    public void onEvent(AjaxRequestTarget target) {
                        page.updateResource(info, target);
                    }
                });
        tester.executeAjaxEvent(page, "ondblclick");
        print(tester.getLastRenderedPage(), true, true);

        // verify contents were updated
        tester.assertContains("the_geom");
    }

    @Test
    public void testSerializedModel() throws Exception {
        CatalogFactory fac = getGeoServerApplication().getCatalog().getFactory();
        FeatureTypeInfo fti = fac.createFeatureType();
        fti.setName("mylayer");
        fti.setStore(
                getGeoServerApplication()
                        .getCatalog()
                        .getDataStoreByName(MockData.POLYGONS.getPrefix()));
        LayerInfo layer = fac.createLayer();
        layer.setResource(fti);

        login();
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, true);

        byte[] serialized;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(page);
            }
            serialized = os.toByteArray();
        }
        ResourceConfigurationPage page2;
        try (ByteArrayInputStream is = new ByteArrayInputStream(serialized)) {
            try (ObjectInputStream ois = new ObjectInputStream(is)) {
                page2 = (ResourceConfigurationPage) ois.readObject();
            }
        }

        assertTrue(page2.getPublishedInfo() instanceof LayerInfo);
        assertEquals(layer.prefixedName(), page2.getPublishedInfo().prefixedName());
        // the crucial test: the layer is attached to the catalog
        assertNotNull(((LayerInfo) page2.getPublishedInfo()).getResource().getCatalog());
    }

    @Test
    public void testComputeLatLon() throws Exception {
        final Catalog catalog = getCatalog();

        final CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(catalog.getStoreByName(MockData.POLYGONS.getPrefix(), DataStoreInfo.class));
        FeatureTypeInfo ft = cb.buildFeatureType(new NameImpl(MockData.POLYGONS));
        LayerInfo layer = cb.buildLayer(ft);

        login();
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, true);
        tester.startPage(page);
        // print(tester.getLastRenderedPage(), true, true, true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:computeLatLon",
                "onclick");
        // print(tester.getLastRenderedPage(), true, true, true);
        // we used to have error messages
        tester.assertNoErrorMessage();
        Component llbox =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:latLonBoundingBox");
        ReferencedEnvelope re = (ReferencedEnvelope) llbox.getDefaultModelObject();
        assertEquals(-93, re.getMinX(), 0.1);
        assertEquals(4.5, re.getMinY(), 0.1);
        assertEquals(-93, re.getMaxX(), 0.1);
        assertEquals(4.5, re.getMaxY(), 0.1);
    }

    @Test
    public void testParametersUI() throws Exception {
        LayerInfo layer =
                getGeoServerApplication().getCatalog().getLayerByName(getLayerId(TIMERANGES));

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        // print(tester.getLastRenderedPage(), true, true);

        // get the list of parameters in the UI
        ListView parametersList =
                (ListView)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:1:content:parameters");
        parametersList.visitChildren(
                ParamPanel.class,
                (c, v) -> {
                    MapModel mapModel = (MapModel) c.getDefaultModel();
                    String parameterKey = mapModel.getExpression();
                    if (USE_JAI_IMAGEREAD.getName().getCode().equals(parameterKey)
                            || ACCURATE_RESOLUTION.getName().getCode().equals(parameterKey)
                            || ALLOW_MULTITHREADING.getName().getCode().equals(parameterKey)
                            || RESCALE_PIXELS.getName().getCode().equals(parameterKey)) {
                        assertThat(
                                parameterKey, c, CoreMatchers.instanceOf(CheckBoxParamPanel.class));
                    } else if (EXCESS_GRANULE_REMOVAL.getName().getCode().equals(parameterKey)
                            || FOOTPRINT_BEHAVIOR.getName().getCode().equals(parameterKey)
                            || MERGE_BEHAVIOR.getName().getCode().equals(parameterKey)
                            || OVERVIEW_POLICY.getName().getCode().equals(parameterKey)) {
                        assertThat(
                                parameterKey,
                                c,
                                CoreMatchers.instanceOf(DropDownChoiceParamPanel.class));
                    } else if (BACKGROUND_COLOR.getName().getCode().equals(parameterKey)
                            || OUTPUT_TRANSPARENT_COLOR.getName().getCode().equals(parameterKey)
                            || INPUT_TRANSPARENT_COLOR.getName().getCode().equals(parameterKey)) {
                        assertThat(
                                parameterKey, c, CoreMatchers.instanceOf(ColorPickerPanel.class));
                    } else {
                        assertThat(parameterKey, c, CoreMatchers.instanceOf(TextParamPanel.class));
                    }
                });
        tester.assertComponent(
                "publishedinfo:tabs:panel:theList:1:content:parameters:0:parameterPanel",
                CheckBoxParamPanel.class);
    }

    @Test
    public void testMissingParameters() {
        // get mosaic, remove a parameter
        CoverageInfo coverage = getCatalog().getCoverageByName(getLayerId(TIMERANGES));
        String bandCode = ImageMosaicFormat.BANDS.getName().getCode();
        coverage.getParameters().remove(bandCode);
        getCatalog().save(coverage);

        // start up the page
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(TIMERANGES));
        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        // print(tester.getLastRenderedPage(), true, true);

        // get the list of parameters in the UI
        ListView parametersList =
                (ListView)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:1:content:parameters");
        AtomicBoolean editorFound = new AtomicBoolean(false);
        parametersList.visitChildren(
                ParamPanel.class,
                (c, v) -> {
                    MapModel mapModel = (MapModel) c.getDefaultModel();
                    String parameterKey = mapModel.getExpression();
                    if (bandCode.equals(parameterKey)) {
                        editorFound.set(true);
                    }
                });
        assertTrue("Bands parameter not found", editorFound.get());
    }

    @Test
    public void testSaveEnumsAsString() {
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(TIMERANGES));

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        // locate the overview parameter editor
        ListView parametersList =
                (ListView)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:1:content:parameters");
        AtomicReference ref = new AtomicReference(null);
        parametersList.visitChildren(
                ParamPanel.class,
                (c, v) -> {
                    MapModel mapModel = (MapModel) c.getDefaultModel();
                    String parameterKey = mapModel.getExpression();
                    if (OVERVIEW_POLICY.getName().getCode().equals(parameterKey)) {
                        ref.set(c.getPageRelativePath().substring("publishedInfo".length() + 1));
                    }
                });

        FormTester ft = tester.newFormTester("publishedinfo");
        ft.select(ref.get() + ":border:border_body:paramValue", 2);
        tester.debugComponentTrees();
        ft.submit("save");
        tester.assertNoErrorMessage();

        // check it was saved
        CoverageInfo ci =
                catalog.getResourceByName(
                        TIMERANGES.getPrefix(), TIMERANGES.getLocalPart(), CoverageInfo.class);
        Map<String, Serializable> parameters = ci.getParameters();
        assertEquals("NEAREST", parameters.get(OVERVIEW_POLICY.getName().toString()));
    }

    @Test
    public void testWFSDataStoreResource() throws IOException {
        // MOCKING WFS DataStore and Mock Remote Response
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();

        URL descURL =
                new URL(baseURL + "/wfs?REQUEST=DescribeFeatureType&VERSION=1.1.0&SERVICE=WFS");
        client.expectGet(
                descURL, new MockHttpResponse(getClass().getResource("/desc_110.xml"), "text/xml"));

        URL descFeatureURL =
                new URL(
                        baseURL
                                + "/wfs?NAMESPACE=xmlns%28topp%3Dhttp%3A%2F%2Fwww.topp.com%29&TYPENAME=topp%3Aroads22&REQUEST=DescribeFeatureType&VERSION=1.1.0&SERVICE=WFS");
        client.expectGet(
                descFeatureURL,
                new MockHttpResponse(getClass().getResource("/desc_feature.xml"), "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        TestHttpClientProvider.bind(client, descFeatureURL);

        // MOCKING Catalog
        URL url = getClass().getResource("/wfs_cap_110.xml");

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

        // MOCKING Feature Type
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        FeatureTypeInfo ftInfo =
                xp.load(
                        getClass().getResourceAsStream("/featuretype.xml"),
                        FeatureTypeInfoImpl.class);
        ((FeatureTypeInfoImpl) ftInfo).setStore(storeInfo);
        final String actualNativeSRS = ftInfo.getSRS();
        getCatalog().add(ftInfo);
        // setting mock feature type as resource of Layer from Test Data
        LayerInfo layerInfo = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        layerInfo.setResource(ftInfo);

        // Injecting Mock Http client in WFS Data Store to read mock respones from XML
        DataAccess dac = ftInfo.getStore().getDataStore(null);
        RetypingDataStore retypingDS = (RetypingDataStore) dac;
        WFSDataStore wfsDS = (WFSDataStore) retypingDS.getWrapped();
        wfsDS.getWfsClient().setHttpClient(client);
        // now begins the test
        // page should show additional SRS in WFS cap document
        login();
        // render page for a layer with WFS-NG resource
        tester.startPage(new ResourceConfigurationPage(layerInfo, false));

        // click the FIND button next to Native SRS text field to open SRS selection popup
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:find", true);

        // verify Layer`s resource is updated with metadata
        assertNotNull(layerInfo.getResource().getMetadata().get(FeatureTypeInfo.OTHER_SRS));

        // click first item in SRS (4326)
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:1:itemProperties:0:component:link",
                true);

        // assert that native SRS has changed from EPSG:26713 to EPSG:4326
        String newNativeSRS =
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:srs")
                        .getDefaultModelObjectAsString();
        assertFalse(newNativeSRS.equalsIgnoreCase(actualNativeSRS));

        // click submit and go back to LayerPage
        FormTester ft = tester.newFormTester("publishedinfo");
        ft.submit("save");

        // check that native SRS is updated in catalog after submitting the page
        String savedSRS = getCatalog().getLayerByName(layerInfo.getName()).getResource().getSRS();
        assertFalse(savedSRS.equalsIgnoreCase(actualNativeSRS));
    }

    @Test
    public void testNullSRIDResource() throws IOException {
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINES));
        assertNotNull(layer);
        // remove SRID from feature
        ResourceInfo ft = layer.getResource();
        ft.setSRS(null);
        ft.setNativeCRS(null);
        catalog.save(ft);
        login();
        // render page for a layer with resource without SRID
        tester.startPage(new ResourceConfigurationPage(layer, false));
        // assert no error occurred on page and page is available for configuration
        tester.assertNoErrorMessage();
        // assert that native srs is set empty
        String nativeSRSTextFieldValue =
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:srs")
                        .getDefaultModelObjectAsString();
        assertTrue(nativeSRSTextFieldValue.isEmpty());
        // assert Find link is not visible
        tester.assertInvisible(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:find");
    }

    @Test
    public void testSecurityTabInactiveWithNoDeafaultAccessManager() {
        TestResourceAccessManager manager = new TestResourceAccessManager();
        SecureCatalogImpl oldSc = (SecureCatalogImpl) GeoServerExtensions.bean("secureCatalog");
        SecureCatalogImpl sc =
                new SecureCatalogImpl(getCatalog(), manager) {

                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        applicationContext.getBeanFactory().destroyBean("secureCatalog");
        GeoServerExtensionsHelper.clear();
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(TIMERANGES));

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        try {
            TabbedPanel tabs =
                    (TabbedPanel) tester.getComponentFromLastRenderedPage("publishedinfo:tabs");
            assertTrue(tabs.getTabs().size() == 3);
        } finally {
            applicationContext.getBeanFactory().destroyBean("secureCatalog");
            GeoServerExtensionsHelper.clear();
            GeoServerExtensionsHelper.singleton("secureCatalog", oldSc, SecureCatalogImpl.class);
        }
    }

    @Test
    public void testWMTSOtherCRS() throws IOException {
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();
        Catalog catalog = getCatalog();
        URL descURL = new URL(baseURL + "/wmts?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        client.expectGet(
                descURL,
                new MockHttpResponse(getClass().getResource("/wmts_getCaps.xml"), "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        WMTSStoreInfo storeInfo = new WMTSStoreInfoImpl(getCatalog());
        storeInfo.setName("Mock WMTS Store");
        storeInfo.setCapabilitiesURL(descURL.toString());
        storeInfo.setConnectTimeout(60);
        storeInfo.setMaxConnections(10);
        storeInfo.setDateCreated(new Date());
        storeInfo.setDateModified(new Date());
        catalog.add(storeInfo);
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        WMTSLayerInfo wmtsInfo =
                xp.load(getClass().getResourceAsStream("/wmtsLayerInfo.xml"), WMTSLayerInfo.class);
        final String actualNativeSRS = wmtsInfo.getSRS();
        wmtsInfo.setStore(storeInfo);
        catalog.add(wmtsInfo);
        LayerInfo layerInfo =
                xp.load(getClass().getResourceAsStream("/wmtsLayer.xml"), LayerInfo.class);
        layerInfo.setResource(wmtsInfo);
        // page should show additional SRS in WMTS cap document
        login();
        tester.startPage(new ResourceConfigurationPage(layerInfo, true));
        // click the FIND button next to open SRS selection popup
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:find");

        // verify Layer`s resource is updated with metadata
        assertNotNull(layerInfo.getResource().getMetadata().get(FeatureTypeInfo.OTHER_SRS));

        // click first item in SRS
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:1:itemProperties:0:component:link",
                true);

        // assert that native SRS has changed
        String newNativeSRS =
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:srs")
                        .getDefaultModelObjectAsString();
        assertFalse(newNativeSRS.equalsIgnoreCase(actualNativeSRS));
    }

    @Test
    public void testWMTSOtherCRSUrnFormat() throws IOException {
        // SRSProvider constructor removes EPSG: from identifier
        // to display only the code in otherSRS list. This test
        // checks that CRS identified through urn format are properly
        // displayed as well.
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();
        Catalog catalog = getCatalog();
        URL descURL = new URL(baseURL + "/wmts?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        client.expectGet(
                descURL,
                new MockHttpResponse(getClass().getResource("/wmts_getCaps.xml"), "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        WMTSStoreInfo storeInfo = new WMTSStoreInfoImpl(getCatalog());
        storeInfo.setName("Another Mock WMTS Store");
        storeInfo.setCapabilitiesURL(descURL.toString());
        storeInfo.setConnectTimeout(60);
        storeInfo.setMaxConnections(10);
        storeInfo.setDateCreated(new Date());
        storeInfo.setDateModified(new Date());
        catalog.add(storeInfo);
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(storeInfo);
        WMTSLayerInfo wmtsLayerInfo = builder.buildWMTSLayer("bmapgrau");
        LayerInfo layerInfo = builder.buildLayer(wmtsLayerInfo);

        // page should show additional SRS in WMTS cap document
        login();
        tester.startPage(new ResourceConfigurationPage(layerInfo, true));
        // click the FIND button next to open SRS selection popup
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:find");

        // verify Layer`s resource is updated with metadata
        assertNotNull(layerInfo.getResource().getMetadata().get(FeatureTypeInfo.OTHER_SRS));

        DataView epsgContainer =
                (DataView)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items");

        // we got two epsg in the otherSrs container
        assertEquals(3, epsgContainer.size());

        Component epsgComponent1 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:1:itemProperties:0:component:link:label");
        Component epsgComponent2 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:2:itemProperties:0:component:link:label");

        Component epsgComponent3 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:3:itemProperties:0:component:link:label");

        // checks that they have been properly displayed with not urn format being cut

        assertEquals("3857", epsgComponent1.getDefaultModel().getObject());
        assertEquals("urn:ogc:def:crs:EPSG::900913", epsgComponent2.getDefaultModel().getObject());
        assertEquals("urn:ogc:def:crs:EPSG::3857", epsgComponent3.getDefaultModel().getObject());
    }

    @Test
    public void testUrnOgcSRIDResource() throws Exception {
        String urnOgc = "urn:ogc:def:crs:EPSG::4326";
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINES));
        assertNotNull(layer);
        ResourceInfo ft = layer.getResource();
        ft.setSRS(urnOgc);
        ft.setNativeCRS(CRS.decode(urnOgc));
        catalog.save(ft);
        login();
        // render page for a layer with resource with SRID defind in URN OGC format
        tester.startPage(new ResourceConfigurationPage(layer, false));
        // assert no error occurred on page and page is available for configuration
        tester.assertNoErrorMessage();
        // assert that native srs is correctly set
        String nativeSRSTextFieldValue =
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:srs")
                        .getDefaultModelObjectAsString();
        assertEquals("Asserting EPSG code", "EPSG:4326", nativeSRSTextFieldValue);
    }
}
