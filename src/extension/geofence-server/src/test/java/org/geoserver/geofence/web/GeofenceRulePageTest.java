/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.data.layergroup.LayerGroupBaseTest;
import org.junit.Test;
import org.opengis.referencing.FactoryException;

public class GeofenceRulePageTest extends LayerGroupBaseTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        login();
        super.onSetUp(testData);
    }

    @Test
    public void testAddLayerGroupRule() throws FactoryException {
        GeofenceRulesModel model = new GeofenceRulesModel();
        tester.startPage(new GeofenceRulePage(model.newRule(), model));
        tester.assertRenderedPage(GeofenceRulePage.class);
        FormTester ft = tester.newFormTester("form");
        Form form = ft.getForm();
        DropDownChoice workspaceDropDown = (DropDownChoice) form.get("tabs:panel:workspace");
        assertEquals(workspaceDropDown.getValue(), "");
        DropDownChoice layer = (DropDownChoice) form.get("tabs:panel:layer");
        // test that with no workspace set global layer groups are present in the dropdown choice
        assertTrue(layer.getChoices().size() > 0);
        assertEquals(layer.getValue(), "");
        ft.setValue("tabs:panel:layer", "nestedLayerGroup");
        ft.setValue("tabs:panel:access", "LIMIT");
        ft.setValue("tabs:panel:catalogMode", "HIDE");
        ft.setValue(
                "tabs:panel:allowedArea",
                "SRID=4326;MULTIPOLYGON(((30 10, 40 40, 20 40, 10 20, 30 10))");
        ft.submit("save");
        tester.assertNoErrorMessage();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        /** Dispose Services */
        this.testData = new SystemTestData();

        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}",
                    e);
        }
    }
}
