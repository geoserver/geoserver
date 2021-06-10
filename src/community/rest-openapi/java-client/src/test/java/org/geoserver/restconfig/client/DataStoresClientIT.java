package org.geoserver.restconfig.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.geoserver.openapi.model.catalog.DataStoreInfo;
import org.geoserver.openapi.v1.model.DataStoreResponse;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
import org.geoserver.restconfig.client.DataStoreParams.Shapefile;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

/** Integration test suite for {@link DataStoresClient} */
public class DataStoresClientIT {

    public @Rule IntegrationTestSupport support = new IntegrationTestSupport();

    private WorkspacesClient workspaces;
    private DataStoresClient dataStores;

    public @Rule TestName testName = new TestName();
    public @Rule ExpectedException ex = ExpectedException.none();

    private SecureRandom rnd = new SecureRandom();

    private WorkspaceSummary workspace;

    private URI roadsShapefile;
    private URI streamsShapefile;

    public @Before void before() {
        Assume.assumeTrue(this.support.isAlive());
        this.workspaces = this.support.client().workspaces();
        this.dataStores = this.support.client().dataStores();
        String wsname =
                String.format("%s-%d", this.testName.getMethodName(), this.rnd.nextInt((int) 1e6));
        this.workspaces.create(wsname);
        this.workspace = this.workspaces.findByName(wsname).get();
        this.roadsShapefile = this.support.getRoadsShapefile();
        this.streamsShapefile = this.support.getStreamsShapefile();
    }

    public @After void after() {
        if (this.workspace != null) {
            this.workspaces.deleteRecursively(this.workspace.getName());
        }
    }

    public @Test void testFindByWorkspace() {
        List<Link> stores = this.dataStores.findByWorkspace("sf");
        assertEquals(1, stores.size());

        stores = this.dataStores.findByWorkspace("topp");
        assertEquals(2, stores.size());
    }

    public @Test void testGetNotFound() {
        Optional<DataStoreResponse> ds = this.dataStores.findByWorkspaceAndName("tiger", "nyc2");
        assertFalse(ds.isPresent());
    }

    public @Test void testGet() throws MalformedURLException {
        Optional<DataStoreResponse> ds = this.dataStores.findByWorkspaceAndName("sf", "sf");
        assertTrue(ds.isPresent());
        DataStoreResponse store = ds.get();
        assertEquals("sf", store.getName());
        assertNotNull(store.getWorkspace());
        assertEquals("sf", store.getWorkspace().getName());
        assertNotNull(store.getConnectionParameters());
        assertEquals(Boolean.TRUE, store.getEnabled());
        assertThat(
                store.getFeatureTypes().toURL().toString(),
                StringContains.containsString(
                        "/rest/workspaces/sf/datastores/sf/featuretypes.json"));
    }

    public @Test void testCreateShapefileDataStoreDefaults() {
        DataStoreResponse created =
                this.dataStores.createShapefileDataStore(
                        this.workspace.getName(), "shpstore_defaults", this.streamsShapefile);

        assertNotNull(created);
        assertEquals("shpstore_defaults", created.getName());
        assertEquals(this.workspace.getName(), created.getWorkspace().getName());
        Map<String, String> params =
                this.dataStores.toConnectionParameters(created.getConnectionParameters());
        assertEquals(params.toString(), 4, params.size());
        assertEquals("shapefile", params.get("filetype"));
        assertEquals("shape", params.get("fstype"));
        assertEquals("http://" + this.workspace.getName(), params.get("namespace"));
        assertEquals(this.streamsShapefile.toString(), params.get("url"));
    }

    public @Test void testCreateShapefileDataStore() {
        String uri = this.roadsShapefile.toString();
        Shapefile connectionParameters =
                new DataStoreParams.Shapefile()
                        .uri(uri)
                        .useMemoryMappedBuffer(true)
                        .useMemoryMaps(true)
                        .createSpatialIndex(true);
        String wsname = this.workspace.getName();
        DataStoreResponse created =
                this.dataStores.create(
                        wsname,
                        "shpStore",
                        "test shapefile based data store",
                        connectionParameters);
        assertNotNull(created);
        assertEquals("shpStore", created.getName());
        assertEquals(wsname, created.getWorkspace().getName());
        Map<String, String> params =
                this.dataStores.toConnectionParameters(created.getConnectionParameters());
        assertEquals("shapefile", params.get("filetype"));
        assertEquals("shape", params.get("fstype"));
        assertEquals("http://" + this.workspace.getName(), params.get("namespace"));
        assertEquals(uri, params.get("url"));
        assertEquals("true", params.get("cache and reuse memory maps"));
        assertEquals("true", params.get("memory mapped buffer"));
        assertEquals("true", params.get("create spatial index"));
    }

