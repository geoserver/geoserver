/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetCapabilitiesTransformerTest extends WMSTestSupport {

    /** default base url to feed a GetCapabilitiesTransformer with for it to append the DTD location */
    private static final String baseUrl = "http://localhost/geoserver";

    /**
     * a mocked up {@link GeoServer} config, almost empty after setUp(), except for the {@link WMSInfo},
     * {@link GeoServerInfo} and empty {@link Catalog}, Specific tests should add content as needed
     */
    private GeoServerImpl geosConfig;

    /** a mocked up {@link GeoServerInfo} for {@link #geosConfig}. Specific tests should set its properties as needed */
    private GeoServerInfoImpl geosInfo;

    /**
     * a mocked up {@link WMSInfo} for {@link #geosConfig}, empty except for the WMSInfo after setUp(), Specific tests
     * should set its properties as needed
     */
    private WMSInfoImpl wmsInfo;

    /**
     * a mocked up {@link Catalog} for {@link #geosConfig}, empty after setUp(), Specific tests should add content as
     * needed
     */
    private CatalogImpl catalog;

    private GetCapabilitiesRequest req;

    private WMS wmsConfig;

    /**
     * Sets up the configuration objects with default values. Since they're live, specific tests can modify their state
     * before running the assertions
     */
    @Before
    public void setUp() throws Exception {
        geosConfig = new GeoServerImpl();

        geosInfo = new GeoServerInfoImpl(geosConfig);
        geosInfo.setContact(new ContactInfoImpl());
        geosConfig.setGlobal(geosInfo);

        wmsInfo = new WMSInfoImpl();
        geosConfig.add(wmsInfo);

        catalog = new CatalogImpl();
        geosConfig.setCatalog(catalog);

        wmsConfig = new WMS(geosConfig);

        req = new GetCapabilitiesRequest();
        req.setBaseUrl(baseUrl);
        req.setVersion("1.1.1");

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wms", "http://www.opengis.net/wms");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Test
    public void testDisableDefaultLayerGroupStyle1_3() throws Exception {
        // test that when defaultGroupStyleEnabled
        // is set to false the default layerGroup style doesn't appears in
        // getCapabilities resp if mode is not single nor opaque.
        String layerGroupName = "aLayerGroup";
        createLayerGroup(layerGroupName);

        Capabilities_1_3_0_Transformer tr = new Capabilities_1_3_0_Transformer(
                wmsConfig,
                baseUrl,
                wmsConfig.getAllowedMapFormats(),
                wmsConfig.getAvailableExtendedCapabilitiesProviders());
        Document dom = WMSTestSupport.transform(req, tr);

        // default case, the style should be present
        NodeList nodeList = dom.getElementsByTagName("Style");
        assertEquals(1, nodeList.getLength());
        Element styleEl = (Element) nodeList.item(0);
        String title = styleEl.getElementsByTagName("Title").item(0).getTextContent();
        assertEquals("aLayerGroup style", title);
        WMS wms = new WMS(geosConfig) {
            @Override
            public boolean isDefaultGroupStyleEnabled() {
                return false;
            }
        };
        Capabilities_1_3_0_Transformer tr2 = new Capabilities_1_3_0_Transformer(
                wms, baseUrl, wms.getAllowedMapFormats(), wms.getAvailableExtendedCapabilitiesProviders());
        Document dom2 = WMSTestSupport.transform(req, tr2);
        nodeList = dom2.getElementsByTagName("Style");
        // the style won't appear
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testDefaultLayerGroupStyle1_3ModeOpaque() throws Exception {
        // test that when defaultGroupStyleEnabled
        // is set to false the default layerGroup style appears in
        // getCapabilities resp if mode is opaque.
        String layerGroupName = "aLayerGroup";
        createLayerGroup(layerGroupName, LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        WMS wms = new WMS(geosConfig) {
            @Override
            public boolean isDefaultGroupStyleEnabled() {
                return false;
            }
        };
        Capabilities_1_3_0_Transformer tr = new Capabilities_1_3_0_Transformer(
                wms, baseUrl, wmsConfig.getAllowedMapFormats(), wmsConfig.getAvailableExtendedCapabilitiesProviders());
        Document dom = WMSTestSupport.transform(req, tr);

        // the style should appear
        NodeList nodeList = dom.getElementsByTagName("Style");
        assertEquals(1, nodeList.getLength());
        Element styleEl = (Element) nodeList.item(0);
        String title = styleEl.getElementsByTagName("Title").item(0).getTextContent();
        assertEquals("aLayerGroup style", title);
        String name = styleEl.getElementsByTagName("Name").item(0).getTextContent();
        assertEquals("default-style-aLayerGroup", name);
    }

    private void createLayerGroup(String layerGroupName) throws FactoryException {
        createLayerGroup(layerGroupName, LayerGroupInfo.Mode.NAMED);
    }

    private void createLayerGroup(String layerGroupName, LayerGroupInfo.Mode mode) throws FactoryException {
        StyleInfo styleInfo = this.catalog.getFactory().createStyle();
        styleInfo.setName("testStyle");
        styleInfo.setFilename("testStyle.sld");
        this.catalog.add(styleInfo);

        NamespaceInfo namespaceInfo = this.catalog.getFactory().createNamespace();
        namespaceInfo.setURI("http://test");
        namespaceInfo.setPrefix("test");
        this.catalog.add(namespaceInfo);

        WorkspaceInfo workspaceInfo = this.catalog.getFactory().createWorkspace();
        workspaceInfo.setName("testDatastore");
        this.catalog.add(workspaceInfo);

        WMSStoreInfo wmsStoreInfo = this.catalog.getFactory().createWebMapServer();
        wmsStoreInfo.setName("testDatastore");
        wmsStoreInfo.setWorkspace(workspaceInfo);
        this.catalog.add(wmsStoreInfo);

        WMSLayerInfo wmsLayerInfo = this.catalog.getFactory().createWMSLayer();
        wmsLayerInfo.setName("testDatastore:testLayer");
        wmsLayerInfo.setStore(wmsStoreInfo);
        wmsLayerInfo.setNamespace(namespaceInfo);
        this.catalog.add(wmsLayerInfo);

        LayerInfo layerInfo = this.catalog.getFactory().createLayer();
        layerInfo.setDefaultStyle(styleInfo);
        layerInfo.setResource(wmsLayerInfo);
        this.catalog.add(layerInfo);

        CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope nativeBounds = new ReferencedEnvelope(-180, 180, -90, 90, nativeCrs);

        LayerGroupInfo layerGroupInfo = this.catalog.getFactory().createLayerGroup();
        layerGroupInfo.setName(layerGroupName);
        layerGroupInfo.setBounds(nativeBounds);
        layerGroupInfo.setMode(mode);
        layerGroupInfo.getLayers().add(layerInfo);
        this.catalog.add(layerGroupInfo);
    }

    @Test
    public void testLayerStyleSections() throws Exception {
        // Given
        String LAYER_GROUP_NAME = "testLayerGroup";
        createLayerGroup(LAYER_GROUP_NAME);

        Capabilities_1_3_0_Transformer tr130 = new Capabilities_1_3_0_Transformer(
                wmsConfig,
                baseUrl,
                wmsConfig.getAllowedMapFormats(),
                wmsConfig.getAvailableExtendedCapabilitiesProviders());
        Document tr130Dom = WMSTestSupport.transform(req, tr130);
        Element tr130Root = tr130Dom.getDocumentElement();

        // Then
        assertEquals("WMS_Capabilities", tr130Root.getNodeName());
        assertEquals(1, tr130Dom.getElementsByTagName("Style").getLength());
    }
}
