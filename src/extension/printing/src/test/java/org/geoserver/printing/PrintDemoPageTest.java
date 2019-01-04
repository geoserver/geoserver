/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class PrintDemoPageTest extends GeoServerWicketTestSupport {

    @Before
    public void setup() {
        login();
    }

    @Test
    public void testCSS() {
        tester.startPage(PrintDemoPage.class);
        tester.assertRenderedPage(PrintDemoPage.class);
        tester.assertContains("http://extjs.cachefly.net/ext-2.2.1/resources/css/ext-all.css");
        tester.assertContains("http://extjs.cachefly.net/ext-2.2.1/examples/shared/examples.css");
    }

    @Test
    public void testJavascript() {
        tester.startPage(PrintDemoPage.class);
        tester.assertRenderedPage(PrintDemoPage.class);
        tester.assertContains("http://openlayers.org/api/2.8/OpenLayers.js");
        tester.assertContains("org.geoserver.printing.PrintDemoPage/GeoExt");
        tester.assertContains("org.geoserver.printing.PrintDemoPage/GeoExtPrinting");
        tester.assertContains("org.geoserver.printing.PrintDemoPage/Printing");
    }
}
