/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class URLManglersTest extends GeoServerSystemTestSupport {

    private static final String BASEURL = "http://localhost:8080/geoserver";

    @BeforeClass
    public static void init() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @AfterClass
    public static void finalizing() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "false");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        FileUtils.copyFileToDirectory(
                new File("./src/test/resources/geoserver-environment.properties"),
                testData.getDataDirectoryRoot());
    }

    @Before
    public void setup() {
        Dispatcher.REQUEST.remove();
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
    public void testUpdateRawKVP() {
        Request wrappedRequest = new Request();
        wrappedRequest.setRawKvp(Collections.singletonMap(LanguageURLMangler.LANGUAGE, "value"));
        Dispatcher.REQUEST.set(wrappedRequest);
        String url = buildURL(BASEURL, "test", new HashMap<>(), URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?Language=value", url);
    }

    @Test
    public void testProxyBase() {
        GeoServerInfo gi = getGeoServer().getGlobal();
        gi.getSettings().setProxyBaseUrl("http://geoserver.org/");
        getGeoServer().save(gi);

        String url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://geoserver.org/test", url);
    }

    @Test
    public void testProxyBaseParametrized() {
        GeoServerInfo gi = getGeoServer().getGlobal();
        gi.getSettings().setProxyBaseUrl("${proxy.custom}");
        getGeoServer().save(gi);
        String url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://custom.host/test", url);

        // check not-matched placeholders remain intact, like the headers placeholders
        gi.getSettings()
                .setProxyBaseUrl("${X-Forwarded-Proto}://${X-Forwarded-Host}/${proxy.custom}");
        getGeoServer().save(gi);
        url = buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("${X-Forwarded-Proto}://${X-Forwarded-Host}/http://custom.host/test", url);
    }
}
