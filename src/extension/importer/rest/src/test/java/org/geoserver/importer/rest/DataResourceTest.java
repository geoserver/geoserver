/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;

import java.io.File;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.importer.Directory;
import org.geoserver.importer.ImporterTestSupport;
import org.junit.Before;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DataResourceTest extends ImporterTestSupport {

    
    @Before
    public void prepareData() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        importer.createContext(new Directory(dir));
    }
    
    @Test
    public void testGet() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/data");
        assertEquals("directory", json.getString("type"));
        assertEquals(2, json.getJSONArray("files").size());
    }

    @Test
    public void testGetFiles() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/data/files");
        assertEquals(2, json.getJSONArray("files").size());
    }

    @Test
    public void testGetFile() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/data/files/archsites.shp");
        assertEquals("archsites.shp", json.getString("file"));
        assertEquals("archsites.prj", json.getString("prj"));
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse response = 
            getAsServletResponse("/rest/imports/0/data/files/archsites.shp");
        assertEquals(200, response.getStatusCode());

        response = deleteAsServletResponse("/rest/imports/0/data/files/archsites.shp");
        assertEquals(204, response.getStatusCode());
        
        response = getAsServletResponse("/rest/imports/0/data/files/archsites.shp");
        assertEquals(404, response.getStatusCode());

        JSONArray arr = ((JSONObject)getAsJSON("/rest/imports/0/data/files")).getJSONArray("files");
        assertEquals(1, arr.size());
    }

}
