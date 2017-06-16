/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class CollectionControllerTest extends OSEOTestSupport {

    @Before
    public void loginAdmin() {
        login("admin", "geoserver", GeoServerRole.ADMIN_ROLE.getAuthority());
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            System.out.println(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertEquals("application/json", response.getContentType());
        return JsonPath.parse(response.getContentAsString());
    }

    @Test
    public void testGetCollections() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections", 200);
        assertEquals(3, json.read("$.collections.*", List.class).size());
        // check the first (sorted alphabetically)
        assertEquals("LANDSAT8", json.read("$.collections[0].name"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/LANDSAT8", json.read("$.collections[0].href"));
        assertEquals("http://localhost:8080/geoserver/oseo/description?parentId=LANDSAT8", json.read("$.collections[0].search"));
    }
    
    @Test
    public void testGetCollectionsPaging() throws Exception {
        DocumentContext json = getAsJSONPath("/rest/oseo/collections?offset=1&limit=1", 200);
        assertEquals(1, json.read("$.collections.*", List.class).size());
        assertEquals("SENTINEL1", json.read("$.collections[0].name"));
        assertEquals("http://localhost:8080/geoserver/rest/oseo/collections/SENTINEL1", json.read("$.collections[0].href"));
        assertEquals("http://localhost:8080/geoserver/oseo/description?parentId=SENTINEL1", json.read("$.collections[0].search"));
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
}
