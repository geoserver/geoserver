/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.junit.Test;

public class NetCDFOutSettingsPanelTest extends GeoServerWicketTestSupport {

    @Test
    public void testComponent() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        MetadataMap map = info.getSettings().getMetadata();

        login();
        // Opening the selected page
        tester.startPage(new GlobalSettingsPage());
        tester.assertRenderedPage(GlobalSettingsPage.class);
        tester.assertNoErrorMessage();

        // check if the component is present and initialized
        tester.assertComponent("form:extensions:0:content", NetCDFOutSettingsPanel.class);
        tester.assertComponent("form:extensions:0:content:panel", NetCDFPanel.class);

        NetCDFSettingsContainer container =
                map.get(NetCDFSettingsContainer.NETCDFOUT_KEY, NetCDFSettingsContainer.class);
        // Ensure the element is in the map
        assertNotNull(container);
        // Ensure the panel is present
        NetCDFPanel panel =
                (NetCDFPanel)
                        tester.getComponentFromLastRenderedPage("form:extensions:0:content:panel");
        assertNotNull(panel);
        // Check that the values are the same
        NetCDFSettingsContainer container2 = (NetCDFSettingsContainer) panel.getModelObject();
        assertNotNull(container2);

        assertEquals(container.getCompressionLevel(), container2.getCompressionLevel(), 0.001d);
        //        assertEquals(container.getNetcdfVersion(), container2.getNetcdfVersion());
        assertEquals(container.isShuffle(), container2.isShuffle());
        assertEquals(container.isCopyAttributes(), container2.isCopyAttributes());
        assertEquals(container.isCopyGlobalAttributes(), container2.isCopyGlobalAttributes());

        assertNotNull(container.getGlobalAttributes());
        assertNotNull(container2.getGlobalAttributes());
        assertEquals(container.getGlobalAttributes(), container2.getGlobalAttributes());

        assertNotNull(container.getVariableAttributes());
        assertNotNull(container2.getVariableAttributes());
        assertEquals(container.getVariableAttributes(), container2.getVariableAttributes());

        assertNotNull(container.getExtraVariables());
        assertNotNull(container2.getExtraVariables());
        assertEquals(container.getExtraVariables(), container2.getExtraVariables());
    }
}
