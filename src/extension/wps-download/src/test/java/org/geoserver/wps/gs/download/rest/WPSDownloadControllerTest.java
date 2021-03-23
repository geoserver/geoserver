/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.rest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wps.gs.download.DownloadServiceConfiguration;
import org.geoserver.wps.gs.download.DownloadServiceConfigurationWatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class WPSDownloadControllerTest extends GeoServerSystemTestSupport {

    @Before
    public void resetConfiguration() throws IOException {
        DownloadServiceConfigurationWatcher watcher =
                GeoServerExtensions.bean(DownloadServiceConfigurationWatcher.class);
        watcher.setConfiguration(DownloadServiceConfiguration.getDemoConfiguration());
    }

    @Test
    public void testGetConfiguration() throws Exception {
        Document dom = getAsDOM("/rest/services/wps/download.xml");
        XpathEngine xpath = XMLUnit.newXpathEngine();
        checkValue(dom, xpath, "maxFeatures", "100000");
        checkValue(dom, xpath, "rasterSizeLimits", "64000000");
        checkValue(dom, xpath, "writeLimits", "64000000");
        checkValue(dom, xpath, "hardOutputLimit", "52428800");
        checkValue(dom, xpath, "compressionLevel", "4");
        checkValue(dom, xpath, "maxAnimationFrames", "1000");
    }

    @Test
    public void testUpdateConfiguration() throws Exception {
        // send over update
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DownloadServiceConfiguration>\n"
                        + "  <maxFeatures>123</maxFeatures>\n"
                        + "  <rasterSizeLimits>456000</rasterSizeLimits>\n"
                        + "  <writeLimits>789000</writeLimits>\n"
                        + "  <hardOutputLimit>123456</hardOutputLimit>\n"
                        + "  <compressionLevel>8</compressionLevel>\n"
                        + "  <maxAnimationFrames>56</maxAnimationFrames>\n"
                        + "</DownloadServiceConfiguration>";
        MockHttpServletResponse response =
                putAsServletResponse("/rest/services/wps/download", xml, "text/xml");
        assertEquals(200, response.getStatus());

        DownloadServiceConfigurationWatcher watcher =
                GeoServerExtensions.bean(DownloadServiceConfigurationWatcher.class);
        DownloadServiceConfiguration config = watcher.getConfiguration();
        assertEquals(123, config.getMaxFeatures());
        assertEquals(456000, config.getRasterSizeLimits());
        assertEquals(789000, config.getWriteLimits());
        assertEquals(123456, config.getHardOutputLimit());
        assertEquals(8, config.getCompressionLevel());
        assertEquals(56, config.getMaxAnimationFrames());
    }

    public void checkValue(Document dom, XpathEngine xpath, String attribute, String expected)
            throws XpathException {
        assertEquals(expected, xpath.evaluate("DownloadServiceConfiguration/" + attribute, dom));
    }
}
