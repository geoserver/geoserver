/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.web;

import static org.junit.Assert.assertTrue;

import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.mapml.MapMLConstants;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link MapMLAdminPanel}. */
public class MapMLAdminPanelTest extends GeoServerWicketTestSupport {
    private WMSInfo wms;

    @Before
    public void setup() {
        wms = getGeoServer().getService(WMSInfo.class);
        getGeoServer().save(wms);

        tester.startPage(new FormTestPage((ComponentBuilder) id -> new MapMLAdminPanel(id, new Model<>(wms))));
    }

    @Test
    public void testEditBasic() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:multiextent", true);
        ft.submit();

        tester.assertModelValue("form:panel:multiextent", true);
        assertTrue(wms.getMetadata().get(MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT, Boolean.class));
    }
}
