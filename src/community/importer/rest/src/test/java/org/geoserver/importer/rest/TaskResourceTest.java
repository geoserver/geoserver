/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.util.IOUtils;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.data.Transaction;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geoserver.importer.DataFormat;
import org.geoserver.importer.Directory;
import org.geoserver.importer.GridFormat;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.VFSWorker;
import org.geoserver.importer.ImportContext.State;
import org.geoserver.importer.ImporterTestSupport.JSONObjectBuilder;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.UpdateMode;

/**
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class TaskResourceTest extends ImporterTestSupport {
    JDBCDataStore jdbcStore;

    // some rest calls now require admin permissions
    private void doLogin() throws Exception {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        l.add(new GeoServerRole("ROLE_ADMINISTRATOR"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "geoserver", l));
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        doLogin();

        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);
        importer.createContext(new Directory(dir));
    }
    
    private Integer putZip(String path) throws Exception {
        File file = new File(path);
        InputStream stream;
        if (file.exists()) {
            stream = new FileInputStream(file);
        } else {
            stream = ImporterTestSupport.class.getResourceAsStream("../test-data/" + path);
        }
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks/" + file.getName());
        req.setContentType("application/zip");
        req.addHeader("Content-Type","application/zip");
        req.setMethod("PUT");
        req.setBodyContent(org.apache.commons.io.IOUtils.toByteArray(stream));
        resp = dispatch(req);
        
        assertEquals(201, resp.getStatusCode());
        
        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        
        return id;
    }
    
    private Integer putZipAsURL(String zip) throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);
        
        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks/");
        Form form = new Form();
        form.add("url", new File(zip).getAbsoluteFile().toURI().toString());
        req.setBodyContent(form.encode());
        req.setMethod("POST");
        req.setContentType(MediaType.APPLICATION_WWW_FORM.toString());
        req.setHeader("Content-Type", MediaType.APPLICATION_WWW_FORM.toString());
        resp = dispatch(req);
        
        assertEquals(201, resp.getStatusCode());
        
        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        
        return id;
    }
    
    Integer upload(String zip, boolean asURL) throws Exception {
        URL resource = ImporterTestSupport.class.getResource("../test-data/" + zip);
        File file = new File(resource.getFile());
        String[] nameext = file.getName().split("\\.");
        Connection conn = jdbcStore.getConnection(Transaction.AUTO_COMMIT);
        String sql = "drop table if exists \"" + nameext[0] + "\"";
        Statement stmt = conn.createStatement();
        stmt.execute(sql);
        stmt.close();
        conn.close();
        if (asURL) {
            // make a copy since, zip as url will archive and delete it
            File copyDir = tmpDir();
            FileUtils.copyFile(file, new File(copyDir,zip));
            return putZipAsURL(new File(copyDir,zip).getAbsolutePath());
        } else {
            return putZip(zip);
        }
    }
    
    public void testGetAllTasks() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks");

        JSONArray tasks = json.getJSONArray("tasks");
        assertEquals(2, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals(0, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/0/tasks/0"));
        
        task = tasks.getJSONObject(1);
        assertEquals(1, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/0/tasks/1"));
    }

    public void testGetTask() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0");
        JSONObject task = json.getJSONObject("task");
        assertEquals(0, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/0/tasks/0"));
    }

    public void testDeleteTask() throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        
        ImportContext context = importer.getContext(id);

        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        
        new File(dir, "extra.file").createNewFile();
        File[] files = dir.listFiles();
        Part[] parts = new Part[files.length];
        for (int i = 0; i < files.length; i++) {
            parts[i] = new FilePart(files[i].getName(), files[i]);
        }

        MultipartRequestEntity multipart =
            new MultipartRequestEntity(parts, new PostMethod().getParams());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        multipart.writeRequest(bout);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setBodyContent(bout.toByteArray());
        resp = dispatch(req);

        context = importer.getContext(context.getId());
        assertEquals(2, context.getTasks().size());

        req = createRequest("/rest/imports/" + id + "/tasks/1");
        req.setMethod("DELETE");
        resp = dispatch(req);
        assertEquals(204, resp.getStatusCode());

        context = importer.getContext(context.getId());
        assertEquals(1, context.getTasks().size());
    }

    public void testPostMultiPartFormData() throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);
        assertNull(context.getData());
        assertTrue(context.getTasks().isEmpty());

        File dir = unpack("shape/archsites_epsg_prj.zip");
        
        Part[] parts = new Part[]{new FilePart("archsites.shp", new File(dir, "archsites.shp")), 
            new FilePart("archsites.dbf", new File(dir, "archsites.dbf")), 
            new FilePart("archsites.shx", new File(dir, "archsites.shx")), 
            new FilePart("archsites.prj", new File(dir, "archsites.prj"))};

        MultipartRequestEntity multipart = 
            new MultipartRequestEntity(parts, new PostMethod().getParams());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        multipart.writeRequest(bout);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setBodyContent(bout.toByteArray());
        resp = dispatch(req);

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals(ImportTask.State.READY, task.getState());
    }
    
    private void uploadGeotiffAndVerify(String taskName,
            InputStream geotiffResourceStream, String contentType) throws Exception {
        // upload  tif or zip file containing a tif and verify the results
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks/" + taskName);
        req.setContentType(contentType);
        req.addHeader("Content-Type", contentType);
        req.setMethod("PUT");
        req.setBodyContent(org.apache.commons.io.IOUtils.toByteArray(geotiffResourceStream));
        resp = dispatch(req);

        assertEquals(201, resp.getStatusCode());

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());

        ImportData importData = task.getData();
        assertTrue(importData instanceof SpatialFile);

        DataFormat format = importData.getFormat();
        assertTrue(format instanceof GridFormat);
    }

    public void testPostGeotiffBz2() throws Exception {
        String path = "geotiff/EmissiveCampania.tif.bz2";
        InputStream stream = ImporterTestSupport.class.getResourceAsStream("test-data/" + path);

        uploadGeotiffAndVerify(new File(path).getName(), stream, "application/x-bzip2");
    }


    public void testPostGeotiff() throws Exception {
        File tempBase = tmpDir();
        File tempDir = new File(tempBase, "testPostGeotiff");
        if (!tempDir.mkdirs()) {
            throw new IllegalStateException("Cannot create temp dir for testing geotiff");
        }

        String tifname = "EmissiveCampania.tif";
        String bz2name = tifname + ".bz2";
        File destinationArchive = new File(tempDir, bz2name);
        InputStream inputStream = ImporterTestSupport.class.getResourceAsStream("test-data/geotiff/" + bz2name);

        IOUtils.copy(inputStream, destinationArchive);

        VFSWorker vfs = new VFSWorker();
        vfs.extractTo(destinationArchive, tempDir);

        File tiff = new File(tempDir, tifname);
        if (!tiff.exists()) {
            throw new IllegalStateException("Did not extract tif correctly");
        }

        FileInputStream fis = new FileInputStream(tiff);
        uploadGeotiffAndVerify(tifname, fis, "image/tiff");
    }

    public void testGetTarget() throws Exception {
        JSONObject json = ((JSONObject) getAsJSON("/rest/imports/0/tasks/0")).getJSONObject("task");

        JSONObject target = json.getJSONObject("target");
        assertTrue(target.has("href"));
        assertTrue(target.getString("href").endsWith("/rest/imports/0/tasks/0/target"));
        assertTrue(target.has("dataStore"));

        target = target.getJSONObject("dataStore");
        assertTrue(target.has("name"));
        
        json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/target");
        assertNotNull(json.get("dataStore"));
    }

    public void testPutTarget() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/target");
        assertEquals("archsites", json.getJSONObject("dataStore").getString("name"));

        String update = "{\"dataStore\": { \"type\": \"foo\" }}";
        put("/rest/imports/0/tasks/0/target", update, MediaType.APPLICATION_JSON.toString());

        json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/target");
        assertEquals("foo", json.getJSONObject("dataStore").getString("type"));
    }

    public void testPutTargetExisting() throws Exception {
        createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "foo");

        String update = "{\"dataStore\": { \"name\": \"foo\" }}";
        put("/rest/imports/0/tasks/0/target", update, MediaType.APPLICATION_JSON.toString());

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/target");
        assertEquals("foo", json.getJSONObject("dataStore").getString("name"));
        assertEquals("H2", json.getJSONObject("dataStore").getString("type"));
    }

    public void testUpdateMode() throws Exception {
        createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "foo");

        ImportContext session = importer.getContext(0);
        assertEquals(UpdateMode.CREATE, session.getTasks().get(0).getUpdateMode());

        // change to append mode
        String update = "{\"task\": { \"updateMode\" : \"APPEND\" }}";
        put("/rest/imports/0/tasks/0", update, MediaType.APPLICATION_JSON.toString());
        session = importer.getContext(0);
        assertEquals(UpdateMode.APPEND, session.getTasks().get(0).getUpdateMode());

        // put a dumby and verify the modified updateMode remains
        update = "{\"task\": {}}";
        put("/rest/imports/0/tasks/0", update, MediaType.APPLICATION_JSON.toString());
        session = importer.getContext(0);
        assertEquals(UpdateMode.APPEND, session.getTasks().get(0).getUpdateMode());
    }

    public void testPutItemSRS() throws Exception {
        File dir = unpack("shape/archsites_no_crs.zip");
        importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/1/tasks/0");
        JSONObject task = json.getJSONObject("task");
        assertEquals("NO_CRS", task.get("state"));
        assertFalse(task.getJSONObject("layer").containsKey("srs"));

        // verify invalid SRS handling
        MockHttpServletResponse resp = setSRSRequest("/rest/imports/1/tasks/0","26713");
        verifyInvalidCRSErrorResponse(resp);
        resp = setSRSRequest("/rest/imports/1/tasks/0","EPSG:9838275");
        verifyInvalidCRSErrorResponse(resp);
        
        setSRSRequest("/rest/imports/1/tasks/0","EPSG:26713");
        
        ImportContext context = importer.getContext(1);
        ReferencedEnvelope latLonBoundingBox = 
            context.getTasks().get(0).getLayer().getResource().getLatLonBoundingBox();
        assertFalse("expected not empty bbox",latLonBoundingBox.isEmpty());

        json = (JSONObject) getAsJSON("/rest/imports/1/tasks/0?expand=2");
        task = json.getJSONObject("task");
        assertEquals("READY", task.get("state"));
        
        assertEquals("EPSG:26713", 
            task.getJSONObject("layer").getString("srs"));
        State state = context.getState();
        assertEquals("Invalid context state", State.PENDING, state);
    }
    
    private void verifyInvalidCRSErrorResponse(MockHttpServletResponse resp) {
        assertEquals(Status.CLIENT_ERROR_BAD_REQUEST.getCode(), resp.getStatusCode());
        JSONObject errorResponse = JSONObject.fromObject(resp.getOutputStreamContent());
        JSONArray errors = errorResponse.getJSONArray("errors");
        assertTrue(errors.get(0).toString().startsWith("Invalid SRS"));
    }

    /**
     * Ideally, many variations of error handling could be tested here.
     * (For performance - otherwise too much tear-down/setup)
     * @throws Exception
     */
    public void testErrorHandling() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0");

        JSONObjectBuilder badDateFormatTransform = new JSONObjectBuilder();
        badDateFormatTransform.
            object().
                key("task").object().
                    key("transformChain").object().
                        key("type").value("VectorTransformChain").
                        key("transforms").array().
                            object().
                                key("field").value("datefield").
                                key("type").value("DateFormatTransform").
                                key("format").value("xxx").
                            endObject().
                        endArray().
                    endObject().
                endObject().
            endObject();
        
        MockHttpServletResponse resp = 
            putAsServletResponse("/rest/imports/0/tasks/0", badDateFormatTransform.buildObject().toString(), "application/json");
        assertErrorResponse(resp, "Invalid date parsing format");
    }

    public void testDeleteTask2() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse("/rest/imports/0/tasks/0");
        assertEquals(204, response.getStatusCode());

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks");

        JSONArray items = json.getJSONArray("tasks");
        assertEquals(1, items.size());
        assertEquals(1, items.getJSONObject(0).getInt("id"));
    }

    public void testGetLayer() throws Exception {
        String path = "/rest/imports/0/tasks/0";
        JSONObject json = ((JSONObject) getAsJSON(path)).getJSONObject("task");
        
        assertTrue(json.has("layer"));
        JSONObject layer = json.getJSONObject("layer");
        assertTrue(layer.has("name"));
        assertTrue(layer.has("href"));
        assertTrue(layer.getString("href").endsWith(path+"/layer"));

        json = (JSONObject) getAsJSON(path+"/layer");

        assertTrue(layer.has("name"));
        assertTrue(layer.has("href"));
        assertTrue(layer.getString("href").endsWith(path+"/layer"));
    }
}
