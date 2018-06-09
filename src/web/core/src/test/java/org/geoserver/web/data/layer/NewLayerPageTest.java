/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import org.apache.wicket.Component;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.data.DataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.junit.Test;

public class NewLayerPageTest extends GeoServerWicketTestSupport {

    private static final String TABLE_PATH = "selectLayersContainer:selectLayers:layers";

    @Test
    public void testKnownStore() {
        login();
        DataStoreInfo store = getCatalog().getStoreByName(MockData.CDF_PREFIX, DataStoreInfo.class);
        tester.startPage(new NewLayerPage(store.getId()));

        tester.assertRenderedPage(NewLayerPage.class);
        assertNull(tester.getComponentFromLastRenderedPage("selector"));
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage(TABLE_PATH);
        assertEquals(
                getCatalog().getResourcesByStore(store, FeatureTypeInfo.class).size(),
                table.getDataProvider().size());
    }

    @Test
    public void testAjaxChooser() {
        login();
        tester.startPage(new NewLayerPage());

        tester.assertRenderedPage(NewLayerPage.class);

        // the tester will return null if the component is there, but not visible
        assertNull(tester.getComponentFromLastRenderedPage("selectLayersContainer:selectLayers"));

        // select the first datastore
        tester.newFormTester("selector").select("storesDropDown", 1);
        tester.executeAjaxEvent("selector:storesDropDown", "change");

        // now it should be there
        assertNotNull(
                tester.getComponentFromLastRenderedPage("selectLayersContainer:selectLayers"));

        // select "choose one" item (unselect the form)
        tester.newFormTester("selector").setValue("storesDropDown", "");
        tester.executeAjaxEvent("selector:storesDropDown", "change");

        // now it should be there
        assertNull(tester.getComponentFromLastRenderedPage("selectLayersContainer:selectLayers"));
    }

    @Test
    public void testAddLayer() throws Exception {
        login();
        DataStoreInfo store =
                getCatalog().getStoreByName(MockData.CITE_PREFIX, DataStoreInfo.class);
        NewLayerPage page = new NewLayerPage(store.getId());
        tester.startPage(page);

        // get the name of the first layer in the list
        String[] names = ((DataStore) store.getDataStore(null)).getTypeNames();
        Arrays.sort(names);

        tester.clickLink(
                TABLE_PATH + ":listContainer:items:1:itemProperties:2:component:link", true);
        tester.assertRenderedPage(ResourceConfigurationPage.class);
        assertEquals(
                names[0],
                ((ResourceConfigurationPage) tester.getLastRenderedPage())
                        .getResourceInfo()
                        .getName());
    }

    @Test
    public void testAddLayerFromWFSDataStore() throws Exception {
        login();
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        DataStoreInfo storeInfo = cb.buildDataStore(MockData.CITE_PREFIX);
        ((DataStoreInfoImpl) storeInfo).setId("1");

        getCatalog().add(storeInfo);

        try {
            URL url = getClass().getResource("/WFSCapabilities.xml");

            storeInfo.getConnectionParameters().put(WFSDataStoreFactory.URL.key, url);
            // required or the store won't fetch caps from a file
            storeInfo.getConnectionParameters().put("TESTING", Boolean.TRUE);
            final ResourcePool rp = getCatalog().getResourcePool();
            DataStore store = (DataStore) rp.getDataStore(storeInfo);

            NewLayerPage page = new NewLayerPage(storeInfo.getId());
            tester.startPage(page);
            Component link =
                    tester.getComponentFromLastRenderedPage("selectLayersContainer")
                            .get("createCascadedWFSStoredQueryContainer");

            assertTrue(link.isVisible());
        } finally {
            getCatalog().remove(storeInfo);
        }
    }

    @Test
    public void testAddLayerFromNotWFSDataStore() throws Exception {
        login();
        DataStoreInfo store =
                getCatalog().getStoreByName(MockData.CITE_PREFIX, DataStoreInfo.class);
        NewLayerPage page = new NewLayerPage(store.getId());
        tester.startPage(page);

        Component link =
                tester.getComponentFromLastRenderedPage("selectLayersContainer")
                        .get("createCascadedWFSStoredQueryContainer");

        assertFalse(link.isVisible());
    }
}
