/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wps.gs.download.DownloadServiceConfiguration;
import org.geoserver.wps.gs.download.DownloadServiceConfigurationWatcher;
import org.geoserver.wps.web.WPSAdminPage;
import org.junit.Before;
import org.junit.Test;

public class DownloadLimitsPanelTest extends GeoServerWicketTestSupport {

    @Before
    @Override
    public void login() {
        super.login();
    }

    @Before
    public void resetConfiguration() throws IOException {
        DownloadServiceConfigurationWatcher watcher =
                GeoServerExtensions.bean(DownloadServiceConfigurationWatcher.class);
        watcher.setConfiguration(DownloadServiceConfiguration.getDemoConfiguration());
    }

    @Test
    public void testDefaults() throws Exception {
        tester.startPage(WPSAdminPage.class);

        // pluggable panels, make sure to get the right one
        ListView lv = (ListView) tester.getComponentFromLastRenderedPage("form:extensions");
        String path = getComponentPath(lv, DownloadLimitsPanel.class);
        assertNotNull(path);

        tester.assertModelValue(path + ":maxFeatures", 100000l);
        tester.assertModelValue(path + ":rasterSizeLimits", 64000000l);
        tester.assertModelValue(path + ":writeLimits", 64000000l);
        tester.assertModelValue(path + ":hardOutputLimit", 52428800l);
        tester.assertModelValue(path + ":maxAnimationFrames", 1000);
        tester.assertModelValue(path + ":compressionLevel", 4);
    }

    @Test
    public void testSave() throws Exception {
        WPSAdminPage page = tester.startPage(WPSAdminPage.class);
        print(page, true, true, true);

        ListView lv = (ListView) tester.getComponentFromLastRenderedPage("form:extensions");
        String path = getComponentPath(lv, DownloadLimitsPanel.class);
        assertNotNull(path);

        FormTester ft = tester.newFormTester("form");
        String formPath = path.substring("form".length() + 1);
        ft.setValue(formPath + ":maxFeatures", "123");
        ft.setValue(formPath + ":rasterSizeLimits", "456000");
        ft.setValue(formPath + ":writeLimits", "789000");
        ft.setValue(formPath + ":hardOutputLimit", "1234567");
        ft.setValue(formPath + ":maxAnimationFrames", "56");
        ft.setValue(formPath + ":compressionLevel", "8");
        ft.submit();

        DownloadServiceConfigurationWatcher watcher =
                GeoServerExtensions.bean(DownloadServiceConfigurationWatcher.class);
        DownloadServiceConfiguration config = watcher.getConfiguration();
        assertEquals(123, config.getMaxFeatures());
        assertEquals(456000, config.getRasterSizeLimits());
        assertEquals(789000, config.getWriteLimits());
        assertEquals(1234567, config.getHardOutputLimit());
        assertEquals(56, config.getMaxAnimationFrames());
        assertEquals(8, config.getCompressionLevel());
    }
}
