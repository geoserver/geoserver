/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemPropertiesStatusTest {

    String KEY = "TESTTESTTEST";

    String VALUE = "ABCDEF_TEST_TEST_TEST";

    @Test
    public void testSystemPropertiesStatus() {
        System.setProperty(KEY, VALUE);
        SystemPropertyStatus status = new SystemPropertyStatus();
        assertTrue(status.getMessage().isPresent());
        assertTrue(status.getMessage().get().contains(KEY));
        assertTrue(status.getMessage().get().contains(VALUE));
    }
}
