/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mbtiles;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.data.DataStore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class MBStoreRestIntegrationTest extends CatalogRESTTestSupport {

    @Test
    public void testPostGetDeleteGetMBStore() throws Exception {

        MockHttpServletResponse response =
                postAsServletResponse(
                        ROOT_PATH + "/workspaces/sf/datastores", getMbStoreBody(), "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/sf/datastores/mbStore"));

        DataStoreInfo newDataStore = catalog.getDataStoreByName("mbStore");
        assertNotNull(newDataStore);
        assertNotNull(newDataStore.getDateCreated());
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/mbStore.xml");
        assertXpathEvaluatesTo("true", "/dataStore/enabled", dom);
        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);
        response = deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/mbStore");
        assertEquals(response.getStatus(), 200);
        assertNull(catalog.getDataStoreByName("mbStore"));
    }

    @Test
    public void testPostPutDeleteMBStore() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(
                        ROOT_PATH + "/workspaces/sf/datastores", getMbStoreBody(), "text/xml");
        assertEquals(response.getStatus(), 201);
        assertTrue(catalog.getDataStoreByName("mbStore").isEnabled());
        String xml =
                "<dataStore>"
                        + "<name>mbStore</name>"
                        + "<enabled>false</enabled>"
                        + "</dataStore>";

        response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/sf/datastores/mbStore", xml, "text/xml");
        assertEquals(200, response.getStatus());
        assertFalse(catalog.getDataStoreByName("mbStore").isEnabled());
        assertNotNull(catalog.getDataStoreByName("sf", "mbStore").getDateModified());
        response = deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/mbStore");
        assertEquals(response.getStatus(), 200);
        assertNull(catalog.getDataStoreByName("mbStore"));
    }

    @Test
    public void testPutFileMBStore() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/datastores/mb_store/external.mbtiles");
        assertEquals(404, resp.getStatus());
        URL url = getClass().getResource("madagascar/madagascar.mbtiles");
        String body = url.toExternalForm();
        put(
                ROOT_PATH + "/workspaces/gs/datastores/mb_store/external.mbtiles",
                body,
                "application/vnd.mapbox-vector-tile");
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getDataStoreByName("gs", "mb_store");
        assertNotNull(ds);
        assertEquals(1, cat.getFeatureTypesByDataStore(ds).size());
        resp =
                getAsServletResponse(
                        ROOT_PATH + "/workspaces/gs/datastores/mb_store/external.mbtiles");
        assertEquals(200, resp.getStatus());
        assertEquals("application/zip", resp.getContentType());
        ByteArrayInputStream bin = getBinaryInputStream(resp);
        ZipInputStream zin = new ZipInputStream(bin);

        ZipEntry entry = zin.getNextEntry();
        assertNotNull(entry);
        assertEquals("madagascar.mbtiles", entry.getName());
        removeStore(ds.getWorkspace().getName(), ds.getName());
    }

    private String getMbStoreBody() throws URISyntaxException {
        File mbStore = new File(getClass().getResource("madagascar/madagascar.mbtiles").toURI());
        String xml =
                "<dataStore>"
                        + "<name>mbStore</name>"
                        + "<connectionParameters>"
                        + "<namespace><string>sf</string></namespace>"
                        + "<database>"
                        + "<string>"
                        + mbStore.getAbsolutePath()
                        + "</string>"
                        + "</database>"
                        + "<dbtype><string>mbtiles</string></dbtype>"
                        + "</connectionParameters>"
                        + "<workspace>sf</workspace>"
                        + "</dataStore>";
        return xml;
    }
}
