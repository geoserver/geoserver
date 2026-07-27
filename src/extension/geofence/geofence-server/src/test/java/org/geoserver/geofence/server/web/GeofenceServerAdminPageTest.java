/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.web;

import static org.junit.Assert.assertEquals;

import java.util.logging.Level;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.util.tester.FormTester;
import org.geofence.core.db.GeofenceTestDatabase;
import org.geofence.core.services.AdminRuleAdminService;
import org.geofence.core.services.dto.ShortAdminRule;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.config.GeoFencePropertyPlaceholderConfigurer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Before;
import org.junit.Test;

public class GeofenceServerAdminPageTest extends GeoServerWicketTestSupport {

    static {
        GeofenceTestDatabase.configureAsDatasourceOverride();
    }

    static GeoFencePropertyPlaceholderConfigurer configurer;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void before() {
        login();
        // this test asserts on the absolute rule count, so start from an empty table
        AdminRuleAdminService adminService =
                (AdminRuleAdminService) GeoServerApplication.get().getBean("adminRuleAdminService");
        for (ShortAdminRule r : adminService.getAll()) {
            adminService.delete(r.getId());
        }
        tester.startPage(GeofenceServerAdminPage.class);
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
            LOGGER.log(Level.WARNING, "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}", e);
        }
    }

    @Test
    public void testAddNewRuleLink() {
        tester.assertRenderedPage(GeofenceServerAdminPage.class);
        tester.assertComponent("addNew", AjaxLink.class);
        tester.clickLink("addNew");
        tester.assertRenderedPage(GeofenceAdminRulePage.class);

        // submit a new rule
        FormTester form = tester.newFormTester("form");
        form.submit("save");

        tester.assertRenderedPage(GeofenceServerAdminPage.class);

        // check the rules model
        @SuppressWarnings("unchecked")
        GeoServerTablePanel<ShortAdminRule> rulesPanel =
                (GeoServerTablePanel<ShortAdminRule>) tester.getComponentFromLastRenderedPage("rulesPanel");
        assertEquals(1, rulesPanel.getDataProvider().size());
    }
}
