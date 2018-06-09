/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.web;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WCSEOAdminPanelTest extends GeoServerWicketTestSupport {

    private WCSInfo wcs;

    @Before
    public void setup() {

        // prepare read only metadata
        wcs = getGeoServer().getService(WCSInfo.class);
        getGeoServer().save(wcs);

        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new WCSEOAdminPanel(id, new Model(wcs));
                            }
                        }));
    }

    @Test
    public void testEditBasic() {
        // print(tester.getLastRenderedPage(), true, true);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:enabled", true);
        ft.submit();

        // print(tester.getLastRenderedPage(), true, true);

        tester.assertModelValue("form:panel:enabled", true);
        assertTrue((boolean) wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class));
    }
}
