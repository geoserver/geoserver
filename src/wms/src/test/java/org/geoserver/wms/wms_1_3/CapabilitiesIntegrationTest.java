/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLUnit.newXpathEngine;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.impl.AbstractDecorator;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.OpenLayersMapOutputFormat;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WMS 1.3 GetCapabilities integration tests
 * 
 * <p>
 * These tests are initialy ported from the 1.1.1 capabilities integration tests
 * </p>
 * 
 * @author Gabriel Roldan
 * 
 */
public class CapabilitiesIntegrationTest extends WMSTestSupport {
    
    static final String CONTAINER_GROUP = "containerGroup";

    public CapabilitiesIntegrationTest() {
        super();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();        
    }
    
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
       
        Catalog catalog = getCatalog();
        DataStoreInfo info =catalog.getDataStoreByName(MockData.SF_PREFIX);
        info.setEnabled(false);
        catalog.save(info);
        
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        
        // add a workspace qualified style
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        testData.addStyle(ws, "Lakes", "Lakes.sld", SystemTestData.class, catalog);
        StyleInfo lakesStyle = catalog.getStyleByName(ws, "Lakes");
        LayerInfo lakesLayer = catalog.getLayerByName(MockData.LAKES.getLocalPart());
        lakesLayer.setDefaultStyle(lakesStyle);
        catalog.save(lakesLayer);
        
