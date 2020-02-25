/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.importer.DataFormat;
import org.geoserver.importer.Directory;
import org.geoserver.importer.GridFormat;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportContext.State;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.UpdateMode;
import org.geoserver.importer.VFSWorker;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.util.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class ImportTaskControllerTest extends ImporterTestSupport {
    JDBCDataStore jdbcStore;

    // some rest calls now require admin permissions
    private void doLogin() throws Exception {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        l.add(new GeoServerRole("ROLE_ADMINISTRATOR"));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("admin", "geoserver", l));
    }

    @Before
    public void prepareData() throws Exception {
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
        MockHttpServletResponse resp =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/imports", "");
        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length - 1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req =
                createRequest(
                        RestBaseController.ROOT_PATH
                                + "/imports/"
                                + id
                                + "/tasks/"
                                + file.getName());
        req.setContentType("application/zip");
        req.addHeader("Content-Type", "application/zip");
        req.setMethod("PUT");
        req.setContent(org.apache.commons.io.IOUtils.toByteArray(stream));
        resp = dispatch(req);

        assertEquals(201, resp.getStatus());

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);

        return id;
    }

    private Integer putZipAsURL(String zip) throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/imports", "");
        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length - 1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req =
                createRequest(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/");
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>(1);
        form.add("url", new File(zip).getAbsoluteFile().toURI().toString());
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final HttpHeaders headers = new HttpHeaders();
        new FormHttpMessageConverter()
                .write(
                        form,
                        MediaType.APPLICATION_FORM_URLENCODED,
                        new HttpOutputMessage() {
                            @Override
                            public OutputStream getBody() throws IOException {
                                return stream;
                            }

                            @Override
                            public HttpHeaders getHeaders() {
                                return headers;
                            }
                        });
        req.setContent(stream.toByteArray());
        req.setMethod("POST");
        req.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        req.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        resp = dispatch(req);

        assertEquals(201, resp.getStatus());

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        SpatialFile data = ((SpatialFile) task.getData());
        assertFalse(new File(data.getFile().getParentFile(), ".locking").exists());
        assertTrue(new File(data.getFile().getParentFile(), ".clean-me").exists());
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
            FileUtils.copyFile(file, new File(copyDir, zip));
            return putZipAsURL(new File(copyDir, zip).getAbsolutePath());
        } else {
            return putZip(zip);
        }
    }

    @Test
    public void testGetAllTasks() throws Exception {
        int id = lastId();
        JSONObject json =
                (JSONObject) getAsJSON(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks");

        JSONArray tasks = json.getJSONArray("tasks");
        assertEquals(2, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals(0, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/" + id + "/tasks/0"));

        task = tasks.getJSONObject(1);
        assertEquals(1, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/" + id + "/tasks/1"));
    }

    @Test
    public void testGetTask() throws Exception {
        int id = lastId();
        JSONObject json =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0");
        JSONObject task = json.getJSONObject("task");
        assertEquals(0, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/" + id + "/tasks/0"));
    }

    @Test
    public void testGetTaskProgress() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + lastId()
                                        + "/tasks/0/progress",
                                200);
        assertEquals("READY", json.get("state"));
        // TODO: trigger import and check progress
    }

    @Test
    public void testDeleteTask() throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/imports", "");
        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length - 1]);

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

        MockHttpServletRequest req =
                createRequest(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setContent(bout.toByteArray());
        resp = dispatch(req);

        context = importer.getContext(context.getId());
        assertEquals(2, context.getTasks().size());

        req = createRequest(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/1");
        req.setMethod("DELETE");
        resp = dispatch(req);
        assertEquals(204, resp.getStatus());

        context = importer.getContext(context.getId());
        assertEquals(1, context.getTasks().size());
    }

    @Test
    public void testPostMultiPartFormData() throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/imports", "");
        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length - 1]);
        ImportContext context = importer.getContext(id);
        assertNull(context.getData());
        assertTrue(context.getTasks().isEmpty());

        File dir = unpack("shape/archsites_epsg_prj.zip");

        Part[] parts =
                new Part[] {
                    new FilePart("archsites.shp", new File(dir, "archsites.shp")),
                    new FilePart("archsites.dbf", new File(dir, "archsites.dbf")),
                    new FilePart("archsites.shx", new File(dir, "archsites.shx")),
                    new FilePart("archsites.prj", new File(dir, "archsites.prj"))
                };

        MultipartRequestEntity multipart =
                new MultipartRequestEntity(parts, new PostMethod().getParams());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        multipart.writeRequest(bout);

        MockHttpServletRequest req =
                createRequest(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setContent(bout.toByteArray());
        resp = dispatch(req);

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals(ImportTask.State.READY, task.getState());
        SpatialFile data = ((SpatialFile) task.getData());
        assertTrue(new File(data.getFile().getParentFile(), ".locking").exists());
    }

    private ImportContext uploadGeotiffAndVerify(
            String taskName, InputStream geotiffResourceStream, String contentType)
            throws Exception {
        return uploadGeotiffAndVerify(
                taskName, geotiffResourceStream, contentType, "", "application/xml");
    }

    private ImportContext uploadGeotiffAndVerify(
            String taskName,
            InputStream geotiffResourceStream,
            String contentType,
            String createImportBody,
            String creationContentType)
            throws Exception {
        // upload  tif or zip file containing a tif and verify the results
        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/imports",
                        createImportBody,
                        creationContentType);
        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length - 1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req =
                createRequest(
                        RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/" + taskName);
        req.setContentType(contentType);
        req.addHeader("Content-Type", contentType);
        req.setMethod("PUT");
        req.setContent(org.apache.commons.io.IOUtils.toByteArray(geotiffResourceStream));
        resp = dispatch(req);

        assertEquals(201, resp.getStatus());

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());

        ImportData importData = task.getData();
        assertTrue(importData instanceof SpatialFile);

        DataFormat format = importData.getFormat();
        assertTrue(format instanceof GridFormat);

        return context;
    }

    @Test
    public void testPostGeotiffBz2() throws Exception {
        String path = "geotiff/EmissiveCampania.tif.bz2";
        InputStream stream = ImporterTestSupport.class.getResourceAsStream("test-data/" + path);

        uploadGeotiffAndVerify(new File(path).getName(), stream, "application/x-bzip2");
    }

    @Test
    public void testPostGeotiffBz2TargetWorkspaceJsonUTF8() throws Exception {
        String path = "geotiff/EmissiveCampania.tif.bz2";
        InputStream stream = ImporterTestSupport.class.getResourceAsStream("test-data/" + path);

        String creationRequest =
                "{\n"
                        + "   \"import\": {\n"
                        + "      \"targetWorkspace\": {\n"
                        + "         \"workspace\": {\n"
                        + "            \"name\": \"sf\"\n"
                        + "         }\n"
                        + "      }\n"
                        + "   }\n"
                        + "}";
        ImportContext context =
                uploadGeotiffAndVerify(
                        new File(path).getName(),
                        stream,
                        "application/x-bzip2",
                        creationRequest,
                        "application/json;charset=UTF-8");
        final ImportTask task = context.getTasks().get(0);
        assertEquals("sf", task.getStore().getWorkspace().getName());
    }

    @Test
    public void testPostGeotiff() throws Exception {
        File tempBase = tmpDir();
        File tempDir = new File(tempBase, "testPostGeotiff");
        if (!tempDir.mkdirs()) {
            throw new IllegalStateException("Cannot create temp dir for testing geotiff");
        }

        String tifname = "EmissiveCampania.tif";
        String bz2name = tifname + ".bz2";
        File destinationArchive = new File(tempDir, bz2name);
        InputStream inputStream =
                ImporterTestSupport.class.getResourceAsStream("test-data/geotiff/" + bz2name);

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

    @Test
    public void testGetTarget() throws Exception {
        int id = lastId();
        JSONObject json =
                ((JSONObject)
                                getAsJSON(
                                        RestBaseController.ROOT_PATH
                                                + "/imports/"
                                                + id
                                                + "/tasks/0"))
                        .getJSONObject("task");

        JSONObject target = json.getJSONObject("target");
        assertTrue(target.has("href"));
        assertTrue(
                target.getString("href")
                        .endsWith(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0/target"));
        assertTrue(target.has("dataStore"));

        target = target.getJSONObject("dataStore");
        assertTrue(target.has("name"));

        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0/target");
        assertNotNull(json.get("dataStore"));
    }

    @Test
    public void testPutTarget() throws Exception {
        int id = lastId();
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0/target");
        assertEquals("archsites", json.getJSONObject("dataStore").getString("name"));

        String update = "{\"dataStore\": { \"type\": \"foo\" }}";
        put(
                RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0/target",
                update,
                MediaType.APPLICATION_JSON.toString());

        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0/target");
        assertEquals("foo", json.getJSONObject("dataStore").getString("type"));
    }

    @Test
    public void testPutTargetExisting() throws Exception {
        createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "foo");

        int id = lastId();
        String update = "{\"dataStore\": { \"name\": \"foo\" }}";
        put(
                RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0/target",
                update,
                MediaType.APPLICATION_JSON.toString());

        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0/target");
        assertEquals("foo", json.getJSONObject("dataStore").getString("name"));
        assertEquals("H2", json.getJSONObject("dataStore").getString("type"));
    }

    @Test
    public void testUpdateMode() throws Exception {
        createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "foo");

        int id = lastId();
        ImportContext session = importer.getContext(id);
        assertEquals(UpdateMode.CREATE, session.getTasks().get(0).getUpdateMode());

        // change to append mode
        String update = "{\"task\": { \"updateMode\" : \"APPEND\" }}";
        put(
                RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0",
                update,
                MediaType.APPLICATION_JSON.toString());
        session = importer.getContext(id);
        assertEquals(UpdateMode.APPEND, session.getTasks().get(0).getUpdateMode());

        // put a dumby and verify the modified updateMode remains
        update = "{\"task\": {}}";
        put(
                RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0",
                update,
                MediaType.APPLICATION_JSON.toString());
        session = importer.getContext(id);
        assertEquals(UpdateMode.APPEND, session.getTasks().get(0).getUpdateMode());
    }

    @Test
    public void testPutItemSRS() throws Exception {
        File dir = unpack("shape/archsites_no_crs.zip");
        importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));

        int id = lastId();
        String firstTaskPath = RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0";
        JSONObject json = (JSONObject) getAsJSON(firstTaskPath);
        JSONObject task = json.getJSONObject("task");
        assertEquals("NO_CRS", task.get("state"));
        assertFalse(task.getJSONObject("layer").containsKey("srs"));

        // verify invalid SRS handling
        MockHttpServletResponse resp = setSRSRequest(firstTaskPath, "26713");
        verifyInvalidCRSErrorResponse(resp);
        resp = setSRSRequest(firstTaskPath, "EPSG:9838275");
        verifyInvalidCRSErrorResponse(resp);

        setSRSRequest(firstTaskPath, "EPSG:26713");

        ImportContext context = importer.getContext(id);

        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0?expand=2");
        task = json.getJSONObject("task");
        assertEquals("READY", task.get("state"));

        assertEquals("EPSG:26713", task.getJSONObject("layer").getString("srs"));
        State state = context.getState();
        assertEquals("Invalid context state", State.PENDING, state);
    }

    /** This variant matches exactly the documentation and puts the changes directly on the layer */
    @Test
    public void testPutItemSRSOnLayer() throws Exception {
        File dir = unpack("shape/archsites_no_crs.zip");
        importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));

        int id = lastId();
        JSONObject json =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0");
        JSONObject task = json.getJSONObject("task");
        assertEquals("NO_CRS", task.get("state"));
        assertFalse(task.getJSONObject("layer").containsKey("srs"));

        setSRSRequest(
                RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0/layer", "EPSG:26713");

        ImportContext context = importer.getContext(id);

        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + id
                                        + "/tasks/0?expand=2");
        task = json.getJSONObject("task");
        assertEquals("READY", task.get("state"));

        assertEquals("EPSG:26713", task.getJSONObject("layer").getString("srs"));
        State state = context.getState();
        assertEquals("Invalid context state", State.PENDING, state);
    }

    /** Rename layer test */
    @Test
    public void testRenameLayerAndImportIntoH2() throws Exception {
        // create H2 store to act as a target
        DataStoreInfo h2Store =
                createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "testTarget");

        // create context with default name
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(0l);
        context.setTargetStore(h2Store);
        importer.changed(context);
        importer.update(context, new SpatialFile(new File(dir, "archsites.shp")));

        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + context.getId()
                                        + "/tasks/0");
        JSONObject task = json.getJSONObject("task");
        assertEquals("READY", task.get("state"));

        // now rename the layer
        String renamer =
                "{\n"
                        + "  \"layer\": {\n"
                        + "\t\t\t\"name\": \"test123\",\n"
                        + "\t\t  \"nativeName\": \"test123\",\n"
                        + "\t\t}\n"
                        + "}";
        JSONObject response =
                (JSONObject)
                        putAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/imports/"
                                        + context.getId()
                                        + "/tasks/0/layer",
                                renamer,
                                "application/json");
        JSONObject layer = response.getJSONObject("layer");
        assertEquals("test123", layer.getString("name"));
        assertEquals("test123", layer.getString("nativeName"));
        assertEquals("archsites", layer.getString("originalName"));

        context = importer.getContext(context.getId());
        importer.run(context);

        // check created type, layer and database table
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName("test123");
        assertNotNull(ftInfo);
        assertEquals("test123", ftInfo.getNativeName());
        DataStore store = (DataStore) h2Store.getDataStore(null);
        assertThat(Arrays.asList(store.getTypeNames()), CoreMatchers.hasItem("test123"));
    }

    private void verifyInvalidCRSErrorResponse(MockHttpServletResponse resp)
            throws UnsupportedEncodingException {
        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatus());
        // TODO: Implement JSON error format
        /*
        JSONObject errorResponse = JSONObject.fromObject(resp.getContentAsString());
        JSONArray errors = errorResponse.getJSONArray("errors");
        assertTrue(errors.get(0).toString().startsWith("Invalid SRS"));
        */
    }

    /**
     * Ideally, many variations of error handling could be tested here. (For performance - otherwise
     * too much tear-down/setup)
     */
    @Test
    public void testErrorHandling() throws Exception {
        int id = lastId();
        JSONObject json =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0");

        JSONObjectBuilder badDateFormatTransform = new JSONObjectBuilder();
        badDateFormatTransform
                .object()
                .key("task")
                .object()
                .key("transformChain")
                .object()
                .key("type")
                .value("VectorTransformChain")
                .key("transforms")
                .array()
                .object()
                .key("field")
                .value("datefield")
                .key("type")
                .value("DateFormatTransform")
                .key("format")
                .value("xxx")
                .endObject()
                .endArray()
                .endObject()
                .endObject()
                .endObject();

        MockHttpServletResponse resp =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/imports/" + id + "/tasks/0",
                        badDateFormatTransform.buildObject().toString(),
                        "application/json");
        assertErrorResponse(resp, "Invalid date parsing format");
    }

    @Test
    public void testDeleteTask2() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/imports/" + lastId() + "/tasks/0");
        assertEquals(204, response.getStatus());

        JSONObject json =
                (JSONObject)
                        getAsJSON(RestBaseController.ROOT_PATH + "/imports/" + lastId() + "/tasks");

        JSONArray items = json.getJSONArray("tasks");
        assertEquals(1, items.size());
        assertEquals(1, items.getJSONObject(0).getInt("id"));
    }

    @Test
    public void testGetLayer() throws Exception {
        String path = RestBaseController.ROOT_PATH + "/imports/" + lastId() + "/tasks/0";
        JSONObject json = ((JSONObject) getAsJSON(path)).getJSONObject("task");

        assertTrue(json.has("layer"));
        JSONObject layer = json.getJSONObject("layer");
        assertTrue(layer.has("name"));
        assertTrue(layer.has("href"));
        assertTrue(layer.getString("href").endsWith(path + "/layer"));

        json = (JSONObject) getAsJSON(path + "/layer");

        assertTrue(layer.has("name"));
        assertTrue(layer.has("href"));
        assertTrue(layer.getString("href").endsWith(path + "/layer"));
    }
}
