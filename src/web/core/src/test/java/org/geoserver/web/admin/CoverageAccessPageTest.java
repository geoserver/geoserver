/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class CoverageAccessPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testLoad() throws Exception {
        login();
        tester.startPage(CoverageAccessPage.class);
        tester.assertRenderedPage(CoverageAccessPage.class);
    }
}
