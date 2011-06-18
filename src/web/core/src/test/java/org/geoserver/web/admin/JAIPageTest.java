package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.TextField;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.GeoServerWicketTestSupport;

public class JAIPageTest extends GeoServerWicketTestSupport {
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
