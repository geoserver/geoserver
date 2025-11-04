/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.store.geoparquet;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GeoparquetRestIntegrationTest extends CatalogRESTTestSupport {

    private static final String PARQUET_FILE = "sample.parquet";

    @Test
    public void testPutFileGeoparquet() throws Exception {
        byte[] body = getResourceContent();
        MockHttpServletResponse resp =
                getAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore/file.geoparquet");
        assertEquals(404, resp.getStatus());

        put(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore/file.geoparquet", body, "application/octet-stream");
        Catalog cat = getCatalog();
        DataStoreInfo sf = cat.getDataStoreByName("sf", "geoparquetstore");
        assertNotNull(sf);
        assertEquals(1, cat.getFeatureTypesByDataStore(sf).size());

        resp = getAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore/file.geoparquet");
        assertEquals(200, resp.getStatus());
        assertEquals("application/zip", resp.getContentType());
        ByteArrayInputStream bin = getBinaryInputStream(resp);
        ZipInputStream zin = new ZipInputStream(bin);
        ZipEntry entry = zin.getNextEntry();
        assertNotNull(entry);
        assertEquals("geoparquetstore.geoparquet", entry.getName());
        removeStore(sf.getWorkspace().getName(), sf.getName());
    }

    @Test
    public void testPostGetDeleteGeoparquet() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", getGeoParquetStoreBody(), "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/sf/datastores/geoparquetstore"));

        DataStoreInfo newDataStore = catalog.getDataStoreByName("geoparquetstore");
        assertNotNull(newDataStore);
        assertNotNull(newDataStore.getDateCreated());
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore.xml");
        assertXpathEvaluatesTo("true", "/dataStore/enabled", dom);

        response = deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore");
        assertEquals(200, response.getStatus());
        assertNull(catalog.getDataStoreByName("geoparquetStore"));
    }

    @Test
    public void testPostPutDeleteGeoparquet() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", getGeoParquetStoreBody(), "text/xml");
        assertEquals(response.getStatus(), 201);
        assertTrue(catalog.getDataStoreByName("geoparquetstore").isEnabled());
        String xml = "<dataStore>" + "<name>geoparquetstore</name>" + "<enabled>false</enabled>" + "</dataStore>";

        response = putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore", xml, "text/xml");
        assertEquals(200, response.getStatus());
        assertFalse(catalog.getDataStoreByName("geoparquetstore").isEnabled());
        assertNotNull(catalog.getDataStoreByName("sf", "geoparquetstore").getDateModified());
        response = deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/geoparquetstore");
        assertEquals(response.getStatus(), 200);
        assertNull(catalog.getDataStoreByName("geoparquetstore"));
    }

    private byte[] getResourceContent() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(getClass().getResource(PARQUET_FILE).toURI()));
    }

    private String getGeoParquetStoreBody() throws URISyntaxException {
        File file = new File(getClass().getResource(PARQUET_FILE).toURI());
        return "<dataStore>"
                + "<name>geoparquetstore</name>"
                + "<connectionParameters>"
                + "<namespace><string>sf</string></namespace>"
                + "<database>"
                + "<string>"
                + file.getAbsolutePath()
                + "</string>"
                + "</database>"
                + "<dbtype><string>geoparquet</string></dbtype>"
                + "</connectionParameters>"
                + "<workspace>sf</workspace>"
                + "</dataStore>";
    }
}
