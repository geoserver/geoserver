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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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
import java.util.List;
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
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
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
import org.geoserver.util.GeoServerDefaultLocale;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.ColorPickerPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.ParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.VirtualTable;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.locationtech.jts.io.WKTReader;
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
                Collections.emptyMap(),
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
    public void testResourceConfigurationPageInfoLabels() {
        LayerInfo layer =
                getGeoServerApplication()
                        .getCatalog()
                        .getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        final String storeNamePath = "publishedinfo:tabs:panel:theList:0:content:storeName";
        assertNotNull(tester.getComponentFromLastRenderedPage(storeNamePath).getMarkup());
        tester.assertLabel(storeNamePath, layer.getResource().getStore().getName());

        final String nativeNamePath = "publishedinfo:tabs:panel:theList:0:content:nativeName";
        assertNotNull(tester.getComponentFromLastRenderedPage(nativeNamePath).getMarkup());
        tester.assertLabel(nativeNamePath, layer.getResource().getNativeName());
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
                    @Override
                    public void onEvent(AjaxRequestTarget target) {
                        page.updateResource(info, target);
                    }
                });
        tester.executeAjaxEvent(page, "ondblclick");

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
        assertNotNull(page2.getPublishedInfo().getResource().getCatalog());
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
        AtomicReference<Object> ref = new AtomicReference<>(null);
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
    public void testApply() {
        Catalog catalog = getGeoServerApplication().getCatalog();
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        LayerInfo layer = catalog.getLayerByName(layerId);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        FormTester ft = tester.newFormTester("publishedinfo");
        String newTitle = "A test title";
        ft.setValue("tabs:panel:theList:0:content:titleAndAbstract:title", newTitle);
        ft.submit("apply");
        tester.executeAjaxEvent("publishedinfo:apply", "submit");
        // no errors, and page is still the same
        tester.assertNoErrorMessage();
        assertThat(
                tester.getLastRenderedPage(), Matchers.instanceOf(ResourceConfigurationPage.class));

        // check the title was updated
        assertEquals(newTitle, getCatalog().getLayerByName(layerId).getTitle());
    }

    @Test
    @SuppressWarnings("deprecation")
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
        storeInfo.setType("Web Feature Server (NG)");
        storeInfo.getConnectionParameters().put(WFSDataStoreFactory.URL.key, url);
        storeInfo.getConnectionParameters().put("usedefaultsrs", Boolean.FALSE);
        storeInfo.getConnectionParameters().put(WFSDataStoreFactory.PROTOCOL.key, Boolean.FALSE);
        storeInfo.getConnectionParameters().put("TESTING", Boolean.TRUE);
        getCatalog().add(storeInfo);

        // MOCKING Feature Type
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        FeatureTypeInfo ftInfo =
                xp.load(
                        getClass().getResourceAsStream("/featuretype.xml"),
                        FeatureTypeInfoImpl.class);
        ftInfo.setStore(storeInfo);
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

        // click first item in SRS (urn:ogc:def:crs:EPSG::4326)
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:1:itemProperties:0:component:link",
                true);

        // assert that native SRS has changed from EPSG:26713 to urn:ogc:def:crs:EPSG::4326
        String newNativeSRS =
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:srs")
                        .getDefaultModelObjectAsString();
        assertFalse(newNativeSRS.equalsIgnoreCase(actualNativeSRS));
        assertTrue(newNativeSRS.equalsIgnoreCase("urn:ogc:def:crs:EPSG::4326"));

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
            assertEquals(3, tabs.getTabs().size());
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
        Catalog catalog = getCatalog();
        URL descURL = configureMockWMTSCapClient();
        WMTSStoreInfo storeInfo = configureWMTSStoreInfo(catalog, descURL, "WMTS Store Urn");
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(storeInfo);
        WMTSLayerInfo wmtsLayerInfo = builder.buildWMTSLayer("topowebb_nedtonad");
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
        assertEquals("urn:ogc:def:crs:EPSG::3006", epsgComponent1.getDefaultModel().getObject());

        // not urn format but checking as well
        assertEquals("EPSG:3857", epsgComponent2.getDefaultModel().getObject());
        assertEquals("EPSG:3006", epsgComponent3.getDefaultModel().getObject());
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

    @Test
    public void testInternationalContent() {
        Catalog catalog = getGeoServerApplication().getCatalog();
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        LayerInfo layer = catalog.getLayerByName(layerId);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        FormTester form = tester.newFormTester("publishedinfo");

        // enable i18n for title
        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");

        form.select(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);

        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");

        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:addNew",
                "click");
        form.select(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);

        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:2:component:remove",
                "click");

        // enable i18n for abstract
        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:abstractLabel:abstractLabel_i18nCheckbox",
                "change");

        form.select(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:0:component:border:border_body:select",
                10);
        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international abstract");

        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:internationalAbstract:container:addNew",
                "click");
        form.select(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:0:component:border:border_body:select",
                20);
        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international abstract");
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:internationalAbstract:container:tablePanel:listContainer:items:2:itemProperties:2:component:remove",
                "click");

        form = tester.newFormTester("publishedinfo");
        form.submit("save");
        tester.assertNoErrorMessage();
    }

    @Test
    public void testDuplicateNullEntry() {
        Catalog catalog = getGeoServerApplication().getCatalog();
        String layerId = getLayerId(MockData.FIFTEEN);
        LayerInfo layer = catalog.getLayerByName(layerId);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        FormTester form = tester.newFormTester("publishedinfo");

        // enable i18n for title
        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:titleLabel:titleLabel_i18nCheckbox",
                "change");

        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:1:itemProperties:1:component:border:border_body:txt",
                "an international title");

        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:addNew",
                "click");

        tester.newFormTester("publishedinfo");
        form.setValue(
                "tabs:panel:theList:0:content:titleAndAbstract:internationalTitle:container:tablePanel:listContainer:items:2:itemProperties:1:component:border:border_body:txt",
                "another international title");
        form.submit("save");
        tester.assertErrorMessages(
                "There are more than one entries for the same language in one of the i18n fields. Duplicate language is empty");
    }

    @Test
    public void testUrnOgcDefaultSRID() throws Exception {
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();
        Catalog catalog = getCatalog();
        URL descURL = new URL(baseURL + "/wmts?REQUEST=GetCapabilities&VERSION=1.1.0&SERVICE=WMS");
        client.expectGet(
                descURL,
                new MockHttpResponse(getClass().getResource("/wms_getCaps_CRS.xml"), "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        WMSStoreInfo storeInfo = new WMSStoreInfoImpl(getCatalog());
        storeInfo.setName("Another Mock WMS Store");
        storeInfo.setCapabilitiesURL(descURL.toString());
        storeInfo.setConnectTimeout(60);
        storeInfo.setMaxConnections(10);
        storeInfo.setDateCreated(new Date());
        storeInfo.setDateModified(new Date());
        catalog.add(storeInfo);
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(storeInfo);
        WMSLayerInfo wmtsLayerInfo = builder.buildWMSLayer("world4326");
        LayerInfo layerInfo = builder.buildLayer(wmtsLayerInfo);

        // page should show expected EPSG:3395 default SRS in WMS cap document
        login();
        tester.startPage(new ResourceConfigurationPage(layerInfo, true));

        String nativeSRSTextFieldValue =
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:srs")
                        .getDefaultModelObjectAsString();
        assertEquals("EPSG:3395", nativeSRSTextFieldValue);
    }

    @Test
    public void testConsistentUpdateWMTSBbox() throws IOException, FactoryException {
        LayerInfo layerInfo = setUpWMTSLayer("WMTS Store BBOX");
        // page should show additional SRS in WMTS cap document
        login();
        tester.startPage(new ResourceConfigurationPage(layerInfo, true));
        ReferencedEnvelope oldEnvelope =
                (ReferencedEnvelope)
                        tester.getComponentFromLastRenderedPage(
                                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeBoundingBox")
                                .getDefaultModel()
                                .getObject();
        // click the FIND button next to open SRS selection popup
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:find");

        // verify Layer`s resource is updated with metadata
        assertNotNull(layerInfo.getResource().getMetadata().get(FeatureTypeInfo.OTHER_SRS));

        // click first item in SRS
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeSRS:popup:content:table:listContainer:items:1:itemProperties:0:component:link",
                true);
        tester.clickLink(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:computeNative", true);
        // assert that native SRS has changed
        ReferencedEnvelope newEnvelope =
                (ReferencedEnvelope)
                        tester.getComponentFromLastRenderedPage(
                                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:nativeBoundingBox")
                                .getDefaultModel()
                                .getObject();
        // these are defined in capabilities
        ReferencedEnvelope envelope =
                new ReferencedEnvelope(
                        4305696.0,
                        8500000.0,
                        -1200000.0,
                        2994304.0,
                        CRS.decode("urn:ogc:def:crs:EPSG::3006"));
        // the envelope was updated
        assertNotEquals(oldEnvelope, newEnvelope);
        // it equals the expected
        assertEquals(envelope, newEnvelope);
    }

    private LayerInfo setUpWMTSLayer(String storeName) throws IOException {
        Catalog catalog = getCatalog();
        URL descURL = configureMockWMTSCapClient();
        WMTSStoreInfo storeInfo = configureWMTSStoreInfo(catalog, descURL, storeName);
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        WMTSLayerInfo wmtsInfo =
                xp.load(getClass().getResourceAsStream("/wmtsLayerInfo.xml"), WMTSLayerInfo.class);
        wmtsInfo.setStore(storeInfo);
        catalog.add(wmtsInfo);
        LayerInfo layerInfo =
                xp.load(getClass().getResourceAsStream("/wmtsLayer.xml"), LayerInfo.class);
        layerInfo.setResource(wmtsInfo);
        return layerInfo;
    }

    private URL configureMockWMTSCapClient() throws IOException {
        String baseURL = TestHttpClientProvider.MOCKSERVER;
        MockHttpClient client = new MockHttpClient();
        URL descURL = new URL(baseURL + "/wmts?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        client.expectGet(
                descURL,
                new MockHttpResponse(getClass().getResource("/wmts_getCaps.xml"), "text/xml"));

        TestHttpClientProvider.bind(client, descURL);
        return descURL;
    }

    private WMTSStoreInfo configureWMTSStoreInfo(Catalog catalog, URL capURL, String name) {
        WMTSStoreInfo storeInfo = new WMTSStoreInfoImpl(getCatalog());
        storeInfo.setName(name);
        storeInfo.setCapabilitiesURL(capURL.toString());
        storeInfo.setConnectTimeout(60);
        storeInfo.setMaxConnections(10);
        storeInfo.setDateCreated(new Date());
        storeInfo.setDateModified(new Date());
        catalog.add(storeInfo);
        return storeInfo;
    }

    @Test
    public void testCustomizeAttributes() {
        String layerId = getLayerId(MockData.GENERICENTITY);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        tester.assertLabel("publishedinfoname", layerId);
        tester.assertComponent(
                "publishedinfo:tabs:panel:theList:0:content", BasicResourceConfig.class);

        // starts with the normal attribute viewer enabled
        String attributesPanel = "publishedinfo:tabs:panel:theList:1:content:attributePanel:";
        tester.assertVisible(attributesPanel + "attributesTable");
        tester.assertInvisible(attributesPanel + "attributesEditor");
        // check one attribute
        String attribute0 = attributesPanel + "attributesTable:attributes:0:";
        tester.assertModelValue(attribute0 + "name", "description");
        tester.assertModelValue(attribute0 + "type", "String");
        tester.assertModelValue(attribute0 + "nillable", "true");
        tester.assertModelValue(attribute0 + "minmax", "0/1");

        // click on the customize checkbox
        FormTester form = tester.newFormTester("publishedinfo");
        form.setValue("tabs:panel:theList:1:content:attributePanel:customizeFeatureType", "true");
        tester.executeAjaxEvent(attributesPanel + "customizeFeatureType", "click");

        tester.assertInvisible(attributesPanel + "attributesTable");
        tester.assertVisible(attributesPanel + "attributesEditor");
        // check one attribute
        String edit1 =
                "publishedinfo:tabs:panel:theList:1:content:attributePanel:attributesEditor:table"
                        + ":listContainer:items:1:itemProperties:";
        tester.assertModelValue(edit1 + "2:component:text", "description");
        tester.assertModelValue(edit1 + "3:component:type", java.lang.String.class);
        tester.assertModelValue(edit1 + "4:component:area", "description");
        tester.assertModelValue(edit1 + "5:component:description", null);
        tester.assertModelValue(edit1 + "6:component:check", true);

        // customize one attribute
        String formEdit1 =
                "tabs:panel:theList:1:content:attributePanel:attributesEditor:table"
                        + ":listContainer:items:1:itemProperties:";
        form.setValue(formEdit1 + "2:component:text", "abstract");
        String cql = "Concatenate(description, ' and more!')";
        form.setValue(formEdit1 + "4:component:area", cql);
        form.setValue(formEdit1 + "5:component:description", "attribute described");
        form.setValue(formEdit1 + "6:component:check", "false");

        // save
        form.submit("apply");
        tester.assertNoErrorMessage();

        // check saving happened
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(layerId);
        List<AttributeTypeInfo> attributes = fti.getAttributes();
        assertNotNull(attributes);
        assertEquals(6, attributes.size());
        AttributeTypeInfo att = attributes.get(0);
        assertEquals("abstract", att.getName());
        assertEquals(
                "attribute described", att.getDescription().toString(GeoServerDefaultLocale.get()));
        assertEquals(cql, att.getSource());
    }

    @Test
    public void testCustomizeAttributesJdbc() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);
        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/foo");
        cat.add(ds);
        SimpleFeatureSource fs1 = getFeatureSource(SystemTestData.FORESTS);
        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(fs1.getSchema());
        store.createSchema(tb.buildFeatureType());
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("Forests");
        fs.addFeatures(fs1.getFeatures());
        addFeature(
                fs,
                "MULTIPOLYGON (((0.008151604330777 -0.0023208963631571, 0.0086527358638763 -0.0012374917185382, 0.0097553137885805 -0.0004505798694767, 0.0156132468328575 0.001226912691216, 0.0164282119026783 0.0012863836826631, 0.0171241513076058 0.0011195104764988, 0.0181763809803841 0.0003258121477801, 0.018663180519973 -0.0007914339515293, 0.0187 -0.0054, 0.0185427596344991 -0.0062643098258021, 0.0178950534559435 -0.0072336706251426, 0.0166538015456463 -0.0078538015456464, 0.0160336706251426 -0.0090950534559435, 0.0150643098258021 -0.0097427596344991, 0.0142 -0.0099, 0.0086 -0.0099, 0.0077356901741979 -0.0097427596344991, 0.0067663293748574 -0.0090950534559435, 0.0062572403655009 -0.0082643098258021, 0.0061 -0.0074, 0.0061055767515099 -0.0046945371967831, 0.0062818025956546 -0.0038730531083409, 0.0066527358638763 -0.0032374917185382, 0.0072813143786463 -0.0026800146279973, 0.008151604330777 -0.0023208963631571)))",
                "110",
                "Foo Forest");
        addFeature(
                fs,
                "MULTIPOLYGON (((-0.0023852705061082 -0.005664537521815, -0.0026781637249217 -0.0063716443030016, -0.0033852705061082 -0.006664537521815, -0.0040923772872948 -0.0063716443030016, -0.0043852705061082 -0.005664537521815, -0.0040923772872947 -0.0049574307406285, -0.0033852705061082 -0.004664537521815, -0.0026781637249217 -0.0049574307406285, -0.0023852705061082 -0.005664537521815)))",
                "111",
                "Bar Forest");
        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        VirtualTable vt = new VirtualTable("test", "SELECT FID,NAD FROM \"Forests\"");
        ft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);

        cat.add(ft);
        LayerInfo layer = cat.getFactory().createLayer();
        layer.setResource(ft);
        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        String text = tester.getLastResponse().getDocument();

        assertTrue(text.contains("gs:Forests"));
        assertTrue(text.contains("Basic Resource Info"));
        assertTrue(text.contains("Feature Type Details"));
        assertTrue(text.contains("Edit sql view"));
        assertTrue(
                text.contains(
                        "Failed to load attribute list, internal error is: Column NAD not found"));

        // After updating SQL view correctly error message should not be present
        VirtualTable vt1 = new VirtualTable("test", "SELECT FID,NAME FROM \"Forests\"");
        ft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt1);

        LayerInfo layer1 = cat.getFactory().createLayer();
        layer1.setResource(ft);
        tester.startPage(new ResourceConfigurationPage(layer1, false));
        String text1 = tester.getLastResponse().getDocument();

        assertTrue(text1.contains("gs:Forests"));
        assertTrue(text1.contains("Basic Resource Info"));
        assertTrue(text1.contains("Feature Type Details"));
        assertTrue(text1.contains("Edit sql view"));
        assertFalse(
                text1.contains(
                        "Failed to load attribute list, internal error is: Column NAD not found"));
    }

    void addFeature(SimpleFeatureStore store, String wkt, Object... atts) throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(store.getSchema());
        b.add(new WKTReader().read(wkt));
        for (Object att : atts) {
            b.add(att);
        }

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);
        features.add(b.buildFeature(null));
        store.addFeatures(features);
    }
}
