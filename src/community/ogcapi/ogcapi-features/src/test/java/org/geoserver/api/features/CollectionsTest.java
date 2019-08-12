/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;

public class CollectionsTest extends FeaturesTestSupport {

    public static final String BASIC_POLYGONS_TITLE = "Basic polygons";
    public static final String BASIC_POLYGONS_DESCRIPTION = "I love basic polygons!";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        basicPolygons.setTitle(BASIC_POLYGONS_TITLE);
        basicPolygons.setAbstract(BASIC_POLYGONS_DESCRIPTION);
        getCatalog().save(basicPolygons);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/collections", 200);
        testCollectionsJson(json);
    }

    private void testCollectionsJson(DocumentContext json) throws Exception {
        int expected = getCatalog().getFeatureTypes().size();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats =
                GeoServerExtensions.bean(
                                APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                        .getProducibleMediaTypes(FeatureCollectionResponse.class, true);
        assertThat(
                formats.size(),
                lessThanOrEqualTo((int) json.read("collections[0].links.length()", Integer.class)));
        for (MediaType format : formats) {
            // check title and rel.
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("item", item.get("rel"));
        }
    }

    @Test
    @Ignore // workspace specific
    public void testCollectionsWorkspaceSpecificJson() throws Exception {
        DocumentContext json = getAsJSONPath("cdf/ogc/features/collections", 200);
        long expected =
                getCatalog()
                        .getFeatureTypes()
                        .stream()
                        .filter(ft -> "cdf".equals(ft.getStore().getWorkspace().getName()))
                        .count();
        // check the filtering
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
        // check the workspace prefixes have been removed
        assertThat(json.read("collections[?(@.name=='Deletes')]"), not(empty()));
        assertThat(json.read("collections[?(@.name=='cdf__Deletes')]"), empty());
        // check the url points to a ws qualified url
        final String deleteHrefPath =
                "collections[?(@.name=='Deletes')].links[?(@.rel=='item' && @.type=='application/geo+json')].href";
        assertEquals(
                "http://localhost:8080/geoserver/cdf/ogc/features/collections/Deletes/items?f=application%2Fgeo%2Bjson",
                ((JSONArray) json.read(deleteHrefPath)).get(0));
    }

    @Test
    public void testCollectionsXML() throws Exception {
        Document dom = getAsDOM("ogc/features/collections?f=application/xml");
        print(dom);
        // TODO: add actual tests
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/features/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json);
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/collections?f=html");

        // check collection links
        List<FeatureTypeInfo> featureTypes = getCatalog().getFeatureTypes();
        for (FeatureTypeInfo featureType : featureTypes) {
            String encodedName = NCNameResourceCodec.encode(featureType);
            assertNotNull(document.select("#html_" + encodedName + "_link"));
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/features/collections/"
                            + encodedName
                            + "/items?f=text%2Fhtml&limit=50",
                    document.select("#html_" + encodedName + "_link").attr("href"));
        }

        // go and check a specific collection title and description
        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        String basicPolygonsName = NCNameResourceCodec.encode(basicPolygons);
        assertEquals(
                BASIC_POLYGONS_TITLE, document.select("#" + basicPolygonsName + "_title").text());
        assertEquals(
                BASIC_POLYGONS_DESCRIPTION,
                document.select("#" + basicPolygonsName + "_description").text());
    }
}
