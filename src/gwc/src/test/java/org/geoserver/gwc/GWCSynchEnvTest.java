/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import org.geoserver.platform.GeoServerEnvironment;
import org.geowebcache.GeoWebCacheEnvironment;
import org.junit.AfterClass;
import org.junit.Test;

/** Tests that the GWC Environment is synchronized with the GeoServer Environment. */
public class GWCSynchEnvTest {

    @AfterClass
    public static void teardown() {
        System.clearProperty("ENV_PROPERTIES");
    }

    @Test
    public void testEnvParametrizationValues() throws Exception {
        URL url = getClass().getResource("geoserver-environment.properties");
        System.setProperty("ENV_PROPERTIES", url.getPath());
        GeoServerEnvironment genv = new GeoServerEnvironment();

        GWCSynchEnv gwcSynchEnv = new GWCSynchEnv(genv);

        GeoWebCacheEnvironment gwcEnv = new GeoWebCacheEnvironment();
        gwcSynchEnv.setGwcEnvironment(gwcEnv);

        // force sync because system setting for allowing parametrization keeps getting reset
        gwcSynchEnv.setForceSync(true);

        gwcSynchEnv.syncEnv();

        assertEquals(genv.resolveValue("${test.env}"), gwcEnv.resolveValue("${test.env}"));
    }
}
