/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import junit.framework.Test;

import org.geoserver.web.GeoServerWicketTestSupport;

public class CachedLayersPageTest extends GeoServerWicketTestSupport {

    public static Test suite() {
        return new OneTimeTestSetup(new CachedLayersPageTest());
    }

    public void testPageLoad() {
        CachedLayersPage page = new CachedLayersPage();

        tester.startPage(page);
        tester.assertRenderedPage(CachedLayersPage.class);

        // print(page, true, true);
    }
}
