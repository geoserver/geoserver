/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.junit.Assert.*;

import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class LayerGroupPageTest extends LayerGroupBaseTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        login();
        tester.startPage(LayerGroupPage.class);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>)
                        tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(getCatalog().getLayerGroups().size(), dv.size());
        LayerGroupInfo lg = (LayerGroupInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals(getCatalog().getLayerGroups().get(0), lg);
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>)
                        tester.getComponentFromLastRenderedPage("table:listContainer:items");

        LayerGroupProvider provider = (LayerGroupProvider) dv.getDataProvider();

        // should have these columns
        assertTrue(provider.getProperties().contains(LayerGroupProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(LayerGroupProvider.MODIFIED_TIMESTAMP));
    }
}
