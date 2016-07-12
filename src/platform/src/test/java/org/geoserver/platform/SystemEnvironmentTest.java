/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemEnvironmentTest {

    @Test
    public void testSystemPropertiesStatus() {
        String key =  System.getenv().keySet().iterator().next();
        String value = System.getenv(key);
        
        SystemEnvironmentStatus status = new SystemEnvironmentStatus();
        assertTrue(status.getMessage().isPresent());
        assertTrue(status.getMessage().get().contains(key));
        assertTrue(status.getMessage().get().contains(value));
    }

}