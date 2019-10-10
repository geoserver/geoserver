/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.*;

import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

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

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
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
        CloseableIterator<StoreInfo> list =
                catalog.list(
                        StoreInfo.class, Filter.INCLUDE, 0, 1, Predicates.sortBy("name", true));
        assertTrue(list.hasNext());
        StoreInfo expected = list.next();

        // Close the iterator
        try {
            if (list != null) {
                list.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(expected, actual);
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

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
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
}
