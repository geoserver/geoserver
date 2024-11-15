/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.dggs.rhealpix;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class RHealPixDGGSFactoryTest {

    @Test
    public void testIsAvailable() throws Exception {
        RHealPixDGGSFactory rHealPixDGGSFactory = new RHealPixDGGSFactory();
        // if JEP is available, nothing bad will happen
        if (!rHealPixDGGSFactory.isAvailable()) {
            // but if not available, this second call used to throw an uncaught Error
            assertFalse(rHealPixDGGSFactory.isAvailable());
        }
    }
}
