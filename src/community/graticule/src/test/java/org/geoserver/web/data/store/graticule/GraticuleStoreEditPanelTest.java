/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.graticule;

import static org.geotools.data.graticule.GraticuleDataStoreFactory.BOUNDS;
import static org.geotools.data.graticule.GraticuleDataStoreFactory.STEPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Map;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geotools.data.graticule.GraticuleDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.junit.Before;
import org.junit.Test;

public class GraticuleStoreEditPanelTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Before
    public void loginBefore() {
        super.login();
    }

    @Test
    public void testCreateModify() throws Exception {
        tester.startPage(new DataAccessNewPage(GraticuleDataStoreFactory.DISPLAY_NAME));
        tester.assertNoErrorMessage();

        tester.executeAjaxEvent(
                "dataStoreForm:parametersPanel:configsContainer:gratpanel:generateBoundsFromCRS",
                "click");
        FormTester ft = tester.newFormTester("dataStoreForm");
        ft.setValue(
                "parametersPanel:configsContainer:gratpanel:steps:border:border_body:paramValue",
                "10");
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "graticule10");
        ft.submit("save");

        tester.assertNoErrorMessage();

        // check the store has been created
        DataStoreInfo graticule10 = getCatalog().getDataStoreByName("graticule10");
        assertNotNull(graticule10);
        Map<String, Serializable> parameters = graticule10.getConnectionParameters();
        assertEquals("10", parameters.get(STEPS.key));
        ReferencedEnvelope world =
                new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));
        assertEquals(
                world, Converters.convert(parameters.get(BOUNDS.key), ReferencedEnvelope.class));

        // open again, and save (used to fail due to empty bounds forced during panel construction)
        tester.startPage(new DataAccessEditPage(graticule10.getId()));
        tester.assertNoErrorMessage();
        ft = tester.newFormTester("dataStoreForm");
        ft.submit("save");
        tester.assertNoErrorMessage();
    }
}
