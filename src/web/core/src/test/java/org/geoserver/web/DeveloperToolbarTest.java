/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.RuntimeConfigurationType;
import org.junit.Test;

public class DeveloperToolbarTest extends GeoServerWicketTestSupport {

    @Override
    protected RuntimeConfigurationType getWicketConfiguration() {
        return RuntimeConfigurationType.DEVELOPMENT;
    }

    /** Smoke test: the home page displays without error in development mode, the developer toolbar is visible */
    @Test
    public void testHomePage() {
        tester.startPage(GeoServerHomePage.class);
        tester.assertRenderedPage(GeoServerHomePage.class);
        tester.assertNoErrorMessage();
        tester.assertComponent("devButtons", DeveloperToolbar.class);
        tester.assertVisible("devButtons");
    }
}
