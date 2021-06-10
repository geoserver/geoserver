package org.geoserver.restconfig.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.openapi.model.catalog.CoverageInfo;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo.TypeEnum;
import org.geoserver.openapi.model.catalog.ProjectionPolicy;
import org.geoserver.openapi.v1.model.CoverageStoreResponse;
import org.geoserver.openapi.v1.model.Layer;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.hamcrest.core.StringStartsWith;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

/** Integration test suite for {@link DataStoresClient} */
@Slf4j
@FixMethodOrder(MethodSorters.JVM)
public class CoveragesClientIT {

    public static @ClassRule IntegrationTestSupport support = new IntegrationTestSupport();

    public @Rule TestName testName = new TestName();
    public @Rule ExpectedException ex = ExpectedException.none();

    private SecureRandom rnd = new SecureRandom();

    // two workspaces
    private WorkspaceSummary ws1, ws2;

    private URI sfdemURI;

    private WorkspacesClient workspaces;
    private CoverageStoresClient coverageStores;
    private CoveragesClient coverages;

    private CoverageStoreResponse sfdemStore;

    public @Before void before() {
        Assume.assumeTrue(support.isAlive());
        this.workspaces = support.client().workspaces();
        this.coverageStores = support.client().coverageStores();
        this.coverages = support.client().coverages();

        String wsname1 =
                String.format("%s-ws1-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));
        String wsname2 =
                String.format("%s-ws2-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));

        this.workspaces.create(wsname1);
        this.workspaces.create(wsname2);
        this.ws1 = workspaces.findByName(wsname1).get();
        this.ws2 = workspaces.findByName(wsname2).get();

        this.sfdemURI = support.getSFDemGeoTiff();

        this.sfdemStore =
                this.coverageStores.create(
                        wsname1, "sfdem", "Test geotiff", TypeEnum.GEOTIFF, sfdemURI.toString());
    }

    public @After void after() {
        tryDelete(this.ws1);
        tryDelete(this.ws2);
    }

    private void tryDelete(WorkspaceSummary ws) {
        if (ws != null) {
            try {
                this.workspaces.deleteRecursively(ws.getName());
            } catch (RuntimeException e) {
                log.warn("Error deleting workspace {} at test tear down phase", ws.getName(), e);
            }
        }
    }

    private void expect(
            Runnable cmd, Class<? extends Exception> expectedException, String expectedMessage) {
        try {
            cmd.run();
            fail(String.format("Expected %s", expectedException.getName()));
        } catch (Throwable t) {
            assertThat(t, IsInstanceOf.instanceOf(expectedException));
            assertThat(t.getMessage(), StringContains.containsString(expectedMessage));
        }
    }

    public @Test void createPreconditions() {
        CoverageInfo sfdem = new CoverageInfo();

        Runnable cmd = () -> this.coverages.create(ws1.getName(), sfdem);

        expect(cmd, NullPointerException.class, "nativeCoverageName is null");

        sfdem.setNativeCoverageName("sfdem");
        expect(cmd, NullPointerException.class, "store is null");

        sfdem.setStore(new CoverageStoreInfo());
        expect(cmd, NullPointerException.class, "store name is null");
    }

    public @Test void createByStoreMinimalInfo() {
        CoverageInfo sfdem = new CoverageInfo().nativeCoverageName("sfdem");
        final String storeName = sfdemStore.getName();
        CoverageInfo created = this.coverages.create(ws1.getName(), storeName, sfdem);
        assertNotNull(created);

        assertEquals("sfdem", created.getNativeName());
        assertEquals("sfdem", created.getName());
        assertEquals("sfdem", created.getTitle());
        assertNull("abstract", created.getAbstract());
        assertEquals("Generated from GeoTIFF", created.getDescription());
        assertEquals("GeoTIFF", created.getNativeFormat());

        assertNotNull(created.getStore());
        assertEquals(sfdemStore.getName(), created.getStore().getName());
        assertNotNull(created.getNamespace());
        assertEquals(ws1.getName(), created.getNamespace().getPrefix());
        assertNotNull(created.getNamespace().getUri());
        assertNotNull(created.getStore().getWorkspace());
        assertEquals(ws1.getName(), created.getStore().getWorkspace().getName());

        // automatically set by geoserver...
        assertNotNull(created.getNativeBoundingBox());
        assertEquals("EPSG:26713", created.getSrs());
        assertThat(
                created.getNativeCRS(),
                StringStartsWith.startsWith("PROJCS[\"NAD27 / UTM zone 13N\""));
        assertEquals(ProjectionPolicy.REPROJECT_TO_DECLARED, created.getProjectionPolicy());
        assertEquals(Boolean.TRUE, created.getEnabled());
        assertEquals(Collections.singletonList("EPSG:26713"), created.getRequestSRS());
        assertEquals(Collections.singletonList("EPSG:26713"), created.getResponseSRS());
    }

    public @Test void createByStoreFixesInvalidStoreInfo() {
        CoverageStoreInfo invalidStore = new CoverageStoreInfo().name("notTheRequestedStore");
        CoverageInfo sfdem = new CoverageInfo().nativeCoverageName("sfdem").store(invalidStore);

        final String storeName = sfdemStore.getName();
        CoverageInfo created = this.coverages.create(ws1.getName(), storeName, sfdem);
        assertNotNull(created);

        assertSame("Argument object changed", invalidStore, sfdem.getStore());
        assertEquals("Argument object changed", "notTheRequestedStore", invalidStore.getName());

        assertNotNull(created.getStore());
        assertEquals(storeName, created.getStore().getName());
    }

    public @Test void createMinimalInformation() {
        CoverageInfo sfdem =
                new CoverageInfo()
                        .nativeCoverageName("sfdem")
                        .store(new CoverageStoreInfo().name(sfdemStore.getName()));

        CoverageInfo created = this.coverages.create(ws1.getName(), sfdem);
        assertNotNull(created);

        assertEquals("sfdem", created.getNativeName());
        assertEquals("sfdem", created.getName());
        assertEquals("sfdem", created.getTitle());
        assertNull("abstract", created.getAbstract());
        assertEquals("Generated from GeoTIFF", created.getDescription());
        assertEquals("GeoTIFF", created.getNativeFormat());

        assertNotNull(created.getStore());
        assertEquals(sfdemStore.getName(), created.getStore().getName());
        assertNotNull(created.getNamespace());
        assertEquals(ws1.getName(), created.getNamespace().getPrefix());
        assertNotNull(created.getNamespace().getUri());
        assertNotNull(created.getStore().getWorkspace());
        assertEquals(ws1.getName(), created.getStore().getWorkspace().getName());

        // automatically set by geoserver...
        assertNotNull(created.getNativeBoundingBox());
        assertEquals("EPSG:26713", created.getSrs());
        assertThat(
                created.getNativeCRS(),
                StringStartsWith.startsWith("PROJCS[\"NAD27 / UTM zone 13N\""));
        assertEquals(ProjectionPolicy.REPROJECT_TO_DECLARED, created.getProjectionPolicy());
        assertEquals(Boolean.TRUE, created.getEnabled());
        assertEquals(Collections.singletonList("EPSG:26713"), created.getRequestSRS());
        assertEquals(Collections.singletonList("EPSG:26713"), created.getResponseSRS());
    }

    public @Test void createMinimalInformationNativeAndPublishedName() {
        CoverageInfo sfdem =
                new CoverageInfo() //
                        .nativeCoverageName("sfdem") //
                        .name("PublishedName") //
                        .store(new CoverageStoreInfo().name(sfdemStore.getName()));

        CoverageInfo created = this.coverages.create(ws1.getName(), sfdem);
        assertNotNull(created);

        assertEquals("PublishedName", created.getNativeName());
        assertEquals("PublishedName", created.getName());
        assertEquals("sfdem", created.getNativeCoverageName());

        // automatically set by geoserver...
        assertNotNull(created.getNativeBoundingBox());
        assertEquals("EPSG:26713", created.getSrs());
        assertThat(
                created.getNativeCRS(),
                StringStartsWith.startsWith("PROJCS[\"NAD27 / UTM zone 13N\""));
        assertEquals(ProjectionPolicy.REPROJECT_TO_DECLARED, created.getProjectionPolicy());
        assertEquals(Boolean.TRUE, created.getEnabled());
        assertEquals(Collections.singletonList("EPSG:26713"), created.getRequestSRS());
        assertEquals(Collections.singletonList("EPSG:26713"), created.getResponseSRS());
    }

    /**
     * This is a bug in geoserver, providing anything other than a simple name makes it ignore the
     * provided "published name" and not automatically compute other fields, but uses exactly what
     * was provided (it does compute the bounds and native CRS though)
     */
    public @Test void createWithTitleAndAbstractDoesNotAtuoComputeSomeFields() {
        CoverageInfo sfdem =
                new CoverageInfo()
                        .nativeCoverageName("sfdem")
                        .name("PublishedName")
                        .title("title")
                        ._abstract("abstract")
                        .description("description")
                        .store(new CoverageStoreInfo().name(sfdemStore.getName()));
        sfdem.setName(null);
        CoverageInfo created = this.coverages.create(ws1.getName(), sfdem);
        assertNotNull(created);

        assertEquals("sfdem", created.getNativeCoverageName());
        assertEquals("title", created.getTitle());
        assertEquals("abstract", created.getAbstract());
        assertEquals("description", created.getDescription());

        assertEquals("Should have been 'PublishedName'", "sfdem", created.getName());
        assertNull("Should have been 'GeoTIFF'", created.getNativeFormat());

        assertNotNull(created.getStore());
        assertEquals(sfdemStore.getName(), created.getStore().getName());
        assertNotNull(created.getNamespace());
        assertEquals(ws1.getName(), created.getNamespace().getPrefix());
        assertNotNull(created.getNamespace().getUri());
        assertNotNull(created.getStore().getWorkspace());
        assertEquals(ws1.getName(), created.getStore().getWorkspace().getName());

        // automatically set by geoserver, bad defaults due to bug...
        assertNotNull(created.getNativeBoundingBox());
        assertEquals("EPSG:26713", created.getSrs());
        assertThat(
                created.getNativeCRS(),
                StringStartsWith.startsWith("PROJCS[\"NAD27 / UTM zone 13N\""));
        assertEquals(
                "Should have been REPROJECT_TO_DECLARED",
                ProjectionPolicy.FORCE_DECLARED,
                created.getProjectionPolicy());
        assertEquals(Boolean.TRUE, created.getEnabled());
        assertNull("Should have been [EPSG:26713]", created.getRequestSRS());
        assertNull("Should have been [EPSG:26713]", created.getResponseSRS());
    }

    public @Test void createBadStoreName() {

        CoverageInfo sfdem = new CoverageInfo().nativeCoverageName("sfdem");

        this.ex.expect(ServerException.NotFound.class);
        this.coverages.create(ws1.getName(), "badStore", sfdem);
    }

    /**
     * It should be possible to create two coverages with the same store and native coverage name on
     * different workspaces. This currently fails, although it was fixed for FeatureTypes with
     * geoserver >= 2.15.4,bug report: https://osgeo-org.atlassian.net/browse/GEOS-9190
     */
    @Ignore
    public @Test void createSameStoreNameDifferentWorkspace() {
        final CoverageStoreResponse store1 = this.sfdemStore;
        final String storeName = store1.getName();
        final String coverageName = "sfdem";
        final String workspace1 = this.ws1.getName();
        final String workspace2 = this.ws2.getName();

        CoverageInfo requestBody =
                new CoverageInfo()
                        .nativeCoverageName(coverageName)
                        .store(new CoverageStoreInfo().name(storeName));
        {
            CoverageInfo c1ws1 = this.coverages.create(workspace1, storeName, requestBody);
            assertEquals(coverageName, c1ws1.getName());
            assertEquals(storeName, c1ws1.getStore().getName());
            assertEquals(workspace1, c1ws1.getStore().getWorkspace().getName());
            assertEquals(workspace1, c1ws1.getNamespace().getPrefix());
        }
        // create a store with the same name than store1
        CoverageStoreResponse store2 =
                this.coverageStores.create(
                        workspace2,
                        storeName,
                        store1.getDescription(),
                        TypeEnum.GEOTIFF,
                        sfdemURI.toString());
        assertNotNull(store2);

        {
            CoverageInfo c1ws2 = this.coverages.create(workspace2, storeName, requestBody);
            assertEquals(coverageName, c1ws2.getName());
            assertEquals(storeName, c1ws2.getStore().getName());
            assertEquals(workspace2, c1ws2.getStore().getWorkspace().getName());
            assertEquals(workspace2, c1ws2.getNamespace().getPrefix());
        }
        requestBody.name("name2");
        {
            this.ex.expect(IllegalStateException.class);
            this.ex.expectMessage("not found right after creation");
            CoverageInfo c2ws1 = this.coverages.create(workspace1, storeName, requestBody);
            assertEquals("name2", c2ws1.getName());
            assertEquals(storeName, c2ws1.getStore().getName());
            assertEquals(workspace1, c2ws1.getStore().getWorkspace().getName());
            assertEquals(workspace1, c2ws1.getNamespace().getPrefix());
        }
        {
            CoverageInfo c2ws2 = this.coverages.create(workspace2, storeName, requestBody);
            assertEquals("name2", c2ws2.getName());
            assertEquals(storeName, c2ws2.getStore().getName());
            assertEquals(workspace2, c2ws2.getStore().getWorkspace().getName());
            assertEquals(workspace2, c2ws2.getNamespace().getPrefix());
        }
    }

    /**
     * Due to a bug in some geoserver versions, can't create a coverage with a name that already
     * exists in another workspace/coveragestore. Nonetheless, we'll make sure to provide unique
     * coverage names, but creating coverages for the same geotiff file in different workspaces,
     * re-using the each workspace coveragestore, should be possible
     */
    @Ignore
    public @Test void createSameStoreNameDifferentWorkspaceUniqueCoverageName() {
        final CoverageStoreResponse store1 = this.sfdemStore;
        final String storeName1 = store1.getName();
        final String storeName2 = storeName1 + "_2";
        final String nativeCoverageName = "sfdem";
        final String workspace1 = this.ws1.getName();
        final String workspace2 = this.ws2.getName();

        // create a store with the same name than store1
        CoverageStoreResponse store2 =
                this.coverageStores.create(
                        workspace2,
                        storeName2,
                        store1.getDescription(),
                        TypeEnum.GEOTIFF,
                        sfdemURI.toString());
        assertNotNull(store2);

        CoverageInfo requestBodyWs1 =
                new CoverageInfo()
                        .nativeCoverageName(nativeCoverageName) //
                        .store(new CoverageStoreInfo().name(storeName1)) //
                        .name("sfdem-ws1");
        CoverageInfo requestBodyWs2 =
                new CoverageInfo()
                        .nativeCoverageName(nativeCoverageName) //
                        .store(new CoverageStoreInfo().name(storeName1)) //
                        .name("sfdem-ws2");
        {
            CoverageInfo c1ws1 = this.coverages.create(workspace1, storeName1, requestBodyWs1);
            assertEquals("sfdem-ws1", c1ws1.getName());
            assertEquals(storeName1, c1ws1.getStore().getName());
            assertEquals(workspace1, c1ws1.getStore().getWorkspace().getName());
            assertEquals(workspace1, c1ws1.getNamespace().getPrefix());
        }

        {
            CoverageInfo c1ws2 = this.coverages.create(workspace2, storeName2, requestBodyWs2);
            assertEquals("sfdem-ws2", c1ws2.getName());
            assertEquals(storeName2, c1ws2.getStore().getName());
            assertEquals(workspace2, c1ws2.getStore().getWorkspace().getName());
            assertEquals(workspace2, c1ws2.getNamespace().getPrefix());
        }
        {
            requestBodyWs1.name("sfdem-ws1-2");
            this.ex.expect(IllegalStateException.class);
            this.ex.expectMessage("not found right after creation");
            CoverageInfo c2ws1 = this.coverages.create(workspace1, storeName1, requestBodyWs1);
            assertEquals("sfdem-ws1-2", c2ws1.getName());
            assertEquals(storeName1, c2ws1.getStore().getName());
            assertEquals(workspace1, c2ws1.getStore().getWorkspace().getName());
            assertEquals(workspace1, c2ws1.getNamespace().getPrefix());
        }
        {
            requestBodyWs2.name("sfdem-ws2-2");
            CoverageInfo c2ws2 = this.coverages.create(workspace2, storeName2, requestBodyWs2);
            assertEquals("name2", c2ws2.getName());
            assertEquals("sfdem-ws2-2", c2ws2.getStore().getName());
            assertEquals(workspace2, c2ws2.getStore().getWorkspace().getName());
            assertEquals(workspace2, c2ws2.getNamespace().getPrefix());
        }
    }

    public @Test void createSameCoverageDifferentPublishedName() {
        final String storeName = this.sfdemStore.getName();
        final String nativeName = "sfdem";
        final String workspace = this.ws1.getName();

        CoverageInfo requestBody = new CoverageInfo().nativeCoverageName(nativeName);

        CoverageInfo coverage1 = this.coverages.create(workspace, storeName, requestBody);
        assertEquals(nativeName, coverage1.getName());

        requestBody.setName("PublishedName1");
        CoverageInfo coverage2 = this.coverages.create(workspace, storeName, requestBody);
        assertEquals("PublishedName1", coverage2.getName());
        assertEquals(workspace, coverage2.getNamespace().getPrefix());

        requestBody.setName("PublishedName2");
        CoverageInfo coverage3 = this.coverages.create(workspace, storeName, requestBody);
        assertEquals("PublishedName2", coverage3.getName());
        assertEquals(workspace, coverage3.getNamespace().getPrefix());
    }

    public @Test void createCoverageAlsoCreatesLayer() {
        CoverageInfo createBody =
                new CoverageInfo() //
                        .nativeCoverageName("sfdem") //
                        .name("PublishedName") //
                        .store(new CoverageStoreInfo().name(sfdemStore.getName()));

        CoverageInfo created = this.coverages.create(ws1.getName(), createBody);
        assertNotNull(created);

        assertEquals("PublishedName", created.getNativeName());
        assertEquals("PublishedName", created.getName());
        assertEquals("sfdem", created.getNativeCoverageName());

        LayersClient layers = support.client().layers();
        Optional<Layer> layer = layers.getLayer(this.ws1.getName(), created.getName());
        assertTrue(layer.isPresent());
        assertEquals("PublishedName", layer.get().getName());
    }

    public @Test void updateBasicMetadata_UseNewCoverageInfo() {
        final String workspace = this.ws1.getName();
        final String storeName = this.sfdemStore.getName();

        CoverageInfo sfdem =
                new CoverageInfo() //
                        .nativeCoverageName("sfdem") //
                        .store(new CoverageStoreInfo().name(storeName));

        CoverageInfo created = this.coverages.create(ws1.getName(), sfdem);
        assertNotNull(created);
        assertEquals("sfdem", created.getName());

        CoverageInfo updateRequestBody =
                new CoverageInfo()
                        .name("UpdatedName")
                        .title("Updated Title")
                        ._abstract("Updated Abstract");

        CoverageInfo updated =
                this.coverages.update(workspace, storeName, created.getName(), updateRequestBody);
        assertEquals("UpdatedName", updated.getName());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Abstract", updated.getAbstract());
    }

    public @Test void deleteRecursively() {
        final String workspace = this.ws1.getName();
        final String storeName = this.sfdemStore.getName();

        CoverageInfo sfdem =
                new CoverageInfo() //
                        .nativeCoverageName("sfdem") //
                        .store(new CoverageStoreInfo().name(storeName));

        CoverageInfo created = this.coverages.create(ws1.getName(), sfdem);
        assertNotNull(created);

        Optional<Layer> layer;
        layer = support.client().layers().getLayer(workspace, created.getName());
        assertTrue(layer.isPresent());

        this.coverages.deleteRecursively(workspace, storeName, created.getName());

        layer = support.client().layers().getLayer(workspace, created.getName());
        assertFalse(layer.isPresent());
    }
}
