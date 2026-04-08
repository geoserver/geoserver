/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.GeofencePageTest)
 */
package org.geoserver.acl.plugin.web.config;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class ACLServiceConfigPageTest extends AclWicketTestSupport {
    @Before
    @Override
    public void beforeEach() throws IOException {
        super.beforeEach();
        login();
        tester.startPage(ACLServiceConfigPage.class);
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-test.xml");
    }

    @Test
    public void testErrorEmptyURL() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("servicesUrl", "");
        tester.clickLink("form:test", true);
        tester.assertRenderedPage(ACLServiceConfigPage.class);

        tester.assertContains("is required");
    }
}
