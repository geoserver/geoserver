/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.junit.Assert.*;

import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geowebcache.layer.TileLayer;
import org.junit.Test;

public class CachedLayersPageTest extends GeoServerWicketTestSupport {
    
    protected static final String NATURE_GROUP = "nature";
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // setup a layer group
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        if(lakes != null && forests != null) {
            group.setName(NATURE_GROUP);
            group.getLayers().add(lakes);
            group.getLayers().add(forests);
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.calculateLayerGroupBounds(group);
            catalog.add(group);
        }
    }
    
    @Test
    public void testPageLoad() {
        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        tester.assertRenderedPage(CachedLayersPage.class);

        // print(page, true, true);
    }
    
    @Test
    public void testLayerGroupLink() {
        GWC gwc = GWC.get();
        TileLayer tileLayer = gwc.getTileLayerByName(NATURE_GROUP);
        assertNotNull(tileLayer);
        
        tester.startComponentInPage(new ConfigureCachedLayerAjaxLink("test", new TileLayerDetachableModel(tileLayer.getName()), null));
        // tester.debugComponentTrees();
        tester.executeAjaxEvent("test:link", "click");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(LayerGroupEditPage.class);
    }
    
    
}
