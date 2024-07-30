/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.Version;
import org.junit.After;
import org.junit.Test;

/**
 * Test suite for {@link org.geoserver.config.DataDirectoryGeoServerLoader}, first creates the test
 * data using the {@link org.geoserver.config.DefaultGeoServerLoader} and then verifies {@link
 * org.geoserver.config.DataDirectoryGeoServerLoader} produces the same results.
 */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class DataDirectoryGeoServerLoaderTest extends GeoServerSystemTestSupport {

    static interface TestService1 extends ServiceInfo {};

    static interface TestService2 extends ServiceInfo {};

    @SuppressWarnings("serial")
    static class TestService1Impl extends ServiceInfoImpl implements TestService1 {};

    @SuppressWarnings("serial")
    static class TestService2Impl extends ServiceInfoImpl implements TestService2 {};

    TestServiceLoader1 serviceLoader1;
    TestServiceLoader2 serviceLoader2;

    private WorkspaceInfo testWs1, testWs2;

    /**
     * Disables using this plugin before setting up the test application context, to check the
     * values of the manually run {@link org.geoserver.config.DataDirectoryGeoServerLoader} against
     * the objects loaded by {@link org.geoserver.config.DefaultGeoServerLoader}
     */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        System.setProperty(DataDirectoryGeoServerLoader.ENABLED_PROPERTY, "false");
        super.setUpTestData(testData);
    }

    @After
    public void after() {
        System.clearProperty("datadir.loader.enabled");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        final GeoServer geoServer = getGeoServer();
        setUpServiceLoaders(geoServer);

        testWs1 = addWorkspace("testWs1");
        geoServer.add(settingsInfo(testWs1));

        testWs2 = addWorkspace("testWs2");
        geoServer.add(settingsInfo(testWs2));

        geoServer.add(serviceInfo(null, "service1", () -> serviceLoader1.create(geoServer)));
        geoServer.add(serviceInfo(null, "service2", () -> serviceLoader2.create(geoServer)));

        geoServer.add(serviceInfo(testWs1, "service1", () -> serviceLoader1.create(geoServer)));
        geoServer.add(serviceInfo(testWs1, "service2", () -> serviceLoader2.create(geoServer)));

        geoServer.add(serviceInfo(testWs2, "service1", () -> serviceLoader1.create(geoServer)));
        geoServer.add(serviceInfo(testWs2, "service2", () -> serviceLoader2.create(geoServer)));
    }

    public SettingsInfo settingsInfo(WorkspaceInfo workspace) {
        SettingsInfo s = new SettingsInfoImpl();
        s.setWorkspace(workspace);
        String id = workspace == null ? "global-settings-id" : workspace.getName() + "-settings-id";
        OwsUtils.set(s, "id", id);

        s.setTitle(workspace == null ? "Global Settings" : workspace.getName() + " Settings");
        s.setCharset("UTF-8");
        s.setNumDecimals(9);
        s.setOnlineResource("http://geoserver.org");
        s.setProxyBaseUrl("http://test.geoserver.org");
        s.setSchemaBaseUrl("file:data/schemas");
        s.setVerbose(true);
        s.setVerboseExceptions(true);
        return s;
    }

    public <S extends ServiceInfo> S serviceInfo(
            WorkspaceInfo workspace, String name, Supplier<S> factory) {
        S s = factory.get();
        String id = String.format("%s:%s-id", workspace == null ? null : workspace.getName(), name);
        OwsUtils.set(s, "id", id);
        s.setName(name);
        s.setWorkspace(workspace);
        s.setTitle(name + " Title");
        s.setAbstract(name + " Abstract");
        s.setInternationalTitle(
                internationalString(
                        Locale.ENGLISH,
                        name + " english title",
                        Locale.CANADA_FRENCH,
                        name + "titre anglais"));
        s.setInternationalAbstract(
                internationalString(
                        Locale.ENGLISH,
                        name + " english abstract",
                        Locale.CANADA_FRENCH,
                        name + "résumé anglais"));
        s.setAccessConstraints("NONE");
        s.setCiteCompliant(true);
        s.setEnabled(true);
        s.getExceptionFormats().add("fake-" + name + "-exception-format");
        s.setFees("NONE");
        s.setMaintainer("Claudious whatever");
        MetadataLinkInfoImpl metadataLink = new MetadataLinkInfoImpl();
        metadataLink.setAbout("about");
        metadataLink.setContent("content");
        metadataLink.setId("medatata-link-" + name);
        metadataLink.setMetadataType("fake");
        metadataLink.setType("void");
        s.setMetadataLink(metadataLink);
        s.setOnlineResource("http://geoserver.org/" + name);
        s.setOutputStrategy("SPEED");
        s.setSchemaBaseURL("file:data/" + name);
        s.setVerbose(true);
        List<Version> versions = Lists.newArrayList(new Version("1.0.0"), new Version("2.0.0"));
        s.getVersions().addAll(versions);
        return s;
    }

    public GrowableInternationalString internationalString(
            Locale l1, String val1, Locale l2, String val2) {
        GrowableInternationalString s = new GrowableInternationalString();
        s.add(l1, val1);
        s.add(l2, val2);
        return s;
    }

    private WorkspaceInfo addWorkspace(String name) {
        Catalog catalog = getCatalog();
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName(name);
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(ws.getName());
        ns.setURI("http://" + name + ".test.com");
        catalog.add(ws);
        catalog.add(ns);
        return ws;
    }

    private void setUpServiceLoaders(final GeoServer geoServer) {
        serviceLoader1 = new TestServiceLoader1(getResourceLoader());
        serviceLoader2 = new TestServiceLoader2(getResourceLoader());
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

    private void assertSameSize(
            Collection<? extends CatalogInfo> expected, Collection<? extends CatalogInfo> actual) {
        assertEquals(expected.size(), actual.size());
    }

    @Test
    public void loadCatalog_decrypts_datastoreinfo_passwords() {
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

    @Test
    public void loadCatalog_decrypts_httpstoreinfo_passwords() {
        final Catalog catalog = super.getCatalog();

        final String plainPassword = "passW0rd";

        WMSStoreInfo wmsstore = catalog.getFactory().createWebMapServer();
        wmsstore.setName("wmsstore");
        wmsstore.setCapabilitiesURL("http://localhost/wms?request=GetCapabilities");
        wmsstore.setUsername("user");
        wmsstore.setPassword(plainPassword);
        catalog.add(wmsstore);

        WMSStoreInfo store = catalog.getStore(wmsstore.getId(), WMSStoreInfo.class);

        { // preflight: verify the store info pwd was stored in encrypted form
            XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
            persister.setEncryptPasswordFields(false);
            XStream xStream = persister.getXStream();
            Resource resource = super.getDataDirectory().config(store);
            WMSStoreInfo depresisted = (WMSStoreInfo) xStream.fromXML(resource.file());
            Serializable encodedPwd = depresisted.getPassword();
            assertNotNull(encodedPwd);
            assertNotEquals(plainPassword, encodedPwd);
        }
        GeoServerResourceLoader resourceLoader = super.getResourceLoader();
        GeoServerSecurityManager secManager = getSecurityManager();
        DataDirectoryGeoServerLoader loader =
                new DataDirectoryGeoServerLoader(resourceLoader, secManager);

        CatalogImpl newCatalog = new CatalogImpl();

        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        WMSStoreInfo depersistedWithDataDirLoader =
                newCatalog.getStore(store.getId(), WMSStoreInfo.class);
        assertNotNull(depersistedWithDataDirLoader);
        assertEquals(plainPassword, depersistedWithDataDirLoader.getPassword());
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
