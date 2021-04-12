/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geowebcache.grid.GridSubsetFactory.createGridSubSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DataBindingException;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geoserver.mapml.tcrs.Bounds;
import org.geoserver.mapml.xml.AxisType;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.Input;
import org.geoserver.mapml.xml.InputType;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.ProjType;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.mapml.xml.UnitType;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.grid.GridSubset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLControllerTest extends WMSTestSupport {
    private GWC gwc;
    private MapMLController mc;
    private Jaxb2Marshaller mapmlMarshaller;
    private XpathEngine xpath;
    private GridSubset osmtile;
    private GWCConfig defaults;

    @Before
    public void setup() {
        mc = applicationContext.getBean(MapMLController.class);
        mapmlMarshaller = (Jaxb2Marshaller) applicationContext.getBean("mapmlMarshaller");
        HashMap<String, String> m = new HashMap<>();
        m.put("html", "http://www.w3.org/1999/xhtml/");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
        xpath = XMLUnit.newXpathEngine();

        Catalog catalog = getCatalog();
        // restore data set up default
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();

        layerMeta.getMetadata().put("mapml.useTiles", false);
        catalog.save(layerMeta);
        gwc = applicationContext.getBean(GWC.class);
        defaults = GWCConfig.getOldDefaults();
        // it seems just the fact of retrieving the bean causes the
        // GridSets to be added to the gwc GridSetBroker, but if you don't do
        // this, they are not added automatically
        MapMLGridsets mgs = applicationContext.getBean(MapMLGridsets.class);
        this.osmtile = createGridSubSet(mgs.getGridSet("OSMTILE").get());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        ResourceInfo ri =
                catalog.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart()).getResource();
        cb.setupBounds(ri);
        catalog.save(ri);
        cb.setupBounds(catalog.getLayerByName(points).getResource());
        cb.setupBounds(catalog.getLayerByName(lines).getResource());
        cb.setupBounds(catalog.getLayerByName(polygons).getResource());
        assertNotNull(cb.getNativeBounds(catalog.getLayerByName(polygons).getResource()));
        assertNotNull(catalog.getLayerByName(polygons).getResource().getLatLonBoundingBox());

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);

        LayerGroupInfo lgi = catalog.getLayerGroupByName(NATURE_GROUP);
        CoordinateReferenceSystem webMerc = MapMLController.previewTcrsMap.get("OSMTILE").getCRS();
        Bounds webMercBounds = MapMLController.previewTcrsMap.get("OSMTILE").getBounds();
        double x1 = webMercBounds.getMin().x;
        double x2 = webMercBounds.getMax().x;
        double y1 = webMercBounds.getMin().y;
        double y2 = webMercBounds.getMax().y;
        ReferencedEnvelope webMercEnv = new ReferencedEnvelope(x1, x2, y1, y2, webMerc);
        lgi.setBounds(webMercEnv);
        catalog.save(lgi);
    }

    @Test
    public void testMapML() throws Exception {

        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        testLayersAndGroupsMapML(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        assertSame(DefaultGeographicCRS.WGS84, lgi.getBounds().getCoordinateReferenceSystem());
        testLayersAndGroupsMapML(lgi);

        lgi = cat.getLayerGroupByName(NATURE_GROUP);
        assertSame(
                MapMLController.previewTcrsMap.get("OSMTILE").getCRS(),
                lgi.getBounds().getCoordinateReferenceSystem());
        testLayersAndGroupsMapML(lgi);
    }

    @Test
    public void testHTML() throws Exception {

        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        testLayersAndGroupsHTML(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName(NATURE_GROUP);
        testLayersAndGroupsHTML(lgi);
    }

    @Test
    public void testNonExistentLayer() throws Exception {
        MockHttpServletRequest request = createRequest("mapml/" + "foo" + "/osmtile/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mc.Html(
                request,
                response,
                "foo",
                "osmtile",
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        assertEquals(
                "Response is 404 Not Found",
                HttpServletResponse.SC_NOT_FOUND,
                response.getStatus());
    }

    @Test
    public void testNonExistentProjection() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        MockHttpServletRequest request = createRequest("mapml/" + li.getName() + "/foo/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        mc.Html(
                request,
                response,
                li.getName(),
                "foo",
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        assertEquals(
                "Response is 400 Bad Request",
                HttpServletResponse.SC_BAD_REQUEST,
                response.getStatus());

        request = createRequest("mapml/" + li.getName() + "/foo/");
        response = new MockHttpServletResponse();

        mc.mapML(
                request,
                response,
                li.getName(),
                "foo",
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        assertEquals(
                "Response is 400 Bad Request",
                HttpServletResponse.SC_BAD_REQUEST,
                response.getStatus());
    }

    private void testLayersAndGroupsHTML(Object l) throws Exception {
        MockHttpServletRequest request =
                createRequest("mapml/" + ((PublishedInfo) l).getName() + "/osmtile/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String htmlResponse =
                mc.Html(
                        request,
                        response,
                        ((PublishedInfo) l).getName(),
                        "osmtile",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        assertNotNull("Html method must return a document", htmlResponse);
        Document doc = Jsoup.parse(htmlResponse);
        Element webmapimport = doc.head().select("script").first();
        assertTrue(
                "HTML document script must use mapml-viewer.js module",
                webmapimport.attr("src").matches(".*mapml-viewer\\.js"));
        Element map = doc.body().select("mapml-viewer").first();
        Element layer = map.getElementsByTag("layer-").first();
        assertTrue(
                "Layer must have label equal to string ",
                layer.attr("label").equalsIgnoreCase(((PublishedInfo) l).getName()));
        assertTrue(!"0".equalsIgnoreCase(doc.select("mapml-viewer").attr("zoom")));
    }

    private void testLayersAndGroupsMapML(Object l) throws Exception {

        MockHttpServletRequest request =
                createRequest("mapml/" + ((PublishedInfo) l).getName() + "/osmtile/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Mapml mapml =
                mc.mapML(
                        request,
                        response,
                        ((PublishedInfo) l).getName(),
                        "osmtile",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        assertNotNull("mapML method must return a MapML document", mapml);
        StringWriter sw = new StringWriter();
        try {
            mapmlMarshaller.marshal(mapml, new StreamResult(sw));
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB object");
        }

        String result = sw.toString();
        // this tests that the result has had namespaces mapped to minimum possible cruft
        assertTrue(result.matches("<mapml xmlns=\"http://www.w3.org/1999/xhtml/\">.*"));

        BodyContent b = mapml.getBody();
        assertNotNull("mapML method must return MapML body in response", b);
        Extent e = b.getExtent();
        String action = e.getAction();
        assertNull(action);
        ProjType projType = e.getUnits();
        assertSame(ProjType.OSMTILE, projType);

        List<Object> lo = e.getInputOrDatalistOrLink();
        for (Object o : lo) {
            if (o instanceof Link) {
                Link link = (Link) o;
                assertNull("extent/link@href unexpected.", link.getHref());
                assertNotNull("extent/link@href must not be null/empty", link.getTref());
                assertFalse("extent/link@href must not be null/empty", link.getTref().isEmpty());
                assertTrue(
                        "link rel for this layer group must bel image or query",
                        (link.getRel() == RelType.IMAGE || link.getRel() == RelType.QUERY));
                // lots of stuff that is better covered by validation.
            } else if (o instanceof Input) {
                Input input = (Input) o;
                assertTrue(
                        "inputs must be of type zoom, location, width or height",
                        input.getType() == InputType.ZOOM
                                || input.getType() == InputType.LOCATION
                                || input.getType() == InputType.WIDTH
                                || input.getType() == InputType.HEIGHT);
                if (input.getType() == InputType.LOCATION
                        && input.getUnits() == UnitType.PCRS
                        && input.getAxis() == AxisType.EASTING) {
                    assertTrue(
                            "input[type=location/@min must equal -2.0037508342789244E7",
                            "-2.0037508342789244E7".equalsIgnoreCase(input.getMin()));
                    assertTrue(
                            "input[type=location/@max must equal 2.0037508342789244E7",
                            "2.0037508342789244E7".equalsIgnoreCase(input.getMax()));
                } else if (input.getType() == InputType.LOCATION
                        && input.getUnits() == UnitType.PCRS
                        && input.getAxis() == AxisType.NORTHING) {
                    assertTrue(
                            "input[type=location/@min must equal -2.0037508342780735E7",
                            "-2.0037508342780735E7".equalsIgnoreCase(input.getMin()));
                    assertTrue(
                            "input[type=location/@max must equal 2.003750834278071E7",
                            "2.003750834278071E7".equalsIgnoreCase(input.getMax()));
                }
            } else {
                fail("Unrecognized test object type:" + o.getClass().getTypeName());
            }
        }
    }

    @Test
    public void testShardingMapMLLayer() throws Exception {
        String path =
                "mapml/"
                        + MockData.ROAD_SEGMENTS.getPrefix()
                        + ":"
                        + MockData.ROAD_SEGMENTS.getLocalPart()
                        + "/osmtile/";

        // set up mapml layer to useTiles
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();
        MetadataMap mm = layerMeta.getMetadata();
        mm.put("mapml.enableSharding", true);
        mm.put("mapml.shardList", "server1,server2,server3");
        mm.put("mapml.shardServerPattern", "{s}.example.com");
        catalog.save(layerMeta);

        org.w3c.dom.Document doc = getMapML(path);

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='image'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:link[@rel='image']/@tref", doc));
        String host = url.getHost();
        assertTrue(host.equalsIgnoreCase("{s}.example.com"));
        assertXpathEvaluatesTo("1", "count(//html:datalist[@id='servers'])", doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@list='servers'][@type='hidden'][@shard='true'][@name='s'])",
                doc);
        assertXpathEvaluatesTo("3", "count(//html:datalist/html:option)", doc);
        assertXpathEvaluatesTo("1", "count(//html:datalist/html:option[@value='server1'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:datalist/html:option[@value='server2'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:datalist/html:option[@value='server3'])", doc);

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:link[@rel='query']/@tref", doc));
        host = url.getHost();
        assertTrue(host.equalsIgnoreCase("{s}.example.com"));
    }

    @Test
    public void testDefaultConfiguredMapMLLayer() throws Exception {
        String path =
                "mapml/"
                        + MockData.ROAD_SEGMENTS.getPrefix()
                        + ":"
                        + MockData.ROAD_SEGMENTS.getLocalPart()
                        + "/osmtile/";

        org.w3c.dom.Document doc = getMapML(path);

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='image'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:link[@rel='image']/@tref", doc));
        HashMap<String, String> vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetMap"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.3.0"));
        assertTrue(vars.get("layers").equalsIgnoreCase(MockData.ROAD_SEGMENTS.getLocalPart()));
        assertTrue(vars.get("crs").equalsIgnoreCase("urn:x-ogc:def:crs:EPSG:3857"));
        assertTrue(vars.get("bbox").equalsIgnoreCase("{xmin},{ymin},{xmax},{ymax}"));
        assertTrue(vars.get("format").equalsIgnoreCase("image/png"));
        assertTrue(vars.get("width").equalsIgnoreCase("{w}"));
        assertTrue(vars.get("height").equalsIgnoreCase("{h}"));
        assertTrue(vars.get("transparent").equalsIgnoreCase("true"));
        assertTrue(vars.get("styles").equalsIgnoreCase(""));

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:link[@rel='query']/@tref", doc));
        vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetFeatureInfo"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.3.0"));
        assertTrue(vars.get("layers").equalsIgnoreCase(MockData.ROAD_SEGMENTS.getLocalPart()));
        assertTrue(vars.get("crs").equalsIgnoreCase("urn:x-ogc:def:crs:EPSG:3857"));
        assertTrue(vars.get("bbox").equalsIgnoreCase("{xmin},{ymin},{xmax},{ymax}"));
        assertTrue(vars.get("width").equalsIgnoreCase("{w}"));
        assertTrue(vars.get("height").equalsIgnoreCase("{h}"));
        assertTrue(vars.get("transparent").equalsIgnoreCase("true"));
        assertTrue(vars.get("styles").equalsIgnoreCase(""));
        assertTrue(vars.get("x").equalsIgnoreCase("{i}"));
        assertTrue(vars.get("y").equalsIgnoreCase("{j}"));
        assertTrue(vars.get("info_format").equalsIgnoreCase("text/mapml"));
        assertTrue(vars.get("feature_count").equalsIgnoreCase("50"));

        // make sure there's an input for each template variable
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@name='xmin'][@type='location'][@units='pcrs'][@axis='easting'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@name='ymin'][@type='location'][@units='pcrs'][@axis='northing'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@name='xmax'][@type='location'][@units='pcrs'][@axis='easting'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@name='ymax'][@type='location'][@units='pcrs'][@axis='northing'][@min][@max])",
                doc);
        assertXpathEvaluatesTo("1", "count(//html:input[@name='w'][@type='width'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:input[@name='h'][@type='height'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:input[@name='i'][@type='location'][@units='map'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:input[@name='j'][@type='location'][@units='map'])", doc);

        // this is a weird one, probably should not be necessary, but if we
        // remove the requirement to have it, we will have to specify a
        // requirement to specify a zoom range via a <meta> element
        assertXpathEvaluatesTo(
                "1", "count(//html:input[@name='z'][@type='zoom'][@min][@max])", doc);
    }

    @Test
    public void testWMSTilesConfiguredMapMLLayer() throws Exception {
        String path =
                "mapml/"
                        + MockData.ROAD_SEGMENTS.getPrefix()
                        + ":"
                        + MockData.ROAD_SEGMENTS.getLocalPart()
                        + "/osmtile/";

        org.w3c.dom.Document doc = getMapML(path);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='image'][@tref])", doc);

        // set up mapml layer to useTiles
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();

        layerMeta.getMetadata().put("mapml.useTiles", true);
        catalog.save(layerMeta);

        doc = getMapML(path);

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='tile'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:link[@rel='tile']/@tref", doc));
        HashMap<String, String> vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetMap"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.3.0"));
        assertTrue(vars.get("layers").equalsIgnoreCase(MockData.ROAD_SEGMENTS.getLocalPart()));
        assertTrue(vars.get("crs").equalsIgnoreCase("urn:x-ogc:def:crs:EPSG:3857"));
        assertTrue(vars.get("bbox").equalsIgnoreCase("{txmin},{tymin},{txmax},{tymax}"));
        assertTrue(vars.get("format").equalsIgnoreCase("image/png"));
        assertTrue(vars.get("width").equalsIgnoreCase("256"));
        assertTrue(vars.get("height").equalsIgnoreCase("256"));
        assertTrue(vars.get("transparent").equalsIgnoreCase("true"));
        assertTrue(vars.get("styles").equalsIgnoreCase(""));

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:link[@rel='query']/@tref", doc));
        vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetFeatureInfo"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.3.0"));
        assertTrue(vars.get("layers").equalsIgnoreCase(MockData.ROAD_SEGMENTS.getLocalPart()));
        assertTrue(vars.get("crs").equalsIgnoreCase("urn:x-ogc:def:crs:EPSG:3857"));
        assertTrue(vars.get("bbox").equalsIgnoreCase("{txmin},{tymin},{txmax},{tymax}"));
        assertTrue(vars.get("width").equalsIgnoreCase("256"));
        assertTrue(vars.get("height").equalsIgnoreCase("256"));
        assertTrue(vars.get("transparent").equalsIgnoreCase("true"));
        assertTrue(vars.get("styles").equalsIgnoreCase(""));
        assertTrue(vars.get("x").equalsIgnoreCase("{i}"));
        assertTrue(vars.get("y").equalsIgnoreCase("{j}"));
        assertTrue(vars.get("info_format").equalsIgnoreCase("text/mapml"));
        assertTrue(vars.get("feature_count").equalsIgnoreCase("50"));

        // make sure there's an input for each template variable
        String xpath =
                "count(//html:input[@name='txmin'][@type='location'][@units='tilematrix'][@axis='easting'][@min][@max])";
        assertXpathEvaluatesTo("1", xpath, doc);
        xpath =
                "count(//html:input[@name='tymin'][@type='location'][@units='tilematrix'][@axis='northing'][@min][@max])";
        assertXpathEvaluatesTo("1", xpath, doc);
        xpath =
                "count(//html:input[@name='txmax'][@type='location'][@units='tilematrix'][@axis='easting'][@min][@max])";
        assertXpathEvaluatesTo("1", xpath, doc);
        xpath =
                "count(//html:input[@name='tymax'][@type='location'][@units='tilematrix'][@axis='northing'][@min][@max])";
        assertXpathEvaluatesTo("1", xpath, doc);
        xpath = "count(//html:input[@name='w'][@type='width'])";
        assertXpathEvaluatesTo("0", xpath, doc);
        xpath = "count(//html:input[@name='h'][@type='height'])";
        assertXpathEvaluatesTo("0", xpath, doc);
        xpath = "count(//html:input[@name='i'][@type='location'][@units='tile'])";
        assertXpathEvaluatesTo("1", xpath, doc);
        xpath = "count(//html:input[@name='j'][@type='location'][@units='tile'])";
        assertXpathEvaluatesTo("1", xpath, doc);

        // this is a weird one, probably should not be necessary, but if we
        // remove the requirement to have it, we will have to specify a
        // requirement to specify a zoom range via a <meta> element
        xpath = "count(//html:input[@name='z'][@type='zoom'][@min][@max])";
        assertXpathEvaluatesTo("1", xpath, doc);
    }

    @Test
    public void testGWCTilesConfiguredMapMLLayer() throws Exception {
        String path =
                "mapml/"
                        + MockData.ROAD_SEGMENTS.getPrefix()
                        + ":"
                        + MockData.ROAD_SEGMENTS.getLocalPart()
                        + "/osmtile/";

        org.w3c.dom.Document doc = getMapML(path);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='image'][@tref])", doc);

        // set up mapml layer to useTiles
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();

        layerMeta.getMetadata().put("mapml.useTiles", true);
        catalog.save(layerMeta);
        LayerInfo layerInfo = catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart());
        GeoServerTileLayer layerInfoTileLayer =
                new GeoServerTileLayer(layerInfo, defaults, gwc.getGridSetBroker());
        layerInfoTileLayer.addGridSubset(osmtile);
        gwc.save(layerInfoTileLayer);
        String wmtsLayerName =
                MockData.ROAD_SEGMENTS.getPrefix() + ":" + MockData.ROAD_SEGMENTS.getLocalPart();

        doc = getMapML(path);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='tile'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:link[@rel='tile']/@tref", doc));
        HashMap<String, String> vars = parseQuery(url);
        assertTrue(vars.get("request").equalsIgnoreCase("GetTile"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMTS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.0.0"));
        assertTrue(vars.get("layer").equalsIgnoreCase(wmtsLayerName));
        assertTrue(vars.get("format").equalsIgnoreCase("image/png"));
        assertTrue(vars.get("tilematrixset").equalsIgnoreCase("OSMTILE"));
        assertTrue(vars.get("tilematrix").equalsIgnoreCase("{z}"));
        assertTrue(vars.get("TileRow").equalsIgnoreCase("{y}"));
        assertTrue(vars.get("TileCol").equalsIgnoreCase("{x}"));
        assertTrue(vars.get("style").equalsIgnoreCase(""));

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:link[@rel='query']/@tref", doc));
        vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetFeatureInfo"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMTS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.0.0"));
        assertTrue(vars.get("layer").equalsIgnoreCase(wmtsLayerName));
        assertTrue(vars.get("tilematrixset").equalsIgnoreCase("OSMTILE"));
        assertTrue(vars.get("tilematrix").equalsIgnoreCase("{z}"));
        assertTrue(vars.get("TileRow").equalsIgnoreCase("{y}"));
        assertTrue(vars.get("TileCol").equalsIgnoreCase("{x}"));
        assertTrue(vars.get("style").equalsIgnoreCase(""));
        assertTrue(vars.get("i").equalsIgnoreCase("{i}"));
        assertTrue(vars.get("j").equalsIgnoreCase("{j}"));
        assertTrue(vars.get("infoformat").equalsIgnoreCase("text/mapml"));
        assertTrue(vars.get("feature_count").equalsIgnoreCase("50"));

        // make sure there's an input for each template variable
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@name='x'][@type='location'][@units='tilematrix'][@axis='column'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:input[@name='y'][@type='location'][@units='tilematrix'][@axis='row'][@min][@max])",
                doc);
        assertXpathEvaluatesTo("0", "count(//html:input[@name='w'][@type='width'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:input[@name='h'][@type='height'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:input[@name='i'][@type='location'][@units='tile'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:input[@name='j'][@type='location'][@units='tile'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:input[@name='z'][@type='zoom'][@min][@max])", doc);
    }

    @Test
    public void testGetFeatureInfoMapML() throws Exception {

        // set up mapml layer featurecaption
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta = catalog.getLayerByName(getLayerId(MockData.FORESTS)).getResource();
        String featureCaptionTemplate = "${NAME}";
        layerMeta.getMetadata().put("mapml.featureCaption", featureCaptionTemplate);
        catalog.save(layerMeta);

        assertTrue(layerMeta.getMetadata().containsKey("mapml.featureCaption"));
        assertTrue(
                layerMeta
                        .getMetadata()
                        .get("mapml.featureCaption")
                        .toString()
                        .equalsIgnoreCase(featureCaptionTemplate));
        String forests = getLayerId(MockData.FORESTS);
        HashMap<String, String> vars = new HashMap<>();
        vars.put("version", "1.1.1");
        vars.put("bbox", "-0.002,-0.002,0.002,0.002");
        vars.put("styles", "");
        vars.put("format", "jpeg");
        vars.put("info_format", "text/mapml");
        vars.put("request", "GetFeatureInfo");
        vars.put("layers", forests);
        vars.put("query_layers", forests);
        vars.put("width", "20");
        vars.put("height", "20");
        vars.put("x", "10");
        vars.put("y", "10");
        org.w3c.dom.Document doc = getMapML("wms", vars);
        assertXpathEvaluatesTo("1", "count(//html:feature)", doc);
        assertXpathEvaluatesTo("1", "count(//html:featurecaption)", doc);
        assertXpathEvaluatesTo("1", "count(//html:geometry)", doc);
        assertXpathEvaluatesTo("1", "count(//html:properties)", doc);
    }

    @Test
    public void testDefaultConfiguredMapMLLayerGetFeatureInfoAsMapML() throws Exception {
        String path =
                "mapml/"
                        + MockData.BASIC_POLYGONS.getPrefix()
                        + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "/osmtile/";

        // set up mapml layer featurecaption
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart()).getResource();

        String featureCaptionTemplate = "${ID}";
        layerMeta.getMetadata().put("mapml.featureCaption", featureCaptionTemplate);
        catalog.save(layerMeta);
        assertTrue(layerMeta.getMetadata().containsKey("mapml.featureCaption"));
        assertTrue(
                layerMeta
                        .getMetadata()
                        .get("mapml.featureCaption")
                        .toString()
                        .equalsIgnoreCase(featureCaptionTemplate));

        org.w3c.dom.Document doc = getMapML(path);

        assertXpathEvaluatesTo("1", "count(//html:link[@rel='image'][@tref])", doc);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='query'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:link[@rel='query']/@tref", doc));
        HashMap<String, String> vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetFeatureInfo"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.3.0"));
        assertTrue(vars.get("layers").equalsIgnoreCase(MockData.BASIC_POLYGONS.getLocalPart()));
        assertTrue(vars.get("crs").equalsIgnoreCase("urn:x-ogc:def:crs:EPSG:3857"));
        assertTrue(vars.get("bbox").equalsIgnoreCase("{xmin},{ymin},{xmax},{ymax}"));
        assertTrue(vars.get("width").equalsIgnoreCase("{w}"));
        assertTrue(vars.get("height").equalsIgnoreCase("{h}"));
        assertTrue(vars.get("transparent").equalsIgnoreCase("true"));
        assertTrue(vars.get("styles").equalsIgnoreCase(""));
        assertTrue(vars.get("x").equalsIgnoreCase("{i}"));
        assertTrue(vars.get("y").equalsIgnoreCase("{j}"));
        assertTrue(vars.get("info_format").equalsIgnoreCase("text/mapml"));
        assertTrue(vars.get("feature_count").equalsIgnoreCase("50"));
        vars.put(
                "bbox",
                "-967387.0299771908,-118630.26789859355,884223.543202919,920913.3167798058");
        vars.put("width", "757");
        vars.put("height", "425");
        vars.put("x", "379");
        vars.put("y", "213");

        doc = getMapML("wms", vars);
        assertXpathEvaluatesTo("2", "count(//html:feature)", doc);
        // empty attributes (such as ID in this case - all empty) won't be used
        assertXpathEvaluatesTo("0", "count(//html:featurecaption)", doc);
        assertXpathEvaluatesTo("", "//html:feature/html:geometry/@cs", doc);
        assertXpathEvaluatesTo("2", "count(//html:feature//html:polygon[1])", doc);
        assertXpathEvaluatesTo("1", "count(//html:meta[@name='projection'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:meta[@name='cs'])", doc);
    }

    /**
     * Executes a request using the GET method and returns the result as an MapML document.
     *
     * @param path The portion of the request after the context, example:
     * @param query A map representing kvp to be used by the request.
     * @return A result of the request parsed into a dom.
     */
    protected org.w3c.dom.Document getMapML(final String path, HashMap<String, String> query)
            throws Exception {
        MockHttpServletRequest request = createRequest(path, query);
        request.addHeader("Accept", "text/mapml");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }
    /**
     * Executes a request using the GET method and returns the result as an MapML document.
     *
     * @param path The portion of the request after the context, example:
     * @return A result of the request parsed into a dom.
     */
    protected org.w3c.dom.Document getMapML(final String path) throws Exception {
        MockHttpServletRequest request = createRequest(path, false);
        request.addHeader("Accept", "text/mapml");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }

    private HashMap<String, String> parseQuery(URL url) {
        String[] variableValues = url.getQuery().split("&");
        HashMap<String, String> vars = new HashMap<>(variableValues.length);
        // int i = variableValues.length;
        for (String variableValue : variableValues) {
            String[] varValue = variableValue.split("=");
            vars.put(varValue[0], varValue.length == 2 ? varValue[1] : "");
        }
        return vars;
    }
}
