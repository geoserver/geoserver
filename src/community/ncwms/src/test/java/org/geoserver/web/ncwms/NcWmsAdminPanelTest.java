/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ncwms;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServer;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.ncwms.NcWMSInfoImpl;
import org.geoserver.wms.ncwms.NcWmsInfo;
import org.geoserver.wms.ncwms.NcWmsService;
import org.geoserver.wms.web.WMSAdminPage;
import org.junit.Test;

public class NcWmsAdminPanelTest extends GeoServerWicketTestSupport {

    private static final int TIME_SERIES_THREADS = 7;
    private static final int MAXTIMES = 200;

    @Test
    public void testPanel() throws Exception {
        // set a specific number of threads
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        NcWmsInfo ncwms = new NcWMSInfoImpl();
        ncwms.setTimeSeriesPoolSize(TIME_SERIES_THREADS);
        ncwms.setMaxTimeSeriesValues(200);
        wms.getMetadata().put(NcWmsService.WMS_CONFIG_KEY, ncwms);
        gs.save(wms);

        login();

        // start the WMS admin page
        tester.startPage(WMSAdminPage.class);
        tester.assertModelValue(
                "form:extensions:0:content:timeSeriesPoolSize", TIME_SERIES_THREADS);
        tester.assertModelValue("form:extensions:0:content:maxTimeSeriesValues", MAXTIMES);

        // update the values
        final int NEW_POOL_VALUE = 5;
        final int NEW_MAXTIMES_VALUE = 27;
        FormTester form = tester.newFormTester("form");
        form.setValue("extensions:0:content:timeSeriesPoolSize", String.valueOf(NEW_POOL_VALUE));
        form.setValue(
                "extensions:0:content:maxTimeSeriesValues", String.valueOf(NEW_MAXTIMES_VALUE));
        form.submit("submit");
        tester.assertNoErrorMessage();

        NcWmsInfo updatedConfig =
                gs.getService(WMSInfo.class)
                        .getMetadata()
                        .get(NcWmsService.WMS_CONFIG_KEY, NcWmsInfo.class);
        assertEquals(NEW_POOL_VALUE, updatedConfig.getTimeSeriesPoolSize());
        assertEquals(NEW_MAXTIMES_VALUE, updatedConfig.getMaxTimeSeriesValues());
    }
}
