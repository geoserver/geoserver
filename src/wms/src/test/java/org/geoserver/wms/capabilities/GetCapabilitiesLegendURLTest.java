/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.wms_1_1_1.GetFeatureInfoTest;
import org.geotools.xml.transform.TransformerBase;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Base class for legendURL support in GetCapabilities tests.
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli at geo-solutions.it)
 */
public abstract class GetCapabilitiesLegendURLTest extends WMSTestSupport {

    /**
     * default base url to feed a GetCapabilitiesTransformer with for it to append the DTD location
     */
    protected static final String baseUrl = "http://localhost/geoserver";

    /** test map formats to feed a GetCapabilitiesTransformer with */
    protected static final Set<String> mapFormats = Collections.singleton("image/png");

    /** test legend formats to feed a GetCapabilitiesTransformer with */
    protected static final Set<String> legendFormats = Collections.singleton("image/png");

    /**
     * a mocked up {@link GeoServer} config, almost empty after setUp(), except for the {@link
     * WMSInfo}, {@link GeoServerInfo} and empty {@link Catalog}, Specific tests should add content
     * as needed
     */
    protected GeoServerImpl geosConfig;

    /**
     * a mocked up {@link GeoServerInfo} for {@link #geosConfig}. Specific tests should set its
     * properties as needed
     */
    protected GeoServerInfoImpl geosInfo;

    /**
     * a mocked up {@link WMSInfo} for {@link #geosConfig}, empty except for the WMSInfo after
     * setUp(), Specific tests should set its properties as needed
     */
    protected WMSInfoImpl wmsInfo;

    /**
     * a mocked up {@link Catalog} for {@link #geosConfig}, empty after setUp(), Specific tests
     * should add content as needed
     */
    protected Catalog catalog;

    protected GetCapabilitiesRequest req;

    protected WMS wmsConfig;

    protected XpathEngine XPATH;

