/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jayway.jsonpath.DocumentContext;

public class CollectionControllerTest extends OSEORestTestSupport {

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
}
