/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rememberme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Test;

public class RememberMeServicesConfigTest {

    @After
    public void clearProperty() {
        System.clearProperty(RememberMeServicesConfig.KEY_PROPERTY);
    }

    @Test
    public void testKeyFromClusterProperty() {
        System.setProperty(RememberMeServicesConfig.KEY_PROPERTY, "cluster-shared-key");
        assertEquals("cluster-shared-key", RememberMeServicesConfig.resolveKey());
    }

    @Test
    public void testRandomKeyWhenPropertyUnset() {
        System.clearProperty(RememberMeServicesConfig.KEY_PROPERTY);

        String key = RememberMeServicesConfig.resolveKey();

        assertFalse(key.isBlank());
        assertEquals(64, key.length());
        assertNotEquals("geoserver", key);
    }

    @Test
    public void testClusterPropertyWinsOverRandom() {
        String random = RememberMeServicesConfig.resolveKey();

        System.setProperty(RememberMeServicesConfig.KEY_PROPERTY, "cluster-shared-key");
        assertEquals("cluster-shared-key", RememberMeServicesConfig.resolveKey());
        assertNotEquals(random, "cluster-shared-key");
    }
}
