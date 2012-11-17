/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GeoServerPagingNavigatorTest extends GeoServerWicketTestSupport {
    @Test
    public void testPageLoads() throws Exception {
        tester.startPage(GeoServerPagingNavigatorTestPage.class);
    }
}
