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
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.geoserver.catalog.Catalog;
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
}
