/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.opensearch.eo.response.GeoJSONSearchResponse;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Resource;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class GeoJSONSearchTest extends OSEOTestSupport {

    private static final String ENCODED_GEOJSON =
            ResponseUtils.urlEncode(GeoJSONSearchResponse.MIME);

    @Test
    public void testTemplatesCopy() throws Exception {
        GeoServerDataDirectory dd = getDataDirectory();
        Resource templates = dd.get("templates/os-eo/");
        assertEquals(Resource.Type.RESOURCE, templates.get("collections.json").getType());
        assertEquals(Resource.Type.RESOURCE, templates.get("products.json").getType());
    }

    @Test
    public void testSearchCollections() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("oseo/search?httpAccept=" + ENCODED_GEOJSON);
        print(json);

        // top level properties
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?httpAccept=application%2Fgeo%2Bjson",
                json.get("id"));
        assertEquals(5, json.get("totalResults"));
        assertEquals(10, json.get("itemsPerPage"));
        assertEquals(1, json.get("startIndex"));
        JSONObject request = json.getJSONObject("queries").getJSONObject("request");
        assertTrue(request.isEmpty());

        // response properties
        JSONObject properties = json.getJSONObject("properties");
        assertEquals("en", properties.getString("lang"));
        assertNotNull(properties.getString("updated")); // mandatory
        assertEquals("OSEO", properties.getString("creator"));
        assertEquals("OSEO - Search Response", properties.getString("title"));
        JSONObject author = properties.getJSONArray("authors").getJSONObject(0);
        assertEquals("GeoServer", author.getString("name"));
        assertEquals("andrea@geoserver.org", author.getString("email"));
        assertEquals("Agent", author.getString("type"));
        JSONObject links = properties.getJSONObject("links");
        assertLink(links, "profiles", GeoJSONSearchResponse.OSEO_GEOJSON_PROFILE, null, null);
        assertPagingLink(links, "first", 1, 10);
        assertFalse(links.has("previous"));
        assertFalse(links.has("previous"));
        assertPagingLink(links, "last", 1, 10);

        // check features
        JSONArray features = json.getJSONArray("features");
        assertEquals(5, features.size());
        String featureId = "SENTINEL2";
        JSONObject sample = getFeature(features, featureId);
        assertNotNull(sample);
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?uid="
                        + featureId
                        + "&httpAccept=application%2Fgeo%2Bjson",
                sample.getString("id"));
        assertEquals("Feature", sample.getString("type"));
        JSONObject sp = sample.getJSONObject("properties");

        assertEquals("The Sentinel-2 mission", sp.getString("title"));
        assertEquals(featureId, sp.getString("identifier"));
        assertEquals("2015-07-01T08:20:21Z/2016-02-26T09:20:21Z", sp.getString("date"));
        JSONObject acquisition = sp.getJSONObject("acquisitionInformation");
        JSONObject platform = acquisition.getJSONObject("platform");
        assertEquals("http://localhost:8080/geoserver/oseo/id/Sentinel-2", platform.get("id"));
        assertEquals("A", platform.get("platformSerialIdentifier"));
        assertEquals("Sentinel-2", platform.get("platformShortName"));
        assertEquals("LEO", platform.get("orbitType"));
        JSONObject instrument = acquisition.getJSONObject("instrument");
        assertEquals("MSI", instrument.get("instrumentShortName"));
        assertEquals("OPTICAL", instrument.get("sensorType"));
        JSONObject product = sp.getJSONObject("productInformation");
        assertEquals("S2MSI1C", product.get("productType"));
        assertEquals("Level-1C", product.get("processingLevel"));
        // TODO: check links
    }

    @Test
    public void testAllSentinel2ProductsFirstPage() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("oseo/search?parentId=SENTINEL2&httpAccept=" + ENCODED_GEOJSON);
        print(json);

        // top level properties
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?parentId=SENTINEL2&httpAccept=application%2Fgeo%2Bjson",
                json.get("id"));
        assertEquals(19, json.get("totalResults"));
        assertEquals(10, json.get("itemsPerPage"));
        assertEquals(1, json.get("startIndex"));
        JSONObject request = json.getJSONObject("queries").getJSONObject("request");
        assertEquals("SENTINEL2", request.getString("eo:parentIdentifier"));

        // response properties
        JSONObject properties = json.getJSONObject("properties");
        assertEquals("en", properties.getString("lang"));
        assertNotNull(properties.getString("updated")); // mandatory
        assertEquals("OSEO", properties.getString("creator"));
        assertEquals("OSEO - Search Response", properties.getString("title"));
        JSONObject author = properties.getJSONArray("authors").getJSONObject(0);
        assertEquals("GeoServer", author.getString("name"));
        assertEquals("andrea@geoserver.org", author.getString("email"));
        assertEquals("Agent", author.getString("type"));
        JSONObject links = properties.getJSONObject("links");
        assertLink(links, "profiles", GeoJSONSearchResponse.OSEO_GEOJSON_PROFILE, null, null);
        assertPagingLink(links, "first", 1, 10);
        assertFalse(links.has("previous"));
        assertPagingLink(links, "next", 11, 10);
        assertPagingLink(links, "last", 11, 10);

        // check features
        JSONArray features = json.getJSONArray("features");
        assertEquals(10, features.size());
        String featureId = "S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04";
        JSONObject sample = getFeature(features, featureId);
        assertNotNull(sample);
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?parentIdentifier=SENTINEL2&uid="
                        + featureId
                        + "&httpAccept=application%2Fgeo%2Bjson",
                sample.getString("id"));
        assertEquals("Feature", sample.getString("type"));
        JSONObject sp = sample.getJSONObject("properties");
        assertEquals("ARCHIVED", sp.getString("status"));
        assertEquals(featureId, sp.getString("identifier"));
        assertEquals("SENTINEL2", sp.getString("parentIdentifier"));
        assertEquals("2016-09-29T08:20:22.026Z/2016-09-29T08:23:44.107Z", sp.getString("date"));
        assertEquals("2016-09-29T16:59:02.000+00:00", sp.getString("created"));
    }

    private JSONObject getFeature(JSONArray features, String id) {
        for (int i = 0; i < features.size(); i++) {
            JSONObject f = features.getJSONObject(i);
            JSONObject props = f.getJSONObject("properties");
            if (id.equals(props.getString("identifier"))) return f;
        }
        return null;
    }

    @Test
    public void testAllSentinel2ProductsSecondPage() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                "oseo/search?parentId=SENTINEL2&httpAccept="
                                        + ENCODED_GEOJSON
                                        + "&startIndex=11");
        print(json);

        // top level properties
        assertEquals(
                "http://localhost:8080/geoserver/oseo/search?parentId=SENTINEL2&httpAccept=application%2Fgeo%2Bjson&startIndex=11",
                json.get("id"));
        assertEquals(19, json.get("totalResults"));
        assertEquals(10, json.get("itemsPerPage"));
        assertEquals(11, json.get("startIndex"));
        JSONObject request = json.getJSONObject("queries").getJSONObject("request");
        assertEquals("SENTINEL2", request.getString("eo:parentIdentifier"));

        // response properties
        JSONObject properties = json.getJSONObject("properties");
        assertEquals("en", properties.getString("lang"));
        assertNotNull(properties.getString("updated")); // mandatory
        assertEquals("OSEO", properties.getString("creator"));
        assertEquals("OSEO - Search Response", properties.getString("title"));
        JSONObject author = properties.getJSONArray("authors").getJSONObject(0);
        assertEquals("GeoServer", author.getString("name"));
        assertEquals("andrea@geoserver.org", author.getString("email"));
        assertEquals("Agent", author.getString("type"));
        JSONObject links = properties.getJSONObject("links");
        assertLink(links, "profiles", GeoJSONSearchResponse.OSEO_GEOJSON_PROFILE, null, null);
        assertPagingLink(links, "first", 1, 10);
        assertPagingLink(links, "previous", 1, 10);
        assertFalse(links.has("next"));
        assertPagingLink(links, "last", 11, 10);

        // check features
        JSONArray features = json.getJSONArray("features");
    }

    private void assertLink(JSONObject links, String name, String href, String type, String title) {
        JSONArray array = links.getJSONArray(name);
        assertEquals(1, array.size());
        JSONObject link = array.getJSONObject(0);
        assertEquals(href, link.getString("href"));
        if (type != null) assertEquals(type, link.getString("type"));
        else assertFalse(link.has("type"));
        if (title != null) assertEquals(title, link.getString("title"));
        else assertFalse(link.has("title"));
    }

    private void assertPagingLink(JSONObject links, String name, int startIndex, int count) {
        JSONArray array = links.getJSONArray(name);
        assertEquals(1, array.size());
        JSONObject link = array.getJSONObject(0);
        assertEquals(GeoJSONSearchResponse.MIME, link.getString("type"));
        String href = link.getString("href");
        assertThat(href, CoreMatchers.containsString("startIndex=" + startIndex));
        assertThat(href, CoreMatchers.containsString("count=" + count));
    }
}
