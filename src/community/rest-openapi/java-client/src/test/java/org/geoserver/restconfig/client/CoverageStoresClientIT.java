package org.geoserver.restconfig.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo.TypeEnum;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.model.CoverageStoreResponse;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/** Integration test suite for {@link CoverageStoresClient} */
public class CoverageStoresClientIT {

    public @Rule IntegrationTestSupport support = new IntegrationTestSupport();

    private WorkspacesClient workspaces;
    private CoverageStoresClient coverages;

    public @Rule TestName testName = new TestName();

    private SecureRandom rnd = new SecureRandom();

    private WorkspaceSummary workspace, workspace2;

    private URI sfdemURI;

    public @Before void before() {
        Assume.assumeTrue(this.support.isAlive());
        this.workspaces = this.support.client().workspaces();
        this.coverages = this.support.client().coverageStores();
        String wsname =
                String.format("%s-%d", this.testName.getMethodName(), this.rnd.nextInt((int) 1e6));
        String wsname2 = wsname + "_2";

        this.workspaces.create(wsname);
        this.workspaces.create(wsname2);
        this.workspace = this.workspaces.findByName(wsname).get();
        this.workspace2 = this.workspaces.findByName(wsname2).get();
        this.sfdemURI = this.support.getSFDemGeoTiff();
    }

    public @After void after() {
        try {
            if (this.workspace != null) {
                this.workspaces.deleteRecursively(this.workspace.getName());
            }
        } finally {
            if (this.workspace2 != null) {
                this.workspaces.deleteRecursively(this.workspace2.getName());
            }
        }
    }

    public @Test void testFindByWorkspace_empty() {
        List<Link> stores = this.coverages.findByWorkspace("tiger");
        assertNotNull(stores);
        assertTrue(stores.isEmpty());
    }

    public @Test void testFindByWorkspace() {
        List<Link> stores = this.coverages.findByWorkspace("sf");
        assertEquals(1, stores.size());
        Link storeLink = stores.get(0);
        assertEquals("sfdem", storeLink.getName());

        stores = this.coverages.findByWorkspace("topp");
        assertEquals(0, stores.size());
    }

    public @Test void testGetNotFound() {
        Optional<CoverageStoreResponse> res;
        res = this.coverages.findByWorkspaceAndName("tiger", "nyc2");
        assertFalse(res.isPresent());
        res = this.coverages.findByWorkspaceAndName("sf", "sfdem2");
        assertFalse(res.isPresent());
    }

    public @Test void testGet() throws MalformedURLException {
        Optional<CoverageStoreResponse> res = this.coverages.findByWorkspaceAndName("sf", "sfdem");
        assertTrue(res.isPresent());
        CoverageStoreResponse store = res.get();
        assertEquals("sfdem", store.getName());
        assertNotNull(store.getWorkspace());
        assertEquals("sf", store.getWorkspace().getName());
        assertEquals("GeoTIFF", store.getType());
        assertEquals(Boolean.TRUE, store.getEnabled());
        assertNotNull(store.getCoverages());
        assertEquals("file:data/sf/sfdem.tif", store.getUrl());
    }

    public @Test void testCreateGeoTiffCoverageStore() {
        String uri = this.sfdemURI.toString();
        String wsname = this.workspace.getName();
        CoverageStoreResponse created =
                this.coverages.create(
                        wsname,
                        "geotiffStore",
                        "test geotiff based data store",
                        TypeEnum.GEOTIFF,
                        uri);
        assertNotNull(created);
        assertEquals("geotiffStore", created.getName());
        assertEquals(wsname, created.getWorkspace().getName());
        assertEquals("GeoTIFF", created.getType());
        assertEquals(uri, created.getUrl());
    }