    public @Test void testCreateShapefileDataStoreDuplicateName() {
        final String wsname = this.workspace.getName();
        final URI uri = this.roadsShapefile;

        final String storeName = "will_duplicate_store_name";

        DataStoreResponse created =
                this.dataStores.createShapefileDataStore(wsname, storeName, uri);
        assertEquals(storeName, created.getName());

        DataStoreResponse nonDuplicateNameSameUri =
                this.dataStores.createShapefileDataStore(wsname, "nonDuplicateNameSameUri", uri);
        assertEquals("nonDuplicateNameSameUri", nonDuplicateNameSameUri.getName());

        try {
            this.dataStores.createShapefileDataStore(wsname, storeName, uri);
        } catch (ServerException.InternalServerError e) {
            // currently geoserver's API doesn't return a sensible error message when
            // attempting to create a store with a duplicate name
            assertTrue(true);
        }
    }

    private DataStoreResponse createShapefileDataStore(URI shpfile) {
        String wsname = this.workspace.getName();
        String uri = shpfile.toString();
        Shapefile connectionParameters =
                new DataStoreParams.Shapefile()
                        .uri(uri)
                        .useMemoryMappedBuffer(true)
                        .useMemoryMaps(true)
                        .createSpatialIndex(true);
        DataStoreResponse created =
                this.dataStores.create(
                        wsname,
                        "shpStore",
                        "test shapefile based data store",
                        connectionParameters);
        return created;
    }

    public @Test void testRenameShapefileDataStore() {
        DataStoreResponse created = this.createShapefileDataStore(this.roadsShapefile);
        String ws = created.getWorkspace().getName();
        assertEquals(this.workspace.getName(), ws);
        String newName = "renamed";
        DataStoreInfo info = new DataStoreInfo();
        info.setConnectionParameters(
                this.dataStores.toConnectionParameters(created.getConnectionParameters()));
        info.setName(newName);
        info.setDescription(created.getDescription());
        info.setEnabled(info.getEnabled());
        this.ex.expect(ServerException.NotFound.class);
        this.dataStores.update(ws, info);
    }

    public @Test void testUpdateShapefileDataStore() {
        String ws = this.workspace.getName();
        DataStoreResponse created =
                this.dataStores.createShapefileDataStore(ws, "streams", this.streamsShapefile);
        assertEquals(ws, created.getWorkspace().getName());

        assertNull(
                this.dataStores
                        .toConnectionParameters(created.getConnectionParameters())
                        .get("memory mapped buffer"));

        DataStoreInfo info = new DataStoreInfo();
        info.setConnectionParameters(
                this.dataStores.toConnectionParameters(created.getConnectionParameters()));
        info.getConnectionParameters().put("memory mapped buffer", "true");

        info.setName(created.getName());
        info.setDescription(created.getDescription());
        info.setEnabled(info.getEnabled());

        DataStoreResponse updated = this.dataStores.update(ws, info);
        Map<String, String> params =
                this.dataStores.toConnectionParameters(updated.getConnectionParameters());
        assertEquals("true", params.get("memory mapped buffer"));
    }

    public @Test void testDeleteShapefileDataStore() {
        String ws = this.workspace.getName();
        DataStoreResponse streams =
                this.dataStores.createShapefileDataStore(ws, "streams", this.streamsShapefile);
        DataStoreResponse roads =
                this.dataStores.createShapefileDataStore(ws, "roads", this.roadsShapefile);

        assertTrue(this.dataStores.findByWorkspaceAndName(ws, streams.getName()).isPresent());
        this.dataStores.deleteRecursive(ws, streams.getName());
        assertFalse(this.dataStores.findByWorkspaceAndName(ws, streams.getName()).isPresent());

        assertTrue(this.dataStores.findByWorkspaceAndName(ws, roads.getName()).isPresent());
        this.dataStores.deleteRecursive(ws, roads.getName());
        assertFalse(this.dataStores.findByWorkspaceAndName(ws, roads.getName()).isPresent());
    }
}
