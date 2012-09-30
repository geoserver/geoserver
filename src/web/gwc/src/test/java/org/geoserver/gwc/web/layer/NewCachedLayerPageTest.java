/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class NewCachedLayerPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        NewCachedLayerPage page = new NewCachedLayerPage();

        tester.startPage(page);
        tester.assertRenderedPage(NewCachedLayerPage.class);

        // print(page, true, true);
    }
}
