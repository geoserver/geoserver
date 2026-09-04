/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.web;

import org.apache.wicket.model.StringResourceModel;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.server.GeofenceDatabaseRule;
import org.geoserver.geofence.web.GeofencePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

/** {@link GeofencePage}'s "test connection" button, exercised with the embedded engine actually on the classpath. */
public class GeofencePageEmbeddedTest extends GeoServerWicketTestSupport {

    @ClassRule
    public static final GeofenceDatabaseRule database = new GeofenceDatabaseRule();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void before() {
        login();
        tester.startPage(GeofencePage.class);
    }

    @Test
    public void testConnectionSucceeds() {
        tester.clickLink("form:test", true);
        String success =
                new StringResourceModel(GeofencePage.class.getSimpleName() + ".connectionSuccessful").getObject();
        tester.assertInfoMessages(success);
    }
}
