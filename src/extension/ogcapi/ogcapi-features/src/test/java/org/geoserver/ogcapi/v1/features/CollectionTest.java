/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.data.test.MockData.ROAD_SEGMENTS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSInfo;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

public class CollectionTest extends FeaturesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // customize metadata and set custom CRS too
        FeatureTypeInfo basicPolygons = getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        basicPolygons.setOverridingServiceSRS(true);
        basicPolygons.getResponseSRS().addAll(Arrays.asList("3857", "32632"));
        getCatalog().save(basicPolygons);
    }

    @Before
    public void cleanupRoads() throws IOException {
        revertLayer(ROAD_SEGMENTS);
    }

    @Before
    public void resetWFS() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.getSRS().clear();
        wfs.setEnabled(true);
        gs.save(wfs);
    }

    @Test
    public void testCollectionJson() throws Exception {
        String roadSegments = getLayerId(ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + roadSegments, 200);

        assertEquals("cite:RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));
        assertEquals(-180, json.read("$.extent.spatial.bbox[0][0]", Double.class), 0d);
        assertEquals(-90, json.read("$.extent.spatial.bbox[0][1]", Double.class), 0d);
        assertEquals(180, json.read("$.extent.spatial.bbox[0][2]", Double.class), 0d);
        assertEquals(90, json.read("$.extent.spatial.bbox[0][3]", Double.class), 0d);
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", json.read("$.extent.spatial.crs", String.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats = getFeaturesResponseFormats();
        assertThat((int) json.read("$.links.length()", Integer.class), Matchers.greaterThanOrEqualTo(formats.size()));
        for (MediaType format : formats) {
            // check title and rel.
            List items = json.read("$.links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("cite:RoadSegments items as " + format, item.get("title"));
            assertEquals("items", item.get("rel"));
        }
        // the ogc/features specific GML3.2 output format is available
        readSingle(json, "links[?(@.type=='application/gml+xml;version=3.2')]");

        // check the queryables link
        assertThat(
                readSingle(json, "links[?(@.rel=='" + Queryables.REL + "' && @.type=='application/schema+json')].href"),
                equalTo("http://localhost:8080/geoserver/ogc/features/v1/collections/cite"
                        + ":RoadSegments/queryables?f=application%2Fschema%2Bjson"));

        // check the CRS list, this feature type shares the top level list
        List<String> crs = json.read("crs");
        assertThat(crs.size(), Matchers.greaterThan(5000)); // lots... the list is growing, hopefully will stay above 5k
        assertThat(
                crs,
                hasItems(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/4326",
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/IAU/0/1000"));
        crs.remove("http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        for (String c : crs) {
            assertTrue(
                    c + " is not using the expect CRS URI format",
                    c.matches("http://www.opengis.net/def/crs/[\\w]+/\\d+/\\d+"));
        }

        // check the storage CRS
        String storageCrs = json.read("storageCrs");
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", storageCrs);
    }

    @Test
    public void testCollectionJsonCustomCRSList() throws Exception {
        String roadSegments = getLayerId(MockData.BASIC_POLYGONS);
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + roadSegments, 200);

        assertEquals("cite:BasicPolygons", json.read("$.id", String.class));

        // check the CRS list, this feature type shares the top level list
        List<String> crs = json.read("crs");
        assertThat(
                crs,
                contains(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/EPSG/0/32632"));
    }

    @Test
    public void testCustomizeGlobalCRSList() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.getSRS().addAll(Arrays.asList("EPSG:4326", "EPSG:3857"));
        gs.save(wfs);

        String polygons = getLayerId(MockData.POLYGONS);
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + polygons, 200);

        assertEquals("cgf:Polygons", json.read("$.id", String.class));
        String storageCrs = json.read("storageCrs");
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/32615", storageCrs);
        // make sure the storage CRS is in the crs list, even if it was not declared globally
        List<String> crs = json.read("crs");
        assertEquals(4, crs.size());
        assertThat(
                crs,
                hasItems(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/4326",
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/EPSG/0/32615"));
    }

    private List<MediaType> getFeaturesResponseFormats() {
        return GeoServerExtensions.bean(APIDispatcher.class, applicationContext)
                .getProducibleMediaTypes(FeaturesResponse.class, true);
    }

    @Test
    public void testCollectionVirtualWorkspace() throws Exception {
        String roadSegments = ROAD_SEGMENTS.getLocalPart();
        DocumentContext json = getAsJSONPath("cite/ogc/features/v1/collections/" + roadSegments, 200);

        assertEquals("RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats = getFeaturesResponseFormats();
        assertThat((int) json.read("$.links.length()", Integer.class), Matchers.greaterThanOrEqualTo(formats.size()));
        for (MediaType format : formats) {
            // check title and rel.
            List items = json.read("$.links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("RoadSegments items as " + format, item.get("title"));
            assertEquals("items", item.get("rel"));
        }
        // the ogc/features specific GML3.2 output format is available
        readSingle(json, "$.links[?(@.type=='application/gml+xml;version=3.2')]");
    }

    @Test
    public void testCollectionYaml() throws Exception {
        getAsString("ogc/features/v1/collections/" + getLayerId(ROAD_SEGMENTS) + "?f=application/yaml");
        // System.out.println(yaml);
    }

    @Test
    public void testCollectionHTMLRemovedInlineJS() throws Exception {
        String html = getAsString("ogc/features/v1/collections/" + getLayerId(MockData.ROAD_SEGMENTS) + "?f=text/html");
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/common.js\"></script>"));
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/features.js\"></script>"));
        assertThat(html, containsString("<input type=\"hidden\" id=\"maxNumberOfFeaturesForPreview\" value=\"50\"/>"));
        assertThat(html, containsString("form-select-open-basic"));
        assertThat(html, containsString("form-select-open-limit"));
        assertThat(html, not(containsString("onchange")));
    }

    @Test
    public void testCustomLinks() throws Exception {
        FeatureTypeInfo roads = getCatalog().getFeatureTypeByName(getLayerId(ROAD_SEGMENTS));
        LinkInfoImpl link1 =
                new LinkInfoImpl("enclosure", "application/geopackage+sqlite3", "http://example.com/roads.gpkg");
        LinkInfoImpl link2 = new LinkInfoImpl("rasterized", "image/tiff", "http://example.com/roads.tif");
        link2.setService("Coverages");
        ArrayList<LinkInfo> links = Stream.of(link1, link2).collect(Collectors.toCollection(ArrayList::new));
        roads.getMetadata().put(LinkInfo.LINKS_METADATA_KEY, links);
        getCatalog().save(roads);

        String rsName = ROAD_SEGMENTS.getLocalPart();
        DocumentContext json = getAsJSONPath("cite/ogc/features/v1/collections/" + rsName, 200);

        // check first link
        DocumentContext l1c = readSingleContext(json, "$.links[?(@.rel=='enclosure')]");
        assertEquals(link1.getHref(), l1c.read("href"));
        assertEquals(link1.getType(), l1c.read("type"));

        // second link should not be there, service does not match
        List l2List = json.read("$.links[?(@.rel=='rasterized')]", List.class);
        assertTrue(l2List.isEmpty());
    }

    @Test
    public void testDescribeFeatureType() throws Exception {
        // WFS enabled
        String roadSegments = getLayerId(ROAD_SEGMENTS);
        String resource = "ogc/features/v1/collections/" + roadSegments;
        DocumentContext jsonEnabled = getAsJSONPath(resource, 200);
        assertThat(
                readSingle(jsonEnabled, "$.links[?(@.rel=='describedBy')].href"),
                allOf(
                        startsWith("http://localhost:8080/geoserver/wfs?"),
                        containsString("request=DescribeFeatureType"),
                        containsString("service=WFS"),
                        containsString("version=2.0"),
                        containsString("typenames=cite%3ARoadSegments")));

        // WFS disabled (commenting out, currently disables OGC API features too)
        // setWFSEnabled(false);
        // DocumentContext jsonDisabled = getAsJSONPath(resource, 200);
        // assertThat(jsonDisabled.read("$.links[?(@.rel=='describedBy')]"), empty());
    }
}
