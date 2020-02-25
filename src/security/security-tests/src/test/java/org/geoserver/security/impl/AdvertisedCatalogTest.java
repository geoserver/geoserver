/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.*;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupVisibilityPolicy;
import org.geoserver.catalog.impl.AdvertisedCatalog;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AdvertisedCatalogTest extends AbstractAuthorizationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Dispatcher.REQUEST.set(new Request());
        Dispatcher.REQUEST.get().setRequest("GetCapabilities");

        populateCatalog();
    }

    @After
    public void tearDown() throws Exception {
        Dispatcher.REQUEST.set(null);
    }

    @Test
    public void testNotAdvertisedLayersInGroupWithHideGroupIfEmptyPolicy() throws Exception {
        AdvertisedCatalog sc = new AdvertisedCatalog(catalog);
        sc.setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy.HIDE_EMPTY);

        assertNull(sc.getLayerByName("topp:states"));
        assertNull(sc.getLayerByName("topp:roads"));
        LayerGroupInfo layerGroup = sc.getLayerGroupByName("topp", "layerGroupWithSomeLockedLayer");
        assertNull(layerGroup);
    }

    @Test
    public void testNotAdvertisedLayersInGroupWithNeverHideGroupPolicy() throws Exception {
        AdvertisedCatalog sc = new AdvertisedCatalog(catalog);
        sc.setLayerGroupVisibilityPolicy(LayerGroupVisibilityPolicy.HIDE_NEVER);

        assertNull(sc.getLayerByName("topp:states"));
        assertNull(sc.getLayerByName("topp:roads"));
        LayerGroupInfo layerGroup = sc.getLayerGroupByName("topp", "layerGroupWithSomeLockedLayer");
        assertNotNull(layerGroup);
        assertEquals(0, layerGroup.getLayers().size());
    }

    @Test
    public void testNotAdvertisedLayerGroup() throws Exception {
        AdvertisedCatalog sc = new AdvertisedCatalog(catalog);
        assertFalse(namedTreeC.isAdvertised());
        assertNull(sc.getLayerGroupByName(namedTreeC.getName()));
        /*assertTrue(lgA.getLayers().size()>0);
        LayerGroupInfo lgB = sc.getLayerGroupByName("namedTreeB");
        assertFalse(lgB.isAdvertised());
        assertFalse(lgB.getLayers().size()>0);*/

    }

    @Test
    public void testLayerSpecificCapabilities() throws Exception {
        AdvertisedCatalog sc = new AdvertisedCatalog(catalog);
        Dispatcher.REQUEST.get().setContext("topp/states");

        assertNotNull(sc.getLayerByName("topp:states"));
    }
}
