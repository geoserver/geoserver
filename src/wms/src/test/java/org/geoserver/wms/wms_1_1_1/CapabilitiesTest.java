/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.custommonkey.xmlunit.XMLUnit.newXpathEngine;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CapabilitiesTest extends WMSTestSupport {
    
    static final String CONTAINER_GROUP = "containerGroup";

    public CapabilitiesTest() {
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
        testData.addStyle(ws, "tiger_roads", "tiger_roads.sld", SystemTestData.class, catalog);
        StyleInfo lakesStyle = catalog.getStyleByName(ws, "Lakes");
        LayerInfo lakesLayer = catalog.getLayerByName(MockData.LAKES.getLocalPart());
        lakesLayer.setDefaultStyle(lakesStyle);
        StyleInfo tigerRoadsStyle = catalog.getStyleByName(ws, "tiger_roads");
        lakesLayer.getStyles().add(tigerRoadsStyle);
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


    @Test
    public void testCapabilities() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
    }

    @Test
    public void testGetCapsContainsNoDisabledTypes() throws Exception {

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);
        assertEquals("WMT_MS_Capabilities", doc.getDocumentElement().getNodeName());

        // see that disabled elements are disabled for good
        assertXpathEvaluatesTo("0", "count(//Name[text()='sf:PrimitiveGeoFeature'])", doc);

    }

    @Test
    public void testFilteredCapabilitiesCite() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&namespace=cite"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//Layer/Name[starts-with(., cite)]", dom).getLength() > 0);
        assertEquals(0, xpath.getMatchingNodes("//Layer/Name[not(starts-with(., cite))]", dom)
                .getLength());
    }

    @Test
    public void testLayerCount() throws Exception {
        List<LayerInfo> layers = new ArrayList<LayerInfo>(getCatalog().getLayers());
        for (ListIterator<LayerInfo> it = layers.listIterator(); it.hasNext();) {
            LayerInfo next = it.next();
            if (!next.enabled() || next.getName().equals(MockData.GEOMETRYLESS.getLocalPart())) {
                it.remove();
            }
        }
        List<LayerGroupInfo> groups = getCatalog().getLayerGroups();

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), true);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        NodeList nodeLayers = xpath.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/Layer",
                dom);

        assertEquals(layers.size() + groups.size() - 1 /* nested group */, nodeLayers.getLength());
    }

    @Test
    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), true);
            assertXpathExists("//Layer[Name='" + layerId + "']", dom);
            
            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            dom = dom(get("wms?request=getCapabilities&version=1.1.1"), true);
            assertXpathNotExists("//Layer[Name='" + layerId + "']", dom);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }
    
    @Test
    public void testNonAdvertisedLayerInLayerSpecificService() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        String context = layerId.replace(":", "/");
        String localName = MockData.BUILDINGS.getLocalPart();
        try {
            // now you see me
            Document dom = dom(get(context + "/wms?request=getCapabilities&version=1.1.1"), true);
            assertXpathExists("//Layer[Name='" + localName + "']", dom);

            // now you... still do :-)
            layer.setAdvertised(false);
            getCatalog().save(layer);
            dom = dom(get(context + "/wms?request=getCapabilities&version=1.1.1"), true);
            assertXpathExists("//Layer[Name='" + localName + "']", dom);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        Document dom = dom(get("cite/wms?request=getCapabilities&version=1.1.1"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(0, xpath.getMatchingNodes("//Layer/Name[starts-with(., 'cite')]", dom).getLength());
        assertTrue (xpath.getMatchingNodes("//Layer/Name[not(starts-with(., 'cite'))]", dom)
                .getLength() > 0 );

        NodeList nodes = xpath.getMatchingNodes("//Layer//OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            String attribute = e.getAttribute("xlink:href");
            assertTrue(attribute.contains("geoserver/cite/wms"));
        }

    }

    @Test
    public void testLayerQualified() throws Exception {
        Document dom = dom(get("cite/Forests/wms?request=getCapabilities&version=1.1.1"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(0, xpath.getMatchingNodes("//Layer/Name[starts-with(., 'cite:Forests')]", dom)
                .getLength());
        assertEquals(1, xpath.getMatchingNodes("//Layer[Name = 'Forests']", dom)
                .getLength());

        NodeList nodes = xpath.getMatchingNodes("//Layer//OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            assertTrue(e.getAttribute("xlink:href").contains("geoserver/cite/Forests/wms"));
        }

    }

    @Test
    public void testAttribution() throws Exception {
        // Uncomment the following lines if you want to use DTD validation for these tests
        // (by passing false as the second param to getAsDOM())
        // BUG: Currently, this doesn't seem to actually validate the document, although
        // 'validation' fails if the DTD is missing

        // GeoServerInfo global = getGeoServer().getGlobal();
        // global.setProxyBaseUrl("src/test/resources/geoserver");
        // getGeoServer().save(global);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo("0", "count(//Attribution)", doc);

        // Add attribution to one of the layers
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        AttributionInfo attr = points.getAttribution();

        attr.setTitle("Point Provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo("1", "count(//Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/Title)", doc);

        // Add href to same layer
        attr = points.getAttribution();
        attr.setHref("http://example.com/points/provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/Title)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/OnlineResource)", doc);

        // Add logo to same layer
        attr = points.getAttribution();
        attr.setLogoURL("http://example.com/points/logo");
        attr.setLogoType("image/logo");
        attr.setLogoHeight(50);
        attr.setLogoWidth(50);
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/Title)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/LogoURL)", doc);
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

    @Test
    public void testAlternateStyles() throws Exception {
        // add an alternate style to Fifteen
        StyleInfo pointStyle = getCatalog().getStyleByName("point");
        LayerInfo layer = getCatalog().getLayerByName("Fifteen");
        layer.getStyles().add(pointStyle);
        getCatalog().save(layer);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//Layer[Name='cdf:Fifteen'])", doc);
        assertXpathEvaluatesTo("2", "count(//Layer[Name='cdf:Fifteen']/Style)", doc);

        XpathEngine xpath = newXpathEngine();
        String href = xpath
                .evaluate(
                        "//Layer[Name='cdf:Fifteen']/Style[Name='Default']/LegendURL/OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=cdf%3AFifteen"));
        assertFalse(href.contains("style"));
        href = xpath
                .evaluate(
                        "//Layer[Name='cdf:Fifteen']/Style[Name='point']/LegendURL/OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=cdf%3AFifteen"));
        assertTrue(href.contains("style=point"));
    }

    @Test 
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

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        String base = "WMT_MS_Capabilities/Service/";
        assertXpathEvaluatesTo("OGC:WMS", base + "Name", doc);
        assertXpathEvaluatesTo("test title", base + "Title", doc);
        assertXpathEvaluatesTo("test abstract", base + "Abstract", doc);
        assertXpathEvaluatesTo("test keyword 1", base + "KeywordList/Keyword[1]", doc);
        assertXpathEvaluatesTo("test keyword 2", base + "KeywordList/Keyword[2]", doc);
        assertXpathEvaluatesTo("http://example.com/geoserver", base + "OnlineResource/@xlink:href", doc);
        
        String cinfo = base + "ContactInformation/";
        assertXpathEvaluatesTo("__me", cinfo + "ContactPersonPrimary/ContactPerson", doc);
        assertXpathEvaluatesTo("__org", cinfo + "ContactPersonPrimary/ContactOrganization", doc);
        assertXpathEvaluatesTo("__position", cinfo + "ContactPosition", doc);
        assertXpathEvaluatesTo("__type", cinfo + "ContactAddress/AddressType", doc);
        assertXpathEvaluatesTo("__address", cinfo + "ContactAddress/Address", doc);
        assertXpathEvaluatesTo("__city", cinfo + "ContactAddress/City", doc);
        assertXpathEvaluatesTo("__state", cinfo + "ContactAddress/StateOrProvince", doc);
        assertXpathEvaluatesTo("__ZIP", cinfo + "ContactAddress/PostCode", doc);
        assertXpathEvaluatesTo("__country", cinfo + "ContactAddress/Country", doc);
        assertXpathEvaluatesTo("__phone", cinfo + "ContactVoiceTelephone", doc);
        assertXpathEvaluatesTo("__fax", cinfo + "ContactFacsimileTelephone", doc);
        assertXpathEvaluatesTo("e@mail", cinfo + "ContactElectronicMailAddress", doc);
    }
    
    @Test 
    public void testNoFeesOrContraints() throws Exception {
        final WMSInfo service = getGeoServer().getService(WMSInfo.class);
        service.setAccessConstraints(null);
        service.setFees(null);
        getGeoServer().save(service);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        String base = "WMT_MS_Capabilities/Service/";
        assertXpathEvaluatesTo("OGC:WMS", base + "Name", doc);
        assertXpathEvaluatesTo("none", base + "Fees", doc);
        assertXpathEvaluatesTo("none", base + "AccessConstraints", doc);
    }

    @Test
    public void testQueryable() throws Exception{
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setQueryable(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setQueryable(false);
        getCatalog().save(points);        

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        assertXpathEvaluatesTo("1", "//Layer[Name='" + linesName + "']/@queryable", doc);
        assertXpathEvaluatesTo("0", "//Layer[Name='" + pointsName + "']/@queryable", doc);
    }

    @Test
    public void testOpaque() throws Exception{
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setOpaque(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setOpaque(false);
        getCatalog().save(points);        

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);

        assertXpathEvaluatesTo("1", "//Layer[Name='" + linesName + "']/@opaque", doc);
        assertXpathEvaluatesTo("0", "//Layer[Name='" + pointsName + "']/@opaque", doc);
    }

    @Test
    public void testExceptions() throws Exception{

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);

        XpathEngine xpath = XMLUnit.newXpathEngine();

        assertTrue(xpath.evaluate("//Exception/Format[1]", doc).equals("application/vnd.ogc.se_xml"));
        assertTrue(xpath.evaluate("//Exception/Format[2]", doc).equals("application/vnd.ogc.se_inimage"));
        assertTrue(xpath.evaluate("//Exception/Format[3]", doc).equals("application/vnd.ogc.se_blank"));
        assertTrue(xpath.evaluate("//Exception/Format[4]", doc).equals("application/json"));
        assertTrue(xpath.getMatchingNodes("//Exception/Format", doc).getLength() >= 4);

        boolean jsonpOriginal = JSONType.isJsonpEnabled();
        try {
            JSONType.setJsonpEnabled(true);
            doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
            assertTrue(xpath.evaluate("//Exception/Format[5]", doc).equals("text/javascript"));
            assertTrue(xpath.getMatchingNodes("//Exception/Format", doc).getLength() == 5);
            JSONType.setJsonpEnabled(false);
            doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
            assertTrue(xpath.getMatchingNodes("//Exception/Format", doc).getLength() == 4);
        } finally {
            JSONType.setJsonpEnabled(jsonpOriginal);
        }
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
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        String xpath = "//Layer[Name='" + layerName + "']/DataURL/Format";
        assertXpathEvaluatesTo("text/xml", xpath, doc);
        
        xpath = "//Layer[Name='" + layerName + "']/DataURL/OnlineResource/@xlink:type";
        assertXpathEvaluatesTo("simple", xpath, doc);
        
        xpath = "//Layer[Name='" + layerName + "']/DataURL/OnlineResource/@xlink:href";
        assertXpathEvaluatesTo("http://geoserver.org", xpath, doc);
        
        // Test transforming localhost to proxyBaseUrl
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = global.getSettings().getProxyBaseUrl();
        mdlink.setContent("/metadata");
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata", xpath, doc);
        
        // Test KVP in URL
        String query = "key=value";
        mdlink.setContent("/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata?" + query, xpath, doc);

        mdlink.setContent("http://localhost/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo("http://localhost/metadata?" + query, xpath, doc);
        
    }
    
    @Test
    public void testStyleWorkspaceQualified() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);

        // check the style name got prefixed too
        assertXpathEvaluatesTo("cite:Lakes", "//Layer[Name='cite:Lakes']/Style[1]/Name", doc);
        assertXpathEvaluatesTo("cite:tiger_roads", "//Layer[Name='cite:Lakes']/Style[2]/Name", doc);
    }

    // GEOS-7217: Make sure Styles are valid to DTD
    @Test
    public void testStyleElementsValidity() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);

        assertXpathExists("//Layer[Name='cite:Lakes']/Style[1]/Name", doc);
        assertXpathExists("//Layer[Name='cite:Lakes']/Style[1]/Title", doc);
        assertXpathExists("//Layer[Name='cite:Lakes']/Style[1]/LegendURL", doc);
        
        assertXpathExists("//Layer[Name='cite:Lakes']/Style[2]/Name", doc);
        assertXpathExists("//Layer[Name='cite:Lakes']/Style[2]/Title", doc);
        assertXpathExists("//Layer[Name='cite:Lakes']/Style[2]/LegendURL", doc);
    }
    
    @Test
    public void testDuplicateLayerGroup() throws Exception {
        // see https://osgeo-org.atlassian.net/browse/GEOS-6154
        Catalog catalog = getCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakes.setAdvertised(false);
        catalog.save(lakes);
        
        try {
            Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
            // print(doc);
            
            // nested
            assertXpathEvaluatesTo("1", "count(//Layer[Title='containerGroup']/Layer[Name='nature'])", doc);
            // no other instances
            assertXpathEvaluatesTo("1", "count(//Layer[Name='nature'])", doc);
        } finally {
            lakes.setAdvertised(true);
            catalog.save(lakes);
        }
    }
}
