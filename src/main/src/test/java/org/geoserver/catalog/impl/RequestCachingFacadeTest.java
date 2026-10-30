/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.DefaultGeoServerFacade;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.filters.RequestCachingFilter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RequestCachingFacadeTest {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /** Calls that reached the backend facades, by method name. */
    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();

    private CatalogImpl catalog;
    private GeoServerImpl geoServer;
    private WorkspaceInfo ws;
    private FeatureTypeInfo featureType;

    @Before
    public void setUp() {
        catalog = new CatalogImpl();
        catalog.setFacade(
                RequestCachingFacade.create(counting(new DefaultCatalogFacade(catalog), CatalogFacade.class)));
        geoServer = new GeoServerImpl();
        geoServer.setCatalog(catalog);
        geoServer.setFacade(
                RequestCachingFacade.create(counting(new DefaultGeoServerFacade(geoServer), GeoServerFacade.class)));

        CatalogFactory factory = catalog.getFactory();
        ws = factory.createWorkspace();
        ws.setName("ws");
        catalog.add(ws);
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix("ws");
        ns.setURI("http://ws");
        catalog.add(ns);
        DataStoreInfo store = factory.createDataStore();
        store.setName("store");
        store.setWorkspace(ws);
        catalog.add(store);
        featureType = factory.createFeatureType();
        featureType.setName("ft");
        featureType.setNativeName("ft");
        featureType.setStore(store);
        featureType.setNamespace(ns);
        catalog.add(featureType);
        ServiceInfo service = new ServiceInfoImpl();
        service.setName("svc");
        geoServer.add(service);
        calls.clear();
    }

    @After
    public void tearDown() {
        RequestCachingFacade.close();
    }

    @SuppressWarnings("unchecked")
    private <T> T counting(T facade, Class<T> type) {
        return ProxyUtils.createProxy(facade, type, (proxy, method, args) -> {
            calls.computeIfAbsent(method.getName(), k -> new AtomicInteger()).incrementAndGet();
            try {
                return method.invoke(facade, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private int calls(String method) {
        AtomicInteger count = calls.get(method);
        return count == null ? 0 : count.get();
    }

    /** The same lookup twice in a request reaches the backend once, found or not; a new request asks again. */
    @Test
    public void testLookupOnce() {
        RequestCachingFacade.open();
        assertEquals("ws", catalog.getWorkspaceByName("ws").getName());
        assertEquals("ws", catalog.getWorkspaceByName("ws").getName());
        assertNull(catalog.getWorkspaceByName("missing"));
        assertNull(catalog.getWorkspaceByName("missing"));
        assertNull(geoServer.getService(ws, ServiceInfo.class));
        assertNull(geoServer.getService(ws, ServiceInfo.class));
        assertNull(geoServer.getSettings(ws));
        assertNull(geoServer.getSettings(ws));
        assertEquals(2, calls("getWorkspaceByName"));
        assertEquals(1, calls("getService"));
        assertEquals(1, calls("getSettings"));

        RequestCachingFacade.close();
        RequestCachingFacade.open();
        assertNull(catalog.getWorkspaceByName("missing"));
        assertEquals(3, calls("getWorkspaceByName"));
    }

    /** Lookups with the facade sentinels as arguments are cached too. */
    @Test
    public void testSentinels() {
        RequestCachingFacade.open();
        // NO_WORKSPACE first, then the default workspace
        assertNull(catalog.getLayerGroupByName("missing"));
        assertNull(catalog.getLayerGroupByName("missing"));
        assertEquals(2, calls("getLayerGroupByName"));
        // the default workspace first, then any workspace
        assertNull(catalog.getStyleByName("missing"));
        assertNull(catalog.getStyleByName("missing"));
        assertEquals(2, calls("getStyleByName"));
    }

    /** Two wrapped catalogs on one thread answer from their own entries. */
    @Test
    public void testTwoFacades() {
        CatalogImpl other = new CatalogImpl();
        other.setFacade(RequestCachingFacade.create(new DefaultCatalogFacade(other)));
        RequestCachingFacade.open();
        assertEquals("ws", catalog.getWorkspaceByName("ws").getName());
        assertNull(other.getWorkspaceByName("ws"));
    }

    /** A lookup by id through a filter, as ModificationProxy.readResolve sends it, reaches the backend once. */
    @Test
    public void testIdFilter() {
        RequestCachingFacade.open();
        String id = featureType.getId();
        FeatureTypeInfo first = catalog.get(FeatureTypeInfo.class, byId(id));
        FeatureTypeInfo second = catalog.get(FeatureTypeInfo.class, byId(id));
        assertNull(catalog.get(FeatureTypeInfo.class, byId("missing")));
        assertNull(catalog.get(FeatureTypeInfo.class, byId("missing")));
        assertEquals("ft", first.getName());
        assertEquals("ft", second.getName());
        assertEquals(2, calls("list"));
        try (CloseableIterator<FeatureTypeInfo> none = catalog.list(FeatureTypeInfo.class, byId(id), null, 0, null)) {
            assertFalse(none.hasNext());
        }
        assertEquals(3, calls("list"));

        // other filters are not cached
        catalog.get(FeatureTypeInfo.class, FF.equals(FF.property("name"), FF.literal("ft")));
        catalog.get(FeatureTypeInfo.class, FF.equals(FF.property("name"), FF.literal("ft")));
        assertEquals(5, calls("list"));
    }

    private static Filter byId(String id) {
        return FF.equals(FF.property("id"), FF.literal(id));
    }

    /** Each hit is a new proxy, so a change not yet saved stays with its caller. */
    @Test
    public void testNewProxyPerHit() {
        RequestCachingFacade.open();
        FeatureTypeInfo first = catalog.getFeatureTypeByName("ws", "ft");
        FeatureTypeInfo second = catalog.getFeatureTypeByName("ws", "ft");
        FeatureTypeInfo third = catalog.getFeatureTypeByName("ws", "ft");
        assertEquals(1, calls("getResourceByName"));
        assertNotSame(second, third);
        second.setTitle("changed");
        assertNull(third.getTitle());
        assertNull(first.getTitle());
        assertNull(catalog.getFeatureTypeByName("ws", "ft").getTitle());
    }

    /** A request that saves and reads back sees the new value, for catalog objects, services and settings. */
    @Test
    public void testWriteThenRead() {
        RequestCachingFacade.open();
        FeatureTypeInfo ft = catalog.getFeatureTypeByName("ws", "ft");
        ft.setTitle("new title");
        catalog.save(ft);
        assertEquals("new title", catalog.getFeatureTypeByName("ws", "ft").getTitle());

        ServiceInfo service = geoServer.getService(ServiceInfo.class);
        service.setTitle("new service");
        geoServer.save(service);
        assertEquals("new service", geoServer.getService(ServiceInfo.class).getTitle());

        assertNull(geoServer.getSettings(ws));
        SettingsInfo settings = geoServer.getFactory().createSettings();
        settings.setWorkspace(ws);
        settings.setTitle("ws settings");
        geoServer.add(settings);
        assertEquals("ws settings", geoServer.getSettings(ws).getTitle());

        assertNull(catalog.getWorkspaceByName("added"));
        WorkspaceInfo added = catalog.getFactory().createWorkspace();
        added.setName("added");
        catalog.add(added);
        assertEquals("added", catalog.getWorkspaceByName("added").getName());
    }

    /** Two requests on two threads keep separate maps, and a write in one does not clear the other. */
    @Test
    public void testConcurrentRequests() throws Exception {
        ExecutorService a = Executors.newSingleThreadExecutor();
        ExecutorService b = Executors.newSingleThreadExecutor();
        try {
            run(a, RequestCachingFacade::open);
            run(b, RequestCachingFacade::open);
            run(a, () -> catalog.getWorkspaceByName("missing"));
            run(b, () -> catalog.getWorkspaceByName("missing"));
            assertEquals(2, calls("getWorkspaceByName"));

            // b adds the workspace: its own map is cleared, a still sees its own answer
            run(b, () -> {
                WorkspaceInfo added = catalog.getFactory().createWorkspace();
                added.setName("missing");
                catalog.add(added);
            });
            run(a, () -> assertNull(catalog.getWorkspaceByName("missing")));
            run(b, () -> assertNotNull(catalog.getWorkspaceByName("missing")));
            assertEquals(3, calls("getWorkspaceByName"));
        } finally {
            run(a, RequestCachingFacade::close);
            run(b, RequestCachingFacade::close);
            a.shutdown();
            b.shutdown();
        }
    }

    private static void run(ExecutorService executor, Runnable task) throws Exception {
        executor.submit(task).get();
    }

    /** Lookups from a thread other than the request thread go to the backend. */
    @Test
    public void testOtherThread() throws Exception {
        RequestCachingFacade.open();
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            run(other, () -> catalog.getWorkspaceByName("missing"));
            run(other, () -> catalog.getWorkspaceByName("missing"));
        } finally {
            other.shutdown();
        }
        assertEquals(2, calls("getWorkspaceByName"));
    }

    /** A request that fails leaves no map behind on its thread. */
    @Test
    public void testErrorClearsCache() {
        FilterChain failing = (request, response) -> {
            catalog.getWorkspaceByName("missing");
            catalog.getWorkspaceByName("missing");
            throw new IOException("failed");
        };
        assertThrows(IOException.class, () -> new RequestCachingFilter().doFilter(null, null, failing));
        assertEquals(1, calls("getWorkspaceByName"));

        catalog.getWorkspaceByName("missing");
        catalog.getWorkspaceByName("missing");
        assertEquals(3, calls("getWorkspaceByName"));
    }

    /** A forward runs the filter again inside the request, and must not drop the outer map. */
    @Test
    public void testNestedFilter() throws Exception {
        RequestCachingFilter filter = new RequestCachingFilter();
        FilterChain inner = (request, response) -> catalog.getWorkspaceByName("missing");
        FilterChain outer = (request, response) -> {
            filter.doFilter(request, response, inner);
            catalog.getWorkspaceByName("missing");
        };
        filter.doFilter(null, null, outer);
        assertEquals(1, calls("getWorkspaceByName"));
    }

    /** With the property set to false the facade is not wrapped. */
    @Test
    public void testDisabled() {
        System.setProperty(RequestCachingFacade.ENABLED_PROPERTY, "false");
        try {
            CatalogFacade facade = new DefaultCatalogFacade(catalog);
            assertSame(facade, RequestCachingFacade.create(facade));
        } finally {
            System.clearProperty(RequestCachingFacade.ENABLED_PROPERTY);
        }
    }
}
