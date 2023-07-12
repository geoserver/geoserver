/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.datadir.config.DataDirectoryLoaderConfiguration.DataDirLoaderEnabledCondition;
import org.geoserver.catalog.faker.CatalogFaker;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServicePersister;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.junit.After;
import org.junit.Test;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class DataDirectoryGeoServerLoaderTest extends GeoServerSystemTestSupport {

    static interface TestService1 extends ServiceInfo {};

    static interface TestService2 extends ServiceInfo {};

    @SuppressWarnings("serial")
    static class TestService1Impl extends ServiceInfoImpl implements TestService1 {};

    @SuppressWarnings("serial")
    static class TestService2Impl extends ServiceInfoImpl implements TestService2 {};

    private WorkspaceInfo testWs1, testWs2;

    /**
     * Disables using this plugin before setting up the test application context, to check the
     * values of the manually run {@link DataDirectoryGeoServerLoader} against the objects loaded by
     * {@link DefaultGeoServerLoader}
     */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        System.setProperty(DataDirLoaderEnabledCondition.KEY, "false");
        super.setUpTestData(testData);
    }

    @After
    public void after() {
        System.clearProperty("datadir.loader.enabled");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        final Catalog catalog = getCatalog();
        final GeoServer geoServer = getGeoServer();

        TestServiceLoader1 serviceLoader1 = new TestServiceLoader1(getResourceLoader());
        TestServiceLoader2 serviceLoader2 = new TestServiceLoader2(getResourceLoader());
        GeoServerExtensionsHelper.singleton(
                "testServiceLoader1", serviceLoader1, XStreamServiceLoader.class);
        GeoServerExtensionsHelper.singleton(
                "testServiceLoader2", serviceLoader2, XStreamServiceLoader.class);

        geoServer.removeListener(
                geoServer.getListeners().stream()
                        .filter(ServicePersister.class::isInstance)
                        .findFirst()
                        .orElse(null));

        final List<XStreamServiceLoader<ServiceInfo>> loaders =
                DataDirectoryGeoServerLoader.findServiceLoaders();

        geoServer.addListener(new ServicePersister(loaders, geoServer));

        CatalogFaker faker = new CatalogFaker(catalog, geoServer);

        catalog.add(testWs1 = faker.workspaceInfo());
        catalog.add(testWs2 = faker.workspaceInfo());
        catalog.add(faker.namespace(testWs1.getName()));
        catalog.add(faker.namespace(testWs2.getName()));

        geoServer.add(faker.serviceInfo("service1", () -> serviceLoader1.create(geoServer)));
        geoServer.add(faker.serviceInfo("service2", () -> serviceLoader2.create(geoServer)));

        geoServer.add(faker.settingsInfo(testWs1));
        geoServer.add(faker.settingsInfo(testWs2));

        geoServer.add(
                faker.serviceInfo(testWs1, "service1", () -> serviceLoader1.create(geoServer)));
        geoServer.add(
                faker.serviceInfo(testWs1, "service2", () -> serviceLoader2.create(geoServer)));

        geoServer.add(
                faker.serviceInfo(testWs2, "service1", () -> serviceLoader1.create(geoServer)));
        geoServer.add(
                faker.serviceInfo(testWs2, "service2", () -> serviceLoader2.create(geoServer)));
    }

    @Test
    public void loadCatalog() {
        GeoServerResourceLoader resourceLoader = super.getResourceLoader();
        GeoServerSecurityManager secManager = getSecurityManager();
        DataDirectoryGeoServerLoader loader =
                new DataDirectoryGeoServerLoader(resourceLoader, secManager);

        final Catalog catalog = super.getCatalog();
        CatalogImpl newCatalog = new CatalogImpl();

        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        assertSameSize(catalog.getWorkspaces(), newCatalog.getWorkspaces());
        assertSameSize(catalog.getNamespaces(), newCatalog.getNamespaces());
        assertSameSize(catalog.getStyles(), newCatalog.getStyles());
        assertSameSize(catalog.getStores(StoreInfo.class), newCatalog.getStores(StoreInfo.class));
        assertSameSize(
                catalog.getResources(ResourceInfo.class),
                newCatalog.getResources(ResourceInfo.class));
        assertSameSize(catalog.getLayers(), newCatalog.getLayers());
        assertSameSize(catalog.getLayerGroups(), newCatalog.getLayerGroups());
    }

    @Test
    public void loadCatalog_decrypts_passwords() {
        final Catalog catalog = super.getCatalog();
        DataStoreInfo infoWithPassword = createPostgisStore();
        infoWithPassword.setEnabled(false);
        final String pwdParam = PostgisNGDataStoreFactory.PASSWD.key;
        final String plainPwd = (String) infoWithPassword.getConnectionParameters().get(pwdParam);

        catalog.add(infoWithPassword);
        infoWithPassword = catalog.getDataStore(infoWithPassword.getId());

        { // preflight: verify the store info pwd was stored in encrypted form
            XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
            persister.setEncryptPasswordFields(false);
            XStream xStream = persister.getXStream();
            Resource resource = super.getDataDirectory().config(infoWithPassword);
            DataStoreInfo depresisted = (DataStoreInfo) xStream.fromXML(resource.file());
            Serializable encodedPwd = depresisted.getConnectionParameters().get(pwdParam);
            assertNotEquals(plainPwd, encodedPwd);
        }
        GeoServerResourceLoader resourceLoader = super.getResourceLoader();
        GeoServerSecurityManager secManager = getSecurityManager();
        DataDirectoryGeoServerLoader loader =
                new DataDirectoryGeoServerLoader(resourceLoader, secManager);

        CatalogImpl newCatalog = new CatalogImpl();

        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        DataStoreInfo depersistedWithDataDirLoader =
                newCatalog.getDataStore(infoWithPassword.getId());
        assertNotNull(depersistedWithDataDirLoader);
        assertEquals(
                plainPwd, depersistedWithDataDirLoader.getConnectionParameters().get(pwdParam));
    }

    /** @return */
    private DataStoreInfo createPostgisStore() {
        DataStoreInfo ds = getCatalog().getFactory().createDataStore();
        ds.setWorkspace(testWs1);
        ds.setName("postgis");
        ds.setType(new PostgisNGDataStoreFactory().getDisplayName());
        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put(
                PostgisNGDataStoreFactory.DBTYPE.key,
                (String) PostgisNGDataStoreFactory.DBTYPE.getDefaultValue());
        params.put(PostgisNGDataStoreFactory.HOST.key, "localhost");
        params.put(PostgisNGDataStoreFactory.DATABASE.key, "test");
        params.put(PostgisNGDataStoreFactory.USER.key, "test");
        params.put(PostgisNGDataStoreFactory.PASSWD.key, "s3cr3t");
        return ds;
    }

    @Test
    public void loadConfig() {
        GeoServerResourceLoader resourceLoader = super.getResourceLoader();
        GeoServerSecurityManager secManager = getSecurityManager();
        DataDirectoryGeoServerLoader loader =
                new DataDirectoryGeoServerLoader(resourceLoader, secManager);

        final GeoServer gs = getGeoServer();
        GeoServer newGs = new GeoServerImpl();
        newGs.setCatalog(gs.getCatalog());

        loader.postProcessBeforeInitialization(newGs, "geoServer");

        assertEquals(gs.getGlobal(), newGs.getGlobal());
        assertEquals(gs.getSettings(), newGs.getSettings());
        assertEquals(gs.getLogging(), newGs.getLogging());

        assertNotNull(newGs.getService(TestService1.class));
        assertNotNull(newGs.getService(TestService2.class));

        WorkspaceInfo ws1 =
                requireNonNull(newGs.getCatalog().getWorkspaceByName(testWs1.getName()));
        WorkspaceInfo ws2 =
                requireNonNull(newGs.getCatalog().getWorkspaceByName(testWs2.getName()));

        assertNotNull(newGs.getSettings(ws1));
        assertNotNull(newGs.getSettings(ws2));

        assertNotNull(newGs.getService(TestService1.class));
        assertNotNull(newGs.getService(TestService2.class));

        assertNotNull(newGs.getService(ws1, TestService1.class));
        assertNotNull(newGs.getService(ws1, TestService2.class));

        assertNotNull(newGs.getService(ws2, TestService1.class));
        assertNotNull(newGs.getService(ws2, TestService2.class));
    }

    private void assertSameSize(Collection<?> expected, Collection<?> actual) {
        assertEquals(expected.size(), actual.size());
    }

    static final class TestServiceLoader1 extends XStreamServiceLoader<TestService1> {

        public TestServiceLoader1(GeoServerResourceLoader resourceLoader) {
            super(resourceLoader, "service1");
        }

        @Override
        public Class<TestService1> getServiceClass() {
            return TestService1.class;
        }

        @Override
        protected TestService1 createServiceFromScratch(GeoServer gs) {
            return new TestService1Impl();
        }
    };

    static final class TestServiceLoader2 extends XStreamServiceLoader<TestService2> {

        public TestServiceLoader2(GeoServerResourceLoader resourceLoader) {
            super(resourceLoader, "service2");
        }

        @Override
        public Class<TestService2> getServiceClass() {
            return TestService2.class;
        }

        @Override
        protected TestService2 createServiceFromScratch(GeoServer gs) {
            return new TestService2Impl();
        }
    };
}
