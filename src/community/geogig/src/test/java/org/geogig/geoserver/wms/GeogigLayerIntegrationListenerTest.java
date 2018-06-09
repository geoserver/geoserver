/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeogigLayerIntegrationListenerTest extends GeoServerSystemTestSupport {

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
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

        // add a branch for the explicit HEAD test
        geogigData.branch("fakeBranch");
        // need to instantiate the listerner so it can register with the test GeoServer instance
        new GeogigLayerIntegrationListener(getGeoServer());
    }

    @After
    public void after() {
        RepositoryManager.close();
    }

    @Test
    public void testAddGeogigLayerForcesCreationOfRootAuthURL() {
        addAvailableGeogigLayers();

        WMSInfo service = getGeoServer().getService(WMSInfo.class);
        List<AuthorityURLInfo> authorityURLs = service.getAuthorityURLs();
        AuthorityURLInfo expected = null;
        for (AuthorityURLInfo auth : authorityURLs) {
            if (GeogigLayerIntegrationListener.AUTHORITY_URL_NAME.equals(auth.getName())) {
                expected = auth;
                break;
            }
        }
        assertNotNull("No geogig auth url found: " + authorityURLs, expected);
    }

    @Test
    public void testAddGeogigLayerAddsLayerIdentifier() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testAddGeogigLayerAddsLayerIdentifierWithExplicitBranch() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        DataStoreInfo store = catalog.getDataStoreByName(catalogBuilder.storeName());
        store.getConnectionParameters().put(GeoGigDataStoreFactory.BRANCH.key, "master");
        catalog.save(store);

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testAddGeogigLayerAddsLayerIdentifierWithExplicitHead() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        DataStoreInfo store = catalog.getDataStoreByName(catalogBuilder.storeName());

        store.getConnectionParameters().put(GeoGigDataStoreFactory.HEAD.key, "fakeBranch");
        catalog.save(store);

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testRenameStore() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String storeName = catalogBuilder.storeName();
        DataStoreInfo store = catalog.getStoreByName(storeName, DataStoreInfo.class);
        store.setName("new_store_name");
        catalog.save(store);

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testRenameWorkspace() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String wsName = catalogBuilder.workspaceName();
        WorkspaceInfo ws = catalog.getWorkspaceByName(wsName);
        String newWsName = "new_ws_name";
        ws.setName(newWsName);
        catalog.save(ws);

        String layerName = newWsName + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = newWsName + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    private void assertIdentifier(LayerInfo layer) {
        assertNotNull(layer);

        final ResourceInfo resource = layer.getResource();
        final DataStoreInfo store = (DataStoreInfo) resource.getStore();
        final Map<String, Serializable> params = store.getConnectionParameters();
        final String repoId = (String) params.get(REPOSITORY.key);

        List<LayerIdentifierInfo> identifiers = layer.getIdentifiers();
        LayerIdentifierInfo expected = null;
        for (LayerIdentifierInfo idinfo : identifiers) {
            if (GeogigLayerIntegrationListener.AUTHORITY_URL_NAME.equals(idinfo.getAuthority())) {
                expected = idinfo;
            }
        }

        assertNotNull("No geogig identifier added for layer " + layer, expected);

        String expectedId = repoId + ":" + resource.getNativeName();
        if (params.containsKey(GeoGigDataStoreFactory.BRANCH.key)) {
            String branch = (String) params.get(GeoGigDataStoreFactory.BRANCH.key);
            expectedId += ":" + branch;
        } else if (params.containsKey(GeoGigDataStoreFactory.HEAD.key)) {
            String head = (String) params.get(GeoGigDataStoreFactory.HEAD.key);
            expectedId += ":" + head;
        }

        assertEquals(expectedId, expected.getIdentifier());
    }

    private void addAvailableGeogigLayers() {
        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().build();
    }
}
