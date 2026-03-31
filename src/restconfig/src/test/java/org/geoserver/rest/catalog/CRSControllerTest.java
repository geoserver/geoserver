/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class CRSControllerTest extends GeoServerSystemTestSupport {

    private static final int TEST_CODES_SIZE = 100;

    private static final String WGS84_TEMPLATE =
            "GEOGCS[\"WGS84(DD) - %s\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.25722%s]],"
                    + " PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic latitude\","
                    + " NORTH], AXIS[\"Geodetic longitude\", EAST],  AUTHORITY[\"%s\",\"%s\"]]";

    @Before
    public void resetCustomAuthorities() throws IOException {
        // add authorities
        addAuthorityList(Map.of("AT", "AuthorityTest"));
        addTestAuthorityProperties("AT");

        // make sure custom authorities are reloaded
        getGeoServer().reset();
    }

    private void addAuthorityList(Map<String, String> authorities) throws IOException {
        File root = testData.getDataDirectoryRoot();
        File userProjections = new File(root, "user_projections");
        userProjections.mkdirs();
        File authoritiesFile = new File(userProjections, "authorities.properties");
        try (PrintWriter out = new PrintWriter(new FileWriter(authoritiesFile))) {
            for (Map.Entry<String, String> entry : authorities.entrySet()) {
                out.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    private void addTestAuthorityProperties(String authority) throws IOException {
        File root = testData.getDataDirectoryRoot();
        File userProjections = new File(root, "user_projections");
        File scaProperties = new File(userProjections, authority + ".properties");
        // Create 100 codes with spheroid having different flattening:
        // 298.25722%s
        int start = 3563;
        try (PrintWriter out = new PrintWriter(new FileWriter(scaProperties))) {
            for (int i = 0; i < TEST_CODES_SIZE; i++) {
                int invFlatt = i + start;
                int code = 1000 + i;
                out.println(code + "="
                        + StringEscapeUtils.escapeJava(WGS84_TEMPLATE.formatted(authority, invFlatt, authority, code)));
            }
        }
    }

    @Test
    public void testListCRSsPaged() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/crs.json?offset=10&limit=10");
        Object codes = json.get("crs");
        assertNotNull(codes);

        if (codes instanceof JSONArray crs) {
            assertEquals(10, crs.size());
            JSONObject first = crs.getJSONObject(0);
            assertNotNull(first.getString("id"));
            assertFalse(first.getString("id").isEmpty());
            assertTrue(first.getString("href").contains("/rest/crs/"));
            assertTrue(first.getString("href").endsWith(".wkt"));
        }

        JSONObject page = (JSONObject) json.get("page");

        assertEquals(10, page.getInt("offset"));
        assertEquals(10, page.getInt("limit"));
        assertEquals(10, page.getInt("returned"));
        assertTrue(page.getInt("total") >= 1000);
    }

    @Test
    public void testListCRSsOnlyAuthorityPaged() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/crs.json?limit=100&authority=AT");
        JSONArray codes = (JSONArray) json.get("crs");
        assertNotNull(codes);

        assertEquals(TEST_CODES_SIZE, codes.size());
        for (int i = 0; i < TEST_CODES_SIZE; i++) {
            int code = 1000 + i;
            assertEquals("AT:" + code, codes.getJSONObject(i).getString("id"));
        }

        JSONObject page = (JSONObject) json.get("page");
        assertEquals(0, page.getInt("offset"));
        assertEquals(100, page.getInt("limit"));
        assertEquals(100, page.getInt("returned"));
        assertEquals(100, page.getInt("total"));
    }

    @Test
    public void testListAuthorities() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/crs/authorities");
        Object auths = json.get("authorities");
        assertNotNull(auths);

        if (auths instanceof JSONArray authorities) {
            String[] expected = {"AT", "CRS", "EPSG"};
            assertEquals(expected.length, authorities.size());
            for (int i = 0; i < expected.length; i++) {
                JSONObject jsonObject = authorities.getJSONObject(i);
                assertEquals(expected[i], jsonObject.getString("name"));
                assertTrue(jsonObject.getString("href").contains("/rest/crs?authority=" + expected[i]));
            }
        }
    }

    @Test
    public void testListCRSsPagedSecondPageIsDifferent() throws Exception {
        JSONObject json1 = (JSONObject) getAsJSON("/rest/crs.json?offset=0&limit=10");
        JSONObject json2 = (JSONObject) getAsJSON("/rest/crs.json?offset=10&limit=10");
        String id1 = json1.getJSONArray("crs").getJSONObject(0).getString("id");
        String id2 = json2.getJSONArray("crs").getJSONObject(0).getString("id");
        assertNotEquals(id1, id2);
    }

    @Test
    public void testLookupExistingCRSAsWkt() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/crs/EPSG:4326.wkt");
        String body = response.getContentAsString();
        assertTrue(body.contains("WGS 84") || body.contains("GEOGCS") || body.contains("GEODCRS"));
    }

    @Test
    public void testLookupExistingCRSAsJson() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/crs/EPSG:4326.json");
        String body = json.getString("definition");
        // Keep this loose enough to survive WKT1/WKT2 differences
        assertTrue(body.contains("WGS 84") || body.contains("GEOGCS") || body.contains("GEODCRS"));
        assertEquals("EPSG:4326", json.getString("id"));
        assertEquals("wkt", json.getString("format"));

        String definition = json.getString("definition");
        assertNotNull(definition);
        assertFalse(definition.isEmpty());
    }

    @Test
    public void testLookupMissingCRSReturns404() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/crs/EPSG:303030303.wkt");

        assertEquals(404, response.getStatus());
    }
}
