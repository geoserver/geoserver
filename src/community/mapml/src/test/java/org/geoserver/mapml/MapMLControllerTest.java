/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.BodyContent;
import org.geoserver.mapml.xml.Extent;
import org.geoserver.mapml.xml.Input;
import org.geoserver.mapml.xml.InputType;
import org.geoserver.mapml.xml.Link;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.ProjType;
import org.geoserver.mapml.xml.RelType;
import org.geoserver.wms.WMSTestSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class MapMLControllerTest extends WMSTestSupport {
    MapMLController mc;

    @Before
    public void setupController() {
        mc = applicationContext.getBean(MapMLController.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setupBounds(catalog.getLayerByName(points).getResource());
        cb.setupBounds(catalog.getLayerByName(lines).getResource());
        cb.setupBounds(catalog.getLayerByName(polygons).getResource());
        assertNotNull(cb.getNativeBounds(catalog.getLayerByName(polygons).getResource()));
        assertNotNull(catalog.getLayerByName(polygons).getResource().boundingBox());

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }

    @Test
    public void testMapML() throws Exception {

        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        testLayersAndGroupsMapML(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        testLayersAndGroupsMapML(lgi);
    }

    @Ignore
    public void testHTML() throws Exception {

        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        testLayersAndGroupsHTML(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName(NATURE_GROUP);
        testLayersAndGroupsHTML(lgi);
    }

    @Ignore
    public void testNonExistentLayer() throws Exception {
        MockHttpServletRequest request = createRequest("mapml/" + "foo" + "/osmtile/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String htmlResponse =
                mc.Html(
                        request,
                        response,
                        "foo",
                        "osmtile",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        assertTrue(
                "Response is 404 Not Found",
                response.getStatus() == HttpServletResponse.SC_NOT_FOUND);
    }

    @Ignore
    public void testNonExistentProjection() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        MockHttpServletRequest request = createRequest("mapml/" + li.getName() + "/foo/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String htmlResponse =
                mc.Html(
                        request,
                        response,
                        li.getName(),
                        "foo",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        assertTrue(
                "Response is 400 Bad Request",
                response.getStatus() == HttpServletResponse.SC_BAD_REQUEST);

        request = createRequest("mapml/" + li.getName() + "/foo/");
        response = new MockHttpServletResponse();

        Mapml mapml =
                mc.mapML(
                        request,
                        response,
                        li.getName(),
                        "foo",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());
        assertTrue(
                "Response is 400 Bad Request",
                response.getStatus() == HttpServletResponse.SC_BAD_REQUEST);
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
        Element webmapimport = doc.head().select("link").first();
        assertTrue(
                "HTML document must import web-map.html",
                webmapimport.attr("href").matches(".*web-map\\.html"));
        Element map = doc.body().select("map[is=web-map]").first();
        Element layer = map.getElementsByTag("layer-").first();
        assertTrue(
                "Layer must have label equal to string ",
                layer.attr("label").equalsIgnoreCase(((PublishedInfo) l).getName()));
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
            JAXB.marshal(mapml, sw);
        } catch (DataBindingException ex) {
            fail("DataBindingException while reading MapML JAXB object");
        }

        BodyContent b = mapml.getBody();
        assertNotNull("mapML method must return MapML body in response", b);
        Extent e = b.getExtent();
        String action = e.getAction();
        assertNull(action);
        ProjType projType = e.getUnits();
        assertTrue(ProjType.OSMTILE == projType);

        List<Object> lo = e.getInputOrDatalistOrLink();
        for (Object o : lo) {
            if (o instanceof Link) {
                Link link = (Link) o;
                assertNull("extent/link@href unexpected.", link.getHref());
                assertNotNull("extent/link@href must not be null/empty", link.getTref());
                assertTrue("extent/link@href must not be null/empty", !link.getTref().isEmpty());
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
            } else {
                fail("Unrecognized test object type:" + o.getClass().getTypeName());
            }
        }
    }
}