    /** Test layers */
    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);

    public static QName STATES = new QName(MockData.CITE_URI, "states", MockData.CITE_PREFIX);
    public static QName WORLD = new QName("http://www.geo-solutions.it", "world", "gs");

    /**
     * Adds required styles to test the selection of maximum and minimum denominator from style's
     * rules.
     */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        this.catalog = getCatalog();
        File dataDirRoot = testData.getDataDirectoryRoot();
        // create legendsamples folder
        new File(
                        dataDirRoot.getAbsolutePath()
                                + File.separator
                                + LegendSampleImpl.LEGEND_SAMPLES_FOLDER)
                .mkdir();

        testData.addStyle("squares", "squares.sld", GetFeatureInfoTest.class, catalog);
        testData.addVectorLayer(
                SQUARES,
                Collections.EMPTY_MAP,
                "squares.properties",
                GetCapabilitiesLegendURLTest.class,
                catalog);
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        testData.addStyle(
                workspaceInfo,
                "states",
                "Population.sld",
                GetCapabilitiesLegendURLTest.class,
                catalog);
        Map<LayerProperty, Object> properties = new HashMap<LayerProperty, Object>();
        properties.put(LayerProperty.STYLE, "states");
        LocalWorkspace.set(workspaceInfo);
        testData.addVectorLayer(
                STATES,
                properties,
                "states.properties",
                GetCapabilitiesLegendURLTest.class,
                catalog);
        LocalWorkspace.set(null);

        testData.addStyle("temperature", "temperature.sld", WMSTestSupport.class, catalog);
        properties = new HashMap<LayerProperty, Object>();
        properties.put(LayerProperty.STYLE, "temperature");
        testData.addRasterLayer(
                WORLD, "world.tiff", null, properties, SystemTestData.class, catalog);
    }

    @Before
    public void internalSetUp() throws IOException {

        this.catalog = getCatalog();
        geosConfig = new GeoServerImpl();

        geosInfo = new GeoServerInfoImpl(geosConfig);
        geosInfo.setContact(new ContactInfoImpl());
        geosConfig.setGlobal(geosInfo);

        wmsInfo = new WMSInfoImpl();
        geosConfig.add(wmsInfo);

        geosConfig.setCatalog(catalog);

        wmsConfig = new WMS(geosConfig);
        wmsConfig.setApplicationContext(applicationContext);

        req = new GetCapabilitiesRequest();
        req.setBaseUrl(baseUrl);

        getTestData()
                .copyTo(
                        getClass().getResourceAsStream("/legendURL/BasicPolygons.png"),
                        LegendSampleImpl.LEGEND_SAMPLES_FOLDER + "/BasicPolygons.png");
        getTestData()
                .copyTo(
                        getClass().getResourceAsStream("/legendURL/Bridges.png"),
                        LegendSampleImpl.LEGEND_SAMPLES_FOLDER + "/Bridges.png");

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        XPATH = XMLUnit.newXpathEngine();
    }

    /** Accessor for global catalog instance from the test application context. */
    protected Catalog getCatalog() {
        return (Catalog) applicationContext.getBean("catalog");
    }

    /** Tests that already cached icons are read from disk and used to calculate size. */
    @Test
    public void testCachedLegendURLSize() throws Exception {

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList legendURLs = XPATH.getMatchingNodes(getLegendURLXPath("cite:BasicPolygons"), dom);
        assertEquals(1, legendURLs.getLength());
        Element legendURL = (Element) legendURLs.item(0);
        assertTrue(legendURL.hasAttribute("width"));
        assertEquals("50", legendURL.getAttribute("width"));
        assertTrue(legendURL.hasAttribute("height"));
        assertEquals("10", legendURL.getAttribute("height"));
    }

    /** Tests that folder for legend samples is created, if missing. */
    @Test
    public void testCachedLegendURLFolderCreated() throws Exception {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        File samplesFolder =
                new File(
                        loader.getBaseDirectory().getAbsolutePath()
                                + File.separator
                                + LegendSampleImpl.LEGEND_SAMPLES_FOLDER);
        removeFileOrFolder(samplesFolder);
        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        assertTrue(samplesFolder.exists());
    }

    /** Tests the layer names are workspace qualified */
    @Test
    public void testLayerWorkspaceQualified() throws Exception {

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);
        // print(dom);

        String legendURL =
                XPATH.evaluate(
                        getLegendURLXPath("cite:squares")
                                + "/"
                                + getElementPrefix()
                                + "OnlineResource/@xlink:href",
                        dom);
        Map<String, Object> kvp = KvpUtils.parseQueryString(legendURL);
        assertEquals("cite:squares", kvp.get("layer"));
    }

    /** Tests that not existing icons are created on disk and used to calculate size. */
    @Test
    public void testCreatedLegendURLSize() throws Exception {

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList legendURLs = XPATH.getMatchingNodes(getLegendURLXPath("cite:squares"), dom);

        assertEquals(1, legendURLs.getLength());
        Element legendURL = (Element) legendURLs.item(0);
        assertTrue(legendURL.hasAttribute("width"));
        assertFalse("20".equals(legendURL.getAttribute("width")));
        assertTrue(legendURL.hasAttribute("height"));
        assertFalse("20".equals(legendURL.getAttribute("height")));

        File sampleFile = getSampleFile("squares");
        assertTrue(sampleFile.exists());
    }

    @Test
    public void testCreatedRasterLegendURLSize() throws Exception {
        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList legendURLs = XPATH.getMatchingNodes(getLegendURLXPath("gs:world"), dom);
        assertEquals(1, legendURLs.getLength());
        Element legendURL = (Element) legendURLs.item(0);
        assertTrue(legendURL.hasAttribute("width"));
        assertFalse("20".equals(legendURL.getAttribute("width")));
        assertTrue(legendURL.hasAttribute("height"));
        assertFalse("20".equals(legendURL.getAttribute("height")));

        File sampleFile = getSampleFile("temperature");
        assertTrue(sampleFile.exists());
    }

    private File getSampleFile(String sampleName) {
        return new File(
                testData.getDataDirectoryRoot().getAbsolutePath()
                        + File.separator
                        + LegendSampleImpl.LEGEND_SAMPLES_FOLDER
                        + File.separator
                        + sampleName
                        + ".png");
    }

    /**
     * Tests that not existing icons for workspace bound styles are created on disk in the workspace
     * styles folder.
     */
    @Test
    public void testCreatedLegendURLFromWorkspaceSize() throws Exception {

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList legendURLs = XPATH.getMatchingNodes(getLegendURLXPath("cite:states"), dom);

        assertEquals(1, legendURLs.getLength());
        Element legendURL = (Element) legendURLs.item(0);
        assertTrue(legendURL.hasAttribute("width"));
        assertFalse("20".equals(legendURL.getAttribute("width")));
        assertTrue(legendURL.hasAttribute("height"));
        assertFalse("20".equals(legendURL.getAttribute("height")));

        File sampleFile = getSampleFile("cite_states");
        assertTrue(sampleFile.exists());
    }

    /** Tests that already cached icons are recreated if related SLD is newer. */
    @Test
    public void testCachedLegendURLUpdatedSize() throws Exception {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource sldResource = loader.get(Paths.path("styles", "Bridges.sld"));
        File sampleFile = getSampleFile("Bridges");

        long lastTime = sampleFile.lastModified();
        long lastLength = sampleFile.length();
        long previousTime = sldResource.lastmodified();
        sldResource.file().setLastModified(lastTime + 1000);

        // force cleaning of samples cache, to get updates on files
        ((LegendSampleImpl) GeoServerExtensions.bean(LegendSample.class)).reloaded();

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList legendURLs = XPATH.getMatchingNodes(getLegendURLXPath("cite:Bridges"), dom);
        assertEquals(1, legendURLs.getLength());
        Element legendURL = (Element) legendURLs.item(0);
        assertTrue(legendURL.hasAttribute("width"));
        assertEquals("20", legendURL.getAttribute("width"));
        assertTrue(legendURL.hasAttribute("height"));
        assertEquals("20", legendURL.getAttribute("height"));
        assertFalse(getSampleFile("Bridges").length() == lastLength);
        sldResource.file().setLastModified(previousTime);
    }

    /**
     * Tests that already cached icons are recreated if related SLD is newer (using Catalog events).
     */
    @Test
    public void testCachedLegendURLUpdatedSize2() throws Exception {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource sldResource = loader.get(Paths.path("styles", "Bridges.sld"));
        File sampleFile = getSampleFile("Bridges");

        long lastTime = sampleFile.lastModified();
        long lastLength = sampleFile.length();
        long previousTime = sldResource.lastmodified();
        sldResource.file().setLastModified(lastTime + 1000);

        catalog.firePostModified(
                catalog.getStyleByName("Bridges"),
                new ArrayList<String>(),
                new ArrayList(),
                new ArrayList());

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList legendURLs = XPATH.getMatchingNodes(getLegendURLXPath("cite:Bridges"), dom);
        assertEquals(1, legendURLs.getLength());
        Element legendURL = (Element) legendURLs.item(0);
        assertTrue(legendURL.hasAttribute("width"));
        assertEquals("20", legendURL.getAttribute("width"));
        assertTrue(legendURL.hasAttribute("height"));
        assertEquals("20", legendURL.getAttribute("height"));
        assertFalse(getSampleFile("Bridges").length() == lastLength);
        sldResource.file().setLastModified(previousTime);
    }

    /** Tests that already cached icons are read from disk and used to calculate size. */
    @Test
    public void testOnlineResourceWidthHeight() throws Exception {

        TransformerBase tr = createTransformer();
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        NodeList onlineResources =
                XPATH.getMatchingNodes(getOnlineResourceXPath("cite:BasicPolygons"), dom);
        assertEquals(1, onlineResources.getLength());
        Element onlineResource = (Element) onlineResources.item(0);
        String href = onlineResource.getAttribute("xlink:href");
        assertNotNull(href);
        assertTrue(href.contains("width=20"));
        assertTrue(href.contains("height=20"));
    }

    private String getLegendURLXPath(String layerName) {
        return "/"
                + getElementPrefix()
                + getRootElement()
                + "/"
                + getElementPrefix()
                + "Capability/"
                + getElementPrefix()
                + "Layer/"
                + getElementPrefix()
                + "Layer["
                + getElementPrefix()
                + "Name/text()='"
                + layerName
                + "']/"
                + getElementPrefix()
                + "Style/"
                + getElementPrefix()
                + "LegendURL";
    }

    private String getOnlineResourceXPath(String layerName) {
        return "/"
                + getElementPrefix()
                + getRootElement()
                + "/"
                + getElementPrefix()
                + "Capability/"
                + getElementPrefix()
                + "Layer/"
                + getElementPrefix()
                + "Layer["
                + getElementPrefix()
                + "Name/text()='"
                + layerName
                + "']/"
                + getElementPrefix()
                + "Style/"
                + getElementPrefix()
                + "LegendURL/"
                + getElementPrefix()
                + "OnlineResource";
    }

    private void removeFileOrFolder(File file) {
        if (!file.exists()) {
            return;
        }

        if (!file.isDirectory()) {
            file.delete();
        } else {

            String[] list = file.list();
            for (int i = 0; i < list.length; i++) {
                removeFileOrFolder(new File(file.getAbsolutePath() + File.separator + list[i]));
            }

            file.delete();
        }
    }

    /** Each WMS version suite of tests has its own TransformerBase implementation. */
    protected abstract TransformerBase createTransformer();

    /** Each WMS version has a different root name for the Capabilities XML document. */
    protected abstract String getRootElement();

    /** Each WMS version uses a different element prefix. */
    protected abstract String getElementPrefix();
}
