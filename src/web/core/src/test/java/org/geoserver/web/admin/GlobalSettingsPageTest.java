/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GlobalSettingsPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();

        login();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertComponent("form:verbose", CheckBox.class);
        tester.assertModelValue("form:verbose", info.isVerbose());
    }
}
