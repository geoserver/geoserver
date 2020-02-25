/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LayerGroupWorkspaceTest extends WMSTestSupport {

    LayerGroupInfo global, global2, sf, cite, nested, world;

    @Before
    public void prepare() throws Exception {
        // get a shorter list of SRS
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getSRS().add("4326");
        gs.save(wms);

        Catalog cat = getCatalog();

        global =
                createLayerGroup(
                        cat,
                        "base",
                        "base default",
                        layer(cat, MockData.LAKES),
                        layer(cat, MockData.FORESTS));
        cat.add(global);

        global2 =
                createLayerGroup(
                        cat,
                        "base2",
                        "base default",
                        layer(cat, MockData.LAKES),
                        layer(cat, MockData.FORESTS));
        cat.add(global2);

        sf =
                createLayerGroup(
                        cat,
                        "base",
                        "sf base",
                        layer(cat, MockData.PRIMITIVEGEOFEATURE),
                        layer(cat, MockData.AGGREGATEGEOFEATURE));
        sf.setWorkspace(cat.getWorkspaceByName("sf"));
        cat.add(sf);

        cite =
                createLayerGroup(
                        cat,
                        "base",
                        "cite base",
                        layer(cat, MockData.BRIDGES),
                        layer(cat, MockData.BUILDINGS));
        cite.setWorkspace(cat.getWorkspaceByName("cite"));
        cat.add(cite);

        world =
                createLayerGroup(
                        cat,
                        "world",
                        "world base",
                        layer(cat, MockData.WORLD),
                        layer(cat, MockData.WORLD));
        cat.add(world);
    }

    @After
    public void rollback() throws Exception {
        Catalog cat = getCatalog();
        if (nested != null) {
            cat.remove(nested);
        }
        cat.remove(cite);
        cat.remove(sf);
        cat.remove(global);
        cat.remove(global2);
        cat.remove(world);
    }

    LayerInfo layer(Catalog cat, QName name) {
        return cat.getLayerByName(getLayerId(name));
    }

    LayerGroupInfo createLayerGroup(Catalog cat, String name, String title, PublishedInfo... layers)
            throws Exception {
        LayerGroupInfo group = cat.getFactory().createLayerGroup();
        group.setName(name);
        group.setTitle("title for layer group " + title);
        group.setAbstract("abstract for layer group " + title);
        for (PublishedInfo layer : layers) {
            group.getLayers().add(layer);
            group.getStyles().add(null);
        }

        new CatalogBuilder(cat).calculateLayerGroupBounds(group);
        return group;
    }

    @Test
    public void testAddLayerGroup() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo lg = createLayerGroup(cat, "base", "base", layer(cat, MockData.LOCKS));
        try {
            cat.add(lg);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testGlobalCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");

        assertXpathExists("//Layer/Name[text() = 'base']", dom);
        assertBounds(global, "base", dom);

        assertXpathExists("//Layer/Name[text() = 'sf:base']", dom);
        assertBounds(sf, "sf:base", dom);

        assertXpathExists("//Layer/Name[text() = 'cite:base']", dom);
        assertBounds(cite, "cite:base", dom);

        String layer = "base";
        assertXpathNotExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", dom);

        addSRSAndSetFlag();

        dom = getAsDOM("wms?request=getcapabilities&version=1.1.1", true);

        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:4326']", dom);
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", dom);
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3857']", dom);
    }

    @Test
    public void testLayerGroupTitleInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        assertXpathExists("//Layer/Title[text() = 'title for layer group base default']", dom);
        assertXpathExists("//Layer/Title[text() = 'title for layer group sf base']", dom);
        assertXpathExists("//Layer/Title[text() = 'title for layer group cite base']", dom);
    }

    @Test
    public void testLayerGroupAbstractInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        assertXpathExists(
                "//Layer/Abstract[text() = 'abstract for layer group base default']", dom);
        assertXpathExists("//Layer/Abstract[text() = 'abstract for layer group sf base']", dom);
        assertXpathExists("//Layer/Abstract[text() = 'abstract for layer group cite base']", dom);
    }

    @Test
    public void testSingleLayerGroupInCapabilities() throws Exception {
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");

        // check layer group is present
        assertXpathExists("/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);

        // check it doesn't have children Layers
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'base']/Layer", dom);

        // check its layers are present at the same level
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Lakes']", dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Forests']", dom);
    }

    @Test
    public void testNamedLayerGroupInCapabilities() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo layerGroup = cat.getLayerGroupByName("base");
        layerGroup.setMode(LayerGroupInfo.Mode.NAMED);
        cat.save(layerGroup);
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");

        // check layer group is present
        assertXpathExists("/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);

        // check its layers are no more present at the same level
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Lakes']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Forests']", dom);

        // check its layers are present as its children
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name[text() = 'base']]/Layer/Name[text() = 'cite:Lakes']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name[text() = 'base']]/Layer/Name[text() = 'cite:Forests']",
                dom);
    }

    @Test
    public void testContainerLayerGroupInCapabilities() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo layerGroup = cat.getLayerGroupByName("base");
        layerGroup.setMode(LayerGroupInfo.Mode.CONTAINER);
        cat.save(layerGroup);
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");

        // check layer group doesn't have a name but eventually a title
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Title[text() = 'title for layer group base default']",
                dom);

        // check its layers are no more present at the same level
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Lakes']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Forests']", dom);

        // check its layers are present as its children
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Title[text() = 'title for layer group base default']]/Layer/Name[text() = 'cite:Lakes']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Title[text() = 'title for layer group base default']]/Layer/Name[text() = 'cite:Forests']",
                dom);
    }

    @Test
    public void testEoLayerGroupInCapabilities() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo layerGroup = cat.getLayerGroupByName("base");
        layerGroup.setMode(LayerGroupInfo.Mode.EO);
        layerGroup.setRootLayer(layer(cat, MockData.BUILDINGS));
        layerGroup.setRootLayerStyle(cat.getStyleByName("Buildings"));
        cat.save(layerGroup);
        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");

        // check layer group exists
        assertXpathExists("/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);

        // check its layers are no more present at the same level
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Lakes']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Forests']", dom);

        // check its layers are present as its children
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name[text() = 'base']]/Layer/Name[text() = 'cite:Lakes']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name[text() = 'base']]/Layer/Name[text() = 'cite:Forests']",
                dom);
    }

    @Test
    public void testWorkspaceCapabilities() throws Exception {
        Document dom = getAsDOM("sf/wms?request=getcapabilities&version=1.1.1");

        assertXpathExists("//Layer/Name[text() = 'base']", dom);
        assertXpathNotExists("//Layer/Name[text() = 'sf:base']", dom);
        assertBounds(sf, "base", dom);

        String layer = "base";
        assertXpathNotExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", dom);

        addSRSAndSetFlag();
        dom = getAsDOM("wms?request=getcapabilities&version=1.1.1", true);

        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:4326']", dom);
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", dom);
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3857']", dom);

        layer = "world";
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3857']", dom);
    }

    @Test
    public void testGlobalGetMap() throws Exception {
        Document dom = getAsDOM("wms/reflect?layers=base&format=rss");
        assertXpathExists("rss/channel/title[text() = 'cite:Lakes,cite:Forests']", dom);

        dom = getAsDOM("wms/reflect?layers=sf:base&format=rss");
        assertXpathExists(
                "rss/channel/title[text() = 'sf:PrimitiveGeoFeature,sf:AggregateGeoFeature']", dom);

        dom = getAsDOM("wms/reflect?layers=cite:base&format=rss");
        assertXpathExists("rss/channel/title[text() = 'cite:Bridges,cite:Buildings']", dom);
    }

    @Test
    public void testWorkspaceGetMap() throws Exception {
        Document dom = getAsDOM("sf/wms?request=reflect&layers=base&format=rss");
        assertXpathExists(
                "rss/channel/title[text() = 'sf:PrimitiveGeoFeature,sf:AggregateGeoFeature']", dom);

        dom = getAsDOM("cite/wms?request=reflect&layers=base&format=rss");
        assertXpathExists("rss/channel/title[text() = 'cite:Bridges,cite:Buildings']", dom);

        dom = getAsDOM("sf/wms?request=reflect&layers=cite:base&format=rss");
        assertXpathExists(
                "rss/channel/title[text() = 'sf:PrimitiveGeoFeature,sf:AggregateGeoFeature']", dom);
    }

    @Test
    public void testSharedLayersInCapabilities() throws Exception {
        Catalog cat = getCatalog();
        LayerGroupInfo global = cat.getLayerGroupByName("base");
        global.setMode(Mode.NAMED);
        cat.save(global);
        LayerGroupInfo global2 = cat.getLayerGroupByName("base2");
        global2.setMode(Mode.NAMED);
        cat.save(global2);

        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        // print(dom);

        // check top level layer group exists
        assertXpathExists("/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base2']", dom);

        // check their layers are no more present at the same level
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Lakes']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Forests']", dom);

        // check its layers are present as their children (in both groups)
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'base']/Layer[Name = 'cite:Lakes']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'base']/Layer[Name = 'cite:Forests']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'base2']/Layer[Name = 'cite:Lakes']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'base2']/Layer[Name = 'cite:Forests']",
                dom);
    }

    @Test
    public void testNestedSingleInCapabilities() throws Exception {
        Catalog cat = getCatalog();
        nested = createLayerGroup(cat, "nested", "nested", layer(cat, MockData.BRIDGES), global);
        nested.setMode(Mode.NAMED);
        cat.add(nested);

        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        // print(dom);

        // check top level layer group exists
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'nested']", dom);

        // check its layers, and nested layers, and nested groups, are no more present at the same
        // level
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Bridges']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);

        // check its layers are present as its children, as well as the nested group
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'cite:Bridges']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'base']",
                dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'base']/Layer[Name = 'cite:Lakes']",
                dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'base']/Layer[Name = 'cite:Forests']",
                dom);
    }

    @Test
    public void testNestedNamedInCapabilities() throws Exception {
        Catalog cat = getCatalog();
        nested = createLayerGroup(cat, "nested", "nested", layer(cat, MockData.BRIDGES), global);
        nested.setMode(Mode.NAMED);
        cat.add(nested);
        LayerGroupInfo global = cat.getLayerGroupByName("base");
        global.setMode(Mode.NAMED);
        cat.save(global);

        Document dom = getAsDOM("wms?request=getcapabilities&version=1.1.1");
        // print(dom);

        // check top level layer group exists
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'nested']", dom);

        // check its layers, and nested layers, and nested groups, are no more present at the same
        // level
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Bridges']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Lakes']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'cite:Forests']", dom);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer/Name[text() = 'base']", dom);

        // check its layers are present as its children, as well as the nested group
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'cite:Bridges']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'base']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'base']/Layer[Name = 'cite:Lakes']",
                dom);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/Layer[Name = 'nested']/Layer[Name = 'base']/Layer[Name = 'cite:Forests']",
                dom);
    }

    @Test
    public void testNestedNamedGetMap() throws Exception {
        Catalog cat = getCatalog();
        nested = createLayerGroup(cat, "nested", "nested", layer(cat, MockData.BRIDGES), global);
        nested.setMode(Mode.NAMED);
        cat.add(nested);
        LayerGroupInfo global = cat.getLayerGroupByName("base");
        global.setMode(Mode.NAMED);
        cat.save(global);

        Document dom = getAsDOM("wms?request=reflect&layers=nested&format=rss");
        // print(dom);

        assertXpathExists(
                "rss/channel/title[text() = 'cite:Bridges,cite:Lakes,cite:Forests']", dom);
    }

    @Test
    public void testNestedSharedGetMap() throws Exception {
        Catalog cat = getCatalog();
        nested = createLayerGroup(cat, "nested", "nested", global, global2);
        nested.setMode(Mode.NAMED);
        cat.add(nested);

        Document dom = getAsDOM("wms?request=reflect&layers=nested&format=rss");
        assertXpathExists(
                "rss/channel/title[text() = 'cite:Lakes,cite:Forests,cite:Lakes,cite:Forests']",
                dom);
    }

    @Test
    public void testNestedSingleGetMap() throws Exception {
        Catalog cat = getCatalog();
        nested = createLayerGroup(cat, "nested", "nested", layer(cat, MockData.BRIDGES), global);
        nested.setMode(Mode.NAMED);
        cat.add(nested);

        Document dom = getAsDOM("wms?request=reflect&layers=nested&format=rss");
        // print(dom);

        assertXpathExists(
                "rss/channel/title[text() = 'cite:Bridges,cite:Lakes,cite:Forests']", dom);
    }

    void assertBounds(LayerGroupInfo lg, String name, Document dom) throws Exception {
        assertXpathEvaluatesTo(
                String.valueOf(Math.round(lg.getBounds().getMinX())),
                "round(//Layer[Name/text() = '" + name + "']/BoundingBox/@minx)",
                dom);
        assertXpathEvaluatesTo(
                String.valueOf(Math.round(lg.getBounds().getMaxX())),
                "round(//Layer[Name/text() = '" + name + "']/BoundingBox/@maxx)",
                dom);
        assertXpathEvaluatesTo(
                String.valueOf(Math.round(lg.getBounds().getMinY())),
                "round(//Layer[Name/text() = '" + name + "']/BoundingBox/@miny)",
                dom);
        assertXpathEvaluatesTo(
                String.valueOf(Math.round(lg.getBounds().getMaxY())),
                "round(//Layer[Name/text() = '" + name + "']/BoundingBox/@maxy)",
                dom);
    }

    void addSRSAndSetFlag() {
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getSRS().add("4326");
        wms.getSRS().add("3005");
        wms.getSRS().add("3857");
        wms.setBBOXForEachCRS(true);
        getGeoServer().save(wms);
    }

    @After
    public void removeSRS() {
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getSRS().remove("4326");
        wms.getSRS().remove("3005");
        wms.getSRS().remove("3857");
        wms.setBBOXForEachCRS(false);
        getGeoServer().save(wms);
    }

    /** Tests Layer group order */
    @Test
    public void testGetCapabilitiesGroupOrder() throws Exception {
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        List<String> originalList = layerGroupNameList(doc);
        assertFalse(originalList.isEmpty());
        List<String> normal =
                originalList.stream().map(x -> removeLayerPrefix(x)).collect(Collectors.toList());
        List<String> ordered = normal.stream().sorted().collect(Collectors.toList());
        assertTrue(ordered.equals(normal));
    }

    /** Test Layer group order on a workspace virtual service */
    @Test
    public void testWorkspaceGetCapabilitiesGroupOrder() throws Exception {
        Document doc = getAsDOM("sf/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists("//Layer/Name[text() = 'base']", doc);
        assertXpathNotExists("//Layer/Name[text() = 'sf:base']", doc);
        assertBounds(sf, "base", doc);
        String layer = "base";
        assertXpathNotExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", doc);
        List<String> originalList = layerGroupNameList(doc);
        assertFalse(originalList.isEmpty());
        List<String> normal =
                originalList.stream().map(x -> removeLayerPrefix(x)).collect(Collectors.toList());
        List<String> ordered = normal.stream().sorted().collect(Collectors.toList());
        assertTrue(ordered.equals(normal));
    }

    /** removes prefix from layer name */
    private String removeLayerPrefix(String prefixedName) {
        if (prefixedName.indexOf(":") > -1) {
            return prefixedName.split(":")[1];
        }
        return prefixedName;
    }

    /** returns list of prefixed layer groups names from document */
    private List<String> layerGroupNameList(Document doc) throws Exception {
        List<Node> nlist =
                xpathList("//WMT_MS_Capabilities/Capability/Layer/Layer[not(@opaque)]/Name", doc);
        List<String> result = new ArrayList<>();
        nlist.forEach(
                x -> {
                    result.add(x.getTextContent().trim());
                });
        return result;
    }

    private List<Node> xpathList(String xpathString, Document doc) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathString);
        NodeList nlist = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < nlist.getLength(); i++) {
            nodeList.add(nlist.item(i));
        }
        return nodeList;
    }
}
