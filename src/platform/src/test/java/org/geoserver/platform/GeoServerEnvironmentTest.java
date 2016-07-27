/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import org.geowebcache.GeoWebCacheExtensions;

import junit.framework.TestCase;

/**
 * Unit test suite for {@link GeoWebCacheExtensions}
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 */
public class GeoServerEnvironmentTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("TEST_SYS_PROPERTY", "ABC");
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        System.setProperty("TEST_SYS_PROPERTY", "");
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "");
    }

    public void testSystemProperty() {
        // check for a property we did set up in the setUp
        GeoServerEnvironment genv = new GeoServerEnvironment();
        assertEquals("ABC", genv.resolveValue("${TEST_SYS_PROPERTY}"));

        assertEquals("ABC", genv.resolveValue("${TEST_SYS_PROPERTY}"));
        assertEquals("${TEST_PROPERTY}", genv.resolveValue("${TEST_PROPERTY}"));
    }

}
