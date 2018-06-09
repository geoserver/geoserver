/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.web.CSWAdminPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.KeywordsEditor;
import org.junit.Test;

public class CSWAdminPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() throws Exception {
        CSWInfo csw = getGeoServerApplication().getGeoServer().getService(CSWInfo.class);

        login();
        tester.startPage(CSWAdminPage.class);

        tester.assertRenderedPage(CSWAdminPage.class);

        // test that components have been filled as expected
        tester.assertComponent("form:keywords", KeywordsEditor.class);
        tester.assertModelValue("form:keywords", csw.getKeywords());
    }
}
