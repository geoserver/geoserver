/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import junit.framework.Test;

import org.geoserver.web.GeoServerWicketTestSupport;

public class GWCSettingsPageTest extends GeoServerWicketTestSupport {

    public static Test suite() {
        return new OneTimeTestSetup(new GWCSettingsPageTest());
    }

    public void testPageLoad() {
        GWCSettingsPage page = new GWCSettingsPage();

        tester.startPage(page);
        tester.assertRenderedPage(GWCSettingsPage.class);

        // print(page, true, true);
    }
}
