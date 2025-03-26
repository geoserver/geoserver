/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.util.Objects.requireNonNull;
import static org.geoserver.data.test.CiteTestData.ROTATED_CAD;
import static org.geoserver.data.test.CiteTestData.TASMANIA_BM;
import static org.geoserver.data.test.CiteTestData.TASMANIA_DEM;
import static org.geoserver.data.test.CiteTestData.WORLD;
import static org.geoserver.data.test.SystemTestData.MULTIBAND;
import static org.geoserver.platform.resource.Resource.Type.RESOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.datadir.DataDirectoryLoaderTestSupport.TestService1;
import org.geoserver.config.datadir.DataDirectoryLoaderTestSupport.TestService2;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link org.geoserver.config.datadir.DataDirectoryGeoServerLoader} acting as the default
 * {@link GeoServerLoader}
 */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class DataDirectoryGeoServerLoaderTest extends GeoServerSystemTestSupport {

    DataDirectoryLoaderTestSupport support;

    @Before
    public void preflight() {
        assertTrue(DataDirectoryGeoServerLoader.isEnabled(applicationContext));
        Logging.getLogger(DataDirectoryGeoServerLoader.class.getPackage().getName())
                .setLevel(Level.CONFIG);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();
        GeoServer geoServer = getGeoServer();
        support = new DataDirectoryLoaderTestSupport(catalog, geoServer);
        support.setUpServiceLoaders();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        support.tearDown();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testCatalogLoaded() {
        Stream.concat(
                        // from testData.setUpDefault()
                        Arrays.stream(CiteTestData.TYPENAMES),
                        // from testData.setUpDefaultRasterLayers()
                        Stream.of(TASMANIA_DEM, TASMANIA_BM, ROTATED_CAD, WORLD, MULTIBAND))
                .forEach(this::assertLayerLoaded);
    }

    private void assertLayerLoaded(QName typeName) {
        Catalog catalog = getCatalog();

        String prefix = typeName.getPrefix();
        String name = typeName.getLocalPart();
        String prefixedName = String.format("%s:%s", prefix, name);
        assertNotNull(catalog.getWorkspaceByName(prefix));
        assertNotNull(catalog.getLayerByName(prefixedName));
    }

    private GeoServerLoaderProxy getLoader() {
        return GeoServerExtensions.bean(GeoServerLoaderProxy.class);
    }

    @Test
    public void reload() throws Exception {
        WorkspaceInfo ws1 = getCatalog().getWorkspaces().get(0);
        GeoServerDataDirectory dataDirectory = getDataDirectory();
        dataDirectory.config(ws1).delete();
        getLoader().reload();

        assertNull(getCatalog().getWorkspace(ws1.getId()));
    }

    @Test
    public void loadCatalog() {
        DataDirectoryGeoServerLoader loader = newLoader();
        CatalogImpl newCatalog = new CatalogImpl();
        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        final Catalog catalog = super.getCatalog();
        assertSameSize(catalog.getWorkspaces(), newCatalog.getWorkspaces());
        assertSameSize(catalog.getNamespaces(), newCatalog.getNamespaces());
        assertSameSize(catalog.getStyles(), newCatalog.getStyles());
        assertSameSize(catalog.getStores(StoreInfo.class), newCatalog.getStores(StoreInfo.class));
        assertSameSize(catalog.getResources(ResourceInfo.class), newCatalog.getResources(ResourceInfo.class));
        assertSameSize(catalog.getLayers(), newCatalog.getLayers());
        assertSameSize(catalog.getLayerGroups(), newCatalog.getLayerGroups());
    }

    /** Verify there are no dangling references to the temporary catalog */
    @Test
    public void loadCatalogResolvedCatalogProperties() {
        DataDirectoryGeoServerLoader loader = newLoader();
        CatalogImpl newCatalog = new CatalogImpl();
        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        newCatalog.getStores(StoreInfo.class).forEach(s -> assertSame(newCatalog, s.getCatalog()));
        newCatalog.getResources(ResourceInfo.class).forEach(s -> assertSame(newCatalog, s.getCatalog()));
        newCatalog
                .getStyles()
                .forEach(s -> assertSame(newCatalog, ((StyleInfoImpl) ModificationProxy.unwrap(s)).getCatalog()));
    }

    private void assertSameSize(Collection<? extends CatalogInfo> expected, Collection<? extends CatalogInfo> actual) {
        assertEquals(expected.size(), actual.size());
    }

    @Test
    public void loadCatalogDecryptsStoreInfoPasswords() {
        final Catalog catalog = super.getCatalog();
        WorkspaceInfo testWs = support.addWorkspace("testWs1");
        DataStoreInfo infoWithPassword = support.createPostgisStore(testWs);
        infoWithPassword.setEnabled(false);
        final String pwdParam = JDBCDataStoreFactory.PASSWD.key;
        final String plainPwd =
                (String) infoWithPassword.getConnectionParameters().get(pwdParam);

        catalog.add(infoWithPassword);
        infoWithPassword = catalog.getDataStore(infoWithPassword.getId());

        // preflight: verify the store info pwd was stored in encrypted form
        assertPasswordEncrypted(infoWithPassword);

        DataDirectoryGeoServerLoader loader = newLoader();

        CatalogImpl newCatalog = new CatalogImpl();

        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        DataStoreInfo depersistedWithDataDirLoader = newCatalog.getDataStore(infoWithPassword.getId());
        assertNotNull(depersistedWithDataDirLoader);
        assertEquals(
                plainPwd, depersistedWithDataDirLoader.getConnectionParameters().get(pwdParam));
    }

    @Test
    public void loadCatalogDecryptsHTTPStoreInfoPasswords() {
        final Catalog catalog = super.getCatalog();

        final String plainPassword = "passW0rd";

        WMSStoreInfo wmsstore = catalog.getFactory().createWebMapServer();
        wmsstore.setName("wmsstore");
        wmsstore.setCapabilitiesURL("http://localhost/wms?request=GetCapabilities");
        wmsstore.setUsername("user");
        wmsstore.setPassword(plainPassword);
        catalog.add(wmsstore);

        WMSStoreInfo store = catalog.getStore(wmsstore.getId(), WMSStoreInfo.class);

        // preflight: verify the store info pwd was stored in encrypted form
        assertPasswordEncrypted(store);

        DataDirectoryGeoServerLoader loader = newLoader();

        CatalogImpl newCatalog = new CatalogImpl();

        loader.postProcessBeforeInitialization(newCatalog, "catalog");

        WMSStoreInfo depersistedWithDataDirLoader = newCatalog.getStore(store.getId(), WMSStoreInfo.class);
        assertNotNull(depersistedWithDataDirLoader);
        assertEquals(plainPassword, depersistedWithDataDirLoader.getPassword());
    }

    @Test
    public void loadConfig() {
        final GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws1 = support.addWorkspace("testWs1");
        WorkspaceInfo ws2 = support.addWorkspace("testWs2");

        geoServer.add(support.createSettings(ws1));
        geoServer.add(support.createSettings(ws2));

        geoServer.add(support.serviceInfo1(null, "service1", geoServer));
        geoServer.add(support.serviceInfo2(null, "service2", geoServer));

        geoServer.add(support.serviceInfo1(ws1, "service1", geoServer));
        geoServer.add(support.serviceInfo2(ws1, "service2", geoServer));

        geoServer.add(support.serviceInfo1(ws2, "service1", geoServer));
        geoServer.add(support.serviceInfo2(ws2, "service2", geoServer));

        // try a separate loader, GeoServer, and Catalog
        DataDirectoryGeoServerLoader loader = newLoader();

        GeoServer newGs = new GeoServerImpl();
        newGs.setCatalog(new CatalogImpl());
        newGs.getCatalog().setResourceLoader(getCatalog().getResourceLoader());
        support.setUpServiceLoaders(newGs);

        loader.postProcessBeforeInitialization(newGs.getCatalog(), "catalog");
        loader.postProcessBeforeInitialization(newGs, "geoServer");

        assertEquals(geoServer.getGlobal(), newGs.getGlobal());
        assertEquals(geoServer.getSettings(), newGs.getSettings());
        assertEquals(geoServer.getLogging(), newGs.getLogging());

        assertNotNull(newGs.getService(TestService1.class));
        assertNotNull(newGs.getService(TestService2.class));

        ws1 = requireNonNull(newGs.getCatalog().getWorkspaceByName(ws1.getName()));
        ws2 = requireNonNull(newGs.getCatalog().getWorkspaceByName(ws2.getName()));

        assertNotNull(newGs.getSettings(ws1));
        assertNotNull(newGs.getSettings(ws2));

        assertNotNull(newGs.getService(TestService1.class));
        assertNotNull(newGs.getService(TestService2.class));

        assertNotNull(newGs.getService(ws1, TestService1.class));
        assertNotNull(newGs.getService(ws1, TestService2.class));

        assertNotNull(newGs.getService(ws2, TestService1.class));
        assertNotNull(newGs.getService(ws2, TestService2.class));
    }

    @Test
    public void testAssignsDefaultWorkspace() throws Exception {
        deleteDefaultWorkspaceFile();

        Catalog catalog = getCatalog();
        getLoader().reload();
        assertNotNull(catalog.getDefaultWorkspace());
    }

    /** @throws behavior in {@link CatalogLoader#setDefaultWorkspace()} */
    @Test
    public void testAssignsDefaultWorkspaceIsPredictably() throws Exception {
        Catalog catalog = getCatalog();

        WorkspaceInfo abc = addWorkspace("abc");
        // preflight
        SortBy sortByName = Predicates.sortBy("name", true);
        try (CloseableIterator<WorkspaceInfo> list =
                catalog.list(WorkspaceInfo.class, Predicates.acceptAll(), 0, 1, sortByName)) {
            assertTrue(list.hasNext());
            WorkspaceInfo ws = list.next();
            assertEquals(abc, ws);
        }

        // assert the default workspace is consistently chosen as the first one sorted by name
        deleteDefaultWorkspaceFile();
        assertDefaultWorkspace(abc);

        WorkspaceInfo aaa = addWorkspace("aaa");
        deleteDefaultWorkspaceFile();
        assertDefaultWorkspace(aaa);

        catalog.remove(catalog.getNamespaceByPrefix(aaa.getName()));
        catalog.remove(aaa);
        assertDefaultWorkspace(abc);
    }

    private void assertDefaultWorkspace(WorkspaceInfo expected) throws Exception {
        getLoader().reload();
        Catalog catalog = getCatalog();
        assertNotNull(catalog.getWorkspaceByName(expected.getName()));
        assertEquals(expected, catalog.getDefaultWorkspace());
    }

    private void deleteDefaultWorkspaceFile() {
        Resource defaultWorkspaceConfig = getDataDirectory().defaultWorkspaceConfig();
        assertTrue(defaultWorkspaceConfig.delete());
    }

    private WorkspaceInfo addWorkspace(String name) {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName(name);
        catalog.add(ws);

        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix(ws.getName());
        ns.setURI(ws.getName());
        catalog.add(ns);
        return ws;
    }

    @Test
    public void testGlobalStyleWithWorkspaceIgnored() throws Exception {
        Catalog catalog = getCatalog();
        StyleInfoImpl s = (StyleInfoImpl) ModificationProxy.unwrap(catalog.getStyleByName("point"));
        String id = "testGlobalStyleWithWorkspaceIgnored";
        s.setId(id);
        s.setName(id);

        Resource globalStyleLocation = getDataDirectory().config(s);
        s.setWorkspace(catalog.getWorkspaces().get(0));

        persist(s, globalStyleLocation);
        getLoader().reload();
        assertNull(catalog.getStyle(id));

        s.setWorkspace(null);
        persist(s, globalStyleLocation);
        getLoader().reload();

        assertEquals(s, catalog.getStyle(id));
    }

    @Test
    public void testWorkspaceStyleWithNullWorkspaceIgnored() throws Exception {
        StyleInfoImpl s = (StyleInfoImpl) ModificationProxy.unwrap(getCatalog().getStyleByName("point"));
        String id = "testWorkspaceStyleWithNullWorkspaceIgnored";
        s.setId(id);
        s.setName(id);

        Catalog catalog = getCatalog();
        WorkspaceInfo workspace = catalog.getWorkspaces().get(0);
        s.setWorkspace(workspace);

        Resource workspaceStyleLocation = getDataDirectory().config(s);

        persist(s, workspaceStyleLocation);
        getLoader().reload();
        assertEquals(s, catalog.getStyle(id));

        s.setWorkspace(null);
        persist(s, workspaceStyleLocation);
        getLoader().reload();

        assertNull(catalog.getStyle(id));
    }

    @Test
    public void testMistmatchStyleWorkspaceIgnored() throws Exception {
        StyleInfoImpl s = (StyleInfoImpl) ModificationProxy.unwrap(getCatalog().getStyleByName("point"));
        String id = "testMistmatchStyleWorkspaceIgnored";
        s.setId(id);
        s.setName(id);

        Catalog catalog = getCatalog();
        WorkspaceInfo workspace1 = requireNonNull(catalog.getWorkspaceByName("wcs"));
        WorkspaceInfo workspace2 = requireNonNull(catalog.getWorkspaceByName("cgf"));

        s.setWorkspace(workspace1);
        Resource workspaceStyleLocation = getDataDirectory().config(s);

        persist(s, workspaceStyleLocation);
        assertNull(catalog.getStyle(id));
        getLoader().reload();
        assertEquals(s, catalog.getStyle(id));

        s.setWorkspace(workspace2);
        persist(s, workspaceStyleLocation);
        getLoader().reload();

        assertNull(catalog.getStyle(id));
    }

    @Test
    public void testLayerGroupInfo() throws Exception {
        LayerGroupInfoImpl layerGroup =
                support.createLayerGroup("globalLG", getCatalog().getLayers());
        layerGroup.setId("testLayerGroupInfo");

        Catalog catalog = getCatalog();

        persist(layerGroup, getDataDirectory().config(layerGroup));

        assertNull(catalog.getLayerGroup(layerGroup.getId()));
        getLoader().reload();
        assertEquals(layerGroup, getCatalog().getLayerGroup(layerGroup.getId()));
    }

    @Test
    public void testLayerGroupInfoWorkspace() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = requireNonNull(catalog.getWorkspaceByName("cite"));
        LayerInfo l1 = requireNonNull(catalog.getLayerByName("cite:Bridges"));
        LayerInfo l2 = requireNonNull(catalog.getLayerByName("cite:Buildings"));

        LayerGroupInfoImpl layerGroup = support.createLayerGroup("workspaceLG", List.of(l1, l2));
        layerGroup.setId("testLayerGroupInfo");
        layerGroup.setWorkspace(ws);

        persist(layerGroup, getDataDirectory().config(layerGroup));

        assertNull(catalog.getLayerGroup(layerGroup.getId()));
        getLoader().reload();
        assertEquals(layerGroup, catalog.getLayerGroup(layerGroup.getId()));
    }

    @Test
    public void testLayerGroupInfoWorkspaceWithLayersFromOtherWorkspaceIgnored() throws Exception {
        Catalog catalog = getCatalog();

        WorkspaceInfo rightWorkspace = requireNonNull(catalog.getWorkspaceByName("cite"));
        WorkspaceInfo wrongWorkspace = requireNonNull(catalog.getWorkspaceByName("wcs"));

        LayerInfo l1 = requireNonNull(catalog.getLayerByName("cite:Bridges"));
        LayerInfo l2 = requireNonNull(catalog.getLayerByName("cite:Buildings"));

        LayerGroupInfoImpl layerGroup = support.createLayerGroup("workspaceLG", List.of(l1, l2));
        layerGroup.setId("testLayerGroupInfoWorkspaceWithLayersFromOtherWorkspaceIgnored");
        layerGroup.setWorkspace(rightWorkspace);
        Resource rightResource = getDataDirectory().config(layerGroup);

        layerGroup.setWorkspace(wrongWorkspace);
        persist(layerGroup, rightResource);

        assertNull(catalog.getLayerGroup(layerGroup.getId()));
        getLoader().reload();
        assertNull(catalog.getLayerGroup(layerGroup.getId()));

        layerGroup.setWorkspace(rightWorkspace);
        persist(layerGroup, rightResource);
        getLoader().reload();
        assertEquals(layerGroup, catalog.getLayerGroup(layerGroup.getId()));
    }

    @Test
    public void unparseableDataStorePasswordDisablesIt() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = requireNonNull(catalog.getWorkspaceByName("wcs"));
        DataStoreInfoImpl store = support.createPostgisStore(ws);
        store.setId("unparseableDataStorePasswordDisablesIt");
        store.setEnabled(true);

        String unparseablePasswd = "crypt1:non-sense";
        store.getConnectionParameters().put(JDBCDataStoreFactory.PASSWD.key, unparseablePasswd);

        Resource resource = getDataDirectory().config(store);
        persist(store, resource);
        assertNull(catalog.getStore(store.getId(), StoreInfo.class));
        getLoader().reload();
        StoreInfo loaded = catalog.getStore(store.getId(), StoreInfo.class);
        assertNotNull(loaded);
        assertFalse(loaded.isEnabled());

        store.setEnabled(false);
        assertEquals(store, loaded);
    }

    @Test
    public void unparseableHttpStorePasswordDisablesIt() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = requireNonNull(catalog.getWorkspaceByName("wcs"));
        WMSStoreInfoImpl store = support.createWmsStore(ws);
        store.setId("unparseableHttpStorePasswordDisablesIt");
        store.setEnabled(true);

        String unparseablePasswd = "crypt1:non-sense";
        store.setPassword(unparseablePasswd);

        Resource resource = getDataDirectory().config(store);
        persist(store, resource);
        assertNull(catalog.getStore(store.getId(), StoreInfo.class));
        getLoader().reload();
        StoreInfo loaded = catalog.getStore(store.getId(), StoreInfo.class);
        assertNotNull(loaded);
        assertFalse(loaded.isEnabled());

        store.setEnabled(false);
        assertEquals(store, loaded);
    }

    @Test
    public void settingsInfoWithNoWorkspaceIsIgnored() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = requireNonNull(catalog.getWorkspaceByName("wcs"));
        SettingsInfo settings = support.createSettings(ws);

        Resource resource = getDataDirectory().config(settings);
        settings.setWorkspace(null);
        persist(settings, resource);

        assertNull(getGeoServer().getSettings(ws));
        getLoader().reload();
        assertNull(getGeoServer().getSettings(ws));

        settings.setWorkspace(ws);
        persist(settings, resource);
        getLoader().reload();
        assertEquals(settings, getGeoServer().getSettings(ws));
    }

    @Test
    public void settingsInfoWithNonExistingWorkspaceIsIgnored() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo correctWs = requireNonNull(catalog.getWorkspaceByName("wcs"));
        SettingsInfo settings = support.createSettings(correctWs);

        Resource resource = getDataDirectory().config(settings);

        WorkspaceInfoImpl missingWs = support.createWorkspace("nonExistentWorkspace");
        missingWs.setId("invalid-ws-id");
        settings.setWorkspace(missingWs);
        persist(settings, resource);

        assertNull(getGeoServer().getSettings(correctWs));
        getLoader().reload();
        assertNull(getGeoServer().getSettings(correctWs));

        settings.setWorkspace(correctWs);
        persist(settings, resource);
        getLoader().reload();
        assertEquals(settings, getGeoServer().getSettings(correctWs));
    }

    @Test
    public void settingsInfoWithNullWorkspaceIdIsIgnored() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo correctWs = requireNonNull(catalog.getWorkspaceByName("wcs"));
        SettingsInfo settings = support.createSettings(correctWs);

        Resource resource = getDataDirectory().config(settings);

        WorkspaceInfoImpl nullIdWorkspace = support.createWorkspace("nullIdWorkspace");
        nullIdWorkspace.setId(null);
        settings.setWorkspace(nullIdWorkspace);
        persist(settings, resource);

        assertNull(getGeoServer().getSettings(correctWs));
        getLoader().reload();
        assertNull(getGeoServer().getSettings(correctWs));

        settings.setWorkspace(correctWs);
        persist(settings, resource);
        getLoader().reload();
        assertEquals(settings, getGeoServer().getSettings(correctWs));
    }

    @Test
    public void settingsInfoWithInvalidWorkspaceIsIgnored() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo correctWs = requireNonNull(catalog.getWorkspaceByName("wcs"));
        SettingsInfo settings = support.createSettings(correctWs);

        Resource resource = getDataDirectory().config(settings);

        WorkspaceInfo wrongWorkspace = requireNonNull(catalog.getWorkspaceByName("cite"));
        settings.setWorkspace(wrongWorkspace);
        persist(settings, resource);

        assertNull(getGeoServer().getSettings(correctWs));
        getLoader().reload();
        assertNull(getGeoServer().getSettings(correctWs));

        settings.setWorkspace(correctWs);
        persist(settings, resource);
        getLoader().reload();
        assertEquals(settings, getGeoServer().getSettings(correctWs));
    }

    @Test
    public void testServiceInfoNullWorkspace() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = requireNonNull(catalog.getWorkspaceByName("wcs"));
        GeoServer geoServer = getGeoServer();
        support.setUpServiceLoaders(geoServer);

        TestService1 service = support.serviceInfo1(ws, username, geoServer);

        String filename = support.serviceLoader1.getFilename();
        Resource resource = getDataDirectory().getWorkspaces(ws.getName(), filename);

        service.setWorkspace(null);
        persist(service, resource);
        getLoader().reload();
        assertNull(geoServer.getService(service.getId(), ServiceInfo.class));

        service.setWorkspace(ws);
        persist(service, resource);
        getLoader().reload();
        ServiceInfo loaded = geoServer.getService(service.getId(), ServiceInfo.class);
        loaded = ModificationProxy.unwrap(loaded);
        assertEquals(service, loaded);
    }

    @Test
    public void initializeDefaultStyle() throws Exception {
        GeoServerResourceLoader resourceLoader = getResourceLoader();
        Resource styles = resourceLoader.get("styles");
        assertTrue(styles.dir().isDirectory());

        deleteStyle(StyleInfo.DEFAULT_POINT, "default_point.sld");
        deleteStyle(StyleInfo.DEFAULT_LINE, "default_line.sld");
        deleteStyle(StyleInfo.DEFAULT_POLYGON, "default_polygon.sld");
        deleteStyle(StyleInfo.DEFAULT_RASTER, "default_raster.sld");
        deleteStyle(StyleInfo.DEFAULT_GENERIC, "default_generic.sld");

        getLoader().reload();

        styles = resourceLoader.get("styles");
        assertEquals(RESOURCE, styles.get(StyleInfo.DEFAULT_POINT + ".xml").getType());
        assertEquals(RESOURCE, styles.get("default_point.sld").getType());
        assertEquals(RESOURCE, styles.get(StyleInfo.DEFAULT_LINE + ".xml").getType());
        assertEquals(RESOURCE, styles.get("default_line.sld").getType());
        assertEquals(RESOURCE, styles.get(StyleInfo.DEFAULT_POLYGON + ".xml").getType());
        assertEquals(RESOURCE, styles.get("default_polygon.sld").getType());
        assertEquals(RESOURCE, styles.get(StyleInfo.DEFAULT_RASTER + ".xml").getType());
        assertEquals(RESOURCE, styles.get("default_raster.sld").getType());
        assertEquals(RESOURCE, styles.get(StyleInfo.DEFAULT_GENERIC + ".xml").getType());
        assertEquals(RESOURCE, styles.get("default_generic.sld").getType());
    }

    private void deleteStyle(String infoName, String sldFile) {
        GeoServerResourceLoader resourceLoader = getResourceLoader();
        Resource styles = resourceLoader.get("styles");
        Resource xml = styles.get(infoName + ".xml");
        Resource sld = styles.get(sldFile);
        assertTrue(!Resources.exists(xml) || xml.delete());
        assertTrue(!Resources.exists(sld) || sld.delete());
    }

    private void persist(Info info, Resource file) throws IOException {
        try (OutputStream out = file.out()) {
            XStreamPersister persister = persister();
            persister.setEncryptPasswordFields(false);
            persister.save(info, out);
        }
    }

    private XStreamPersister persister() {
        return new XStreamPersisterFactory().createXMLPersister();
    }

    private void assertPasswordEncrypted(DataStoreInfo store) {
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        persister.setEncryptPasswordFields(false);
        XStream xStream = persister.getXStream();
        Resource resource = super.getDataDirectory().config(store);
        DataStoreInfo depresisted = (DataStoreInfo) xStream.fromXML(resource.file());

        String pwdParam = JDBCDataStoreFactory.PASSWD.key;
        Serializable encodedPwd = depresisted.getConnectionParameters().get(pwdParam);
        String plainPassword = (String) store.getConnectionParameters().get(pwdParam);
        assertNotNull(plainPassword);
        assertNotEquals(plainPassword, encodedPwd);
    }

    private void assertPasswordEncrypted(WMSStoreInfo store) {
        XStreamPersister persister = new XStreamPersisterFactory().createXMLPersister();
        persister.setEncryptPasswordFields(false);
        XStream xStream = persister.getXStream();
        Resource resource = super.getDataDirectory().config(store);
        WMSStoreInfo depresisted = (WMSStoreInfo) xStream.fromXML(resource.file());
        Serializable encodedPwd = depresisted.getPassword();
        assertNotNull(encodedPwd);
        String plainPassword = store.getPassword();
        assertNotEquals(plainPassword, encodedPwd);
    }

    private DataDirectoryGeoServerLoader newLoader() {
        GeoServerResourceLoader resourceLoader = getResourceLoader();
        GeoServerSecurityManager secManager = getSecurityManager();
        GeoServerConfigurationLock configLock = GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        return newLoader(resourceLoader, secManager, configLock);
    }

    private DataDirectoryGeoServerLoader newLoader(
            GeoServerResourceLoader resourceLoader,
            GeoServerSecurityManager secManager,
            GeoServerConfigurationLock configLock) {
        GeoServerDataDirectory dataDirectory = new GeoServerDataDirectory(resourceLoader);
        return new DataDirectoryGeoServerLoader(dataDirectory, secManager, configLock);
    }
}
