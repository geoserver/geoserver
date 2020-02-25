/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;

import java.io.File;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImporterTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Created by vickdw on 3/30/17. */
public class ImportDataControllerTest extends ImporterTestSupport {
    @Before
    public void prepareData() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        importer.createContext(new Directory(dir));
    }

    @Test
    public void testGet() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(ROOT_PATH + "/imports/0/data", 200);
        assertEquals("directory", json.getString("type"));
        assertEquals(2, json.getJSONArray("files").size());
    }

    @Test
    public void testGetFiles() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(ROOT_PATH + "/imports/0/data/files", 200);
        assertEquals(2, json.getJSONArray("files").size());
    }

    @Test
    public void testGetFile() throws Exception {
        JSONObject json =
                (JSONObject) getAsJSON(ROOT_PATH + "/imports/0/data/files/archsites.shp", 200);
        // System.out.println(json);
        assertEquals("archsites.shp", json.getString("file"));
        assertEquals("archsites.prj", json.getString("prj"));
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(ROOT_PATH + "/imports/0/data/files/archsites.shp");
        assertEquals(200, response.getStatus());

        response = deleteAsServletResponse(ROOT_PATH + "/imports/0/data/files/archsites.shp");
        assertEquals(204, response.getStatus());

        response = getAsServletResponse(ROOT_PATH + "/imports/0/data/files/archsites.shp");
        assertEquals(404, response.getStatus());

        JSONArray arr =
                ((JSONObject) getAsJSON(ROOT_PATH + "/imports/0/data/files")).getJSONArray("files");
        assertEquals(1, arr.size());
    }
}
