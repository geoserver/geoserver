/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class PrintDemoPageTest extends GeoServerWicketTestSupport  {
	
	@Before 
    public void setup() {
        login();
    }
	
	@Test
    public void testCSS() {
        tester.startPage(PrintDemoPage.class);
        tester.assertRenderedPage(PrintDemoPage.class);
        tester.assertContains("ext-all.css");
        tester.assertContains("examples.css");
    }
	
	public void testJavascript() {
        tester.startPage(PrintDemoPage.class);
        tester.assertRenderedPage(PrintDemoPage.class);
        tester.assertContains("OpenLayers.js");
        tester.assertContains("GeoExt.js");
        tester.assertContains("GeoExtPrinting.js");
    }
}
