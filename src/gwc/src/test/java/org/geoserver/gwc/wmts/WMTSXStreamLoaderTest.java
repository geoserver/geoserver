/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class WMTSXStreamLoaderTest extends GeoServerSystemTestSupport {

    @Test
    public void testLoadSimpleConfiguration() throws Exception {
        // imitating the necessary xml parser and factories
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        WMTSXStreamLoader loader = GeoServerExtensions.bean(WMTSXStreamLoader.class);
        loader.initXStreamPersister(xp, getGeoServer());
        // parsing service information
        try (InputStream is = getClass().getResourceAsStream("/wmts-test.xml")) {
            WMTSInfo serviceInfo = loader.initialize(xp.load(is, WMTSInfo.class));
            assertThat(serviceInfo.getId(), is("WMTS-TEST"));
            assertThat(serviceInfo.isEnabled(), is(false));
            assertThat(serviceInfo.getName(), is("WMTS"));
            assertThat(serviceInfo.getTitle(), is("GeoServer Web Map Tile Service"));
            assertThat(serviceInfo.getMaintainer(), is("geoserver"));
            assertThat(serviceInfo.getAbstract(), is("Testing the WMTS service."));
            assertThat(serviceInfo.getAccessConstraints(), is("SOME"));
            assertThat(serviceInfo.getFees(), is("MONEY"));
            assertThat(serviceInfo.getOnlineResource(), is("http://geoserver.org"));
            assertThat(serviceInfo.getSchemaBaseURL(), is("http://schemas.opengis.net"));
        }
    }
}
