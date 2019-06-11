/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.rest;

import static org.junit.Assert.assertNull;

import org.geoserver.geofence.rest.xml.JaxbRule;
import org.junit.Test;

public class JaxbRuleTest {

    @Test
    public void testSetArea() {
        JaxbRule.LayerDetails details = new JaxbRule.LayerDetails();
        // used to NPE here
        details.setAllowedArea(null);
        assertNull(details.getAllowedArea());
    }
}
