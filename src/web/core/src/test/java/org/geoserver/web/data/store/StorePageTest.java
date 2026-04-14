/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.api.filter.Filter;
import org.junit.Before;
import org.junit.Test;

public class StorePageTest extends GeoServerWicketTestSupport {

    @Before
    public void init() {
        login();
        tester.startPage(StorePage.class);

        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStores(StoreInfo.class).size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StoreProvider);

        // Cast to StoreProvider
        StoreProvider provider = (StoreProvider) dataProvider;

        // Ensure that an unsupportedException is thrown when requesting the Items directly
        boolean catchedException = false;
        try {
            provider.getItems();
        } catch (UnsupportedOperationException e) {
            catchedException = true;
        }

        // Ensure the exception is cacthed
        assertTrue(catchedException);

        StoreInfo actual = provider.iterator(0, 1).next();
        try (CloseableIterator<StoreInfo> list =
                catalog.list(StoreInfo.class, Filter.INCLUDE, 0, 1, Predicates.sortBy("name", true))) {
            assertTrue(list.hasNext());
            StoreInfo expected = list.next();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testTimeColumnsToggle() {

        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);
        login();
        tester.startPage(StorePage.class);
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStores(StoreInfo.class).size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StoreProvider);

        // Cast to StoreProvider
        StoreProvider provider = (StoreProvider) dataProvider;

        // should show both columns
        assertTrue(provider.getProperties().contains(StoreProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(StoreProvider.MODIFIED_TIMESTAMP));
    }

    @Test
    public void testSerializedProvider() throws Exception {
        StoreProvider provider = new StoreProvider();

        byte[] serialized;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(provider);
            }
            serialized = os.toByteArray();
        }
        StoreProvider provider2;
        try (ByteArrayInputStream is = new ByteArrayInputStream(serialized)) {
            try (ObjectInputStream ois = new ObjectInputStream(is)) {
                provider2 = (StoreProvider) ois.readObject();
            }
        }

        assertEquals(provider.getProperties(), provider2.getProperties());
    }

    @Test
    public void testModificationUserColumnToggle() {

        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowModifiedUserInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);
        login();
        tester.startPage(StorePage.class);
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStores(StoreInfo.class).size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StoreProvider);

        // Cast to StoreProvider
        StoreProvider provider = (StoreProvider) dataProvider;

        // should show both columns
        assertTrue(provider.getProperties().contains(StoreProvider.MODIFIED_BY));
    }

    @Test
    public void testWorkspaceParameterFiltersStores() {
        Catalog catalog = getCatalog();
        List<StoreInfo> citeStores = catalog.getStoresByWorkspace("cite", StoreInfo.class);

        tester.startPage(StorePage.class, new PageParameters().add("workspace", "cite"));
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(citeStores.size(), dv.size());

        // verify every store in the rendered page belongs to the "cite" workspace
        IDataProvider dataProvider = dv.getDataProvider();
        assertTrue(dataProvider instanceof StoreProvider);
        StoreProvider provider = (StoreProvider) dataProvider;
        Iterator<StoreInfo> it = provider.iterator(0, dv.size());
        while (it.hasNext()) {
            StoreInfo store = it.next();
            assertEquals("cite", store.getWorkspace().getName());
        }
    }

    @Test
    public void testLayerParameterFiltersToBackingStore() {
        // The layer param is the unqualified name; the backing store is "cite"
        LayerInfo layer = getCatalog().getLayerByName("cite:BasicPolygons");
        StoreInfo backingStore = layer.getResource().getStore();

        tester.startPage(StorePage.class, new PageParameters().add("layer", "BasicPolygons"));
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());

        StoreProvider provider = (StoreProvider) dv.getDataProvider();
        StoreInfo rendered = provider.iterator(0, 1).next();
        assertEquals(backingStore.getId(), rendered.getId());
    }

    @Test
    public void testLayerAndWorkspaceParametersFiltersStores() {
        // Typical URL: ?workspace=cite&layer=BasicPolygons
        // Both params match: BasicPolygons is backed by the "cite" store in workspace "cite"
        LayerInfo layer = getCatalog().getLayerByName("cite:BasicPolygons");
        StoreInfo backingStore = layer.getResource().getStore();

        tester.startPage(
                StorePage.class, new PageParameters().add("workspace", "cite").add("layer", "BasicPolygons"));
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());

        StoreProvider provider = (StoreProvider) dv.getDataProvider();
        StoreInfo rendered = provider.iterator(0, 1).next();
        assertEquals(backingStore.getId(), rendered.getId());
        assertEquals("cite", rendered.getWorkspace().getName());
    }

    @Test
    public void testLayerAndMismatchedWorkspaceYieldsNoStores() {
        // Typical URL: ?workspace=sf&layer=BasicPolygons
        // BasicPolygons is backed by the "cite" store, so the "sf" workspace filter excludes it
        tester.startPage(
                StorePage.class, new PageParameters().add("workspace", "sf").add("layer", "BasicPolygons"));
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(0, dv.size());
    }
}
