/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.TextField;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class JAIPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() {
        JAIInfo info = (JAIInfo) getGeoServerApplication()
            .getGeoServer()
            .getGlobal().getJAI();

        login();

        tester.startPage(JAIPage.class);
        tester.assertComponent("form:tileThreads", TextField.class);
        tester.assertModelValue("form:tileThreads", info.getTileThreads());
    }
}
