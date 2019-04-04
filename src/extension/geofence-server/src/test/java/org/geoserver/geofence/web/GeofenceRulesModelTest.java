/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/**
 * Geofence Rules Model Test
 *
 * @author Niels Charlier
 */
public class GeofenceRulesModelTest extends GeoServerWicketTestSupport {

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

    @Test
    public void testRulesModel() {
        GeofenceRulesModel model = new GeofenceRulesModel();

        ShortRule rule1 = model.newRule();
        rule1.setUserName("pipo");
        model.save(rule1);

        ShortRule rule2 = model.newRule();
        rule2.setUserName("jantje");
        model.save(rule2);

        ShortRule rule3 = model.newRule();
        rule3.setUserName("oen");
        model.save(rule3);

        assertEquals(3, model.getItems().size());
        assertEquals(rule1, model.getItems().get(2));
        assertEquals(rule2, model.getItems().get(1));
        assertEquals(rule3, model.getItems().get(0));

        assertEquals(0, rule3.getPriority());
        assertEquals(1, rule2.getPriority());
        assertEquals(2, rule1.getPriority());

        assertSynchronized(model);

        assertFalse(model.canDown(rule1));
        assertFalse(model.canUp(rule3));
        assertTrue(model.canDown(rule2));
        assertTrue(model.canUp(rule2));

        model.moveDown(rule2);
        model.moveUp(rule3);
        assertEquals(rule3, model.getItems().get(0));
        assertEquals(rule1, model.getItems().get(1));
        assertEquals(rule2, model.getItems().get(2));

        assertEquals(0, rule3.getPriority());
        assertEquals(1, rule1.getPriority());
        assertEquals(2, rule2.getPriority());

        rule2.setService("WFS");
        model.save(rule2);
        model.remove(Arrays.asList(rule1, rule3));

        assertEquals(1, model.getItems().size());

        assertSynchronized(model);
    }

    public void assertSynchronized(GeofenceRulesModel model) {
        GeofenceRulesModel freshModel = new GeofenceRulesModel(); // reload rules from db
        assertEquals(model.getItems().toString(), freshModel.getItems().toString());
    }
}
