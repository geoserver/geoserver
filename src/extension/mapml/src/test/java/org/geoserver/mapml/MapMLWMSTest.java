/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
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
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DataBindingException;
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
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
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
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.GrowableInternationalString;
import org.geowebcache.grid.GridSubset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class MapMLWMSTest extends WMSTestSupport {

    private XpathEngine xpath;

    @Before
    public void setup() {
        HashMap<String, String> m = new HashMap<>();
        m.put("html", "http://www.w3.org/1999/xhtml");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
        xpath = XMLUnit.newXpathEngine();

        Catalog catalog = getCatalog();
        // restore data set up default
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();

        layerMeta.getMetadata().put("mapml.useTiles", false);
        catalog.save(layerMeta);
    }

    @After
    public void tearDown() {
        // restore default MapMLMultilayerAsMultiextent
        GeoServer geoServer = getGeoServer();
        WMSInfo wms = geoServer.getService(WMSInfo.class);
        wms.getMetadata()
                .put(
                        MapMLDocumentBuilder.MAPML_MULTILAYER_AS_MULTIEXTENT,
                        MapMLDocumentBuilder.MAPML_MULTILAYER_AS_MULTIEXTENT_DEFAULT);
        geoServer.save(wms);
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

        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "A i18n title for polygons");
        title.add(Locale.CANADA_FRENCH, "Le titre français");
        ri.setInternationalTitle(title);

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
        lgi.setInternationalTitle(title);
        CoordinateReferenceSystem webMerc =
                MapMLDocumentBuilder.PREVIEW_TCRS_MAP.get("OSMTILE").getCRS();
        Bounds webMercBounds = MapMLDocumentBuilder.PREVIEW_TCRS_MAP.get("OSMTILE").getBounds();
        double x1 = webMercBounds.getMin().x;
        double x2 = webMercBounds.getMax().x;
        double y1 = webMercBounds.getMin().y;
        double y2 = webMercBounds.getMax().y;
        ReferencedEnvelope webMercEnv = new ReferencedEnvelope(x1, x2, y1, y2, webMerc);
        lgi.setBounds(webMercEnv);
        catalog.save(lgi);
    }

    @Before
    public void resetLayers() throws IOException {
        revertLayer(SystemTestData.ROAD_SEGMENTS);
    }

    @Test
    public void testMapMLSingleLayer() throws Exception {
        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        ResourceInfo layerMeta = li.getResource();
        layerMeta.getMetadata().put("mapml.useTiles", true);
        cat.save(layerMeta);
        Mapml m = testLayersAndGroupsMapML(li, null);
        String title = m.getHead().getTitle();
        assertTrue(title.equalsIgnoreCase(li.getName()));

        li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        layerMeta = li.getResource();
        layerMeta.getMetadata().put("mapml.useTiles", false);
        cat.save(layerMeta);
        m = testLayersAndGroupsMapML(li, null);
        title = m.getHead().getTitle();
        assertTrue(title.equalsIgnoreCase(li.getName()));
        List<Link> extentLinksForSingle =
                getTypeFromInputOrDataListOrLink(
                        m.getBody().getExtents().get(0).getInputOrDatalistOrLink(), Link.class);
        assertEquals(
                "Use tiles is set to false so the link should be a image link",
                extentLinksForSingle.get(0).getRel(),
                RelType.IMAGE);
        assertTrue(
                "Use tiles is set to false so the link should contain getMap",
                extentLinksForSingle.get(0).getTref().contains("GetMap"));

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        assertSame(DefaultGeographicCRS.WGS84, lgi.getBounds().getCoordinateReferenceSystem());
        m = testLayersAndGroupsMapML(lgi, null);
        title = m.getHead().getTitle();
        assertTrue(title.equalsIgnoreCase(lgi.getName()));

        lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put("mapml.useTiles", true);
        cat.save(lgi);
        m = testLayersAndGroupsMapML(lgi, Locale.CANADA_FRENCH);
        title = m.getHead().getTitle();
        assertTrue(title.equalsIgnoreCase(lgi.getName()));

        lgi = cat.getLayerGroupByName(NATURE_GROUP);
        assertSame(
                MapMLDocumentBuilder.PREVIEW_TCRS_MAP.get("OSMTILE").getCRS(),
                lgi.getBounds().getCoordinateReferenceSystem());
        m = testLayersAndGroupsMapML(lgi, null);
        title = m.getHead().getTitle();
        String expectedInternationalTitle = lgi.getInternationalTitle().toString();
        assertTrue("A i18n title for polygons".equalsIgnoreCase(expectedInternationalTitle));
        assertTrue(title.equalsIgnoreCase(expectedInternationalTitle));

        lgi = cat.getLayerGroupByName(NATURE_GROUP);
        lgi.getMetadata().put("mapml.useTiles", true);
        cat.save(lgi);
        assertSame(
                MapMLDocumentBuilder.PREVIEW_TCRS_MAP.get("OSMTILE").getCRS(),
                lgi.getBounds().getCoordinateReferenceSystem());
        m = testLayersAndGroupsMapML(lgi, Locale.CANADA_FRENCH);
        title = m.getHead().getTitle();
        expectedInternationalTitle = lgi.getInternationalTitle().toString(Locale.CANADA_FRENCH);
        assertTrue("Le titre français".equalsIgnoreCase(expectedInternationalTitle));
        assertTrue(title.equalsIgnoreCase(expectedInternationalTitle));
    }

    @Test
    public void testMapMLMultiLayer() throws Exception {
        Catalog cat = getCatalog();
        GeoServer geoServer = getGeoServer();
        WMSInfo wms = geoServer.getService(WMSInfo.class);
        wms.getMetadata().put(MapMLDocumentBuilder.MAPML_MULTILAYER_AS_MULTIEXTENT, Boolean.FALSE);
        geoServer.save(wms);

        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        ResourceInfo layerMeta = li.getResource();
        layerMeta.getMetadata().put("mapml.useTiles", true);
        cat.save(layerMeta);

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put("mapml.useTiles", true);
        cat.save(lgi);

        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        "layerGroup" + "," + MockData.POLYGONS.getLocalPart(),
                        null,
                        null,
                        "EPSG:3857",
                        null);

        MapMLEncoder encoder = new MapMLEncoder();
        StringReader reader = new StringReader(requestResponse.response.getContentAsString());
        Mapml mapmlSingleExtent = null;
        try {
            mapmlSingleExtent = encoder.decode(reader);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }
        List<Link> selfStyleLinksForSingle =
                getLinkByRelType(mapmlSingleExtent.getHead().getLinks(), RelType.SELF_STYLE);
        assertTrue(
                "No matter the setting, self style should refer to all layers",
                selfStyleLinksForSingle.get(0).getHref().contains("layers=layerGroup%2CPolygons&"));
        assertTrue(
                "Self style should have a bbox",
                selfStyleLinksForSingle.get(0).getHref().contains("bbox=0.0%2C0.0%2C1.0%2C1.0"));
        assertTrue(
                "Self style should have a width",
                selfStyleLinksForSingle.get(0).getHref().contains("width=150"));
        assertTrue(
                "Self style should have a height",
                selfStyleLinksForSingle.get(0).getHref().contains("height=150"));
        List<Link> alternateLinksForSingle =
                getLinkByRelType(mapmlSingleExtent.getHead().getLinks(), RelType.ALTERNATE);
        assertTrue(
                "Alternate link width should be set",
                alternateLinksForSingle.get(0).getHref().contains("width=150"));
        assertTrue(
                "Alternate link height should be set",
                alternateLinksForSingle.get(0).getHref().contains("height=150"));
        assertTrue(
                "Alternate link should include the bbox",
                alternateLinksForSingle.get(0).getHref().contains("bbox=0.0%2C0.0%2C1.0%2C1.0"));
        assertEquals(
                "There should be one extent object that combines the attributes of all layers",
                1,
                mapmlSingleExtent.getBody().getExtents().size());

        List<Link> extentLinksForSingle =
                getTypeFromInputOrDataListOrLink(
                        mapmlSingleExtent.getBody().getExtents().get(0).getInputOrDatalistOrLink(),
                        Link.class);
        List<Link> queryLinksForSingle = getLinkByRelType(extentLinksForSingle, RelType.QUERY);
        assertEquals("Query links supported for combined layers", 1, queryLinksForSingle.size());
        assertTrue(
                "query_layers should contain all layer names",
                queryLinksForSingle.get(0).getTref().contains("query_layers=layerGroup,Polygons&"));
        List<Link> tileLinksForSingle = getLinkByRelType(extentLinksForSingle, RelType.TILE);
        assertEquals("Tile links not supported for combined layers", 0, tileLinksForSingle.size());
        List<Link> imageLinksForSingle = getLinkByRelType(extentLinksForSingle, RelType.IMAGE);
        assertTrue(
                "Image link tref should contain all layer names",
                imageLinksForSingle.get(0).getTref().contains("layers=layerGroup,Polygons&"));
        List<Input> inputsSingleExtent =
                getTypeFromInputOrDataListOrLink(
                        mapmlSingleExtent.getBody().getExtents().get(0).getInputOrDatalistOrLink(),
                        Input.class);
        List<String> inputNamesSingleExtent =
                inputsSingleExtent.stream()
                        .map(input -> input.getName())
                        .collect(java.util.stream.Collectors.toList());
        assertTrue(
                "Input names should include all extent attributes",
                inputNamesSingleExtent.containsAll(
                        List.of("xmin", "ymin", "xmax", "ymax", "w", "h")));

        assertFalse(
                "For single, the extent hidden attribute should contain hidden",
                reader.toString().contains("hidden"));

        // Change To Return Multiple Extents for Multiple Layers
        wms.getMetadata().put(MapMLDocumentBuilder.MAPML_MULTILAYER_AS_MULTIEXTENT, Boolean.TRUE);
        geoServer.save(wms);

        MockRequestResponse requestResponseMultiExtent =
                getMockRequestResponse(
                        "layerGroup" + "," + MockData.POLYGONS.getLocalPart(),
                        null,
                        null,
                        "EPSG:3857",
                        null);
        StringReader readerMultiExtent =
                new StringReader(requestResponseMultiExtent.response.getContentAsString());
        Mapml mapmlMultiExtent = null;
        try {
            mapmlMultiExtent = encoder.decode(readerMultiExtent);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }
        List<Link> selfStyleLinksForMulti =
                getLinkByRelType(mapmlMultiExtent.getHead().getLinks(), RelType.SELF_STYLE);
        assertTrue(
                "No matter the setting, self style should refer to all layers",
                selfStyleLinksForMulti.get(0).getHref().contains("layers=layerGroup%2CPolygons&"));
        assertTrue(
                "Multi-layer multi-extent first map-extent should have a label",
                mapmlMultiExtent
                        .getBody()
                        .getExtents()
                        .get(0)
                        .getLabel()
                        .equalsIgnoreCase("layerGroup"));
        assertTrue(
                "Multi-layer multi-extent second map-extent should have a label",
                mapmlMultiExtent
                        .getBody()
                        .getExtents()
                        .get(1)
                        .getLabel()
                        .equalsIgnoreCase("Polygons"));
        assertEquals(
                "There should be one extent object for every layer",
                2,
                mapmlMultiExtent.getBody().getExtents().size());

        List<Link> extentLinks =
                getTypeFromInputOrDataListOrLink(
                        mapmlMultiExtent.getBody().getExtents().get(0).getInputOrDatalistOrLink(),
                        Link.class);
        List<Link> queryLinks = getLinkByRelType(extentLinks, RelType.QUERY);
        assertEquals("There should be one query link for every layer", 1, queryLinks.size());
        assertTrue(
                "The query link TREF should refer to only one layer name",
                queryLinks.get(0).getTref().contains("query_layers=layerGroup&"));
        List<Link> tileLinks = getLinkByRelType(extentLinks, RelType.TILE);
        assertEquals("There should be one tile link for every layer", 1, tileLinks.size());
        assertTrue(
                "The tile link TREF should refer to only one layer name",
                tileLinks.get(0).getTref().contains("layers=layerGroup&"));
        List<Input> inputsMultiExtent =
                getTypeFromInputOrDataListOrLink(
                        mapmlMultiExtent.getBody().getExtents().get(0).getInputOrDatalistOrLink(),
                        Input.class);
        List<String> inputNamesMultiExtent =
                inputsMultiExtent.stream()
                        .map(input -> input.getName())
                        .collect(java.util.stream.Collectors.toList());
        assertTrue(
                "Input names should include all extent attributes",
                inputNamesMultiExtent.containsAll(
                        List.of("z", "tymin", "txmax", "tymax", "txmin", "i", "j")));

        assertFalse(
                "For multi-extent, the extent hidden attribute should be excluded",
                readerMultiExtent.toString().contains("hidden"));
        StyleInfo styleInfo = getCatalog().getStyleByName("BasicPolygons");
        li.getStyles().add(styleInfo);
        cat.save(li);
        MockRequestResponse requestResponseMultiExtentWithMultiStyles =
                getMockRequestResponse(
                        MockData.POLYGONS.getLocalPart() + "," + "layerGroup",
                        null,
                        null,
                        "EPSG:3857",
                        "BasicPolygons,");
        StringReader readerMultiExtentWithMultiStyles =
                new StringReader(
                        requestResponseMultiExtentWithMultiStyles.response.getContentAsString());
        Mapml mapmlMultiExtentWithMultiStyles = null;
        try {
            mapmlMultiExtentWithMultiStyles = encoder.decode(readerMultiExtentWithMultiStyles);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }
        assertTrue(
                "Can handle multiple styles, with the last style left as blank",
                mapmlMultiExtentWithMultiStyles
                        .getHead()
                        .getLinks()
                        .get(0)
                        .getHref()
                        .contains("styles=BasicPolygons%2C&"));
    }

    @SuppressWarnings("unchecked") // filtering by clazz
    private <T> List<T> getTypeFromInputOrDataListOrLink(
            List<Object> inputOrDatalistOrLink, Class<T> clazz) {
        return (List<T>)
                inputOrDatalistOrLink.stream()
                        .filter(o -> clazz.isInstance(o))
                        .collect(java.util.stream.Collectors.toList());
    }

    private List<Link> getLinkByRelType(List<Link> links, RelType relType) {
        return links.stream()
                .filter(link -> link.getRel().equals(relType))
                .collect(java.util.stream.Collectors.toList());
    }

    @Test
    public void testNonExistentLayer() throws Exception {
        MockRequestResponse requestResponse =
                getMockRequestResponse("nonexistent", null, null, "EPSG:3857", null);
        assertTrue(
                requestResponse
                        .response
                        .getContentAsString()
                        .contains(
                                "<ServiceException code=\"LayerNotDefined\" locator=\"layers\">"));
    }

    @Test
    public void testNonExistentProjection() throws Exception {
        MockRequestResponse requestResponse =
                getMockRequestResponse("Polgons", null, null, "EPSG:9999", null);
        assertTrue(
                requestResponse
                        .response
                        .getContentAsString()
                        .contains(
                                "<ServiceException code=\"InvalidParameterValue\" locator=\"crs\">"));
    }

    @Test
    public void testShardingMapMLLayer() throws Exception {

        // set up mapml layer to useTiles
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();
        MetadataMap mm = layerMeta.getMetadata();
        mm.put("mapml.enableSharding", true);
        mm.put("mapml.shardList", "server1,server2,server3");
        mm.put("mapml.shardServerPattern", "{s}.example.com");
        catalog.save(layerMeta);

        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        MockData.ROAD_SEGMENTS.getPrefix()
                                + ":"
                                + MockData.ROAD_SEGMENTS.getLocalPart(),
                        null,
                        null,
                        "EPSG:3857",
                        null);

        org.w3c.dom.Document doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponse.response.getContentAsString().getBytes()),
                        true);

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        String url = xpath.evaluate("//html:map-link[@rel='image']/@tref", doc);
        assertTrue(url.startsWith("http://{s}.example.com"));
        assertXpathEvaluatesTo("1", "count(//html:map-datalist[@id='servers'])", doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@list='servers'][@type='hidden'][@shard='true'][@name='s'])",
                doc);
        assertXpathEvaluatesTo("3", "count(//html:map-datalist/map-option)", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-datalist/map-option[@value='server1'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-datalist/map-option[@value='server2'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-datalist/map-option[@value='server3'])", doc);

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='query'][@tref])", doc);
        url = xpath.evaluate("//html:map-link[@rel='query']/@tref", doc);
        assertTrue(url.startsWith("http://{s}.example.com"));
    }

    @Test
    public void testDefaultConfiguredMapMLLayer() throws Exception {
        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        MockData.ROAD_SEGMENTS.getPrefix()
                                + ":"
                                + MockData.ROAD_SEGMENTS.getLocalPart(),
                        null,
                        Locale.FRENCH,
                        "EPSG:3857",
                        null);

        org.w3c.dom.Document doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponse.response.getContentAsString().getBytes()),
                        true);

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:map-link[@rel='image']/@tref", doc));
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
        assertTrue(vars.get("language").equalsIgnoreCase(Locale.FRENCH.getLanguage()));

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:map-link[@rel='query']/@tref", doc));
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
        assertTrue(vars.get("language").equalsIgnoreCase(Locale.FRENCH.getLanguage()));

        // make sure there's an input for each template variable
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@name='xmin'][@type='location'][@units='pcrs'][@axis='easting'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@name='ymin'][@type='location'][@units='pcrs'][@axis='northing'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@name='xmax'][@type='location'][@units='pcrs'][@axis='easting'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@name='ymax'][@type='location'][@units='pcrs'][@axis='northing'][@min][@max])",
                doc);
        assertXpathEvaluatesTo("1", "count(//html:map-input[@name='w'][@type='width'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-input[@name='h'][@type='height'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:map-input[@name='i'][@type='location'][@units='map'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:map-input[@name='j'][@type='location'][@units='map'])", doc);

        // this is a weird one, probably should not be necessary, but if we
        // remove the requirement to have it, we will have to specify a
        // requirement to specify a zoom range via a <meta> element
        assertXpathEvaluatesTo(
                "1", "count(//html:map-input[@name='z'][@type='zoom'][@min][@max])", doc);
    }

    @Test
    public void testGWCTilesConfiguredMapMLLayer() throws Exception {
        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        MockData.ROAD_SEGMENTS.getPrefix()
                                + ":"
                                + MockData.ROAD_SEGMENTS.getLocalPart(),
                        null,
                        null,
                        "EPSG:4326",
                        null);

        org.w3c.dom.Document doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponse.response.getContentAsString().getBytes()),
                        true);

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);

        // set up mapml layer to useTiles
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();

        layerMeta.getMetadata().put("mapml.useTiles", true);
        catalog.save(layerMeta);
        GWC gwc = applicationContext.getBean(GWC.class);
        GWCConfig defaults = GWCConfig.getOldDefaults();
        // it seems just the fact of retrieving the bean causes the
        // GridSets to be added to the gwc GridSetBroker, but if you don't do
        // this, they are not added automatically
        MapMLGridsets mgs = applicationContext.getBean(MapMLGridsets.class);
        GridSubset wgs84gridset = createGridSubSet(mgs.getGridSet("WGS84").get());
        GridSubset osmtilegridset = createGridSubSet(mgs.getGridSet("OSMTILE").get());
        LayerInfo layerInfo = catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart());
        GeoServerTileLayer layerInfoTileLayer =
                new GeoServerTileLayer(layerInfo, defaults, gwc.getGridSetBroker());
        layerInfoTileLayer.addGridSubset(wgs84gridset);
        layerInfoTileLayer.addGridSubset(osmtilegridset);
        gwc.save(layerInfoTileLayer);
        String wmtsLayerName =
                MockData.ROAD_SEGMENTS.getPrefix() + ":" + MockData.ROAD_SEGMENTS.getLocalPart();
        MockRequestResponse requestResponseJapanese =
                getMockRequestResponse(
                        MockData.ROAD_SEGMENTS.getPrefix()
                                + ":"
                                + MockData.ROAD_SEGMENTS.getLocalPart(),
                        null,
                        Locale.JAPANESE,
                        "EPSG:4326",
                        null);

        doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponseJapanese.response.getContentAsString().getBytes()),
                        true);

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='tile'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc));
        HashMap<String, String> vars = parseQuery(url);
        assertTrue(vars.get("request").equalsIgnoreCase("GetTile"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMTS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.0.0"));
        assertTrue(vars.get("layer").equalsIgnoreCase(wmtsLayerName));
        assertTrue(vars.get("format").equalsIgnoreCase("image/png"));
        assertTrue(vars.get("tilematrixset").equalsIgnoreCase("WGS84"));
        assertTrue(vars.get("tilematrix").equalsIgnoreCase("{z}"));
        assertTrue(vars.get("TileRow").equalsIgnoreCase("{y}"));
        assertTrue(vars.get("TileCol").equalsIgnoreCase("{x}"));
        assertTrue(vars.get("style").equalsIgnoreCase(""));
        assertNull(vars.get("language"));
        //        assertTrue(vars.get("language").equalsIgnoreCase(Locale.ENGLISH.getLanguage()));

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:map-link[@rel='query']/@tref", doc));
        vars = parseQuery(url);

        assertTrue(vars.get("request").equalsIgnoreCase("GetFeatureInfo"));
        assertTrue(vars.get("service").equalsIgnoreCase("WMTS"));
        assertTrue(vars.get("version").equalsIgnoreCase("1.0.0"));
        assertTrue(vars.get("layer").equalsIgnoreCase(wmtsLayerName));
        assertTrue(vars.get("tilematrixset").equalsIgnoreCase("WGS84"));
        assertTrue(vars.get("tilematrix").equalsIgnoreCase("{z}"));
        assertTrue(vars.get("TileRow").equalsIgnoreCase("{y}"));
        assertTrue(vars.get("TileCol").equalsIgnoreCase("{x}"));
        assertTrue(vars.get("style").equalsIgnoreCase(""));
        assertTrue(vars.get("i").equalsIgnoreCase("{i}"));
        assertTrue(vars.get("j").equalsIgnoreCase("{j}"));
        assertTrue(vars.get("infoformat").equalsIgnoreCase("text/mapml"));
        assertTrue(vars.get("feature_count").equalsIgnoreCase("50"));
        assertNull(vars.get("language"));
        //        assertTrue(vars.get("language").equalsIgnoreCase(Locale.ENGLISH.getLanguage()));

        // make sure there's an input for each template variable
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@name='x'][@type='location'][@units='tilematrix'][@axis='column'][@min][@max])",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "count(//html:map-input[@name='y'][@type='location'][@units='tilematrix'][@axis='row'][@min][@max])",
                doc);
        assertXpathEvaluatesTo("0", "count(//html:map-input[@name='w'][@type='width'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-input[@name='h'][@type='height'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:map-input[@name='i'][@type='location'][@units='tile'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:map-input[@name='j'][@type='location'][@units='tile'])", doc);
        assertXpathEvaluatesTo(
                "1", "count(//html:map-input[@name='z'][@type='zoom'][@min][@max])", doc);

        MockRequestResponse requestResponseOSMTile =
                getMockRequestResponse(
                        MockData.ROAD_SEGMENTS.getPrefix()
                                + ":"
                                + MockData.ROAD_SEGMENTS.getLocalPart(),
                        null,
                        Locale.JAPANESE,
                        "EPSG:3857",
                        null);

        org.w3c.dom.Document docOsmTile =
                dom(
                        new ByteArrayInputStream(
                                requestResponseOSMTile.response.getContentAsString().getBytes()),
                        true);
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='tile'][@tref])", docOsmTile);
        URL osmTileUrl = new URL(xpath.evaluate("//html:map-link[@rel='tile']/@tref", docOsmTile));
        HashMap<String, String> osmTileVars = parseQuery(osmTileUrl);
        assertTrue(osmTileVars.get("request").equalsIgnoreCase("GetTile"));
        assertTrue(osmTileVars.get("service").equalsIgnoreCase("WMTS"));
        assertTrue(osmTileVars.get("version").equalsIgnoreCase("1.0.0"));
        assertTrue(osmTileVars.get("layer").equalsIgnoreCase(wmtsLayerName));
        assertTrue(osmTileVars.get("format").equalsIgnoreCase("image/png"));
        assertTrue(osmTileVars.get("tilematrixset").equalsIgnoreCase("OSMTILE"));
        assertTrue(osmTileVars.get("tilematrix").equalsIgnoreCase("{z}"));
        assertTrue(osmTileVars.get("TileRow").equalsIgnoreCase("{y}"));
        assertTrue(osmTileVars.get("TileCol").equalsIgnoreCase("{x}"));
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
        MockRequestResponse requestResponse =
                getMockRequestResponse(forests, vars, null, null, null);
        org.w3c.dom.Document doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponse.response.getContentAsString().getBytes()),
                        true);
        assertXpathEvaluatesTo("1", "count(//html:map-feature)", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-featurecaption)", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-geometry)", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-properties)", doc);
    }

    @Test
    public void testDefaultConfiguredMapMLLayerGetFeatureInfoAsMapML() throws Exception {

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

        MockRequestResponse requestResponseEnglish =
                getMockRequestResponse(
                        MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart(),
                        null,
                        Locale.ENGLISH,
                        "EPSG:3857",
                        null);

        org.w3c.dom.Document doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponseEnglish.response.getContentAsString().getBytes()),
                        true);

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='query'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:map-link[@rel='query']/@tref", doc));
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
        assertTrue(vars.get("language").equalsIgnoreCase(Locale.ENGLISH.getLanguage()));
        vars.put(
                "bbox",
                "-967387.0299771908,-118630.26789859355,884223.543202919,920913.3167798058");
        vars.put("width", "757");
        vars.put("height", "425");
        vars.put("x", "379");
        vars.put("y", "213");
        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        MockData.BASIC_POLYGONS.getLocalPart(), vars, null, null, null);
        doc =
                dom(
                        new ByteArrayInputStream(
                                requestResponse.response.getContentAsString().getBytes()),
                        true);
        assertXpathEvaluatesTo("2", "count(//html:map-feature)", doc);
        // empty attributes (such as ID in this case - all empty) won't be used
        assertXpathEvaluatesTo("0", "count(//html:map-featurecaption)", doc);
        assertXpathEvaluatesTo("", "//html:map-feature/html:map-geometry/@cs", doc);
        assertXpathEvaluatesTo("2", "count(//html:map-feature//html:map-polygon[1])", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-meta[@name='projection'])", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-meta[@name='cs'])", doc);
    }

    @Test
    public void testHTML() throws Exception {

        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        Document d = testLayersAndGroupsHTML(li, Locale.CANADA_FRENCH);

        assertTrue(
                "HTML layer title must be internationalized",
                "Le titre français".equalsIgnoreCase(d.title()));

        LayerGroupInfo lgi = cat.getLayerGroupByName(NATURE_GROUP);
        d = testLayersAndGroupsHTML(lgi, null);
        assertTrue(
                "HTML layer group title must be internationalized",
                "A i18n title for polygons".equalsIgnoreCase(d.title()));

        LayerGroupInfo noTitleLG = cat.getLayerGroupByName("layerGroup");
        d = testLayersAndGroupsHTML(noTitleLG, null);
        assertTrue(
                "HTML layer group title must NOT be internationalized",
                "layerGroup".equalsIgnoreCase(d.title()));
    }

    @SuppressWarnings("PMD.SimplifiableTestAssertion")
    private Document testLayersAndGroupsHTML(Object l, Locale locale) throws Exception {
        String layerName;
        String layerLabel;
        LayerInfo lyrInfo = getCatalog().getLayerByName(((PublishedInfo) l).getName());
        LayerGroupInfo lyrGpInfo = getCatalog().getLayerGroupByName(((PublishedInfo) l).getName());
        if (lyrInfo != null) { // layer...
            layerName = lyrInfo.getName();
        } else {
            layerName = lyrGpInfo.getName();
        }
        String path =
                "wms?LAYERS="
                        + layerName
                        + "&STYLES=&FORMAT="
                        + MapMLConstants.MAPML_HTML_MIME_TYPE
                        + "&SERVICE=WMS&VERSION=1.3.0"
                        + "&REQUEST=GetMap"
                        + "&SRS=epsg:3857"
                        + "&BBOX=-13885038,2870337,-7455049,6338174"
                        + "&WIDTH=150"
                        + "&HEIGHT=150"
                        + "&format_options="
                        + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                        + ":image/png";
        MockHttpServletRequest request = createRequest(path);
        if (locale != null) {
            request.addPreferredLocale(locale);
        }
        if (lyrInfo != null) { // layer...
            layerLabel = getLabel(lyrInfo, layerName, request);
        } else { // layer group...
            layerLabel = getLabel(lyrGpInfo, layerName, request);
        }
        request.setMethod("GET");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request, "UTF-8");
        String htmlResponse = response.getContentAsString();
        assertNotNull("Html method must return a document", htmlResponse);
        Document doc = Jsoup.parse(htmlResponse);
        Element webmapimport = doc.head().select("script").first();
        assertTrue(
                "HTML document script must use mapml-viewer.js module",
                webmapimport.attr("src").matches(".*mapml-viewer\\.js"));
        Element map = doc.body().select("mapml-viewer").first();
        Element layer = map.getElementsByTag("layer-").first();
        assertTrue(
                "Layer must have label equal to title or layer name if no title",
                layer.attr("label").equalsIgnoreCase(layerLabel));
        assertTrue(
                "HTML title and layer- label attribute should be equal",
                layer.attr("label").equalsIgnoreCase(doc.title()));
        String zoom = doc.select("mapml-viewer").attr("zoom");
        // zoom is calculated based on a display size and the extent of the
        // layer.  In the case of the test layer group "layerGroup", the extent is the
        // maximum extent, so zoom should be 1;
        if (layerName.equalsIgnoreCase("layerGroup")) {
            assertTrue("4".equalsIgnoreCase(zoom));
        } else {
            assertTrue(!"0".equalsIgnoreCase(zoom));
        }
        return doc;
    }

    @SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation")
    public Mapml testLayersAndGroupsMapML(Object l, Locale locale) throws Exception {

        MockRequestResponse requestResponse =
                getMockRequestResponse(
                        ((PublishedInfo) l).getName(), null, locale, "EPSG:3857", null);

        MapMLEncoder encoder = new MapMLEncoder();
        StringReader reader = new StringReader(requestResponse.response.getContentAsString());
        Mapml mapml = null;
        try {
            mapml = encoder.decode(reader);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }

        String layerName = "";
        String layerTitle = "";
        LayerInfo lyrInfo = getCatalog().getLayerByName(((PublishedInfo) l).getName());
        LayerGroupInfo lyrGpInfo = getCatalog().getLayerGroupByName(((PublishedInfo) l).getName());
        if (lyrInfo != null) { // layer...
            layerName = lyrInfo.getName();
            layerTitle = getLabel(lyrInfo, layerName, requestResponse.request);
        } else { // layer group...
            layerName = lyrGpInfo.getName();
            layerTitle = getLabel(lyrGpInfo, layerName, requestResponse.request);
        }
        String result = requestResponse.response.getContentAsString();
        // this tests that the result has had namespaces mapped to minimum possible cruft
        assertTrue(result.contains("<mapml- xmlns=\"http://www.w3.org/1999/xhtml\">"));

        String title = mapml.getHead().getTitle();
        assertTrue(
                "MapML document title must equal layer title", title.equalsIgnoreCase(layerTitle));
        BodyContent b = mapml.getBody();
        assertNotNull("mapML method must return MapML body in response", b);
        List<Extent> es = b.getExtents();
        Extent e = es.get(0);
        String checked = e.getChecked();
        assertTrue(
                "extent checked attribute is always checked", checked.equalsIgnoreCase("checked"));

        String hidden = e.getHidden();
        assertTrue("single extent is always hidden", hidden.equalsIgnoreCase("hidden"));

        String label = e.getLabel();
        assertNull(label);

        ProjType projType = e.getUnits();
        assertSame(ProjType.OSMTILE, projType);

        List<Object> lo = e.getInputOrDatalistOrLink();
        for (Object o : lo) {
            if (o instanceof Link) {
                Link link = (Link) o;
                assertNull("map-extent/map-link@href unexpected.", link.getHref());
                assertNotNull("map-extent/map-link@href must not be null/empty", link.getTref());
                assertFalse(
                        "map-extent/map-link@href must not be null/empty",
                        link.getTref().isEmpty());
                assertTrue(
                        "link rel for this layer group must bel image, query or tile",
                        (link.getRel() == RelType.IMAGE
                                || link.getRel() == RelType.QUERY
                                || link.getRel() == RelType.TILE));
                // lots of stuff that is better covered by validation.
            } else if (o instanceof Input) {
                Input input = (Input) o;
                assertTrue(
                        "inputs must be of type zoom, location, width or height",
                        input.getType() == InputType.ZOOM
                                || input.getType() == InputType.LOCATION
                                || input.getType() == InputType.WIDTH
                                || input.getType() == InputType.HEIGHT);
                if (input.getType() == InputType.LOCATION && input.getAxis() == AxisType.EASTING) {
                    assertTrue(
                            "map-input[type=location/@min must equal -2.0037508342789244E7",
                            "-2.0037508342789244E7".equalsIgnoreCase(input.getMin()));
                    assertTrue(
                            "map-input[type=location/@max must equal 2.0037508342789244E7",
                            "2.0037508342789244E7".equalsIgnoreCase(input.getMax()));
                } else if (input.getType() == InputType.LOCATION
                        && input.getAxis() == AxisType.NORTHING) {
                    assertTrue(
                            "map-input[type=location/@min must equal -2.0037508342780735E7",
                            "-2.0037508342780735E7".equalsIgnoreCase(input.getMin()));
                    assertTrue(
                            "map-input[type=location/@max must equal 2.003750834278071E7",
                            "2.003750834278071E7".equalsIgnoreCase(input.getMax()));
                }
            } else {
                fail("Unrecognized test object type:" + o.getClass().getTypeName());
            }
        }
        return mapml;
    }

    private MockRequestResponse getMockRequestResponse(
            String name, Map kvp, Locale locale, String srs, String styles) throws Exception {
        String path = null;
        MockHttpServletRequest request = null;
        if (kvp != null) {
            path = "wms";
            request = createRequest(path, kvp);
        } else {
            path =
                    "wms?LAYERS="
                            + name
                            + "&STYLES="
                            + (styles != null ? styles : "")
                            + "&FORMAT="
                            + MapMLConstants.MAPML_MIME_TYPE
                            + "&SERVICE=WMS&VERSION=1.3.0"
                            + "&REQUEST=GetMap"
                            + "&SRS="
                            + srs
                            + "&BBOX=0,0,1,1"
                            + "&WIDTH=150"
                            + "&HEIGHT=150"
                            + "&format_options="
                            + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                            + ":image/png";
            request = createRequest(path);
        }

        if (locale != null) {
            request.addPreferredLocale(locale);
        }
        request.setMethod("GET");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request, "UTF-8");
        MockRequestResponse result = new MockRequestResponse(request, response);
        return result;
    }

    private static class MockRequestResponse {
        public final MockHttpServletRequest request;
        public final MockHttpServletResponse response;

        public MockRequestResponse(
                MockHttpServletRequest request, MockHttpServletResponse response) {
            this.request = request;
            this.response = response;
        }
    }

    /**
     * Get the potentially localized label string for a layer or layer group
     *
     * @param p LayerInfo or LayerGroupInfo object
     * @param def default label string, usually pass in the layer name
     * @param request the localized servlet request
     * @return the potentially localized label string for a layer or layer group
     */
    String getLabel(PublishedInfo p, String def, HttpServletRequest request) {
        if (p instanceof LayerGroupInfo) {
            LayerGroupInfo li = (LayerGroupInfo) p;
            if (li.getInternationalTitle() != null
                    && li.getInternationalTitle().toString(request.getLocale()) != null) {
                // use international title per request or default locale
                return li.getInternationalTitle().toString(request.getLocale());
            } else if (li.getTitle() != null && !li.getTitle().trim().isEmpty()) {
                return li.getTitle().trim();
            } else {
                return li.getName().trim().isEmpty() ? def : li.getName().trim();
            }
        } else {
            LayerInfo li = (LayerInfo) p;
            if (li.getInternationalTitle() != null
                    && li.getInternationalTitle().toString(request.getLocale()) != null) {
                // use international title per request or default locale
                return li.getInternationalTitle().toString(request.getLocale());
            } else if (li.getTitle() != null && !li.getTitle().trim().isEmpty()) {
                return li.getTitle().trim();
            } else {
                return li.getName().trim().isEmpty() ? def : li.getName().trim();
            }
        }
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
