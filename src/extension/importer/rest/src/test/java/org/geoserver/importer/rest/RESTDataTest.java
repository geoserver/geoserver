/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImporterTestSupport;
import org.geotools.data.DataStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.restlet.data.MediaType;

import com.google.common.collect.Lists;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class RESTDataTest extends ImporterTestSupport {

    @Test
    public void testSingleFileUpload() throws Exception {
        int i = postNewImport();
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        postImport(i);
        runChecks("archsites");
    }

    @Test
    public void testTitleAndDescriptionOnUpload() throws Exception {
        int i = postNewImport();
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        putTaskLayer(i, t, "{\"title\":\"Archsites\", \"abstract\":\"Archeological Sites\"}");
        postImport(i);
        runChecks("archsites");

        LayerInfo l = getCatalog().getLayerByName("archsites");
        ResourceInfo r = l.getResource();

        assertEquals("Archsites", r.getTitle());
        assertEquals("Archeological Sites", r.getAbstract());
    }

    @Test
    public void testFilePut() throws Exception {
        int i = postNewImport();
        int t1 = putNewTask(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t1);
        assertEquals("READY", task.getString("state"));
        
        postImport(i);
        runChecks("archsites");
    }

    @Test
    public void testMultipleFileUpload() throws Exception {
        int i = postNewImport();
        int t1 = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t1);
        assertEquals("READY", task.getString("state"));

        int t2 = postNewTaskAsMultiPartForm(i, "shape/bugsites_esri_prj.tar.gz");
        task = getTask(i, t2);
        assertEquals("READY", task.getString("state"));
        
        postImport(i);
        runChecks("archsites");
        runChecks("bugsites");
    }

    @Test
    public void testFileUploadWithConfigChange() throws Exception {
        int i = postNewImport();
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_no_crs.zip");

        JSONObject task = getTask(i, t);
        assertEquals("NO_CRS", task.getString("state"));
        
        String json = 
        "{" +
          "\"task\": {" +
            "\"layer\": {" +
                    "\"srs\": \"EPSG:4326\"" + 
             "}" +
           "}" + 
        "}";
        putTask(i, t, json);

        task = getTask(i, t);
        assertEquals("READY", task.getString("state"));
        assertEquals("gs_archsites", task.getJSONObject("layer").getJSONObject("style").getString("name"));
        json = 
        "{" +
          "\"task\": {" +
            "\"layer\": {" +
              "\"style\": {" +
                    "\"name\": \"point\"" + 
                 "}" +
               "}" +
           "}" + 
        "}";
        putTask(i, t,json);

        task = getTask(i, t);
        
        assertEquals("READY", task.getString("state"));
        assertEquals("point", task.getJSONObject("layer").getJSONObject("style").getString("name"));

        postImport(i);
        runChecks("archsites");
    }

    @Test
    public void testSingleFileUploadIntoDb() throws Exception {
        DataStoreInfo acme = createH2DataStore("sf", "acme");

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
                            "\"name\": \"acme\"" + 
                        "}" + 
                     "}" +
                "}" + 
            "}";
        int i = postNewImport(json);
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        Catalog cat = importer.getCatalog();
        assertTrue(cat.getFeatureTypesByDataStore(acme).isEmpty());

        postImport(i);

        assertEquals(1, cat.getFeatureTypesByDataStore(acme).size());
        assertNotNull(cat.getFeatureTypeByStore(acme, "archsites"));
        runChecks("sf:archsites");
    }

    @Test
    public void testSingleFileUploadIntoDb2() throws Exception {
        DataStoreInfo acme = createH2DataStore("sf", "acme");

        String json = 
            "{" + 
                "\"import\": { " + 
                    "\"targetStore\": {" +
                        "\"dataStore\": {" + 
                            "\"name\": \"acme\", " +
                            "\"workspace\": {" + 
                                "\"name\": \"sf\"" + 
                            "}" + 
                        "}" + 
                     "}" +
                "}" + 
            "}";
        int i = postNewImport(json);
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        Catalog cat = importer.getCatalog();
        assertTrue(cat.getFeatureTypesByDataStore(acme).isEmpty());

        postImport(i);

        assertEquals(1, cat.getFeatureTypesByDataStore(acme).size());
        assertNotNull(cat.getFeatureTypeByStore(acme, "archsites"));
        runChecks("sf:archsites");
    }

    @Test
    public void testIndirectUpdateSRS() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = createH2DataStore(cat.getDefaultWorkspace().getName(), "spearfish");

        File dir = tmpDir();
        unpack("shape/archsites_no_crs.zip", dir);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        
        JSONObject task = getTask(0, 0);
        assertEquals("NO_CRS", task.get("state"));

        String json = "{\"id\":0,\"layer\":{\"srs\":\"EPSG:26713\"}}";
        putTask(0, 0, json);

        task = getTask(0, 0);
        assertEquals("READY", task.get("state"));

        DataStore store = (DataStore) ds.getDataStore(null);
        assertEquals(store.getTypeNames().length, 0);

        postImport(0);
        
        assertEquals(store.getTypeNames().length, 1);
        assertEquals("archsites", store.getTypeNames()[0]);
    }

    @Test
    public void testMosaicUpload() throws Exception {
        String json = 
                "{" + 
                    "\"import\": { " + 
                        "\"data\": {" +
                           "\"type\": \"mosaic\" " + 
                         "}" +
                    "}" + 
                "}";
        int i = postNewImport(json);
        postNewTaskAsMultiPartForm(i, "mosaic/bm.zip");
        postImport(i);

        ImportContext context = importer.getContext(i);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        String layername = context.getTasks().get(0).getLayer().getName();
        runChecks(layername);
    }

    @Test
    public void testTimeMosaicUpload() throws Exception {
        String json = 
                "{" + 
                    "\"import\": { " + 
                        "\"data\": {" +
                           "\"type\": \"mosaic\", " +
                           "\"name\": \"myname\", " +
                           "\"time\": {" +
                              " \"mode\": \"filename\"," + 
                              " \"filenameRegex\": \"(\\\\d){6}\"," + 
                              " \"timeFormat\": \"yyyyMM\"" + 
                           "}" + 
                         "}" +
                    "}" + 
                "}";
        int i = postNewImport(json);
        int t = putNewTask(i, "mosaic/bm_time.zip");

        postImport(i);

        LayerInfo l = importer.getContext(i).getTasks().get(0).getLayer();
        assertEquals("myname", l.getName());
        runChecks(l.getName());
    }

    @Test
    public void testTimeMosaicManual() throws Exception {
        String json = 
                "{" + 
                    "\"import\": { " + 
                        "\"data\": {" +
                           "\"type\": \"mosaic\", " + 
                           "\"time\": {" +
                              " \"mode\": \"manual\"" + 
                           "}" + 
                         "}" +
                    "}" + 
                "}";
        int imp = postNewImport(json);
        int task = postNewTaskAsMultiPartForm(imp, "mosaic/bm_time.zip");

        //update all the files
        JSONObject t = getTask(imp, task);
        JSONArray files = t.getJSONObject("data").getJSONArray("files");
        assertEquals(4, files.size());

        for (int i = 0; i < files.size(); i++) {
            JSONObject obj = files.getJSONObject(i);
            assertFalse(obj.has("timestamp"));
        }

        for (int i = 0; i < files.size(); i++) {
            JSONObject obj = files.getJSONObject(i);

            String path =
                String.format("/rest/imports/%d/data/files/%s", imp, obj.getString("file"));
            json = String.format("{" + 
                       "\"timestamp\": \"2004-0%d-01T00:00:00.000+0000\"" +  
                "}", (i+1));
            put(path, json, "application/json");
        }

        t = getTask(imp, task);

        files = t.getJSONObject("data").getJSONArray("files");
        for (int i = 0; i < files.size(); i++) {
            JSONObject obj = files.getJSONObject(i);
            assertTrue(obj.has("timestamp"));

            String timestamp = obj.getString("timestamp");
            assertEquals(String.format("2004-0%d-01T00:00:00.000+0000", (i+1)), 
                timestamp);
        }
    }

    @Test
    public void testTimeMosaicAuto() throws Exception {
        String json = 
                "{" + 
                    "\"import\": { " + 
                        "\"data\": {" +
                           "\"type\": \"mosaic\", " + 
                           "\"time\": {" +
                              " \"mode\": \"auto\"" + 
                           "}" + 
                         "}" +
                    "}" + 
                "}";
        int imp = postNewImport(json);
        int task = postNewTaskAsMultiPartForm(imp, "mosaic/bm_time.zip");

        //verify files have a timestamp
        JSONObject t = getTask(imp, task);
        JSONArray files = t.getJSONObject("data").getJSONArray("files");
        assertEquals(4, files.size());

        for (int i = 0; i < files.size(); i++) {
            JSONObject obj = files.getJSONObject(i);
            assertTrue(obj.has("timestamp"));
        }

        t = getTask(imp, task);
        files = t.getJSONObject("data").getJSONArray("files");

        //ensure all dates set up, can't rely on iteration order
        List<Integer> ints = Lists.newArrayList(1,2,3,4);
        Pattern p = Pattern.compile("2004-0(\\d)-01T00:00:00.000\\+0000");

        for (int i = 0; i < files.size(); i++) {
            JSONObject obj = files.getJSONObject(i);
            assertTrue(obj.has("timestamp"));

            String timestamp = obj.getString("timestamp");
            Matcher m = p.matcher(timestamp);
            assertTrue(m.matches());

            ints.remove((Object)Integer.parseInt(m.group(1)));
        }
        assertTrue(ints.isEmpty());
        
        postImport(imp);

        LayerInfo l = importer.getContext(imp).getTasks().get(0).getLayer();
        runChecks(l.getName());
    }

    JSONObject getImport(int imp) throws Exception {
        JSON json = getAsJSON(String.format("/rest/imports/%d", imp));
        return ((JSONObject)json).getJSONObject("import");
    }

    JSONObject getTask(int imp, int task) throws Exception {
        JSON json = getAsJSON(String.format("/rest/imports/%d/tasks/%d?expand=all", imp, task));
        return ((JSONObject)json).getJSONObject("task");
    }

    void putTask(int imp, int task, String json) throws Exception {
        MockHttpServletResponse resp = putAsServletResponse(
            String.format("/rest/imports/%d/tasks/%d", imp, task), json, "application/json");
        assertEquals(204, resp.getStatusCode());
    }

    void putTaskLayer(int imp, int task, String json) throws Exception {
        MockHttpServletResponse resp = putAsServletResponse(
            String.format("/rest/imports/%d/tasks/%d/layer", imp, task), json, "application/json");
        assertEquals(202, resp.getStatusCode());
    }

    int postNewTaskAsMultiPartForm(int imp, String data) throws Exception {
        File dir = unpack(data);

        List<Part> parts = new ArrayList<Part>(); 
        for (File f : dir.listFiles()) {
            parts.add(new FilePart(f.getName(), f));
        }
        MultipartRequestEntity multipart = new MultipartRequestEntity(
            parts.toArray(new Part[parts.size()]), new PostMethod().getParams());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        multipart.writeRequest(bout);

        MockHttpServletRequest req = createRequest("/rest/imports/" + imp + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setBodyContent(bout.toByteArray());

        MockHttpServletResponse resp = dispatch(req);
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        assertTrue(resp.getHeader("Location").matches(".*/imports/"+imp+"/tasks/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject task = json.getJSONObject("task");
        return task.getInt("id");
    }

    int putNewTask(int imp, String data) throws Exception {
        File zip = file(data);
        byte[] payload = new byte[ (int) zip.length()];
        FileInputStream fis = new FileInputStream(zip);
        fis.read(payload);
        fis.close();

        MockHttpServletRequest req = createRequest("/rest/imports/" + imp + "/tasks/" + new File(data).getName());
        req.setHeader("Content-Type", MediaType.APPLICATION_ZIP.toString());
        req.setMethod("PUT");
        req.setBodyContent(payload);

        MockHttpServletResponse resp = dispatch(req);
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        assertTrue(resp.getHeader("Location").matches(".*/imports/"+imp+"/tasks/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject task = json.getJSONObject("task");
        return task.getInt("id");
    }

    int postNewImport() throws Exception {
        return postNewImport(null);
    }
    int postNewImport(String body) throws Exception {
        MockHttpServletResponse resp = body == null ? postAsServletResponse("/rest/imports", "")
            : postAsServletResponse("/rest/imports", body, "application/json");
        
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );
        assertTrue(resp.getHeader("Location").matches(".*/imports/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject imprt = json.getJSONObject("import");
        return imprt.getInt("id");
    }

    void postImport(int imp) throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports/" + imp, "");
        assertEquals(204, resp.getStatusCode());
    }
}