        // create a group containing the other group
        LayerGroupInfo containerGroup = catalog.getFactory().createLayerGroup();
        LayerGroupInfo nature = catalog.getLayerGroupByName(NATURE_GROUP);
        containerGroup.setName(CONTAINER_GROUP);
        containerGroup.setMode(Mode.CONTAINER);
        containerGroup.getLayers().add(nature);
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.calculateLayerGroupBounds(containerGroup);
        catalog.add(containerGroup);
    }
    
    
    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }


  
    
    @org.junit.Test 
    public void testCapabilities() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
    }
    
    @org.junit.Test 
    public void testCapabilitiesNoWGS84DD() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        XMLAssert.assertXpathNotExists("//wms:CRS[text() = 'EPSG:WGS84(DD)']", dom);
    }

    @org.junit.Test 
    public void testGetCapsContainsNoDisabledTypes() throws Exception {

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        // see that disabled elements are disabled for good
        assertXpathEvaluatesTo("0", "count(//Name[text()='sf:PrimitiveGeoFeature'])", doc);

    }

    @org.junit.Test 
    public void testFilteredCapabilitiesCite() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0&namespace=cite"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wms:Layer/wms:Name[starts-with(., cite)]", dom)
                .getLength() > 0);
        assertEquals(0,
                xpath.getMatchingNodes("//wms:Layer/wms:Name[not(starts-with(., cite))]", dom)
                        .getLength());
    }

    @org.junit.Test 
    public void testLayerCount() throws Exception {
        List<LayerInfo> layers = new ArrayList<LayerInfo>(getCatalog().getLayers());
        for (ListIterator<LayerInfo> it = layers.listIterator(); it.hasNext();) {
            LayerInfo next = it.next();
            if (!next.enabled() || next.getName().equals(MockData.GEOMETRYLESS.getLocalPart())) {
                it.remove();
            }
        }
        List<LayerGroupInfo> groups = getCatalog().getLayerGroups();

        Document dom = dom(get("wms?request=GetCapabilities&version=1.3.0"), true);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        NodeList nodeLayers = xpath.getMatchingNodes(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Layer", dom);

        assertEquals(layers.size() + groups.size() - 1 /* nested layer group */, nodeLayers.getLength());
    }

    @org.junit.Test 
    public void testWorkspaceQualified() throws Exception {
        Document dom = dom(get("cite/wms?request=getCapabilities&version=1.3.0"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wms:Layer/wms:Name[starts-with(., cite)]", dom)
                .getLength() > 0);
        assertEquals(0,
                xpath.getMatchingNodes("//wms:Layer/wms:Name[not(starts-with(., cite))]", dom)
                        .getLength());

        NodeList nodes = xpath.getMatchingNodes("//wms:Layer//wms:OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            String attribute = e.getAttribute("xlink:href");
            assertTrue(attribute.contains("geoserver/cite/ows"));
        }

    }

    @org.junit.Test 
    public void testLayerQualified() throws Exception {
        // Qualify the request with a layer.  Other layers should not be included.
        Document dom = dom(get("cite/Forests/wms?service=WMS&request=getCapabilities&version=1.3.0"), true);
        // print(dom);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(0, xpath
                .getMatchingNodes("//wms:Layer/wms:Name[starts-with(., 'cite:Forests')]", dom)
                .getLength());
        assertEquals(1, xpath.getMatchingNodes("//wms:Layer/wms:Layer", dom).getLength());

        NodeList nodes = xpath.getMatchingNodes("//wms:Layer//wms:OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            String attribute = e.getAttribute("xlink:href");
            assertTrue(attribute.contains("geoserver/cite/Forests/ows"));
        }

    }

    @org.junit.Test
    public void testAttribution() throws Exception {
        // Uncomment the following lines if you want to use DTD validation for these tests
        // (by passing false as the second param to getAsDOM())
        // BUG: Currently, this doesn't seem to actually validate the document, although
        // 'validation' fails if the DTD is missing

        // GeoServerInfo global = getGeoServer().getGlobal();
        // global.setProxyBaseUrl("src/test/resources/geoserver");
        // getGeoServer().save(global);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("0", "count(//wms:Attribution)", doc);

        // Add attribution to one of the layers
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        AttributionInfo attr = points.getAttribution();

        attr.setTitle("Point Provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:Title)", doc);

        // Add href to same layer
        attr = points.getAttribution();
        attr.setHref("http://example.com/points/provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:Title)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:OnlineResource)", doc);

        // Add logo to same layer
        attr = points.getAttribution();
        attr.setLogoURL("http://example.com/points/logo");
        attr.setLogoType("image/logo");
        attr.setLogoHeight(50);
        attr.setLogoWidth(50);
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:Title)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:LogoURL)", doc);
    }
    
    @Test
    public void testLayerGroup() throws Exception {
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        CatalogBuilder builder = new CatalogBuilder(getCatalog());        
        
        //create layergr
        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
        //attribution
        lg.setName("MyLayerGroup");
        lg.getLayers().add(points);
        builder.calculateLayerGroupBounds(lg, CRS.decode("EPSG:4326"));
        lg.setAttribution(getCatalog().getFactory().createAttribution());
        lg.getAttribution().setTitle("My Attribution");
        MetadataLinkInfo info = getCatalog().getFactory().createMetadataLink();
        info.setType("text/html");
        info.setMetadataType("FGDC");
        info.setContent("http://my/metadata/link");
        lg.getMetadataLinks().add(info);
        getCatalog().add(lg);
        
        try {
            Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
            //print(doc);
            assertXpathEvaluatesTo("1", "count(//Layer[Name='MyLayerGroup']/Attribution)", doc);
            assertXpathEvaluatesTo("My Attribution", "//Layer[Name='MyLayerGroup']/Attribution/Title", doc);
            assertXpathEvaluatesTo("1", "count(//Layer[Name='MyLayerGroup']/MetadataURL)", doc);
            assertXpathEvaluatesTo("http://my/metadata/link", "//Layer[Name='MyLayerGroup']/MetadataURL/OnlineResource/@xlink:href", doc);
        } finally {    
            //clean up
            getCatalog().remove(lg);
        }
    }

    @org.junit.Test 
    public void testAlternateStyles() throws Exception {
        // add an alternate style to Fifteen
        StyleInfo pointStyle = getCatalog().getStyleByName("point");
        LayerInfo layer = getCatalog().getLayerByName("Fifteen");
        layer.getStyles().add(pointStyle);
        getCatalog().save(layer);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='cdf:Fifteen'])", doc);
        assertXpathEvaluatesTo("2", "count(//wms:Layer[wms:Name='cdf:Fifteen']/wms:Style)", doc);

        XpathEngine xpath = newXpathEngine();
        String href = xpath
                .evaluate(
                        "//wms:Layer[wms:Name='cdf:Fifteen']/wms:Style[wms:Name='Default']/wms:LegendURL/wms:OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=cdf%3AFifteen"));
        assertFalse(href.contains("style"));
        href = xpath
                .evaluate(
                        "//wms:Layer[wms:Name='cdf:Fifteen']/wms:Style[wms:Name='point']/wms:LegendURL/wms:OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=cdf%3AFifteen"));
        assertTrue(href.contains("style=point"));
    }

    @org.junit.Test
    public void testServiceMetadata() throws Exception {
        final WMSInfo service = getGeoServer().getService(WMSInfo.class);
        service.setTitle("test title");
        service.setAbstract("test abstract");
        service.setAccessConstraints("test accessConstraints");
        service.setFees("test fees");
        service.getKeywords().clear();
        service.getKeywords().add(new Keyword("test keyword 1"));
        service.getKeywords().add(new Keyword("test keyword 2"));
        service.setMaintainer("test maintainer");
        service.setOnlineResource("http://example.com/geoserver");
        GeoServerInfo global = getGeoServer().getGlobal();
        ContactInfo contact = global.getContact();
        contact.setAddress("__address");
        contact.setAddressCity("__city");
        contact.setAddressCountry("__country");
        contact.setAddressPostalCode("__ZIP");
        contact.setAddressState("__state");
        contact.setAddressType("__type");
        contact.setContactEmail("e@mail");
        contact.setContactOrganization("__org");
        contact.setContactFacsimile("__fax");
        contact.setContactPerson("__me");
        contact.setContactPosition("__position");
        contact.setContactVoice("__phone");
        
        getGeoServer().save(global);
        getGeoServer().save(service);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        //print(doc);

        String base = "wms:WMS_Capabilities/wms:Service/";
        assertXpathEvaluatesTo("WMS", base + "wms:Name", doc);
        assertXpathEvaluatesTo("test title", base + "wms:Title", doc);
        assertXpathEvaluatesTo("test abstract", base + "wms:Abstract", doc);
        assertXpathEvaluatesTo("test keyword 1", base + "wms:KeywordList/wms:Keyword[1]", doc);
        assertXpathEvaluatesTo("test keyword 2", base + "wms:KeywordList/wms:Keyword[2]", doc);
        assertXpathEvaluatesTo("http://example.com/geoserver", base + "wms:OnlineResource/@xlink:href", doc);
        
        String cinfo = base + "wms:ContactInformation/";
        assertXpathEvaluatesTo("__me", cinfo + "wms:ContactPersonPrimary/wms:ContactPerson", doc);
        assertXpathEvaluatesTo("__org", cinfo + "wms:ContactPersonPrimary/wms:ContactOrganization", doc);
        assertXpathEvaluatesTo("__position", cinfo + "wms:ContactPosition", doc);
        assertXpathEvaluatesTo("__type", cinfo + "wms:ContactAddress/wms:AddressType", doc);
        assertXpathEvaluatesTo("__address", cinfo + "wms:ContactAddress/wms:Address", doc);
        assertXpathEvaluatesTo("__city", cinfo + "wms:ContactAddress/wms:City", doc);
        assertXpathEvaluatesTo("__state", cinfo + "wms:ContactAddress/wms:StateOrProvince", doc);
        assertXpathEvaluatesTo("__ZIP", cinfo + "wms:ContactAddress/wms:PostCode", doc);
        assertXpathEvaluatesTo("__country", cinfo + "wms:ContactAddress/wms:Country", doc);
        assertXpathEvaluatesTo("__phone", cinfo + "wms:ContactVoiceTelephone", doc);
        assertXpathEvaluatesTo("__fax", cinfo + "wms:ContactFacsimileTelephone", doc);
        assertXpathEvaluatesTo("e@mail", cinfo + "wms:ContactElectronicMailAddress", doc);
    }
    
    @Test 
    public void testNoFeesOrContraints() throws Exception {
        final WMSInfo service = getGeoServer().getService(WMSInfo.class);
        service.setAccessConstraints(null);
        service.setFees(null);
        getGeoServer().save(service);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);

        String base = "wms:WMS_Capabilities/wms:Service/";
        assertXpathEvaluatesTo("WMS", base + "wms:Name", doc);
        assertXpathEvaluatesTo("none", base + "wms:Fees", doc);
        assertXpathEvaluatesTo("none", base + "wms:AccessConstraints", doc);
    }
    
    @Test
    public void testExceptions() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("XML", "wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format[1]", doc);
        assertXpathEvaluatesTo("INIMAGE", "wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format[2]", doc);
        assertXpathEvaluatesTo("BLANK", "wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format[3]", doc);
        assertXpathEvaluatesTo("JSON", "wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format[4]", doc);

        boolean jsonpOriginal = JSONType.isJsonpEnabled();
        try {
            JSONType.setJsonpEnabled(true);
            doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            assertXpathEvaluatesTo("JSONP", "wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format[5]", doc);
            assertXpathEvaluatesTo("5", "count(wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format)", doc);
            JSONType.setJsonpEnabled(false);
            doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            assertXpathEvaluatesTo("4", "count(wms:WMS_Capabilities/wms:Capability/wms:Exception/wms:Format)", doc);
        } finally {
            JSONType.setJsonpEnabled(jsonpOriginal);
        }
    }

    @org.junit.Test
    public void testQueryable() throws Exception {
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setQueryable(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setQueryable(false);
        getCatalog().save(points);

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathEvaluatesTo("1", "//wms:Layer[wms:Name='" + linesName + "']/@queryable", doc);
        assertXpathEvaluatesTo("0", "//wms:Layer[wms:Name='" + pointsName + "']/@queryable", doc);
    }
    
    @org.junit.Test
    public void testOpaque() throws Exception {
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setOpaque(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setOpaque(false);
        getCatalog().save(points);

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathEvaluatesTo("1", "//wms:Layer[wms:Name='" + linesName + "']/@opaque", doc);
        assertXpathEvaluatesTo("0", "//wms:Layer[wms:Name='" + pointsName + "']/@opaque", doc);
    }

    @org.junit.Test
    public void testKeywordVocab() throws Exception {
        FeatureTypeInfo lines = getFeatureTypeInfo(MockData.LINES);

        Keyword kw = new Keyword("foo");
        kw.setVocabulary("bar");
        lines.getKeywords().add(kw);
        
        getCatalog().save(lines);

        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        
        kw = new Keyword("baz");
        kw.setVocabulary("bar");
        wms.getKeywords().add(kw);
        getGeoServer().save(wms);
        
        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        String xpath = "//wms:Layer[wms:Name='" + linesName + "']/wms:KeywordList/wms:Keyword[@vocabulary='bar']";
        assertXpathExists(xpath, doc);
        assertXpathEvaluatesTo("foo", xpath, doc);

        xpath = "//wms:Service/wms:KeywordList/wms:Keyword[@vocabulary='bar']";
        assertXpathExists(xpath, doc);
        assertXpathEvaluatesTo("baz", xpath, doc);

    }
    
    @org.junit.Test
    public void testBoundingBoxCRS84() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathExists("/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'CRS:84']", doc);
        assertXpathExists("/wms:WMS_Capabilities/wms:Capability/wms:Layer//wms:Layer/wms:BoundingBox[@CRS = 'CRS:84']", doc);
    }
    
    @org.junit.Test 
    public void testMetadataLinks() throws Exception {
        String layerName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();
        
        LayerInfo layer = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        MetadataLinkInfo mdlink = getCatalog().getFactory().createMetadataLink();
        mdlink.setMetadataType("FGDC");
        mdlink.setContent("http://geoserver.org");
        mdlink.setType("text/xml");
        ResourceInfo resource = layer.getResource();
        resource.getMetadataLinks().add(mdlink);
        getCatalog().save(resource);
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        String xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/wms:Format";
        assertXpathEvaluatesTo("text/xml", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/@type";
        assertXpathEvaluatesTo("FGDC", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/wms:OnlineResource/@xlink:type";
        assertXpathEvaluatesTo("simple", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/wms:OnlineResource/@xlink:href";
        assertXpathEvaluatesTo("http://geoserver.org", xpath, doc);
        
        // Test transforming localhost to proxyBaseUrl
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = global.getSettings().getProxyBaseUrl();
        mdlink.setContent("/metadata");
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata", xpath, doc);
        
        // Test KVP in URL
        String query = "key=value";
        mdlink.setContent("/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata?" + query, xpath, doc);

        mdlink.setContent("http://localhost/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("http://localhost/metadata?" + query, xpath, doc);
        
    }
    
    @org.junit.Test 
    public void testDataLinks() throws Exception {
        String layerName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();
        
        LayerInfo layer = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        DataLinkInfo mdlink = getCatalog().getFactory().createDataLink();
        mdlink.setContent("http://geoserver.org");
        mdlink.setType("text/xml");
        ResourceInfo resource = layer.getResource();
        resource.getDataLinks().add(mdlink);
        getCatalog().save(resource);
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        String xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:DataURL/wms:Format";
        assertXpathEvaluatesTo("text/xml", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:DataURL/wms:OnlineResource/@xlink:type";
        assertXpathEvaluatesTo("simple", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:DataURL/wms:OnlineResource/@xlink:href";
        assertXpathEvaluatesTo("http://geoserver.org", xpath, doc);
        
        // Test transforming localhost to proxyBaseUrl
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = global.getSettings().getProxyBaseUrl();
        mdlink.setContent("/metadata");
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata", xpath, doc);
        
        // Test KVP in URL
        String query = "key=value";
        mdlink.setContent("/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata?" + query, xpath, doc);

        mdlink.setContent("http://localhost/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("http://localhost/metadata?" + query, xpath, doc);
        
    }
    
    // [GEOS-6312] OpenLayers output format is not listed in WMS 1.3 capabilities document
    @Test
    public void testOpenlayersFormat() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("1", "count(//wms:GetMap[wms:Format = '" + OpenLayersMapOutputFormat.MIME_TYPE + "'])", doc);
    }

    @Test
    public void testStyleWorkspaceQualified() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        // check the style name got prefixed too
        assertXpathEvaluatesTo("cite:Lakes",
                "//wms:Layer[wms:Name='cite:Lakes']/wms:Style[1]/wms:Name", doc);
    }
    
    @Test
    public void testDuplicateLayerGroup() throws Exception {
        // see https://osgeo-org.atlassian.net/browse/GEOS-6154
        Catalog catalog = getCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakes.setAdvertised(false);
        catalog.save(lakes);
        
        try {
            Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            // print(doc);
            
            // should show up just once
            assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='nature'])", doc);
            assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Title='containerGroup']/wms:Layer[wms:Name='nature'])", doc);
        } finally {
            lakes.setAdvertised(true);
            catalog.save(lakes);
        }
    }

    @Test
    public void testGlobalBoundingBoxForLayerGroups() throws Exception {
        Catalog catalog = getCatalog();
        // create a new workspace for this tests
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName("NON_ADVERTISED");
        NamespaceInfoImpl nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("NON_ADVERTISED");
        nameSpace.setURI("http://non-advertised.org");
        // remove all layer groups and store them
        List<LayerGroupInfo> layerGroups = catalog.getLayerGroups().stream()
                .map(this::unwrapLayerGroup).collect(Collectors.toList());
        catalog.getLayerGroups().forEach(layerGroup -> new CascadeDeleteVisitor(catalog).visit(layerGroup));
        try {
            catalog.add(workspace);
            catalog.add(nameSpace);
            // add some layers by duplicating existing layers and create a layer group
            LayerInfo layer1 = cloneVectorLayerIntoWorkspace(workspace, nameSpace, MockData.BUILDINGS.getLocalPart());
            LayerInfo layer2 = cloneVectorLayerIntoWorkspace(workspace, nameSpace, MockData.BRIDGES.getLocalPart());
            LayerGroupInfo layerGroup = createLayerGroup(workspace, "NON_ADVERTISED", layer1, layer2);
            // reduce layer group bounds and store the original bounds which correspond to all layers bounds
            ReferencedEnvelope layersBounds = layerGroup.getBounds();
            ReferencedEnvelope layerGroupBounds = new ReferencedEnvelope(-10, 10, -20, 20, layersBounds.getCoordinateReferenceSystem());
            layerGroup.setBounds(layerGroupBounds);
            catalog.save(layerGroup);
            // perform a get capabilities request targeting only the created workspace
            Document document = getAsDOM("NON_ADVERTISED/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            checkGlobalBoundingBox(layersBounds, document);
            // make layers non advertised
            layer1.setAdvertised(false);
            layer2.setAdvertised(false);
            catalog.save(layer1);
            catalog.save(layer2);
            // perform a get capabilities request targeting only the created workspace
            document = getAsDOM("NON_ADVERTISED/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            checkGlobalBoundingBox(layerGroupBounds, document);
        } finally {
            // add layer groups back
            layerGroups.forEach(catalog::add);
            // remove the created workspace and namespace
            CascadeDeleteVisitor deleteVisitor = new CascadeDeleteVisitor(catalog);
            deleteVisitor.visit(workspace);
            catalog.remove(nameSpace);
            catalog.remove(workspace);
        }
    }

    @Test
    public void testLayerGroupsPerWorkspace() throws Exception {
        Catalog catalog = getCatalog();
        // create workspace A
        WorkspaceInfoImpl workspaceA = new WorkspaceInfoImpl();
        workspaceA.setName("LG_TEST_WORKSPACE_A");
        NamespaceInfoImpl nameSpaceA = new NamespaceInfoImpl();
        nameSpaceA.setPrefix("LG_TEST_WORKSPACE_A");
        nameSpaceA.setURI("http://lg-test-workspace-a.org");
        // create workspace B
        WorkspaceInfoImpl workspaceB = new WorkspaceInfoImpl();
        workspaceB.setName("LG_TEST_WORKSPACE_B");
        NamespaceInfoImpl nameSpaceB = new NamespaceInfoImpl();
        nameSpaceB.setPrefix("LG_TEST_WORKSPACE_B");
        nameSpaceB.setURI("http://lg-test-workspace-b.org");
        // keep global layers group reference
        LayerGroupInfo globalLayerGroup = null;
        try {
            // save workspace A
            catalog.add(workspaceA);
            catalog.add(nameSpaceA);
            // save workspace B
            catalog.add(workspaceB);
            catalog.add(nameSpaceB);
            // create layers groups
            createLayerGroup(workspaceA, "LAYER_GROUP_A",
                    cloneVectorLayerIntoWorkspace(workspaceA, nameSpaceA, MockData.BUILDINGS.getLocalPart()),
                    cloneVectorLayerIntoWorkspace(workspaceA, nameSpaceA, MockData.BRIDGES.getLocalPart()));
            createLayerGroup(workspaceB, "LAYER_GROUP_B",
                    cloneVectorLayerIntoWorkspace(workspaceB, nameSpaceB, MockData.BUILDINGS.getLocalPart()),
                    cloneVectorLayerIntoWorkspace(workspaceB, nameSpaceB, MockData.BRIDGES.getLocalPart()));
            globalLayerGroup = createLayerGroup("LAYER_GROUP_C",
                    catalog.getLayerByName(MockData.BUILDINGS.getLocalPart()),
                    catalog.getLayerByName(MockData.BRIDGES.getLocalPart()));
            // perform a get capabilities request targeting the global service
            Document document = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            assertXpathEvaluatesTo("1", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LG_TEST_WORKSPACE_A:LAYER_GROUP_A'])", document);
            assertXpathEvaluatesTo("1", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LG_TEST_WORKSPACE_B:LAYER_GROUP_B'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_A'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_B'])", document);
            assertXpathEvaluatesTo("1", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_C'])", document);
            // perform a get capabilities request targeting workspace A service
            document = getAsDOM("LG_TEST_WORKSPACE_A/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LG_TEST_WORKSPACE_A:LAYER_GROUP_A'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LG_TEST_WORKSPACE_B:LAYER_GROUP_B'])", document);
            assertXpathEvaluatesTo("1", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_A'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_B'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_C'])", document);
            // perform a get capabilities request targeting workspace B service
            document = getAsDOM("LG_TEST_WORKSPACE_B/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LG_TEST_WORKSPACE_A:LAYER_GROUP_A'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LG_TEST_WORKSPACE_B:LAYER_GROUP_B'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_A'])", document);
            assertXpathEvaluatesTo("1", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_B'])", document);
            assertXpathEvaluatesTo("0", "count(//wms:Capability/wms:Layer/wms:Layer[wms:Name='LAYER_GROUP_C'])", document);
        } finally {
            // remove the created workspaces and namespaces
            CascadeDeleteVisitor deleteVisitor = new CascadeDeleteVisitor(catalog);
            deleteVisitor.visit(workspaceA);
            catalog.remove(nameSpaceA);
            catalog.remove(workspaceA);
            deleteVisitor.visit(workspaceB);
            catalog.remove(nameSpaceB);
            catalog.remove(workspaceB);
            // remove global layer group
            if (globalLayerGroup != null) {
                catalog.remove(globalLayerGroup);
            }
        }
    }

    /**
     * Check that the global bounding box matches the expected envelope
     */
    private void checkGlobalBoundingBox(ReferencedEnvelope expectedBoundingBox, Document capabilitiesResult) throws Exception {
        // check that the returned capabilities document contains the correct bounding box
        XpathEngine xpath = newXpathEngine();
        // extract bounding box values from the capabilities document
        String minX = xpath.evaluate("//wms:Capability/wms:Layer/wms:EX_GeographicBoundingBox/wms:westBoundLongitude/text()", capabilitiesResult);
        String maxX = xpath.evaluate("//wms:Capability/wms:Layer/wms:EX_GeographicBoundingBox/wms:eastBoundLongitude/text()", capabilitiesResult);
        String minY = xpath.evaluate("//wms:Capability/wms:Layer/wms:EX_GeographicBoundingBox/wms:southBoundLatitude/text()", capabilitiesResult);
        String maxY = xpath.evaluate("//wms:Capability/wms:Layer/wms:EX_GeographicBoundingBox/wms:northBoundLatitude/text()", capabilitiesResult);
        // check bounding box values
        checkNumberSimilar(minX, expectedBoundingBox.getMinX(), 0.0001);
        checkNumberSimilar(maxX, expectedBoundingBox.getMaxX(), 0.0001);
        checkNumberSimilar(minY, expectedBoundingBox.getMinY(), 0.0001);
        checkNumberSimilar(maxY, expectedBoundingBox.getMaxY(), 0.0001);
    }

    /**
     * Check that a number represent by a string is similar to the expected number
     * A number is considered similar to another if the difference between them is
     * inferior or equal to the provided precision.
     */
    private void checkNumberSimilar(String rawValue, double expected, double precision) {
        // try to extract a double value
        assertThat(rawValue, is(notNullValue()));
        assertThat(rawValue.trim().isEmpty(), is(false));
        double value = 0;
        try {
            value = Double.parseDouble(rawValue);
        } catch (NumberFormatException exception) {
            fail(String.format("Value '%s' is not a number.", rawValue));
        }
        // compare the parsed double value with the expected one
        double difference = Math.abs(expected - value);
        assertThat(difference <= precision, is(true));
    }

    /**
     * Helper method that unwraps a layer group making him suitable to be added
     * to the catalog. If proxyfied the proxy will also be removed.
     */
    private LayerGroupInfo unwrapLayerGroup(LayerGroupInfo layerGroup) {
        // get the original layer group object
        while (layerGroup instanceof AbstractDecorator) {
            AbstractDecorator decorator = (AbstractDecorator) layerGroup;
            layerGroup = (LayerGroupInfo) decorator.unwrap(LayerGroupInfo.class);
        }
        // catalog detach doesn't work for layer groups
        if (Proxy.isProxyClass(layerGroup.getClass())) {
            // we have a proxy we need to get rid of the proxy
            ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(layerGroup);
            proxy.commit();
            layerGroup = (LayerGroupInfo) proxy.getProxyObject();
        }
        return layerGroup;
    }

    /**
     * Helper method that creates a layer group using the provided name and layers.
     */
    private LayerGroupInfo createLayerGroup(String layerGroupName, LayerInfo... layers) throws Exception {
        return createLayerGroup(null, layerGroupName, layers);
    }

    /**
     * Helper method that creates a layer group using the provided name, workspace and layers.
     */
    private LayerGroupInfo createLayerGroup(WorkspaceInfo workspace, String layerGroupName, LayerInfo... layers) throws Exception {
        // create a new layer group using the provided name
        LayerGroupInfo layerGroup = getCatalog().getFactory().createLayerGroup();
        layerGroup.setName(layerGroupName);
        // set workspace
        layerGroup.setWorkspace(workspace);
        // add the provided layers
        for (LayerInfo layerInfo : layers) {
            layerGroup.getLayers().add(layerInfo);
        }
        // set the layer group bounds by merging all layers bounds
        CatalogBuilder catalogBuilder = new CatalogBuilder(getCatalog());
        catalogBuilder.calculateLayerGroupBounds(layerGroup);
        getCatalog().add(layerGroup);
        // retrieve the created layer group by name
        if (workspace == null) {
            return getCatalog().getLayerGroupByName(layerGroupName);
        }
        return getCatalog().getLayerGroupByName(workspace, layerGroupName);
    }

    /**
     * Helper method that clones vector layer into a certain workspace.
     */
    private LayerInfo cloneVectorLayerIntoWorkspace(WorkspaceInfoImpl workspace, NamespaceInfoImpl nameSpace, String layerName) {
        Catalog catalog = getCatalog();
        // get the original object from the catalog
        LayerInfo originalLayerInfo = catalog.getLayerByName(layerName);
        FeatureTypeInfo originalFeatureTypeInfo = (FeatureTypeInfo) originalLayerInfo.getResource();
        DataStoreInfo originalStoreInfo = originalFeatureTypeInfo.getStore();
        // copy the data store, changing is workspace, id and name
        DataStoreInfoImpl copyDataStoreInfo = new DataStoreInfoImpl(catalog);
        OwsUtils.copy(originalStoreInfo, copyDataStoreInfo, DataStoreInfo.class);
        copyDataStoreInfo.setId(UUID.randomUUID().toString());
        copyDataStoreInfo.setName(UUID.randomUUID().toString());
        copyDataStoreInfo.setWorkspace(workspace);
        // copy the feature type info, changing the data store and name space
        FeatureTypeInfoImpl copyFeatureTypeInfo = new FeatureTypeInfoImpl(catalog);
        OwsUtils.copy(originalFeatureTypeInfo, copyFeatureTypeInfo, FeatureTypeInfo.class);
        copyFeatureTypeInfo.setNamespace(nameSpace);
        copyFeatureTypeInfo.setStore(copyDataStoreInfo);
        // copy the layer, changing the feature type
        LayerInfoImpl copyLayerInfo = new LayerInfoImpl();
        OwsUtils.copy(originalLayerInfo, copyLayerInfo, LayerInfo.class);
        copyLayerInfo.setId(layerName);
        copyLayerInfo.setName(layerName);
        copyLayerInfo.setResource(copyFeatureTypeInfo);
        // add everything to the catalog
        catalog.add(copyDataStoreInfo);
        catalog.add(copyFeatureTypeInfo);
        catalog.add(copyLayerInfo);
        // retrieve the cloned layer by name
        return catalog.getLayerByName(workspace.getName() + ":" + layerName);
    }
}
