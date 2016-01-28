/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.RESOLVER_CLASS_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geogig.geoserver.config.GeoGigInitializer;
import org.geogig.geoserver.config.GeoServerStoreRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.plumbing.ResolveGeogigDir;
import org.locationtech.geogig.api.porcelain.InitOp;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Integration test suite with GeoServer's REST API
 *
 */
public class GeoGigGeoServerRESTntegrationTest extends CatalogRESTTestSupport {

    @Rule
    public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        geogigData.init()//
                .config("user.name", "gabriel")//
                .config("user.email", "gabriel@test.com")//
                .createTypeTree("lines", "geom:LineString:srid=4326")//
                .createTypeTree("points", "geom:Point:srid=4326")//
                .add()//
                .commit("created type trees")//
                .get();

        geogigData.insert("points",//
                "p1=geom:POINT(0 0)",//
                "p2=geom:POINT(1 1)",//
                "p3=geom:POINT(2 2)");

        geogigData.insert("lines",//
                "l1=geom:LINESTRING(-10 0, 10 0)",//
                "l2=geom:LINESTRING(0 0, 180 0)");

        geogigData.add().commit("Added test features");

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.setUpWorkspace("gigws");

        // Catalog catalog = getCatalog();
        // CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        // catalogBuilder.addAllRepoLayers().build();
        //
        // String layerName = catalogBuilder.workspaceName() + ":points";
        // LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        // assertNotNull(pointLayerInfo);
        //
        // layerName = catalogBuilder.workspaceName() + ":lines";
        // LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        // assertNotNull(lineLayerInfo);
    }

    /**
     * Override so that default layers are not added
     */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        //
    }

    /**
     * Create a GeoGig DataStore through the REST API indicating the repository directory as the
     * {@link GeoGigDataStoreFactory#REPOSITORY} parameter instead of the
     * {@link RepositoryInfo#getId() RepositoryInfo id} and {@link GeoServerStoreRepositoryResolver
     * org.geogig.geoserver.config.GeoServerStoreRepositoryResolver} as the
     * {@link GeoGigDataStoreFactory#RESOLVER_CLASS_NAME} parameter.
     * <p>
     * This is probably going to be the most common way of configuring a GeoGig store with REST. The
     * GeoGig plugin shall take care of finding an existing {@link RepositoryInfo} for the given
     * repo directory or create a new one, and update the {@link DataStoreInfo} connection
     * parameters accordingly.
     */
    @Test
    public void createDataStoreOldConfigNewRepo() throws Exception {

        GeoGIG geogig = geogigData.createRpository("new_repo_old_config");
        try {
            geogig.command(InitOp.class).call();
            final File repo = geogig.command(ResolveGeogigDir.class).getFile().get()
                    .getParentFile();
            assertTrue(RepositoryManager.isGeogigDirectory(repo));

            final String repository = repo.getAbsolutePath();
            String message = "<dataStore>\n"//
                    + " <name>repo_old_config</name>\n"//
                    + " <type>GeoGIG</type>\n"//
                    + " <connectionParameters>\n"//
                    + "   <entry key=\"geogig_repository\">${repository}</entry>\n"//
                    + " </connectionParameters>\n"//
                    + "</dataStore>\n";
            message = message.replace("${repository}", repository);

            Catalog catalog = getCatalog();

            final String uri = "/rest/workspaces/gigws/datastores";
            MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");

            assertEquals(201, response.getStatusCode());

            String locationHeader = response.getHeader("Location");
            assertNotNull(locationHeader);
            assertTrue(locationHeader.endsWith("/workspaces/gigws/datastores/repo_old_config"));

            DataStoreInfo newDataStore = catalog.getDataStoreByName("repo_old_config");
            assertNotNull(newDataStore);

            DataStore ds = (DataStore) newDataStore.getDataStore(null);
            assertNotNull(ds);

            checkNewConfig(newDataStore);
        } finally {
            geogig.close();
        }
    }

    /**
     * Repository does not exist on geoserver nor on disk, use {@code <create>true</code>} to force
     * creating a new repository at the specified path
     */
    @Test
    public void createDataStoreOldConfigCreatesRepo() throws Exception {

        File targetDirectory = new File(geogigData.tmpFolder().getRoot(), "old_config_new_repo");
        final String repository = targetDirectory.getAbsolutePath();

        String message = "<dataStore>\n"//
                + " <name>repo_old_config_new_repo</name>\n"//
                + " <type>GeoGIG</type>\n"//
                + " <connectionParameters>\n"//
                + "   <entry key=\"geogig_repository\">${repository}</entry>\n"//
                + "   <entry key=\"create\">true</entry>\n"//
                + " </connectionParameters>\n"//
                + "</dataStore>\n";
        message = message.replace("${repository}", repository);

        Catalog catalog = getCatalog();

        final String uri = "/rest/workspaces/gigws/datastores";
        MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");

        assertEquals(201, response.getStatusCode());

        String locationHeader = response.getHeader("Location");
        assertNotNull(locationHeader);
        assertTrue(locationHeader.endsWith("/workspaces/gigws/datastores/repo_old_config_new_repo"));

        // DataStoreInfo created, the catalog listener DeprecatedDataStoreConfigFixer should have
        // forced the creation of the repo and updated the DataStoreInfo connection parameters with
        // the proper GeoGigDataStoreFactory.RESOLVER_CLASS_NAME
        assertTrue(targetDirectory.exists());

        DataStoreInfo newDataStore = catalog.getDataStoreByName("repo_old_config_new_repo");
        assertNotNull(newDataStore);

        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);

        checkNewConfig(newDataStore);
    }

    @Test
    public void createDataStoreOldConfigExistingRepo() throws Exception {

        // make sure a repository is configured in geoserver for that location
        final File repo = geogigData.repoDirectory();
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(repo.getAbsolutePath());
        info = RepositoryManager.get().save(info);

        final String repository = repo.getAbsolutePath();
        String message = "<dataStore>\n"//
                + " <name>repo_old_config_existing_repo</name>\n"//
                + " <type>GeoGIG</type>\n"//
                + " <connectionParameters>\n"//
                + "   <entry key=\"geogig_repository\">${repository}</entry>\n"//
                + " </connectionParameters>\n"//
                + "</dataStore>\n";
        message = message.replace("${repository}", repository);
        Catalog catalog = getCatalog();

        final String uri = "/rest/workspaces/gigws/datastores";
        MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");

        assertEquals(201, response.getStatusCode());

        DataStoreInfo newDataStore = catalog.getDataStoreByName("repo_old_config_existing_repo");
        assertNotNull(newDataStore);

        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);

        checkNewConfig(newDataStore);
        assertEquals(info.getId(), newDataStore.getConnectionParameters().get(REPOSITORY.key));
    }

    private void checkNewConfig(DataStoreInfo ds) throws IOException {
        Map<String, Serializable> params = ds.getConnectionParameters();
        String repository = (String) params.get(REPOSITORY.key);
        String resolverClassName = (String) params.get(RESOLVER_CLASS_NAME.key);
        assertNotNull(repository);
        assertEquals(GeoGigInitializer.REPO_RESOLVER_CLASSNAME, resolverClassName);

        RepositoryInfo info = RepositoryManager.get().get(repository);
        assertNotNull(info);
    }

    @Test
    public void createDataStoreNewConfig() throws Exception {
        String message = "<dataStore>\n"//
                + " <name>repo_new_config</name>\n"//
                + " <type>GeoGIG</type>\n"//
                + " <connectionParameters>\n"//
                + "   <entry key=\"geogig_repository\">${repository}</entry>\n"//
                + "   <entry key=\"resolver\">${resolver}</entry>\n"//
                + " </connectionParameters>\n"//
                + "</dataStore>\n";

        GeoGIG geogig = geogigData.createRpository("new_repo");
        try {
            geogig.command(InitOp.class).call();
            File repo = geogig.command(ResolveGeogigDir.class).getFile().get();
            final String location = repo.getParentFile().getAbsolutePath();
            RepositoryManager manager = RepositoryManager.get();
            RepositoryInfo info = new RepositoryInfo();
            info.setLocation(location);
            info = manager.save(info);

            final String repoId = info.getId();
            message = message.replace("${repository}", repoId);
            message = message.replace("${resolver}", GeoGigInitializer.REPO_RESOLVER_CLASSNAME);

            // System.err.println(message);

            Catalog catalog = getCatalog();
            CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
            catalogBuilder.setUpWorkspace("new_ws");

            final String uri = "/rest/workspaces/new_ws/datastores";
            MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");

            assertEquals(201, response.getStatusCode());

            String locationHeader = response.getHeader("Location");
            assertNotNull(locationHeader);
            assertTrue(locationHeader.endsWith("/workspaces/new_ws/datastores/repo_new_config"));

            DataStoreInfo newDataStore = catalog.getDataStoreByName("repo_new_config");
            assertNotNull(newDataStore);

            DataStore ds = (DataStore) newDataStore.getDataStore(null);
            assertNotNull(ds);

            checkNewConfig(newDataStore);
        } finally {
            geogig.close();
        }
    }

}
