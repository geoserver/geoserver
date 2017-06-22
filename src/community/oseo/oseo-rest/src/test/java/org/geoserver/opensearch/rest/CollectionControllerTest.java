/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jayway.jsonpath.DocumentContext;

public class CollectionControllerTest extends OSEORestTestSupport {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Before
    public void cleanupTestCollection() throws IOException {
        DataStoreInfo ds = getCatalog().getDataStoreByName("oseo");
        OpenSearchAccess access = (OpenSearchAccess) ds.getDataStore(null);
        FeatureStore store = (FeatureStore) access.getCollectionSource();
        store.removeFeatures(
                FF.equal(FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier")),
                        FF.literal("TEST123"), true));
    }

    @Test
    public void testGetCollections() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections", 200);
        assertEquals(3, json.read("$.collections.*", List.class).size());
        // check the first (sorted alphabetically)
        assertEquals("LANDSAT8", json.read("$.collections[0].name"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/LANDSAT8",
                json.read("$.collections[0].href"));
        assertEquals("http://localhost:8080/geoserver/oseo/description?parentId=LANDSAT8",
                json.read("$.collections[0].search"));
    }

    @Test
    public void testGetCollectionsPaging() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections?offset=1&limit=1", 200);
        assertEquals(1, json.read("$.collections.*", List.class).size());
        assertEquals("SENTINEL1", json.read("$.collections[0].name"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL1",
                json.read("$.collections[0].href"));
        assertEquals("http://localhost:8080/geoserver/oseo/description?parentId=SENTINEL1",
                json.read("$.collections[0].search"));
    }

    @Test
    public void testGetCollectionsPagingValidation() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/oseo/collections?offset=-1");
        assertEquals(400, response.getStatus());
        assertThat(response.getErrorMessage(), containsString("offset"));

        response = getAsServletResponse("/rest/oseo/collections?limit=-1");
        assertEquals(400, response.getStatus());
        assertThat(response.getErrorMessage(), containsString("limit"));

        response = getAsServletResponse("/rest/oseo/collections?limit=1000");
        assertEquals(400, response.getStatus());
        assertThat(response.getErrorMessage(), containsString("limit"));
    }

    @Test
    public void testNonExistingCollection() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/oseo/collections/foobar");
        assertEquals(404, response.getStatus());
        assertThat(response.getContentAsString(), containsString("foobar"));
    }

    @Test
    public void testGetCollection() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2", 200);
        assertEquals("SENTINEL2", json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("SENTINEL2", json.read("$.properties.name"));
        assertEquals("S2MSI1C", json.read("$.properties['eo:productType']"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/ogcLinks",
                json.read("$.properties['ogcLinksHref']"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/metadata",
                json.read("$.properties['metadataHref']"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/description",
                json.read("$.properties['descriptionHref']"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/thumbnail",
                json.read("$.properties['thumbnailHref']"));
    }

    @Test
    public void testGetCollectionLinks() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2/ogcLinks", 200);
        assertEquals("http://www.opengis.net/spec/owc/1.0/req/atom/wms",
                json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals("application/xml", json.read("$.links[0].type"));
        assertEquals("${BASE_URL}/sentinel2/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
    }

    @Test
    public void testGetCollectionMetadata() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/metadata");
        assertEquals(200, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        assertThat(response.getContentAsString(),
                both(containsString("gmi:MI_Metadata")).and(containsString("Sentinel-2")));
    }

    @Test
    public void testGetCollectionDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/description");
        assertEquals(200, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertThat(response.getContentAsString(),
                both(containsString("<table>")).and(containsString("Sentinel-2")));
    }

    @Test
    public void testGetCollectionThumbnail() throws Exception {
        // missing from the DB right now
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/oseo/collections/SENTINEL2/thumbnail");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testCreateCollectionNotJson() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("rest/oseo/collections",
                "This is not JSON", MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateCollectionNotGeoJson() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("rest/oseo/collections",
                "{foo: 45}", MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateInvalidAttributeSyntax() throws Exception {
        String testData = new String(getTestData("/collection.json"), "UTF-8");
        // inject an invalid attribute name
        String invalidTestData = testData.replace("primary", "1:2:primary");
        MockHttpServletResponse response = postAsServletResponse("rest/oseo/collections",
                invalidTestData, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("1:2:primary"));
    }

    @Test
    public void testCreateInvalidAttributePrefix() throws Exception {
        String testData = new String(getTestData("/collection.json"), "UTF-8");
        // inject an invalid attribute name
        String invalidTestData = testData.replace("eo:productType", "abc:productType");
        MockHttpServletResponse response = postAsServletResponse("rest/oseo/collections",
                invalidTestData, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("abc:productType"));
    }

    @Test
    public void testCreateInvalidAttributeName() throws Exception {
        String testData = new String(getTestData("/collection.json"), "UTF-8");
        // inject an invalid attribute name
        String invalidTestData = testData.replace("eo:productType", "eo:newProductType");
        MockHttpServletResponse response = postAsServletResponse("rest/oseo/collections",
                invalidTestData, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("eo:newProductType"));
    }

    @Test
    public void testCreateCollection() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("rest/oseo/collections",
                getTestData("/collection.json"), MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/TEST123",
                response.getHeader("location"));

        // check it's really there
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123", 200);
        assertEquals("TEST123", json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("TEST123", json.read("$.properties.name"));
        assertEquals("S2MS1C", json.read("$.properties['eo:productType']"));
        assertEquals("A", json.read("$.properties['eo:platformSerialIdentifier']"));
        assertEquals("MSI", json.read("$.properties['eo:instrument']"));
        assertEquals("2012-04-23T18:25:43.511+0000", json.read("$.properties['timeStart']"));
    }

    byte[] getTestData(String location) throws IOException {
        return IOUtils.toByteArray(getClass().getResourceAsStream(location));
    }
}
