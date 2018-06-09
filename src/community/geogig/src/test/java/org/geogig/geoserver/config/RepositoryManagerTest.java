/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
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
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.porcelain.BranchCreateOp;
import org.locationtech.geogig.porcelain.BranchListOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Repository;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/** Unit tests for the RepositoryManager */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class RepositoryManagerTest extends GeoServerSystemTestSupport {

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    private RepositoryManager repoManager;

    private static final Random rnd = new Random();

    @Before
    public void before() throws Exception {
        this.repoManager = RepositoryManager.get();
    }

    @After
    public void after() {
        RepositoryManager.close();
        getCatalog().dispose();
    }

    @Test
    public void testGet() {
        assertNotNull(repoManager);
        RepositoryManager repoManager2 = RepositoryManager.get();
        assertNotNull(repoManager2);
        assertEquals(repoManager, repoManager2);
    }

    @Test
    public void testCloseTwice() {
        assertNotNull(repoManager);
        RepositoryManager.close();
        // second close will happen in after()
    }

    @Test
    public void testCreateAndGetRepos() throws IOException {
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        Repository repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        RepositoryInfo info1 = saveRepository(repo);
        repo.close();

        List<RepositoryInfo> repositories = repoManager.getAll();
        assertEquals(1, repositories.size());
        assertEquals(info1, repositories.get(0));

        hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo2");
        repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        URI repoURI = repo.getLocation();
        RepositoryInfo info2 = saveRepository(repo);
        repo.close();

        // creating the same repo should return the one we already made
        repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertTrue(repo.isOpen());
        assertEquals(repoURI, repo.getLocation());
        repo.close();

        repositories = repoManager.getAll();
        assertEquals(2, repositories.size());
        assertTrue(repositories.contains(info1));
        assertTrue(repositories.contains(info2));

        RepositoryInfo info1Get = repoManager.get(info1.getId());
        assertEquals(info1, info1Get);
        RepositoryInfo info2Get = repoManager.get(info2.getId());
        assertEquals(info2, info2Get);

        String randomUUID = UUID.randomUUID().toString();
        try {
            repoManager.get(randomUUID);
            fail();
        } catch (NoSuchElementException e) {
            // expected;
            assertEquals("Repository not found: " + randomUUID, e.getMessage());
        }

        info1Get = repoManager.getByRepoName("repo1");
        info2Get = repoManager.getByRepoName("repo2");
        assertEquals(info1, info1Get);
        assertEquals(info2, info2Get);

        RepositoryInfo rpoByName = repoManager.getByRepoName("nonexistent");
        assertNull("Expected repository to be non-existent", rpoByName);
    }

    @Test
    public void testCreateRepoUnsupportedURIScheme() {
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        hints.set(Hints.REPOSITORY_URL, "unknown://repo1");
        try {
            repoManager.createRepo(hints);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
            assertEquals(
                    "No repository initializer found capable of handling this kind of URI: unknown://repo1",
                    e.getMessage());
        }
    }

    @Test
    public void testInvalidate() throws IOException {
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        Repository repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        RepositoryInfo info = saveRepository(repo);
        repo.close();

        List<RepositoryInfo> repositories = repoManager.getAll();
        assertEquals(1, repositories.size());
        assertEquals(info, repositories.get(0));

        // get repository
        Repository repo1 = repoManager.getRepository(info.getId());
        assertNotNull(repo1);

        // subsequent calls should return the same repo object
        assertTrue(repo1 == repoManager.getRepository(info.getId()));

        // invalidating should clear the cache
        repoManager.invalidate(info.getId());
        Repository repo1_after = repoManager.getRepository(info.getId());
        assertNotNull(repo1_after);

        // they should be different instances
        assertFalse(repo1 == repo1_after);
    }

    @Test
    public void testCatalog() throws IOException {
        Catalog catalog = this.getCatalog();
        repoManager.setCatalog(catalog);
        assertTrue(catalog == repoManager.getCatalog());

        geogigData
                .init() //
                .config("user.name", "gabriel") //
                .config("user.email", "gabriel@test.com") //
                .createTypeTree("lines", "geom:LineString:srid=4326") //
                .createTypeTree("points", "geom:Point:srid=4326") //
                .add() //
                .commit("created type trees") //
                .get();

        geogigData.insert(
                "points", //
                "p1=geom:POINT(0 0)", //
                "p2=geom:POINT(1 1)", //
                "p3=geom:POINT(2 2)");

        geogigData.insert(
                "lines", //
                "l1=geom:LINESTRING(-10 0, 10 0)", //
                "l2=geom:LINESTRING(0 0, 180 0)");

        geogigData.add().commit("Added test features");

        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        int i = rnd.nextInt();
        catalogBuilder
                .namespace("geogig.org/" + i)
                .workspace("geogigws" + i)
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

        List<DataStoreInfo> geogigDataStores = repoManager.findGeogigStores();
        assertEquals(1, geogigDataStores.size());
        assertEquals(dsInfo, geogigDataStores.get(0));

        List<RepositoryInfo> repositoryInfos = repoManager.getAll();
        assertEquals(1, repositoryInfos.size());

        RepositoryInfo repoInfo = repositoryInfos.get(0);
        List<DataStoreInfo> dataStoresForRepoInfo = repoManager.findDataStores(repoInfo.getId());
        assertEquals(1, dataStoresForRepoInfo.size());
        assertEquals(dsInfo, dataStoresForRepoInfo.get(0));

        List<LayerInfo> dataStoreLayers = repoManager.findLayers(dsInfo);
        assertEquals(2, dataStoreLayers.size());
        assertTrue(dataStoreLayers.contains(pointLayerInfo));
        assertTrue(dataStoreLayers.contains(lineLayerInfo));

        List<FeatureTypeInfo> dataStoreFeatureTypes = repoManager.findFeatureTypes(dsInfo);
        assertEquals(2, dataStoreFeatureTypes.size());
        FeatureTypeInfo pointsTypeInfo, linesTypeInfo;
        if (dataStoreFeatureTypes.get(0).getName().equals("points")) {
            pointsTypeInfo = dataStoreFeatureTypes.get(0);
            linesTypeInfo = dataStoreFeatureTypes.get(1);
        } else {
            pointsTypeInfo = dataStoreFeatureTypes.get(1);
            linesTypeInfo = dataStoreFeatureTypes.get(0);
        }
        assertEquals("points", pointsTypeInfo.getName());
        assertEquals("lines", linesTypeInfo.getName());

        List<? extends CatalogInfo> catalogObjects =
                repoManager.findDependentCatalogObjects(repoInfo.getId());
        assertEquals(5, catalogObjects.size());
        assertTrue(catalogObjects.contains(dsInfo));
        assertTrue(catalogObjects.contains(pointLayerInfo));
        assertTrue(catalogObjects.contains(lineLayerInfo));
        assertTrue(catalogObjects.contains(pointsTypeInfo));
        assertTrue(catalogObjects.contains(linesTypeInfo));
    }

    @Test
    public void testRenameRepository() throws IOException {
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        Repository repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        RepositoryInfo info = saveRepository(repo);
        repo.close();

        List<RepositoryInfo> repositories = repoManager.getAll();
        assertEquals(1, repositories.size());
        assertEquals(info, repositories.get(0));

        RepositoryInfo renamed = new RepositoryInfo();
        renamed.setId(info.getId());
        renamed.setLocation(info.getLocation());
        renamed.setRepoName("repo1_renamed");
        repoManager.save(renamed);

        repositories = repoManager.getAll();
        assertEquals(1, repositories.size());
        assertEquals(renamed, repositories.get(0));

        RepositoryInfo infoGet = repoManager.get(info.getId());
        assertEquals(renamed, infoGet);

        infoGet = repoManager.getByRepoName("repo1_renamed");
        assertEquals(renamed, infoGet);

        RepositoryInfo repoByName = repoManager.getByRepoName("repo1");
        assertNull("Expected \"repo1\" to be non-existent", repoByName);
    }

    @Test
    public void testIsGeogigDirectory() throws IOException {
        assertFalse(RepositoryManager.isGeogigDirectory(null));

        File repoDir = new File(testData.getDataDirectoryRoot(), "testRepo");
        repoDir.mkdirs();
        assertFalse(RepositoryManager.isGeogigDirectory(repoDir));

        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        hints.set(Hints.REPOSITORY_URL, repoDir.toURI().toString());
        Repository repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        saveRepository(repo);
        repo.close();

        assertTrue(RepositoryManager.isGeogigDirectory(repoDir));

        File fakeRepoDir = new File(testData.getDataDirectoryRoot(), "fakeRepoDir");
        fakeRepoDir.mkdirs();
        File fakeGeogig = new File(fakeRepoDir, ".geogig");
        fakeGeogig.createNewFile();

        assertFalse(RepositoryManager.isGeogigDirectory(fakeRepoDir));
    }

    @Test
    public void testFindOrCreateByLocation() throws IOException {
        File repoDir = new File(testData.getDataDirectoryRoot(), "testRepo");
        repoDir.mkdirs();
        assertFalse(RepositoryManager.isGeogigDirectory(repoDir));
        URI repoURI = repoDir.toURI();
        RepositoryInfo info = repoManager.findOrCreateByLocation(repoURI);
        assertEquals(repoURI, info.getLocation());
        // the repository should be created
        assertTrue(RepositoryManager.isGeogigDirectory(repoDir));
        Repository repo = repoManager.getRepository(info.getId());
        String repoName = repo.command(ResolveRepositoryName.class).call();
        assertEquals("testRepo", repoName);

        // should return the same info since it was already created.
        RepositoryInfo infoGet = repoManager.findOrCreateByLocation(repoURI);
        assertEquals(info, infoGet);
    }

    @Test
    public void testDeleteRepository() throws IOException {
        Catalog catalog = this.getCatalog();
        repoManager.setCatalog(catalog);
        assertTrue(catalog == repoManager.getCatalog());

        geogigData
                .init() //
                .config("user.name", "gabriel") //
                .config("user.email", "gabriel@test.com") //
                .createTypeTree("lines", "geom:LineString:srid=4326") //
                .createTypeTree("points", "geom:Point:srid=4326") //
                .add() //
                .commit("created type trees") //
                .get();

        geogigData.insert(
                "points", //
                "p1=geom:POINT(0 0)", //
                "p2=geom:POINT(1 1)", //
                "p3=geom:POINT(2 2)");

        geogigData.insert(
                "lines", //
                "l1=geom:LINESTRING(-10 0, 10 0)", //
                "l2=geom:LINESTRING(0 0, 180 0)");

        geogigData.add().commit("Added test features");

        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        int i = rnd.nextInt();
        catalogBuilder
                .namespace("geogig.org/" + i)
                .workspace("geogigws" + i)
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

        List<DataStoreInfo> geogigDataStores = repoManager.findGeogigStores();
        assertEquals(1, geogigDataStores.size());
        assertEquals(dsInfo, geogigDataStores.get(0));

        List<RepositoryInfo> repositoryInfos = repoManager.getAll();
        assertEquals(1, repositoryInfos.size());

        RepositoryInfo info = repositoryInfos.get(0);

        repoManager.delete(info.getId());

        repositoryInfos = repoManager.getAll();
        assertEquals(0, repositoryInfos.size());

        RepositoryInfo repoByName = repoManager.getByRepoName("repo1");
        assertNull("Expected \"repo1\" to be non-existent", repoByName);

        try {
            repoManager.getRepository(info.getId());
            fail();
        } catch (Exception e) {
            e.printStackTrace();
            // expected
            assertTrue(e.getMessage().contains("Repository not found: " + info.getId()));
        }

        geogigDataStores = repoManager.findGeogigStores();
        assertEquals(0, geogigDataStores.size());
    }

    @Test
    public void testListBranches() throws IOException {
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        Repository repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        repo.command(ConfigOp.class)
                .setAction(ConfigAction.CONFIG_SET)
                .setName("user.name")
                .setValue("TestUser")
                .call();
        repo.command(ConfigOp.class)
                .setAction(ConfigAction.CONFIG_SET)
                .setName("user.email")
                .setValue("test@user.com")
                .call();
        repo.command(CommitOp.class).setAllowEmpty(true).setMessage("initial commit").call();
        repo.command(BranchCreateOp.class).setName("branch1").call();
        List<Ref> branches = repo.command(BranchListOp.class).call();
        RepositoryInfo info = saveRepository(repo);
        repo.close();

        List<Ref> repoBranches = repoManager.listBranches(info.getId());
        assertTrue(repoBranches.containsAll(branches));
        assertTrue(branches.containsAll(repoBranches));
        assertEquals(branches.size(), repoBranches.size());
    }

    @Test
    public void testPingRemote() throws Exception {
        try {
            RepositoryManager.pingRemote(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
            assertEquals("Please indicate the remote repository URL", e.getMessage());
        }
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_NAME, "repo1");
        Repository repo = repoManager.createRepo(hints);
        assertNotNull(repo);
        assertFalse(repo.isOpen());
        repo.command(InitOp.class).call();
        repo.command(ConfigOp.class)
                .setAction(ConfigAction.CONFIG_SET)
                .setName("user.name")
                .setValue("TestUser")
                .call();
        repo.command(ConfigOp.class)
                .setAction(ConfigAction.CONFIG_SET)
                .setName("user.email")
                .setValue("test@user.com")
                .call();
        repo.command(CommitOp.class).setAllowEmpty(true).setMessage("initial commit").call();
        Ref headRef = repo.command(RefParse.class).setName(Ref.HEAD).call().get();
        repo.command(InitOp.class).call();
        RepositoryInfo info1 = saveRepository(repo);
        repo.close();

        assertEquals(
                headRef, RepositoryManager.pingRemote(info1.getLocation().toString(), "user", ""));

        File notInitialized = new File(testData.getDataDirectoryRoot(), "notARepo");
        notInitialized.mkdirs();

        try {
            RepositoryManager.pingRemote(notInitialized.toURI().toString(), "user", "");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Unable to connect: "));
            assertTrue(e.getMessage().contains("not a geogig repository"));
        }
    }

    @Test
    public void testCreate() throws Exception {
        File repoFolder = new File(testData.getDataDirectoryRoot(), "someRepoName");
        URI uri = repoFolder.toURI();
        RepositoryInfo repoInfo = new RepositoryInfo();
        repoInfo.setLocation(uri);
        // now call save() with the RepositoryInfo
        // since the repo doesn't exist, save() should try to create it
        RepositoryInfo savedInfo = RepositoryManager.get().save(repoInfo);
        assertNotNull(savedInfo);
        // make sure it's retrievable as well
        assertNotNull(RepositoryManager.get().getByRepoName("someRepoName"));
    }

    private RepositoryInfo saveRepository(Repository repo) {
        RepositoryInfo repoInfo = new RepositoryInfo();
        URI location = repo.getLocation().normalize();
        if ("file".equals(location.getScheme())) {
            // need the parent
            File parentDir = new File(location).getParentFile();
            location = parentDir.toURI().normalize();
        }
        // set the URI
        repoInfo.setLocation(location);
        // save the repo, this will set a UUID
        return RepositoryManager.get().save(repoInfo);
    }
}