    public @Test void testCreateGeoTiffCoverageStoreDuplicateName() {
        String uri = this.sfdemURI.toString();
        String wsname = this.workspace.getName();
        CoverageStoreResponse created =
                this.coverages.create(
                        wsname,
                        "geotiffStore",
                        "test geotiff based data store",
                        TypeEnum.GEOTIFF,
                        uri);
        assertNotNull(created);
        assertEquals("geotiffStore", created.getName());
        assertEquals(wsname, created.getWorkspace().getName());
        assertEquals("GeoTIFF", created.getType());
        assertEquals(uri, created.getUrl());
        try {
            this.coverages.create(
                    wsname, "geotiffStore", "test geotiff based data store", TypeEnum.GEOTIFF, uri);
        } catch (ServerException.InternalServerError e) {
            // currently geoserver's API doesn't return a sensible error message when
            // attempting to create a store with a duplicate name
            assertTrue(true);
        }
    }

    private CoverageStoreResponse createGeoTiffStore(URI geotiffUri) {
        String wsname = this.workspace.getName();
        String uri = geotiffUri.toString();
        return this.coverages.create(
                wsname, "geotiffStore", "test geotiff based data store", TypeEnum.GEOTIFF, uri);
    }

    public @Test void testRenameCoverageStore() {
        CoverageStoreResponse created = this.createGeoTiffStore(this.sfdemURI);
        String ws = created.getWorkspace().getName();
        assertEquals(this.workspace.getName(), ws);
        String newName = "renamed";
        CoverageStoreInfo info = new CoverageStoreInfo();
        info.setName(newName);

        info.setWorkspace(new WorkspaceInfo().name(created.getWorkspace().getName()));
        info.setDescription(created.getDescription());
        info.setEnabled(created.getEnabled());
        info.setType(TypeEnum.fromValue(created.getType()));
        info.setUrl(created.getUrl());

        CoverageStoreResponse updated = this.coverages.update(ws, created.getName(), info);
        assertEquals(newName, updated.getName());
        assertEquals(ws, updated.getWorkspace().getName());
    }

    public @Test void testChangeCoverageStoreWorkspace() {
        CoverageStoreResponse created = this.createGeoTiffStore(this.sfdemURI);
        String ws = created.getWorkspace().getName();
        assertEquals(this.workspace.getName(), ws);
        CoverageStoreInfo info = new CoverageStoreInfo();

        info.setWorkspace(new WorkspaceInfo().name(this.workspace2.getName()));

        info.setName(created.getName());
        info.setDescription(created.getDescription());
        info.setEnabled(created.getEnabled());
        info.setType(TypeEnum.fromValue(created.getType()));
        info.setUrl(created.getUrl());

        CoverageStoreResponse updated = this.coverages.update(ws, created.getName(), info);
        assertEquals(this.workspace2.getName(), updated.getWorkspace().getName());
    }

    public @Test void testUpdateCoverageStore() {
        CoverageStoreResponse created = this.createGeoTiffStore(this.sfdemURI);
        String ws = created.getWorkspace().getName();
        assertEquals(this.workspace.getName(), ws);
        CoverageStoreInfo info = new CoverageStoreInfo();

        info.setWorkspace(new WorkspaceInfo().name(created.getWorkspace().getName()));
        info.setName(created.getName());
        info.setDescription(created.getDescription() + " changed");
        info.setEnabled(!created.getEnabled());
        info.setType(TypeEnum.fromValue(created.getType()));
        info.setUrl(created.getUrl());

        CoverageStoreResponse updated = this.coverages.update(info);
        assertEquals(info.getName(), updated.getName());
        assertEquals(info.getDescription(), updated.getDescription());
        assertEquals(info.getEnabled(), updated.getEnabled());
        assertEquals(info.getType().getValue(), updated.getType());
        assertEquals(info.getUrl(), updated.getUrl());
        assertEquals(info.getWorkspace().getName(), updated.getWorkspace().getName());
    }

    public @Test void testDeleteCoverageStore() {
        CoverageStoreResponse created = this.createGeoTiffStore(this.sfdemURI);
        String ws = created.getWorkspace().getName();
        String name = created.getName();
        assertEquals(this.workspace.getName(), ws);

        assertTrue(this.coverages.findByWorkspaceAndName(ws, name).isPresent());
        this.coverages.deleteRecursive(created.getWorkspace().getName(), created.getName());
        assertFalse(this.coverages.findByWorkspaceAndName(ws, name).isPresent());
    }
}
