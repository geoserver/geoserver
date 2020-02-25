/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.repository.IndexInfo;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.storage.IndexDatabase;
import org.springframework.mock.web.MockHttpServletResponse;

/** */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeoGigCatalogVisitorTest extends CatalogRESTTestSupport {

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        geogigData
                .init() //
                .config("user.name", "geogig") //
                .config("user.email", "geogig@test.com") //
                .createTypeTree(
                        "lines",
                        "geom:LineString:srid=4326,S_TIME:String,E_TIME:String,S_ELEV:String,E_ELEV:String") //
                .add() //
                .commit("created type trees") //
                .get();

        geogigData.insert(
                "lines", //
                "l1=geom:LINESTRING(-10 0, 10 0);S_TIME:startTime;E_TIME:endTime;S_ELEV:startElev;E_TIME:endElev", //
                "l2=geom:LINESTRING(0 0, 180 0);S_TIME:startTime;E_TIME:endTime;S_ELEV:startElev;E_TIME:endElev");

        geogigData.add().commit("Added test features");
        // need to instantiate the listerner so it can register with the test GeoServer instance
        new GeogigLayerIntegrationListener(getGeoServer());
        catalog = getCatalog();
    }

    @After
    public void after() {
        RepositoryManager.close();
    }

    private List<IndexInfo> waitForIndexes(
            final IndexDatabase indexDb, final String layerName, final int expectedSize)
            throws InterruptedException {
        assertNotNull("Expected a non null Layer Name", layerName);
        List<IndexInfo> indexInfos = indexDb.getIndexInfos(layerName);
        assertNotNull("Expected IndexInfo objects from Index database", indexInfos);
        int infoSize = indexInfos.size();
        final int maxWaitInSeconds = 10;
        int waitCount = 0;
        while (infoSize < expectedSize && waitCount++ < maxWaitInSeconds) {
            // wait a second for the index
            Thread.sleep(1_000);
            // get the infos again
            indexInfos = indexDb.getIndexInfos(layerName);
            infoSize = indexInfos.size();
        }
        // make sure the ocunt matches
        assertEquals(
                String.format("Expected exactly %s IndexInfo", expectedSize),
                expectedSize,
                indexInfos.size());
        return indexInfos;
    }

    @Test
    public void testGetAttribute_SapitalIndexOnly() throws Exception {
        addAvailableGeogigLayers();
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();
        visitor.visit(lineLayerInfo);
        GeoGIG geoGig = geogigData.getGeogig();
        IndexDatabase indexDatabase = geoGig.getRepository().indexDatabase();
        List<IndexInfo> indexInfos = waitForIndexes(indexDatabase, "lines", 1);
        IndexInfo indexInfo = indexInfos.get(0);
        Set<String> materializedAttributeNames = IndexInfo.getMaterializedAttributeNames(indexInfo);
        assertTrue("Expected empty extra Attributes set", materializedAttributeNames.isEmpty());
    }

    @Test
    public void testGetAttribute_SpatialIndexWithExtraAttributes() throws Exception {
        addAvailableGeogigLayers();
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        // set the layer up with some time/elevation metadata
        MetadataMap metadata = lineLayerInfo.getResource().getMetadata();
        DimensionInfo timeInfo = new DimensionInfoImpl();
        timeInfo.setAttribute("S_TIME");
        timeInfo.setEndAttribute("E_TIME");
        DimensionInfo elevationInfo = new DimensionInfoImpl();
        elevationInfo.setAttribute("S_ELEV");
        elevationInfo.setEndAttribute("E_ELEV");
        metadata.put("time", timeInfo);
        metadata.put("elevation", elevationInfo);
        GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();
        visitor.visit(lineLayerInfo);
        GeoGIG geoGig = geogigData.getGeogig();
        IndexDatabase indexDatabase = geoGig.getRepository().indexDatabase();
        List<IndexInfo> indexInfos = waitForIndexes(indexDatabase, "lines", 1);
        IndexInfo indexInfo = indexInfos.get(0);
        Set<String> materializedAttributeNames = IndexInfo.getMaterializedAttributeNames(indexInfo);
        assertFalse(
                "Expected non-empty extra Attributes set", materializedAttributeNames.isEmpty());
        assertEquals("Expected 4 extra attributes", 4, materializedAttributeNames.size());
    }

    private void addAvailableGeogigLayers() throws IOException {
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().build();
        // set the DataStore to auto-index
        DataStoreInfo dataStore =
                catalog.getDataStoreByName(
                        GeoGigTestData.CatalogBuilder.WORKSPACE,
                        GeoGigTestData.CatalogBuilder.STORE);
        dataStore.getConnectionParameters().put(GeoGigDataStoreFactory.AUTO_INDEXING.key, true);
        catalog.save(dataStore);
    }

    @Test
    public void testGetAttribute_SapitalIndexOnlyUsingRest() throws Exception {
        addAvailableGeoGigLayersWithDataStoreAddedViaRest();
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();
        visitor.visit(lineLayerInfo);
        GeoGIG geoGig = geogigData.getGeogig();
        IndexDatabase indexDatabase = geoGig.getRepository().indexDatabase();
        List<IndexInfo> indexInfos = waitForIndexes(indexDatabase, "lines", 1);
        IndexInfo indexInfo = indexInfos.get(0);
        Set<String> materializedAttributeNames = IndexInfo.getMaterializedAttributeNames(indexInfo);
        assertTrue("Expected empty extra Attributes set", materializedAttributeNames.isEmpty());
    }

    private void addAvailableGeoGigLayersWithDataStoreAddedViaRest() throws Exception {
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().buildWithoutDataStores();
        // create dtatastore via REST, with autoIndexing
        String message =
                "<dataStore>\n" //
                        + " <name>"
                        + GeoGigTestData.CatalogBuilder.STORE
                        + "</name>\n" //
                        + " <type>GeoGIG</type>\n" //
                        + " <connectionParameters>\n" //
                        + "   <entry key=\"geogig_repository\">${repository}</entry>\n" //
                        + "   <entry key=\"autoIndexing\">true</entry>\n"
                        + " </connectionParameters>\n" //
                        + "</dataStore>\n";
        GeoGIG geogig = geogigData.getGeogig();
        // make sure the Repository is in the Repo Manager
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(geogig.getRepository().getLocation());
        RepositoryManager.get().save(info);
        final String repoName = info.getRepoName();
        message =
                message.replace(
                        "${repository}", GeoServerGeoGigRepositoryResolver.getURI(repoName));
        final String uri =
                "/rest/workspaces/" + GeoGigTestData.CatalogBuilder.WORKSPACE + "/datastores";
        MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");
        assertEquals(
                "POST new DataStore config failed: " + response.getContentAsString(),
                201,
                response.getStatus());
        // now add the layers
        DataStoreInfo ds = catalog.getDataStoreByName(GeoGigTestData.CatalogBuilder.STORE);
        catalogBuilder.setUpLayers(ds);
    }
}
