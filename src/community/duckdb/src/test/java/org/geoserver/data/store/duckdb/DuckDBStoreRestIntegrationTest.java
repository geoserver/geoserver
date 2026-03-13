/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.store.duckdb;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DuckDBStoreRestIntegrationTest extends CatalogRESTTestSupport {

    @Test
    public void testPostGetDeleteDuckDBStore() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", getValidDuckdbStoreBody(), "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/sf/datastores/duckdbstore"));

        DataStoreInfo newDataStore = catalog.getDataStoreByName("duckdbstore");
        assertNotNull(newDataStore);
        assertNotNull(newDataStore.getDateCreated());

        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/duckdbstore.xml");
        assertXpathEvaluatesTo("true", "/dataStore/enabled", dom);

        response = deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/duckdbstore");
        assertEquals(200, response.getStatus());
        assertNull(catalog.getDataStoreByName("duckdbstore"));
    }

    @Test
    public void testPostPutDeleteDuckDBStore() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", getValidDuckdbStoreBody(), "text/xml");
        assertEquals(201, response.getStatus());
        assertTrue(catalog.getDataStoreByName("duckdbstore").isEnabled());

        String xml = "<dataStore>" + "<name>duckdbstore</name>" + "<enabled>false</enabled>" + "</dataStore>";
        response = putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/duckdbstore", xml, "text/xml");
        assertEquals(200, response.getStatus());
        assertFalse(catalog.getDataStoreByName("duckdbstore").isEnabled());
        assertNotNull(catalog.getDataStoreByName("sf", "duckdbstore").getDateModified());

        response = deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/duckdbstore");
        assertEquals(200, response.getStatus());
        assertNull(catalog.getDataStoreByName("duckdbstore"));
    }

    @Test
    public void testPostDuckDBStoreFailsValidationOnConnectWhenStorageConfigurationIsInvalid() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", getInvalidDuckdbStoreBody(), "text/xml");
        assertEquals(201, response.getStatus());
        DataStoreInfo storeInfo = catalog.getDataStoreByName("duckdbinvalid");
        assertNotNull(storeInfo);
        try {
            storeInfo.getDataStore(null);
            fail("Expected datastore initialization to fail for invalid DuckDB storage configuration");
        } catch (IOException e) {
            // expected: invalid DuckDB storage parameters are rejected when the datastore is initialized
            assertNotNull(e.getMessage());
        } finally {
            deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/duckdbinvalid");
        }
    }

    private String getValidDuckdbStoreBody() {
        return "<dataStore>"
                + "<name>duckdbstore</name>"
                + "<connectionParameters>"
                + "<namespace><string>sf</string></namespace>"
                + "<memory><string>true</string></memory>"
                + "<read_only><string>false</string></read_only>"
                + "<dbtype><string>duckdb</string></dbtype>"
                + "</connectionParameters>"
                + "<workspace>sf</workspace>"
                + "</dataStore>";
    }

    private String getInvalidDuckdbStoreBody() {
        return "<dataStore>"
                + "<name>duckdbinvalid</name>"
                + "<connectionParameters>"
                + "<namespace><string>sf</string></namespace>"
                + "<memory><string>false</string></memory>"
                + "<dbtype><string>duckdb</string></dbtype>"
                + "</connectionParameters>"
                + "<workspace>sf</workspace>"
                + "</dataStore>";
    }
}
