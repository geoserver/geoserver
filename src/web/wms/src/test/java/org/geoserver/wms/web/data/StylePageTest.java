/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class StylePageTest extends GeoServerWicketTestSupport {
    
    @Test
    public void testPageLoad() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
    }
}
