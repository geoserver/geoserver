/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public class CSPDefaultConfigurationTest {

    @Test
    public void testNotSameInstance() {
        // a new instance should be created each time since it is mutable
        CSPConfiguration instance1 = CSPDefaultConfiguration.newInstance();
        assertNotNull(instance1);
        CSPConfiguration instance2 = CSPDefaultConfiguration.newInstance();
        assertEquals(instance1, instance2);
        assertNotSame(instance1, instance2);
    }
}
