/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.DocumentContext;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.rest.CollectionsController.CollectionPart;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class CollectionsControllerTest extends OSEORestTestSupport {

    @Test
    public void testGetCollections() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections", 200);
        assertEquals(7, json.read("$.collections.*", List.class).size());
        // check the first (sorted alphabetically)
        assertEquals("ATMTEST", json.read("$.collections[0].name"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/ATMTEST",
                json.read("$.collections[0].href"));
        assertEquals(
                "http://localhost:8080/geoserver/oseo/description?parentId=ATMTEST",
                json.read("$.collections[0].search"));
    }

    @Test
    public void testGetCollectionsPaging() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections?offset=4&limit=1", 200);
        assertEquals(1, json.read("$.collections.*", List.class).size());
        assertEquals("LANDSAT8", json.read("$.collections[0].name"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/LANDSAT8",
                json.read("$.collections[0].href"));
        assertEquals(
                "http://localhost:8080/geoserver/oseo/description?parentId=LANDSAT8",
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
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/ogcLinks",
                json.read("$.properties['ogcLinksHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/metadata",
                json.read("$.properties['metadataHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/description",
                json.read("$.properties['descriptionHref']"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL2/thumbnail",
                json.read("$.properties['thumbnailHref']"));
    }

    @Test
    public void testCreateCollectionNotJson() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections",
                        "This is not JSON",
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateCollectionNotGeoJson() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections", "{\"foo\": 45}", MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateInvalidAttributeSyntax() throws Exception {
        String testData = new String(getTestData("/collection.json"), UTF_8);
        // inject an invalid attribute name
        String invalidTestData = testData.replace("primary", "1:2:primary");
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections", invalidTestData, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("1:2:primary"));
    }

    @Test
    public void testCreateInvalidAttributePrefix() throws Exception {
        String testData = new String(getTestData("/collection.json"), UTF_8);
        // inject an invalid attribute name
        String invalidTestData = testData.replace("eo:productType", "abc:productType");
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections", invalidTestData, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("abc:productType"));
    }

    @Test
    public void testCreateInvalidAttributeName() throws Exception {
        String testData = new String(getTestData("/collection.json"), UTF_8);
        // inject an invalid attribute name
        String invalidTestData = testData.replace("eo:productType", "eo:newProductType");
        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections", invalidTestData, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString("eo:newProductType"));
    }

    @Test
    public void testCreateCollection() throws Exception {
        createTest123Collection();
        assertTest123CollectionCreated();
    }

    @Test
    public void testUpdateCollection() throws Exception {
        MockHttpServletResponse response;
        createTest123Collection();

        // grab the JSON to modify some bits
        JSONObject feature = (JSONObject) getAsJSON("/rest/oseo/collections/TEST123");
        JSONObject properties = feature.getJSONObject("properties");
        JSONArray queryables = properties.getJSONArray("queryables");
        JSONArray workspaces = properties.getJSONArray("workspaces");
        properties.element("eo:productType", "PT-123");
        properties.element("timeStart", "2017-01-01T00:00:00Z");
        queryables.element(0, "eo:orbitType");
        workspaces.element(0, "cite");

        // send it back
        response =
                putAsServletResponse(
                        "rest/oseo/collections/TEST123", feature.toString(), "application/json");
        assertEquals(200, response.getStatus());

        // check the changes
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123", 200);

        assertEquals("PT-123", json.read("$.properties['eo:productType']"));
        assertEquals("2017-01-01T00:00:00.000Z", json.read("$.properties['timeStart']"));
        assertEquals(
                "[\"eo:orbitType\",\"eo:productType\",\"eo:platform\",\"eo:platformSerialIdentifier\",\"eo:instrument\",\"eo:sensorType\",\"eo:compositeType\"]",
                json.read("$.properties['queryables']").toString());
        assertEquals("cite", json.read("$.properties['workspaces'][0]"));
    }

    @Test
    public void testUpdateEnabled() throws Exception {
        MockHttpServletResponse response;
        createTest123Collection();

        // grab the JSON to modify some bits
        JSONObject feature = (JSONObject) getAsJSON("/rest/oseo/collections/DISABLED_COLLECTION");
        JSONObject properties = feature.getJSONObject("properties");
        properties.element("enabled", "true");

        // send it back
        response =
                putAsServletResponse(
                        "rest/oseo/collections/DISABLED_COLLECTION",
                        feature.toString(),
                        "application/json");
        assertEquals(200, response.getStatus());

        // check the changes
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/DISABLED_COLLECTION", 200);
        assertEquals(Boolean.TRUE, json.read("$.properties['enabled']"));
    }

    @Test
    public void testDeleteCollection() throws Exception {
        MockHttpServletResponse response;
        createTest123Collection();

        // it's there
        getAsJSONPath("/rest/oseo/collections/TEST123", 200);

        // and now kill the poor beast
        response = deleteAsServletResponse("/rest/oseo/collections/TEST123");
        assertEquals(200, response.getStatus());

        // no more there
        response = getAsServletResponse("/rest/oseo/collections/TEST123");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetCollectionLinks() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/SENTINEL2/ogcLinks", 200);
        assertEquals(
                "http://www.opengis.net/spec/owc/1.0/req/atom/wms",
                json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals("application/xml", json.read("$.links[0].type"));
        assertEquals(
                "${BASE_URL}/sentinel2/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
    }

    @Test
    public void testPutCollectionLinks() throws Exception {
        MockHttpServletResponse response;
        createTest123Collection();

        // create the links
        response =
                putAsServletResponse(
                        "rest/oseo/collections/TEST123/ogcLinks",
                        getTestData("/test123-links.json"),
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(200, response.getStatus());

        // check they are there
        assertTest123Links();
    }

    private void assertTest123Links() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123/ogcLinks", 200);
        assertEquals(
                "http://www.opengis.net/spec/owc/1.0/req/atom/wms",
                json.read("$.links[0].offering"));
        assertEquals("GET", json.read("$.links[0].method"));
        assertEquals("GetCapabilities", json.read("$.links[0].code"));
        assertEquals("application/xml", json.read("$.links[0].type"));
        assertEquals(
                "${BASE_URL}/test123/ows?service=wms&version=1.3.0&request=GetCapabilities",
                json.read("$.links[0].href"));
        assertEquals("123", json.read("$.links[0].intTest").toString());
        assertEquals("123.1", json.read("$.links[0].floatTest").toString());
        assertEquals("false", json.read("$.links[0].booleanTest").toString());
        assertEquals("2015-07-01T00:00:00.000Z", json.read("$.links[0].dateTest").toString());
        assertEquals("text123", json.read("$.links[0].varcharTest").toString());
    }

    @Test
    public void testDeleteCollectionLinks() throws Exception {
        testPutCollectionLinks();

        // delete the links
        MockHttpServletResponse response =
                deleteAsServletResponse("rest/oseo/collections/TEST123/ogcLinks");
        assertEquals(200, response.getStatus());

        // check they are gone
        response = getAsServletResponse("rest/oseo/collections/TEST123/ogcLinks");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetCollectionThumbnail() throws Exception {
        // missing from the DB right now
        MockHttpServletResponse response =
                getAsServletResponse("/rest/oseo/collections/SENTINEL2/thumbnail");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testCreateCollectionAsZip() throws Exception {
        // build all possible combinations of elements in the zip and check they all work
        Set<Set<CollectionPart>> sets =
                Sets.powerSet(
                        new HashSet<>(
                                Arrays.asList(CollectionPart.Collection, CollectionPart.OwsLinks)));

        for (Set<CollectionPart> parts : sets) {
            if (parts.isEmpty()) {
                continue;
            }

            cleanupTestCollection();
            testCreateCollectionAsZip(parts);
        }
    }

    private void testCreateCollectionAsZip(Set<CollectionPart> parts) throws Exception {
        LOGGER.info("Testing: " + parts);
        byte[] zip = null;
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (CollectionPart part : parts) {
                String resource, name;
                switch (part) {
                    case Collection:
                        resource = "/collection.json";
                        name = "collection.json";
                        break;
                    case OwsLinks:
                        resource = "/test123-links.json";
                        name = "owsLinks.json";
                        break;
                    default:
                        throw new RuntimeException("Unexpected part " + part);
                }

                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                IOUtils.copy(getClass().getResourceAsStream(resource), zos);
                zos.closeEntry();
            }
            zip = bos.toByteArray();
        }

        MockHttpServletResponse response =
                postAsServletResponse(
                        "rest/oseo/collections", zip, MediaTypeExtensions.APPLICATION_ZIP_VALUE);
        if (parts.contains(CollectionPart.Collection)) {
            assertEquals(201, response.getStatus());
            assertEquals(
                    "http://localhost:8080/geoserver/rest/oseo/collections/TEST123",
                    response.getHeader("location"));

            assertTest123CollectionCreated();
        } else {
            assertEquals(400, response.getStatus());
            assertThat(response.getContentAsString(), containsString("collection.json"));
            // failed, nothing else to check
            return;
        }

        if (parts.contains(CollectionPart.OwsLinks)) {
            assertTest123Links();
        }
    }

    private void assertTest123CollectionCreated() throws Exception {
        // check it's really there
        DocumentContext json = getAsJSONPath("/rest/oseo/collections/TEST123", 200);
        assertEquals("TEST123", json.read("$.id"));
        assertEquals("Feature", json.read("$.type"));
        assertEquals("TEST123", json.read("$.properties.name"));
        assertEquals("S2MS1C", json.read("$.properties['eo:productType']"));
        assertEquals("A", json.read("$.properties['eo:platformSerialIdentifier']"));
        assertEquals(Integer.valueOf(2), json.read("$.properties['eo:instrument'].length()"));
        assertEquals("OLI", json.read("$.properties['eo:instrument'][0]"));
        assertEquals("TIRS", json.read("$.properties['eo:instrument'][1]"));
        assertEquals("2012-04-23T18:25:43.511Z", json.read("$.properties['timeStart']"));
        assertEquals(
                "[\"eo:identifier\",\"eo:productType\",\"eo:platform\",\"eo:platformSerialIdentifier\",\"eo:instrument\",\"eo:sensorType\",\"eo:compositeType\"]",
                json.read("$.properties['queryables']").toString());
        // test the JSON field
        assertEquals(
                "https://geoserver.org/stac-examples/test123.xml",
                json.read("$.properties['assets'].metadata_iso_19139.href"));
        assertEquals(
                "ISO 19139 metadata", json.read("$.properties['assets'].metadata_iso_19139.title"));

        SimpleFeature sf = GeoJSONReader.parseFeature(json.jsonString());
        ReferencedEnvelope bounds = ReferencedEnvelope.reference(sf.getBounds());
        assertTrue(new Envelope(-180, 180, -90, 90).equals(bounds));
    }
}
