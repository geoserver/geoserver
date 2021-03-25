package org.geoserver.restconfig.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.security.SecureRandom;
import org.geoserver.openapi.model.catalog.DataStoreInfo;
import org.geoserver.openapi.model.catalog.FeatureTypeInfo;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
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
@FixMethodOrder(MethodSorters.JVM)
public class FeatureTypesClientIT {

    public static @ClassRule IntegrationTestSupport support = new IntegrationTestSupport();

    public @Rule TestName testName = new TestName();
    public @Rule ExpectedException ex = ExpectedException.none();

    private SecureRandom rnd = new SecureRandom();

    // two workspaces, both have a "roads" and a "streams" shapefile datastore
    private WorkspaceSummary ws1, ws2;

    private URI roadsShapefile;

    private URI streamsShapefile;

    private WorkspacesClient workspaces;
    private DataStoresClient dataStores;
    private FeatureTypesClient featureTyes;

    public @Before void before() {
        Assume.assumeTrue(support.isAlive());
        this.workspaces = support.client().workspaces();
        this.dataStores = support.client().dataStores();
        this.featureTyes = support.client().featureTypes();

        String wsname1 =
                String.format("%s-ws1-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));
        String wsname2 =
                String.format("%s-ws2-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));

        this.workspaces.create(wsname1);
        this.workspaces.create(wsname2);
        this.ws1 = workspaces.findByName(wsname1).get();
        this.ws2 = workspaces.findByName(wsname2).get();

        this.roadsShapefile = support.getRoadsShapefile();
        this.streamsShapefile = support.getStreamsShapefile();

        this.dataStores.createShapefileDataStore(wsname1, "roadsStoreWs1", roadsShapefile);
        this.dataStores.createShapefileDataStore(wsname1, "streams", streamsShapefile);

        this.dataStores.createShapefileDataStore(wsname2, "roadsStoreWs2", roadsShapefile);
        this.dataStores.createShapefileDataStore(wsname2, "streams", streamsShapefile);

        this.roadsShapefile = support.getRoadsShapefile();
        this.streamsShapefile = support.getStreamsShapefile();
    }

    public @After void after() {
        if (this.ws1 != null) {
            this.workspaces.deleteRecursively(this.ws1.getName());
        }
        if (this.ws2 != null) {
            this.workspaces.deleteRecursively(this.ws2.getName());
        }
    }

    @Ignore // we're setting name to nativeName if not given, since 2.15.2 requires
    // nativeName
    public @Test void createRequiresName() {
        FeatureTypeInfo roads =
                new FeatureTypeInfo()
                        .nativeName("roads")
                        .store(new DataStoreInfo().name("roadsStoreWs1"));
        roads.setName(null);
        this.ex.expect(ServerException.BadRequest.class);
        this.featureTyes.create(ws1.getName(), roads);
    }

    public @Test void createRequiresStore() {
        FeatureTypeInfo roads = new FeatureTypeInfo().store(null);
        roads.setName("roads123");
        roads.setNativeName("roads");
        this.ex.expect(IllegalArgumentException.class);
        this.ex.expectMessage("Target store not provided");
        this.featureTyes.create(ws1.getName(), roads);
    }

    public @Test void createBadStoreName() {
        FeatureTypeInfo roads =
                new FeatureTypeInfo()
                        .nativeName("roads")
                        .store(new DataStoreInfo().name("bad-store-name"));
        this.ex.expect(ServerException.NotFound.class);
        //		server is not sending this message, but it logs it
        //		ex.expectMessage("No such data store");
        this.featureTyes.create(ws1.getName(), roads);
    }

    /**
     * It should be possible to create two feature types with the same store and FT name on
     * different workspaces. Fails with geoserver < 2.15.4, bug report:
     * https://osgeo-org.atlassian.net/browse/GEOS-9190
     */
    public @Test void createSameStoreNameDifferentWorkspace() {
        final String storeName = "streams"; // created above in before()
        final String nativeFeatureTypeName = "streams";

        FeatureTypeInfo streamsWorkspace1 =
                new FeatureTypeInfo()
                        .nativeName(nativeFeatureTypeName)
                        .store(new DataStoreInfo().name(storeName));
        FeatureTypeInfo streamsWorkspace2 =
                new FeatureTypeInfo()
                        .nativeName(nativeFeatureTypeName)
                        .store(new DataStoreInfo().name(storeName));
        {
            FeatureTypeInfo ft1ws1 = this.featureTyes.create(ws1.getName(), streamsWorkspace1);
            assertEquals(nativeFeatureTypeName, ft1ws1.getName());
            assertEquals(storeName, ft1ws1.getStore().getName());
            assertEquals(ws1.getName(), ft1ws1.getStore().getWorkspace().getName());
            assertEquals(ws1.getName(), ft1ws1.getNamespace().getPrefix());
        }
        {
            FeatureTypeInfo ft1ws2 = this.featureTyes.create(ws2.getName(), streamsWorkspace2);
            assertEquals(nativeFeatureTypeName, ft1ws2.getName());
            assertEquals(storeName, ft1ws2.getStore().getName());
            assertEquals(ws2.getName(), ft1ws2.getStore().getWorkspace().getName());
            assertEquals(ws2.getName(), ft1ws2.getNamespace().getPrefix());
        }
        {
            FeatureTypeInfo ft2ws1 =
                    this.featureTyes.create(ws1.getName(), streamsWorkspace1.name("name2"));
            assertEquals("name2", ft2ws1.getName());
            assertEquals(storeName, ft2ws1.getStore().getName());
            assertEquals(ws1.getName(), ft2ws1.getStore().getWorkspace().getName());
            assertEquals(ws1.getName(), ft2ws1.getNamespace().getPrefix());
        }
        {
            FeatureTypeInfo ft2ws2 =
                    this.featureTyes.create(ws2.getName(), streamsWorkspace2.name("name2"));
            assertEquals("name2", ft2ws2.getName());
            assertEquals(storeName, ft2ws2.getStore().getName());
            assertEquals(ws2.getName(), ft2ws2.getStore().getWorkspace().getName());
            assertEquals(ws2.getName(), ft2ws2.getNamespace().getPrefix());
        }
    }

    public @Test void createMinimalInformation() {
        FeatureTypeInfo roadsws1 =
                new FeatureTypeInfo()
                        .name("roads")
                        .nativeName("roads")
                        .store(new DataStoreInfo().name("roadsStoreWs1"));
        FeatureTypeInfo roadsws2 =
                new FeatureTypeInfo()
                        .name("roads")
                        .nativeName("roads")
                        .store(new DataStoreInfo().name("roadsStoreWs2"));

        this.featureTyes.create(ws1.getName(), roadsws1);
        this.featureTyes.create(ws2.getName(), roadsws2);
    }

    public @Test void createMinimalInformationNativeAndPublishedName() {
        FeatureTypeInfo roadsws1 =
                new FeatureTypeInfo()
                        .nativeName("roads")
                        .name("roads-123")
                        .store(new DataStoreInfo().name("roadsStoreWs1"));
        FeatureTypeInfo roadsws2 =
                new FeatureTypeInfo()
                        .nativeName("roads")
                        .name("roads-345")
                        .store(new DataStoreInfo().name("roadsStoreWs2"));

        this.featureTyes.create(ws1.getName(), roadsws1);
        this.featureTyes.create(ws2.getName(), roadsws2);
    }

    public @Test void createFullInformation() {
        FeatureTypeInfo roads = new FeatureTypeInfo().name("roads");
        roads.setName("roads-renamed");
        roads.setNativeName("roads");
        roads.setTitle("title");
        roads.setAbstract("abstract");
        roads.setStore(new DataStoreInfo().name("roadsStoreWs1"));
        roads.setSrs("EPSG:4326");

        FeatureTypeInfo created = featureTyes.create(ws1.getName(), roads);
        assertNotNull(created);
        assertEquals(roads.getName(), created.getName());
        assertEquals(roads.getNativeName(), created.getNativeName());
        assertEquals(roads.getTitle(), created.getTitle());
        assertEquals(roads.getAbstract(), created.getAbstract());
        assertEquals(roads.getStore().getName(), created.getStore().getName());
        assertEquals(ws1.getName(), created.getStore().getWorkspace().getName());
        assertEquals(roads.getSrs(), created.getSrs());
    }

    public @Test void update() {
        FeatureTypeInfo roadsws1 =
                new FeatureTypeInfo()
                        .name("roads")
                        .nativeName("roads")
                        .store(new DataStoreInfo().name("roadsStoreWs1"));
        FeatureTypeInfo roadsws2 =
                new FeatureTypeInfo()
                        .name("roads")
                        .nativeName("roads")
                        .store(new DataStoreInfo().name("roadsStoreWs2"));

        this.featureTyes.create(ws1.getName(), roadsws1);
        this.featureTyes.create(ws2.getName(), roadsws2);

        roadsws1.setName("new name");
        roadsws1.setTitle("New Title");
        String currentName = "roads";
        FeatureTypeInfo updated = this.featureTyes.update(ws1.getName(), currentName, roadsws1);
        assertEquals("new name", updated.getName());
        assertEquals("New Title", updated.getTitle());
    }
}
