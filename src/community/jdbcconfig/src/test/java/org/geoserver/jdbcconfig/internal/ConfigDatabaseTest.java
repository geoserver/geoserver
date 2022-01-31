/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.jdbcconfig.internal;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opengis.util.ProgressListener;

/** @author groldan */
@RunWith(Parameterized.class)
public class ConfigDatabaseTest {

    private JDBCConfigTestSupport testSupport;

    private ConfigDatabase database;

    private GeoServer geoServer;

    public ConfigDatabaseTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        testSupport = new JDBCConfigTestSupport(dbConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    @Before
    public void setUp() throws Exception {
        testSupport.setUp();
        database = testSupport.getDatabase();

        // Mock the GeoServer instance to accept a listener, and to provide that listener back when
        // asked
        geoServer = createMock(GeoServer.class);
        final Capture<ConfigurationListener> cap = Capture.newInstance(CaptureType.LAST);
        geoServer.addListener(capture(cap));
        expectLastCall().asStub();
        expect(geoServer.getListeners())
                .andStubAnswer(
                        new IAnswer<Collection<ConfigurationListener>>() {

                            @Override
                            public Collection<ConfigurationListener> answer() throws Throwable {
                                return cap.getValues();
                            }
                        });
        replay(geoServer);

        database.setGeoServer(geoServer);
    }

    @After
    public void tearDown() throws Exception {
        verify(geoServer);
        database.dispose();
        testSupport.tearDown();
    }

    @Test
    public void testAdd() throws Exception {

        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        try {
            database.add(ws);
            fail("Expected NPE on null id");
        } catch (NullPointerException e) {
            assertEquals("Object has no id", e.getMessage());
        }
        ws.setId("wsid");
        ws.setName("ws1");

        WorkspaceInfo addedWs = database.add(ws);
        assertNotNull(addedWs);
        assertTrue(addedWs instanceof Proxy);
        assertEquals(ws, addedWs);

        DataStoreInfo addedDs = addDataStore(ws);
        assertNotNull(addedDs);
    }

    private DataStoreInfo addDataStore(WorkspaceInfo ws) {
        Catalog catalog = database.getCatalog();
        DataStoreInfoImpl ds = new DataStoreInfoImpl(catalog);
        ds.setWorkspace(ws);
        ds.setId("ds1");
        ds.getConnectionParameters().put("param1", "value1");
        ds.getConnectionParameters().put("param2", "value2");
        ds.setName("data store one");
        ds.setDescription("data store description one");
        ds.setEnabled(true);
        ds.setType("Foo");

        DataStoreInfo addedDs = database.add(ds);
        return addedDs;
    }

    @Test
    public void testModifyWorkspace() throws Exception {
        WorkspaceInfo ws = addWorkspace();
        ws.setName("newName");
        testSaved(ws);
    }

    private WorkspaceInfo addWorkspace() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ((WorkspaceInfoImpl) ws).setId("wsid");
        ws.setName("ws1");

        ws = database.add(ws);
        return ws;
    }

    private LayerInfo addLayer() {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setId("wsid");
        ws.setName("ws1");
        database.add(ws);

        NamespaceInfo ns = addNamespace();

        Catalog catalog = database.getCatalog();
        DataStoreInfoImpl ds = new DataStoreInfoImpl(catalog);
        ds.setWorkspace(ws);
        ds.setId("ds1");
        ds.getConnectionParameters().put("param1", "value1");
        ds.getConnectionParameters().put("param2", "value2");
        ds.setName("data store one");
        ds.setDescription("data store description one");
        ds.setEnabled(true);
        ds.setType("Foo");
        database.add(ds);

        ResourceInfo ri = new FeatureTypeInfoImpl(catalog);
        ((FeatureTypeInfoImpl) ri).setId("resourceid");
        ri.setNamespace(ns);
        ri.setName("ri1");
        ri.setStore(ds);
        ri = database.add(ri);

        LayerInfo li = new LayerInfoImpl();
        ((LayerInfoImpl) li).setId("layerid");
        li.setResource(ri);
        li = database.add(li);

        return li;
    }

    private NamespaceInfo addNamespace() {
        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setId("nsid");
        ns.setPrefix("ws1");
        database.add(ns);
        NamespaceInfo byId = database.getById(ns.getId(), NamespaceInfo.class);
        assertEquals(ns, byId);
        return byId;
    }

