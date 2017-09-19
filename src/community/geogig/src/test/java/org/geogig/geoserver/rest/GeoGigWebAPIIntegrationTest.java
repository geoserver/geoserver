/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.locationtech.geogig.model.impl.RevObjectTestSupport.hashString;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Random;

import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geotools.data.DataAccess;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.ResolveTreeish;
import org.locationtech.geogig.plumbing.RevObjectParse;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.storage.datastream.DataStreamSerializationFactoryV1;
import org.locationtech.geogig.storage.impl.ObjectSerializingFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeoGigWebAPIIntegrationTest extends GeoServerSystemTestSupport {

    /**
     * {@code /geogig/repos/<repoId>}
     */
    private String BASE_URL;

    private static final Random rnd = new Random();

    @Rule
    public GeoGigTestData geogigData = new GeoGigTestData();

    /**
     * Override to avoid creating default geoserver test data
     */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do nothing
    }

    @Before
    public void before() throws Exception {
        // protected void onSetUp(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();
        geogigData.init()//
                .config("user.name", "gabriel")//
                .config("user.email", "gabriel@test.com")//
                .createTypeTree("lines", "geom:LineString:srid=4326")//
                .createTypeTree("points", "geom:Point:srid=4326")//
                .add()//
                .commit("created type trees")//
                .get();

        geogigData.insert("points", //
                "p1=geom:POINT(0 0)", //
                "p2=geom:POINT(1 1)", //
                "p3=geom:POINT(2 2)");

        geogigData.insert("lines", //
                "l1=geom:LINESTRING(-10 0, 10 0)", //
                "l2=geom:LINESTRING(0 0, 180 0)");

        geogigData.add().commit("Added test features");

        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        int i = rnd.nextInt();
        catalogBuilder.namespace("geogig.org/" + i).workspace("geogigws" + i)
                .store("geogigstore" + i);
        catalogBuilder.addAllRepoLayers().build();

        String workspaceName = catalogBuilder.workspaceName();
        String storeName = catalogBuilder.storeName();

        String layerName = workspaceName + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertNotNull(pointLayerInfo);

        layerName = workspaceName + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertNotNull(lineLayerInfo);

        DataStoreInfo dsInfo = catalog.getDataStoreByName(workspaceName, storeName);
        assertNotNull(dsInfo);
        assertEquals(GeoGigDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = dsInfo.getDataStore(null);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeoGigDataStore);

        String repoStr = (String) dsInfo.getConnectionParameters()
                .get(GeoGigDataStoreFactory.REPOSITORY.key);
        // resolve the repo
        URI repoURI = new URI(repoStr);
        RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
        String repoName = resolver.getName(repoURI);
        RepositoryInfo repositoryInfo = RepositoryManager.get().getByRepoName(repoName);
        assertNotNull(repositoryInfo);
        BASE_URL = "/geogig/repos/testrepo";
    }

    @After
    public void after() {
        RepositoryManager.close();
        getCatalog().dispose();
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/manifest}
     */
    @Test
    public void testGetManifest() throws Exception {
        final String url = BASE_URL + "/repo/manifest";
        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());

        String contentType = sr.getContentType();
        assertTrue(contentType, sr.getContentType().startsWith("text/plain"));

        String responseBody = sr.getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody, responseBody.startsWith("HEAD refs/heads/master"));
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/exists?oid=...}
     */
    @Test
    public void testRevObjectExists() throws Exception {
        final String resource = BASE_URL + "/repo/exists?oid=";

        GeoGIG geogig = geogigData.getGeogig();
        Ref head = geogig.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();

        String url;
        url = resource + commitId.toString();
        assertResponse(url, "1");

        ObjectId treeId = geogig.command(ResolveTreeish.class).setTreeish(commitId).call().get();
        url = resource + treeId.toString();
        assertResponse(url, "1");

        url = resource + hashString("fake");
        assertResponse(url, "0");
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/objects/<oid>}
     */
    @Test
    public void testGetObject() throws Exception {
        GeoGIG geogig = geogigData.getGeogig();
        Ref head = geogig.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();
        ObjectId treeId = geogig.command(ResolveTreeish.class).setTreeish(commitId).call().get();

        testGetRemoteObject(commitId);
        testGetRemoteObject(treeId);
    }

    private void testGetRemoteObject(ObjectId oid) throws Exception {
        GeoGIG geogig = geogigData.getGeogig();

        final String resource = BASE_URL + "/repo/objects/";
        final String url = resource + oid.toString();

        MockHttpServletResponse servletResponse;
        InputStream responseStream;

        servletResponse = getAsServletResponse(url);
        assertEquals(200, servletResponse.getStatus());

        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        assertEquals(contentType, servletResponse.getContentType());

        responseStream = getBinaryInputStream(servletResponse);

        ObjectSerializingFactory factory = DataStreamSerializationFactoryV1.INSTANCE;

        RevObject actual = factory.read(oid, responseStream);
        RevObject expected = geogig.command(RevObjectParse.class).setObjectId(oid).call().get();
        assertEquals(expected, actual);
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/batchobjects}
     */
    @Test
    public void testGetBatchedObjects() throws Exception {
        GeoGIG geogig = geogigData.getGeogig();
        Ref head = geogig.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();

        testGetBatchedRemoteObjects(commitId);
    }

    private void testGetBatchedRemoteObjects(ObjectId oid) throws Exception {
        GeoGIG geogig = geogigData.getGeogig();

        final String resource = BASE_URL + "/repo/batchobjects";
        final String url = resource;

        RevObject expected = geogig.command(RevObjectParse.class).setObjectId(oid).call().get();

        JsonObject requestBody = new JsonObject();
        JsonArray wantList = new JsonArray();
        wantList.add(new JsonPrimitive(oid.toString()));
        requestBody.add("want", wantList);

        MockHttpServletResponse servletResponse;
        InputStream responseStream;

        servletResponse = postAsServletResponse(url, requestBody.toString(), "application/json");
        assertEquals(200, servletResponse.getStatus());

        String contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
        assertEquals(contentType, servletResponse.getContentType());

        responseStream = getBinaryInputStream(servletResponse);

        ObjectSerializingFactory factory = DataStreamSerializationFactoryV1.INSTANCE;

        List<RevObject> objects = Lists
                .newArrayList(new ObjectStreamIterator(responseStream, factory));
        assertFalse(objects.isEmpty());
        RevObject actual = objects.get(objects.size() - 1);
        assertEquals(expected, actual);
    }

    private MockHttpServletResponse assertResponse(String url, String expectedContent)
            throws Exception {

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(sr.getContentAsString(), 200, sr.getStatus());

        String responseBody = sr.getContentAsString();

        assertNotNull(responseBody);
        assertEquals(expectedContent, responseBody);
        return sr;
    }

    private class ObjectStreamIterator extends AbstractIterator<RevObject> {
        private final InputStream bytes;

        private final ObjectSerializingFactory formats;

        public ObjectStreamIterator(InputStream input, ObjectSerializingFactory formats) {
            this.bytes = input;
            this.formats = formats;
        }

        @Override
        protected RevObject computeNext() {
            try {
                byte[] id = new byte[20];
                int len = bytes.read(id, 0, 20);
                if (len < 0)
                    return endOfData();
                if (len != 20)
                    throw new IllegalStateException("We need a 'readFully' operation!");
                return formats.read(new ObjectId(id), bytes);
            } catch (EOFException e) {
                return endOfData();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Test
    public void testRemoteAdd() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";

        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());

        Document dom = super.dom(new ByteArrayInputStream(sr.getContentAsString().getBytes()),
                true);

        // <response><success>true</success><name>upstream</name></response>
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("upstream", "/response/name", dom);

        dom = getAsDOM(url);
        print(dom);
        // <response><success>false</success><error>REMOTE_ALREADY_EXISTS</error></response>
        assertXpathEvaluatesTo("false", "/response/success", dom);
        assertXpathEvaluatesTo("REMOTE_ALREADY_EXISTS", "/response/error", dom);
    }

    @Test
    public void testRemoteRemove() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";

        final String addUrl = BASE_URL + "/remote?remoteURL=" + remoteURL + "&remoteName=";
        final String removeUrl = BASE_URL + "/remote?remove=true&remoteName=";
        final String listUrl = BASE_URL + "/remote?list=true";

        MockHttpServletResponse sr;
        Document dom;
        dom = getAsDOM(addUrl + "upstream");
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("upstream", "/response/name", dom);

        dom = getAsDOM(addUrl + "origin");
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("origin", "/response/name", dom);

        dom = getAsDOM(listUrl);
        assertXpathExists("/response/Remote/name[text() = 'upstream']", dom);
        assertXpathExists("/response/Remote/name[text() = 'origin']", dom);

        dom = getAsDOM(removeUrl + "upstream");
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("upstream", "/response/name", dom);

        dom = getAsDOM(listUrl);
        assertXpathNotExists("/response/Remote/name[text() = 'upstream']", dom);
        assertXpathExists("/response/Remote/name[text() = 'origin']", dom);

        dom = getAsDOM(removeUrl + "origin");
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("origin", "/response/name", dom);

        dom = getAsDOM(listUrl);
        assertXpathNotExists("/response/Remote/name[text() = 'upstream']", dom);
        assertXpathNotExists("/response/Remote/name[text() = 'origin']", dom);
    }

    @Test
    public void testRemoteUpdate() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";
        String newURL = "http://new.example.com/geogig/upstream";

        final String addUrl = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        final String renameUrl = BASE_URL
                + "/remote?update=true&remoteName=upstream&newName=new_name&remoteURL=" + newURL;

        Document dom = getAsDOM(addUrl);
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("upstream", "/response/name", dom);

        dom = getAsDOM(renameUrl);
        assertXpathEvaluatesTo("true", "/response/success", dom);
        assertXpathEvaluatesTo("new_name", "/response/name", dom);
    }

}
