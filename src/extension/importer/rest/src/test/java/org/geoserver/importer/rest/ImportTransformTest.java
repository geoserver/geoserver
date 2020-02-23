/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.rest.RestBaseController;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

public class ImportTransformTest extends ImporterTestSupport {

    DataStoreInfo store;

    private static String BASEPATH = RestBaseController.ROOT_PATH;

    /**
     * Create a test transform context: one import task with two transforms:
     *
     * <p>One ReprojectTransform and one IntegerFieldToDateTransform.
     */
    @Before
    public void setupTransformContext() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        ImportTask importTask = context.getTasks().get(0);
        importTask.getTransform().add(new ReprojectTransform(CRS.decode("EPSG:4326")));
        importTask.getTransform().add(new IntegerFieldToDateTransform("pretendDateIntField"));
        importer.changed(importTask);
    }

    @Test
    public void testGetTransforms() throws Exception {
        int id = lastId();
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms");
        List<JSONObject> txs = parseTransformObjectsFromResponse(j);

        assertEquals(2, txs.size());
        assertEquals("ReprojectTransform", txs.get(0).get("type"));
        assertEquals("IntegerFieldToDateTransform", txs.get(1).get("type"));
    }

    @Test
    public void testGetTransform() throws Exception {
        int id = lastId();
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");

        assertTrue(j instanceof JSONObject);
        assertEquals("ReprojectTransform", ((JSONObject) j).get("type"));
    }

    @Test
    public void testGetTransformsExpandNone() throws Exception {
        int id = lastId();
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms?expand=none");
        List<JSONObject> txs = parseTransformObjectsFromResponse(j);

        assertEquals(2, txs.size());
        assertTrue(txs.get(0).containsKey("href"));
        assertTrue(txs.get(1).containsKey("href"));
    }

    @Test
    public void testPostTransform() throws Exception {
        int id = lastId();
        String json = "{\"type\": \"ReprojectTransform\", \"target\": \"EPSG:3005\"}";
        MockHttpServletResponse resp =
                postAsServletResponse(
                        BASEPATH + "/imports/" + id + "/tasks/0/transforms",
                        json,
                        "application/json");

        String location = resp.getHeader("Location");
        assertEquals(HttpStatus.CREATED.value(), resp.getStatus());

        // Make sure it was created
        ImportTask importTask = importer.getContext(id).getTasks().get(0);
        assertEquals(3, importTask.getTransform().getTransforms().size());
    }

    @Test
    public void testDeleteTransform() throws Exception {
        int id = lastId();
        MockHttpServletResponse resp =
                deleteAsServletResponse(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");
        assertEquals(HttpStatus.OK.value(), resp.getStatus());

        // Make sure it was deleted
        ImportTask importTask = importer.getContext(id).getTasks().get(0);
        assertEquals(1, importTask.getTransform().getTransforms().size());
    }

    @Test
    public void testPutTransform() throws Exception {
        String json = "{\"type\": \"ReprojectTransform\", \"target\": \"EPSG:3005\"}";

        int id = lastId();
        MockHttpServletResponse resp =
                putAsServletResponse(
                        BASEPATH + "/imports/" + id + "/tasks/0/transforms/0",
                        json,
                        "application/json");

        assertEquals(HttpStatus.OK.value(), resp.getStatus());

        // Get it again and make sure it changed.
        JSON j = getAsJSON(BASEPATH + "/imports/" + id + "/tasks/0/transforms/0");
        assertTrue(j instanceof JSONObject);
        assertEquals("EPSG:3005", ((JSONObject) j).get("target"));
    }

    /**
     * Parses the transforms list out of a /transforms response (example below), asserting that the
     * structure and types are as expected.
     *
     * <pre>
     *
     * {
     *     "transformChain": {
     *         "transforms": [
     *             {
     *                 "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/0",
     *                 "source": null,
     *                 "target": "EPSG:4326",
     *                 "type": "ReprojectTransform"
     *             },
     *             {
     *                 "field": "pretendDateIntField",
     *                 "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/1",
     *                 "type": "IntegerFieldToDateTransform"
     *             }
     *         ],
     *         "type": "vector"
     *     }
     * }
     * </pre>
     *
     * For the above example, this will check the structure and types and then return:
     *
     * <pre>
     * [
     *     {
     *         "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/0",
     *         "source": null,
     *         "target": "EPSG:4326",
     *         "type": "ReprojectTransform"
     *     },
     *     {
     *         "field": "pretendDateIntField",
     *         "href": "http://localhost:8080/geoserver/restng/imports/0/tasks/0/transforms/imports/0/tasks/0/transforms/1",
     *         "type": "IntegerFieldToDateTransform"
     *     }
     * ]
     * </pre>
     */
    List<JSONObject> parseTransformObjectsFromResponse(JSON transformsResponse) {
        assertTrue(transformsResponse instanceof JSONObject);
        JSONObject jo = (JSONObject) transformsResponse;
        assertTrue(
                jo.containsKey("transformChain") && jo.get("transformChain") instanceof JSONObject);
        JSONObject tco = (JSONObject) jo.get("transformChain");
        assertTrue(tco.containsKey("transforms") && tco.get("transforms") instanceof JSONArray);
        JSONArray array = (JSONArray) tco.get("transforms");

        List<JSONObject> transformsList = new ArrayList<>();
        for (Object i : array) {
            assertTrue(i instanceof JSONObject);
            transformsList.add((JSONObject) i);
        }

        return transformsList;
    }
}
