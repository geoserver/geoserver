/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import junit.framework.Test;

import org.geoserver.web.GeoServerWicketTestSupport;

public class GridSetsPageTest extends GeoServerWicketTestSupport {

    /**
     * This is a read only test, but we save the per test set up cost anyways
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GridSetsPageTest());
    }

    public void testPageLoad() {
        GridSetsPage page = new GridSetsPage();

        tester.startPage(page);
        tester.assertRenderedPage(GridSetsPage.class);

        // print(page, true, true);
    }
}