    /** @param info */
    private void testSaved(Info info) {
        Info saved = database.save(info);
        assertNotSame(info, saved);
        if (info instanceof DataStoreInfo) {
            assertEquals(
                    ((DataStoreInfo) info).getWorkspace(), ((DataStoreInfo) saved).getWorkspace());
        }
        assertEquals(info, saved);
    }

    @Test
    public void testRemoveWorkspace() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ((WorkspaceInfoImpl) ws).setId("removeid");
        ws.setName("remove");
        ws = database.add(ws);
        assertNotNull(database.getById(ws.getId(), WorkspaceInfo.class));
        // org.geoserver.catalog.NamespaceWorkspaceConsistencyListener.handleRemoveEvent(CatalogRemoveEvent)
        // can cause remove to actually be called twice on the workspace.
        database.remove(ws);
        database.remove(ws);
        // Notify of update
        database.getCatalog().fireRemoved(ws);

        assertNull(database.getById(ws.getId(), WorkspaceInfo.class));
    }

    @Test
    public void testModifyService() {

        // Create a service to modify
        WMSInfo service = new WMSInfoImpl();
        ((WMSInfoImpl) service).setId("WMS-TEST");
        service.setName("WMS");
        service.setMaintainer("Foo");

        service = database.add(service);

        assertEquals(service.getMaintainer(), "Foo");

        service.setMaintainer("Bar");
        testSaved(service);
    }

    @Test
    public void testCacheCatalog() throws Exception {
        // Simulates the situation where multiple GeoServer instances are sharing a database.

        WorkspaceInfo ws = addWorkspace();
        ws.setName("name1");
        testSaved(ws);
        // test identity cache
        ws = database.getByIdentity(WorkspaceInfo.class, "name", "name1");
        assertNotNull(ws);

        // Change the stored configuration
        // KS: sorry, this is an utter kludge
        Connection conn = testSupport.getDataSource().getConnection();
        try {
            Statement stmt = conn.createStatement();
            assertEquals(
                    1,
                    stmt.executeUpdate(
                            "UPDATE object_property SET value='name2' WHERE property_type=(SELECT oid FROM property_type WHERE type_id = (SELECT oid FROM type WHERE typename='org.geoserver.catalog.WorkspaceInfo') AND name='name') AND id = '"
                                    + ws.getId()
                                    + "'"));
            assertEquals(
                    1,
                    stmt.executeUpdate(
                            "UPDATE object SET blob=(SELECT replace(blob, '<name>name1</name>', '<name>name2</name>') FROM object WHERE id = '"
                                    + ws.getId()
                                    + "')"));
        } finally {
            conn.close();
        }

        // Should be cached
        WorkspaceInfo ws2 = database.getById(ws.getId(), WorkspaceInfo.class);
        assertEquals("name1", ws2.getName());

        // Notify of update
        testSupport
                .getCatalog()
                .fireModified(
                        ws2, Arrays.asList("name"), Arrays.asList("name1"), Arrays.asList("name2"));
        ws2.setName("name2");
        ModificationProxy.handler(ws2).commit();
        testSupport
                .getCatalog()
                .firePostModified(
                        ws2, Arrays.asList("name"), Arrays.asList("name1"), Arrays.asList("name2"));

        // Should show the new value
        WorkspaceInfo ws3 = database.getById(ws.getId(), WorkspaceInfo.class);
        assertEquals("name2", ws3.getName());
        // test identity cache update
        ws3 = database.getByIdentity(WorkspaceInfo.class, "name", "name1");
        assertNull(ws3);
        ws3 = database.getByIdentity(WorkspaceInfo.class, "name", "name2");
        assertNotNull(ws3);
    }

    @Test
    public void testCacheResourceLayer() throws Exception {
        // check that saving a resource updates the layer cache
        LayerInfo layer = addLayer();
        ResourceInfo resourceInfo =
                database.getById(layer.getResource().getId(), ResourceInfo.class);
        resourceInfo.setName("rs2");
        testSaved(resourceInfo);
        layer = database.getById(layer.getId(), LayerInfo.class);
        assertEquals("rs2", layer.getResource().getName());
    }

    @Test
    public void testCacheResourceLayerLocked() throws Exception {
        // check that saving a resource updates the layer cache
        LayerInfo layer = addLayer();
        ResourceInfo resourceInfo =
                database.getById(layer.getResource().getId(), ResourceInfo.class);
        resourceInfo.setName("rs2");
        testSupport
                .getCatalog()
                .addListener(
                        new CatalogListener() {

                            @Override
                            public void handleAddEvent(CatalogAddEvent event)
                                    throws CatalogException {}

                            @Override
                            public void handleRemoveEvent(CatalogRemoveEvent event)
                                    throws CatalogException {}

                            @Override
                            public void handleModifyEvent(CatalogModifyEvent event)
                                    throws CatalogException {
                                // this shouldn't cause re-caching because of lock
                                database.getById(layer.getId(), LayerInfo.class);
                            }

                            @Override
                            public void handlePostModifyEvent(CatalogPostModifyEvent event)
                                    throws CatalogException {}

                            @Override
                            public void reloaded() {}
                        });
        testSupport.getFacade().save(resourceInfo);
        LayerInfo layer2 = database.getById(layer.getId(), LayerInfo.class);
        assertEquals("rs2", layer2.getResource().getName());
    }

    @Test
    public void testCacheConfig() throws Exception {
        // Simulates the situation where multiple GeoServer instances are sharing a database.

        ServiceInfo service = new WMSInfoImpl();
        ((WMSInfoImpl) service).setId("WMS-TEST");
        service.setName("WMS");
        service.setMaintainer("Foo");

        service = database.add(service);

        assertEquals(service.getMaintainer(), "Foo");

        // Change the stored configuration
        // KS: sorry, this is an utter kludge
        Connection conn = testSupport.getDataSource().getConnection();
        try {
            Statement stmt = conn.createStatement();
            // assertEquals(1, stmt.executeUpdate("UPDATE object_property SET value='Bar' WHERE
            // property_type=(SELECT oid FROM property_type WHERE type_id = (SELECT oid FROM type
            // WHERE typename='org.geoserver.wms.ServiceInfo') AND name='maintainer') AND id =
            // '"+service.getId()+"';"));
            assertEquals(
                    1,
                    stmt.executeUpdate(
                            "UPDATE object SET blob=(SELECT replace(blob, '<maintainer>Foo</maintainer>', '<maintainer>Bar</maintainer>') FROM object WHERE id = '"
                                    + service.getId()
                                    + "')"));
        } finally {
            conn.close();
        }

        // Should be cached
        service = database.getById(service.getId(), ServiceInfo.class);
        assertEquals("Foo", service.getMaintainer());

        // Notify of update
        service.setMaintainer("Bar");
        for (ConfigurationListener l : database.getGeoServer().getListeners()) {
            l.handleServiceChange(service, null, null, null);
        }
        ModificationProxy.handler(service).commit();
        for (ConfigurationListener l : database.getGeoServer().getListeners()) {
            l.handlePostServiceChange(service);
        }

        // Should show the new value
        service = database.getById(service.getId(), ServiceInfo.class);
        assertEquals("Bar", service.getMaintainer());
    }

    @Test
    public void testGetServiceWithGeoServerRef() {
        WMSInfo service = new WMSInfoImpl();
        ((WMSInfoImpl) service).setId("WMS-TEST");
        service.setName("WMS");
        service.setMaintainer("Foo");

        service = database.add(service);
        database.clearCache(service);

        service = database.getAll(WMSInfo.class).iterator().next();
        assertNotNull(service.getGeoServer());
    }

    @Test
    public void testWMSCascadingInfos() throws Exception {
        WorkspaceInfo ws = addWorkspace();

        WMSStoreInfoImpl wmsStore = new WMSStoreInfoImpl(database.getCatalog());
        wmsStore.setCapabilitiesURL(
                ConfigDatabaseTest.class.getResource("/wms_capabilities.xml").toString());
        wmsStore.setId("theWmsStore");
        wmsStore.setName("fakeGeoServer");
        wmsStore.setWorkspace(ws);
        wmsStore.setUseConnectionPooling(false); // allows to hit the file system
        database.add(wmsStore);

        CatalogBuilder cb = new CatalogBuilder(database.getCatalog());
        cb.setStore(wmsStore);
        WMSLayerInfoImpl wmsLayer = (WMSLayerInfoImpl) cb.buildWMSLayer("states");
        wmsLayer.reset();
        wmsLayer.setId("theWmsLayer");
        wmsLayer.setForcedRemoteStyle("population");
        wmsLayer.setSelectedRemoteStyles(new ArrayList<>(Arrays.asList("pophatch", "polygon")));
        database.add(wmsLayer);
        LayerInfoImpl layer = (LayerInfoImpl) cb.buildLayer(wmsLayer);
        layer.setId("theLayer");
        // used to fail here, as the styles are generated on the fly
        database.add(layer);

        StyleInfo defaultStyle = layer.getDefaultStyle();
        assertRemoteStyle(defaultStyle);

        Set<StyleInfo> styles = layer.getStyles();
        assertEquals(2, styles.size());
        styles.forEach(this::assertRemoteStyle);
    }

    @Test
    public void testWMTSStore() throws Exception {
        final WMTSStoreInfo store = addWMTSStore();

        database.clearCache(store);

        StoreInfo byId = database.getById(store.getId(), StoreInfo.class);
        assertEquals(store, byId);
        byId = database.getById(store.getId(), WMTSStoreInfo.class);
        assertEquals(store, byId);

        database.clearCache(store);
        StoreInfo storeByName =
                database.getByIdentity(
                        StoreInfo.class,
                        "workspace.id",
                        store.getWorkspace().getId(),
                        "name",
                        store.getName());
        assertEquals(store, storeByName);

        WebMapTileServer wmts = store.getWebMapTileServer((ProgressListener) null);
        assertNotNull(wmts);
        WMTSCapabilities capabilities = wmts.getCapabilities();
        assertNotNull(capabilities);
        List<WMTSLayer> layerList = capabilities.getLayerList();
        assertEquals(2, layerList.size());
    }

    @Test
    public void testWMTSLayer() throws Exception {
        final WMTSLayerInfo added = addWMTSLayer();

        database.clearCache(added);
        ResourceInfo resourceById = database.getById(added.getId(), ResourceInfo.class);
        assertEquals(added, resourceById);

        database.clearCache(added);
        WMTSLayerInfo wmtsLayerById = database.getById(added.getId(), WMTSLayerInfo.class);
        assertEquals(added, wmtsLayerById);

        NamespaceInfo ns = added.getNamespace();
        String name = added.getName();

        database.clearCache(added);
        ResourceInfo resourceByName =
                database.getByIdentity(
                        ResourceInfo.class, "namespace.id", ns.getId(), "name", name);
        assertEquals(added, resourceByName);
    }

    private WMTSLayerInfo addWMTSLayer() throws IOException {
        final WMTSStoreInfo store = addWMTSStore();
        final NamespaceInfo ns = addNamespace();
        final Catalog catalog = database.getCatalog();
        WMTSLayerInfo wmtsResource = catalog.getFactory().createWMTSLayer();

        WMTSLayer layer =
                store.getWebMapTileServer((ProgressListener) null)
                        .getCapabilities()
                        .getLayer("topp:tasmania_cities");
        assertNotNull(layer);

        OwsUtils.set(wmtsResource, "id", "wmtsResource");
        wmtsResource.setStore(store);
        wmtsResource.setNamespace(ns);
        wmtsResource.setEnabled(true);
        wmtsResource.setName(layer.getName());

        final WMTSLayerInfo added = database.add(wmtsResource);
        assertNotNull(added);
        assertNotSame(wmtsResource, added);
        assertEquals(wmtsResource, added);
        return added;
    }

    private WMTSStoreInfo addWMTSStore() {
        final WorkspaceInfo ws = addWorkspace();
        final Catalog catalog = database.getCatalog();
        WMTSStoreInfo wmtsStore = catalog.getFactory().createWebMapTileServer();
        wmtsStore.setCapabilitiesURL(
                ConfigDatabaseTest.class.getResource("/wmts_capabilities.xml").toString());
        OwsUtils.set(wmtsStore, "id", "theMmtsStore");
        wmtsStore.setName("fakeWMTSStore");
        wmtsStore.setWorkspace(ws);
        wmtsStore.setUseConnectionPooling(false); // allows to hit the file system
        database.add(wmtsStore);

        WMTSStoreInfo info = database.getById(wmtsStore.getId(), WMTSStoreInfo.class);
        assertEquals(wmtsStore, info);
        return info;
    }

    private void assertRemoteStyle(StyleInfo defaultStyle) {
        assertNull(defaultStyle.getId());
        assertEquals(true, defaultStyle.getMetadata().get(StyleInfoImpl.IS_REMOTE, Boolean.class));
    }
}
