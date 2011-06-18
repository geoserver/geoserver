package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;

public class GlobalSettingsPageTest extends GeoServerWicketTestSupport {
    public void testValues() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();

        login();
        tester.startPage(GlobalSettingsPage.class);
        tester.assertComponent("form:verbose", CheckBox.class);
        tester.assertModelValue("form:verbose", info.isVerbose());
    }
}
