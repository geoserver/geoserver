/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GridSetsPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        login();
        GridSetsPage page = new GridSetsPage();

        tester.startPage(page);
        tester.assertRenderedPage(GridSetsPage.class);

        // print(page, true, true);
    }
}
