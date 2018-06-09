/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.*;
import static org.junit.Assert.*;

import java.util.Collections;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class URLManglersTest extends GeoServerSystemTestSupport {

    private static final String BASEURL = "http://localhost:8080/geoserver";

    @Before
    public void setup() {
        GeoServerInfo gi = getGeoServer().getGlobal();
        gi.getSettings().setProxyBaseUrl(null);
        getGeoServer().save(gi);
    }

    @Test
    public void testBasic() {
        String url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test", url);
    }

    @Test
    public void testKVP() {
        String url =
                buildURL(
                        BASEURL,
                        "test",
                        Collections.singletonMap("param", "value()"),
                        URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?param=value%28%29", url);
    }

    @Test
    public void testProxyBase() {
        GeoServerInfo gi = getGeoServer().getGlobal();
        gi.getSettings().setProxyBaseUrl("http://geoserver.org/");
        getGeoServer().save(gi);

        String url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://geoserver.org/test", url);
    }
}
