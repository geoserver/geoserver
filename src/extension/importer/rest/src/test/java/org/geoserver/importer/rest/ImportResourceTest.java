/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportStore;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.MemoryImportStore;
import org.geoserver.importer.SpatialFile;
import org.geotools.data.h2.H2DataStoreFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class ImportResourceTest extends ImporterTestSupport {

    @Before
    public void prepareData() throws Exception {
        // prepare the contexts used in thsi test
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        importer.createContext(new Directory(dir));
        
        dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        importer.createContext(new Directory(dir));
        
        dir = unpack("shape/archsites_no_crs.zip");
        importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));
    }
    
    @After
    public void cleanupData() throws Exception {
        // remove the cookbook store if any
        removeStore(null, "cookbook");
    }

    @Test
    public void testGetAllImports() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports?all");
        assertNotNull(json.get("imports"));

        JSONArray imports = (JSONArray) json.get("imports");
        assertEquals(3, imports.size());

        JSONObject imprt = imports.getJSONObject(0);
        assertEquals(0, imprt.getInt("id"));
        assertTrue(imprt.getString("href").endsWith("/imports/0"));

        imprt = imports.getJSONObject(1);
        assertEquals(1, imprt.getInt("id"));
        assertTrue(imprt.getString("href").endsWith("/imports/1"));
        
        imprt = imports.getJSONObject(2);
        assertEquals(2, imprt.getInt("id"));
        assertTrue(imprt.getString("href").endsWith("/imports/2"));
    }
    
    @Test
    public void testGetNonExistantImport() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse(("/rest/imports/9999"));
        
        assertEquals(404, resp.getStatusCode());
    }

    @Test
    public void testGetImport() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0");
        assertNotNull(json.get("import"));
        
        JSONObject imprt = json.optJSONObject("import");
        assertEquals(0, imprt.getInt("id"));
        
        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(2, tasks.size());

        assertEquals("READY", tasks.getJSONObject(0).get("state"));
        assertEquals("READY", tasks.getJSONObject(1).get("state"));
    }
    
    @Test
    public void testGetImportExpandChildren() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0?expand=2");

        JSONObject source = json.getJSONObject("import").getJSONObject("data");
        assertEquals("directory", source.getString("type"));
        assertEquals("Shapefile", source.getString("format"));
        
        ImportContext context = importer.getContext(0);
        assertEquals(((Directory)context.getData()).getFile().getPath(), 
            source.getString("location"));
        
        JSONArray files = source.getJSONArray("files");
        assertEquals(2, files.size());
        
        JSONArray tasks = json.getJSONObject("import").getJSONArray("tasks");
        assertEquals(2, tasks.size());
        
        JSONObject t = tasks.getJSONObject(0);
        assertEquals("READY", t.getString("state"));
        
        t = tasks.getJSONObject(1);
        assertEquals("READY", t.getString("state"));
    }
    
    @Test
    public void testGetImport2() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/1?expand=3");
        assertNotNull(json.get("import"));
        
        JSONObject imprt = json.optJSONObject("import");
        assertEquals(1, imprt.getInt("id"));
        
        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(1, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals("READY", task.get("state"));

        JSONObject source = imprt.getJSONArray("tasks").getJSONObject(0).getJSONObject("data");
        assertEquals("file", source.getString("type"));
        assertEquals("GeoTIFF", source.getString("format"));
        
        ImportContext context = importer.getContext(1);
        assertEquals(((SpatialFile)context.getTasks().get(0).getData()).getFile().getParentFile().getPath(), 
            source.getString("location"));

        assertEquals("EmissiveCampania.tif", source.getString("file"));
    }

    @Test
    public void testGetImport3() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/2?expand=2");
        assertNotNull(json.get("import"));
        
        JSONObject imprt = json.optJSONObject("import");
        assertEquals(2, imprt.getInt("id"));
        
        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(1, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals("NO_CRS", task.get("state"));

        JSONObject source = task.getJSONObject("data");
        assertEquals("file", source.getString("type"));
        assertEquals("Shapefile", source.getString("format"));
        assertEquals("archsites.shp", source.getString("file"));
    }

    @Test
    public void testGetImportDatabase() throws Exception {
        File dir = unpack("h2/cookbook.zip");

        Map params = new HashMap();
        params.put(H2DataStoreFactory.DBTYPE.key, "h2");
        params.put(H2DataStoreFactory.DATABASE.key, new File(dir, "cookbook").getAbsolutePath());
        importer.createContext(new Database(params));

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/3?expand=2");
        
        assertNotNull(json.get("import"));

        JSONObject source = json.getJSONObject("import").getJSONObject("data");
        assertEquals("database", source.getString("type"));
        assertEquals("H2", source.getString("format"));

        JSONArray tables = source.getJSONArray("tables");
        assertTrue(tables.contains("point"));
        assertTrue(tables.contains("line"));
        assertTrue(tables.contains("polygon"));
    }

    @Test
    public void testPost() throws Exception {
        
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        int id = lastId();
        assertTrue( resp.getHeader("Location").endsWith( "/imports/"+ id));

        JSONObject json = (JSONObject) json(resp);
        JSONObject imprt = json.getJSONObject("import");

        assertEquals(ImportContext.State.PENDING.name(), imprt.get("state"));
        assertEquals(id, imprt.getInt("id"));
    }
    
    @Test
    public void testPutWithId() throws Exception {
        // propose a new import id
        MockHttpServletResponse resp = putAsServletResponse("/rest/imports/8675309");
        assertEquals(201, resp.getStatusCode());

        resp = getAsServletResponse("/rest/imports/8675309");
        assertEquals(200, resp.getStatusCode());
        JSONObject json = (JSONObject) json(resp);
        JSONObject imprt = json.getJSONObject("import");
        
        assertEquals(8675309, imprt.getInt("id"));
        
        // now propose a new one that is less than the earlier
        resp = putAsServletResponse("/rest/imports/8675000");
        assertEquals(201, resp.getStatusCode());
        // it should be one more than the latest
        assertTrue(resp.getHeader("Location").endsWith("/rest/imports/8675310"));
        
        // and just make sure the other parts work
        resp = getAsServletResponse("/rest/imports/8675310");
        assertEquals(200, resp.getStatusCode());
        json = (JSONObject) json(resp);
        imprt = json.getJSONObject("import");
        assertEquals(8675310, imprt.getInt("id"));

        // now a normal request - make sure it continues the sequence
        resp = postAsServletResponse("/rest/imports/", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );
        assertTrue(resp.getHeader("Location").endsWith( "/imports/8675311"));
    }

    @Test
    public void testPostWithTarget() throws Exception {
        createH2DataStore("sf", "skunkworks");

        String json = 
            "{" + 
                "\"import\": { " + 
                    "\"targetWorkspace\": {" +
                       "\"workspace\": {" + 
                           "\"name\": \"sf\"" + 
                       "}" + 
                    "}," +
                    "\"targetStore\": {" +
                        "\"dataStore\": {" + 
                            "\"name\": \"skunkworks\"" + 
                        "}" + 
                     "}" +
                "}" + 
            "}";
        
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", json, "application/json");
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        int id = lastId();
        assertTrue( resp.getHeader("Location").endsWith( "/imports/"+ id));

        ImportContext ctx = importer.getContext(id);
        assertNotNull(ctx);
        assertNotNull(ctx.getTargetWorkspace());
        assertEquals("sf", ctx.getTargetWorkspace().getName());
        assertNotNull(ctx.getTargetStore());
        assertEquals("skunkworks", ctx.getTargetStore().getName());
    }

    private MockHttpServletResponse postAsServletResponseNoContentType(String path, String body) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setBodyContent(body);
        return dispatch(request);
    }

    @Test
    public void testPostNoMediaType() throws Exception {
        MockHttpServletResponse resp = postAsServletResponseNoContentType("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
    }

    @Test
    public void testImportSessionIdNotInt() throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports/foo", "");
        assertEquals(404, resp.getStatusCode());
    }

    @Test
    public void testContentNegotiation() throws Exception {
        MockHttpServletResponse res = getAsServletResponse("/rest/imports/0");
        assertEquals("application/json", res.getContentType());

        MockHttpServletRequest req = createRequest("/rest/imports/0");
        req.setMethod("GET");
        req.setHeader("Accept", "text/html");

        res = dispatch(req);
        assertEquals("text/html", res.getContentType());
    }

    @Test
    public void testGetImportGeoJSON() throws Exception {
        File dir = unpack("geojson/point.json.zip");
        importer.createContext(new SpatialFile(new File(dir, "point.json")));
        int id = lastId();

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/" + id + "?expand=3");
        assertNotNull(json);

        JSONObject imprt = json.optJSONObject("import");
        assertEquals(id, imprt.getInt("id"));

        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(1, tasks.size());

        JSONObject task = tasks.getJSONObject(0);

        JSONObject source = task.getJSONObject("data");
        assertEquals("file", source.getString("type"));
        assertEquals("GeoJSON", source.getString("format"));
        assertEquals("point.json", source.getString("file"));

        JSONObject layer = task.getJSONObject("layer");
        JSONArray attributes = layer.getJSONArray("attributes");
        assertNotEquals(0, attributes.size());

        assertTrue(layer.containsKey("bbox"));
    }
}
