/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.data.DataStore;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DataStoreControllerTest extends CatalogRESTTestSupport {

    @Before
    public void addDataStores() throws IOException {
        removeStore("sf", "newDataStore"); // may have been created by other tests
        // the store configuration gets ruined by tests in more than one way, let's recreate it
        removeStore("sf", "sf");
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, catalog);
        getTestData().addVectorLayer(SystemTestData.AGGREGATEGEOFEATURE, catalog);
        getTestData().addVectorLayer(SystemTestData.GENERICENTITY, catalog);
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores.xml");
        assertEquals(
                catalog.getStoresByWorkspace("sf", DataStoreInfo.class).size(),
                dom.getElementsByTagName("dataStore").getLength());
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(ROOT_PATH + "/workspaces/sf/datastores/sf.json");

        JSONObject dataStore = ((JSONObject) json).getJSONObject("dataStore");
        assertNotNull(dataStore);

        assertEquals("sf", dataStore.get("name"));
        assertEquals("sf", dataStore.getJSONObject("workspace").get("name"));
        assertNotNull(dataStore.get("connectionParameters"));
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON(ROOT_PATH + "/workspaces/sf/datastores.json");
        assertTrue(json instanceof JSONObject);

        Object datastores = ((JSONObject) json).getJSONObject("dataStores").get("dataStore");
        assertNotNull(datastores);

        if (datastores instanceof JSONArray) {
            assertEquals(
                    catalog.getDataStoresByWorkspace("sf").size(), ((JSONArray) datastores).size());
        } else {
            assertEquals(1, catalog.getDataStoresByWorkspace("sf").size());
        }
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores.html");
        List<DataStoreInfo> datastores = catalog.getDataStoresByWorkspace("sf");

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(datastores.size(), links.getLength());

        for (int i = 0; i < datastores.size(); i++) {
            DataStoreInfo ds = datastores.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(ds.getName() + ".html"));
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405, putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores").getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405, deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores").getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        print(dom);
        assertEquals("dataStore", dom.getDocumentElement().getNodeName());
        assertEquals("sf", xp.evaluate("/dataStore/name", dom));
        assertEquals("sf", xp.evaluate("/dataStore/workspace/name", dom));
        assertXpathExists("/dataStore/connectionParameters", dom);
        assertThat(
                xp.evaluate("/dataStore/featureTypes/atom:link/@href", dom),
                endsWith(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/datastores/sf/featuretypes.xml"));
    }

    @Test
    public void testRoundTripGetAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        assertEquals("dataStore", dom.getDocumentElement().getNodeName());
        assertEquals("sf", xp.evaluate("/dataStore/name", dom));
        assertEquals("sf", xp.evaluate("/dataStore/workspace/name", dom));
        assertXpathExists("/dataStore/connectionParameters", dom);

        Document dom2 = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("true", "/dataStore/enabled", dom);

        String xml =
                "<dataStore>" + "<name>sf</name>" + "<enabled>false</enabled>" + "</dataStore>";

        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals(200, response.getStatus());

        dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("false", "/dataStore/enabled", dom);

        assertFalse(catalog.getDataStoreByName("sf", "sf").isEnabled());
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.html");

        DataStoreInfo ds = catalog.getDataStoreByName("sf");
        List<FeatureTypeInfo> featureTypes = catalog.getFeatureTypesByDataStore(ds);

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(featureTypes.size(), links.getLength());

        for (int i = 0; i < featureTypes.size(); i++) {
            FeatureTypeInfo ft = featureTypes.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(ft.getName() + ".html"));
        }
    }

    @Test
    public void testGetWrongDataStore() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String ds = "sfssssss";
        // Request path
        String requestPath = ROOT_PATH + "/workspaces/" + ws + "/datastores/" + ds + ".html";
        // Exception path
        String exception = "No such datastore: " + ws + "," + ds;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    File setupNewDataStore() throws Exception {
        return setupNewDataStore("newDataStore");
    }

    File setupNewDataStore(String name) throws Exception {
        Properties props = new Properties();
        props.put("_", "name:StringpointProperty:Point");
        props.put("NewDataStore.0", "'zero'|POINT(0 0)");
        props.put("NewDataStore.1", "'one'|POINT(1 1)");

        File dir = new File("./target/nds/" + name);
        dir.mkdirs();

        File file = new File(dir, name + ".properties");
        file.deleteOnExit();
        dir.deleteOnExit();

        props.store(new FileOutputStream(file), null);
        return dir;
    }

    @Test
    public void testPostAsXML() throws Exception {

        File dir = setupNewDataStore();
        String xml =
                "<dataStore>"
                        + "<name>newDataStore</name>"
                        + "<connectionParameters>"
                        + "<namespace><string>sf</string></namespace>"
                        + "<directory>"
                        + "<string>"
                        + dir.getAbsolutePath()
                        + "</string>"
                        + "</directory>"
                        + "</connectionParameters>"
                        + "<workspace>sf</workspace>"
                        + "</dataStore>";
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location").endsWith("/workspaces/sf/datastores/newDataStore"));

        DataStoreInfo newDataStore = catalog.getDataStoreByName("newDataStore");
        assertNotNull(newDataStore);
        assertNotNull(newDataStore.getDateCreated());

        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);
    }

    @Test
    public void testPostAsXMLNoWorkspace() throws Exception {
        File dir = setupNewDataStore();
        String xml =
                "<dataStore>"
                        + "<name>newDataStore</name>"
                        + "<connectionParameters>"
                        + "<namespace><string>sf</string></namespace>"
                        + "<directory>"
                        + "<string>"
                        + dir.getAbsolutePath()
                        + "</string>"
                        + "</directory>"
                        + "</connectionParameters>"
                        + "</dataStore>";
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location").endsWith("/workspaces/sf/datastores/newDataStore"));

        DataStoreInfo newDataStore = catalog.getDataStoreByName("newDataStore");
        assertNotNull(newDataStore);
        assertNotNull(newDataStore.getDateCreated());

        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeStore("sf", "newDataStore");
        File dir = setupNewDataStore();
        String json =
                "{'dataStore':{"
                        + "'connectionParameters': {"
                        + "'namespace': {'string':'sf'},"
                        + "'directory': {'string':'"
                        + dir.getAbsolutePath().replace('\\', '/')
                        + "'}"
                        + "},"
                        + "'workspace':'sf',"
                        + "'name':'newDataStore',"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", json, "text/json");

        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location").endsWith("/workspaces/sf/datastores/newDataStore"));

        DataStoreInfo newDataStore = catalog.getDataStoreByName("newDataStore");
        assertNotNull(newDataStore);
        assertNotNull(newDataStore.getDateCreated());

        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml =
                "<dataStore>" + "<name>sf</name>" + "<enabled>false</enabled>" + "</dataStore>";

        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPut() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("true", "/dataStore/enabled", dom);

        String xml =
                "<dataStore>" + "<name>sf</name>" + "<enabled>false</enabled>" + "</dataStore>";

        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals(200, response.getStatus());

        dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("false", "/dataStore/enabled", dom);

        assertFalse(catalog.getDataStoreByName("sf", "sf").isEnabled());
        assertNotNull(catalog.getDataStoreByName("sf", "sf").getDateModified());
    }

    @Test
    public void testPut2() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("2", "count(//dataStore/connectionParameters/*)", dom);

        String xml =
                "<dataStore>"
                        + "<name>sf</name>"
                        + "<connectionParameters>"
                        + "<one>"
                        + "<string>1</string>"
                        + "</one>"
                        + "<two>"
                        + "<string>2</string>"
                        + "</two>"
                        + "</connectionParameters>"
                        + "</dataStore>";

        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals(200, response.getStatus());

        DataStoreInfo ds = catalog.getDataStoreByName("sf", "sf");
        assertEquals(2, ds.getConnectionParameters().size());
        assertTrue(ds.getConnectionParameters().containsKey("one"));
        assertTrue(ds.getConnectionParameters().containsKey("two"));
        assertNotNull(ds.getDateModified());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        DataStoreInfo ds = catalog.getDataStoreByName("sf", "sf");
        assertTrue(ds.isEnabled());

        String xml = "<dataStore>" + "<name>sf</name>" + "</dataStore>";

        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals(200, response.getStatus());

        assertTrue(ds.isEnabled());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<dataStore>" + "<name>changed</name>" + "</dataStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/sf/datastores/nonExistant", xml, "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/nonExistant")
                        .getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        removeStore("sf", "newDataStore");
        File dir = setupNewDataStore();
        String xml =
                "<dataStore>"
                        + "<name>newDataStore</name>"
                        + "<connectionParameters>"
                        + "<entry>"
                        + "<string>namespace</string>"
                        + "<string>sf</string>"
                        + "</entry>"
                        + "<entry>"
                        + "<string>directory</string>"
                        + "<string>"
                        + dir.getAbsolutePath()
                        + "</string>"
                        + "</entry>"
                        + "</connectionParameters>"
                        + "<workspace>sf</workspace>"
                        + "</dataStore>";
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(catalog.getDataStoreByName("sf", "newDataStore"));

        assertEquals(
                200,
                deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/newDataStore")
                        .getStatus());
        assertNull(catalog.getDataStoreByName("sf", "newDataStore"));
    }

    @Test
    public void testDeleteNonEmptyForbidden() throws Exception {
        assertEquals(
                403,
                deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf").getStatus());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getDataStoreByName("sf", "sf"));
        MockHttpServletResponse response =
                deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf?recurse=true");
        assertEquals(200, response.getStatus());

        assertNull(catalog.getDataStoreByName("sf", "sf"));

        for (FeatureTypeInfo ft : catalog.getFeatureTypes()) {
            if (ft.getStore().getName().equals("sf")) {
                fail();
            }
        }
    }

    @Test // GEOS-9189
    public void testDeleteNonEmptyNonRecursiveNonUniqueStoreOnWorkspace() throws Exception {
        // create two additional stores besides "sf" on workspace "sf", one that would
        // be sorted before and one after "sf". Failure is order dependent, if the store
        // to be deleted is the first one returned by catalog.getStoresByWorkspace(...)
        // then no failure occurs
        String store1 = "aa_sf";
        String store2 = "zz_sf";
        createDataStore("sf", store1);
        createDataStore("sf", store2);
        try {
            List<DataStoreInfo> dataStoresByWorkspace = catalog.getDataStoresByWorkspace("sf");
            assertEquals(3, dataStoresByWorkspace.size());

            assertEquals(
                    200,
                    deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/" + store1)
                            .getStatus());
            assertEquals(
                    200,
                    deleteAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/" + store2)
                            .getStatus());
            assertNull(catalog.getDataStoreByName("sf", store1));
            assertNull(catalog.getDataStoreByName("sf", store2));
        } finally {
            removeStore("sf", store1);
            removeStore("sf", store2);
        }
    }

    private void createDataStore(String workspace, String name) throws Exception {
        removeStore(workspace, name);
        File dir = setupNewDataStore(name);
        String xml =
                "<dataStore>"
                        + "<name>"
                        + name
                        + "</name>"
                        + "<connectionParameters>"
                        + "<entry>"
                        + "<string>namespace</string>"
                        + "<string>"
                        + workspace
                        + "</string>"
                        + "</entry>"
                        + "<entry>"
                        + "<string>directory</string>"
                        + "<string>"
                        + dir.getAbsolutePath()
                        + "</string>"
                        + "</entry>"
                        + "</connectionParameters>"
                        + "<workspace>"
                        + workspace
                        + "</workspace>"
                        + "</dataStore>";
        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores", xml, "text/xml");
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testPutNameChangeForbidden() throws Exception {
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, getCatalog());
        String xml = "<dataStore>" + "<name>newName</name>" + "</dataStore>";
        assertEquals(
                403,
                putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml")
                        .getStatus());
    }

    @Test
    public void testPutWorkspaceChangeForbidden() throws Exception {
        String xml = "<dataStore>" + "<workspace>gs</workspace>" + "</dataStore>";
        assertEquals(
                403,
                putAsServletResponse(ROOT_PATH + "/workspaces/sf/datastores/sf", xml, "text/xml")
                        .getStatus());
    }
}
