/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_ENABLED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test verifying the {@link DataDirectoryGeoServerLoader} loads the same config objects as
 * {@link DefaultGeoServerLoader}
 */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class DefaultGeoServerLoaderCompatibilityTest extends GeoServerSystemTestSupport {

    DataDirectoryLoaderTestSupport support;

    @BeforeClass
    public static void disableDatadirLoader() {
        System.setProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED, "false");
    }

    @AfterClass
    public static void clearSystemProp() {
        System.clearProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();
        GeoServer geoServer = getGeoServer();
        support = new DataDirectoryLoaderTestSupport(catalog, geoServer);
        support.setUpServiceLoaders();
        geoServer.add(support.serviceInfo1(null, "service1", geoServer));
        geoServer.add(support.serviceInfo2(null, "service2", geoServer));
    }

    @Before
    public void preflight() {
        assertNull(GeoServerExtensions.bean(GeoServerLoader.class));
    }

    @After
    public void after() {
        support.tearDown();
    }

    private DataDirectoryGeoServerLoader newLoader() {
        GeoServerSecurityManager secManager = getSecurityManager();
        GeoServerConfigurationLock configLock = new GeoServerConfigurationLock();
        GeoServerDataDirectory dataDirectory = super.getDataDirectory();
        return new DataDirectoryGeoServerLoader(dataDirectory, secManager, configLock);
    }

    @Test
    public void testCatalogEquality() {
        final Catalog defaultLoaderCatalog = super.getCatalog();
        addLayerGroup(defaultLoaderCatalog);

        DataDirectoryGeoServerLoader loader = newLoader();
        CatalogImpl datadirLoaderCatalog = new CatalogImpl();
        loader.postProcessBeforeInitialization(datadirLoaderCatalog, "catalog");

        checkEquals(defaultLoaderCatalog.getWorkspaces(), datadirLoaderCatalog.getWorkspaces());
        checkEquals(defaultLoaderCatalog.getNamespaces(), datadirLoaderCatalog.getNamespaces());
        checkEquals(defaultLoaderCatalog.getStyles(), datadirLoaderCatalog.getStyles());
        checkEquals(defaultLoaderCatalog.getStores(StoreInfo.class), datadirLoaderCatalog.getStores(StoreInfo.class));
        checkEquals(
                defaultLoaderCatalog.getResources(ResourceInfo.class),
                datadirLoaderCatalog.getResources(ResourceInfo.class));
        checkEquals(defaultLoaderCatalog.getLayers(), datadirLoaderCatalog.getLayers());
        checkEquals(defaultLoaderCatalog.getLayerGroups(), datadirLoaderCatalog.getLayerGroups());

        assertEquals(defaultLoaderCatalog.getDefaultWorkspace(), datadirLoaderCatalog.getDefaultWorkspace());
        assertEquals(defaultLoaderCatalog.getDefaultNamespace(), datadirLoaderCatalog.getDefaultNamespace());

        for (WorkspaceInfo ws : defaultLoaderCatalog.getWorkspaces()) {
            DataStoreInfo expected = defaultLoaderCatalog.getDefaultDataStore(ws);
            DataStoreInfo actual = datadirLoaderCatalog.getDefaultDataStore(ws);
            assertEquals(expected, actual);
        }
    }

    private void addLayerGroup(Catalog catalog) {
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lg1");
        lg.setTitle("lg1");
        lg.getLayers().addAll(catalog.getLayers());
        catalog.add(lg);
    }

    @Test
    public void testConfigEquality() {
        DataDirectoryGeoServerLoader loader = newLoader();
        CatalogImpl datadirLoaderCatalog = new CatalogImpl();
        loader.postProcessBeforeInitialization(datadirLoaderCatalog, "catalog");

        GeoServer datadirLoaderGeoServer = new GeoServerImpl();
        datadirLoaderGeoServer.setCatalog(datadirLoaderCatalog);
        loader.postProcessBeforeInitialization(datadirLoaderGeoServer, "geoServer");

        final GeoServer defaultLoaderGeoServer = getGeoServer();

        assertEquals(defaultLoaderGeoServer.getGlobal(), datadirLoaderGeoServer.getGlobal());
        assertEquals(defaultLoaderGeoServer.getSettings(), datadirLoaderGeoServer.getSettings());
        assertEquals(defaultLoaderGeoServer.getLogging(), datadirLoaderGeoServer.getLogging());
        checkEquals(defaultLoaderGeoServer.getServices(), datadirLoaderGeoServer.getServices());
    }

    private void checkEquals(Collection<? extends Info> expected, Collection<? extends Info> actual) {
        assertThat(expected.size(), greaterThan(0));
        assertEquals(expected.size(), actual.size());
        Map<String, Info> expectedMap = toIdMap(expected);
        Map<String, Info> actualMap = toIdMap(expected);
        assertEquals(expectedMap.keySet(), actualMap.keySet());

        for (Info e : expectedMap.values()) {
            Info a = actualMap.get(e.getId());
            e = ModificationProxy.unwrap(e);
            a = ModificationProxy.unwrap(a);
            assertEquals(e, a);
        }
    }

    private Map<String, Info> toIdMap(Collection<? extends Info> infos) {
        return infos.stream().collect(Collectors.toMap(Info::getId, Function.identity()));
    }
}
